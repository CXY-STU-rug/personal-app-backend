package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.liyuq.VO.NoteTagsVo;
import com.liyuq.common.UserContext;
import com.liyuq.entity.NoteTags;
import com.liyuq.mapper.NoteTagsMapper;
import com.liyuq.service.NoteTagsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 标签表 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
@Service
public class NoteTagsServiceImpl extends ServiceImpl<NoteTagsMapper, NoteTags> implements NoteTagsService {

    @Autowired
    private NoteTagsMapper noteTagsMapper;   // 操作 note_tags 表

    @Override
    public List<NoteTagsVo> listTags() {
        Long userId = UserContext.getUserContextId();          // 当前登录用户（token解析来的，不信前端）

        // 只查"我自己的"标签：where user_id = 当前用户
        LambdaQueryWrapper<NoteTags> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(NoteTags::getUserId, userId);
        List<NoteTags> tags = noteTagsMapper.selectList(wrapper);   // 查回该用户全部标签实体

        // 实体 → VO：只挑 id/name 两个字段，userId 不给前端
        List<NoteTagsVo> voList = new ArrayList<>();
        for (NoteTags tag : tags) {              // 逐个转换
            NoteTagsVo vo = new NoteTagsVo();
            vo.setId(tag.getId());
            vo.setName(tag.getName());
            voList.add(vo);
        }
        return voList;                           // 返回纯数组，外层 Result 由 controller 包
    }
}
