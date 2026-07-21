package com.liyuq.service;

import com.liyuq.DTO.FinanceRecordsDto;
import com.liyuq.DTO.ListRecordDto;
import com.liyuq.VO.FinanceRecordsVo;
import com.liyuq.VO.FinanceStatisticsVo;
import com.liyuq.entity.FinanceRecords;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 记账记录表 服务类（对外承诺的业务方法清单，具体实现在 FinanceRecordsServiceImpl）
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
public interface FinanceRecordsService extends IService<FinanceRecords> {   // 继承IService白拿一批通用CRUD，再加下面自定义的业务方法

    Long create(FinanceRecordsDto financeRecordsDto);   // 创建记录，返回新生成的id


    List<FinanceRecordsVo> listFinanceRecords(ListRecordDto dto);   // 按筛选条件查当前用户的记录列表

    void updateFinanceRecord(Long id, FinanceRecordsDto financeRecordsDto);   // 更新指定记录（内部会做归属+跨表校验）

    void deleteFinanceRecord(Long id);   // 删除只需要知道删谁，一个id就够

    FinanceStatisticsVo listStatisticFinanceRecords(String month);   // 月度收支统计，month格式YYYY-MM
}
