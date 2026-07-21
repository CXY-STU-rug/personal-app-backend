package com.liyuq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyuq.entity.FinanceCategories;
import org.apache.ibatis.annotations.Mapper;

// @Mapper 让 MyBatis 为这个接口生成实现并注册成 Spring Bean，
// 少了它，注入这个 Mapper 的地方启动时就会报 NoSuchBeanDefinitionException
@Mapper
public interface FinanceCategoriesMapper extends BaseMapper<FinanceCategories> {
}
