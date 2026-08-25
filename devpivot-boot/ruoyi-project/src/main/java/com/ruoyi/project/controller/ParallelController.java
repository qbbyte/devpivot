package com.ruoyi.project.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.ai.domain.AiParallelTask;
import com.ruoyi.ai.service.IAiParallelTaskService;

/**
 * 门户·并行任务数据接口（/portal/parallel，仅登录态）
 * 承载并行任务结果的读取；任务创建/执行（调 LLM）见 ruoyi-ai 的 AiParallelController（/ai/parallel/run）。
 * @author devpivot
 */
@RestController
@RequestMapping("/portal/parallel")
public class ParallelController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(ParallelController.class);

    @Autowired
    private IAiParallelTaskService parallelTaskService;

    /** 查询并行任务详情（含 resultSummary=各模型结果 JSON Map） */
    @GetMapping("/{taskId}")
    public AjaxResult get(@PathVariable Long taskId)
    {
        return success(parallelTaskService.selectAiParallelTaskByTaskId(taskId));
    }
}
