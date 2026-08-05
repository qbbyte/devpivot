package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.AiVersionRecord;

/**
 * 版本全链路记录Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface AiVersionRecordMapper 
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
     * 删除版本全链路记录
     * 
     * @param recordId 版本全链路记录主键
     * @return 结果
     */
    public int deleteAiVersionRecordByRecordId(Long recordId);

    /**
     * 批量删除版本全链路记录
     * 
     * @param recordIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiVersionRecordByRecordIds(Long[] recordIds);
}
