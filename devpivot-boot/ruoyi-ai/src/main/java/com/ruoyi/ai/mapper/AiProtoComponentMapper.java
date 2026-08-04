package com.ruoyi.ai.mapper;

import java.util.List;
import com.ruoyi.ai.domain.AiProtoComponent;

/**
 * 原型组件清单Mapper接口
 * 
 * @author ruoyi
 * @date 2026-08-04
 */
public interface AiProtoComponentMapper 
{
    /**
     * 查询原型组件清单
     * 
     * @param compId 原型组件清单主键
     * @return 原型组件清单
     */
    public AiProtoComponent selectAiProtoComponentByCompId(Long compId);

    /**
     * 查询原型组件清单列表
     * 
     * @param aiProtoComponent 原型组件清单
     * @return 原型组件清单集合
     */
    public List<AiProtoComponent> selectAiProtoComponentList(AiProtoComponent aiProtoComponent);

    /**
     * 新增原型组件清单
     * 
     * @param aiProtoComponent 原型组件清单
     * @return 结果
     */
    public int insertAiProtoComponent(AiProtoComponent aiProtoComponent);

    /**
     * 修改原型组件清单
     * 
     * @param aiProtoComponent 原型组件清单
     * @return 结果
     */
    public int updateAiProtoComponent(AiProtoComponent aiProtoComponent);

    /**
     * 删除原型组件清单
     * 
     * @param compId 原型组件清单主键
     * @return 结果
     */
    public int deleteAiProtoComponentByCompId(Long compId);

    /**
     * 批量删除原型组件清单
     * 
     * @param compIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiProtoComponentByCompIds(Long[] compIds);
}
