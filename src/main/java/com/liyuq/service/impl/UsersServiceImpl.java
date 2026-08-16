package com.liyuq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.liyuq.DTO.*;
import com.liyuq.VO.UserCenterVo;
import com.liyuq.VO.UserVo;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.Result;
import com.liyuq.common.UserContext;
import com.liyuq.common.util.JwtUtil;
import com.liyuq.entity.*;
import com.liyuq.mapper.*;
import com.liyuq.service.EmailService;
import com.liyuq.service.UsersService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import jakarta.annotation.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * <p>
 * 鐢ㄦ埛琛 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements UsersService {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    @Autowired
    private UsersMapper usersMapper;
    @Resource
    private JwtUtil jwtUtil;
    @Autowired
    private UserContext userContext;

    @Autowired
    private PasswordResetTokenMapper passwordResetTokenMapper;

    @Autowired
    private EmailService emailService;



    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostImagesMapper postImagesMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostLikeMapper postLikeMapper;

    @Autowired
    private TodosMapper todosMapper;

    @Autowired
    private FinanceRecordsMapper financeRecordsMapper;

    @Autowired
    private SchedulesMapper schedulesMapper;

    @Autowired
    private AiMessagesMapper aiMessagesMapper;


    @Autowired
    private NotesMapper notesMapper;

    @Autowired
    private NoteTagRelationsMapper noteTagRelationsMapper;

    @Autowired
    private NoteTagsMapper noteTagsMapper;

    @Value("${app.frontend-url}")
    private String frontendUrl;
// 从配置读，不写死。本地和线上只改 yml，代码不动
@Autowired
private UserOauthBingingMapper userOauthBingingMapper;

    public Result<Void> register(RegisterDto registerDto){
        LambdaQueryWrapper<Users> wrapper = Wrappers.lambdaQuery();

       wrapper.eq(Users::getUsername, registerDto.getUsername());
        List<Users> list=usersMapper.selectList(wrapper);
        if(list.size()>0){
            return Result.fail(400,"用户已存在");
        }
        Users user=new Users();
        user.setUsername(registerDto.getUsername());
        user.setPassword(ENCODER.encode(registerDto.getPassword()));
        user.setNickname(registerDto.getNickname() != null ? registerDto.getNickname() : registerDto.getUsername());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        usersMapper.insert(user);
        return Result.success();
    }

    @Override
    public Result<UserVo> login(UserDto userDto) {
        LambdaQueryWrapper<Users> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Users::getUsername,userDto.getUsername());
        Users user = usersMapper.selectOne(wrapper);
        // ── 第二段：两种失败合并成一句提示 ──
        // matches(明文, 密文)：参数顺序不能反！
        // || 有短路特性：user 为 null 时直接判定失败，不会执行后半句（所以不会空指针）
        if (user == null || !ENCODER.matches(userDto.getPassword(), user.getPassword())) {
            // 文档 3.2：统一提示，不让外人探测出"这个账号存在但密码错了"
            return Result.fail(422, "账号或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());
        UserVo vo = new UserVo();
        vo.setToken(token);
        vo.setNickname(user.getNickname());
        vo.setUserId(user.getId());
        return Result.success(vo);
    }

    @Override
    public UserCenterVo LoadUserInfo() {
        Long userId=UserContext.getUserContextId();
        LambdaQueryWrapper<Users> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Users::getId,userId);
       Users users= usersMapper.selectOne(wrapper);
        UserCenterVo vo = new UserCenterVo();
        vo.setId(userId);
        vo.setNickname(users.getNickname());
        vo.setUsername(users.getUsername());
        vo.setCreatedAt(users.getCreatedAt());   // 注册时间也要搬进VO，漏了这行就返回null
        return vo;

    }

    @Override
    public Integer UpdateUserInfo(UserInfoDto userInfoDto) {
        Long userId=UserContext.getUserContextId();
        Users users=new Users();
        users.setId(userId);
        users.setNickname(userInfoDto.getNickname());
        users.setUpdatedAt(LocalDateTime.now());
             int b=usersMapper.updateById(users);

        return b;
    }

    @Override
    public void UpadtePassword(UpdatePassWordDto dto) {
        // 这里原来有一句 oldPassWord==null 就抛错的判空，删了：
        // OAuth 建号的用户压根没有旧密码，会在那一行就被拦掉，
        // 下面的 isOauthAccount 分支一次都执行不到。
        // 两个字段的判空分别挪到了各自该在的位置（旧密码在 !isOauthAccount 里，新密码在最后）
        LambdaQueryWrapper<Users> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Users::getId,userContext.getUserContextId());
       Users users= usersMapper.selectOne(wrapper);
        if (users == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // OAuth 建号时 password 存的是空串（不能让 null 进 NOT NULL 列），
        // 这类账号没有"旧密码"可校验，本次相当于首次设置密码
        boolean isOauthAccount = users.getPassword().isEmpty();
        if (!isOauthAccount) {
            if (dto.getOldPassWord() == null) {
                throw new BusinessException(403, "旧密码不能为空");
            }
            if (!ENCODER.matches(dto.getOldPassWord(), users.getPassword())) {
                throw new BusinessException(403, "旧密码不一致");
            }
        }

        if (dto.getNewPassWord() == null || dto.getNewPassWord().isBlank()) {
            throw new BusinessException(404, "新密码不能为空");
        }

        users.setPassword(ENCODER.encode(dto.getNewPassWord()));
        usersMapper.updateById(users);
    }

    @Override
    public void UpdateAvatar(String avatar) {
        Long userId=UserContext.getUserContextId();
        LambdaQueryWrapper<Users> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Users::getId,userId);
        Users users= usersMapper.selectOne(wrapper);
        users.setAvatar(avatar);
        usersMapper.updateById(users);
    }

    @Override
    public void BingEmail(EmailBodyDto dto) {
        // 查出当前登录用户
        Users users = usersMapper.selectOne(
                new LambdaQueryWrapper<Users>().eq(Users::getId, userContext.getUserContextId()));

        // 绑定/改绑邮箱都是敏感操作，先校验密码——不管是首次绑还是改绑，一律先过这一关，
        // 否则谁拿到登录态就能把邮箱换成自己的，等于账号被接管
        if (!ENCODER.matches(dto.getPassword(), users.getPassword())) {
            throw new BusinessException(403, "密码不一致");
        }

        // 邮箱唯一性：不能绑一个已经被别的账号占用的邮箱
        Users owner = usersMapper.selectOne(
                new LambdaQueryWrapper<Users>().eq(Users::getEmail, dto.getEmail()));
        if (owner != null && !owner.getId().equals(users.getId())) {
            throw new BusinessException(409, "该邮箱已被其他账号绑定");
        }

        // 校验通过，写入新邮箱
        users.setEmail(dto.getEmail());
        usersMapper.updateById(users);
    }

    @Override
public void forgotPassword(String email) {


    // 1. 按邮箱查用户
    LambdaQueryWrapper<Users> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Users::getEmail, email);
    Users user = usersMapper.selectOne(wrapper);

    // 2. 用户不存在也直接返回，防止被探测邮箱是否注册
    if (user == null) {
        return;
    }

    // 3. 生成明文令牌与 SHA-256 哈希
    String token = generateToken();
    String tokenHash = DigestUtils.sha256Hex(token);
    LocalDateTime now = LocalDateTime.now();

    PasswordResetToken resetToken = new PasswordResetToken();
    resetToken.setUserId(user.getId());
    resetToken.setTokenHash(tokenHash);
    resetToken.setExpiresAt(now.plusMinutes(30));
    resetToken.setUsed(false);
    resetToken.setCreatedAt(now);

        // 任意一封邮件泄漏都能改密码——文档 2304 行要求的"用一次即作废"是两件事：
        // 用过的要作废（reset 里那条），没用过的旧的也要作废（这条）
        LambdaUpdateWrapper<PasswordResetToken> invalidate = new LambdaUpdateWrapper<>();
        invalidate.eq(PasswordResetToken::getUserId, user.getId())   // 只动这个用户的
                .eq(PasswordResetToken::getUsed, false)            // 只动还没用过的
                .set(PasswordResetToken::getUsed, true);           // 统统置为已用
        passwordResetTokenMapper.update(null, invalidate);

    passwordResetTokenMapper.insert(resetToken);

    // 4. 发送含明文令牌的链接到邮箱
    String resetLink = frontendUrl+"/reset?token=" + token;
    emailService.sendPasswordResetEmail(email, resetLink);
}


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void reset(ResetPasswordDto dto) {
        // 先从 DTO 里取出来，下面整段逻辑一行都不用动。
        // DTO 上虽然有 @NotBlank/@Size，但那要 controller 加了 @Valid 才生效，
        // service 这层自己再判一次，防止以后别处直接调进来绕过校验
        String token = dto.getToken();
        String password = dto.getNewPassword();

        if (token == null || token.isBlank()) {
            throw new BusinessException(400, "令牌不能为空");
        }

        // 2. 计算哈希
        String tokenHash = DigestUtils.sha256Hex(token);

        // 3. 按哈希和未使用状态查记录
        LambdaQueryWrapper<PasswordResetToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PasswordResetToken::getTokenHash, tokenHash)
                .eq(PasswordResetToken::getUsed, false);
        PasswordResetToken resetToken = passwordResetTokenMapper.selectOne(wrapper);

        // 4. 查不到说明令牌无效或已使用
        if (resetToken == null) {
            throw new BusinessException(400, "重置链接无效或已被使用");
        }

        // 5. 过期校验
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "重置链接已过期，请重新申请");
        }
       Users user = usersMapper.selectById(resetToken.getUserId());
if (user == null) {
    throw  new BusinessException(404,"用户不存在");
}

        user.setPassword(ENCODER.encode(password));
        resetToken.setUsed(true);
        usersMapper.updateById(user);
        passwordResetTokenMapper.updateById(resetToken);


    }

    @Override
    // 下面要按顺序删 11 张表，中间任何一步失败都必须整体回滚。
    // 没有事务的话会留下"帖子还在、作者已经没了"这种半删状态，
    // 之后任何人打开那条帖子的详情都会 NPE（PostServiceImpl 里拿 users.getUsername()）
    @Transactional(rollbackFor = Exception.class)
    public void delete() {

        Long userId = UserContext.getUserContextId();

        // 1. 查出该用户发布的所有帖子ID，便于先删帖子依赖数据
        List<Posts> userPosts = postMapper.selectList(
                new LambdaQueryWrapper<Posts>().eq(Posts::getUserId, userId));
        List<Long> postIds = userPosts.stream().map(Posts::getId).toList();

        if (!postIds.isEmpty()) {
            // 1.1 删帖子关联图片（post_images）
            postImagesMapper.delete(
                    new LambdaQueryWrapper<PostImages>().in(PostImages::getPostId, postIds));

            // 1.2 删帖子下所有评论（comments 的 post_id 属于这些帖子）
            commentMapper.delete(
                    new LambdaQueryWrapper<Comment>().in(Comment::getPostId, postIds));

            // 1.3 删帖子下所有点赞（post_likes 的 post_id 属于这些帖子）
            postLikeMapper.delete(
                    new LambdaQueryWrapper<PostLike>().in(PostLike::getPostId, postIds));
        }

        // 2. 删除该用户自己发表的评论
        commentMapper.delete(
                new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId));

        // 3. 删除该用户自己点的赞
        postLikeMapper.delete(
                new LambdaQueryWrapper<PostLike>().eq(PostLike::getUserId, userId));

        // 4. 删除该用户发布的帖子（此时依赖数据已清空）
        postMapper.delete(
                new LambdaQueryWrapper<Posts>().eq(Posts::getUserId, userId));

        // 5. 删除待办
        todosMapper.delete(
                new LambdaQueryWrapper<Todos>().eq(Todos::getUserId, userId));

        // 6. 删除记账记录
        financeRecordsMapper.delete(
                new LambdaQueryWrapper<FinanceRecords>().eq(FinanceRecords::getUserId, userId));

        // 7. 删除日程
        schedulesMapper.delete(
                new LambdaQueryWrapper<Schedules>().eq(Schedules::getUserId, userId));

        // 8. 删除 AI 对话消息
        aiMessagesMapper.delete(
                new LambdaQueryWrapper<AiMessages>().eq(AiMessages::getUserId, userId));

        // 9. 删除密码重置令牌
        passwordResetTokenMapper.delete(
                new LambdaQueryWrapper<PasswordResetToken>().eq(PasswordResetToken::getUserId, userId));
// 9.5 删除第三方登录绑定（GitHub 等）
        userOauthBingingMapper.delete(
                new LambdaQueryWrapper<UserOauthBinging>().eq(UserOauthBinging::getUserId, userId));
        // 10. 删除笔记相关
        // 10.1 查出该用户所有笔记ID，先删关联表
        List<Notes> userNotes = notesMapper.selectList(
                new LambdaQueryWrapper<Notes>().eq(Notes::getUserId, userId));
        List<Long> noteIds = userNotes.stream().map(Notes::getId).toList();

        if (!noteIds.isEmpty()) {
            noteTagRelationsMapper.delete(
                    new LambdaQueryWrapper<NoteTagRelations>().in(NoteTagRelations::getNoteId, noteIds));
        }

        // 10.2 删除笔记本体
        notesMapper.delete(
                new LambdaQueryWrapper<Notes>().eq(Notes::getUserId, userId));

        // 10.3 删除该用户创建的标签（此时关联表已无引用）
        noteTagsMapper.delete(
                new LambdaQueryWrapper<NoteTags>().eq(NoteTags::getUserId, userId));

        // 11. 最后删除用户主表记录
        usersMapper.deleteById(userId);












    }

    private String generateToken() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
}

}
