package com.liyuq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.liyuq.entity.Comment;


@org.apache.ibatis.annotations.Mapper
public interface CommentMapper extends Mapper<Comment>, BaseMapper<Comment> {

}
