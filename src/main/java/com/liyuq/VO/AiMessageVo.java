package com.liyuq.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话历史的一条消息（接口文档3.29返回数组的元素）
 * 为什么不直接返回ai_messages实体：实体里有userId，页面不展示的不外传——和TodosVo同理
 */
@Data
public class AiMessageVo {

    private Long id;          // 自增id天然就是时间顺序，前端按数组顺序渲染即可

    private String role;      // user=用户说的 / assistant=AI回的，前端靠它决定气泡靠左还是靠右

    private String content;   // 消息正文

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")   // 输出方向：按文档承诺的格式返回，去掉ISO的T
    private LocalDateTime createdAt;
}
