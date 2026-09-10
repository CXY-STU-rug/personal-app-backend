package com.liyuq.common.Exception;
import com.liyuq.VO.ScheduleVo;
import com.liyuq.common.Result;
import org.springframework.http.converter.HttpMessageNotReadableException;   // 请求体读不动时抛的
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;     // 文件超 multipart 上限时抛的
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;


@ControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public Result<Void> handleBusinessEx(BusinessException e) {
        // @Data生成getCode、getMsg
        return Result.fail(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseBody   // 不加就不走JSON序列化，Result会被当成视图名去找HTML——同第29行注释里记的坑
    public Result<List<ScheduleVo>> handleConflictEx(ConflictException e) {
        return Result.fail(e.getCode(), e.getMsg(),e.getData());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody   // 类上是 @ControllerAdvice 不是 @RestControllerAdvice，每个方法都得自己加，漏了就走视图解析
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        // getBindingResult().getFieldError() 拿第一个校验失败的字段，
        // getDefaultMessage() 就是你在注解 message 里写的那句
        // getBindingResult() 拿到所有校验结果，getFieldError() 取第一个出错的字段
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = (fieldError==null)?"":fieldError.getDefaultMessage();
        return Result.fail(400, msg);
    }

    // ⭐ 新增：文件超过 multipart 上限
    // 不接这个的话会掉进最下面的 Exception 兜底，用户看到"系统繁忙，请稍后再试"——
    // 这是最没用的提示：他以为是服务器坏了，其实只要换张小点的图就行。
    // 属于用户传错东西，所以是 400 不是 500。
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody   // 同上：类上是 @ControllerAdvice，每个方法都得自己加
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return Result.fail(400, "文件太大，图片不超过5MB、视频不超过100MB");
    }

    // ⭐ 新增：请求体解析失败（JSON 语法错、字段类型对不上、编码不是合法 UTF-8）
    // 之前用 curl 发 GBK 编码的中文时就撞到过：Jackson 解析失败 → 掉进 Exception 兜底 → 500，
    // 让人误以为是后端崩了。请求体是客户端发的，发错了就该 400。
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(400, "请求参数格式错误");
    }

    //打印日志的log静态对象，通过日志工厂获取，然后写上需要打印的类
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(value = Exception.class)
    @ResponseBody//不加这个异常不会解析成json,就不会按照这个处理方法去执行，而是去找视图，Spring 会把返回的 Result 对象当成"视图名"去找一个 HTML 页面来渲染——找不到，于是异常处理器自己又抛异常。效果是：业务异常能正常返回 JSON，但真正的系统异常（500 那条路）反而会炸，而这恰恰是最需要兜底的路
    public Result<Void> handleException(Exception e) {
       log.error(e.getMessage(), e);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }


}
