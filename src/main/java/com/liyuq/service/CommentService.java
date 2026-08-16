package com.liyuq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liyuq.DTO.postCommentDto;
import com.liyuq.VO.CommentVo;
import com.liyuq.entity.Comment;

import java.util.List;

public interface CommentService extends IService<Comment> {
    Long PostComment(Long id, postCommentDto dto);

    void deleteComment(Long id);

    // 3.40 查某帖的评论列表（按时间倒序，带评论人昵称）
    List<CommentVo> listComments(Long postId);
}
