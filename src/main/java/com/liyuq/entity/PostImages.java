package com.liyuq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName(value = "post_images")
public class PostImages {
@TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private String imageUrl;

    private Integer sort;



}
