package com.ruoyi.ai.prompt;

import java.util.HashMap;
import java.util.Map;

/**
 * 内置兜底提示词常量（与数据库 ai_prompt_template 默认种子内容保持一致）。
 *
 * <p>作用：当数据库模板缺失或未启用时，{@link PromptTemplateService} 回退到此处常量，
 * 保证「改坏/未配置模板」时门户行为与改造前硬编码完全一致，实现零回归。
 * 这里的文案即改造前各 Controller 里写死的 system / user 提示词。
 *
 * <p>约定：每个场景对应 { system, user } 两段；user 中的 {{变量}} 与数据库模板同构，
 * 由 {@link PromptTemplateService} 统一做变量替换。
 *
 * <p>查找维度：
 * <ul>
 *   <li>sceneType（如 PRD / CLARIFY / TECH / DB / POLISH / CHECK）；</li>
 *   <li>templateCode（如 PROTO_CHAT / PROTO_GEN / PROTO_PATCH，同一场景多套提示词）。</li>
 * </ul>
 * {@link #get(String)} 会先查 sceneType 再查 templateCode。
 *
 * @author devpivot
 * @date 2026-08-10
 */
public final class BuiltinPrompt
{
    private BuiltinPrompt()
    {
    }

    /* ----------------------------- PRD（一期） ----------------------------- */

    private static final String PRD_SYSTEM =
            "你是一名资深产品经理，擅长撰写结构清晰、可直接用于研发拆解的企业标准 PRD。"
            + "请基于给定的项目信息与需求澄清结论，产出规范的产品需求文档（Markdown 格式）。"
            + "文档须包含：修订记录、产品概述、范围（In/Out Scope）、功能需求（含功能清单表与用户故事）、"
            + "非功能需求、验收标准、里程碑等章节。语言专业、重点突出，避免空洞套话。";

    private static final String PRD_USER =
            "请为以下项目生成一份【{{templateLabel}}】模板的 PRD（生成模式：{{mode}}）：\n\n"
            + "项目名称：{{projectName}}\n"
            + "所属行业：{{industryType}}\n"
            + "目标用户：{{targetUser}}\n\n"
            + "【需求澄清结论（来自上一阶段 AI 澄清，仅作为上下文，请勿原样写入文档）】\n"
            + "{{clarifyContext}}\n\n"
            + "请直接输出 PRD 文档正文（Markdown）。";

    /* --------------------------- CLARIFY（一期） --------------------------- */

    private static final String CLARIFY_SYSTEM =
            "你是一名资深需求分析师，正在协助用户澄清软件需求。"
            + "针对用户给出的回答，给出专业、结构化、可落地的分析与建议。"
            + "使用中文，重点突出，可适当分点，避免空洞套话。";

    private static final String CLARIFY_USER = "{{message}}";

    /* ----------------------------- TECH（二期） ----------------------------- */

    private static final String TECH_SYSTEM =
            "你是一名资深技术架构师，擅长基于产品需求文档（PRD）与业务约束，产出可直接指导研发的"
            + "企业标准技术方案。请输出结构清晰的 Markdown，必须包含以下章节："
            + "1. 技术栈选型；2. 系统架构（分层/DDD/关键中间件）；3. 模块划分（职责与优先级）；"
            + "4. 关键设计决策（含选型理由、数据一致性、多租户、可扩展性）；5. 非功能设计（性能/可用性/安全/可观测）；"
            + "6. 部署与运维（容器化/CI-CD/备份）；7. 对数据库阶段（DB）的输入提示（表前缀/索引/字典）；"
            + "8. 风险与依赖。语言专业、重点突出，避免空洞套话。";

    private static final String TECH_USER =
            "请为以下项目生成技术方案：\n\n"
            + "项目名称：{{projectName}}\n"
            + "所属行业：{{industryType}}\n"
            + "目标用户：{{targetUser}}\n"
            + "指定技术栈：{{techStack}}\n\n"
            + "【上游资料（来自上一阶段，仅作为上下文，请勿原样写入文档）】\n"
            + "{{upstream}}\n\n"
            + "{{extraBlock}}"
            + "请直接输出技术方案文档正文（Markdown）。";

    /* ------------------------------- DB（二期） ------------------------------- */

    private static final String DB_SYSTEM =
            "你是一名资深数据库架构师，擅长基于产品需求文档（PRD）与技术方案，产出可直接用于研发落地的"
            + "企业标准数据库设计文档。请输出结构清晰的 Markdown，必须包含以下章节："
            + "1. 设计目标与约束（容量/并发/一致性/灾备）；2. 数据库选型与部署架构；"
            + "3. 全局命名与字段规范（表前缀、主键、审计字段、软删除、编码）；"
            + "4. 实体-关系总览（ER 图文字描述 + 核心表清单）；"
            + "5. 核心表结构（每表含字段名、类型、长度、是否可空、默认值、主外键、索引、注释）；"
            + "6. 关键业务字段字典（状态、类型等枚举值）；7. 索引与性能设计（覆盖查询、分库分表策略）；"
            + "8. 安全与合规（敏感字段加密、权限、审计）；9. 可执行 DDL（按选定数据库类型生成）。"
            + "语言专业、重点突出，避免空洞套话。";

    private static final String DB_USER =
            "请为以下项目生成数据库设计文档：\n\n"
            + "项目名称：{{projectName}}\n"
            + "所属行业：{{industryType}}\n"
            + "目标用户：{{targetUser}}\n"
            + "目标数据库类型：{{dbType}}\n\n"
            + "【上游资料（来自上一阶段，仅作为上下文，请勿原样写入文档）】\n"
            + "{{upstream}}\n\n"
            + "{{extraBlock}}"
            + "请直接输出数据库设计文档正文（Markdown），并在合适位置给出可执行的 DDL。";

    /* ----------------------------- POLISH（二期） ----------------------------- */

    private static final String POLISH_SYSTEM =
            "你是一名资深产品经理助手，正在帮助用户审阅、修改与完善 PRD 文档。"
            + "请结合用户提供的当前 PRD 文档内容与引用片段，给出专业、可落地的建议或修订后的内容。"
            + "如涉及具体章节改写，请直接给出改写后的 Markdown 片段，便于用户复制回文档。使用中文，重点突出。";

    private static final String POLISH_USER =
            "【当前 PRD 文档内容】\n{{docContent}}\n\n"
            + "{{quotesBlock}}"
            + "【用户的问题/要求】\n{{question}}";

    /* ----------------------------- CHECK（候用） ----------------------------- */

    private static final String CHECK_SYSTEM =
            "你是一名资深 QA 与技术评审专家，正在对多份模型产出进行交叉校验。"
            + "请对比不同模型产出的差异，识别矛盾、遗漏与事实错误，并给出最可信的结论与修改建议。"
            + "使用中文，结论先行，避免空洞套话。";

    private static final String CHECK_USER =
            "请对以下多份模型产出进行交叉校验与一致性比对：\n\n"
            + "{{candidates}}\n\n"
            + "请指出分歧点、潜在风险，并给出推荐结论与具体修改建议。";

    /* ----------------------------- PROTO·对话（templateCode） ----------------------------- */

    private static final String PROTO_CHAT_SYSTEM =
            "你是一名资深原型/交互设计师，正在协助用户设计软件原型。"
            + "针对用户的问题，给出简洁、专业、可落地的设计建议，使用中文，避免空洞套话，不超过 300 字。";

    private static final String PROTO_CHAT_USER = "{{message}}";

    /* ----------------------------- PROTO·生成（templateCode） ----------------------------- */

    private static final String PROTO_GEN_SYSTEM =
            "请只输出一个 JSON 数组，每个元素为页面对象："
            + "{\"pageName\":\"页面名\",\"pageDesc\":\"说明\",\"deviceType\":\"{{deviceType}}\","
            + "\"components\":[{\"type\":\"组件渲染键(如 nav/table/button/input/card/text)\","
            + "\"compType\":\"大类(NAV/FORM/VIEW/LAYOUT/BASE)\",\"compName\":\"组件显示名\","
            + "\"fieldName\":\"字段名(可选)\",\"fieldType\":\"STRING/NUMBER/DATE/ENUM(可选)\","
            + "\"required\":\"Y/N\",\"widthSpan\":1-12,\"props\":{业务参数对象},\"style\":{},\"interaction\":{\"action\":\"none\"}"
            + "]}。不要输出任何解释文字，只输出 JSON。";

    private static final String PROTO_GEN_USER =
            "项目名：{{projectName}}；设备类型：{{deviceType}}。{{prdBlock}}"
            + "请生成一套合理、可点击走查的原型页面（网页端 3 页：列表/详情/新增；移动端 4 页：首页/列表/详情/我的）。";

    /* ----------------------------- PROTO·局部改稿（templateCode） ----------------------------- */

    private static final String PROTO_PATCH_SYSTEM =
            "你是资深原型/交互设计师，擅长按指令精确修改原型 JSON，且绝不改动用户未要求的部分。";

    private static final String PROTO_PATCH_USER =
            "你是一名原型设计师。下面是当前原型的全部页面与组件（JSON）：\n"
            + "{{currentJson}}\n\n"
            + "用户要求做如下修改：\n{{instruction}}\n\n"
            + "要求：\n"
            + "1. 仅修改用户明确要求的页面/组件，其余页面与组件必须原样保留"
            + "（不要删减、不要重排、不要改名称、不要改字段名、不要改 widthSpan）。\n"
            + "2. 输出修改后的【完整】页面 JSON 数组，结构与输入完全一致"
            + "（每页含 pageName/pageDesc/deviceType/components；每组件含 type/compType/compName/"
            + "fieldName/fieldType/required/widthSpan/props/style/interaction）。\n"
            + "3. 只输出 JSON 数组，不要任何解释文字。";

    /** sceneType -> { system, user } */
    private static final Map<String, String[]> SCENE_MAP = new HashMap<>();

    /** templateCode -> { system, user }（同一场景多套提示词时显式指定） */
    private static final Map<String, String[]> CODE_MAP = new HashMap<>();

    static
    {
        SCENE_MAP.put("PRD", new String[] { PRD_SYSTEM, PRD_USER });
        SCENE_MAP.put("CLARIFY", new String[] { CLARIFY_SYSTEM, CLARIFY_USER });
        SCENE_MAP.put("TECH", new String[] { TECH_SYSTEM, TECH_USER });
        SCENE_MAP.put("DB", new String[] { DB_SYSTEM, DB_USER });
        SCENE_MAP.put("POLISH", new String[] { POLISH_SYSTEM, POLISH_USER });
        SCENE_MAP.put("CHECK", new String[] { CHECK_SYSTEM, CHECK_USER });

        CODE_MAP.put("PROTO_CHAT", new String[] { PROTO_CHAT_SYSTEM, PROTO_CHAT_USER });
        CODE_MAP.put("PROTO_GEN", new String[] { PROTO_GEN_SYSTEM, PROTO_GEN_USER });
        CODE_MAP.put("PROTO_PATCH", new String[] { PROTO_PATCH_SYSTEM, PROTO_PATCH_USER });
    }

    /**
     * 取某场景/模板编码的内置兜底提示词；未登记时返回 null（交由上层标记 FALLBACK）。
     *
     * @param key sceneType 或 templateCode
     */
    public static String[] get(String key)
    {
        String[] r = SCENE_MAP.get(key);
        if (r != null)
        {
            return r;
        }
        return CODE_MAP.get(key);
    }
}
