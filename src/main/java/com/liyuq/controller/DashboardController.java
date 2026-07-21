package com.liyuq.controller;

import com.liyuq.VO.DashboardVo;
import com.liyuq.common.Result;
import com.liyuq.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页仪表盘控制器（PRD V1 第5块）。
 * 只有一个只读接口：把今日待办/本月收支/最近笔记聚合成一份返回。
 */
@RestController                        // REST控制器，返回值自动转JSON
@RequestMapping("/api/dashboard")      // 类级前缀，下面方法都以 /api/dashboard 开头
public class DashboardController {

    @Autowired                         // 注入仪表盘service
    private DashboardService dashboardService;

    // GET /api/dashboard/overview：前端首页一进来就调这一个，拿全部三块数据
    @GetMapping("/overview")
    public Result<DashboardVo> overview() {
        DashboardVo vo = dashboardService.getOverview();   // 交给service聚合
        return Result.success(vo);                         // 包进统一返回体
    }
}
