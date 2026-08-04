package com.ruoyi.ai.service;

import java.util.List;
import com.ruoyi.ai.domain.AiProtoPage;

/**
 * 原型页面Service接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface IAiProtoPageService 
{
    /**
     * 查询原型页面
     * 
     * @param pageId 原型页面主键
     * @return 原型页面
     */
    public AiProtoPage selectAiProtoPageByPageId(Long pageId);

    /**
     * 查询原型页面列表
     * 
     * @param aiProtoPage 原型页面
     * @return 原型页面集合
     */
    public List<AiProtoPage> selectAiProtoPageList(AiProtoPage aiProtoPage);

    /**
     * 新增原型页面
     * 
     * @param aiProtoPage 原型页面
     * @return 结果
     */
    public int insertAiProtoPage(AiProtoPage aiProtoPage);

    /**
     * 修改原型页面
     * 
     * @param aiProtoPage 原型页面
     * @return 结果
     */
    public int updateAiProtoPage(AiProtoPage aiProtoPage);

    /**
     * 批量删除原型页面
     * 
     * @param pageIds 需要删除的原型页面主键集合
     * @return 结果
     */
    public int deleteAiProtoPageByPageIds(Long[] pageIds);

    /**
     * 删除原型页面信息
     * 
     * @param pageId 原型页面主键
     * @return 结果
     */
    public int deleteAiProtoPageByPageId(Long pageId);
}
