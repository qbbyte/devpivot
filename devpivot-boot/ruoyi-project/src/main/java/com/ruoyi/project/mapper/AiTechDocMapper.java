package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.AiTechDoc;

/**
 * 技术方案文档Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface AiTechDocMapper 
{
    /**
     * 查询技术方案文档
     * 
     * @param docId 技术方案文档主键
     * @return 技术方案文档
     */
    public AiTechDoc selectAiTechDocByDocId(Long docId);

    /**
     * 查询技术方案文档列表
     * 
     * @param aiTechDoc 技术方案文档
     * @return 技术方案文档集合
     */
    public List<AiTechDoc> selectAiTechDocList(AiTechDoc aiTechDoc);

    /**
     * 新增技术方案文档
     * 
     * @param aiTechDoc 技术方案文档
     * @return 结果
     */
    public int insertAiTechDoc(AiTechDoc aiTechDoc);

    /**
     * 修改技术方案文档
     * 
     * @param aiTechDoc 技术方案文档
     * @return 结果
     */
    public int updateAiTechDoc(AiTechDoc aiTechDoc);

    /**
     * 删除技术方案文档
     * 
     * @param docId 技术方案文档主键
     * @return 结果
     */
    public int deleteAiTechDocByDocId(Long docId);

    /**
     * 批量删除技术方案文档
     * 
     * @param docIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiTechDocByDocIds(Long[] docIds);
}
