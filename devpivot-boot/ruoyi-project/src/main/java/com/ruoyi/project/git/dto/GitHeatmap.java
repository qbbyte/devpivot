package com.ruoyi.project.git.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 提交热力图数据(平台无关)。
 * 时间窗口为过去 365 天(Asia/Shanghai), list 仅包含有提交的日期, 缺省视为 0。
 * 注意: 字段用 List&lt;GitHeatmapDay&gt; 而非 Map&lt;String,Integer&gt;,
 * 因 FastJson2JsonRedisSerializer 序列化泛型 Map 会写入内嵌 @type,
 * 反序列化时被按 Integer 解析导致 JSONException。
 */
public class GitHeatmap
{
    /** 时间窗口天数 */
    private int days = 365;
    /** 窗口起始日期 yyyy-MM-dd(含) */
    private String startDate;
    /** 窗口结束日期 yyyy-MM-dd(含) */
    private String endDate;
    /** 窗口内提交总数 */
    private int total;
    /** 有提交的日期明细(仅非零项) */
    private List<GitHeatmapDay> list = new ArrayList<>();

    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public List<GitHeatmapDay> getList() { return list; }
    public void setList(List<GitHeatmapDay> list) { this.list = list; }
}
