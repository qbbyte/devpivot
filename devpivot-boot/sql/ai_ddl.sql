-- ----------------------------
-- AI智能需求设计与数据库生成系统 新增表结构
-- 版本: V1.0
-- 日期: 2026-08-04
-- ----------------------------

-- ----------------------------
-- 1、项目表
-- ----------------------------
drop table if exists ai_project;
create table ai_project (
  project_id       bigint(20)      not null auto_increment    comment '项目ID',
  project_name     varchar(64)     not null                   comment '项目名称',
  industry_type    varchar(32)     default ''                 comment '行业分类',
  project_intro    varchar(500)    default ''                 comment '项目简介',
  target_user      varchar(255)    default ''                 comment '目标用户群体',
  db_type          varchar(16)     default 'MySQL'            comment '目标数据库类型(MySQL/PostgreSQL)',
  model_strategy   json            default null               comment '默认模型策略(JSON: 默认模型/是否多模型/并行数量与名单)',
  step             varchar(16)     default 'REQ'              comment '项目进度阶段(REQ需求/CLARIFY澄清/PRD原型/TECH技术/DB库表/DONE完成)',
  is_top           char(1)         default 'N'                comment '是否置顶(Y/N)',
  status           char(1)         default '0'                comment '项目状态(0正常 1归档)',
  del_flag         char(1)         default '0'                comment '删除标志(0存在 2删除)',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (project_id)
) engine=innodb auto_increment=100 comment = 'AI项目表';

-- ----------------------------
-- 2、版本全链路记录表
-- ----------------------------
drop table if exists ai_version_record;
create table ai_version_record (
  record_id        bigint(20)      not null auto_increment    comment '记录ID',
  project_id       bigint(20)      not null                   comment '项目ID',
  biz_type         varchar(16)     not null                   comment '产物类型(REQ需求/PRD需求文档/PROTO原型/TECH技术文档/DB数据库)',
  biz_id           bigint(20)      not null                   comment '产物ID',
  version_no       varchar(16)     not null                   comment '版本号(V1.0)',
  content_snapshot longtext                                   comment '确认时完整内容快照(JSON,用于回退)',
  change_remark    varchar(500)    default null               comment '修改备注',
  source_model     varchar(32)     default ''                 comment '生成所用模型',
  model_params     json            default null               comment '生成参数(JSON)',
  status           char(1)         default '0'                comment '版本状态(0草稿 1正式版本)',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (record_id)
) engine=innodb auto_increment=100 comment = '版本全链路记录表';

-- ----------------------------
-- 3、需求基线表(全链路唯一数据源)
-- ----------------------------
drop table if exists ai_req_baseline;
create table ai_req_baseline (
  baseline_id      bigint(20)      not null auto_increment    comment '基线ID',
  project_id       bigint(20)      not null                   comment '项目ID',
  content          longtext                                   comment '结构化需求内容(JSON: 功能点/业务规则/角色权限/字段信息)',
  status           char(1)         default '0'                comment '状态(0草稿 1已确认)',
  source_model     varchar(32)     default ''                 comment '生成模型',
  model_params     json            default null               comment '生成参数(JSON)',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (baseline_id)
) engine=innodb auto_increment=100 comment = '需求基线表';

-- ----------------------------
-- 4、AI澄清问题记录表
-- ----------------------------
drop table if exists ai_clarify_record;
create table ai_clarify_record (
  record_id        bigint(20)      not null auto_increment    comment '记录ID',
  project_id       bigint(20)      not null                   comment '项目ID',
  category         varchar(16)     default ''                 comment '类别(ROLE角色权限/BUSINESS业务流程/DATA数据规则/BOUNDARY边界场景)',
  question         varchar(1000)   default ''                 comment '问题内容',
  answer           text                                       comment '用户回答',
  status           char(1)         default '0'                comment '状态(0待回答 1已回答 2已跳过)',
  source_model     varchar(32)     default ''                 comment '提出该问题的模型',
  highlight_type   char(1)         default '0'                comment '多模型对比标记(0共识 1独有 2观点差异)',
  model_list       json            default null               comment '语义一致命中该问题的模型列表',
  is_merged        char(1)         default 'N'                comment '是否勾选合并进最终清单(Y/N)',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (record_id)
) engine=innodb auto_increment=100 comment = 'AI澄清问题记录表';

-- ----------------------------
-- 5、PRD需求文档表
-- ----------------------------
drop table if exists ai_prd_doc;
create table ai_prd_doc (
  doc_id           bigint(20)      not null auto_increment    comment '文档ID',
  project_id       bigint(20)      not null                   comment '项目ID',
  doc_name         varchar(128)    default ''                 comment '文档标题',
  template_type    varchar(16)     default 'standard'         comment '文档模板(SIMPLE精简/STANDARD标准/DETAIL详细)',
  content          longtext                                   comment '文档内容(Markdown)',
  diff_result      longtext                                   comment '多模型对比差异结果(JSON)',
  multi_source     longtext                                   comment '各模型生成结果及融合来源(JSON)',
  status           char(1)         default '0'                comment '状态(0草稿 1已确认)',
  source_model     varchar(32)     default ''                 comment '生成模型',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (doc_id)
) engine=innodb auto_increment=100 comment = 'PRD需求文档表';

-- ----------------------------
-- 6、技术方案文档表
-- ----------------------------
drop table if exists ai_tech_doc;
create table ai_tech_doc (
  doc_id           bigint(20)      not null auto_increment    comment '文档ID',
  project_id       bigint(20)      not null                   comment '项目ID',
  doc_name         varchar(128)    default ''                 comment '文档标题',
  tech_stack       varchar(16)     default 'java'             comment '技术栈倾向(JAVA/PYTHON)',
  content          longtext                                   comment '文档内容(Markdown)',
  diff_result      longtext                                   comment '多模型对比差异结果(JSON)',
  multi_source     longtext                                   comment '各模型生成结果及融合来源(JSON)',
  status           char(1)         default '0'                comment '状态(0草稿 1已确认)',
  source_model     varchar(32)     default ''                 comment '生成模型',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (doc_id)
) engine=innodb auto_increment=100 comment = '技术方案文档表';

-- ----------------------------
-- 7、原型页面表
-- ----------------------------
drop table if exists ai_proto_page;
create table ai_proto_page (
  page_id          bigint(20)      not null auto_increment    comment '页面ID',
  project_id       bigint(20)      not null                   comment '项目ID',
  page_name        varchar(64)     default ''                 comment '页面名称',
  page_desc        varchar(255)    default ''                 comment '页面说明',
  layout           longtext                                   comment '画布布局数据(JSON: 组件树/栅格/坐标)',
  status           char(1)         default '0'                comment '状态(0草稿 1已确认)',
  source_model     varchar(32)     default ''                 comment '生成来源(人工/AI生成)',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (page_id)
) engine=innodb auto_increment=100 comment = '原型页面表';

-- ----------------------------
-- 8、原型组件/字段清单表
-- ----------------------------
drop table if exists ai_proto_component;
create table ai_proto_component (
  comp_id          bigint(20)      not null auto_increment    comment '组件ID',
  page_id          bigint(20)      not null                   comment '所属页面ID',
  comp_type        varchar(32)     default ''                 comment '组件类型(LAYOUT布局/NAV导航/FORM表单/VIEW展示/BASE基础)',
  comp_name        varchar(64)     default ''                 comment '组件显示名称',
  field_name       varchar(64)     default ''                 comment '绑定字段名',
  field_type       varchar(16)     default ''                 comment '字段类型(STRING/NUMBER/DATE/.../JSON)',
  required         char(1)         default 'N'                comment '是否必填(Y/N)',
  default_value    varchar(255)    default null               comment '默认值',
  validate_rule    json            default null               comment '校验规则(JSON)',
  width_span       int(4)          default 12                 comment '栅格宽度占比(1-12)',
  biz_desc         varchar(500)    default null               comment '业务说明',
  interact_desc    varchar(500)    default null               comment '交互说明',
  parent_id        bigint(20)      default 0                  comment '父组件ID(支持嵌套)',
  sort             int(4)          default 0                  comment '排序',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (comp_id)
) engine=innodb auto_increment=100 comment = '原型组件清单表';

-- ----------------------------
-- 9、数据库表结构表
-- ----------------------------
drop table if exists ai_db_table;
create table ai_db_table (
  table_id         bigint(20)      not null auto_increment    comment '表结构ID',
  project_id       bigint(20)      not null                   comment '项目ID',
  table_name       varchar(64)     not null                   comment '表名',
  table_comment    varchar(255)    default ''                 comment '表说明',
  db_type          varchar(16)     default 'MySQL'            comment '数据库类型(MySQL/PostgreSQL)',
  relation_desc    longtext                                   comment '表关系说明(JSON)',
  ddl_sql          longtext                                   comment '完整DDL脚本',
  check_report     longtext                                   comment '规范校验结果(JSON)',
  status           char(1)         default '0'                comment '状态(0草稿 1已确认)',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (table_id)
) engine=innodb auto_increment=100 comment = '数据库表结构表';

-- ----------------------------
-- 10、数据库字段定义表
-- ----------------------------
drop table if exists ai_db_column;
create table ai_db_column (
  column_id        bigint(20)      not null auto_increment    comment '字段ID',
  table_id         bigint(20)      not null                   comment '所属表结构ID',
  column_name      varchar(64)     not null                   comment '字段名',
  column_comment   varchar(255)    default ''                 comment '字段注释',
  column_type      varchar(32)     default ''                 comment '字段类型',
  column_length    int(11)         default null               comment '长度',
  nullable         char(1)         default 'Y'                comment '是否为空(Y/N)',
  default_value    varchar(255)    default null               comment '默认值',
  is_pk            char(1)         default 'N'                comment '是否主键(Y/N)',
  fk_table         varchar(64)     default null               comment '外键关联表',
  fk_column        varchar(64)     default null               comment '外键关联字段',
  is_unique        char(1)         default 'N'                comment '是否唯一约束(Y/N)',
  index_type       varchar(16)     default null               comment '索引类型(NORMAL普通/UNIQUE唯一/UNION联合)',
  sort             int(4)          default 0                  comment '排序',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (column_id)
) engine=innodb auto_increment=100 comment = '数据库字段定义表';

-- ----------------------------
-- 11、模型配置表
-- ----------------------------
drop table if exists ai_model_config;
create table ai_model_config (
  model_id         bigint(20)      not null auto_increment    comment '模型ID',
  model_code       varchar(64)     not null                   comment '模型标识',
  model_name       varchar(64)     not null                   comment '模型名称',
  provider         varchar(32)     default ''                 comment '供应商(OpenAI/DeepSeek/Qwen等)',
  base_url         varchar(255)    default ''                 comment '接口地址',
  api_key          varchar(255)    default ''                 comment 'API密钥(加密存储)',
  model_type       varchar(16)     default 'general'          comment '路由类型(GENERAL通用/STRUCT结构化/ENGINEER工程/LIGHT轻量)',
  context_length   int(11)         default 128000             comment '上下文长度',
  is_enabled       char(1)         default '0'                comment '是否启用(0启用 1停用)',
  default_params   json            default null               comment '默认参数(JSON: temperature/top_p)',
  sort             int(4)          default 0                  comment '排序',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (model_id)
) engine=innodb auto_increment=100 comment = 'AI模型配置表';

-- ----------------------------
-- 12、Prompt模板表
-- ----------------------------
drop table if exists ai_prompt_template;
create table ai_prompt_template (
  template_id      bigint(20)      not null auto_increment    comment '模板ID',
  template_code    varchar(64)     not null                   comment '模板编码',
  scene_type       varchar(16)     default ''                 comment '场景类型(CLARIFY/PRD/TECH/DB/CHECK/POLISH)',
  template_name    varchar(64)     default ''                 comment '模板名称',
  template_content text                                       comment '模板内容',
  model_specific   json            default null               comment '多模型差异化Prompt(JSON)',
  is_default       char(1)         default 'N'                comment '是否默认(Y/N)',
  is_enabled       char(1)         default '0'                comment '是否启用(0启用 1停用)',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (template_id)
) engine=innodb auto_increment=100 comment = 'Prompt模板表';

-- ----------------------------
-- 13、多模型并行任务表
-- ----------------------------
drop table if exists ai_parallel_task;
create table ai_parallel_task (
  task_id          bigint(20)      not null auto_increment    comment '任务ID',
  project_id       bigint(20)      not null                   comment '项目ID',
  task_type        varchar(16)     not null                   comment '任务类型(CLARIFY/PRD/TECH/DB_CHECK)',
  model_ids        json            default null               comment '参与模型列表(JSON)',
  request_params   longtext                                   comment '请求参数(JSON)',
  result_summary   longtext                                   comment '融合汇总结果(JSON)',
  compare_result   longtext                                   comment '差异比对结果(JSON)',
  status           char(1)         default '0'                comment '任务状态(0运行中 1完成 2部分失败 3失败)',
  est_tokens       int(11)         default 0                  comment '预估token',
  total_tokens     int(11)         default 0                  comment '实际消耗token',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (task_id)
) engine=innodb auto_increment=100 comment = '多模型并行任务表';

-- ----------------------------
-- 14、AI调用日志表
-- ----------------------------
drop table if exists ai_model_call_log;
create table ai_model_call_log (
  log_id           bigint(20)      not null auto_increment    comment '日志ID',
  project_id       bigint(20)      default null               comment '项目ID',
  task_id          bigint(20)      default null               comment '并行任务ID(单模型为空)',
  task_type        varchar(16)     default ''                 comment '场景类型',
  model_id         bigint(20)      default null               comment '模型ID',
  req_tokens       int(11)         default 0                  comment '输入token',
  resp_tokens      int(11)         default 0                  comment '输出token',
  cost             decimal(10,4)   default 0                  comment '费用',
  cache_hit        char(1)         default 'N'                comment '是否命中缓存(Y/N)',
  status           char(1)         default '0'                comment '状态(0成功 1失败 2超时 3降级)',
  error_msg        varchar(500)    default null               comment '错误信息',
  consume_ms       int(11)         default 0                  comment '耗时(毫秒)',
  start_time       datetime                                   comment '开始时间',
  end_time         datetime                                   comment '结束时间',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (log_id)
) engine=innodb auto_increment=100 comment = 'AI模型调用日志表';

-- ----------------------------
-- 15、用户API Key配置表
-- ----------------------------
drop table if exists ai_user_api_key;
create table ai_user_api_key (
  key_id           bigint(20)      not null auto_increment    comment '密钥ID',
  user_id          bigint(20)      not null                   comment '用户ID(sys_user)',
  provider         varchar(32)     default ''                 comment '供应商',
  api_key          varchar(255)    default ''                 comment 'API密钥(加密存储)',
  is_active        char(1)         default 'Y'                comment '是否启用(Y/N)',
  daily_quota      int(11)         default 0                  comment '每日调用次数上限',
  used_tokens      bigint(20)      default 0                  comment '累计消耗token',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (key_id)
) engine=innodb auto_increment=100 comment = '用户API Key配置表';

-- ----------------------------
-- 新增字典类型
-- ----------------------------
insert into sys_dict_type values(101, '数据库类型', 'ai_db_type',          '0', 'admin', sysdate(), '', null, '目标数据库类型列表');
insert into sys_dict_type values(102, '产物类型', 'ai_biz_type',          '0', 'admin', sysdate(), '', null, '版本产物类型列表');
insert into sys_dict_type values(103, '项目进度阶段', 'ai_project_step',   '0', 'admin', sysdate(), '', null, 'AI项目进度阶段列表');
insert into sys_dict_type values(104, '澄清问题类别', 'ai_clarify_category','0', 'admin', sysdate(), '', null, 'AI澄清问题类别列表');
insert into sys_dict_type values(105, '澄清问题状态', 'ai_clarify_status', '0', 'admin', sysdate(), '', null, '澄清问题处理状态');
insert into sys_dict_type values(106, '高亮类型', 'ai_highlight_type',    '0', 'admin', sysdate(), '', null, '多模型差异高亮标记');
insert into sys_dict_type values(107, 'PRD模板类型', 'ai_prd_template',   '0', 'admin', sysdate(), '', null, 'PRD文档模板类型');
insert into sys_dict_type values(108, '技术栈倾向', 'ai_tech_stack',      '0', 'admin', sysdate(), '', null, '技术方案技术栈倾向');
insert into sys_dict_type values(109, '组件类型', 'ai_comp_type',         '0', 'admin', sysdate(), '', null, '原型组件类型');
insert into sys_dict_type values(110, '字段类型', 'ai_field_type',        '0', 'admin', sysdate(), '', null, '原型字段类型');
insert into sys_dict_type values(111, '索引类型', 'ai_index_type',        '0', 'admin', sysdate(), '', null, '数据库索引类型');
insert into sys_dict_type values(112, '模型路由类型', 'ai_model_type',    '0', 'admin', sysdate(), '', null, 'AI模型路由分类');
insert into sys_dict_type values(113, 'Prompt场景', 'ai_scene_type',      '0', 'admin', sysdate(), '', null, 'Prompt模板场景类型');
insert into sys_dict_type values(114, '并行任务状态', 'ai_task_status',   '0', 'admin', sysdate(), '', null, '多模型并行任务状态');
insert into sys_dict_type values(115, '调用日志状态', 'ai_call_status',   '0', 'admin', sysdate(), '', null, 'AI调用日志状态');

-- ----------------------------
-- 新增字典数据
-- ----------------------------
-- 项目状态
insert into sys_dict_data values(100, 1,  '正常', '0', 'ai_project_status', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '项目正常状态');
insert into sys_dict_data values(101, 2,  '归档', '1', 'ai_project_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '项目归档状态');
-- 项目进度阶段
insert into sys_dict_data values(155, 1,  '需求采集',   'REQ',     'ai_project_step', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '需求采集阶段');
insert into sys_dict_data values(156, 2,  '智能澄清',   'CLARIFY', 'ai_project_step', '', 'info',    'N', '0', 'admin', sysdate(), '', null, 'AI智能澄清阶段');
insert into sys_dict_data values(157, 3,  'PRD确认',    'PRD',     'ai_project_step', '', 'success', 'N', '0', 'admin', sysdate(), '', null, 'PRD文档确认阶段');
insert into sys_dict_data values(158, 4,  '原型确认',   'PROTO',   'ai_project_step', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '原型设计确认阶段');
insert into sys_dict_data values(159, 5,  '技术文档',   'TECH',    'ai_project_step', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '技术方案文档阶段');
insert into sys_dict_data values(160, 6,  '数据库生成', 'DB',      'ai_project_step', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '数据库表结构生成阶段');
insert into sys_dict_data values(161, 7,  '已完成',     'DONE',    'ai_project_step', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '项目全部完成');
-- 数据库类型
insert into sys_dict_data values(102, 1,  'MySQL',       'MySQL',       'ai_db_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, 'MySQL数据库');
insert into sys_dict_data values(103, 2,  'PostgreSQL',  'PostgreSQL',  'ai_db_type', '', 'info',    'N', '0', 'admin', sysdate(), '', null, 'PostgreSQL数据库');
-- 产物类型
insert into sys_dict_data values(104, 1,  '需求基线',     'REQ',   'ai_biz_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '需求基线产物');
insert into sys_dict_data values(105, 2,  '需求文档PRD',  'PRD',   'ai_biz_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, 'PRD需求文档');
insert into sys_dict_data values(106, 3,  '原型',         'PROTO', 'ai_biz_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '可视化原型');
insert into sys_dict_data values(107, 4,  '技术文档',     'TECH',  'ai_biz_type', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '技术方案文档');
insert into sys_dict_data values(108, 5,  '数据库',       'DB',    'ai_biz_type', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '数据库表结构');
-- 澄清问题类别
insert into sys_dict_data values(109, 1,  '角色权限',     'ROLE',     'ai_clarify_category', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '角色权限类问题');
insert into sys_dict_data values(110, 2,  '业务流程',     'BUSINESS', 'ai_clarify_category', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '业务流程类问题');
insert into sys_dict_data values(111, 3,  '数据规则',     'DATA',     'ai_clarify_category', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '数据规则类问题');
insert into sys_dict_data values(112, 4,  '边界场景',     'BOUNDARY', 'ai_clarify_category', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '边界场景类问题');
-- 澄清问题状态
insert into sys_dict_data values(113, 1,  '待回答', '0', 'ai_clarify_status', '', 'warning', 'Y', '0', 'admin', sysdate(), '', null, '问题待回答');
insert into sys_dict_data values(114, 2,  '已回答', '1', 'ai_clarify_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '问题已回答');
insert into sys_dict_data values(115, 3,  '已跳过', '2', 'ai_clarify_status', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '问题已跳过');
-- 高亮类型
insert into sys_dict_data values(116, 1,  '共识内容',   '0', 'ai_highlight_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '多模型共识内容');
insert into sys_dict_data values(117, 2,  '独有补充',   '1', 'ai_highlight_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '单模型独有内容');
insert into sys_dict_data values(118, 3,  '观点差异',   '2', 'ai_highlight_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '多模型观点差异');
-- PRD模板类型
insert into sys_dict_data values(119, 1,  '精简版', 'simple',   'ai_prd_template', '', 'info',    'Y', '0', 'admin', sysdate(), '', null, '精简版PRD模板');
insert into sys_dict_data values(120, 2,  '标准版', 'standard', 'ai_prd_template', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '标准版PRD模板');
insert into sys_dict_data values(121, 3,  '详细版', 'detail',   'ai_prd_template', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '详细版PRD模板');
-- 技术栈倾向
insert into sys_dict_data values(122, 1,  'Java生态',   'java',   'ai_tech_stack', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, 'Java技术栈方案');
insert into sys_dict_data values(123, 2,  'Python生态', 'python', 'ai_tech_stack', '', 'success', 'N', '0', 'admin', sysdate(), '', null, 'Python技术栈方案');
-- 组件类型
insert into sys_dict_data values(124, 1,  '布局容器', 'LAYOUT', 'ai_comp_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '布局容器类组件');
insert into sys_dict_data values(125, 2,  '导航类',   'NAV',    'ai_comp_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '导航类组件');
insert into sys_dict_data values(126, 3,  '表单输入', 'FORM',   'ai_comp_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '表单输入类组件');
insert into sys_dict_data values(127, 4,  '数据展示', 'VIEW',   'ai_comp_type', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '数据展示类组件');
insert into sys_dict_data values(128, 5,  '基础元素', 'BASE',   'ai_comp_type', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '基础元素类组件');
-- 字段类型
insert into sys_dict_data values(129, 1,  '字符串',   'STRING', 'ai_field_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '字符串字段');
insert into sys_dict_data values(130, 2,  '数字',     'NUMBER', 'ai_field_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '数字字段');
insert into sys_dict_data values(131, 3,  '日期时间', 'DATE',   'ai_field_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '日期时间字段');
insert into sys_dict_data values(132, 4,  '布尔',     'BOOLEAN','ai_field_type', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '布尔字段');
insert into sys_dict_data values(133, 5,  'JSON',     'JSON',   'ai_field_type', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, 'JSON复杂字段');
-- 索引类型
insert into sys_dict_data values(134, 1,  '普通索引', 'NORMAL', 'ai_index_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '普通索引');
insert into sys_dict_data values(135, 2,  '唯一索引', 'UNIQUE', 'ai_index_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '唯一索引');
insert into sys_dict_data values(136, 3,  '联合索引', 'UNION',  'ai_index_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '联合索引');
-- 模型路由类型
insert into sys_dict_data values(137, 1,  '通用模型',   'GENERAL',   'ai_model_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '长上下文通用大模型');
insert into sys_dict_data values(138, 2,  '结构化输出', 'STRUCT',    'ai_model_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '结构化输出能力强的模型');
insert into sys_dict_data values(139, 3,  '工程能力',   'ENGINEER',  'ai_model_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '代码/工程能力强的模型');
insert into sys_dict_data values(140, 4,  '轻量模型',   'LIGHT',     'ai_model_type', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '轻量低成本模型');
-- Prompt场景
insert into sys_dict_data values(141, 1,  '需求澄清',     'CLARIFY', 'ai_scene_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '需求澄清场景');
insert into sys_dict_data values(142, 2,  'PRD生成',      'PRD',     'ai_scene_type', '', 'success', 'N', '0', 'admin', sysdate(), '', null, 'PRD生成场景');
insert into sys_dict_data values(143, 3,  '技术方案',     'TECH',    'ai_scene_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '技术方案生成场景');
insert into sys_dict_data values(144, 4,  '数据库设计',   'DB',      'ai_scene_type', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '数据库设计场景');
insert into sys_dict_data values(145, 5,  '交叉校验',     'CHECK',   'ai_scene_type', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '多模型交叉校验场景');
insert into sys_dict_data values(146, 6,  '文档润色',     'POLISH',  'ai_scene_type', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '文档润色整理场景');
-- 并行任务状态
insert into sys_dict_data values(147, 1,  '运行中',   '0', 'ai_task_status', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '任务运行中');
insert into sys_dict_data values(148, 2,  '已完成',   '1', 'ai_task_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '任务完成');
insert into sys_dict_data values(149, 3,  '部分失败', '2', 'ai_task_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '部分模型失败');
insert into sys_dict_data values(150, 4,  '失败',     '3', 'ai_task_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '任务失败');
-- 调用日志状态
insert into sys_dict_data values(151, 1,  '成功', '0', 'ai_call_status', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '调用成功');
insert into sys_dict_data values(152, 2,  '失败', '1', 'ai_call_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '调用失败');
insert into sys_dict_data values(153, 3,  '超时', '2', 'ai_call_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '调用超时');
insert into sys_dict_data values(154, 4,  '降级', '3', 'ai_call_status', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '降级为整包返回');
