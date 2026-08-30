<template>
  <div class="task-board">
    <div class="tb-toolbar">
      <span class="tb-title">任务看板</span>
      <span class="tb-count">共 {{ tasks.length }} 项</span>
      <div class="tb-spacer"></div>
      <el-button v-if="isManager" type="primary" size="small" @click="openCreate">
        <el-icon><Plus /></el-icon><span>新建任务</span>
      </el-button>
    </div>

    <div v-loading="loading" class="tb-columns">
      <div v-for="col in columns" :key="col.status" class="tb-col">
        <div class="tb-col-head">
          <span class="tb-dot" :style="{ background: col.color }"></span>
          <span>{{ col.label }}</span>
          <span class="tb-col-count">{{ groupByStatus[col.status]?.length || 0 }}</span>
        </div>
        <div class="tb-col-body">
          <div v-for="t in groupByStatus[col.status]" :key="t.id" class="tb-card" @click="openDetail(t)">
            <div class="tb-card-title">{{ t.title }}</div>
            <div class="tb-card-meta">
              <el-tag v-if="t.stage" size="small" class="tb-stage">{{ stageLabel(t.stage) }}</el-tag>
              <el-tag :style="priorityStyle(t.priority)" size="small">{{ priorityLabel(t.priority) }}</el-tag>
            </div>
            <div class="tb-card-foot">
              <span class="tb-assignee">
                <el-avatar :size="20" class="tb-avatar">{{ (t.assigneeName || '未分配').slice(0, 1) }}</el-avatar>
                <span>{{ t.assigneeName || '未分配' }}</span>
              </span>
              <span v-if="t.dueAt" class="tb-due" :class="{ overdue: isOverdue(t.dueAt) }">{{ fmtDate(t.dueAt) }}</span>
            </div>
            <div class="tb-card-actions" @click.stop>
              <template v-if="t.status === 'TODO' && (isManager || !t.assigneeId)">
                <el-button v-if="!t.assigneeId" size="small" link type="primary" @click="claim(t)">认领</el-button>
              </template>
              <template v-if="t.status === 'DOING' && isAssignee(t)">
                <el-button size="small" link type="primary" @click="submit(t)">提交</el-button>
              </template>
              <template v-if="t.status === 'REVIEW' && isManager">
                <el-button size="small" link type="success" @click="review(t, true)">通过</el-button>
                <el-button size="small" link type="warning" @click="review(t, false)">打回</el-button>
              </template>
              <el-button v-if="isManager" size="small" link type="danger" @click="remove(t)">删除</el-button>
            </div>
          </div>
          <div v-if="!groupByStatus[col.status]?.length" class="tb-empty">暂无任务</div>
        </div>
      </div>
    </div>

    <!-- 新建 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑任务' : '新建任务'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="任务标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="任务描述" />
        </el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="form.stage" placeholder="关联流水线阶段(可选)" clearable style="width:100%">
            <el-option v-for="s in stageOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.assigneeId" placeholder="未分配(可稍后认领)" clearable style="width:100%">
            <el-option v-for="m in members" :key="m.userId" :label="m.nickName || m.userName" :value="m.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width:100%">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止">
          <el-date-picker v-model="form.dueAt" type="datetime" placeholder="截止时间" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTeamTasks, createTeamTask, updateTeamTask, claimTeamTask, submitTeamTask, reviewTeamTask, deleteTeamTask } from '@/api/ai/team'

const props = defineProps({
  teamId: { type: Number, required: true },
  members: { type: Array, default: () => [] },
  currentUserId: { type: Number, required: true },
  myRole: { type: String, default: 'MEMBER' }
})

const isManager = computed(() => props.myRole === 'OWNER' || props.myRole === 'ADMIN')

const tasks = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const editingId = ref(null)
const form = ref({ title: '', description: '', stage: '', assigneeId: null, priority: 'MEDIUM', dueAt: null })

const columns = [
  { status: 'TODO', label: '待办', color: '#9aa0a6' },
  { status: 'DOING', label: '进行中', color: '#2563eb' },
  { status: 'REVIEW', label: '待复核', color: '#f59e0b' },
  { status: 'DONE', label: '完成', color: '#10b981' }
]

const stageOptions = [
  { value: 'REQ', label: '需求' },
  { value: 'CLARIFY', label: '澄清' },
  { value: 'PRD', label: 'PRD' },
  { value: 'PROTO', label: '原型' },
  { value: 'TECH', label: '技术' },
  { value: 'DB', label: '数据库' }
]

const groupByStatus = computed(() => {
  const map = { TODO: [], DOING: [], REVIEW: [], DONE: [] }
  for (const t of tasks.value) {
    if (map[t.status]) map[t.status].push(t)
  }
  return map
})

function stageLabel(s) {
  return (stageOptions.find(o => o.value === s) || {}).label || s
}
function priorityLabel(p) {
  return p === 'HIGH' ? '高' : p === 'LOW' ? '低' : '中'
}
function priorityStyle(p) {
  if (p === 'HIGH') return { color: '#e24b4a', borderColor: '#f7c1c1', background: '#fcebeb' }
  if (p === 'LOW') return { color: '#639922', borderColor: '#c0dd97', background: '#eaf3de' }
  return { color: '#854f0b', borderColor: '#fac775', background: '#faedda' }
}
function fmtDate(v) {
  return String(v || '').slice(0, 16)
}
function isOverdue(v) {
  return v && new Date(v).getTime() < Date.now()
}
function isAssignee(t) {
  return t.assigneeId && t.assigneeId === props.currentUserId
}

function fetchTasks() {
  loading.value = true
  listTeamTasks(props.teamId, {}).then(res => {
    tasks.value = res.data || []
  }).finally(() => { loading.value = false })
}

function openCreate() {
  editing.value = false
  editingId.value = null
  form.value = { title: '', description: '', stage: '', assigneeId: null, priority: 'MEDIUM', dueAt: null }
  dialogVisible.value = true
}

function submitForm() {
  if (!form.value.title) { ElMessage.warning('请填写任务标题'); return }
  const payload = { ...form.value }
  if (editing.value) {
    updateTeamTask(props.teamId, editingId.value, payload).then(() => { ElMessage.success('已保存'); dialogVisible.value = false; fetchTasks() })
  } else {
    createTeamTask(props.teamId, payload).then(() => { ElMessage.success('已创建'); dialogVisible.value = false; fetchTasks() })
  }
}

function claim(t) {
  claimTeamTask(props.teamId, t.id).then(() => { ElMessage.success('已认领'); fetchTasks() })
}
function submit(t) {
  submitTeamTask(props.teamId, t.id).then(() => { ElMessage.success('已提交复核'); fetchTasks() })
}
function review(t, approved) {
  reviewTeamTask(props.teamId, t.id, approved).then(() => { ElMessage.success(approved ? '已通过' : '已打回'); fetchTasks() })
}
function remove(t) {
  ElMessageBox.confirm('确认删除该任务？', '提示', { type: 'warning' }).then(() => {
    deleteTeamTask(props.teamId, t.id).then(() => { ElMessage.success('已删除'); fetchTasks() })
  }).catch(() => {})
}
function openDetail(t) {
  if (isManager.value) {
    editing.value = true
    editingId.value = t.id
    form.value = { title: t.title, description: t.description, stage: t.stage, assigneeId:  t.assigneeId, priority: t.priority, dueAt: t.dueAt }
    dialogVisible.value = true
  }
}

onMounted(fetchTasks)
</script>

<style scoped>
.task-board { padding: 4px 0; }
.tb-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.tb-title { font-size: 15px; font-weight: 600; color: var(--c-text, #1f2937); }
.tb-count { font-size: 12px; color: #6b7280; }
.tb-spacer { flex: 1; }
.tb-columns { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.tb-col { background: #f7f9fc; border: 1px solid #e3e9f2; border-radius: 10px; padding: 10px; min-height: 200px; }
.tb-col-head { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; color: #1f2937; margin-bottom: 10px; }
.tb-dot { width: 10px; height: 10px; border-radius: 50%; }
.tb-col-count { margin-left: auto; font-size: 12px; color: #6b7280; }
.tb-col-body { display: flex; flex-direction: column; gap: 10px; }
.tb-card { background: #fff; border: 1px solid #e3e9f2; border-radius: 8px; padding: 10px; cursor: pointer; transition: box-shadow .15s; }
.tb-card:hover { box-shadow: 0 2px 8px rgba(37,99,235,.12); }
.tb-card-title { font-size: 13px; color: #1f2937; margin-bottom: 8px; line-height: 1.4; }
.tb-card-meta { display: flex; gap: 6px; margin-bottom: 8px; }
.tb-stage { color: #185fa5; border-color: #b5d4f4; background: #e6f1fb; }
.tb-card-foot { display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: #6b7280; }
.tb-assignee { display: flex; align-items: center; gap: 6px; }
.tb-avatar { background: #2563eb; color: #fff; font-size: 12px; }
.tb-due.overdue { color: #e24b4a; }
.tb-card-actions { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 8px; }
.tb-empty { font-size: 12px; color: #9aa0a6; text-align: center; padding: 16px 0; }
</style>
