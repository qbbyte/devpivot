package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.AiDbDoc;

/**
 * 数据库设计文档Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-08
 */
public interface AiDbDocMapper 
{
    /**
     * 查询数据库设计文档
     * 
     * @param docId 数据库设计文档主键
     * @return 数据库设计文档
     */
    public AiDbDoc selectAiDbDocByDocId(Long docId);

    /**
     * 查询数据库设计文档列表
     * 
     * @param aiDbDoc 数据库设计文档
     * @return 数据库设计文档集合
     */
    public List<AiDbDoc> selectAiDbDocList(AiDbDoc aiDbDoc);

    /**
     * 新增数据库设计文档
     * 
     * @param aiDbDoc 数据库设计文档
     * @return 结果
     */
    public int insertAiDbDoc(AiDbDoc aiDbDoc);

    /**
     * 修改数据库设计文档
     * 
     * @param aiDbDoc 数据库设计文档
     * @return 结果
     */
    public int updateAiDbDoc(AiDbDoc aiDbDoc);

    /**
     * 删除数据库设计文档
     * 
     * @param docId 数据库设计文档主键
     * @return 结果
     */
    public int deleteAiDbDocByDocId(Long docId);

    /**
     * 批量删除数据库设计文档
     * 
     * @param docIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiDbDocByDocIds(Long[] docIds);
}
