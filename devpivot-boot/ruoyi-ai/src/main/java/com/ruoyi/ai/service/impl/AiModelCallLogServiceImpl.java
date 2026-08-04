package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiModelCallLogMapper;
import com.ruoyi.ai.domain.AiModelCallLog;
import com.ruoyi.ai.service.IAiModelCallLogService;

/**
 * AI模型调用日志Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiModelCallLogServiceImpl implements IAiModelCallLogService 
{
    @Autowired
    private AiModelCallLogMapper aiModelCallLogMapper;

    /**
     * 查询AI模型调用日志
     * 
     * @param logId AI模型调用日志主键
     * @return AI模型调用日志
     */
    @Override
    public AiModelCallLog selectAiModelCallLogByLogId(Long logId)
    {
        return aiModelCallLogMapper.selectAiModelCallLogByLogId(logId);
    }

    /**
     * 查询AI模型调用日志列表
     * 
     * @param aiModelCallLog AI模型调用日志
     * @return AI模型调用日志
     */
    @Override
    public List<AiModelCallLog> selectAiModelCallLogList(AiModelCallLog aiModelCallLog)
    {
        return aiModelCallLogMapper.selectAiModelCallLogList(aiModelCallLog);
    }

    /**
     * 新增AI模型调用日志
     * 
     * @param aiModelCallLog AI模型调用日志
     * @return 结果
     */
    @Override
    public int insertAiModelCallLog(AiModelCallLog aiModelCallLog)
    {
        aiModelCallLog.setCreateTime(DateUtils.getNowDate());
        return aiModelCallLogMapper.insertAiModelCallLog(aiModelCallLog);
    }

    /**
     * 修改AI模型调用日志
     * 
     * @param aiModelCallLog AI模型调用日志
     * @return 结果
     */
    @Override
    public int updateAiModelCallLog(AiModelCallLog aiModelCallLog)
    {
        return aiModelCallLogMapper.updateAiModelCallLog(aiModelCallLog);
    }

    /**
     * 批量删除AI模型调用日志
     * 
     * @param logIds 需要删除的AI模型调用日志主键
     * @return 结果
     */
    @Override
    public int deleteAiModelCallLogByLogIds(Long[] logIds)
    {
        return aiModelCallLogMapper.deleteAiModelCallLogByLogIds(logIds);
    }

    /**
     * 删除AI模型调用日志信息
     * 
     * @param logId AI模型调用日志主键
     * @return 结果
     */
    @Override
    public int deleteAiModelCallLogByLogId(Long logId)
    {
        return aiModelCallLogMapper.deleteAiModelCallLogByLogId(logId);
    }
}
