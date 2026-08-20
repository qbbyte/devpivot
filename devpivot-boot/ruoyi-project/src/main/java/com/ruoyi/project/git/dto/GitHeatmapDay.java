package com.ruoyi.project.git.dto;

/**
 * 热力图单日提交数(平台无关)。
 * 用对象而非 Map&lt;String,Integer&gt; 承载, 规避 fastjson2 Redis 序列化对
 * 泛型 Map 写入内嵌 @type 后反序列化 parseInt 报错的问题。
 */
public class GitHeatmapDay
{
    /** 日期 yyyy-MM-dd(Asia/Shanghai) */
    private String date;
    /** 当日提交数 */
    private int count;

    public GitHeatmapDay()
    {
    }

    public GitHeatmapDay(String date, int count)
    {
        this.date = date;
        this.count = count;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
