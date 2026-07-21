package com.liyuq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyuq.entity.NoteTags;
import org.apache.ibatis.annotations.Mapper;

// 命名规则：实体类名 + Mapper（实体叫 NoteTags，所以是 NoteTagsMapper，之前多打了个 s）
@Mapper
public interface NoteTagsMapper extends BaseMapper<NoteTags> {

}
