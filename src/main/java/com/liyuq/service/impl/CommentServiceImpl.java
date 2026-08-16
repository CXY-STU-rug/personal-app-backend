package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyuq.DTO.postCommentDto;
import com.liyuq.VO.CommentVo;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.UserContext;
import com.liyuq.entity.Comment;
import com.liyuq.entity.Posts;
import com.liyuq.entity.Users;
import com.liyuq.mapper.CommentMapper;
import com.liyuq.mapper.PostMapper;
import com.liyuq.mapper.UsersMapper;
import com.liyuq.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private UsersMapper usersMapper;   // 查评论人昵称要用

    @Override
    public Long PostComment(Long PostId, postCommentDto dto) {
        Long userId = UserContext.getUserContextId();
        // 不能只判"帖子存在"就放行：别人的私密帖也存在，但轮不到你来评论。
        // getVisiblePost 里连可见性一起判了，私密帖对非作者直接 404
        getVisiblePost(PostId);

        Comment comment = new Comment();
       comment.setPostId(PostId);
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);

        return comment.getId();
    }

    @Override
    public void deleteComment(Long id) {

        KnowUserComment(id);
        commentMapper.deleteById(id);
        return;
    }

    // 3.40 查某帖的评论列表：按时间倒序 + 每条带评论人昵称
    @Override
    public List<CommentVo> listComments(Long postId) {
        // 1) 校验帖子存在【且当前访问者看得到】。
        //    这个接口在 SecurityConfig 白名单里对游客开放，只判存在的话，
        //    随便猜个 postId 就能读到别人私密帖下的全部评论内容
        Posts post = getVisiblePost(postId);

        // 2) 查这条帖的所有评论，按创建时间倒序（最新的排最前）
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId)
                .orderByDesc(Comment::getCreatedAt);
        List<Comment> comments = commentMapper.selectList(wrapper);

        // 没有评论就直接返回空列表，省掉后面查用户这一步
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }

        // 3) 批量查昵称（避免 N+1：不在循环里一条条查用户）
        //    先把评论里的 userId 收集去重，一次性把这些用户查出来
        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<Users> users = usersMapper.selectBatchIds(userIds);
        //    做成 userId -> nickname 的字典（文档要显示"昵称"，用 nickname 不是 username），后面 O(1) 取
        Map<Long, String> nameMap = users.stream()
                .collect(Collectors.toMap(Users::getId, Users::getNickname));

        // 4) 组装 VO 列表：评论字段直接搬，昵称从字典取
        List<CommentVo> voList = new ArrayList<>();
        for (Comment c : comments) {
            CommentVo vo = new CommentVo();
            vo.setId(c.getId());
            vo.setUserId(c.getUserId());
            vo.setUserName(nameMap.get(c.getUserId()));
            vo.setContent(c.getContent());
            vo.setCreatedAt(c.getCreatedAt());
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 取帖子 + 可见性校验。
     * 规则和 PostServiceImpl.GetPostDEtail 一致：私密帖只有作者本人能看，
     * 其他人（包括游客）一律返回 404 而不是 403 ——
     * 返回 403 等于告诉对方"这个 id 确实存在一篇帖子"，那本身就是信息泄漏。
     */
    private Posts getVisiblePost(Long postId) {
        Posts post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }

        // 用 post.getUserId().equals(userId) 而不是反过来：
        // 游客的 userId 是 null，Long.equals(null) 只返回 false，而 null.equals(...) 会 NPE
        Long userId = UserContext.getUserContextId();
        if (post.getUserId().equals(userId)) {
            return post;                       // 作者本人，公开私密都能看
        }
        if (userId == null || !post.getUserId().equals(userId)) {
            if (post.getIsPublic()) {
                return post;                   // 不是作者，但帖子是公开的
            } else {
                throw new BusinessException(404, "帖子不存在");   // 私密帖对非作者一律当不存在
            }
        }
        // 上面那个 if 的条件其实恒为 true（能走到这行说明前面 equals 已经是 false 了），
        // 所以这句永远执行不到。但 Java 编译器不做这种推断，方法有返回值就必须有兜底 return，
        // 少了这行直接编译报"缺少返回语句"
        return post;
    }

    // 参数是评论id不是帖子id，原来叫 postId 容易看错成"按帖子查"
    private Comment KnowUserComment(Long commentId){

        Long userId = UserContext.getUserContextId();

        Comment comment = commentMapper.selectById(commentId);
        if(comment==null)
        {throw  new BusinessException(404,"评论不存在");}
        if (comment.getUserId().equals(userId)) {
            return comment;

        } else {
            throw new BusinessException(403, "无权访问");
        }
    }

}
