package com.liyuq.controller;

import com.liyuq.DTO.AiChatDto;
import com.liyuq.VO.AiChatVo;
import com.liyuq.VO.AiMessageVo;
import com.liyuq.VO.AiModelVo;
import com.liyuq.common.Result;
import com.liyuq.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI助手模块（契约3.28~3.30）
 * 分层职责同TodosController：收参数、调service、包装Result，业务全在service层。
 * 3.28的chat接口台阶①时再加——只建马上要用的。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * 3.29 拉对话历史：GET /api/ai/history?limit=20
     * 注意和3.30共用同一路径/history——URL表示资源（对话历史），HTTP方法表示动作（GET拿/DELETE删）
     */
    @GetMapping("/history")
    public Result<List<AiMessageVo>> history(@RequestParam(required = false) Integer limit) {
        // limit可不传，默认值20的兜底逻辑在service里（参数规则是业务的一部分，不散落在controller）
        return Result.success(aiService.history(limit));
    }

    /**
     * 3.30 清空对话：DELETE /api/ai/history
     * 无参——删谁的数据由token说了算，永远不收前端指定
     */
    @DeleteMapping("/history")
    public Result<Void> clearHistory() {
        aiService.clearHistory();
        return Result.success(null);   // 合同：成功返回data:null
    }

    @PostMapping("/chat")
    public Result<AiChatVo> chat(@RequestBody AiChatDto dto) {
        AiChatVo aiChatVo = aiService.chat(dto);
        return Result.success(aiChatVo);
    }

    /**
     * 3.31 模型列表：GET /api/ai/models
     * 前端模型切换器拉这个——有哪些模型可选由后端返回，前端不写死
     */
    @GetMapping("/models")
    public Result<List<AiModelVo>> models() {
        return Result.success(aiService.models());
    }
}
