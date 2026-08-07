import request from '@/utils/request'
import { getToken } from '@/utils/auth'

/* =========================================================================
 * 原型设计 · 门户接口层（mock 优先，后端就绪后删除 mock 分支）
 *
 * 设计约定（与 prd/doc 同款）：
 *  - 前端先跑通，后端 /api/ai/proto/* 就绪后，把真实接口分支打开、删掉 mock 回退。
 *  - 数据模型对齐后端：
 *      ai_proto_page  : pageId/projectId/pageName/pageDesc/layout(JSON)/status(0草稿·1确认)/sourceModel
 *      ai_proto_component: compId/pageId/compType(LAYOUT·NAV·FORM·VIEW·BASE)/compName/
 *                          fieldName/fieldType/required/defaultValue/widthSpan(1-12)/bizDesc/
 *                          interactDesc/parentId/sort
 *  - 前端用 uid 作为内存唯一键（落库时由后端生成 compId/pageId）。
 *  - 单一数据源：组件实例数组即权威；layout 仅作栅格快照（gridSpan + sort），由数组生成。
 * ========================================================================= */

/* ============================ 组件库（用户可拖用的零件） ============================ */
// compType 五大类展开为具体可拖组件；type 为渲染器键。
export const PALETTE = [
  {
    group: 'NAV', label: '导航',
    items: [
      { type: 'nav', compType: 'NAV', label: '顶部导航栏', icon: 'Menu', widthSpan: 12,
        compName: '顶部导航栏', fieldName: '', fieldType: '', required: 'N',
        props: { menus: ['首页', '列表', '我的'] },
        fields: [{ key: 'props.menus', label: '菜单项', control: 'tags' }] },
      { type: 'ep-menu', compType: 'NAV', label: '菜单', icon: 'Menu', widthSpan: 12,
        compName: '菜单', fieldName: '', fieldType: '', required: 'N',
        props: { mode: 'horizontal', items: ['首页', '商品', '订单', '我的'] },
        fields: [
          { key: 'props.mode', label: '排列', control: 'select', options: ['horizontal', 'vertical'] },
          { key: 'props.items', label: '菜单项', control: 'tags' }
        ] },
      { type: 'ep-breadcrumb', compType: 'NAV', label: '面包屑', icon: 'Right', widthSpan: 12,
        compName: '面包屑', fieldName: '', fieldType: '', required: 'N',
        props: { items: ['首页', '商品管理', '商品详情'] },
        fields: [{ key: 'props.items', label: '层级', control: 'tags' }] },
      { type: 'ep-tabs', compType: 'NAV', label: '标签页', icon: 'Files', widthSpan: 12,
        compName: '标签页', fieldName: '', fieldType: '', required: 'N',
        props: { tabs: ['基本信息', '高级设置', '权限配置'] },
        fields: [{ key: 'props.tabs', label: '标签', control: 'tags' }] },
      { type: 'ep-steps', compType: 'NAV', label: '步骤条', icon: 'Tickets', widthSpan: 12,
        compName: '步骤条', fieldName: '', fieldType: '', required: 'N',
        props: { steps: ['填写表单', '确认信息', '提交完成'], active: 0 },
        fields: [
          { key: 'props.steps', label: '步骤', control: 'tags' },
          { key: 'props.active', label: '当前步', control: 'number', min: 0, max: 10 }
        ] },
      { type: 'ep-dropdown', compType: 'NAV', label: '下拉菜单', icon: 'ArrowDown', widthSpan: 4,
        compName: '下拉菜单', fieldName: '', fieldType: '', required: 'N',
        props: { items: ['选项一', '选项二', '选项三'] },
        fields: [{ key: 'props.items', label: '选项', control: 'tags' }] },
      { type: 'ep-page-header', compType: 'NAV', label: '页头', icon: 'Back', widthSpan: 12,
        compName: '页头', fieldName: '', fieldType: '', required: 'N', ep: 'el-page-header',
        epProps: { title: '返回', content: '页面标题' },
        fields: [
          { key: 'epProps.title', label: '返回文字', control: 'text' },
          { key: 'epProps.content', label: '标题', control: 'text' }
        ] },
      { type: 'ep-pagination', compType: 'NAV', label: '分页', icon: 'More', widthSpan: 12,
        compName: '分页', fieldName: '', fieldType: '', required: 'N', ep: 'el-pagination',
        epProps: { layout: 'prev,pager,next', total: 100, 'small': true },
        fields: [
          { key: 'epProps.total', label: '总条数', control: 'number', min: 0, max: 100000 },
          { key: 'epProps.layout', label: '布局', control: 'text' }
        ] }
    ]
  },
  {
    group: 'FORM', label: '表单',
    items: [
      { type: 'input', compType: 'FORM', label: '输入框', icon: 'EditPen', widthSpan: 4,
        compName: '输入框', fieldName: 'field', fieldType: 'STRING', required: 'N',
        props: { placeholder: '请输入', label: '字段' },
        fields: [
          { key: 'props.label', label: '标签', control: 'text' },
          { key: 'props.placeholder', label: '占位提示', control: 'text' }
        ] },
      { type: 'select', compType: 'FORM', label: '下拉选择', icon: 'ArrowDown', widthSpan: 4,
        compName: '下拉选择', fieldName: 'selectField', fieldType: 'ENUM', required: 'N',
        props: { placeholder: '请选择', label: '下拉', options: ['选项一', '选项二', '选项三'] },
        fields: [
          { key: 'props.label', label: '标签', control: 'text' },
          { key: 'props.options', label: '选项', control: 'tags' }
        ] },
      { type: 'date', compType: 'FORM', label: '日期选择', icon: 'Calendar', widthSpan: 4,
        compName: '日期选择', fieldName: 'dateField', fieldType: 'DATE', required: 'N',
        props: { label: '日期' },
        fields: [{ key: 'props.label', label: '标签', control: 'text' }] },
      { type: 'switch', compType: 'FORM', label: '开关', icon: 'Switch', widthSpan: 3,
        compName: '开关', fieldName: 'switchField', fieldType: 'BOOLEAN', required: 'N',
        props: { label: '是否启用', activeText: '开', inactiveText: '关' },
        fields: [
          { key: 'props.label', label: '标签', control: 'text' },
          { key: 'props.activeText', label: '开文字', control: 'text' },
          { key: 'props.inactiveText', label: '关文字', control: 'text' }
        ] },
      { type: 'number', compType: 'FORM', label: '数字输入', icon: 'Sort', widthSpan: 4,
        compName: '数字输入', fieldName: 'numField', fieldType: 'NUMBER', required: 'N',
        props: { placeholder: '请输入数字', label: '数量' },
        fields: [
          { key: 'props.label', label: '标签', control: 'text' },
          { key: 'props.placeholder', label: '占位提示', control: 'text' }
        ] },
      { type: 'textarea', compType: 'FORM', label: '文本域', icon: 'Document', widthSpan: 12,
        compName: '文本域', fieldName: 'textField', fieldType: 'STRING', required: 'N',
        props: { placeholder: '请输入多行文本', label: '描述', rows: 3 },
        fields: [
          { key: 'props.label', label: '标签', control: 'text' },
          { key: 'props.rows', label: '行数', control: 'number', min: 1, max: 20 }
        ] },
      { type: 'submit', compType: 'FORM', label: '提交按钮', icon: 'Check', widthSpan: 3,
        compName: '提交按钮', fieldName: '', fieldType: '', required: 'N',
        props: { text: '提交', linkTo: '' },
        fields: [{ key: 'props.text', label: '按钮文字', control: 'text' }] },
      { type: 'ep-radio', compType: 'FORM', label: '单选框', icon: 'Select', widthSpan: 6,
        compName: '单选框', fieldName: '', fieldType: '', required: 'N', ep: 'el-radio-group',
        epProps: { options: ['选项一', '选项二', '选项三'] },
        fields: [{ key: 'epProps.options', label: '选项', control: 'tags' }] },
      { type: 'ep-checkbox', compType: 'FORM', label: '多选框', icon: 'Check', widthSpan: 6,
        compName: '多选框', fieldName: '', fieldType: '', required: 'N', ep: 'el-checkbox-group',
        epProps: { options: ['阅读', '运动', '音乐'] },
        fields: [{ key: 'epProps.options', label: '选项', control: 'tags' }] },
      { type: 'ep-rate', compType: 'FORM', label: '评分', icon: 'StarFilled', widthSpan: 4,
        compName: '评分', fieldName: '', fieldType: '', required: 'N', ep: 'el-rate',
        epProps: { modelValue: 3 },
        fields: [{ key: 'epProps.modelValue', label: '默认分值', control: 'number', min: 0, max: 5 }] },
      { type: 'ep-slider', compType: 'FORM', label: '滑块', icon: 'Sort', widthSpan: 6,
        compName: '滑块', fieldName: '', fieldType: '', required: 'N', ep: 'el-slider',
        epProps: { modelValue: 40, showInput: false },
        fields: [
          { key: 'epProps.modelValue', label: '默认值', control: 'number', min: 0, max: 100 },
          { key: 'epProps.showInput', label: '显示输入框', control: 'switch' }
        ] },
      { type: 'ep-cascader', compType: 'FORM', label: '级联选择', icon: 'ArrowDown', widthSpan: 4,
        compName: '级联选择', fieldName: '', fieldType: '', required: 'N', ep: 'el-cascader',
        epProps: { placeholder: '请选择', options: [
          { value: 'zhejiang', label: '浙江', children: [{ value: 'hangzhou', label: '杭州' }] },
          { value: 'jiangsu', label: '江苏', children: [{ value: 'nanjing', label: '南京' }] }
        ] },
        fields: [{ key: 'epProps.placeholder', label: '占位提示', control: 'text' }] },
      { type: 'ep-time-picker', compType: 'FORM', label: '时间选择', icon: 'Clock', widthSpan: 4,
        compName: '时间选择', fieldName: '', fieldType: '', required: 'N', ep: 'el-time-picker',
        epProps: { placeholder: '选择时间', modelValue: '' },
        fields: [{ key: 'epProps.placeholder', label: '占位提示', control: 'text' }] },
      { type: 'ep-color-picker', compType: 'FORM', label: '取色器', icon: 'Brush', widthSpan: 3,
        compName: '取色器', fieldName: '', fieldType: '', required: 'N', ep: 'el-color-picker',
        epProps: { modelValue: '#409EFF' },
        fields: [{ key: 'epProps.modelValue', label: '默认颜色', control: 'text' }] },
      { type: 'ep-transfer', compType: 'FORM', label: '穿梭框', icon: 'Right', widthSpan: 12,
        compName: '穿梭框', fieldName: '', fieldType: '', required: 'N', ep: 'el-transfer',
        epProps: { modelValue: [], data: [
          { key: 1, label: '选项 1' }, { key: 2, label: '选项 2' }, { key: 3, label: '选项 3' }
        ] },
        fields: [] },
      { type: 'ep-upload', compType: 'FORM', label: '上传', icon: 'Upload', widthSpan: 6,
        compName: '上传', fieldName: '', fieldType: '', required: 'N', ep: 'el-upload',
        epProps: { action: '#', 'list-type': 'text', 'show-file-list': true },
        fields: [{ key: 'epProps.list-type', label: '列表样式', control: 'select', options: ['text', 'picture', 'picture-card'] }] },
      { type: 'ep-autocomplete', compType: 'FORM', label: '自动补全', icon: 'EditPen', widthSpan: 6,
        compName: '自动补全', fieldName: '', fieldType: '', required: 'N', ep: 'el-autocomplete',
        epProps: { placeholder: '请输入关键词', fetchSuggestions: (q, cb) => cb([]) },
        fields: [{ key: 'epProps.placeholder', label: '占位提示', control: 'text' }] }
    ]
  },
  {
    group: 'VIEW', label: '展示',
    items: [
      { type: 'table', compType: 'VIEW', label: '数据表格', icon: 'Grid', widthSpan: 12,
        compName: '数据表格', fieldName: '', fieldType: '', required: 'N',
        props: { columns: ['名称', '状态', '创建时间', '操作'], rows: 4 },
        fields: [
          { key: 'props.columns', label: '列名', control: 'tags' },
          { key: 'props.rows', label: '示例行数', control: 'number', min: 0, max: 50 }
        ] },
      { type: 'list', compType: 'VIEW', label: '列表', icon: 'List', widthSpan: 6,
        compName: '列表', fieldName: '', fieldType: '', required: 'N',
        props: { items: ['列表项一', '列表项二', '列表项三'] },
        fields: [{ key: 'props.items', label: '列表项', control: 'tags' }] },
      { type: 'card', compType: 'VIEW', label: '卡片', icon: 'Postcard', widthSpan: 4,
        compName: '卡片', fieldName: '', fieldType: '', required: 'N',
        props: { title: '卡片标题', desc: '卡片描述内容示例' },
        fields: [
          { key: 'props.title', label: '标题', control: 'text' },
          { key: 'props.desc', label: '描述', control: 'text' }
        ] },
      { type: 'chart', compType: 'VIEW', label: '图表占位', icon: 'TrendCharts', widthSpan: 6,
        compName: '图表占位', fieldName: '', fieldType: '', required: 'N',
        props: { chartType: '柱状图' },
        fields: [{ key: 'props.chartType', label: '图表类型', control: 'select', options: ['柱状图', '折线图', '饼图'] }] },
      { type: 'ep-tag', compType: 'VIEW', label: '标签', icon: 'Collection', widthSpan: 2,
        compName: '标签', fieldName: '', fieldType: '', required: 'N', ep: 'el-tag',
        epProps: { type: 'primary', effect: 'light' }, epText: '标签',
        fields: [
          { key: 'epProps.type', label: '类型', control: 'select', options: ['primary', 'success', 'warning', 'danger', 'info'] },
          { key: 'epProps.effect', label: '效果', control: 'select', options: ['light', 'dark', 'plain'] },
          { key: 'epText', label: '文字', control: 'text' }
        ] },
      { type: 'ep-badge', compType: 'VIEW', label: '徽标数', icon: 'Bell', widthSpan: 2,
        compName: '徽标数', fieldName: '', fieldType: '', required: 'N', ep: 'el-badge',
        epProps: { value: 12, type: 'danger' }, epText: '消息',
        fields: [
          { key: 'epProps.value', label: '数值', control: 'text' },
          { key: 'epProps.type', label: '类型', control: 'select', options: ['primary', 'success', 'warning', 'danger', 'info'] },
          { key: 'epText', label: '文字', control: 'text' }
        ] },
      { type: 'ep-avatar', compType: 'VIEW', label: '头像', icon: 'UserFilled', widthSpan: 2,
        compName: '头像', fieldName: '', fieldType: '', required: 'N', ep: 'el-avatar',
        epProps: { icon: 'UserFilled', shape: 'circle' },
        fields: [
          { key: 'epProps.icon', label: '图标', control: 'select', options: ['UserFilled', 'User', 'Picture'] },
          { key: 'epProps.shape', label: '形状', control: 'select', options: ['circle', 'square'] }
        ] },
      { type: 'ep-progress', compType: 'VIEW', label: '进度条', icon: 'Loading', widthSpan: 6,
        compName: '进度条', fieldName: '', fieldType: '', required: 'N', ep: 'el-progress',
        epProps: { percentage: 70, type: 'line' },
        fields: [
          { key: 'epProps.percentage', label: '进度', control: 'number', min: 0, max: 100 },
          { key: 'epProps.type', label: '类型', control: 'select', options: ['line', 'circle', 'dashboard'] }
        ] },
      { type: 'ep-tree', compType: 'VIEW', label: '树形控件', icon: 'Share', widthSpan: 6,
        compName: '树形控件', fieldName: '', fieldType: '', required: 'N', ep: 'el-tree',
        epProps: { 'default-expand-all': true, data: [
          { label: '一级节点', children: [{ label: '二级节点 A' }, { label: '二级节点 B' }] },
          { label: '另一个节点' }
        ] },
        fields: [] },
      { type: 'ep-calendar', compType: 'VIEW', label: '日历', icon: 'Calendar', widthSpan: 6,
        compName: '日历', fieldName: '', fieldType: '', required: 'N', ep: 'el-calendar', epProps: {},
        fields: [] },
      { type: 'ep-result', compType: 'VIEW', label: '结果页', icon: 'SuccessFilled', widthSpan: 6,
        compName: '结果页', fieldName: '', fieldType: '', required: 'N', ep: 'el-result',
        epProps: { icon: 'success', title: '操作成功', subTitle: '请根据提示进行后续操作' },
        fields: [
          { key: 'epProps.icon', label: '图标', control: 'select', options: ['success', 'warning', 'error', 'info'] },
          { key: 'epProps.title', label: '标题', control: 'text' },
          { key: 'epProps.subTitle', label: '副标题', control: 'text' }
        ] },
      { type: 'ep-statistic', compType: 'VIEW', label: '统计数值', icon: 'TrendCharts', widthSpan: 4,
        compName: '统计数值', fieldName: '', fieldType: '', required: 'N', ep: 'el-statistic',
        epProps: { title: '活跃用户', value: 1128 },
        fields: [
          { key: 'epProps.title', label: '标题', control: 'text' },
          { key: 'epProps.value', label: '数值', control: 'text' }
        ] },
      { type: 'ep-empty', compType: 'VIEW', label: '空状态', icon: 'Box', widthSpan: 4,
        compName: '空状态', fieldName: '', fieldType: '', required: 'N', ep: 'el-empty',
        epProps: { description: '暂无数据' },
        fields: [{ key: 'epProps.description', label: '描述', control: 'text' }] },
      { type: 'ep-skeleton', compType: 'VIEW', label: '骨架屏', icon: 'Loading', widthSpan: 6,
        compName: '骨架屏', fieldName: '', fieldType: '', required: 'N', ep: 'el-skeleton',
        epProps: { rows: 3, animated: true },
        fields: [
          { key: 'epProps.rows', label: '行数', control: 'number', min: 1, max: 20 },
          { key: 'epProps.animated', label: '动画', control: 'switch' }
        ] },
      { type: 'ep-segmented', compType: 'VIEW', label: '分段控件', icon: 'Switch', widthSpan: 6,
        compName: '分段控件', fieldName: '', fieldType: '', required: 'N', ep: 'el-segmented',
        epProps: { options: ['日', '周', '月'], modelValue: '日' },
        fields: [
          { key: 'epProps.options', label: '分段', control: 'tags' },
          { key: 'epProps.modelValue', label: '默认值', control: 'text' }
        ] },
      { type: 'ep-timeline', compType: 'VIEW', label: '时间线', icon: 'Clock', widthSpan: 6,
        compName: '时间线', fieldName: '', fieldType: '', required: 'N',
        props: { items: ['创建订单', '支付完成', '发货中', '已签收'] },
        fields: [{ key: 'props.items', label: '节点', control: 'tags' }] },
      { type: 'ep-carousel', compType: 'VIEW', label: '走马灯', icon: 'Picture', widthSpan: 6,
        compName: '走马灯', fieldName: '', fieldType: '', required: 'N', props: { count: 3 },
        fields: [{ key: 'props.count', label: '图片数', control: 'number', min: 1, max: 10 }] },
      { type: 'ep-collapse', compType: 'VIEW', label: '折叠面板', icon: 'ArrowDown', widthSpan: 6,
        compName: '折叠面板', fieldName: '', fieldType: '', required: 'N',
        props: { items: [
          { title: '一致性 Consistency', content: '与现实生活一致的设计语言。' },
          { title: '反馈 Feedback', content: '通过界面样式和交互动效让用户可以清晰的感知操作。' }
        ] },
        fields: [{ key: 'props.items', label: '折叠项', control: 'kv', shape: [{ k: 'title', label: '标题' }, { k: 'content', label: '内容' }] }] },
      { type: 'ep-descriptions', compType: 'VIEW', label: '描述列表', icon: 'Document', widthSpan: 12,
        compName: '描述列表', fieldName: '', fieldType: '', required: 'N',
        props: { items: [
          { label: '用户名', value: '张三' }, { label: '手机号', value: '138****8888' },
          { label: '状态', value: '在线' }, { label: '注册时间', value: '2026-01-01' }
        ] },
        fields: [{ key: 'props.items', label: '条目', control: 'kv', shape: [{ k: 'label', label: '标签' }, { k: 'value', label: '值' }] }] }
    ]
  },
  {
    group: 'LAYOUT', label: '布局',
    items: [
      { type: 'container', compType: 'LAYOUT', label: '容器/分栏', icon: 'CopyDocument', widthSpan: 12,
        compName: '容器', fieldName: '', fieldType: '', required: 'N',
        props: { title: '分组', columns: 2 },
        fields: [
          { key: 'props.title', label: '标题', control: 'text' },
          { key: 'props.columns', label: '分栏数', control: 'number', min: 1, max: 6 }
        ] },
      { type: 'divider', compType: 'LAYOUT', label: '分割线', icon: 'Minus', widthSpan: 12,
        compName: '分割线', fieldName: '', fieldType: '', required: 'N',
        props: { text: '' },
        fields: [{ key: 'props.text', label: '文字', control: 'text' }] },
      { type: 'ep-space', compType: 'LAYOUT', label: '间距容器', icon: 'Share', widthSpan: 12,
        compName: '间距容器', fieldName: '', fieldType: '', required: 'N', props: {},
        fields: [] },
      { type: 'ep-watermark', compType: 'LAYOUT', label: '水印', icon: 'Brush', widthSpan: 12,
        compName: '水印', fieldName: '', fieldType: '', required: 'N', props: { content: 'DEVPIVOT' },
        fields: [{ key: 'props.content', label: '水印文字', control: 'text' }] }
    ]
  },
  {
    group: 'BASE', label: '基础',
    items: [
      { type: 'text', compType: 'BASE', label: '文本', icon: 'Tickets', widthSpan: 12,
        compName: '文本', fieldName: '', fieldType: '', required: 'N',
        props: { text: '这是一段示例文本，可在右侧属性面板修改内容。' },
        fields: [{ key: 'props.text', label: '文本内容', control: 'textarea' }] },
      { type: 'image', compType: 'BASE', label: '图片', icon: 'Picture', widthSpan: 6,
        compName: '图片', fieldName: '', fieldType: '', required: 'N',
        props: { ratio: '16:9' },
        fields: [{ key: 'props.ratio', label: '比例', control: 'select', options: ['16:9', '4:3', '1:1', '1:2'] }] },
      { type: 'button', compType: 'BASE', label: '按钮', icon: 'Pointer', widthSpan: 2,
        compName: '按钮', fieldName: '', fieldType: '', required: 'N',
        props: { text: '按钮', linkTo: '' },
        fields: [
          { key: 'props.text', label: '按钮文字', control: 'text' },
          { key: 'props.type', label: '样式', control: 'select', options: ['default', 'primary', 'success', 'warning', 'danger', 'info'] }
        ] },
      { type: 'icon', compType: 'BASE', label: '图标', icon: 'Star', widthSpan: 1,
        compName: '图标', fieldName: '', fieldType: '', required: 'N',
        props: { name: 'Star' },
        fields: [{ key: 'props.name', label: '图标名', control: 'select', options: ['Star', 'Setting', 'User', 'Search', 'Bell', 'HomeFilled', 'Edit', 'Delete', 'Plus', 'Check'] }] },
      { type: 'ep-link', compType: 'BASE', label: '链接', icon: 'Link', widthSpan: 2,
        compName: '链接', fieldName: '', fieldType: '', required: 'N', ep: 'el-link',
        epProps: { type: 'primary' }, epText: '链接文字',
        fields: [
          { key: 'epProps.type', label: '类型', control: 'select', options: ['primary', 'success', 'warning', 'danger', 'info', 'default'] },
          { key: 'epText', label: '文字', control: 'text' }
        ] },
      { type: 'ep-text', compType: 'BASE', label: '文字(EP)', icon: 'Document', widthSpan: 6,
        compName: '文字', fieldName: '', fieldType: '', required: 'N', ep: 'el-text',
        epProps: { type: 'primary', size: 'default' }, epText: '这是一段文字内容',
        fields: [
          { key: 'epProps.type', label: '类型', control: 'select', options: ['primary', 'success', 'warning', 'danger', 'info'] },
          { key: 'epProps.size', label: '尺寸', control: 'select', options: ['small', 'default', 'large'] },
          { key: 'epText', label: '文字', control: 'text' }
        ] }
    ]
  },
  {
    group: 'FEEDBACK', label: '反馈提示',
    items: [
      { type: 'ep-alert', compType: 'VIEW', label: '警告提示', icon: 'WarningFilled', widthSpan: 12,
        compName: '警告提示', fieldName: '', fieldType: '', required: 'N', ep: 'el-alert',
        epProps: { title: '这是一条提示信息，用于反馈页面状态。', type: 'info', 'show-icon': true },
        fields: [
          { key: 'epProps.title', label: '提示文字', control: 'text' },
          { key: 'epProps.type', label: '类型', control: 'select', options: ['success', 'warning', 'info', 'error'] }
        ] },
      { type: 'ep-tooltip', compType: 'VIEW', label: '文字提示', icon: 'QuestionFilled', widthSpan: 3,
        compName: '文字提示', fieldName: '', fieldType: '', required: 'N', props: { content: '这是一段提示文字' },
        fields: [{ key: 'props.content', label: '提示内容', control: 'text' }] },
      { type: 'ep-popover', compType: 'VIEW', label: '弹出框', icon: 'ChatLineSquare', widthSpan: 3,
        compName: '弹出框', fieldName: '', fieldType: '', required: 'N', props: { content: '弹出框内容，常用于承载更多操作。' },
        fields: [{ key: 'props.content', label: '弹窗内容', control: 'text' }] }
    ]
  }
]

/* 图标库（供图标组件选材，墨刀式图标面板） */
export const ICON_LIST = [
  'Star', 'StarFilled', 'Setting', 'User', 'UserFilled', 'Search', 'Bell', 'BellFilled',
  'HomeFilled', 'Grid', 'Files', 'Edit', 'EditPen', 'Delete', 'Plus', 'Check', 'CheckFilled',
  'Picture', 'Document', 'List', 'Postcard', 'Menu', 'ArrowDown', 'Calendar', 'Switch', 'Sort',
  'TrendCharts', 'CopyDocument', 'Minus', 'Tickets', 'Pointer', 'Refresh', 'View', 'Position',
  'MagicStick', 'Close', 'CloseBold', 'ArrowLeft', 'ArrowRight', 'Filter', 'Upload', 'Download',
  'Share', 'Lock', 'Warning', 'WarningFilled', 'SuccessFilled', 'CircleCheck', 'InfoFilled',
  'More', 'MoreFilled', 'Back', 'Right', 'Bottom', 'Top', 'Loading', 'RefreshRight', 'RefreshLeft'
]

let _seq = 0
export function uid(prefix = 'c') {
  _seq += 1
  return `${prefix}_${Date.now().toString(36)}_${_seq}`
}

/* 组件视觉样式默认值（属性面板「外观」编辑，渲染器合并到根节点 inline style） */
export function defaultStyle() {
  return {
    align: 'stretch',    // 水平对齐：stretch 撑满 / left / center / right
    valign: 'start',     // 垂直对齐：start 上 / middle 中 / bottom 下（需配合固定高度生效）
    width: '',          // 固定宽度 px（空=自适应栅格）
    height: '',         // 固定高度 px
    rotate: 0,          // 旋转角度
    opacity: 100,       // 不透明度 0-100
    borderRadius: 0,    // 圆角 px
    backgroundColor: '',// 填充色
    borderColor: '',    // 描边色
    borderWidth: 0,     // 描边宽 px（设置 borderColor 后生效）
    shadowEnabled: false,
    shadowColor: 'rgba(0,0,0,0.15)',
    shadowBlur: 12,
    shadowX: 0,
    shadowY: 4,
    fontSize: null,     // 文本样式
    fontWeight: '',
    color: '',
    textAlign: '',
    lineHeight: ''
  }
}

/* 由 palette item 生成画布组件实例 */
export function buildComponent(item, overrides = {}) {
  const base = {
    uid: uid('c'),
    type: item.type,
    compType: item.compType,
    compName: item.compName,
    fieldName: item.fieldName || '',
    fieldType: item.fieldType || '',
    required: item.required || 'N',
    widthSpan: item.widthSpan || 6,
    bizDesc: '',
    interactDesc: '',
    interaction: { action: 'none', linkTo: '' },
    props: JSON.parse(JSON.stringify(item.props || {})),
    // EP 通用渲染：ep=组件标签(如 el-tag)，epProps=默认 props；复合组件(需子节点)不填 ep，由渲染器专属分支处理
    ep: item.ep || '',
    epProps: JSON.parse(JSON.stringify(item.epProps || {})),
    epText: item.epText || '',
    style: { ...defaultStyle(), ...(item.style || {}) }
  }
  const { style: ovStyle, ...rest } = overrides
  const comp = { ...base, ...rest, style: { ...base.style, ...(ovStyle || {}) } }
  // 按钮/提交类若声明了跳转目标，自动开启页面跳转交互（墨刀式点击走查）
  if ((comp.type === 'button' || comp.type === 'submit') && comp.props && comp.props.linkTo) {
    comp.interaction = { action: 'navigate', linkTo: comp.props.linkTo }
  }
  return comp
}

/* ============================ 后端真实接口（门户门面 /ai/proto，无 /api 前缀） ============================ */
// 后端页面 Map → 前端页面对象
export function fromBackendPage(p = {}) {
  return {
    uid: String(p.pageId),
    pageId: p.pageId,
    pageName: p.pageName || '',
    pageDesc: p.pageDesc || '',
    status: p.status || '0',
    deviceType: p.deviceType || 'WEB',
    sourceModel: p.sourceModel || '',
    components: (p.components || []).map(fromBackendComp)
  }
}

// 后端组件 Map → 前端组件对象（uid 复用 compId，meta 还原为 ep/epProps/epText 顶层字段）
export function fromBackendComp(c = {}) {
  const style = { ...defaultStyle(), ...(c.style || {}) }
  return {
    uid: c.uid || String(c.compId),
    compId: c.compId,
    type: c.type || 'text',
    compType: c.compType || 'BASE',
    compName: c.compName || '',
    fieldName: c.fieldName || '',
    fieldType: c.fieldType || '',
    required: c.required || 'N',
    widthSpan: c.widthSpan || 6,
    bizDesc: c.bizDesc || '',
    interactDesc: c.interactDesc || '',
    parentId: c.parentId || 0,
    sort: c.sort || 0,
    props: c.props || {},
    style,
    interaction: c.interaction || { action: 'none', linkTo: '' },
    ep: c.ep || '',
    epProps: c.epProps || {},
    epText: c.epText || ''
  }
}

// 按项目读取已存原型页面（权威源：后端库）
export function getProtoPages(projectId) {
  return request({ url: `/ai/proto/pages/${projectId}`, method: 'get' }).then(res => {
    const pages = (res && res.data && res.data.pages) || []
    return pages.map(fromBackendPage)
  })
}
// upsert 页面 + 组件（草稿保存：前端 page/comp 对象直接透传，后端按 map 取字段）
export function saveProto(projectId, pages, sourceModel = '人工') {
  return request({ url: `/ai/proto/save/${projectId}`, method: 'post', data: { pages, sourceModel } })
}
// 确认原型，推进项目阶段到 TECH
export function confirmProto(projectId) {
  return request({ url: `/ai/proto/confirm/${projectId}`, method: 'post', data: { projectId } })
}

/* ============================ 本地草稿持久化（mock 阶段唯一落库） ============================ */
const draftKey = (projectId) => `proto_draft_${projectId}`

export function saveProtoDraft(projectId, pages, meta = {}) {
  try {
    const payload = {
      pages: JSON.parse(JSON.stringify(pages)),
      deviceModel: meta.deviceModel || 'iphone-13',
      deviceType: meta.deviceType || 'WEB',
      customSize: meta.customSize || { width: 390, height: 844 }
    }
    localStorage.setItem(draftKey(projectId), JSON.stringify(payload))
  } catch (e) { /* ignore */ }
}

export function loadProtoDraft(projectId) {
  try {
    const raw = localStorage.getItem(draftKey(projectId))
    if (raw) {
      const obj = JSON.parse(raw)
      // 兼容旧版：直接是数组
      if (Array.isArray(obj)) return { pages: obj, deviceModel: 'iphone-13', deviceType: 'WEB', customSize: { width: 390, height: 844 } }
      return {
        pages: obj.pages || [],
        deviceModel: obj.deviceModel || 'iphone-13',
        deviceType: obj.deviceType || 'WEB',
        customSize: obj.customSize || { width: 390, height: 844 }
      }
    }
  } catch (e) { /* ignore */ }
  return null
}

/* ============================ 设备类型 ============================ */
export const DEVICE_OPTIONS = [
  { value: 'WEB', label: '网页端' },
  { value: 'H5', label: '移动端 H5' },
  { value: 'MINI', label: '小程序' }
]

/* 移动端机型（逻辑像素：width 直接作 CSS 宽度，height 作画布最小高度，radius 作外壳圆角） */
export const DEVICE_MODELS = [
  { value: 'iphone-se', label: 'iPhone SE', width: 375, height: 667, radius: 30, notch: 'none' },
  { value: 'iphone-8', label: 'iPhone 8', width: 375, height: 667, radius: 30, notch: 'none' },
  { value: 'iphone-13', label: 'iPhone 13/14', width: 390, height: 844, radius: 47, notch: 'notch' },
  { value: 'iphone-14-pro', label: 'iPhone 14 Pro', width: 393, height: 852, radius: 47, notch: 'island' },
  { value: 'iphone-15-pm', label: 'iPhone 15 Pro Max', width: 430, height: 932, radius: 55, notch: 'island' },
  { value: 'pixel-7', label: 'Pixel 7', width: 412, height: 915, radius: 32, notch: 'none' },
  { value: 'ipad-mini', label: 'iPad mini', width: 768, height: 1024, radius: 20, notch: 'none' }
]

/* ============================ 生成原型（调用后端 /ai/proto/generate） ============================ */
/**
 * 生成原型（调真实后端：AI 优先，无模型/失败则后端模板兜底）
 * @param {Object} params { projectId, projectName, model, deviceType, prdText }
 * @param {Object} handlers { onPage(pageObj), onDone(), onError(err), onProgress(text) }
 * @returns {{ stop: Function }}
 */
export function generateProto(params, handlers = {}) {
  const body = {
    projectId: params.projectId,
    projectName: params.projectName || '',
    deviceType: params.deviceType || 'WEB',
    model: params.model || '',
    prdText: params.prdText || ''
  }
  const base = import.meta.env.VITE_APP_BASE_API || ''
  const ctrl = new AbortController()
  handlers.onProgress && handlers.onProgress('正在生成原型…')
  fetch(base + '/ai/proto/generate/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + getToken()
    },
    body: JSON.stringify(body),
    signal: ctrl.signal
  }).then(resp => {
    if (!resp.ok || !resp.body) {
      handlers.onError && handlers.onError(new Error('HTTP ' + resp.status))
      return
    }
    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    const pump = () => reader.read().then(({ done, value }) => {
      if (done) { handlers.onDone && handlers.onDone(); return }
      buffer += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const raw = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        const ev = parseProtoSse(raw)
        if (!ev || !ev.data) continue
        if (ev.data.type === 'token') {
          handlers.onProgress && handlers.onProgress('AI 正在生成原型…')
        } else if (ev.data.type === 'pages') {
          const pages = (ev.data.pages || []).map(fromBackendPage)
          pages.forEach(p => handlers.onPage && handlers.onPage(p))
        }
      }
      pump()
    })
    pump()
  }).catch(err => {
    if (err.name !== 'AbortError') handlers.onError && handlers.onError(err)
  })
  return { stop() { ctrl.abort() } }
}

/* 局部改稿：把当前页面 + 自然语言指令发给后端 /ai/proto/patch（SSE），返回修改后的完整页面 */
export function applyProtoPatch(params, handlers = {}) {
  const body = {
    projectId: params.projectId,
    instruction: params.instruction || '',
    model: params.model || '',
    pages: params.pages || []
  }
  const base = import.meta.env.VITE_APP_BASE_API || ''
  const ctrl = new AbortController()
  handlers.onProgress && handlers.onProgress('正在应用修改…')
  fetch(base + '/ai/proto/patch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + getToken()
    },
    body: JSON.stringify(body),
    signal: ctrl.signal
  }).then(resp => {
    if (!resp.ok || !resp.body) {
      handlers.onError && handlers.onError(new Error('HTTP ' + resp.status))
      return
    }
    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let full = ''
    const pump = () => reader.read().then(({ done, value }) => {
      if (done) { handlers.onDone && handlers.onDone(); return }
      buffer += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const raw = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        const ev = parseProtoSse(raw)
        if (!ev || !ev.data) continue
        if (ev.data.type === 'token') {
          full += (ev.data.delta || '')
          handlers.onProgress && handlers.onProgress(full)
        } else if (ev.data.type === 'pages') {
          const pages = (ev.data.pages || []).map(fromBackendPage)
          pages.forEach(p => handlers.onPage && handlers.onPage(p))
        }
      }
      pump()
    })
    pump()
  }).catch(err => {
    if (err.name !== 'AbortError') handlers.onError && handlers.onError(err)
  })
  return { stop() { ctrl.abort() } }
}

/* ============================ 历史版本 ============================ */
export function saveVersion(projectId, pages, versionName = '', remark = '') {
  return request({ url: `/ai/proto/version/${projectId}`, method: 'post', data: { pages, versionName, remark, sourceModel: '人工' } })
}
export function listVersions(projectId) {
  return request({ url: `/ai/proto/versions/${projectId}`, method: 'get' }).then(res => res.data || [])
}
export function getVersion(versionId) {
  return request({ url: `/ai/proto/version/${versionId}`, method: 'get' }).then(res => res.data || {})
}
export function restoreVersion(versionId) {
  return request({ url: `/ai/proto/version/restore/${versionId}`, method: 'post' }).then(res => res.data || {})
}

/* 根据项目信息 + 设备类型拼装一套合理页面+组件（mock） */
function buildMockPages(p = {}, deviceType = 'WEB') {
  const name = p.projectName || '产品'
  if (deviceType && deviceType !== 'WEB') return buildMobilePages(p, deviceType)
  return buildWebPages(p, name, deviceType)
}

function buildWebPages(p, name, deviceType = 'WEB') {
  const listUid = uid('p')
  const detailUid = uid('p')
  const editUid = uid('p')

  const listPage = {
    uid: listUid, pageName: `${name} · 列表页`, pageDesc: '数据列表与操作入口', status: '0', deviceType,
    components: [
      buildComponent(PALETTE[0].items[0], { props: { menus: [name, '数据管理', '系统设置'] } }),
      buildComponent(findPalette('table'), {
        compName: '数据表格',
        props: { columns: ['名称', '状态', '负责人', '更新时间', '操作'], rows: 5 }
      }),
      buildComponent(findPalette('button'), { compName: '新建', props: { text: '＋ 新建', linkTo: editUid } })
    ]
  }

  const detailPage = {
    uid: detailUid, pageName: `${name} · 详情页`, pageDesc: '单条数据详情展示', status: '0', deviceType,
    components: [
      buildComponent(PALETTE[0].items[0], { props: { menus: [name, '返回列表', '编辑'] } }),
      buildComponent(findPalette('text'), { compName: '标题', props: { text: `${name} 详情` } }),
      buildComponent(findPalette('input'), { compName: '名称', fieldName: 'name', props: { label: '名称', placeholder: '示例数据' } }),
      buildComponent(findPalette('select'), { compName: '状态', fieldName: 'status', fieldType: 'ENUM', props: { label: '状态', options: ['启用', '停用'] } }),
      buildComponent(findPalette('date'), { compName: '创建时间', fieldName: 'createTime', fieldType: 'DATE', props: { label: '创建时间' } }),
      buildComponent(findPalette('textarea'), { compName: '备注', fieldName: 'remark', props: { label: '备注', rows: 2 } }),
      buildComponent(findPalette('button'), { widthSpan: 2, compName: '返回', props: { text: '← 返回', linkTo: listUid } }),
      buildComponent(findPalette('button'), { widthSpan: 2, compName: '编辑', props: { text: '编辑', linkTo: editUid, type: 'primary' } })
    ]
  }

  const editPage = {
    uid: editUid, pageName: `${name} · 新增/编辑页`, pageDesc: '表单录入与提交', status: '0', deviceType,
    components: [
      buildComponent(PALETTE[0].items[0], { props: { menus: [name, '返回列表', '保存'] } }),
      buildComponent(findPalette('input'), { compName: '名称', fieldName: 'name', required: 'Y', props: { label: '名称', placeholder: '请输入名称' } }),
      buildComponent(findPalette('select'), { compName: '类型', fieldName: 'type', fieldType: 'ENUM', props: { label: '类型', options: ['类型A', '类型B', '类型C'] } }),
      buildComponent(findPalette('number'), { compName: '数量', fieldName: 'count', fieldType: 'NUMBER', props: { label: '数量' } }),
      buildComponent(findPalette('date'), { compName: '生效日期', fieldName: 'effectDate', fieldType: 'DATE', props: { label: '生效日期' } }),
      buildComponent(findPalette('switch'), { compName: '是否启用', fieldName: 'enabled', fieldType: 'BOOLEAN', props: { label: '是否启用' } }),
      buildComponent(findPalette('textarea'), { compName: '描述', fieldName: 'desc', props: { label: '描述', rows: 3 } }),
      buildComponent(findPalette('submit'), { widthSpan: 3, compName: '提交', props: { text: '保存提交', linkTo: listUid } })
    ]
  }

  return [listPage, detailPage, editPage]
}

/* 移动端（H5 / 小程序）：卡片流首页 + 单元格列表 + 单列表单详情 + 个人中心 */
function buildMobilePages(p, deviceType) {
  const name = p.projectName || '产品'
  const homeUid = uid('p')
  const listUid = uid('p')
  const detailUid = uid('p')
  const mineUid = uid('p')
  const tabs = ['首页', '分类', '我的']
  const m = (type, overrides = {}) => buildComponent(findPalette(type), { widthSpan: 12, ...overrides })

  const homePage = {
    uid: homeUid, pageName: `${name} · 首页`, pageDesc: '移动端首页（卡片流）', status: '0', deviceType,
    components: [
      m('nav', { props: { menus: tabs } }),
      m('input', { compName: '搜索', props: { label: '搜索商品', placeholder: '搜索' } }),
      m('card', { widthSpan: 6, compName: '商品卡片', props: { title: '商品 A', desc: '¥99 · 已售 1.2k' } }),
      m('card', { widthSpan: 6, compName: '商品卡片', props: { title: '商品 B', desc: '¥129 · 已售 860' } }),
      m('button', { compName: '发布', props: { text: '＋ 发布', type: 'primary' } })
    ]
  }

  const listPage = {
    uid: listUid, pageName: `${name} · 列表`, pageDesc: '移动端列表（单元格）', status: '0', deviceType,
    components: [
      m('nav', { props: { menus: ['商品', '分类', '我的'] } }),
      m('list', { compName: '商品列表', props: { items: ['商品 A · ¥99', '商品 B · ¥129', '商品 C · ¥59', '商品 D · ¥39'] } })
    ]
  }

  const detailPage = {
    uid: detailUid, pageName: `${name} · 详情`, pageDesc: '移动端详情（单列表单）', status: '0', deviceType,
    components: [
      m('nav', { props: { menus: ['详情', '编辑', '返回'] } }),
      m('text', { compName: '标题', props: { text: `${name} 详情` } }),
      m('input', { compName: '名称', fieldName: 'name', props: { label: '名称', placeholder: '示例' } }),
      m('select', { compName: '规格', fieldName: 'spec', fieldType: 'ENUM', props: { label: '规格', options: ['标准', '豪华'] } }),
      m('number', { compName: '数量', fieldName: 'count', fieldType: 'NUMBER', props: { label: '数量' } }),
      m('textarea', { compName: '备注', fieldName: 'remark', props: { label: '备注', rows: 2 } }),
      buildComponent(findPalette('button'), { widthSpan: 6, compName: '返回', props: { text: '← 返回', linkTo: listUid } }),
      buildComponent(findPalette('button'), { widthSpan: 6, compName: '立即购买', props: { text: '立即购买', type: 'primary' } })
    ]
  }

  const minePage = {
    uid: mineUid, pageName: `${name} · 我的`, pageDesc: '移动端个人中心', status: '0', deviceType,
    components: [
      m('nav', { props: { menus: tabs } }),
      m('list', { compName: '我的菜单', props: { items: ['我的订单', '收货地址', '优惠券', '设置'] } })
    ]
  }

  return [homePage, listPage, detailPage, minePage]
}

function findPalette(type) {
  for (const g of PALETTE) {
    const hit = g.items.find(it => it.type === type)
    if (hit) return hit
  }
  return PALETTE[1].items[0]
}

/* ============================ AI 对话（调用后端 /ai/proto/chat 流式 SSE） ============================ */
/**
 * 原型设计 AI 对话（调真实后端流式接口，无模型时后端给规则兜底）
 * @param {Object} params { projectId, message, pages, model }
 * @param {Object} handlers { onChunk(text), onDone(), onError(err) }
 * @returns {{ stop: Function }}
 */
export function sendProtoChat(params, handlers = {}) {
  const base = import.meta.env.VITE_APP_BASE_API || ''
  const body = {
    projectId: params.projectId,
    message: params.message || '',
    model: params.model || '',
    pages: params.pages || []
  }
  const ctrl = new AbortController()
  fetch(base + '/ai/proto/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + getToken()
    },
    body: JSON.stringify(body),
    signal: ctrl.signal
  }).then(resp => {
    if (!resp.ok || !resp.body) {
      handlers.onError && handlers.onError(new Error('HTTP ' + resp.status))
      return
    }
    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let full = ''
    const pump = () => reader.read().then(({ done, value }) => {
      if (done) {
        handlers.onDone && handlers.onDone()
        return
      }
      buffer += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const raw = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        const ev = parseProtoSse(raw)
        if (ev && ev.data && ev.data.type === 'token') {
          full += (ev.data.delta || '')
          handlers.onChunk && handlers.onChunk(full)
        }
      }
      pump()
    })
    pump()
  }).catch(err => {
    if (err.name !== 'AbortError') handlers.onError && handlers.onError(err)
  })
  return { stop() { ctrl.abort() } }
}

// 解析单个 SSE 事件块（含 event:/data: 行），返回 { event, data }
function parseProtoSse(raw) {
  let eventName = ''
  let dataStr = ''
  raw.split('\n').forEach(line => {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataStr += line.slice(5).trim()
    }
  })
  if (!dataStr) return null
  try {
    return { event: eventName, data: JSON.parse(dataStr) }
  } catch (e) {
    return null
  }
}

function buildChatReply(params) {
  const msg = (params.message || '').trim()
  const pages = params.pages || []
  const pageNames = pages.map(p => p.pageName).join('、') || '（暂无页面）'
  if (/表单|字段|输入/.test(msg)) {
    return `针对表单设计，建议：\n1. 必填字段（带 *）放在表单靠前位置，降低填写中断率。\n2. 当前页面包含：${pageNames}。\n3. 可在右侧属性面板把 fieldType 设为 NUMBER/DATE/ENUM 以便下游「技术方案 / 数据库」自动推导列类型。\n4. 需要我直接生成一套标准增删改查表单吗？告诉我实体名即可。`
  }
  if (/列表|表格/.test(msg)) {
    return `列表页建议：\n1. 操作列（查看/编辑/删除）固定靠右，配合分页与搜索框。\n2. 当前页面：${pageNames}。\n3. 「新建」按钮建议链接到新增/编辑页，形成可点击走查原型。\n4. 行数据可先放 5 行示例，便于演示。`
  }
  if (/导航|菜单/.test(msg)) {
    return `导航设计建议：\n1. 顶部导航承载一级模块，当前建议菜单：${pageNames}。\n2. 导航项可配置 linkTo 实现页面间跳转走查。\n3. 保持菜单数量 ≤ 5 个，超出用「更多」收起。`
  }
  return `我已了解你的需求：「${msg || '（空）'}」。\n当前原型包含页面：${pageNames}。\n我可以帮你：\n- 设计某一页的表单 / 列表 / 导航结构\n- 调整组件栅格宽度（拖拽角柄即可）\n- 给出字段类型建议，方便后续生成数据库表\n\n请用一句话描述你想改的页面或组件，例如「给列表页加一个搜索框」。`
}
