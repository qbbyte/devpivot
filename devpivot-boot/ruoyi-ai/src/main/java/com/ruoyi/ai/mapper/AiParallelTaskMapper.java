package com.ruoyi.ai.mapper;

import java.util.List;
import com.ruoyi.ai.domain.AiParallelTask;

/**
 * 多模型并行任务Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface AiParallelTaskMapper 
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
     * 删除多模型并行任务
     * 
     * @param taskId 多模型并行任务主键
     * @return 结果
     */
    public int deleteAiParallelTaskByTaskId(Long taskId);

    /**
     * 批量删除多模型并行任务
     * 
     * @param taskIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiParallelTaskByTaskIds(Long[] taskIds);
}
