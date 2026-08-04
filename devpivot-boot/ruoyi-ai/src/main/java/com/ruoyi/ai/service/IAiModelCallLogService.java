package com.ruoyi.ai.service;

import java.util.List;
import com.ruoyi.ai.domain.AiModelCallLog;

/**
 * AI模型调用日志Service接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface IAiModelCallLogService 
{
    /**
     * 查询AI模型调用日志
     * 
     * @param logId AI模型调用日志主键
     * @return AI模型调用日志
     */
    public AiModelCallLog selectAiModelCallLogByLogId(Long logId);

    /**
     * 查询AI模型调用日志列表
     * 
     * @param aiModelCallLog AI模型调用日志
     * @return AI模型调用日志集合
     */
    public List<AiModelCallLog> selectAiModelCallLogList(AiModelCallLog aiModelCallLog);

    /**
     * 新增AI模型调用日志
     * 
     * @param aiModelCallLog AI模型调用日志
     * @return 结果
     */
    public int insertAiModelCallLog(AiModelCallLog aiModelCallLog);

    /**
     * 修改AI模型调用日志
     * 
     * @param aiModelCallLog AI模型调用日志
     * @return 结果
     */
    public int updateAiModelCallLog(AiModelCallLog aiModelCallLog);

    /**
     * 批量删除AI模型调用日志
     * 
     * @param logIds 需要删除的AI模型调用日志主键集合
     * @return 结果
     */
    public int deleteAiModelCallLogByLogIds(Long[] logIds);

    /**
     * 删除AI模型调用日志信息
     * 
     * @param logId AI模型调用日志主键
     * @return 结果
     */
    public int deleteAiModelCallLogByLogId(Long logId);
}
