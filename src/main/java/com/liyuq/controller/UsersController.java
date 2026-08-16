package com.liyuq.controller;

import com.liyuq.DTO.*;
import com.liyuq.VO.UserCenterVo;
import com.liyuq.VO.UserVo;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.common.Result;
import com.liyuq.service.UsersService;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 鐢ㄦ埛琛 前端控制器
 * </p>
 *
 * @author liyuq
 * @since 2026-07-13
 */
@RestController
@RequestMapping("/api/user")
public class UsersController {
    @Resource
    private UsersService usersService;


    @PostMapping("register")
    public Result<Void> register(@RequestBody RegisterDto registerDto) {
        // service 现在自己返回 Result（你改的设计），controller 直接透传即可，
        // 不能自己再包一层 Result.success()——否则注册失败时 service 返回的错误会被丢掉
        return usersService.register(registerDto);
    }

    @PostMapping("login")
    public Result<UserVo> login(@RequestBody UserDto userDto) {
        // 同上：service 返回的已经是 Result<UserVo>，直接返回，不需要再取出 UserVo 重新包装
        return usersService.login(userDto);
    }

    @GetMapping("/info")
    public Result<UserCenterVo> Userinfo() {
         UserCenterVo userCenterVo=usersService.LoadUserInfo();
return Result.success(userCenterVo);
    }


    @PutMapping("/update")
    public Result<Void> update(@RequestBody UserInfoDto dto) {

           Integer b=  usersService.UpdateUserInfo(dto);
           if(b==null){

               throw new BusinessException(403,"更改失败");
           }

             return Result.success();
    }
    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestBody UpdatePassWordDto dto) {
        usersService.UpadtePassword(dto);
        return Result.success();
    }

    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestParam("avatar") String avatar) {

        usersService.UpdateAvatar(avatar);

        return  Result.success();
    }

    @PutMapping("/email")
    public Result<Void> Email(@RequestBody EmailBodyDto dto) {

        usersService.BingEmail(dto);
        return Result.success();
    }
    @PostMapping("/password/forgot")
public Result<Void> forgotPassword(@RequestBody @Valid ForgotPasswordDto dto) {
    usersService.forgotPassword(dto.getEmail());
    return Result.success(200,"若该邮箱已注册，我们已发送重置邮件，请查收");
}

@PostMapping("/password/reset")
public Result<Void> resetPassword(@RequestBody @Valid ResetPasswordDto dto) {
      usersService.reset(dto);

return Result.success(200,"重置成功");

    }



    @DeleteMapping
    public Result<Void> delete() {
        usersService.delete();
        // service 是 void，删完直接回成功；Result<Void> 方法必须有返回值
        return Result.success();
    }

}


