package com.liyuq.service;

import com.liyuq.VO.FinanceCategoriesVo;
import com.liyuq.entity.FinanceCategories;
import com.baomidou.mybatisplus.extension.service.IService;   // MP的service顶层接口，白送一批CRUD方法声明

import java.util.List;

/**
 * <p>
 * 记账分类表（系统预置数据，不属于某个用户） 服务类
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */

public interface FinanceCategoriesService extends IService<FinanceCategories> {   // 继承IService拿到通用CRUD，再加自己的业务方法

   List<FinanceCategoriesVo> listcategories(Integer id);   // 查分类列表（参数是筛选用的type，形参名写成id但含义是type）
}
