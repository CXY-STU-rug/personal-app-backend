package com.liyuq.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.liyuq.DTO.PostDto;
import com.liyuq.DTO.postCommentDto;
import com.liyuq.VO.PostLikeVo;
import com.liyuq.VO.PostVO;
import com.liyuq.VO.PublicVo;
import com.liyuq.VO.postDetailVo;
import com.liyuq.entity.Posts;

import java.util.List;

public interface PostService extends IService<Posts> {


     Long post(PostDto postDto);

     void updatePost(Long postid, PostDto postDto);

     void deletePost(Long id);

     PostVO listPosts(Integer page, Integer size);


    postDetailVo GetPostDEtail(Long id);

    PostVO GetUserPost(Long userId,Integer page, Integer size);



    PostLikeVo PostLike(Long id);

  List< PublicVo> GetHotPosts(Integer limit);
}