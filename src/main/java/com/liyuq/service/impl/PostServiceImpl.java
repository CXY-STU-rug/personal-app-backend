package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liyuq.DTO.PostDto;
import com.liyuq.VO.PostLikeVo;
import com.liyuq.VO.PostVO;
import com.liyuq.VO.PublicVo;
import com.liyuq.VO.postDetailVo;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.UserContext;
import com.liyuq.entity.*;
import com.liyuq.mapper.*;
import com.liyuq.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Posts> implements PostService {

    @Autowired
    private PostMapper postMapper;
    @Autowired
    private UsersMapper usersMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private PostLikeMapper postLikeMapper;
    @Autowired
    private PostImagesMapper postImagesMapper;
    @Qualifier("restTemplate")
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;   // 直接注入，Spring Boot 已自动配好

    private static final String HOT_KEY = "hot:post:rank";  // 榜单的 Redis key，抽成常量

    @Transactional
    @Override
    public Long post(PostDto postDto) {
        Long userId = UserContext.getUserContextId();

    List<String> imageUrlList = postDto.getImageUrls();

        Posts post = new Posts();
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setUserId(userId);

        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());   // updated_at 是 NOT NULL，新建时=创建时间
        post.setIsPublic(postDto.getIsPublic());
        post.setVideoUrl(postDto.getVideoUrl());
        postMapper.insert(post);

        Long postId = post.getId();


// 先判空：没传图（纯文字帖）就直接跳过，避免 NPE
    if (imageUrlList != null && !imageUrlList.isEmpty()) {
        for (int i = 0; i < imageUrlList.size(); i++) {   // ← 遍历“图片”，不是 posts
            PostImages img = new PostImages();            // ← 每张图 new 一个新对象
            img.setPostId(postId);
            img.setImageUrl(imageUrlList.get(i));
            img.setSort(i);                               // 下标即显示顺序
            postImagesMapper.insert(img);                 // ← 真正写进库
        }
    }
        return postId;

    }

    @Override
    public void updatePost(Long postId, PostDto postDto) {

        Long userId = UserContext.getUserContextId();
        KnowUserPost(postId);
        Posts post = new Posts();
        post.setId(postId);
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setUpdatedAt(LocalDateTime.now());   // 编辑时刷新更新时间
        post.setIsPublic(postDto.getIsPublic());
        post.setUserId(userId);
        post.setVideoUrl(postDto.getVideoUrl());
        postMapper.updateById(post);
        return;
    }
    @Transactional
    @Override
    public void deletePost(Long id) {
        // 归属校验在 KnowUserPost 里做，不是自己的帖子会直接抛 403，
        // 所以这里不需要再单独取一次 userId（原来那行取了从没用过）
        KnowUserPost(id);
        postMapper.deleteById(id);
        LambdaQueryWrapper<PostLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PostLike::getPostId, id);
        postLikeMapper.delete(queryWrapper);
        LambdaQueryWrapper<PostImages> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(PostImages::getPostId, id);
        postImagesMapper.delete(queryWrapper1);

        LambdaQueryWrapper<Comment> queryWrapper2 = new LambdaQueryWrapper<>();
queryWrapper2.eq(Comment::getPostId, id);
commentMapper.delete(queryWrapper2);
     // ⚠️ 这里必须 .toString()，不能直接传 Long：
     //    StringRedisTemplate 的序列化器是 StringRedisSerializer，它会把成员强转成 String，
     //    传 Long 进来运行时直接 ClassCastException（删帖整个功能 500）。
     //    而且就算不报错也删不掉——本类另外 4 处（incrementScore ×2、add ×1）写进榜单时
     //    用的都是 postId.toString()，Redis 里存的是字符串 "14"，
     //    拿数字 14 去 remove 序列化出来的字节不一样，匹配不上，热榜会残留已删除的帖子
     stringRedisTemplate.opsForZSet().remove(HOT_KEY, id.toString());
return;
    }

    @Override
    public PostVO listPosts(Integer page, Integer size) {
        if (page == null || size == null) {
            page=1;
            size=10;
        }
        Page<Posts> postsIPage = new Page<>(page, size);
        LambdaQueryWrapper<Posts> queryWrapper = new LambdaQueryWrapper<Posts>();
        queryWrapper.orderByDesc(Posts::getCreatedAt).eq(Posts::getIsPublic,true);

        IPage<Posts> result = postMapper.selectPage(postsIPage, queryWrapper);
        PostVO postVO = new PostVO();
        postVO.setTotal(result.getTotal());
        //查出来主表post的分页对象
        List<Posts> list = result.getRecords();
        //定义嵌套vo集合
        List<PublicVo> listPublicVo = new ArrayList<>();
        //这是主表里面该有的userId
        List<Long> userIdList = new ArrayList<>();

        List<Users> usersList = new ArrayList<>();


        for (Posts post : list) {
//循环里面创建list的泛型对象
            PublicVo publicVo = new PublicVo();
//组装对象
            userIdList.add(post.getUserId());
            publicVo.setTitle(post.getTitle());
            publicVo.setCreatedAt(post.getCreatedAt());
            publicVo.setId(post.getId());
            publicVo.setAuthorId(post.getUserId());
            publicVo.setVideoUrl(post.getVideoUrl());   // 列表也返回视频url
            listPublicVo.add(publicVo);
//插入数据commentCount
            LambdaQueryWrapper<Comment> commentLambdaQueryWrapper = new LambdaQueryWrapper<Comment>();
            commentLambdaQueryWrapper.eq(Comment::getPostId, post.getId());
            //这里是一个帖子多个评论，通过外键确定数量
            List<Comment> comments = commentMapper.selectList(commentLambdaQueryWrapper);
            Integer commentCount = comments.size();
            publicVo.setCommentCount(commentCount);
//插入数据likeCount
            LambdaQueryWrapper<PostLike> postLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            postLikeLambdaQueryWrapper.eq(PostLike::getPostId, post.getId());
            //这里面是一个帖子多个点赞，根据外键post_id确定一个帖子多少点赞
            List<PostLike> postLikes = postLikeMapper.selectList(postLikeLambdaQueryWrapper);
            Integer likeCount = postLikes.size();
            publicVo.setLikeCount(likeCount);
//插入首页封面，一个帖子多张图片
            LambdaQueryWrapper<PostImages> postImagesLambdaQueryWrapper = new LambdaQueryWrapper<PostImages>();
            postImagesLambdaQueryWrapper.eq(PostImages::getPostId, post.getId())
                    .orderByAsc(PostImages::getSort);
            //查的是图片集合
            List<PostImages> postImagesList = postImagesMapper.selectList(postImagesLambdaQueryWrapper);

            if (postImagesList.isEmpty()) {
                publicVo.setCoverUrl(null);          // 没图 → 封面留空
            } else {
                publicVo.setCoverUrl(postImagesList.get(0).getImageUrl());  // 有图 → 取排序第一张的 URL
            }
        }
        //插入AuthorName，根据上面查出来的用户id组成集合然后去查用户名，要集合遍历
        for (Long userId : userIdList) {

            LambdaQueryWrapper<Users> userLambdaQueryWrapper = new LambdaQueryWrapper<Users>();
            userLambdaQueryWrapper.eq(Users::getId, userId);
//一条一条遍历查出来放进用户集合
            Users users = usersMapper.selectOne(userLambdaQueryWrapper);

            usersList.add(users);


        }
        //遍历 publicVolist集合,去插入里面的对象
            for (PublicVo publicVo : listPublicVo) {
                //这里又要取出用户集合的数据插入主集合
                for (Users users : usersList) {
                    if (users.getId() .equals( publicVo.getAuthorId())) {
                        publicVo.setAuthorName(users.getUsername());
                    }
                }
            }
postVO.setList(listPublicVo);

return postVO;
        }

    @Override
    public postDetailVo GetPostDEtail(Long id) {
      Posts  post = postMapper.selectById(id);
      if (post == null) {
          throw  new BusinessException(404,"帖子不存在");
      }
        // 私密帖：只有作者本人可见
        if (!post.getIsPublic()) {
            Long currentUserId = UserContext.getUserContextId();   // 当前登录人（游客时可能为 null）
            if (currentUserId == null || !currentUserId.equals(post.getUserId())) {
                throw new BusinessException(404, "帖子不存在");
            }
        }

      //组装post表字段
     postDetailVo postDetailVo = new postDetailVo();
      postDetailVo.setId(post.getId());
      postDetailVo.setTitle(post.getTitle());
      postDetailVo.setContent(post.getContent());
      postDetailVo.setCreatedAt(post.getCreatedAt());
      postDetailVo.setAuthorId(post.getUserId());

        //组装用户名
      LambdaQueryWrapper<Users>queryWrapper = new LambdaQueryWrapper<>();
      queryWrapper.eq(Users::getId, post.getUserId());
      Users users = usersMapper.selectOne(queryWrapper);
      postDetailVo.setAuthorName(users.getUsername());

//组装评论数
        LambdaQueryWrapper<Comment> commentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        commentLambdaQueryWrapper.eq(Comment::getPostId, post.getId());
        List<Comment> comments = commentMapper.selectList(commentLambdaQueryWrapper);
        Integer commentCount = comments.size();
        postDetailVo.setCommentCount(commentCount);
        //组装图片url
        LambdaQueryWrapper<PostImages> postImagesLambdaQueryWrapper = new LambdaQueryWrapper<>();
        postImagesLambdaQueryWrapper.eq(PostImages::getPostId, post.getId());
        List<PostImages> postImages = postImagesMapper.selectList(postImagesLambdaQueryWrapper);
        List<String>urlList = new ArrayList<>();
        //循环插入url集合
          for (PostImages postImage : postImages) {
         if (postImage.getImageUrl() != null) {
        urlList.add(postImage.getImageUrl());
     }
   }
        postDetailVo.setImages(urlList);
        //组装点赞数
        LambdaQueryWrapper<PostLike> postLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        postLikeLambdaQueryWrapper.eq(PostLike::getPostId, post.getId());
        List<PostLike> postLikes = postLikeMapper.selectList(postLikeLambdaQueryWrapper);
        Integer likeCount = postLikes.size();
        postDetailVo.setLikeCount(likeCount);
        //组装视频url（有则前端渲染播放器）
        postDetailVo.setVideoUrl(post.getVideoUrl());

        return postDetailVo;
    }

    @Override
    public PostVO GetUserPost(Long userId,Integer page, Integer size) {
        if (page==null||size==null) {page=1;size=10;}


    //分页对象
    Page<Posts>postsPage=new Page<>(page,size);

    //组装分页查询条件
    LambdaQueryWrapper<Posts> postLambdaQueryWrapper = new LambdaQueryWrapper<>();
    postLambdaQueryWrapper.eq(Posts::getUserId, userId)
            .eq(Posts::getIsPublic, true);
    postLambdaQueryWrapper.orderByAsc(Posts::getId);
    //查询具体的作者帖子
   IPage<Posts>result=  postMapper.selectPage(postsPage, postLambdaQueryWrapper);
   //建返回对象
   PostVO postVO = new PostVO();
   //组装返回对象字段
postVO.setTotal(result.getTotal());
//组装返回对象的第二个字段，集合
        List<PublicVo>publicVoList = new ArrayList<>();



        for (Posts post : result.getRecords()) {
            PublicVo publicVo = new PublicVo();
            //组装泛型的vo
            publicVo.setAuthorId(userId);
            publicVo.setId(post.getId());
            publicVo.setTitle(post.getTitle());
            publicVo.setCreatedAt(post.getCreatedAt());
            publicVo.setVideoUrl(post.getVideoUrl());   // 作者页列表也返回视频url
            //组装点赞数
            LambdaQueryWrapper<PostLike> postLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            postLikeLambdaQueryWrapper.eq(PostLike::getPostId, post.getId());
            List<PostLike> postLikes = postLikeMapper.selectList(postLikeLambdaQueryWrapper);
            Integer likeCount = postLikes.size();
            publicVo.setLikeCount(likeCount);
            //组装评论数
            LambdaQueryWrapper<Comment>commentLambdaQueryWrapper = new LambdaQueryWrapper<>();
            commentLambdaQueryWrapper.eq(Comment::getPostId, post.getId());
            List<Comment> comments = commentMapper.selectList(commentLambdaQueryWrapper);
            Integer commentCount = comments.size();
            publicVo.setCommentCount(commentCount);
            //组装图片
            LambdaQueryWrapper<PostImages> postImagesLambdaQueryWrapper = new LambdaQueryWrapper<>();
          postImagesLambdaQueryWrapper.eq(PostImages::getPostId, post.getId());

          //返回图片集合
          List<PostImages> postImages = postImagesMapper.selectList(postImagesLambdaQueryWrapper);

//把图片的第一张作为封面
            if (postImages.isEmpty()) {
                publicVo.setCoverUrl(null);
            } else {
                publicVo.setCoverUrl(postImages.get(0).getImageUrl());
            }

         LambdaQueryWrapper<Users>userLambdaQueryWrapper = new LambdaQueryWrapper<>();
         userLambdaQueryWrapper.eq(Users::getId, userId);
         Users users = usersMapper.selectOne(userLambdaQueryWrapper);
         publicVo.setAuthorName(users.getUsername());
            publicVoList.add(publicVo);


        }

        postVO.setList(publicVoList);
        return postVO;
    }



@Transactional
    @Override
    public PostLikeVo PostLike(Long postId) {
    Posts posts=postMapper.selectById(postId);
    if (posts==null||!posts.getIsPublic()) { throw  new BusinessException(404,"帖子未公开或帖子不存在");}

        LambdaQueryWrapper<PostLike>postLikeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        postLikeLambdaQueryWrapper.eq(PostLike::getPostId, postId);
        postLikeLambdaQueryWrapper.eq(PostLike::getUserId, UserContext.getUserContextId());
      PostLike postLikes=  postLikeMapper.selectOne(postLikeLambdaQueryWrapper);
      if(postLikes==null){
          PostLike postLike = new PostLike();
          postLike.setUserId(UserContext.getUserContextId());
          postLike.setPostId(postId);
          postLike.setCreatedAt(LocalDateTime.now());
          postLikeMapper.insert(postLike);
          stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, postId.toString(), 1);
          //查该贴有多少点赞
          Integer   Likecount=CheckLikeCount(postId);
          PostLikeVo postLikeVo= new PostLikeVo();
          postLikeVo.setLiked(true);
          postLikeVo.setLikesCount(Likecount);

          return postLikeVo;

      }
      // 走到这里说明 postLikes != null，即"我"之前已经赞过这条帖 → 执行"取消赞"
      // 按同样的 post_id + user_id 条件把那条点赞记录删掉
      postLikeMapper.delete(postLikeLambdaQueryWrapper);
        stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, postId.toString(), -1);
      // 取消后重新统计这条帖的点赞总数
      Integer likeCount = CheckLikeCount(postId);
      PostLikeVo postLikeVo = new PostLikeVo();
      postLikeVo.setLiked(false);              // 现在是"未赞"状态
      postLikeVo.setLikesCount(likeCount);
      return postLikeVo;
    }


    //redis热榜排行，需要zset
    @Override
    public List<PublicVo> GetHotPosts(Integer limit) {

        if (limit == null || limit <= 0) limit = 10;

        // 1) 读榜：ZREVRANGE，一次拿到"已按点赞数降序"的前 N 个 (成员+分数)
        //    分数(score)本身就是点赞数，所以顺带把 likeCount 也拿到了，不用再查 post_like！
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(HOT_KEY, 0, limit - 1);

        // 2) 冷启动兜底：Redis 空（刚重启/还没人点赞）→ 走旧的全量DB版重建（见下方说明）
        if (tuples == null || tuples.isEmpty()) {
            return rebuildHotFromDb(limit);   // 把你旧版那段逻辑抽成这个私有方法
        }


        // 3) 把有序的 id 抽出来（tuples 是 LinkedHashSet，顺序=榜单顺序，别打乱）
        //    同时把 id->score(点赞数) 存一份，等会组装直接用
        List<Long> orderedIds = new ArrayList<>();
        Map<Long, Integer> likeFromScore = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            Long pid = Long.valueOf(t.getValue());      // 成员是字符串，转回 Long
            orderedIds.add(pid);
            likeFromScore.put(pid, t.getScore().intValue()); // score 是 Double，转 int
        }

        // 4) 【坑】批量查帖子补全字段，但 selectBatchIds 返回顺序≠榜单顺序！
        //    所以先查出来建成 Map，再按 orderedIds 的顺序去 Map 里捞，才不会打乱名次。
        Map<Long, Posts> postMap = postMapper.selectBatchIds(orderedIds).stream()
                .collect(Collectors.toMap(Posts::getId, p -> p));

        // 5) 补全 Redis 不存的字段：作者名 / 评论数 / 封面。都按 orderedIds 批量查，避免 N+1
        //    5.1 作者名：收集这批帖子的作者 id，批量查 users 建成 id->用户名 字典
        List<Long> authorIds = new ArrayList<>();
        for (Posts p : postMap.values()) {
            authorIds.add(p.getUserId());
        }
        Map<Long, String> userNameMap = new HashMap<>();
        for (Users u : usersMapper.selectBatchIds(authorIds)) {
            userNameMap.put(u.getId(), u.getUsername());
        }

        //    5.2 评论数：一次性把这批帖子的评论查出来，按 postId 计数
        Map<Long, Integer> commentCountMap = new HashMap<>();
        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().in(Comment::getPostId, orderedIds));
        for (Comment c : comments) {
            commentCountMap.merge(c.getPostId(), 1, Integer::sum);
        }

        //    5.3 封面：按 sort 升序批量查图片，每个帖子只保留第一张（=封面）
        Map<Long, PostImages> coverMap = new HashMap<>();
        List<PostImages> imgs = postImagesMapper.selectList(
                new LambdaQueryWrapper<PostImages>()
                        .in(PostImages::getPostId, orderedIds)
                        .orderByAsc(PostImages::getSort));
        for (PostImages img : imgs) {
            if (coverMap.containsKey(img.getPostId())) continue;   // 已有封面就跳过，保住 sort 最小那张
            coverMap.put(img.getPostId(), img);
        }

        // 6) 按 orderedIds 的顺序（=榜单名次）逐个拼装 PublicVo
        List<PublicVo> publicVoList = new ArrayList<>();
        int rank = 1;                                     // 名次单独计数，跳过失效帖时才不会断号
        for (Long pid : orderedIds) {
            Posts p = postMap.get(pid);
            if (p == null || !p.getIsPublic()) continue;  // 帖子被删或已转私密 → 不该出现在公开热榜

            PublicVo publicVo = new PublicVo();
            publicVo.setId(p.getId());
            publicVo.setTitle(p.getTitle());
            publicVo.setAuthorId(p.getUserId());
            publicVo.setAuthorName(userNameMap.get(p.getUserId()));
            publicVo.setCreatedAt(p.getCreatedAt());
            publicVo.setVideoUrl(p.getVideoUrl());
            publicVo.setLikeCount(likeFromScore.get(pid));           // 赞数直接用 ZSet 的 score，省一次查库
            publicVo.setCommentCount(commentCountMap.getOrDefault(pid, 0));
            PostImages cover = coverMap.get(pid);
            publicVo.setCoverUrl(cover == null ? null : cover.getImageUrl());  // 没图就留 null
            publicVo.setRank(rank++);                     // 填名次后自增
            publicVoList.add(publicVo);
        }

        return publicVoList;




    }

    private Posts KnowUserPost(Long postId){

            Long userId = UserContext.getUserContextId();

            Posts post = postMapper.selectById(postId);
            if(post==null)
            {throw  new BusinessException(404,"帖子不存在");}
            if (post.getUserId().equals(userId)) {
                return post;

            } else {
                throw new BusinessException(403, "无权访问");
            }
        }

        private Integer CheckLikeCount(Long id){
            //查该贴有多少点赞
            LambdaQueryWrapper<PostLike> postLambdaQueryWrapper = new LambdaQueryWrapper<>();
            postLambdaQueryWrapper.eq(PostLike::getPostId, id);
            List<PostLike>postLikes = postLikeMapper.selectList(postLambdaQueryWrapper);
            return postLikes.size();

        }


        public  List<PublicVo>  rebuildHotFromDb(Integer limit){
    if (limit == null || limit <= 0) {
        limit = 10;
    }
    LambdaQueryWrapper<Posts> postsLambdaQueryWrapper = new LambdaQueryWrapper<>();
    postsLambdaQueryWrapper.eq(Posts::getIsPublic, true);

    List<Posts> posts = postMapper.selectList(postsLambdaQueryWrapper);
    List<PostLike> allLikes = postLikeMapper.selectList(new LambdaQueryWrapper<PostLike>());
    Map<Long, Integer> likeCountMap = new HashMap<>();
    for (PostLike like : allLikes) {
        likeCountMap.merge(like.getPostId(), 1, Integer::sum);
    }

    List<Comment> allComments = commentMapper.selectList(null);
    Map<Long, Integer> commentCountMap = new HashMap<>();
    for (Comment c : allComments) {
        commentCountMap.merge(c.getPostId(), 1, Integer::sum);
    }
    List<Users> allUsers = usersMapper.selectList(null);
    Map<Long, String> userCountMap = new HashMap<>();
    for (Users users : allUsers) {
        if (userCountMap.containsKey(users.getId())) {
            continue;
        }
        userCountMap.put(users.getId(), users.getUsername());
    }

    List<PostImages>postImagesList = postImagesMapper.selectList(new LambdaQueryWrapper<PostImages>().orderByAsc(PostImages::getPostId).orderByAsc(PostImages::getSort));
    Map<Long, PostImages> postImagesMap = new HashMap<>();
    for (PostImages postImages : postImagesList) {
        if (postImagesMap.containsKey(postImages.getPostId())) {
            continue;
        }
        postImagesMap.put(postImages.getPostId(), postImages);
    }
    List<Posts> hot = posts.stream()
            .sorted(Comparator.comparingInt(
                    (Posts p) -> likeCountMap.getOrDefault(p.getId(), 0)).reversed())
            .limit(limit)
            .toList();
    List<PublicVo> publicVoList = new ArrayList<>();
    for (int i = 0; i < hot.size(); i++) {
        Posts p=hot.get(i);
        PublicVo publicVo = new PublicVo();
        publicVo.setTitle(p.getTitle());
        publicVo.setId(p.getId());
        publicVo.setAuthorId(p.getUserId());
        publicVo.setCreatedAt(p.getCreatedAt());
        publicVo.setAuthorName(userCountMap.get(p.getUserId()));
        publicVo.setLikeCount(likeCountMap.getOrDefault(p.getId(), 0));
        publicVo.setCommentCount(commentCountMap.getOrDefault(p.getId(), 0));
        PostImages cover = postImagesMap.get(p.getId());   // 先取出来，可能为 null
        publicVo.setCoverUrl(cover == null ? null : cover.getImageUrl());  // 没图就留 null
        publicVo.setRank(i+1);
        publicVoList.add(publicVo);

    }
            // 冷启动重建后回填 ZSet，下次直接走 Redis 不再全量查库。
            // 遍历 hot 而不是 likeCountMap：likeCountMap 来自全表 post_likes，没经过 isPublic 过滤，
            // 拿它回填会把私密帖/已删帖的 postId 一起写进榜单；
            // 而且 0 赞的公开帖不在 likeCountMap 里，用它回填这些帖子永远上不了榜。
            // 遍历 hot 能保证「写进 Redis 的」和「返回给用户的」完全一致。
            for (Posts p : hot) {
                // 用 add（直接设 score），不能用 incrementScore——重建是"覆盖"不是"累加"
                stringRedisTemplate.opsForZSet().add(HOT_KEY, p.getId().toString(),
                        likeCountMap.getOrDefault(p.getId(), 0));
            }


            return publicVoList;
}




}

