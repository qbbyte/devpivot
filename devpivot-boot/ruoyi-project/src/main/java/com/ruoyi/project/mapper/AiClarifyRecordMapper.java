package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.AiClarifyRecord;

/**
 * AI澄清问题记录Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface AiClarifyRecordMapper 
{
    /**
     * 查询AI澄清问题记录
     * 
     * @param recordId AI澄清问题记录主键
     * @return AI澄清问题记录
     */
    public AiClarifyRecord selectAiClarifyRecordByRecordId(Long recordId);

    /**
     * 查询AI澄清问题记录列表
     * 
     * @param aiClarifyRecord AI澄清问题记录
     * @return AI澄清问题记录集合
     */
    public List<AiClarifyRecord> selectAiClarifyRecordList(AiClarifyRecord aiClarifyRecord);

    /**
     * 新增AI澄清问题记录
     * 
     * @param aiClarifyRecord AI澄清问题记录
     * @return 结果
     */
    public int insertAiClarifyRecord(AiClarifyRecord aiClarifyRecord);

    /**
     * 修改AI澄清问题记录
     * 
     * @param aiClarifyRecord AI澄清问题记录
     * @return 结果
     */
    public int updateAiClarifyRecord(AiClarifyRecord aiClarifyRecord);

    /**
     * 删除AI澄清问题记录
     * 
     * @param recordId AI澄清问题记录主键
     * @return 结果
     */
    public int deleteAiClarifyRecordByRecordId(Long recordId);

    /**
     * 批量删除AI澄清问题记录
     * 
     * @param recordIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiClarifyRecordByRecordIds(Long[] recordIds);
}
