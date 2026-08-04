package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiParallelTaskMapper;
import com.ruoyi.ai.domain.AiParallelTask;
import com.ruoyi.ai.service.IAiParallelTaskService;

/**
 * 多模型并行任务Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiParallelTaskServiceImpl implements IAiParallelTaskService 
{
    @Autowired
    private AiParallelTaskMapper aiParallelTaskMapper;

    /**
     * 查询多模型并行任务
     * 
     * @param taskId 多模型并行任务主键
     * @return 多模型并行任务
     */
    @Override
    public AiParallelTask selectAiParallelTaskByTaskId(Long taskId)
    {
        return aiParallelTaskMapper.selectAiParallelTaskByTaskId(taskId);
    }

    /**
     * 查询多模型并行任务列表
     * 
     * @param aiParallelTask 多模型并行任务
     * @return 多模型并行任务
     */
    @Override
    public List<AiParallelTask> selectAiParallelTaskList(AiParallelTask aiParallelTask)
    {
        return aiParallelTaskMapper.selectAiParallelTaskList(aiParallelTask);
    }

    /**
     * 新增多模型并行任务
     * 
     * @param aiParallelTask 多模型并行任务
     * @return 结果
     */
    @Override
    public int insertAiParallelTask(AiParallelTask aiParallelTask)
    {
        aiParallelTask.setCreateTime(DateUtils.getNowDate());
        return aiParallelTaskMapper.insertAiParallelTask(aiParallelTask);
    }

    /**
     * 修改多模型并行任务
     * 
     * @param aiParallelTask 多模型并行任务
     * @return 结果
     */
    @Override
    public int updateAiParallelTask(AiParallelTask aiParallelTask)
    {
        aiParallelTask.setUpdateTime(DateUtils.getNowDate());
        return aiParallelTaskMapper.updateAiParallelTask(aiParallelTask);
    }

    /**
     * 批量删除多模型并行任务
     * 
     * @param taskIds 需要删除的多模型并行任务主键
     * @return 结果
     */
    @Override
    public int deleteAiParallelTaskByTaskIds(Long[] taskIds)
    {
        return aiParallelTaskMapper.deleteAiParallelTaskByTaskIds(taskIds);
    }

    /**
     * 删除多模型并行任务信息
     * 
     * @param taskId 多模型并行任务主键
     * @return 结果
     */
    @Override
    public int deleteAiParallelTaskByTaskId(Long taskId)
    {
        return aiParallelTaskMapper.deleteAiParallelTaskByTaskId(taskId);
    }
}
