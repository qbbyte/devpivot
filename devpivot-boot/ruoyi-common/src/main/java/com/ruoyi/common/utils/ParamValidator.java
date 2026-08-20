package com.ruoyi.common.utils;

import com.ruoyi.common.exception.ServiceException;

/**
 * 通用入参校验助手。
 *
 * 用于替代 AI 控制器中「projectId 仅靠 toLong 解析、缺参即 NPE / 静默降级」的写法，
 * 统一以 ServiceException 抛出明确的业务错误信息（被全局异常处理器转为 500 + msg）。
 *
 * @author devpivot
 * @date 2026-08-19
 */
public class ParamValidator
{
    /** 校验项目ID非空且合法(&gt;0)；非法抛出 ServiceException */
    public static long projectId(Long id)
    {
        if (id == null || id <= 0)
        {
            throw new ServiceException("项目ID不能为空或非法");
        }
        return id;
    }

    /** 从任意对象(Number / String)解析并校验项目ID */
    public static long projectId(Object o)
    {
        if (o == null)
        {
            throw new ServiceException("项目ID不能为空或非法");
        }
        Long v;
        if (o instanceof Number)
        {
            v = ((Number) o).longValue();
        }
        else
        {
            try
            {
                v = Long.parseLong(String.valueOf(o).trim());
            }
            catch (NumberFormatException e)
            {
                throw new ServiceException("项目ID格式非法");
            }
        }
        if (v == null || v <= 0)
        {
            throw new ServiceException("项目ID不能为空或非法");
        }
        return v;
    }

    /**
     * 校验文本长度上限；allowBlank=true 时允许空串(仅做长度校验)。
     *
     * @param s       文本
     * @param max     最大长度
     * @param field   字段中文名(用于错误信息)
     * @param allowBlank 是否允许为空
     * @return 原值
     */
    public static String requireText(String s, int max, String field, boolean allowBlank)
    {
        if (s == null || s.isEmpty())
        {
            if (allowBlank)
            {
                return s;
            }
            throw new ServiceException(field + "不能为空");
        }
        if (s.length() > max)
        {
            throw new ServiceException(field + "长度不能超过 " + max + " 字符");
        }
        return s;
    }

    /** 校验枚举取值是否在允许集合内 */
    public static String requireEnum(String s, String field, String... allowed)
    {
        if (s == null)
        {
            throw new ServiceException(field + "不能为空");
        }
        for (String a : allowed)
        {
            if (a.equals(s))
            {
                return s;
            }
        }
        throw new ServiceException(field + "取值非法，允许：" + String.join("/", allowed));
    }
}
