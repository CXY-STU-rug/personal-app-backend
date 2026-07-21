package com.liyuq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyuq.VO.CategoryBreakdownVo;
import com.liyuq.entity.FinanceRecords;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

// 注意继承的是 BaseMapper（带全套CRUD方法），不是那个空的标记接口 Mapper——
// 两者在自动补全里长得像，选错就会报"getBaseMapper()返回类型不兼容"
@org.apache.ibatis.annotations.Mapper
public interface FinanceRecordsMapper extends BaseMapper<FinanceRecords> {

   // 手写聚合SQL：Wrapper不好表达SUM+GROUP BY，直接写原生SQL
   //   category_id, type    → 分组维度，也是SELECT要带出的列
   //   SUM(amount) AS amount → 每组金额合计，AS改成驼峰才能对上VO的amount字段
   //   WHERE user_id=#{}     → 查询锁，只统计自己的账（#{}是MyBatis占位符，防注入）
   //   record_time >= begin AND < end → 半开区间，精确框住某个月
   //   GROUP BY category_id,type → SELECT里的非聚合列必须都出现在这，否则SQL报错
   @Select("select category_id, type,SUM(amount)AS amount FROM finance_records WHERE user_id=#{userId} AND record_time >= #{begin} AND record_time<#{end} GROUP BY category_id,type")
   List<CategoryBreakdownVo> selectFinanceStatistics (Long userId, LocalDateTime begin, LocalDateTime end);   // 结果每行=一个分类的合计，categoryName/percentage留空由service补


}
