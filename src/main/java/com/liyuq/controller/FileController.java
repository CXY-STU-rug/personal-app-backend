package com.liyuq.controller;


import com.liyuq.VO.VideoVo;
import com.liyuq.common.Result;
import com.liyuq.service.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileController {
    // MinioProperties 原本只被上面删掉的那行用到，一起去掉，controller 不需要知道存储细节
    @Autowired
    private MinioService minioService;


    @PostMapping("/image")
    public Result<String> upload(
            @RequestParam("file") MultipartFile file) {
          String fileUrl=   minioService.upload(file);
          // 原来这里还算了一个 objectName 但从没用过，删掉；service 返回的已经是完整可访问 URL
          return Result.success(fileUrl);

    }
    @PostMapping("/video")
    public Result<VideoVo> uploadVideo( @RequestParam("file") MultipartFile file) {
    VideoVo vo =   minioService.uploadVideo(file);

        return Result.success(vo);
    }


}
