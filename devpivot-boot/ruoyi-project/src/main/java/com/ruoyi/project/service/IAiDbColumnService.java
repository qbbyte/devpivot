package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.AiDbColumn;

/**
 * 数据库字段定义Service接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface IAiDbColumnService 
{
    /**
     * 查询数据库字段定义
     * 
     * @param columnId 数据库字段定义主键
     * @return 数据库字段定义
     */
    public AiDbColumn selectAiDbColumnByColumnId(Long columnId);

    /**
     * 查询数据库字段定义列表
     * 
     * @param aiDbColumn 数据库字段定义
     * @return 数据库字段定义集合
     */
    public List<AiDbColumn> selectAiDbColumnList(AiDbColumn aiDbColumn);

    /**
     * 新增数据库字段定义
     * 
     * @param aiDbColumn 数据库字段定义
     * @return 结果
     */
    public int insertAiDbColumn(AiDbColumn aiDbColumn);

    /**
     * 修改数据库字段定义
     * 
     * @param aiDbColumn 数据库字段定义
     * @return 结果
     */
    public int updateAiDbColumn(AiDbColumn aiDbColumn);

    /**
     * 批量删除数据库字段定义
     * 
     * @param columnIds 需要删除的数据库字段定义主键集合
     * @return 结果
     */
    public int deleteAiDbColumnByColumnIds(Long[] columnIds);

    /**
     * 删除数据库字段定义信息
     * 
     * @param columnId 数据库字段定义主键
     * @return 结果
     */
    public int deleteAiDbColumnByColumnId(Long columnId);
}
