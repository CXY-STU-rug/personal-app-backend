package com.liyuq.controller;

import com.liyuq.VO.FinanceCategoriesVo;       // 返回给前端的分类"成品"，不是数据库实体
import com.liyuq.common.Result;                // 统一响应外壳：{code, msg, data}
import com.liyuq.service.FinanceCategoriesService;   // 业务接口，controller只认接口不认实现（面向接口编程）
import org.springframework.beans.factory.annotation.Autowired;   // 让Spring自动把Bean塞进字段
import org.springframework.web.bind.annotation.*;   // 一次导入 @RestController/@GetMapping/@RequestParam 等一整包

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 记账分类表（系统预置数据，不属于某个用户） 前端控制器
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
@RestController                       // = @Controller + @ResponseBody：这个类每个方法的返回值都自动转成JSON写回响应体
@RequestMapping("/api/finance")       // 类级别的路径前缀，下面所有方法的路径都拼在这后面
public class FinanceCategoriesController {
      @Autowired
    private FinanceCategoriesService financeCategoriesService;   // 注入业务层，具体实现由Spring在启动时找到并塞进来


    @GetMapping("/categories")        // 完整路径 = /api/finance/categories，GET=查询
    public Result<List<FinanceCategoriesVo>> categories(@RequestParam(required = false) Integer type) {   // type选填：不传查全部，传了按类型筛
       List<FinanceCategoriesVo> list =  financeCategoriesService.listcategories(type);   // 把活派给service，controller只负责收发不做业务
        return Result.success(list);  // 用200成功外壳包住list返回；前端拿到的就是 {code:200, msg:"成功", data:[...]}
    }
}
