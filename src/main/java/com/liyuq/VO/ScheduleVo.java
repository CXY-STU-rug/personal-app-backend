package com.liyuq.VO;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleVo {
    private Long id;
    private String title;
    private String remark;   // 日视图(3.23)要展示备注；冲突列表(3.24)带上也无害，前端不用就忽略
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

}
