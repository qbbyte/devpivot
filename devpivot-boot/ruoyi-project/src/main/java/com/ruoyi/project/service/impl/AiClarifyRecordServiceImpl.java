package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.AiClarifyRecordMapper;
import com.ruoyi.project.domain.AiClarifyRecord;
import com.ruoyi.project.service.IAiClarifyRecordService;

/**
 * AI澄清问题记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiClarifyRecordServiceImpl implements IAiClarifyRecordService 
{
    @Autowired
    private AiClarifyRecordMapper aiClarifyRecordMapper;

    /**
     * 查询AI澄清问题记录
     * 
     * @param recordId AI澄清问题记录主键
     * @return AI澄清问题记录
     */
    @Override
    public AiClarifyRecord selectAiClarifyRecordByRecordId(Long recordId)
    {
        return aiClarifyRecordMapper.selectAiClarifyRecordByRecordId(recordId);
    }

    /**
     * 查询AI澄清问题记录列表
     * 
     * @param aiClarifyRecord AI澄清问题记录
     * @return AI澄清问题记录
     */
    @Override
    public List<AiClarifyRecord> selectAiClarifyRecordList(AiClarifyRecord aiClarifyRecord)
    {
        return aiClarifyRecordMapper.selectAiClarifyRecordList(aiClarifyRecord);
    }

    /**
     * 新增AI澄清问题记录
     * 
     * @param aiClarifyRecord AI澄清问题记录
     * @return 结果
     */
    @Override
    public int insertAiClarifyRecord(AiClarifyRecord aiClarifyRecord)
    {
        aiClarifyRecord.setCreateTime(DateUtils.getNowDate());
        return aiClarifyRecordMapper.insertAiClarifyRecord(aiClarifyRecord);
    }

    /**
     * 修改AI澄清问题记录
     * 
     * @param aiClarifyRecord AI澄清问题记录
     * @return 结果
     */
    @Override
    public int updateAiClarifyRecord(AiClarifyRecord aiClarifyRecord)
    {
        aiClarifyRecord.setUpdateTime(DateUtils.getNowDate());
        return aiClarifyRecordMapper.updateAiClarifyRecord(aiClarifyRecord);
    }

    /**
     * 批量删除AI澄清问题记录
     * 
     * @param recordIds 需要删除的AI澄清问题记录主键
     * @return 结果
     */
    @Override
    public int deleteAiClarifyRecordByRecordIds(Long[] recordIds)
    {
        return aiClarifyRecordMapper.deleteAiClarifyRecordByRecordIds(recordIds);
    }

    /**
     * 删除AI澄清问题记录信息
     * 
     * @param recordId AI澄清问题记录主键
     * @return 结果
     */
    @Override
    public int deleteAiClarifyRecordByRecordId(Long recordId)
    {
        return aiClarifyRecordMapper.deleteAiClarifyRecordByRecordId(recordId);
    }
}
