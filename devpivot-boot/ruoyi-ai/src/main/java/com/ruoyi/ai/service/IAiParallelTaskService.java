package com.ruoyi.ai.service;

import java.util.List;
import com.ruoyi.ai.domain.AiParallelTask;

/**
 * 多模型并行任务Service接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface IAiParallelTaskService 
{
    /**
     * 查询多模型并行任务
     * 
     * @param taskId 多模型并行任务主键
     * @return 多模型并行任务
     */
    public AiParallelTask selectAiParallelTaskByTaskId(Long taskId);

    /**
     * 查询多模型并行任务列表
     * 
     * @param aiParallelTask 多模型并行任务
     * @return 多模型并行任务集合
     */
    public List<AiParallelTask> selectAiParallelTaskList(AiParallelTask aiParallelTask);

    /**
     * 新增多模型并行任务
     * 
     * @param aiParallelTask 多模型并行任务
     * @return 结果
     */
    public int insertAiParallelTask(AiParallelTask aiParallelTask);

    /**
     * 修改多模型并行任务
     * 
     * @param aiParallelTask 多模型并行任务
     * @return 结果
     */
    public int updateAiParallelTask(AiParallelTask aiParallelTask);

    /**
     * 批量删除多模型并行任务
     * 
     * @param taskIds 需要删除的多模型并行任务主键集合
     * @return 结果
     */
    public int deleteAiParallelTaskByTaskIds(Long[] taskIds);

    /**
     * 删除多模型并行任务信息
     * 
     * @param taskId 多模型并行任务主键
     * @return 结果
     */
    public int deleteAiParallelTaskByTaskId(Long taskId);

    /**
     * 创建并同步执行多模型并行任务：
     * 解析 modelIds(JSON 数组，元素为 model_code) 与 requestParams(JSON：{systemPrompt, userPrompt})，
     * 并发调用各模型，结果按模型写入 resultSummary(JSON Map)，status 流转 0→1（2 部分失败/3 失败暂未细分）。
     *
     * @param task 待执行任务（projectId/taskType/modelIds/requestParams 必填）
     * @return 执行完成后的任务（含 taskId 与 resultSummary）
     */
    public AiParallelTask executeParallelTask(AiParallelTask task);
}
