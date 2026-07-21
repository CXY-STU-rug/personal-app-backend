package com.liyuq.common.Exception;
import com.liyuq.VO.ScheduleVo;
import com.liyuq.common.Result;
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

    //打印日志的log静态对象，通过日志工厂获取，然后写上需要打印的类
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(value = Exception.class)
    @ResponseBody//不加这个异常不会解析成json,就不会按照这个处理方法去执行，而是去找视图，Spring 会把返回的 Result 对象当成"视图名"去找一个 HTML 页面来渲染——找不到，于是异常处理器自己又抛异常。效果是：业务异常能正常返回 JSON，但真正的系统异常（500 那条路）反而会炸，而这恰恰是最需要兜底的路
    public Result<Void> handleException(Exception e) {
       log.error(e.getMessage(), e);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}
