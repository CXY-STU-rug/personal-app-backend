package com.liyuq.VO;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class postDetailVo {
    private Long id;

    private String title;

    private String content;

    private Long authorId;

    private String authorName;

    private List<String> images;

    private String videoUrl;   // 视频URL，有值前端渲染播放器、无值渲染图片（和 images 二选一）

    private Integer likeCount;

    private Integer commentCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;



}
