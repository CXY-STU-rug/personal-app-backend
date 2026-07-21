package com.liyuq.common.Exception;

import lombok.Data;

@Data    // @Data生成getCode、getMsg
public class BusinessException extends RuntimeException {
    private Integer code;
    private String msg;

    public BusinessException(Integer code, String msg) {

        this.code = code;
        this.msg = msg;
    }
}
