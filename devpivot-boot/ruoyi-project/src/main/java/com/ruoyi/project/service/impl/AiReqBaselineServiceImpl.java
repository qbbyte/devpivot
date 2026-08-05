package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.AiReqBaselineMapper;
import com.ruoyi.project.domain.AiReqBaseline;
import com.ruoyi.project.service.IAiReqBaselineService;

/**
 * 需求基线Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiReqBaselineServiceImpl implements IAiReqBaselineService 
{
    @Autowired
    private AiReqBaselineMapper aiReqBaselineMapper;

    /**
     * 查询需求基线
     * 
     * @param baselineId 需求基线主键
     * @return 需求基线
     */
    @Override
    public AiReqBaseline selectAiReqBaselineByBaselineId(Long baselineId)
    {
        return aiReqBaselineMapper.selectAiReqBaselineByBaselineId(baselineId);
    }

    /**
     * 查询需求基线列表
     * 
     * @param aiReqBaseline 需求基线
     * @return 需求基线
     */
    @Override
    public List<AiReqBaseline> selectAiReqBaselineList(AiReqBaseline aiReqBaseline)
    {
        return aiReqBaselineMapper.selectAiReqBaselineList(aiReqBaseline);
    }

    /**
     * 新增需求基线
     * 
     * @param aiReqBaseline 需求基线
     * @return 结果
     */
    @Override
    public int insertAiReqBaseline(AiReqBaseline aiReqBaseline)
    {
        aiReqBaseline.setCreateTime(DateUtils.getNowDate());
        return aiReqBaselineMapper.insertAiReqBaseline(aiReqBaseline);
    }

    /**
     * 修改需求基线
     * 
     * @param aiReqBaseline 需求基线
     * @return 结果
     */
    @Override
    public int updateAiReqBaseline(AiReqBaseline aiReqBaseline)
    {
        aiReqBaseline.setUpdateTime(DateUtils.getNowDate());
        return aiReqBaselineMapper.updateAiReqBaseline(aiReqBaseline);
    }

    /**
     * 批量删除需求基线
     * 
     * @param baselineIds 需要删除的需求基线主键
     * @return 结果
     */
    @Override
    public int deleteAiReqBaselineByBaselineIds(Long[] baselineIds)
    {
        return aiReqBaselineMapper.deleteAiReqBaselineByBaselineIds(baselineIds);
    }

    /**
     * 删除需求基线信息
     * 
     * @param baselineId 需求基线主键
     * @return 结果
     */
    @Override
    public int deleteAiReqBaselineByBaselineId(Long baselineId)
    {
        return aiReqBaselineMapper.deleteAiReqBaselineByBaselineId(baselineId);
    }
}
