package com.liyuq.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class ListFinanceRecordVo {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Integer type;
    private double amount;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
