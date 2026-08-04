package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiVersionRecordMapper;
import com.ruoyi.ai.domain.AiVersionRecord;
import com.ruoyi.ai.service.IAiVersionRecordService;

/**
 * 版本全链路记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiVersionRecordServiceImpl implements IAiVersionRecordService 
{
    @Autowired
    private AiVersionRecordMapper aiVersionRecordMapper;

    /**
     * 查询版本全链路记录
     * 
     * @param recordId 版本全链路记录主键
     * @return 版本全链路记录
     */
    @Override
    public AiVersionRecord selectAiVersionRecordByRecordId(Long recordId)
    {
        return aiVersionRecordMapper.selectAiVersionRecordByRecordId(recordId);
    }

    /**
     * 查询版本全链路记录列表
     * 
     * @param aiVersionRecord 版本全链路记录
     * @return 版本全链路记录
     */
    @Override
    public List<AiVersionRecord> selectAiVersionRecordList(AiVersionRecord aiVersionRecord)
    {
        return aiVersionRecordMapper.selectAiVersionRecordList(aiVersionRecord);
    }

    /**
     * 新增版本全链路记录
     * 
     * @param aiVersionRecord 版本全链路记录
     * @return 结果
     */
    @Override
    public int insertAiVersionRecord(AiVersionRecord aiVersionRecord)
    {
        aiVersionRecord.setCreateTime(DateUtils.getNowDate());
        return aiVersionRecordMapper.insertAiVersionRecord(aiVersionRecord);
    }

    /**
     * 修改版本全链路记录
     * 
     * @param aiVersionRecord 版本全链路记录
     * @return 结果
     */
    @Override
    public int updateAiVersionRecord(AiVersionRecord aiVersionRecord)
    {
        aiVersionRecord.setUpdateTime(DateUtils.getNowDate());
        return aiVersionRecordMapper.updateAiVersionRecord(aiVersionRecord);
    }

    /**
     * 批量删除版本全链路记录
     * 
     * @param recordIds 需要删除的版本全链路记录主键
     * @return 结果
     */
    @Override
    public int deleteAiVersionRecordByRecordIds(Long[] recordIds)
    {
        return aiVersionRecordMapper.deleteAiVersionRecordByRecordIds(recordIds);
    }

    /**
     * 删除版本全链路记录信息
     * 
     * @param recordId 版本全链路记录主键
     * @return 结果
     */
    @Override
    public int deleteAiVersionRecordByRecordId(Long recordId)
    {
        return aiVersionRecordMapper.deleteAiVersionRecordByRecordId(recordId);
    }
}
