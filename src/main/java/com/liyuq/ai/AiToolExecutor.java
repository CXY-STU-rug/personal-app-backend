package com.liyuq.ai;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyuq.DTO.FinanceRecordsDto;
import com.liyuq.DTO.TodosDto;
import com.liyuq.entity.FinanceCategories;
import com.liyuq.mapper.FinanceCategoriesMapper;
import com.liyuq.service.CalendarService;
import com.liyuq.service.FinanceRecordsService;
import com.liyuq.service.NotesService;
import com.liyuq.service.TodosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AI工具执行器：把模型的"使唤"（工具名+参数）翻译成对现有service的真实调用。
 * 铁律：只复用已有service，不自己写库——归属校验、参数校验全自动继承，AI绝不能绕过。
 */
@Component
public class AiToolExecutor {

    @Autowired
    private TodosService todosService;
    @Autowired
    private FinanceRecordsService financeService;   // 复用记账service（统计3.12）
    @Autowired
    private NotesService notesService;              // 复用笔记service（搜索3.17）
    @Autowired
    private FinanceCategoriesMapper categoriesMapper;   // 记账工具要把"分类名"翻译成categoryId
    @Autowired
    private CalendarService calendarService;            // 复用日历service（日视图3.23）
    @Autowired
    private ObjectMapper objectMapper;              // 查询类工具：把结果VO序列化成JSON发回给模型

    public String execute(String toolName, JsonNode args) throws Exception {
        if ("create_todo".equals(toolName)) {
            TodosDto todo = new TodosDto();
            todo.setTitle(args.path("title").asText());   // title是required，模型一定会给

            // deadline/priority是可选的：模型没传时path给的是幽灵节点，
            // 硬转会炸（parse("")抛异常）。所以先判"有没有传"，没传就留null，service那边有兜底。
            if (args.has("deadline") && !args.path("deadline").asText().isBlank()) {
                todo.setDeadline(LocalDateTime.parse(
                        args.path("deadline").asText(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));   // 模型按这个格式给
            }
            if (args.has("priority")) {
                todo.setPriority(args.path("priority").asInt());
            }

            Long id = todosService.create(todo);
            return "已创建待办，id=" + id;
        }

        // ===== 查询类工具：结果VO序列化成JSON字符串返回，模型自己读JSON组织人话 =====

        if ("get_finance_statistics".equals(toolName)) {
            // month是可选：模型没传就传null，service那边默认当月
            String month = args.has("month") && !args.path("month").asText().isBlank()
                    ? args.path("month").asText() : null;
            return objectMapper.writeValueAsString(financeService.listStatisticFinanceRecords(month));
        }

        if ("search_notes".equals(toolName)) {
            String keyword = args.path("keyword").asText();   // keyword是required
            return objectMapper.writeValueAsString(notesService.searchNotes(keyword));
        }

        if ("query_calendar".equals(toolName)) {
            String date = args.path("date").asText();   // date是required，格式yyyy-MM-dd
            return objectMapper.writeValueAsString(calendarService.dayView(date));
        }

        if ("query_todos".equals(toolName)) {
            // 决策①：scope直接对齐现有list视角(today/upcoming/completed/all)，零映射
            // all→null（list里null=全部）；其余原样透传
            String scope = args.path("scope").asText();
            String status = "all".equals(scope) ? null : scope;
            return objectMapper.writeValueAsString(todosService.list(status));
        }

        if ("create_finance_record".equals(toolName)) {
            Integer type = args.path("type").asInt();                    // 1支出 2收入
            String categoryName = args.path("categoryName").asText();
            BigDecimal amount = new BigDecimal(args.path("amount").asText());  // 用字符串构造BigDecimal，别用double（精度会丢）
            String remark = args.has("remark") ? args.path("remark").asText() : null;

            // 决策②：分类名→categoryId。先按"名字+type"精确查
            LambdaQueryWrapper<FinanceCategories> qw = new LambdaQueryWrapper<>();
            qw.eq(FinanceCategories::getName, categoryName).eq(FinanceCategories::getType, type);
            FinanceCategories cat = categoriesMapper.selectOne(qw);

            Integer categoryId;
            Integer realType;
            if (cat != null) {
                categoryId = cat.getId();
                realType = cat.getType();     // 用分类自己的type，不信模型传的（防收支不一致撞校验）
            } else {
                // 查不到 → 归"其他支出"或"其他收入"，type按模型给的走
                String otherName = (type != null && type == 2) ? "其他收入" : "其他支出";
                LambdaQueryWrapper<FinanceCategories> qw2 = new LambdaQueryWrapper<>();
                qw2.eq(FinanceCategories::getName, otherName);
                categoryId = categoriesMapper.selectOne(qw2).getId();
                realType = (type != null && type == 2) ? 2 : 1;
            }

            FinanceRecordsDto dto = new FinanceRecordsDto();
            dto.setCategoryId(categoryId);
            dto.setType(realType);
            dto.setAmount(amount);
            dto.setRemark(remark);
            dto.setRecordTime(LocalDateTime.now());   // AI记账默认记在当下（模型没有"补记过去"的场景）
            financeService.create(dto);
            return "已记一笔：" + (realType == 2 ? "收入" : "支出") + " " + amount + "元（" + categoryName + "）";
        }

        // 未匹配到：返回给模型，让它知道这个工具没实现（而不是静默失败）
        return "未知工具：" + toolName;
    }

}
