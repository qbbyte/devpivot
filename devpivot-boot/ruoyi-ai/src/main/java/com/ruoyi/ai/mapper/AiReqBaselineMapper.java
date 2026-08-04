package com.ruoyi.ai.mapper;

import java.util.List;
import com.ruoyi.ai.domain.AiReqBaseline;

/**
 * 需求基线Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface AiReqBaselineMapper 
{
    /**
     * 查询需求基线
     * 
     * @param baselineId 需求基线主键
     * @return 需求基线
     */
    public AiReqBaseline selectAiReqBaselineByBaselineId(Long baselineId);

    /**
     * 查询需求基线列表
     * 
     * @param aiReqBaseline 需求基线
     * @return 需求基线集合
     */
    public List<AiReqBaseline> selectAiReqBaselineList(AiReqBaseline aiReqBaseline);

    /**
     * 新增需求基线
     * 
     * @param aiReqBaseline 需求基线
     * @return 结果
     */
    public int insertAiReqBaseline(AiReqBaseline aiReqBaseline);

    /**
     * 修改需求基线
     * 
     * @param aiReqBaseline 需求基线
     * @return 结果
     */
    public int updateAiReqBaseline(AiReqBaseline aiReqBaseline);

    /**
     * 删除需求基线
     * 
     * @param baselineId 需求基线主键
     * @return 结果
     */
    public int deleteAiReqBaselineByBaselineId(Long baselineId);

    /**
     * 批量删除需求基线
     * 
     * @param baselineIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiReqBaselineByBaselineIds(Long[] baselineIds);
}
