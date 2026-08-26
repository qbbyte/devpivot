<template>
  <div class="history-entry" @click="open">
    <div v-if="recentUsers.length" class="he-avatars" :title="entryTitle">
      <span v-for="(u, i) in recentUsers" :key="u.operatorId + '-' + i" class="he-avatar" :style="{ zIndex: 10 - i }">
        {{ initial(u.operatorName) }}
      </span>
      <span class="he-time">{{ recentTimeText }}</span>
    </div>
    <div v-else class="he-empty" title="查看历史记录">
      <el-icon class="he-icon"><Clock /></el-icon>
      <span class="he-label">历史记录</span>
    </div>

    <HistoryPanel
      :visible="panelVisible"
      :project-id="projectId"
      :stage="stage"
      :snapshot="snapshot"
      :version-enabled="versionEnabled"
      @update:visible="panelVisible = $event"
      @restored="emit('restored', $event)"
    />
  </div>
</template>

<script setup name="HistoryEntry">
import { ref, computed, onMounted } from 'vue'
import { recentHistory } from '@/api/ai/history'
import HistoryPanel from './HistoryPanel.vue'

const props = defineProps({
  projectId: { type: [Number, String], required: true },
  stage: { type: String, required: true },
  snapshot: { type: [String, Object, Array], default: null },
  versionEnabled: { type: Boolean, default: true }
})
const emit = defineEmits(['restored'])

const panelVisible = ref(false)
const recent = ref([])

function loadRecent() {
  if (!props.projectId) return
  recentHistory(props.projectId)
    .then(list => { recent.value = list || [] })
    .catch(() => { recent.value = [] })
}

onMounted(loadRecent)

const recentUsers = computed(() => {
  const seen = new Set()
  const out = []
  for (const item of recent.value) {
    if (!item.operatorName) continue
    if (seen.has(item.operatorName)) continue
    seen.add(item.operatorName)
    out.push({ operatorId: item.operatorId, operatorName: item.operatorName })
    if (out.length >= 3) break
  }
  return out
})

const recentTimeText = computed(() => {
  const first = recent.value[0]
  if (!first || !first.createTime) return '历史记录'
  return relativeTime(first.createTime)
})

const entryTitle = computed(() => {
  const names = recentUsers.value.map(u => u.operatorName).join('、')
  return names ? names + ' · ' + recentTimeText.value : '查看历史记录'
})

function open() {
  loadRecent()
  panelVisible.value = true
}

function relativeTime(t) {
  const d = new Date(String(t).replace(/-/g, '/'))
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚更新'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前更新'
  if (diff < 86400 && d.getDate() === now.getDate()) return '今天 ' + d.getHours() + ':' + String(d.getMinutes()).padStart(2, '0')
  if (diff < 172800) return '昨天更新'
  return (d.getMonth() + 1) + '-' + d.getDate() + ' 更新'
}

function initial(name) { return (name || '?').charAt(0).toUpperCase() }

defineExpose({ open })
</script>

<style scoped>
.history-entry {
  display: inline-flex; align-items: center; cursor: pointer;
  padding: 4px 10px; border-radius: 8px; transition: background-color 0.2s;
}
.history-entry:hover { background: var(--c-primary-bg, #eff4ff); }
.he-avatars { display: inline-flex; align-items: center; }
.he-avatar {
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--c-primary, #2563eb); color: #fff;
  font-size: 12px; font-weight: 500;
  display: inline-flex; align-items: center; justify-content: center;
  border: 2px solid var(--c-surface, #fff);
  margin-left: -6px;
}
.he-avatar:first-child { margin-left: 0; }
.he-time { margin-left: 8px; font-size: 12px; color: var(--c-text-muted, #64748b); white-space: nowrap; }
.he-empty { display: inline-flex; align-items: center; gap: 4px; }
.he-icon { font-size: 14px; color: var(--c-text-subtle, #94a3b8); }
.he-label { font-size: 12px; color: var(--c-text-muted, #64748b); }
</style>
