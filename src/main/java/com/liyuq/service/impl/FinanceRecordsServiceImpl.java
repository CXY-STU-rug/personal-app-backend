package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liyuq.DTO.FinanceRecordsDto;
import com.liyuq.DTO.ListRecordDto;
import com.liyuq.VO.CategoryBreakdownVo;   // 统计里"分类明细"每一项，兼职当手写SQL的接收容器
import com.liyuq.VO.FinanceRecordsVo;
import com.liyuq.VO.FinanceStatisticsVo;   // 统计外层VO
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.UserContext;
import com.liyuq.entity.FinanceCategories;
import com.liyuq.entity.FinanceRecords;
import com.liyuq.mapper.FinanceCategoriesMapper;
import com.liyuq.mapper.FinanceRecordsMapper;
import com.liyuq.service.FinanceRecordsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;   // ⚠️多余import，service层用不到这个web注解，可删

import java.math.BigDecimal;
import java.math.RoundingMode;   // BigDecimal除法的舍入模式（HALF_UP四舍五入）
import java.time.LocalDateTime;
import java.time.YearMonth;      // 把"2026-07"解析成年月、再推算月首月尾
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 记账记录表 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
@Service
public class FinanceRecordsServiceImpl extends ServiceImpl<FinanceRecordsMapper, FinanceRecords> implements FinanceRecordsService {

    @Autowired
    private FinanceCategoriesMapper financeCategoriesMapper;   // 查分类表：校验分类是否存在、补分类名
    @Autowired
    private FinanceRecordsMapper financeRecordsMapper;         // 查/增/改/删记账记录主表

    public Long create(FinanceRecordsDto financeRecordsDto) {
        FinanceCategories category = financeCategoriesMapper.selectById(financeRecordsDto.getCategoryId());   // 拿前端传的分类id去查分类
        if (category == null) {                              // 分类不存在 → 拦截
            throw new BusinessException(400, "创建的id的分类为空");
        }
        if (!category.getType().equals(financeRecordsDto.getType())) {   // 跨表校验：记录的type必须和分类的type一致
            throw new BusinessException(400, "类型与分类不匹配");   // 防"选了工资却记成支出"
        }
        Long userId = UserContext.getUserContextId();        // 属主从token取，绝不信前端传的userId（写入锁）
        FinanceRecords financeRecords = new FinanceRecords(); // 组装准备入库的实体
        financeRecords.setCreatedAt(LocalDateTime.now());     // 出生证明=此刻，只写这一次
        financeRecords.setCategoryId(financeRecordsDto.getCategoryId());
        financeRecords.setRecordTime(financeRecordsDto.getRecordTime());   // 发生时间用前端给的（可补记）
        financeRecords.setAmount(financeRecordsDto.getAmount());
        financeRecords.setType(financeRecordsDto.getType());
        financeRecords.setUserId(userId);                     // 盖上属主章
        financeRecords.setRemark(financeRecordsDto.getRemark());
        financeRecordsMapper.insert(financeRecords);          // 入库；AUTO主键会被MP回填进对象
        Long id = financeRecords.getId();                     // 取回填的新id
        return id;                                            // 返回给controller拼 {id:xx}
    }

    public List<FinanceRecordsVo> listFinanceRecords(ListRecordDto dto) {
        Long userId = UserContext.getUserContextId();         // 查询锁：只查自己的记录

        LambdaQueryWrapper<FinanceRecords> queryWrapper = new LambdaQueryWrapper<FinanceRecords>();   // 空条件起手
        queryWrapper.eq(FinanceRecords::getUserId, userId)    // 必带：user_id = 当前用户
                .eq(dto.getType() != null, FinanceRecords::getType, dto.getType())   // 选填：传了type才拼 type=?
                .ge(dto.getStartDate() != null, FinanceRecords::getRecordTime,
                        dto.getStartDate() == null ? null : dto.getStartDate().atStartOfDay())   // LocalDate→当天0点，和列的LocalDateTime类型对齐
                .lt(dto.getEndDate() != null, FinanceRecords::getRecordTime,
                        dto.getEndDate() == null ? null : dto.getEndDate().plusDays(1).atStartOfDay())   // 半开区间右边界：结束日的次日0点，含结束日整天。三元是为了防null时eager求值NPE
                .orderByDesc(FinanceRecords::getRecordTime);  // 按发生时间倒序，新的在上
        List<FinanceRecords> list = financeRecordsMapper.selectList(queryWrapper);   // 执行查询


        // 一次性把分类表查出来（就11条），做成 id→分类名 的字典，循环里查字典不查库（防N+1）
        Map<Integer, String> nameMap = new HashMap<>();   // 键类型必须和查字典的钥匙(getCategoryId的Integer)一致，否则get永远null
        for (FinanceCategories c : financeCategoriesMapper.selectList(null)) {      // selectList(null)=无条件查全表
            nameMap.put(c.getId(), c.getName());                                // 存入：键=分类id，值=分类名
        }
        List<FinanceRecordsVo> financeRecordsVoList = new ArrayList<>();   // 装转换后VO的袋子
        for (FinanceRecords financeRecords : list) {          // 逐条实体→VO

            FinanceRecordsVo financeRecordsVo = new FinanceRecordsVo();

            financeRecordsVo.setId(financeRecords.getId());
            financeRecordsVo.setCategoryId(financeRecords.getCategoryId());
            financeRecordsVo.setRecordTime(financeRecords.getRecordTime());
            financeRecordsVo.setAmount(financeRecords.getAmount());
            financeRecordsVo.setType(financeRecords.getType());
            financeRecordsVo.setRemark(financeRecords.getRemark());
            financeRecordsVo.setCategoryName(nameMap.get(financeRecords.getCategoryId()));   // 查字典补分类名（实体没这字段）
            financeRecordsVoList.add(financeRecordsVo);

        }
        return financeRecordsVoList;   // 返回整袋VO
    }
      public void updateFinanceRecord(Long id,FinanceRecordsDto financeRecordsDto) {
          FinanceRecords old = getOwnedRecord(id);   // 查不到/不是自己的，里面直接throw，走到这行old必不为null

          // final值 = dto传了用dto的，没传用库里旧的 → 算出"改完之后"的状态
          Integer finalCategoryId = financeRecordsDto.getCategoryId() != null
                  ? financeRecordsDto.getCategoryId() : old.getCategoryId();
          Integer finalType = financeRecordsDto.getType() != null
                  ? financeRecordsDto.getType() : old.getType();

          // 拿final值做和create一样的跨表校验：校验的是未来状态，不是输入本身
          FinanceCategories category = financeCategoriesMapper.selectById(finalCategoryId);
          if (category == null) {
              throw new BusinessException(400, "分类不存在");       // 改成了一个压根没有的分类id
          }
          if (!category.getType().equals(finalType)) {
              throw new BusinessException(400, "类型与分类不匹配");   // 改完之后类型和分类对不上
          }

    FinanceRecords financeRecords = new FinanceRecords();   // 组装要更新的字段（没设id以外只更新非null列）
      financeRecords.setId(id);                              // 指明改哪条
      financeRecords.setAmount(financeRecordsDto.getAmount());
      financeRecords.setType(finalType);                    // 用最终值而非dto原值
      financeRecords.setRemark(financeRecordsDto.getRemark());
      financeRecords.setCategoryId(finalCategoryId);        // 用最终值
      financeRecords.setRecordTime(financeRecordsDto.getRecordTime());
        financeRecordsMapper.updateById(financeRecords);    // 按id更新；MP默认跳过为null的字段不生成SET
        return;

}

    public void deleteFinanceRecord(Long id) {
        getOwnedRecord(id);                    // 只借它的归属锁：不是自己的/不存在的，里面直接throw拦下
        financeRecordsMapper.deleteById(id);   // 锁过了才真删
    }




    private FinanceRecords getOwnedRecord (Long id)   // 归属校验公共方法：查记录+验属主，fail-fast风格
       {
       FinanceRecords financeRecords = financeRecordsMapper.selectById(id);   // 先按id取记录
    if (financeRecords == null ) {
        throw new BusinessException(404, "记录不存在");   // 404 = 这条记录压根没有
    }
    if (!financeRecords.getUserId().equals(UserContext.getUserContextId())) {   // 属主对不上
throw new BusinessException(403,"无权操作");             // 403 = 记录有，但不是你的（归属锁）
    }
    return financeRecords;   // 校验通过，把实体给调用方（update要用它取旧值）
}




    public FinanceStatisticsVo listStatisticFinanceRecords(String month) {

        // 第1步：把"2026-07"翻译成时间范围 [7月1号0点, 8月1号0点)
        YearMonth ym = (month == null) ? YearMonth.now() : YearMonth.parse(month);   // 不传就用当前月
        LocalDateTime begin = ym.atDay(1).atStartOfDay();                 // 这个月1号 00:00
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();     // 下个月1号 00:00（半开区间右边界）

        Long userId = UserContext.getUserContextId();   // 查询锁：只统计自己的账
        List<CategoryBreakdownVo>list= financeRecordsMapper.selectFinanceStatistics( userId,begin,end);   // 一条聚合SQL：按分类分堆求和（此时categoryName/percentage还是null）
        BigDecimal totalIncome = BigDecimal.ZERO;    // 总收入，从0开始攒
        BigDecimal totalExpense = BigDecimal.ZERO;   // 总支出
        for (CategoryBreakdownVo vo : list) {        // 第3步：按type把各分类合计累加成两个总额
            if (vo.getType() == 1) {                              // Integer和int字面量比较会自动拆箱，这里安全
                totalExpense = totalExpense.add(vo.getAmount());  // BigDecimal不可变：add完必须接回去，光调用等于没加
            } else {
                totalIncome = totalIncome.add(vo.getAmount());    // type=2归入收入
            }
        }

        Map<Integer, String> nameMap = new HashMap<>();   // 第4步：建分类名字典（同list接口）
        for (FinanceCategories c : financeCategoriesMapper.selectList(null)) {
            nameMap.put(c.getId(), c.getName());
        }

        // 第5步：再遍历一次，补名字 + 算百分比
        for (CategoryBreakdownVo vo : list) {
            vo.setCategoryName(nameMap.get(vo.getCategoryId()));    // 查字典补名
            BigDecimal total = vo.getType() == 1 ? totalExpense : totalIncome;   // 分母：支出项除以总支出，收入项除以总收入
            if (total.compareTo(BigDecimal.ZERO) == 0) {            // 判零必须用compareTo（equals连0和0.00都嫌不一样）
                vo.setPercentage(BigDecimal.ZERO);                  // 总额为0没法除，占比记0
            } else {
                vo.setPercentage(vo.getAmount()
                        .multiply(new BigDecimal("100"))            // 先乘100变成百分数
                        .divide(total, 1, RoundingMode.HALF_UP));   // 除法必须说明：保留1位、四舍五入，否则除不尽直接抛异常
            }
        }
        // 第6步：装外层VO
        FinanceStatisticsVo result = new FinanceStatisticsVo();
        result.setTotalIncome(totalIncome);
        result.setTotalExpense(totalExpense);
        result.setBalance(totalIncome.subtract(totalExpense));   // 结余=收入-支出，减法用subtract
        result.setCategoryBreakdown(list);                       // 补好名字和百分比的list直接挂上
        return result;
    }
}
