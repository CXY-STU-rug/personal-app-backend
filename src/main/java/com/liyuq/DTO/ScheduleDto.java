package com.liyuq.DTO;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleDto {
private  String title;
private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime endTime;
private boolean force;   // 基本类型boolean不传默认false，正好符合契约"默认正常检测"


}
