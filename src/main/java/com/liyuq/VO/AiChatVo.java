package com.liyuq.VO;

import lombok.Data;

import java.util.List;

/**
 * AI对话返回体（接口文档3.28的data结构）
 * 和DayViewVo同一个套路：字段名reply/actions就是前端的取值路径
 */
@Data
public class AiChatVo {

    private String reply;               // AI的最终回复文字，前端直接渲染到聊天气泡

    private List<AiActionVo> actions;   // 本次实际执行的写操作列表；纯聊天时给空数组[]，不给null
                                        // （和CalendarVo垫0是同一条原则：契约承诺的结构要显式给出来）
}
