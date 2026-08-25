package com.ruoyi.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ai.domain.AiParallelTask;
import com.ruoyi.ai.service.IAiParallelTaskService;

/**
 * 多模型并行生成 · AI 接口（/ai/parallel）
 * 创建并执行并行任务（调 LLM，属 AI 动作）；任务结果查询（纯数据）见 ruoyi-project 的 ParallelController（/portal/parallel）。
 * 注：modelIds 为 JSON 数组（元素为 ai_model_config.model_code），requestParams 为 JSON {systemPrompt, userPrompt}。
 * @author devpivot
 */
@RestController
@Validated
@RequestMapping("/ai/parallel")
public class AiParallelController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(AiParallelController.class);

    @Autowired
    private IAiParallelTaskService parallelTaskService;

    /** 创建并同步执行多模型并行任务，返回含各模型结果的任务对象 */
    @PostMapping("/run")
    public AjaxResult run(@RequestBody AiParallelTask task)
    {
        try
        {
            if (task == null || task.getProjectId() == null)
            {
                return error("项目ID不能为空");
            }
            if (StringUtils.isBlank(task.getModelIds()))
            {
                return error("请选择参与模型（modelIds 为模型标识数组）");
            }
            AiParallelTask done = parallelTaskService.executeParallelTask(task);
            return success(done);
        }
        catch (IllegalArgumentException e)
        {
            return error(e.getMessage());
        }
        catch (Exception e)
        {
            log.error("并行任务执行失败", e);
            return error("并行任务执行失败：" + e.getMessage());
        }
    }
}
