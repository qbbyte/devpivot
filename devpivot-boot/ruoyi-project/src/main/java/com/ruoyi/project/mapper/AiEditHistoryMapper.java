package com.ruoyi.project.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.project.domain.AiEditHistory;

/**
 * 成员修改记录Mapper接口
 *
 * @author devpivot
 * @date 2026-08-26
 */
public interface AiEditHistoryMapper
{
    /**
     * 查询修改记录列表（条件：projectId/stage/versionId/operatorId/时间范围）
     */
    public List<AiEditHistory> selectAiEditHistoryList(AiEditHistory aiEditHistory);

    /**
     * 新增修改记录
     */
    public int insertAiEditHistory(AiEditHistory aiEditHistory);

    /**
     * 最近 N 条记录（头像组入口数据源）
     */
    public List<AiEditHistory> selectRecent(@Param("projectId") Long projectId, @Param("limit") int limit);

    /**
     * 按操作人聚合（成员贡献视图）：操作次数/版本相关次数/最近活跃时间
     */
    public List<Map<String, Object>> selectAggregateByOperator(@Param("projectId") Long projectId,
                                                               @Param("startTime") java.util.Date startTime);
}
