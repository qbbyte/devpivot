<template>
  <el-drawer
    v-model="drawerVisible"
    title="历史记录"
    size="400px"
    :append-to-body="true"
    class="history-drawer"
    @closed="emit('update:visible', false)"
  >
    <div class="hd-body">
      <el-tabs v-model="activeTab" class="hd-tabs">
        <!-- ==================== Tab1 修改记录（腾讯文档式时间线） ==================== -->
        <el-tab-pane label="修改记录" name="history">
          <div v-loading="historyLoading" class="hd-timeline">
            <div v-for="group in groupedHistory" :key="group.label" class="ht-group">
              <div class="ht-group-label">{{ group.label }}</div>
              <el-timeline class="ht-timeline">
                <el-timeline-item
                  v-for="item in group.items"
                  :key="item.historyId"
                  :type="timelineType(item.action)"
                  placement="top"
                >
                  <div class="ht-item">
                    <div class="ht-head">
                      <span class="ht-avatar">{{ initial(item.operatorName) }}</span>
                      <span class="ht-op">{{ item.operatorName }}</span>
                      <span class="ht-tag" :class="'ht-tag-' + tagKind(item.action)">{{ actionText(item.action) }}</span>
                      <span class="ht-time" :title="item.createTime">{{ formatTime(item.createTime) }}</span>
                    </div>
                    <div class="ht-desc">
                      <span>{{ item.actionDesc || actionText(item.action) }}</span>
                      <span v-if="item.targetLabel" class="ht-target">{{ item.targetLabel }}</span>
                    </div>
                    <div v-if="item.changeSummary" class="ht-summary">{{ summaryText(item.changeSummary) }}</div>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <div v-if="!group.items.length" class="ht-empty">
                <el-empty description="暂无修改记录" :image-size="72" />
              </div>
            </div>
            <div v-if="historyTotal > historyList.length" class="ht-more">
              <el-button text type="primary" :loading="historyLoading" @click="loadHistoryMore">查看全部（{{ historyTotal }} 条）</el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- ==================== Tab2 版本（工具向） ==================== -->
        <el-tab-pane :label="'版本' + (versionList.length ? ' (' + versionList.length + ')' : '')" name="version">
          <div v-if="versionEnabled" class="hd-save">
            <div class="hd-save-row">
              <el-input v-model="saveName" size="small" placeholder="版本名称（留空自动命名）" maxlength="100" />
            </div>
            <div class="hd-save-row">
              <el-input v-model="saveRemark" size="small" placeholder="修改备注（可选）" maxlength="200" />
            </div>
            <el-button type="primary" size="small" class="hd-save-btn" :disabled="!canSave" :loading="saving" @click="onSaveVersion">
              <el-icon><Collection /></el-icon>&nbsp;保存当前为版本
            </el-button>
            <div v-if="!canSave" class="hd-save-hint">当前阶段暂无内容可保存</div>
          </div>
          <div v-else class="hd-save hd-save-disabled">
            <el-icon><InfoFilled /></el-icon>&nbsp;该阶段暂不支持版本管理
          </div>

          <div v-loading="versionLoading" class="hd-version-list">
            <div v-for="v in versionList" :key="v.versionId" class="hv-item" :class="{ 'hv-current': v.status === 'RELEASED' && v.versionId === currentVersionId }">
              <div class="hv-head">
                <span class="hv-no">{{ v.versionNo }}</span>
                <span class="hv-name" :title="v.versionName">{{ v.versionName || '未命名版本' }}</span>
                <span class="hv-status" :class="'hv-status-' + (v.status || 'DRAFT').toLowerCase()">{{ statusText(v.status) }}</span>
              </div>
              <div class="hv-meta">{{ v.createBy }} · {{ v.createTime }}<span v-if="v.changeRemark" class="hv-remark" :title="v.changeRemark"> · {{ v.changeRemark }}</span></div>
              <div class="hv-ops">
                <el-button v-if="v.status === 'DRAFT'" link type="primary" size="small" :loading="releasingId === v.versionId" @click="onRelease(v)">发布</el-button>
                <el-button v-if="v.status === 'RELEASED' && stage === 'PROTO'" link type="primary" size="small" :loading="restoringId === v.versionId" @click="onRestore(v)">还原</el-button>
                <el-button v-if="v.status === 'RELEASED' && stage !== 'PROTO'" link type="info" size="small" disabled>还原（待支持）</el-button>
                <el-button link type="danger" size="small" :loading="deletingId === v.versionId" @click="onDelete(v)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="!versionList.length" description="还没有版本，保存第一个版本吧" :image-size="72" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-drawer>
</template>

<script setup name="HistoryPanel">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listHistory } from '@/api/ai/history'
import { listVersions, saveVersion, releaseVersion, restoreVersion, deleteVersion } from '@/api/ai/version'

const props = defineProps({
  visible: { type: Boolean, default: false },
  projectId: { type: [Number, String], required: true },
  stage: { type: String, required: true },
  snapshot: { type: [String, Object, Array], default: null },
  versionEnabled: { type: Boolean, default: true }
})
const emit = defineEmits(['update:visible', 'restored'])

const drawerVisible = computed({
  get: () => props.visible,
  set: v => emit('update:visible', v)
})

const activeTab = ref('history')

/* ---------------- 修改记录 ---------------- */
const historyList = ref([])
const historyTotal = ref(0)
const historyLoading = ref(false)
const historyPage = ref(1)
const PAGE_SIZE = 20

function loadHistory(reset = true) {
  if (reset) { historyPage.value = 1; historyList.value = [] }
  historyLoading.value = true
  listHistory({ projectId: props.projectId, stage: props.stage, pageNum: historyPage.value, pageSize: PAGE_SIZE })
    .then(rows => {
      if (reset) historyList.value = rows
      else historyList.value = historyList.value.concat(rows)
      historyTotal.value = rows.length < PAGE_SIZE && !reset ? historyTotal.value : (historyTotal.value || 0)
    })
    .catch(() => { historyList.value = reset ? [] : historyList.value })
    .finally(() => { historyLoading.value = false })
}

function loadHistoryMore() {
  historyPage.value += 1
  loadHistory(false)
}

watch(() => props.visible, v => {
  if (v) {
    activeTab.value = 'history'
    loadHistory()
  }
})

/* ---------------- 版本 ---------------- */
const versionList = ref([])
const versionLoading = ref(false)
const saveName = ref('')
const saveRemark = ref('')
const saving = ref(false)
const releasingId = ref(null)
const restoringId = ref(null)
const deletingId = ref(null)
const currentVersionId = ref(null)

const canSave = computed(() => {
  if (!props.versionEnabled || props.snapshot == null) return false
  const s = snapshotStr.value
  return s !== '' && s !== 'null' && s !== '""'
})

const snapshotStr = computed(() => {
  try {
    const raw = props.snapshot
    if (raw == null) return ''
    const v = typeof raw === 'string' ? raw : JSON.stringify(raw)
    return v || ''
  } catch (e) {
    return ''
  }
})

function loadVersions() {
  versionLoading.value = true
  listVersions({ projectId: props.projectId, stage: props.stage })
    .then(list => {
      versionList.value = list || []
      const released = [...(list || [])].filter(v => v.status === 'RELEASED')
      currentVersionId.value = released.length ? released[0].versionId : null
    })
    .catch(() => { versionList.value = [] })
    .finally(() => { versionLoading.value = false })
}

watch(() => props.visible, v => { if (v && props.versionEnabled) loadVersions() })

function onSaveVersion() {
  const name = saveName.value.trim()
  saving.value = true
  saveVersion({
    projectId: props.projectId,
    stage: props.stage,
    artifactType: props.stage,
    versionName: name,
    snapshot: snapshotStr.value,
    sourceType: 'MANUAL',
    changeRemark: saveRemark.value.trim()
  })
    .then(() => {
      ElMessage.success('已保存版本')
      saveName.value = ''
      saveRemark.value = ''
      loadVersions()
      loadHistory(true)
    })
    .catch(() => {})
    .finally(() => { saving.value = false })
}

function onRelease(row) {
  ElMessageBox.confirm(`确定发布版本 ${row.versionNo} ？发布后可作为还原目标。`, '发布版本', { type: 'warning' })
    .then(() => {
      releasingId.value = row.versionId
      releaseVersion(row.versionId)
        .then(() => { ElMessage.success('已发布'); loadVersions(); loadHistory(true) })
        .catch(() => {})
        .finally(() => { releasingId.value = null })
    })
    .catch(() => {})
}

function onRestore(row) {
  ElMessageBox.confirm(`还原将覆盖当前阶段内容并生成新版本，确定还原到「${row.versionName || row.versionNo}」？`, '还原版本', { type: 'warning' })
    .then(() => {
      restoringId.value = row.versionId
      restoreVersion(row.versionId)
        .then(res => {
          ElMessage.success('已还原并生成新版本')
          emit('restored', { version: res, content: res.snapshot, artifactType: res.artifactType })
          loadVersions()
          loadHistory(true)
        })
        .catch(() => {})
        .finally(() => { restoringId.value = null })
    })
    .catch(() => {})
}

function onDelete(row) {
  ElMessageBox.confirm(`确定删除版本 ${row.versionNo}（${row.versionName || '未命名'}）？删除后不可恢复。`, '删除版本', { type: 'warning' })
    .then(() => {
      deletingId.value = row.versionId
      deleteVersion(row.versionId)
        .then(() => { ElMessage.success('已删除'); loadVersions(); loadHistory(true) })
        .catch(() => {})
        .finally(() => { deletingId.value = null })
    })
    .catch(() => {})
}

/* ---------------- 展示工具 ---------------- */
const GROUP_LABELS = [{ re: /^今天/, label: '今天' }, { re: /^昨天/, label: '昨天' }]

const groupedHistory = computed(() => {
  const groups = [
    { label: '今天', items: [] },
    { label: '昨天', items: [] },
    { label: '更早', items: [] }
  ]
  const now = new Date()
  const todayStr = now.toDateString()
  const yesterday = new Date(now.getTime() - 86400000).toDateString()
  for (const item of historyList.value) {
    const d = new Date(String(item.createTime).replace(/-/g, '/'))
    const ds = d.toDateString()
    if (ds === todayStr) groups[0].items.push(item)
    else if (ds === yesterday) groups[1].items.push(item)
    else groups[2].items.push(item)
  }
  return groups.filter(g => g.items.length)
})

const ACTION_MAP = {
  CREATE: '创建', UPDATE: '编辑', DELETE: '删除', RESTORE: '还原', RELEASE: '发布', EXPORT: '导出', ROLLBACK: '回退'
}
function actionText(a) { return ACTION_MAP[a] || a || '' }
function tagKind(a) {
  if (a === 'CREATE' || a === 'RELEASE') return 'blue'
  if (a === 'UPDATE') return 'green'
  if (a === 'DELETE') return 'red'
  if (a === 'RESTORE') return 'amber'
  return 'gray'
}
function timelineType(a) {
  if (a === 'DELETE') return 'danger'
  if (a === 'RESTORE') return 'warning'
  if (a === 'UPDATE') return 'success'
  return 'primary'
}
function statusText(s) {
  return { DRAFT: '草稿', RELEASED: '正式', ARCHIVED: '归档' }[s] || s || '草稿'
}
function initial(name) { return (name || '?').charAt(0).toUpperCase() }
function formatTime(t) {
  if (!t) return ''
  const d = new Date(String(t).replace(/-/g, '/'))
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400 && d.getDate() === now.getDate()) return d.getHours() + ':' + String(d.getMinutes()).padStart(2, '0')
  if (diff < 172800) return '昨天 ' + d.getHours() + ':' + String(d.getMinutes()).padStart(2, '0')
  return (d.getMonth() + 1) + '-' + d.getDate()
}
function summaryText(s) {
  try {
    const o = JSON.parse(s)
    if (o && typeof o === 'object') {
      const parts = []
      if (o.added) parts.push('新增 ' + o.added)
      if (o.removed) parts.push('删除 ' + o.removed)
      if (o.modified) parts.push('修改 ' + o.modified)
      if (o.versionNo) return '版本 ' + o.versionNo
      return parts.join(' · ')
    }
  } catch (e) { /* ignore */ }
  return ''
}

defineExpose({ loadVersions, loadHistory })
</script>

<style scoped>
.hd-body { min-height: 320px; }
.hd-tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
.hd-timeline { min-height: 200px; }
.ht-group-label { font-size: 12px; font-weight: 500; color: var(--c-text-muted, #64748b); margin: 8px 0 4px; }
.ht-timeline { padding-left: 4px; }
.ht-item { padding-bottom: 4px; }
.ht-head { display: flex; align-items: center; gap: 6px; }
.ht-avatar {
  width: 22px; height: 22px; border-radius: 50%; flex-shrink: 0;
  background: var(--c-primary-bg, #eff4ff); color: var(--c-primary, #2563eb);
  font-size: 12px; font-weight: 500; display: inline-flex; align-items: center; justify-content: center;
}
.ht-op { font-size: 13px; font-weight: 500; color: var(--c-text, #1e293b); }
.ht-tag {
  font-size: 11px; line-height: 1; padding: 3px 7px; border-radius: 999px; white-space: nowrap;
}
.ht-tag-blue { background: var(--c-primary-bg, #eff4ff); color: var(--c-primary, #2563eb); }
.ht-tag-green { background: var(--c-accent-bg, #ecfdf5); color: var(--c-accent, #059669); }
.ht-tag-red { background: var(--c-danger-bg, #fef2f2); color: var(--c-danger, #ef4444); }
.ht-tag-amber { background: var(--c-warning-bg, #fffbeb); color: var(--c-warning, #f59e0b); }
.ht-tag-gray { background: var(--c-border-light, #f1f5f9); color: var(--c-text-muted, #64748b); }
.ht-time { margin-left: auto; font-size: 11px; color: var(--c-text-subtle, #94a3b8); white-space: nowrap; }
.ht-desc { font-size: 12px; color: var(--c-text, #1e293b); margin-top: 3px; display: flex; gap: 6px; align-items: center; }
.ht-target { font-size: 11px; color: var(--c-primary, #2563eb); background: var(--c-primary-bg, #eff4ff); padding: 1px 6px; border-radius: 4px; }
.ht-summary { font-size: 11px; color: var(--c-text-muted, #64748b); margin-top: 2px; }
.ht-more { text-align: center; margin-top: 8px; }
.ht-empty { padding: 8px 0; }

.hd-save {
  display: flex; flex-direction: column; gap: 8px;
  padding: 12px; border: 1px solid var(--c-border, #e2e8f0); border-radius: 10px; margin-bottom: 12px;
  background: var(--c-surface, #fff);
}
.hd-save-btn { align-self: flex-start; }
.hd-save-hint { font-size: 11px; color: var(--c-text-subtle, #94a3b8); }
.hd-save-disabled { flex-direction: row; align-items: center; font-size: 12px; color: var(--c-text-muted, #64748b); background: var(--c-border-light, #f1f5f9); }

.hd-version-list { min-height: 120px; }
.hv-item {
  padding: 10px 12px; border: 1px solid var(--c-border, #e2e8f0); border-radius: 10px; margin-bottom: 8px;
  background: var(--c-surface, #fff);
}
.hv-item.hv-current { border-color: var(--c-primary, #2563eb); box-shadow: 0 0 0 1px var(--c-primary, #2563eb) inset; }
.hv-head { display: flex; align-items: center; gap: 8px; }
.hv-no { font-size: 13px; font-weight: 500; color: var(--c-primary, #2563eb); }
.hv-name { font-size: 13px; font-weight: 500; color: var(--c-text, #1e293b); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hv-status { margin-left: auto; font-size: 11px; padding: 2px 8px; border-radius: 999px; white-space: nowrap; }
.hv-status-draft { background: var(--c-border-light, #f1f5f9); color: var(--c-text-muted, #64748b); }
.hv-status-released { background: var(--c-accent-bg, #ecfdf5); color: var(--c-accent, #059669); }
.hv-status-archived { background: var(--c-border-light, #f1f5f9); color: var(--c-text-subtle, #94a3b8); }
.hv-meta { font-size: 11px; color: var(--c-text-muted, #64748b); margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hv-remark { color: var(--c-text-subtle, #94a3b8); }
.hv-ops { margin-top: 6px; display: flex; gap: 2px; }
</style>
