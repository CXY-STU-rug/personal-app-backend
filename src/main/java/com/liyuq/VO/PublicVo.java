package com.liyuq.VO;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublicVo {
    private Long id;

    private String title;

    private Long authorId;

    private  String authorName;

    private  String coverUrl;

    private  String videoUrl;   // 视频URL，列表页前端有值就渲染视频、无值渲染封面图

    // 字段名对齐文档 3.36 的 likeCount（原来叫 likecount，JSON key 会错）
    private  Integer likeCount;

    // 字段名对齐文档的 createdAt，并加时间格式化，否则 LocalDateTime 会被序列化成数组/带 T 的 ISO 串
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    private Integer commentCount;

    // 名次：只有热榜接口用得到，别的接口返回时它是 null
    @JsonInclude(JsonInclude.Include.NON_NULL)   // 为 null 时不出现在 JSON 里，免得污染 3.36 列表
    private Integer rank;



}
