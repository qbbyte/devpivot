package com.ruoyi.ai.prompt;

/**
 * 渲染后的提示词结果。
 *
 * <p>由 {@link PromptTemplateService#render(String, String, java.util.Map)} 产出，
 * 包含最终投递给大模型的 system / user 提示词，以及来源标记（DB / BUILTIN / FALLBACK）。
 *
 * @author devpivot
 * @date 2026-08-10
 */
public class RenderedPrompt
{
    /** system 提示词（对应 ai_prompt_template.template_content） */
    private final String systemPrompt;

    /** user 提示词（对应 ai_prompt_template.user_template，变量已替换） */
    private final String userPrompt;

    /** 场景类型 */
    private final String sceneType;

    /** 来源：DB（数据库模板）/ BUILTIN（内置兜底常量）/ FALLBACK（无可用模板） */
    private final String source;

    public RenderedPrompt(String systemPrompt, String userPrompt, String sceneType, String source)
    {
        this.systemPrompt = systemPrompt == null ? "" : systemPrompt;
        this.userPrompt = userPrompt == null ? "" : userPrompt;
        this.sceneType = sceneType;
        this.source = source == null ? "FALLBACK" : source;
    }

    public String getSystemPrompt()
    {
        return systemPrompt;
    }

    public String getUserPrompt()
    {
        return userPrompt;
    }

    public String getSceneType()
    {
        return sceneType;
    }

    public String getSource()
    {
        return source;
    }
}
