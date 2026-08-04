package com.ruoyi.ai.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.ai.mapper.AiProtoPageMapper;
import com.ruoyi.ai.domain.AiProtoPage;
import com.ruoyi.ai.service.IAiProtoPageService;

/**
 * 原型页面Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
@Service
public class AiProtoPageServiceImpl implements IAiProtoPageService 
{
    @Autowired
    private AiProtoPageMapper aiProtoPageMapper;

    /**
     * 查询原型页面
     * 
     * @param pageId 原型页面主键
     * @return 原型页面
     */
    @Override
    public AiProtoPage selectAiProtoPageByPageId(Long pageId)
    {
        return aiProtoPageMapper.selectAiProtoPageByPageId(pageId);
    }

    /**
     * 查询原型页面列表
     * 
     * @param aiProtoPage 原型页面
     * @return 原型页面
     */
    @Override
    public List<AiProtoPage> selectAiProtoPageList(AiProtoPage aiProtoPage)
    {
        return aiProtoPageMapper.selectAiProtoPageList(aiProtoPage);
    }

    /**
     * 新增原型页面
     * 
     * @param aiProtoPage 原型页面
     * @return 结果
     */
    @Override
    public int insertAiProtoPage(AiProtoPage aiProtoPage)
    {
        aiProtoPage.setCreateTime(DateUtils.getNowDate());
        return aiProtoPageMapper.insertAiProtoPage(aiProtoPage);
    }

    /**
     * 修改原型页面
     * 
     * @param aiProtoPage 原型页面
     * @return 结果
     */
    @Override
    public int updateAiProtoPage(AiProtoPage aiProtoPage)
    {
        aiProtoPage.setUpdateTime(DateUtils.getNowDate());
        return aiProtoPageMapper.updateAiProtoPage(aiProtoPage);
    }

    /**
     * 批量删除原型页面
     * 
     * @param pageIds 需要删除的原型页面主键
     * @return 结果
     */
    @Override
    public int deleteAiProtoPageByPageIds(Long[] pageIds)
    {
        return aiProtoPageMapper.deleteAiProtoPageByPageIds(pageIds);
    }

    /**
     * 删除原型页面信息
     * 
     * @param pageId 原型页面主键
     * @return 结果
     */
    @Override
    public int deleteAiProtoPageByPageId(Long pageId)
    {
        return aiProtoPageMapper.deleteAiProtoPageByPageId(pageId);
    }
}
