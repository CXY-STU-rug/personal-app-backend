package com.liyuq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("posts")
public class Posts {

    @TableId(type = IdType.AUTO)
 private Long id;

    private  Long userId;

    private String title;

    private String content;

    private Boolean isPublic;

   private String videoUrl;

    // 字段名对齐数据库列 created_at / updated_at（和 notes/todos 等全项目统一）
    @JsonFormat(pattern ="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern ="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

}
