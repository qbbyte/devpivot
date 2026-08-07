<template>
  <div class="proto-comp" :class="[`ct-${comp.type}`, { 'is-preview': preview }]" :style="mergedStyle" @click="onClickComp">
    <!-- NAV 导航（网页：顶栏 / 移动端：底部标签栏） -->
    <div v-if="comp.type === 'nav' && !isMobile" class="r-nav">
      <div class="nav-logo">LOGO</div>
      <div class="nav-menus">
        <span
          v-for="(m, i) in (comp.props.menus || [])"
          :key="i"
          class="nav-menu"
          :class="{ active: i === 0 }"
          @click="onNav(m)"
        >{{ m }}</span>
      </div>
    </div>
    <div v-else-if="comp.type === 'nav' && isMobile" class="r-nav-mobile">
      <div v-for="(m, i) in (comp.props.menus || [])" :key="i" class="nav-tab" :class="{ active: i === 0 }">
        <el-icon><component :is="tabIcon(i)" /></el-icon>
        <span>{{ m }}</span>
      </div>
    </div>

    <!-- FORM 输入框 -->
    <div v-else-if="comp.type === 'input'" class="r-form-item">
      <label v-if="comp.props.label">{{ comp.props.label }}<i v-if="comp.required==='Y'" class="req">*</i></label>
      <el-input :model-value="''" :placeholder="comp.props.placeholder || '请输入'" disabled size="default" />
    </div>

    <!-- FORM 下拉 -->
    <div v-else-if="comp.type === 'select'" class="r-form-item">
      <label v-if="comp.props.label">{{ comp.props.label }}<i v-if="comp.required==='Y'" class="req">*</i></label>
      <el-select :model-value="''" :placeholder="comp.props.placeholder || '请选择'" disabled style="width:100%">
        <el-option v-for="(o,i) in (comp.props.options||[])" :key="i" :label="o" :value="o" />
      </el-select>
    </div>

    <!-- FORM 日期 -->
    <div v-else-if="comp.type === 'date'" class="r-form-item">
      <label v-if="comp.props.label">{{ comp.props.label }}<i v-if="comp.required==='Y'" class="req">*</i></label>
      <el-date-picker :model-value="''" type="date" placeholder="选择日期" disabled style="width:100%" />
    </div>

    <!-- FORM 开关 -->
    <div v-else-if="comp.type === 'switch'" class="r-form-item r-inline">
      <label v-if="comp.props.label">{{ comp.props.label }}<i v-if="comp.required==='Y'" class="req">*</i></label>
      <el-switch :model-value="true" disabled />
    </div>

    <!-- FORM 数字 -->
    <div v-else-if="comp.type === 'number'" class="r-form-item">
      <label v-if="comp.props.label">{{ comp.props.label }}<i v-if="comp.required==='Y'" class="req">*</i></label>
      <el-input type="number" :model-value="''" :placeholder="comp.props.placeholder || '请输入数字'" disabled />
    </div>

    <!-- FORM 文本域 -->
    <div v-else-if="comp.type === 'textarea'" class="r-form-item">
      <label v-if="comp.props.label">{{ comp.props.label }}<i v-if="comp.required==='Y'" class="req">*</i></label>
      <el-input type="textarea" :rows="comp.props.rows || 3" :model-value="''" :placeholder="comp.props.placeholder || '请输入'" disabled />
    </div>

    <!-- FORM 提交按钮 -->
    <div v-else-if="comp.type === 'submit'" class="r-form-item">
      <el-button type="primary" @click="onNav(comp.props.linkTo)">{{ comp.props.text || '提交' }}</el-button>
    </div>

    <!-- VIEW 表格（网页：宽表 / 移动端：单元格列表） -->
    <div v-else-if="comp.type === 'table' && !isMobile" class="r-table">
      <el-table :data="tableData" size="small" border style="width:100%" :show-overflow-tooltip="true">
        <el-table-column v-for="(c,i) in (comp.props.columns||[])" :key="i" :label="c" :prop="`c${i}`" />
      </el-table>
      <el-pagination small layout="prev,pager,next" :total="50" class="r-page" />
    </div>
    <div v-else-if="comp.type === 'table' && isMobile" class="r-list-mobile">
      <div v-for="(row, r) in tableData" :key="r" class="list-cell">
        <div class="cell-title">{{ row.c0 }}</div>
        <div v-if="metaLine(row)" class="cell-meta">{{ metaLine(row) }}</div>
      </div>
    </div>

    <!-- VIEW 列表 -->
    <div v-else-if="comp.type === 'list'" class="r-list">
      <div v-for="(it,i) in (comp.props.items||[])" :key="i" class="list-row">
        <el-icon><Tickets /></el-icon><span>{{ it }}</span>
      </div>
    </div>

    <!-- VIEW 卡片 -->
    <div v-else-if="comp.type === 'card'" class="r-card">
      <div class="card-title">{{ comp.props.title || '卡片标题' }}</div>
      <div class="card-desc">{{ comp.props.desc || '卡片描述内容示例' }}</div>
    </div>

    <!-- VIEW 图表占位 -->
    <div v-else-if="comp.type === 'chart'" class="r-chart">
      <el-icon :size="28" class="chart-ico"><TrendCharts /></el-icon>
      <span>{{ comp.props.chartType || '图表' }}（占位）</span>
    </div>

    <!-- LAYOUT 容器 -->
    <div v-else-if="comp.type === 'container'" class="r-container">
      <div class="container-title" v-if="comp.props.title">{{ comp.props.title }}</div>
      <div class="container-body" :style="{ gridTemplateColumns: `repeat(${comp.props.columns||2}, 1fr)` }">
        <div v-for="n in (comp.props.columns||2)" :key="n" class="container-cell">区域 {{ n }}</div>
      </div>
    </div>

    <!-- LAYOUT 分割线 -->
    <div v-else-if="comp.type === 'divider'" class="r-divider">
      <el-divider>{{ comp.props.text || '' }}</el-divider>
    </div>

    <!-- BASE 文本 -->
    <div v-else-if="comp.type === 'text'" class="r-text">{{ comp.props.text || '文本内容' }}</div>

    <!-- BASE 图片 -->
    <div v-else-if="comp.type === 'image'" class="r-image" :class="`ratio-${ratioClass}`">
      <el-icon :size="28"><Picture /></el-icon><span>图片占位</span>
    </div>

    <!-- BASE 按钮 -->
    <div v-else-if="comp.type === 'button'" class="r-button">
      <el-button :type="comp.props.type || 'default'" @click="onNav(comp.props.linkTo)">{{ comp.props.text || '按钮' }}</el-button>
    </div>

    <!-- BASE 图标 -->
    <div v-else-if="comp.type === 'icon'" class="r-icon">
      <el-icon :size="24"><component :is="iconComp(comp.props.name)" /></el-icon>
    </div>

    <!-- ===== 复合 EP 组件（需子节点，专属 markup） ===== -->
    <!-- 菜单 -->
    <div v-else-if="comp.type === 'ep-menu'" class="r-ep">
      <el-menu :mode="comp.props.mode || 'horizontal'" :default-active="'0'" class="r-menu">
        <el-menu-item v-for="(m, i) in (comp.props.items || [])" :key="i" :index="String(i)">{{ m }}</el-menu-item>
      </el-menu>
    </div>
    <!-- 面包屑 -->
    <div v-else-if="comp.type === 'ep-breadcrumb'" class="r-ep">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="(b, i) in (comp.props.items || [])" :key="i">{{ b }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <!-- 标签页 -->
    <div v-else-if="comp.type === 'ep-tabs'" class="r-ep">
      <el-tabs>
        <el-tab-pane v-for="(t, i) in (comp.props.tabs || [])" :key="i" :label="t" :name="String(i)" />
      </el-tabs>
    </div>
    <!-- 步骤条 -->
    <div v-else-if="comp.type === 'ep-steps'" class="r-ep">
      <el-steps :active="comp.props.active ?? 0" finish-status="success">
        <el-step v-for="(s, i) in (comp.props.steps || [])" :key="i" :title="s" />
      </el-steps>
    </div>
    <!-- 下拉菜单 -->
    <div v-else-if="comp.type === 'ep-dropdown'" class="r-ep">
      <el-dropdown>
        <el-button type="primary">下拉菜单<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-for="(d, i) in (comp.props.items || [])" :key="i">{{ d }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <!-- 时间线 -->
    <div v-else-if="comp.type === 'ep-timeline'" class="r-ep">
      <el-timeline>
        <el-timeline-item v-for="(t, i) in (comp.props.items || [])" :key="i" :timestamp="`步骤 ${i + 1}`">{{ t }}</el-timeline-item>
      </el-timeline>
    </div>
    <!-- 走马灯 -->
    <div v-else-if="comp.type === 'ep-carousel'" class="r-ep">
      <el-carousel height="150px" indicator-position="outside">
        <el-carousel-item v-for="n in (comp.props.count || 3)" :key="n">
          <div class="r-carousel-item">幻灯片 {{ n }}</div>
        </el-carousel-item>
      </el-carousel>
    </div>
    <!-- 折叠面板 -->
    <div v-else-if="comp.type === 'ep-collapse'" class="r-ep">
      <el-collapse>
        <el-collapse-item v-for="(c, i) in (comp.props.items || [])" :key="i" :title="c.title" :name="i">
          <div class="r-collapse-content">{{ c.content }}</div>
        </el-collapse-item>
      </el-collapse>
    </div>
    <!-- 描述列表 -->
    <div v-else-if="comp.type === 'ep-descriptions'" class="r-ep">
      <el-descriptions title="信息明细" :column="2" border>
        <el-descriptions-item v-for="(d, i) in (comp.props.items || [])" :key="i" :label="d.label">{{ d.value }}</el-descriptions-item>
      </el-descriptions>
    </div>
    <!-- 文字提示 -->
    <div v-else-if="comp.type === 'ep-tooltip'" class="r-ep">
      <el-tooltip :content="comp.props.content || '提示内容'" placement="top">
        <el-button>悬浮查看提示</el-button>
      </el-tooltip>
    </div>
    <!-- 弹出框 -->
    <div v-else-if="comp.type === 'ep-popover'" class="r-ep">
      <el-popover :content="comp.props.content || '弹出内容'" placement="top" trigger="click" :width="220">
        <template #reference>
          <el-button>点击弹出</el-button>
        </template>
      </el-popover>
    </div>
    <!-- 间距容器 -->
    <div v-else-if="comp.type === 'ep-space'" class="r-ep">
      <el-space wrap>
        <el-button>按钮</el-button>
        <el-tag type="success">标签</el-tag>
        <el-button type="primary">主要按钮</el-button>
        <el-link type="primary">链接</el-link>
      </el-space>
    </div>
    <!-- 水印 -->
    <div v-else-if="comp.type === 'ep-watermark'" class="r-ep r-watermark">
      <el-watermark :content="comp.props.content || 'DEVPIVOT'">
        <div class="r-wm-box">内容区域（带水印覆盖）</div>
      </el-watermark>
    </div>

    <!-- ===== 通用 EP 组件（单元素，直接渲染真实 EP 组件） ===== -->
    <div v-else-if="comp.ep" class="r-ep">
      <component :is="comp.ep" v-bind="comp.epProps">{{ comp.epText || '' }}</component>
    </div>

    <!-- 兜底 -->
    <div v-else class="r-text">未知组件：{{ comp.type }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const props = defineProps({
  comp: { type: Object, required: true },
  preview: { type: Boolean, default: false },
  pages: { type: Array, default: () => [] },
  deviceType: { type: String, default: 'WEB' }
})
const emit = defineEmits(['navigate'])

function onClickComp() {
  if (!props.preview) return
  const it = props.comp.interaction
  if (it && it.action === 'navigate' && it.linkTo) emit('navigate', it.linkTo)
}

const isMobile = computed(() => (props.deviceType || 'WEB') !== 'WEB')
const ratioClass = computed(() => (props.comp.props.ratio || '16:9').replace(':', 'x'))

/* 视觉样式：把 style 字段合并为 inline style，应用到根节点 */
const mergedStyle = computed(() => {
  const s = props.comp.style || {}
  const out = {}
  // 水平对齐：撑满 / 左 / 中 / 右
  const align = s.align || 'stretch'
  if (align === 'stretch') out.width = '100%'
  else {
    // block 根元素 width:auto 本就填满父级，必须用 fit-content 才能收缩
    out.width = 'fit-content'
    out.maxWidth = '100%'
    if (align === 'center') { out.marginLeft = 'auto'; out.marginRight = 'auto' }
    else if (align === 'right') out.marginLeft = 'auto'
  }
  // 垂直对齐（需配合固定高度才有可视空间）
  const valign = s.valign || 'start'
  if (s.height) {
    out.display = 'flex'
    out.flexDirection = 'column'
    if (valign === 'middle') out.justifyContent = 'center'
    else if (valign === 'bottom') out.justifyContent = 'flex-end'
  }
  if (s.width) out.width = s.width + 'px'
  if (s.height) out.height = s.height + 'px'
  if (s.rotate) out.transform = `rotate(${s.rotate}deg)`
  if (s.opacity != null && s.opacity !== 100) out.opacity = s.opacity / 100
  if (s.borderRadius) out.borderRadius = s.borderRadius + 'px'
  if (s.backgroundColor) out.backgroundColor = s.backgroundColor
  if (s.borderWidth) {
    out.borderWidth = s.borderWidth + 'px'
    out.borderStyle = 'solid'
    out.borderColor = s.borderColor || '#d9dde3'
  } else if (s.borderColor) {
    out.borderWidth = '1px'
    out.borderStyle = 'solid'
    out.borderColor = s.borderColor
  }
  if (s.shadowEnabled) {
    out.boxShadow = `${Number(s.shadowX) || 0}px ${Number(s.shadowY) || 0}px ${Number(s.shadowBlur) || 0}px ${s.shadowColor || 'rgba(0,0,0,0.15)'}`
  }
  if (s.fontSize) out.fontSize = s.fontSize + 'px'
  if (s.fontWeight) out.fontWeight = s.fontWeight
  if (s.color) out.color = s.color
  if (s.textAlign) out.textAlign = s.textAlign
  if (s.lineHeight) out.lineHeight = s.lineHeight
  return out
})

const tableData = computed(() => {
  const rows = props.comp.props.rows || 4
  const cols = props.comp.props.columns || []
  return Array.from({ length: rows }, (_, r) =>
    cols.reduce((acc, c, i) => { acc[`c${i}`] = i === cols.length - 1 ? '查看 编辑' : `示例${r + 1}`; return acc }, {})
  )
})

function iconComp(name) {
  const map = { Star: 'Star', Setting: 'Setting', User: 'User', Search: 'Search', Bell: 'Bell' }
  const key = map[name] || 'Star'
  return ElementPlusIconsVue[key] || ElementPlusIconsVue.Star
}

function onNav(linkTo) {
  if (!linkTo) return
  const target = props.pages.find(p => p.uid === linkTo)
  if (target) emit('navigate', target.uid)
}

function tabIcon(i) {
  const arr = ['HomeFilled', 'Grid', 'User', 'Setting', 'Bell']
  return arr[i] || 'Menu'
}
function metaLine(row) {
  const cols = Object.keys(row).filter(k => k !== 'c0')
  return cols.map(k => row[k]).join(' · ')
}
</script>

<style scoped>
.proto-comp { width: 100%; }
.req { color: #f56c6c; font-style: normal; margin-left: 2px; }

/* NAV */
.r-nav { display: flex; align-items: center; gap: 16px; height: 48px; background: #1f2329; border-radius: 8px; padding: 0 16px; color: #fff; }
.nav-logo { font-weight: 700; letter-spacing: 1px; }
.nav-menus { display: flex; gap: 18px; }
.nav-menu { font-size: 13px; color: #c9cdd4; cursor: pointer; padding: 4px 0; }
.nav-menu.active { color: #fff; font-weight: 600; border-bottom: 2px solid #3370ff; }
.nav-menu:hover { color: #fff; }

/* FORM */
.r-form-item { display: flex; flex-direction: column; gap: 6px; }
.r-form-item.r-inline { flex-direction: row; align-items: center; gap: 12px; }
.r-form-item label { font-size: 12px; color: #4e5969; }

/* TABLE */
.r-table { width: 100%; }
.r-page { margin-top: 8px; justify-content: flex-end; }

/* LIST */
.r-list { display: flex; flex-direction: column; gap: 8px; }
.list-row { display: flex; align-items: center; gap: 8px; padding: 10px 12px; background: #f7f8fa; border-radius: 8px; font-size: 13px; color: #1f2329; }

/* CARD */
.r-card { border: 1px solid #ebedf0; border-radius: 10px; padding: 16px; background: #fff; }
.card-title { font-weight: 600; margin-bottom: 6px; }
.card-desc { font-size: 13px; color: #86909c; line-height: 1.6; }

/* CHART */
.r-chart { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; height: 120px; background: #f7f8fa; border: 1px dashed #d9dde3; border-radius: 10px; color: #86909c; font-size: 12px; }
.chart-ico { color: #3370ff; }

/* CONTAINER */
.r-container { border: 1px dashed #c9d3e0; border-radius: 10px; padding: 14px; background: #fafbfc; }
.container-title { font-size: 13px; font-weight: 600; margin-bottom: 10px; color: #1f2329; }
.container-body { display: grid; gap: 12px; }
.container-cell { min-height: 56px; background: #eef2f8; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #86909c; }

/* DIVIDER */
.r-divider { width: 100%; }

/* TEXT */
.r-text { font-size: 14px; color: #1f2329; line-height: 1.7; }

/* IMAGE */
.r-image { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; width: 100%; background: #f2f3f5; border: 1px dashed #d9dde3; border-radius: 8px; color: #a8abb2; font-size: 12px; }
.r-image.ratio-16x9 { aspect-ratio: 16 / 9; }
.r-image.ratio-4x3 { aspect-ratio: 4 / 3; }
.r-image.ratio-1x1 { aspect-ratio: 1 / 1; }

/* BUTTON / ICON */
.r-button { display: inline-block; }
.r-icon { display: inline-flex; align-items: center; justify-content: center; width: 40px; height: 40px; border-radius: 8px; background: #f2f3f5; color: #3370ff; }

/* NAV 移动端底部标签栏 */
.r-nav-mobile { display: flex; justify-content: space-around; align-items: center; background: #fff; border-top: 1px solid #ebedf0; border-radius: 0 0 10px 10px; padding: 8px 4px; }
.nav-tab { display: flex; flex-direction: column; align-items: center; gap: 2px; font-size: 11px; color: #8a919f; cursor: pointer; }
.nav-tab.active { color: #3370ff; }
.nav-tab .el-icon { font-size: 18px; }

/* VIEW 移动端单元格列表 */
.r-list-mobile { display: flex; flex-direction: column; gap: 8px; }
.list-cell { display: flex; flex-direction: column; gap: 2px; padding: 12px; background: #f7f8fa; border-radius: 10px; }
.cell-title { font-size: 13px; color: #1f2329; font-weight: 500; }
.cell-meta { font-size: 12px; color: #86909c; }

/* ===== EP 通用 / 复合组件 ===== */
.r-ep { width: 100%; }
.r-menu { border-bottom: none; }
.r-menu.el-menu--horizontal { border-bottom: none; }
.r-carousel-item { display: flex; align-items: center; justify-content: center; height: 150px; background: #eef2f8; color: #646a73; border-radius: 8px; font-size: 13px; }
.r-collapse-content { font-size: 13px; color: #4e5969; line-height: 1.7; }
.r-watermark { min-height: 96px; }
.r-wm-box { display: flex; align-items: center; justify-content: center; min-height: 96px; background: #f7f8fa; border: 1px dashed #d9dde3; border-radius: 10px; color: #86909c; font-size: 13px; }
</style>
