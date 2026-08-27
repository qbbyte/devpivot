package com.ruoyi.project.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * JSON 结构化 diff 工具（基于 fastjson2 树遍历）
 * 输出摘要 {added, removed, modified} + 路径级 detail [{path, op, oldValue, newValue}]
 * 路径规范：pages[2].components[0].props.label（点路径 + 数组下标）
 *
 * @author devpivot
 * @date 2026-08-26
 */
public final class JsonDiffUtil
{
    private JsonDiffUtil() { }

    /**
     * 对比两个 JSON 字符串
     *
     * @return {summary:{added,removed,modified}, detail:[{path,op,oldValue,newValue}]}
     */
    public static Map<String, Object> diff(String beforeJson, String afterJson)
    {
        List<Map<String, Object>> detail = new ArrayList<>();
        int[] counts = new int[3]; // added / removed / modified
        Object before = parse(beforeJson);
        Object after = parse(afterJson);
        walk("", before, after, detail, counts);
        Map<String, Object> summary = new HashMap<>(3);
        summary.put("added", counts[0]);
        summary.put("removed", counts[1]);
        summary.put("modified", counts[2]);
        Map<String, Object> result = new HashMap<>(2);
        result.put("summary", summary);
        result.put("detail", detail);
        return result;
    }

    private static Object parse(String json)
    {
        if (json == null || json.isEmpty())
        {
            return null;
        }
        try
        {
            return JSON.parse(json);
        }
        catch (Exception e)
        {
            return json; // 非 JSON（如纯文本快照）按字符串整体对比
        }
    }

    private static void walk(String path, Object before, Object after,
                             List<Map<String, Object>> detail, int[] counts)
    {
        if (before == null && after == null)
        {
            return;
        }
        if (before == null)
        {
            counts[0]++;
            detail.add(entry(path, "add", null, after));
            return;
        }
        if (after == null)
        {
            counts[1]++;
            detail.add(entry(path, "remove", before, null));
            return;
        }
        if (before instanceof JSONObject && after instanceof JSONObject)
        {
            JSONObject b = (JSONObject) before;
            JSONObject a = (JSONObject) after;
            java.util.Set<String> keys = new java.util.LinkedHashSet<>(b.keySet());
            keys.addAll(a.keySet());
            for (String key : keys)
            {
                walk(join(path, key), b.get(key), a.get(key), detail, counts);
            }
            return;
        }
        if (before instanceof JSONArray && after instanceof JSONArray)
        {
            JSONArray b = (JSONArray) before;
            JSONArray a = (JSONArray) after;
            int n = Math.max(b.size(), a.size());
            for (int i = 0; i < n; i++)
            {
                Object bv = i < b.size() ? b.get(i) : null;
                Object av = i < a.size() ? a.get(i) : null;
                walk(path + "[" + i + "]", bv, av, detail, counts);
            }
            return;
        }
        if (!Objects.equals(before, after))
        {
            counts[2]++;
            detail.add(entry(path, "modify", before, after));
        }
    }

    private static String join(String path, String key)
    {
        return path.isEmpty() ? key : path + "." + key;
    }

    private static Map<String, Object> entry(String path, String op, Object oldValue, Object newValue)
    {
        Map<String, Object> m = new HashMap<>(4);
        m.put("path", path);
        m.put("op", op);
        if (oldValue != null)
        {
            m.put("oldValue", oldValue);
        }
        if (newValue != null)
        {
            m.put("newValue", newValue);
        }
        return m;
    }
}
