package com.liyuq.controller;


import com.liyuq.DTO.PostDto;
import com.liyuq.VO.PostLikeVo;
import com.liyuq.VO.PostVO;
import com.liyuq.VO.PublicVo;
import com.liyuq.VO.postDetailVo;
import com.liyuq.common.Result;
import com.liyuq.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/post")
public class PostController {
    @Autowired
    private PostService postService;

       @PostMapping()
    public Result<Map<String,Long>> post(@RequestBody PostDto postDto) {

    Long id=postService.post(postDto);

      Map<String,Long> map=new HashMap<>();
          map.put("id",id);
       return Result.success(map);

}
     @PutMapping()
     public Result<Void> update(@RequestParam Long id, @RequestBody PostDto postDto) {

    postService.updatePost(id,postDto);

return  Result.success();
}

@DeleteMapping()
    public Result<Void> delete(@RequestParam Long id) {

    postService.deletePost(id);
    return  Result.success();
}

@GetMapping("/public")
    public Result<PostVO> getPublicPosts(Integer page,Integer size) {
   PostVO postVO= postService.listPosts(page,size);
    return  Result.success(postVO);
}

@GetMapping("/detail/{id}")
    public Result<postDetailVo> getPostDetail(@PathVariable Long id) {
      postDetailVo postDetailVo=     postService.GetPostDEtail(id);

return  Result.success(postDetailVo);
}

@GetMapping("user/{userId}")
public Result<PostVO> getUserIds(  @PathVariable Long userId   ,Integer page,Integer size) {
   PostVO postVO= postService.GetUserPost(userId,page,size);

    return Result.success(postVO);
}



@PostMapping("/like")
    public Result<PostLikeVo>postLike(@RequestParam Long postId){

         PostLikeVo postLikeVo= postService.PostLike(postId);
return  Result.success(postLikeVo);
}


@GetMapping("hot")
public Result<List<PublicVo>> getHotPosts(Integer limit) {
   List<PublicVo> list=        postService.GetHotPosts(limit);

return Result.success(list);
}








}
