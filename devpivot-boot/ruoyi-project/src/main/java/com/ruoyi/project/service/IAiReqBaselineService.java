package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.AiReqBaseline;

/**
 * 需求基线Service接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface IAiReqBaselineService 
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
     * 批量删除需求基线
     * 
     * @param baselineIds 需要删除的需求基线主键集合
     * @return 结果
     */
    public int deleteAiReqBaselineByBaselineIds(Long[] baselineIds);

    /**
     * 删除需求基线信息
     * 
     * @param baselineId 需求基线主键
     * @return 结果
     */
    public int deleteAiReqBaselineByBaselineId(Long baselineId);
}
