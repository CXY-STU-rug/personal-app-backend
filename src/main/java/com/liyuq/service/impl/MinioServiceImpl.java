package com.liyuq.service.impl;


import com.liyuq.VO.VideoVo;
import com.liyuq.common.Exception.BusinessException;
import com.liyuq.service.MinioService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.liyuq.config.MinioProperties;


import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MinioServiceImpl implements MinioService {

    @Autowired
    private MinioClient minioClient;
    @Autowired private MinioProperties props;

    /**
     * 后缀 → Content-Type 的固定映射。
     * 绝不能直接用 file.getContentType()：那个值完全由上传方控制。
     * 攻击者可以传一个后缀是 .png、但 Content-Type 声明成 text/html 的文件，
     * MinIO 会照着存进去的类型原样返回，而 public-url 是直接暴露给浏览器的，
     * 浏览器拿到 text/html 就会当网页渲染并执行里面的脚本 → 存储型 XSS。
     * 后缀已经过白名单校验，所以这里 get 一定取得到值。
     */
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".jpg",  "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png",  "image/png",
            ".gif",  "image/gif",
            ".webp", "image/webp",
            ".mp4",  "video/mp4",
            ".mov",  "video/quicktime"
    );

    /**
     * 上传文件
     * @param file    前端传来的文件
     * @return 可访问的完整 URL
     */


    @Override
    public String upload(MultipartFile file) {
        // 1. 空校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException(500, "文件为空");
        }
        final long MAX_SIZE = 5 * 1024 * 1024L;
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(500, "文件大小不能超过5MB");
        }

        // 2. 构造 objectName: {bizType}/{今天}/{UUID}.{原扩展名}
        String fileName = file.getOriginalFilename();              // 取原文件名
        // getOriginalFilename() 是可能返回 null 的（客户端不带 filename 就会）。
        // 不先判空的话，下面 fileName.lastIndexOf 直接 NPE，构造个请求就能打出 500。
        // uploadVideo 里本来就有这道判空，这里是漏了，补齐。
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }
        int dotIndex = fileName.lastIndexOf(".");
        // 没有后缀直接拦截
        if (dotIndex == -1) {
            throw new BusinessException(500, "文件缺少后缀名称");
        }
        String suffix = fileName.substring(fileName.lastIndexOf("."));  // .jpg / .png
        //支持的后缀文件
        List<String> allowSuffix = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");
        if (!allowSuffix.contains(suffix.toLowerCase())) {
            throw new BusinessException(500, "仅支持jpg、png、gif、webp格式图片");
        }
        String objectName =  LocalDate.now() + "/" + UUID.randomUUID() + suffix;

        // 3. 流式上传 (try-with-resources 自动关 InputStream)
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectName)
                            .stream(is, file.getSize(), -1)         // -1 表示 partSize 自动
                            // 用后缀查表得到的固定类型，不用客户端声明的（见 CONTENT_TYPES 注释）
                            .contentType(CONTENT_TYPES.get(suffix.toLowerCase()))
                            .build()
            );
        } catch (Exception e) {
            throw new BusinessException(500, "上传失败: " + e.getMessage());
        }

        // 4. 拼完整 URL 返回给前端
        return props.getPublicUrl() + "/" + objectName;
    }

    @Override
    public VideoVo uploadVideo(MultipartFile file) {
        // 1. 空校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException(500, "文件为空");
        }
        final long MAX_SIZE = 100 * 1024 * 1024L;
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(500, "文件大小不能超过100MB");
        }
        String fileName = file.getOriginalFilename();// 取原文件名
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }

        int dotIndex = fileName.lastIndexOf(".");
        // 没有后缀直接拦截
        if (dotIndex == -1) {
            throw new BusinessException(500, "文件缺少后缀名称");
        }
        String suffix = fileName.substring(fileName.lastIndexOf("."));  // .jpg / .png
        //支持的后缀文件
        List<String> allowSuffix = Arrays.asList(".mp4",".mov");
        if (!allowSuffix.contains(suffix.toLowerCase())) {
            throw new BusinessException(400, "仅支持MP4和MOV格式视频上传");
        }


        String objectName =  LocalDate.now() + "/" + UUID.randomUUID() + suffix;

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(objectName)
                            .stream(is, file.getSize(), -1)         // -1 表示 partSize 自动
                            // 用后缀查表得到的固定类型，不用客户端声明的（见 CONTENT_TYPES 注释）
                            .contentType(CONTENT_TYPES.get(suffix.toLowerCase()))
                            .build()
            );
        } catch (Exception e) {
            throw new BusinessException(500, "上传失败: " + e.getMessage());
        }
VideoVo vo = new VideoVo();
        vo.setUrl(props.getPublicUrl() + "/" + objectName);
        vo.setDuration(null);

        return vo;
    }


}




