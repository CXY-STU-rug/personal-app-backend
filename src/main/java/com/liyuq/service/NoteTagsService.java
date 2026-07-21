package com.liyuq.service;

import com.liyuq.VO.NoteTagsVo;
import com.liyuq.entity.NoteTags;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 绗旇?鏍囩?琛 服务类
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
public interface NoteTagsService extends IService<NoteTags> {

    // 3.18 获取当前用户的所有标签，返回给前端做筛选下拉框
    List<NoteTagsVo> listTags();
}
