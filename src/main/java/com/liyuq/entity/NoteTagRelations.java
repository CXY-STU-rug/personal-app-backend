package com.liyuq.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("note_tag_relations")     // 多对多的"中间表/关联表"：一行=一条"笔记↔标签"的配对
public class NoteTagRelations {
    private Long noteId;             // 哪条笔记
    private Integer tagId;           // 连着哪个标签
    // 没有自增id：主键是 (noteId, tagId) 两列组合，所以删除只能按条件删，不能 deleteById
}
