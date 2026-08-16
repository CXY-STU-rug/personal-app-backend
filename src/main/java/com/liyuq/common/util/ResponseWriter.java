package com.liyuq.common.util;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyuq.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class ResponseWriter {

    @Autowired
    private ObjectMapper objectMapper;

    public void writeResponse(HttpServletResponse response,Integer code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        Result<Void> result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        try {
            objectMapper.writeValue(response.getWriter(),result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }




}
