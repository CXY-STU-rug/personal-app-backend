package com.liyuq.service;

import com.liyuq.DTO.AiChatDto;
import com.liyuq.VO.AiChatVo;
import com.liyuq.VO.AiMessageVo;
import com.liyuq.VO.AiModelVo;
import java.util.List;

/**
 * AI助手模块的业务接口（契约3.28~3.30）
 * 3.28的chat方法先不声明——台阶①到了再加，只建马上要用的
 */
public interface AiService {

    /**
     * 契约3.29：拉最近的对话历史（打开聊天页回填用）
     * @param limit 要几条；null→默认20，上限100
     * @return 最近limit条，按id升序（越往下越新，聊天窗的阅读顺序）
     */
    List<AiMessageVo> history(Integer limit);

    /**
     * 契约3.30：清空当前用户的全部对话
     * 用户身份从UserContext取，不收参数
     */
    void clearHistory();

    AiChatVo chat(AiChatDto dto);

    /** 契约3.31：返回后端配置的可用模型清单（写死，不查库） */
    List<AiModelVo> models();
}
