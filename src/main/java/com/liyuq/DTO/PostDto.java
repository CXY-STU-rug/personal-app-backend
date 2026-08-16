package com.liyuq.DTO;


import lombok.Data;

import java.util.List;

@Data
public class PostDto {
private String title;
private String content;
private Boolean isPublic;
private List<String>imageUrls;

    private String videoUrl;   // 可选，前端有视频才传
}
