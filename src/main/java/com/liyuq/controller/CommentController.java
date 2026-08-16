package com.liyuq.controller;

import com.liyuq.DTO.postCommentDto;
import com.liyuq.VO.CommentVo;
import com.liyuq.common.Result;
import com.liyuq.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class CommentController {

    @Autowired private CommentService commentService;

    // 3.39 发表评论：POST /api/post/{postId}/comment
    @PostMapping("/api/post/{postId}/comment")
    public Result<Map<String, Long>> postComment( @PathVariable Long postId, @RequestBody postCommentDto dto){
        Long postid= commentService.PostComment(postId,dto);

        return Result.success(Map.of("id",postid));
    }


    // 3.41 删除评论：DELETE /api/comment/{id}
    @DeleteMapping("/api/comment/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {

      commentService.deleteComment(id);
        return Result.success();
    }

    // 3.40 帖子评论列表：GET /api/post/comments/{postId}（游客可看，返回一个数组）
    @GetMapping("/api/post/comments/{postId}")
    public Result<List<CommentVo>> listComments(@PathVariable Long postId) {
        return Result.success(commentService.listComments(postId));
    }
}
