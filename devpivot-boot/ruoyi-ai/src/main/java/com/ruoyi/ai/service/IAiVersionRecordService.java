package com.ruoyi.ai.service;

import java.util.List;
import com.ruoyi.ai.domain.AiVersionRecord;

/**
 * 版本全链路记录Service接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface IAiVersionRecordService 
{
    /**
     * 查询版本全链路记录
     * 
     * @param recordId 版本全链路记录主键
     * @return 版本全链路记录
     */
    public AiVersionRecord selectAiVersionRecordByRecordId(Long recordId);

    /**
     * 查询版本全链路记录列表
     * 
     * @param aiVersionRecord 版本全链路记录
     * @return 版本全链路记录集合
     */
    public List<AiVersionRecord> selectAiVersionRecordList(AiVersionRecord aiVersionRecord);

    /**
     * 新增版本全链路记录
     * 
     * @param aiVersionRecord 版本全链路记录
     * @return 结果
     */
    public int insertAiVersionRecord(AiVersionRecord aiVersionRecord);

    /**
     * 修改版本全链路记录
     * 
     * @param aiVersionRecord 版本全链路记录
     * @return 结果
     */
    public int updateAiVersionRecord(AiVersionRecord aiVersionRecord);

    /**
     * 批量删除版本全链路记录
     * 
     * @param recordIds 需要删除的版本全链路记录主键集合
     * @return 结果
     */
    public int deleteAiVersionRecordByRecordIds(Long[] recordIds);

    /**
     * 删除版本全链路记录信息
     * 
     * @param recordId 版本全链路记录主键
     * @return 结果
     */
    public int deleteAiVersionRecordByRecordId(Long recordId);
}
