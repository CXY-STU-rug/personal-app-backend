package com.liyuq.service;

import com.liyuq.VO.DashboardVo;

/**
 * 首页仪表盘服务。
 * 注意：它不对应任何数据库表，所以不 extends IService<实体>——
 * 别的service继承IService是为了白拿单表CRUD，而dashboard自己不碰表，
 * 只是把todo/finance/note三个service叫过来聚合，所以是个普通接口。
 */
public interface DashboardService {

    // 聚合首页要的三块数据，打包成一个 DashboardVo 返回
    DashboardVo getOverview();
}
