package com.liyuq.service;


import com.liyuq.VO.VideoVo;
import org.springframework.web.multipart.MultipartFile;

public interface MinioService {
    String upload(MultipartFile file);

    VideoVo uploadVideo(MultipartFile file);
}
