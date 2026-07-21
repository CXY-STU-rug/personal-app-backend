package com.liyuq.controller;

import com.liyuq.DTO.FinanceRecordsDto;   // 创建/更新的"进门登记表"
import com.liyuq.DTO.ListRecordDto;       // 列表查询的三个筛选参数打包
import com.liyuq.VO.FinanceRecordsVo;     // 列表返回的"出门衣服"
import com.liyuq.VO.FinanceStatisticsVo;  // 统计返回的外层VO（套着明细数组）
import com.liyuq.common.Result;
import com.liyuq.service.FinanceRecordsService;
import org.apache.coyote.Request;   // ⚠️多余的import（自动补全误伤），全文没用到，可删
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 记账记录表 前端控制器
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
@RestController
@RequestMapping("/api/finance")   // 和分类controller共用前缀；两个类挂同一前缀是允许的，路径不冲突即可
public class FinanceRecordsController {

    @Autowired
private  FinanceRecordsService financeRecordsService;   // 注入记账业务层

    @PostMapping("/create")   // POST=造新的 → /api/finance/create
public Result<Map> createFinanceRecord(@RequestBody FinanceRecordsDto financeRecordsDto) {   // @RequestBody：把请求体JSON反序列化成DTO

       Long id= financeRecordsService.create(financeRecordsDto);   // service完成校验+入库，返回新记录id

    return Result.success(Map.of("id", id));   // 文档要求 data:{id:xx}，用Map.of包一层键值对
}
    @GetMapping("/list")   // GET=查询 → /api/finance/list
    public Result<List<FinanceRecordsVo>> listFinanceRecords(ListRecordDto dto) {   // query参数自动绑定到DTO字段（无需@RequestBody）

          List <FinanceRecordsVo>list=financeRecordsService.listFinanceRecords(dto);   // 按筛选条件查当前用户的记录
          return Result.success(list);   // 成功外壳包住列表

}

    @PutMapping("/update/{id}")   // PUT=改旧的；{id}占位符标出要改哪条
public Result<Void>updateFinanceRecord(@PathVariable Long id, @RequestBody FinanceRecordsDto financeRecordsDto){   // @PathVariable取路径里的id，@RequestBody取新数据
        financeRecordsService.updateFinanceRecord(id,financeRecordsDto);   // service内部做归属校验+跨表校验+更新
return Result.success();   // 文档要求data:null，用无参success
    }

    @DeleteMapping("/delete/{id}")                          // DELETE动词=删东西，和PUT改、POST造一个家族
    public Result<Void> deleteFinanceRecord(@PathVariable Long id) {   // 路径里的{id}和@PathVariable永远成对出现
        financeRecordsService.deleteFinanceRecord(id);      // service内部先锁归属再删
        return Result.success();                            // 文档要求 data: null，无参success正好
    }

    @GetMapping("/statistics")   // GET → /api/finance/statistics，月度收支统计
    public Result<FinanceStatisticsVo> statisticsFinanceRecords(@RequestParam(required = false) String month) {   // month选填(YYYY-MM)，不传默认当月

     FinanceStatisticsVo financeStatisticsVo= financeRecordsService.listStatisticFinanceRecords(month);   // service完成聚合查询+百分比计算
     return  Result.success(financeStatisticsVo);   // 返回外层VO：三个总额 + 分类明细数组

    }
}
