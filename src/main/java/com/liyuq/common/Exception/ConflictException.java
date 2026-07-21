package com.liyuq.common.Exception;


import com.liyuq.VO.ScheduleVo;
import lombok.Data;

import java.util.List;

@Data
public class ConflictException extends RuntimeException {

    private Integer code;

    private String msg;

    private List<ScheduleVo> data;
    public ConflictException(Integer code, String msg, List<ScheduleVo> data) {   // 参数和字段的泛型对齐，消掉unchecked警告
        this.code = code;
        this.msg = msg;
        this.data = data;
    }


}
