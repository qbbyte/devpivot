/**
 * 原型设计 → 纯静态 HTML 渲染器（不依赖 Element Plus，离线可直接打开）。
 * 复刻 src/components/proto/ProtoComponent.vue 的各组件类型，但输出原生 HTML + 内联 CSS。
 * 输入：后端 getProtoPages 返回的 pages 结构（{ pageName, pageDesc, deviceType, components: [...] }）。
 */

function esc(s) {
  if (s === null || s === undefined) return ''
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function props(c) {
  return c && c.props ? c.props : {}
}

function renderComp(c) {
  const p = props(c)
  const type = (c.type || 'text')
  switch (type) {
    case 'nav': {
      const menus = Array.isArray(p.menus) ? p.menus : []
      if (c.deviceType === 'H5' || c.deviceType === 'MINI') {
        const tabs = menus.map((m, i) =>
          `<div class="p-nav-tab${i === 0 ? ' active' : ''}"><span>${esc(m)}</span></div>`).join('')
        return `<div class="p-nav-mobile">${tabs}</div>`
      }
      const items = menus.map((m, i) =>
        `<span class="p-nav-menu${i === 0 ? ' active' : ''}">${esc(m)}</span>`).join('')
      return `<div class="p-nav"><div class="p-nav-logo">LOGO</div><div class="p-nav-menus">${items}</div></div>`
    }
    case 'input':
    case 'number':
      return `<div class="p-form-item"><label>${esc(p.label || '')}${c.required === 'Y' ? '<i class="p-req">*</i>' : ''}</label><input class="p-input" type="${type === 'number' ? 'number' : 'text'}" placeholder="${esc(p.placeholder || '请输入')}" disabled /></div>`
    case 'select':
      return `<div class="p-form-item"><label>${esc(p.label || '')}${c.required === 'Y' ? '<i class="p-req">*</i>' : ''}</label><select class="p-input" disabled><option>${esc(p.placeholder || '请选择')}</option></select></div>`
    case 'date':
      return `<div class="p-form-item"><label>${esc(p.label || '')}${c.required === 'Y' ? '<i class="p-req">*</i>' : ''}</label><input class="p-input" type="text" placeholder="选择日期" disabled /></div>`
    case 'switch':
      return `<div class="p-form-item p-inline"><label>${esc(p.label || '')}${c.required === 'Y' ? '<i class="p-req">*</i>' : ''}</label><span class="p-switch"></span></div>`
    case 'textarea':
      return `<div class="p-form-item"><label>${esc(p.label || '')}${c.required === 'Y' ? '<i class="p-req">*</i>' : ''}</label><textarea class="p-input" rows="${p.rows || 3}" placeholder="${esc(p.placeholder || '请输入')}" disabled></textarea></div>`
    case 'submit':
      return `<div class="p-form-item"><button class="p-btn p-btn-primary">${esc(p.text || '提交')}</button></div>`
    case 'button':
      return `<div class="p-button"><button class="p-btn ${p.type === 'primary' ? 'p-btn-primary' : 'p-btn-default'}">${esc(p.text || '按钮')}</button></div>`
    case 'table': {
      const cols = Array.isArray(p.columns) ? p.columns : []
      const th = cols.map(c => `<th>${esc(c)}</th>`).join('')
      const tr = cols.map(c => `<td>示例</td>`).join('')
      let rows = ''
      for (let i = 0; i < 4; i++) rows += `<tr>${tr}</tr>`
      return `<div class="p-table"><table border="1"><thead><tr>${th}</tr></thead><tbody>${rows}</tbody></table></div>`
    }
    case 'list': {
      const items = Array.isArray(p.items) ? p.items : []
      return `<div class="p-list">${items.map(it => `<div class="p-list-row">· ${esc(it)}</div>`).join('')}</div>`
    }
    case 'card':
      return `<div class="p-card"><div class="p-card-title">${esc(p.title || '卡片标题')}</div><div class="p-card-desc">${esc(p.desc || '卡片描述内容示例')}</div></div>`
    case 'chart':
      return `<div class="p-chart"><span>${esc(p.chartType || '图表')}（占位）</span></div>`
    case 'container': {
      const n = p.columns || 2
      const cells = []
      for (let i = 1; i <= n; i++) cells.push(`<div class="p-container-cell">区域 ${i}</div>`)
      return `<div class="p-container"><div class="p-container-title">${esc(p.title || '')}</div><div class="p-container-body" style="grid-template-columns:repeat(${n},1fr)">${cells.join('')}</div></div>`
    }
    case 'divider':
      return `<div class="p-divider">${esc(p.text || '')}</div>`
    case 'text':
      return `<div class="p-text">${esc(p.text || '文本内容')}</div>`
    case 'image':
      return `<div class="p-image"><span>图片占位</span></div>`
    case 'icon':
      return `<div class="p-icon">[图标]</div>`
    case 'ep-menu': {
      const items = Array.isArray(p.items) ? p.items : []
      const lis = items.map(it => `<div class="p-menu-item">${esc(it)}</div>`).join('')
      return `<div class="p-ep p-menu">${lis}</div>`
    }
    case 'ep-breadcrumb': {
      const items = Array.isArray(p.items) ? p.items : []
      return `<div class="p-ep p-breadcrumb">${items.map(it => `<span>${esc(it)}</span>`).join('<span class="p-sep">/</span>')}</div>`
    }
    case 'ep-tabs': {
      const tabs = Array.isArray(p.tabs) ? p.tabs : []
      const lis = tabs.map((t, i) => `<div class="p-tab${i === 0 ? ' active' : ''}">${esc(t)}</div>`).join('')
      return `<div class="p-ep p-tabs">${lis}</div>`
    }
    case 'ep-steps': {
      const steps = Array.isArray(p.steps) ? p.steps : []
      const lis = steps.map(s => `<div class="p-step">${esc(s)}</div>`).join('')
      return `<div class="p-ep p-steps">${lis}</div>`
    }
    case 'ep-dropdown':
      return `<div class="p-ep"><button class="p-btn p-btn-primary">下拉菜单 ▾</button></div>`
    case 'ep-timeline': {
      const items = Array.isArray(p.items) ? p.items : []
      const lis = items.map((t, i) => `<div class="p-tl-item"><span class="p-tl-dot"></span>步骤 ${i + 1}：${esc(t)}</div>`).join('')
      return `<div class="p-ep p-timeline">${lis}</div>`
    }
    case 'ep-carousel':
      return `<div class="p-ep p-carousel"><div class="p-carousel-item">幻灯片 1</div></div>`
    case 'ep-collapse': {
      const items = Array.isArray(p.items) ? p.items : []
      const lis = items.map(it => `<details class="p-collapse"><summary>${esc(it.title || '')}</summary><div>${esc(it.content || '')}</div></details>`).join('')
      return `<div class="p-ep p-collapses">${lis}</div>`
    }
    case 'ep-descriptions': {
      const items = Array.isArray(p.items) ? p.items : []
      const lis = items.map(it => `<div class="p-desc"><span class="p-desc-label">${esc(it.label || '')}</span><span class="p-desc-value">${esc(it.value || '')}</span></div>`).join('')
      return `<div class="p-ep p-descs">${lis}</div>`
    }
    case 'ep-tooltip':
      return `<div class="p-ep"><button class="p-btn p-btn-default">悬浮查看提示</button></div>`
    case 'ep-popover':
      return `<div class="p-ep"><button class="p-btn p-btn-default">点击弹出</button></div>`
    case 'ep-space':
      return `<div class="p-ep p-space">间距容器</div>`
    default:
      return `<div class="p-unknown">[${esc(type)}]</div>`
  }
}

function renderPage(page) {
  const isMobile = page.deviceType === 'H5' || page.deviceType === 'MINI'
  const comps = Array.isArray(page.components) ? page.components : []
  const inner = comps.map(c => `<div class="p-comp">${renderComp({ ...c, deviceType: page.deviceType })}</div>`).join('')
  return `<section class="p-page${isMobile ? ' mobile' : ''}">
    <div class="p-page-head">
      <h2>${esc(page.pageName || '未命名页面')}</h2>
      ${page.pageDesc ? `<p class="p-page-desc">${esc(page.pageDesc)}</p>` : ''}
      <span class="p-page-device">${esc(page.deviceType || 'WEB')}</span>
    </div>
    <div class="p-canvas">${inner || '<div class="p-canvas-empty">（本页面暂无组件）</div>'}</div>
  </section>`
}

const STYLE = `
<style>
  * { box-sizing: border-box; }
  body { margin: 0; font-family: -apple-system, "Segoe UI", "Microsoft YaHei", Arial, sans-serif; color: #1f2329; background: #f5f6f8; }
  .p-wrap { max-width: 960px; margin: 0 auto; padding: 24px; }
  .p-page { background: #fff; border: 1px solid #ebedf0; border-radius: 12px; margin-bottom: 24px; overflow: hidden; }
  .p-page.mobile { max-width: 420px; margin-left: auto; margin-right: auto; }
  .p-page-head { position: relative; padding: 16px 20px; border-bottom: 1px solid #f0f1f3; }
  .p-page-head h2 { margin: 0; font-size: 18px; }
  .p-page-desc { margin: 6px 0 0; font-size: 13px; color: #646a73; }
  .p-page-device { position: absolute; top: 16px; right: 20px; font-size: 12px; color: #909399; background: #f2f3f5; padding: 2px 8px; border-radius: 10px; }
  .p-canvas { padding: 18px 20px; display: flex; flex-direction: column; gap: 14px; }
  .p-canvas-empty { color: #a0aab5; font-size: 13px; text-align: center; padding: 20px; }
  .p-comp { font-size: 14px; }
  .p-nav { display: flex; align-items: center; gap: 18px; padding: 10px 0; border-bottom: 1px solid #f0f1f3; }
  .p-nav-logo { font-weight: 700; color: #409eff; }
  .p-nav-menus { display: flex; gap: 16px; }
  .p-nav-menu { color: #646a73; cursor: default; }
  .p-nav-menu.active { color: #1f2329; font-weight: 600; }
  .p-nav-mobile { display: flex; justify-content: space-around; padding: 10px 0; border-top: 1px solid #f0f1f3; }
  .p-nav-tab.active { color: #409eff; font-weight: 600; }
  .p-form-item { display: flex; flex-direction: column; gap: 6px; }
  .p-form-item.inline { flex-direction: row; align-items: center; }
  .p-form-item label { font-size: 13px; color: #646a73; }
  .p-req { color: #f56c6c; font-style: normal; }
  .p-input { width: 100%; padding: 8px 10px; border: 1px solid #dcdfe6; border-radius: 6px; font-size: 14px; background: #fafafa; color: #909399; }
  .p-switch { width: 40px; height: 22px; border-radius: 11px; background: #409eff; position: relative; }
  .p-switch::after { content: ''; position: absolute; right: 2px; top: 2px; width: 18px; height: 18px; border-radius: 50%; background: #fff; }
  .p-btn { padding: 8px 16px; border-radius: 6px; border: 1px solid #dcdfe6; background: #fff; cursor: default; font-size: 14px; }
  .p-btn-primary { background: #409eff; border-color: #409eff; color: #fff; }
  .p-table table { width: 100%; border-collapse: collapse; font-size: 13px; }
  .p-table th, .p-table td { padding: 8px 10px; border: 1px solid #ebeef5; text-align: left; color: #646a73; }
  .p-list-row { padding: 6px 0; color: #303133; }
  .p-card { border: 1px solid #ebedf0; border-radius: 8px; padding: 14px; }
  .p-card-title { font-weight: 600; margin-bottom: 4px; }
  .p-card-desc { color: #646a73; font-size: 13px; }
  .p-chart { border: 1px dashed #dcdfe6; border-radius: 8px; padding: 24px; text-align: center; color: #909399; }
  .p-container { border: 1px solid #ebedf0; border-radius: 8px; padding: 12px; }
  .p-container-title { font-weight: 600; margin-bottom: 8px; }
  .p-container-body { display: grid; gap: 10px; }
  .p-container-cell { background: #f5f6f8; border-radius: 6px; padding: 14px; text-align: center; color: #909399; font-size: 13px; }
  .p-divider { color: #909399; font-size: 13px; border-top: 1px solid #ebeef5; padding-top: 8px; }
  .p-text { line-height: 1.6; }
  .p-image { border: 1px dashed #dcdfe6; border-radius: 8px; padding: 24px; text-align: center; color: #909399; }
  .p-icon { color: #606266; }
  .p-ep { font-size: 14px; }
  .p-menu { display: flex; gap: 8px; background: #f5f6f8; padding: 8px; border-radius: 8px; flex-wrap: wrap; }
  .p-menu-item { padding: 4px 12px; }
  .p-breadcrumb span { margin-right: 4px; color: #646a73; }
  .p-sep { color: #c0c4cc; margin: 0 4px; }
  .p-tabs { display: flex; gap: 4px; border-bottom: 1px solid #ebeef5; }
  .p-tab { padding: 6px 14px; color: #646a73; }
  .p-tab.active { color: #409eff; font-weight: 600; border-bottom: 2px solid #409eff; }
  .p-steps { display: flex; gap: 8px; flex-wrap: wrap; }
  .p-step { background: #ecf5ff; color: #409eff; padding: 4px 10px; border-radius: 12px; font-size: 13px; }
  .p-timeline { display: flex; flex-direction: column; gap: 8px; }
  .p-tl-item { position: relative; padding-left: 16px; }
  .p-tl-dot { position: absolute; left: 0; top: 6px; width: 8px; height: 8px; border-radius: 50%; background: #409eff; }
  .p-carousel-item { background: #f5f6f8; border-radius: 8px; padding: 24px; text-align: center; color: #909399; }
  .p-collapse { border: 1px solid #ebedf0; border-radius: 6px; padding: 8px 12px; margin-bottom: 6px; }
  .p-collapse summary { cursor: default; font-weight: 500; }
  .p-descs { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
  .p-desc { border: 1px solid #ebedf0; border-radius: 6px; padding: 8px 10px; }
  .p-desc-label { color: #909399; font-size: 12px; display: block; }
  .p-desc-value { color: #303133; }
  .p-space { color: #a0aab5; }
  .p-unknown { color: #f56c6c; font-size: 12px; }
</style>`

/**
 * 生成可直接下载/打开的 HTML 文档字符串。
 */
export function protoToHtml(pages, projectName) {
  const list = Array.isArray(pages) ? pages : []
  const body = list.length
    ? list.map(renderPage).join('')
    : '<div class="p-canvas-empty">该项目暂无原型页面</div>'
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>${esc(projectName || '原型设计')} · 原型预览</title>
${STYLE}
</head>
<body>
<div class="p-wrap">
  <h1 style="font-size:22px;margin:0 0 4px;">${esc(projectName || '原型设计')}</h1>
  <p style="color:#909399;font-size:13px;margin:0 0 20px;">原型设计预览（由 devPivot 导出）</p>
  ${body}
</div>
</body>
</html>`
}
