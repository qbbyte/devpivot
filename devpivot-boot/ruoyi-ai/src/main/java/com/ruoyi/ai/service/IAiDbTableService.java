package com.ruoyi.ai.service;

import java.util.List;
import com.ruoyi.ai.domain.AiDbTable;

/**
 * 数据库结构Service接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface IAiDbTableService 
{
    /**
     * 查询数据库结构
     * 
     * @param tableId 数据库结构主键
     * @return 数据库结构
     */
    public AiDbTable selectAiDbTableByTableId(Long tableId);

    /**
     * 查询数据库结构列表
     * 
     * @param aiDbTable 数据库结构
     * @return 数据库结构集合
     */
    public List<AiDbTable> selectAiDbTableList(AiDbTable aiDbTable);

    /**
     * 新增数据库结构
     * 
     * @param aiDbTable 数据库结构
     * @return 结果
     */
    public int insertAiDbTable(AiDbTable aiDbTable);

    /**
     * 修改数据库结构
     * 
     * @param aiDbTable 数据库结构
     * @return 结果
     */
    public int updateAiDbTable(AiDbTable aiDbTable);

    /**
     * 批量删除数据库结构
     * 
     * @param tableIds 需要删除的数据库结构主键集合
     * @return 结果
     */
    public int deleteAiDbTableByTableIds(Long[] tableIds);

    /**
     * 删除数据库结构信息
     * 
     * @param tableId 数据库结构主键
     * @return 结果
     */
    public int deleteAiDbTableByTableId(Long tableId);
}
