package com.liyuq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyuq.entity.NoteTagRelations;
import org.apache.ibatis.annotations.Mapper;

// 泛型必须是实体类 NoteTagRelations（之前误写成了 Mapper 自己）
// 注意：note_tag_relations 表是联合主键（note_id + tag_id），没有单列 id，
// 所以本 Mapper 的 selectById/deleteById 等按主键操作的方法不可用，
// 操作这张表要用 QueryWrapper 按 note_id / tag_id 条件来写
@Mapper
public interface NoteTagRelationsMapper extends BaseMapper<NoteTagRelations> {

}
