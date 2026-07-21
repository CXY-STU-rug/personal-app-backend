package com.liyuq.controller;

import com.liyuq.VO.NoteTagsVo;
import com.liyuq.common.Result;
import com.liyuq.service.NoteTagsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 标签表 前端控制器
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
@RestController
@RequestMapping("/api/note")          // 和笔记接口同前缀，符合文档 /api/note/tags
public class NoteTagsController {

    @Autowired
    private NoteTagsService noteTagsService;   // 注入标签service

    // GET /api/note/tags —— 获取当前用户所有标签（筛选下拉框用）
    @GetMapping("/tags")
    public Result<List<NoteTagsVo>> listTags() {
        List<NoteTagsVo> tags = noteTagsService.listTags();   // 调service查
        return Result.success(tags);                          // 包进统一返回体
    }
}
