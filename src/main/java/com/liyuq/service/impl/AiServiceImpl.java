
package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyuq.DTO.AiChatDto;
import com.liyuq.VO.AiActionVo;
import com.liyuq.VO.AiChatVo;
import com.liyuq.VO.AiMessageVo;
import com.liyuq.VO.AiModelVo;
import com.liyuq.ai.AiToolExecutor;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.UserContext;
import com.liyuq.entity.AiMessages;
import com.liyuq.entity.FinanceCategories;
import com.liyuq.mapper.AiMessagesMapper;
import com.liyuq.mapper.FinanceCategoriesMapper;
import com.liyuq.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI助手业务实现（契约3.28对话 / 3.29拉历史 / 3.30清空）
 */
@Slf4j     // 造一个叫log的日志器：第三方异常转译前先留真相（天气模块的老教训）
@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private AiMessagesMapper aiMessagesMapper;
    @Autowired
    private RestTemplate aiRestTemplate;

    @Value("${ai.api-url}")   // 按yml的键名逐字符对暗号：ai.api-url
    private String aiUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AiToolExecutor aiToolExecutor;
    @Autowired
    private FinanceCategoriesMapper categoriesMapper;   // system prompt要把分类名列表喂给模型

    // model参数白名单：只有这两个id允许透传给DeepSeek。前端传别的（或想指定任意模型名绕过成本控制）→
    // 一律回落默认。和"userId只从token取"同一条思想：前端给的一切都要校验，不可信。
    private static final Set<String> ALLOWED_MODELS = Set.of("deepseek-chat", "deepseek-reasoner");
    private static final String DEFAULT_MODEL = "deepseek-chat";   // 3.31里isDefault=true那个

    /**
     * 工具清单（发给DeepSeek，告诉它有哪些函数可调）。3a先只挂create_todo一个。
     * description是写给"模型"看的说明书——它靠这段文字判断何时该调这个工具、参数怎么填。
     * 这段是死格式，直接写成常量JSON串，比用Map.of嵌套三层可读得多；用时readTree成对象塞进body。
     */
    private static final String TOOLS_JSON = """
            [{
              "type": "function",
              "function": {
                "name": "create_todo",
                "description": "为用户创建一条待办任务。当用户说要记待办、加提醒、要做某件事时调用",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "title":    { "type": "string",  "description": "任务标题" },
                    "deadline": { "type": "string",  "description": "截止时间，格式yyyy-MM-dd HH:mm:ss，没有就不传" },
                    "priority": { "type": "integer", "description": "优先级：1高2中3低，不确定就不传" }
                  },
                  "required": ["title"]
                }
              }
            },
            {
              "type": "function",
              "function": {
                "name": "get_finance_statistics",
                "description": "查询用户某个月的收支统计（总收入、总支出、结余、各分类占比）。当用户问'这个月花了多少''某月收支'时调用",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "month": { "type": "string", "description": "月份，格式yyyy-MM，如2026-07；不传则查当月" }
                  },
                  "required": []
                }
              }
            },
            {
              "type": "function",
              "function": {
                "name": "search_notes",
                "description": "按关键字搜索用户的笔记（标题和正文）。当用户问'我之前记过xxx吗''找一下关于xxx的笔记'时调用",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "keyword": { "type": "string", "description": "搜索关键字" }
                  },
                  "required": ["keyword"]
                }
              }
            },
            {
              "type": "function",
              "function": {
                "name": "query_todos",
                "description": "查询用户的待办任务。当用户问'我今天/接下来有什么要做''这周没完成的''我的待办'时调用",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "scope": {
                      "type": "string",
                      "enum": ["today", "upcoming", "completed", "all"],
                      "description": "范围：today=今天截止未完成，upcoming=未来未完成，completed=已完成，all=全部"
                    }
                  },
                  "required": ["scope"]
                }
              }
            },
            {
              "type": "function",
              "function": {
                "name": "query_calendar",
                "description": "查询用户某一天的日程安排和当天截止的待办。当用户问'我明天有什么安排''X月X号有什么日程'时调用",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "date": { "type": "string", "description": "日期，格式yyyy-MM-dd，如2026-07-18" }
                  },
                  "required": ["date"]
                }
              }
            },
            {
              "type": "function",
              "function": {
                "name": "create_finance_record",
                "description": "为用户记一笔账。当用户说'记一笔''花了xx元''收入xx'时调用",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "type":         { "type": "integer", "description": "1=支出 2=收入" },
                    "categoryName": { "type": "string",  "description": "分类名，如餐饮/交通/工资；不确定就填最接近的" },
                    "amount":       { "type": "number",  "description": "金额，必须大于0" },
                    "remark":       { "type": "string",  "description": "备注，可不传" }
                  },
                  "required": ["type", "categoryName", "amount"]
                }
              }
            }]
            """;

    @Override
    public List<AiMessageVo> history(Integer limit) {
        // ① limit收敛到1~100：不传/非法→默认20，超过100按100算（也顺便保证了下面拼接的安全）
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        if (limit > 100) {
            limit = 100;
        }

        // ② 查"最近的limit条"：按当前用户过滤 + id倒序（倒序才是"最近的"）
        //    last()是裸拼SQL没有参数绑定，所以#{}在这不工作；
        //    只准拼数字——limit是Integer且已收敛，注入不进来
        LambdaQueryWrapper<AiMessages> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiMessages::getUserId, UserContext.getUserContextId())
                .orderByDesc(AiMessages::getId)
                .last("LIMIT " + limit);
        List<AiMessages> list = aiMessagesMapper.selectList(queryWrapper);

        // ③ 翻转：查出来是新→旧，聊天窗要旧→新（合同3.29：最新的在数组末尾）
        Collections.reverse(list);

        // ④ 实体转VO：只拷页面要的4个字段，userId不外传（VO存在的意义）
        List<AiMessageVo> voList = new ArrayList<>();
        for (AiMessages aiMessages : list) {
            AiMessageVo aiMessageVo = new AiMessageVo();
            aiMessageVo.setId(aiMessages.getId());
            aiMessageVo.setRole(aiMessages.getRole());
            aiMessageVo.setContent(aiMessages.getContent());
            aiMessageVo.setCreatedAt(aiMessages.getCreatedAt());
            voList.add(aiMessageVo);
        }
        return voList;
    }

    /**
     * 构造system prompt：每次对话开头喂给模型的"最高指令"，含两样必须动态填充的信息——
     * ① 今天的日期（否则模型不知道"明天/这周"是哪天，会瞎猜）；
     * ② 可用的记账分类列表（否则模型报的分类名和库里对不上）。
     * 还立了一条硬规矩：涉及数据操作必须真调工具，堵住"假装完成"的偷懒。
     */
    private String buildSystemPrompt() {
        // ① 今天日期：LocalDate.now()每次现算（不能提成常量，否则过了午夜就是错的）
        String today = LocalDate.now().toString();   // 形如"2026-07-18"

        // ② 查所有记账分类，名字拼成"餐饮、交通、购物…"喂给模型
        //    stream+map+joining：把实体列表里每个的name取出来、用顿号连成一个字符串
        String categories = categoriesMapper.selectList(null).stream()
                .map(FinanceCategories::getName)
                .collect(Collectors.joining("、"));

        // 用文本块拼prompt，%s占位由formatted填充。定位写宽（"通用助手"），别写窄成"记账助手"，
        // 否则模型会拒绝聊别的——助手多广是prompt说了算
        return """
                你是用户的个人AI助手，可以陪TA聊任何话题、回答问题、帮TA写东西；
                同时你能操作TA的待办、记账、笔记、日历数据。
                今天是 %s，用户说"今天/明天/这周"时以此为准计算日期。
                重要规矩：涉及用户数据的增删改查，必须真实调用对应的工具，
                绝不允许假装完成或编造结果——没调工具就不要说"已帮你记好/已创建"。
                记账时可用的分类有：%s。请从中挑最贴切的，选不出就归"其他支出/其他收入"。
                """.formatted(today, categories);
    }

    @Override
    public List<AiModelVo> models() {
        // 契约3.31：模型清单写死在后端（是"部署时定的运行配置"，不是会变的用户数据，不建表）。
        // 要新增模型改这里、重启即可，前端零改动。id同时是3.28请求里model参数的白名单合法值。
        AiModelVo chat = new AiModelVo();
        chat.setId("deepseek-chat");
        chat.setName("通用对话");
        chat.setDescription("响应快，适合日常聊天、记账、建待办");
        chat.setIsDefault(true);          // 整个列表有且仅有一个true

        AiModelVo reasoner = new AiModelVo();
        reasoner.setId("deepseek-reasoner");
        reasoner.setName("深度思考");
        reasoner.setDescription("推理更强、回答更慢，适合分析和规划");
        reasoner.setIsDefault(false);

        return List.of(chat, reasoner);
    }

    @Override
    public void clearHistory() {
        // 删除条件只有一个也只能有一个：user_id=当前用户——只清自己的对话（合同3.30）
        LambdaQueryWrapper<AiMessages> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiMessages::getUserId, UserContext.getUserContextId());
        aiMessagesMapper.delete(queryWrapper);
    }

    @Override
    public AiChatVo chat(AiChatDto dto) {
        // ========== ① 开门校验（合同3.28：content必填，1~2000字）==========
        // getContent()==null：前端连这个字段都没传；isBlank()：传了但全是空格
        // 两个条件用 || 连起来，任一成立就是"空消息"，拦下返回422（参数错，不是服务器错）
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BusinessException(422, "消息不能为空");
        }
        // 边界用">2000"而不是">=2000"：恰好2000字是合法的，第2001字才算超（差一位的坑要抠清）
        if (dto.getContent().length() > 2000) {
            throw new BusinessException(422, "消息最长2000字");
        }

        // 当前登录用户ID：从token解析的ThreadLocal取，绝不信前端传参（防越权铁律）
        // 下面存两条消息、查历史都要用它，取一次存局部变量复用
        Long userId = UserContext.getUserContextId();

        // ========== ② 先存"用户这句话"入库 ==========
        // 顺序讲究：先存用户消息，再调模型。放前面是因为——就算等下DeepSeek挂了，
        // 用户说过的话也已经落库、不会丢（合同流程：存用户→调模型→存AI回复）
        AiMessages userMsg = new AiMessages();          // new一个空实体
        userMsg.setUserId(userId);                      // 归属：这条属于当前用户
        userMsg.setRole("user");                        // 角色写死user（和DeepSeek的role同名同值）
        userMsg.setContent(dto.getContent());           // 正文=用户输入
        userMsg.setCreatedAt(LocalDateTime.now());      // 创建时间=现在（NOT NULL列，不set会SQL报错）
        aiMessagesMapper.insert(userMsg);               // 落库，自增id会回填进userMsg

        // try包住"调第三方"的全过程：任何意外都在下面catch里统一转502，不让它裸奔成500
        try {
            // ========== ③ 组装请求体body（要发给DeepSeek的JSON）==========
            Map<String, Object> body = new HashMap<>();  // body是个Map，等下Jackson把它序列化成JSON

            // 查"最近10条历史"当上下文：条件=当前用户 + id倒序 + 只取10条
            LambdaQueryWrapper<AiMessages> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AiMessages::getUserId, userId)  // 数据隔离：只查自己的
                    .orderByDesc(AiMessages::getId)         // id倒序=最新的在前
                    .last("LIMIT 10");                      // 只要最近10条，控制token成本
            List<AiMessages> recent = aiMessagesMapper.selectList(queryWrapper);

            // 查出来是"新→旧"，但模型读剧本要"旧→新"，翻转过来
            Collections.reverse(recent);

            // 历史实体列表 → 消息列表。类型是List<Object>不是List<Map>——因为循环里回禀时，
            // 要往这个列表塞"模型返回的原始JsonNode"，Map和JsonNode都是Object才装得下两种
            List<Object> messages = new ArrayList<>();
            // 第0条：system prompt——给模型立规矩+喂动态信息，放messages最前面，模型当最高优先级指令
            messages.add(Map.of("role", "system", "content", buildSystemPrompt()));
            for (AiMessages m : recent) {
                // 历史都是简单的user/assistant消息，两字段Map就够（数据库只存这两种role）
                messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
            }

            // model白名单校验：在名单内就用用户选的，否则（null/空/乱传）回落默认——绝不透传前端字符串
            String model = (dto.getModel() != null && ALLOWED_MODELS.contains(dto.getModel()))
                    ? dto.getModel()
                    : DEFAULT_MODEL;

            // model和tools是固定的，循环外put一次就行；messages每圈会变长，放进循环里put
            body.put("model", model);                              // 用校验后的模型
            body.put("tools", objectMapper.readTree(TOOLS_JSON));   // 挂上工具清单，模型才知道有哪些函数能调

            // ========== 请求头：认证 + 内容类型 ==========
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);                        // 自动拼成 Authorization: Bearer sk-xxx
            headers.setContentType(MediaType.APPLICATION_JSON);   // 声明"我发的是JSON"

            // ========== ④ function calling核心循环（最多3圈）==========
            // 每圈：POST问模型 → 看它要不要调工具 → 要就执行+把结果回禀 → 再问；
            // 直到模型不再要工具、给出最终文字就break。上限3圈，防它无限调工具烧钱。
            String reply = "";                              // 循环外声明：循环里赋值，循环后存库+返回都要用它
            List<AiActionVo> actions = new ArrayList<>();    // 收集本次AI真正执行的写操作，纯聊天时保持空数组

            for (int round = 0; round < 3; round++) {        // round从0到2，最多转3圈
                body.put("messages", messages);              // 每圈都用"当前"的messages（回禀后它会变长）

                // 发出请求，拿回响应JSON字符串。整个方法只有这一处POST，循环带着它转N遍
                String json = aiRestTemplate.exchange(
                        aiUrl,                               // DeepSeek的对话接口地址
                        HttpMethod.POST,                     // POST：这次要往body里塞货
                        new HttpEntity<>(body, headers),     // 信封=货(body)+头(headers)
                        String.class                         // 响应按String原样接住，我们自己解析
                ).getBody();

                // 从响应里挖出"模型这一圈回的整条消息"：路径 choices[0].message
                JsonNode message = objectMapper.readTree(json).path("choices").path(0).path("message");
                // 再看这条消息里有没有tool_calls（要不要调工具）；取数组第0个
                JsonNode toolCall = message.path("tool_calls").path(0);

                // —— 分叉A：没有tool_calls（幽灵节点）= 模型给出最终答案了 ——
                if (toolCall.isMissingNode()) {              // 判断"挖没挖到"必须用isMissingNode，不能用==null
                    reply = message.path("content").asText();  // 最终回复就是content那段文字
                    break;                                   // 活干完了，跳出循环
                }

                // —— 分叉B：有tool_calls = 模型要使唤你调工具 ——
                // 挖工具名：tool_calls[0].function.name
                String toolName = toolCall.path("function").path("name").asText();
                // 挖参数：arguments是"字符串套JSON"，先asText拿到那串字符串，再readTree解析成JsonNode
                JsonNode args = objectMapper.readTree(
                        toolCall.path("function").path("arguments").asText());
                // 交给执行器真正干活（它复用你的service写库），拿回一句人话结果
                String result = aiToolExecutor.execute(toolName, args);

                // 把这次动作记进actions（前端靠它提示"AI替你改了数据"）
                AiActionVo action = new AiActionVo();
                action.setType(toolName);                    // 干了什么（工具名）
                action.setDetail(result);                    // 结果详情
                actions.add(action);

                // —— 回禀：往messages追加两条，下一圈带着它们再问模型 ——
                // 第1条：把模型那条"使唤"原样塞回去（JsonNode，DeepSeek要求tool结果前必须有对应的assistant消息）
                messages.add(message);
                // 第2条：执行结果，role=tool。tool_call_id必须带，模型靠它对上"这是哪次使唤的结果"
                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", toolCall.path("id").asText(),
                        "content", result));
                // 这里不break——自然进入下一圈。下圈messages里带着执行结果，模型看到后就会给最终文字
            }

            // 循环兜底：3圈都用完还没等到文字（模型死犟一直要工具），给句人话，别返回空reply
            if (reply.isBlank()) {
                reply = "抱歉，这个操作有点复杂，我没能完成，你可以换个说法再试试。";
            }

            // ========== ⑤ 存"AI的最终回复"入库 ==========
            // 只存最终这句assistant回复；循环中间的tool_calls/tool消息只活在内存messages里，不进库
            // （数据库ai_messages只存"给用户看的对话"，不存内部的工具调用细节）
            AiMessages aiMsg = new AiMessages();
            aiMsg.setUserId(userId);
            aiMsg.setRole("assistant");                     // 这条永远是AI说的
            aiMsg.setContent(reply);
            aiMsg.setCreatedAt(LocalDateTime.now());
            aiMessagesMapper.insert(aiMsg);

            // ========== ⑥ 装VO返回 ==========
            AiChatVo aiChatVo = new AiChatVo();
            aiChatVo.setReply(reply);                        // 最终回复文字
            aiChatVo.setActions(actions);                    // 动作列表（空数组也行，就是别给null——前端遍历null会崩）
            return aiChatVo;

        } catch (BusinessException e) {
            // 自己抛的422/502原样放行，别被下面的兜底catch误当成"意外"改判成502
            throw e;
        } catch (Exception e) {
            // 超时、余额不足、返回格式变了……一切没预料到的意外，统一转502
            // 转译前先log.error留真相：给前端体面话术，给自己完整堆栈（gzip那次就吃过没日志的亏）
            log.error("调用DeepSeek失败", e);
            throw new BusinessException(502, "AI服务暂时不可用");
        }
    }




}
