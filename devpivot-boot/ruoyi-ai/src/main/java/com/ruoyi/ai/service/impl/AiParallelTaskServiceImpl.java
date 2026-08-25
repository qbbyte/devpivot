package com.ruoyi.ai.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.ai.service.AiModelClient;
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

    @Autowired
    private AiModelClient aiModelClient;

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

    /**
     * 创建并同步执行并行任务：解析 modelIds(JSON 数组，元素为 model_code) 与 requestParams(JSON：{systemPrompt, userPrompt})，
     * 并发调用各模型（AiModelClient.chat 自带用户 Key 覆盖与兜底文案），结果按模型写入 resultSummary(JSON Map)，status 0→1。
     */
    @Override
    public AiParallelTask executeParallelTask(AiParallelTask task)
    {
        if (task == null || task.getProjectId() == null)
        {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        if (StringUtils.isBlank(task.getModelIds()))
        {
            throw new IllegalArgumentException("请选择参与模型");
        }
        JSONArray modelArr = JSON.parseArray(task.getModelIds());
        if (modelArr == null || modelArr.isEmpty())
        {
            throw new IllegalArgumentException("参与模型列表为空");
        }
        JSONObject params = StringUtils.isNotBlank(task.getRequestParams())
                ? JSON.parseObject(task.getRequestParams()) : new JSONObject();
        String systemPrompt = params.getString("systemPrompt");
        String userPrompt = params.getString("userPrompt");
        if (StringUtils.isBlank(userPrompt))
        {
            throw new IllegalArgumentException("生成指令(userPrompt)不能为空");
        }

        // 落库：运行中
        task.setStatus("0");
        task.setCreateTime(DateUtils.getNowDate());
        aiParallelTaskMapper.insertAiParallelTask(task);

        // 并发调用各模型（线程数不超过模型数，上限 4）
        Map<String, String> results = new LinkedHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(modelArr.size(), 4));
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < modelArr.size(); i++)
        {
            String modelCode = String.valueOf(modelArr.getString(i)).trim();
            if (StringUtils.isBlank(modelCode))
            {
                continue;
            }
            futures.add(CompletableFuture.runAsync(() -> {
                String text = aiModelClient.chat(modelCode, systemPrompt, userPrompt);
                synchronized (results)
                {
                    results.put(modelCode, text);
                }
            }, pool));
        }
        try
        {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        finally
        {
            pool.shutdown();
        }

        // 汇总结果并落库（AiModelClient.chat 不抛异常，单模型失败会返回兜底文案，故此处视为整体完成）
        task.setResultSummary(JSON.toJSONString(results));
        task.setStatus("1");
        task.setTotalTokens(0L);
        task.setUpdateTime(DateUtils.getNowDate());
        aiParallelTaskMapper.updateAiParallelTask(task);
        return task;
    }
}
