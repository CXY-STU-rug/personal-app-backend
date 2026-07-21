package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;   // MyBatis-Plus的条件构造器，用Lambda拼WHERE，方法名写错编译期就报
import com.liyuq.VO.FinanceCategoriesVo;
import com.liyuq.common.Exception.BusinessException;   // 自定义业务异常，带自定义状态码，被全局处理器捕获转成JSON
import com.liyuq.common.UserContext;                   // 从ThreadLocal里取当前登录用户id的工具（token解析后存进去的）
import com.liyuq.entity.FinanceCategories;
import com.liyuq.mapper.FinanceCategoriesMapper;
import com.liyuq.service.FinanceCategoriesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;   // MP提供的service基类，白送一批CRUD方法
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;         // 标注这是业务层Bean，Spring扫描后纳入容器

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 记账分类表（系统预置数据，不属于某个用户） 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
@Service   // 注册成Spring Bean，controller里@Autowired才能拿到它
public class FinanceCategoriesServiceImpl extends ServiceImpl<FinanceCategoriesMapper, FinanceCategories> implements FinanceCategoriesService {
    // extends ServiceImpl<Mapper, 实体>：继承一堆现成CRUD；implements 接口：对外承诺提供listcategories

    @Autowired
    private  FinanceCategoriesMapper financeCategoriesMapper;   // 直接操作分类表的Mapper，下面用它查库
    @Autowired
    private UserContext userContext;   // ⚠️注意：这个注入其实没用到——下面取用户id走的是静态方法UserContext.getUserContextId()，这行是死代码

    public List<FinanceCategoriesVo> listcategories(Integer type) {
          Long userId =UserContext.getUserContextId();   // ⚠️死代码：分类是全局共享数据，查询不按用户过滤，这个userId拿了却从没用到
        if (type != null) {                               // 传了type才校验，没传（null）跳过——不传代表"查全部"
            if (type != 2 && type != 1) {                 // 只放行1和2，其它值(0、3...)视为非法参数
                throw new BusinessException(400,"类型参数非法，仅支持1(支出)、2(收入)");   // 文案和真实口径对齐：1支出/2收入
            }
        }
        LambdaQueryWrapper<FinanceCategories> queryWrapper = new LambdaQueryWrapper<>();   // 新建一个空条件，准备往里加WHERE
       queryWrapper.eq(type != null, FinanceCategories::getType, type)   // 布尔重载：第一个参数为true才生成 type=? 条件；null时整条不拼，避免 WHERE type=null 匹配不到任何行
       ;
       List<FinanceCategories>list =financeCategoriesMapper.selectList(queryWrapper);   // 按条件查库，拿到一批实体
        List<FinanceCategoriesVo> financeCategoriesListVo =new ArrayList<>();   // 准备一个空袋子装转换后的VO
    for ( FinanceCategories financeCategories: list){          // 遍历每个实体，逐个搬进VO（实体不直接给前端）
        FinanceCategoriesVo financeCategoriesVo = new FinanceCategoriesVo();   // 每轮新建一个VO对象
        financeCategoriesVo.setId(financeCategories.getId());          // 搬id
        financeCategoriesVo.setName(financeCategories.getName());      // 搬分类名
        financeCategoriesVo.setIcon(financeCategories.getIcon());      // 搬图标
        financeCategoriesVo.setType(financeCategories.getType());      // 搬类型
        financeCategoriesListVo.add(financeCategoriesVo);             // 装进袋子
    }
    return financeCategoriesListVo;   // 把整袋VO返回给controller


    }

}
