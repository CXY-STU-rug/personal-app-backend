package com.liyuq.DTO;


import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ListRecordDto {
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;   // Spring会把query里的"2026-07-01"自动转成LocalDate
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;     // 声明成什么类型，Spring就帮你转成什么类型
    private Integer type;          // 类型筛选（选填）：1支出/2收入，不传则不按类型过滤
}
