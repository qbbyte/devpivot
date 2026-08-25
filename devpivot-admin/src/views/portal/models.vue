<template>
  <div class="models-page">
    <PortalHeader />
    <main class="models-main">
      <div class="page-head">
        <div>
          <h2>我的模型 Key</h2>
          <p class="page-desc">
            配置你自己的大模型 API Key。生成方案时将优先使用你的 Key（按供应商匹配），未配置则回退平台的全局模型配置。
            密钥加密存储，页面仅展示脱敏后 4 位。
          </p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增 Key</el-button>
      </div>

      <div class="global-models">
        <div class="gm-head">
          <h3>系统全局模型</h3>
          <span class="gm-tip">平台已启用以下模型，可直接使用；想用自己的 Key，按相同供应商在下方「我的 API Key」配置即可（生成时优先使用你的 Key）</span>
        </div>
        <el-table v-loading="gmLoading" :data="globalModels" class="gm-table" empty-text="暂无启用的全局模型">
          <el-table-column prop="modelName" label="模型名称" min-width="180" />
          <el-table-column prop="modelCode" label="模型标识" min-width="160" />
          <el-table-column prop="provider" label="供应商" width="140" />
        </el-table>
      </div>

      <el-table v-loading="loading" :data="list" class="keys-table" empty-text="还没有配置 Key">
        <el-table-column prop="provider" label="供应商" min-width="140" />
        <el-table-column label="API Key" min-width="220">
          <template #default="{ row }">
            <span class="masked">{{ row.maskedApiKey || '未设置' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isActive === 'Y' ? 'success' : 'info'" size="small">
              {{ row.isActive === 'Y' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="dialogVisible" :title="form.keyId ? '编辑 Key' : '新增 Key'" width="480px">
        <el-form :model="form" label-width="90px">
          <el-form-item label="供应商">
            <el-select v-model="form.provider" placeholder="选择或输入供应商" allow-create filterable style="width: 100%">
              <el-option label="OpenAI" value="openai" />
              <el-option label="DeepSeek" value="deepseek" />
              <el-option label="通义千问" value="qwen" />
              <el-option label="Ollama" value="ollama" />
            </el-select>
          </el-form-item>
          <el-form-item label="API Key">
            <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-..." />
            <div v-if="form.keyId" class="form-tip">留空表示保留原 Key 不变</div>
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="form.isActive" active-value="Y" inactive-value="N" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </template>
      </el-dialog>
    </main>
  </div>
</template>

<script setup name="PortalModels">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PortalHeader from './components/PortalHeader.vue'
import { listMyKeys, addMyKey, updateMyKey, delMyKey } from '@/api/ai/userkey'
import { getGlobalModels } from '@/api/ai/modelconfig'

const loading = ref(false)
const saving = ref(false)
const gmLoading = ref(false)
const globalModels = ref([])
const list = ref([])
const dialogVisible = ref(false)
const form = reactive({ keyId: null, provider: '', apiKey: '', isActive: 'Y' })

function load() {
  loading.value = true
  listMyKeys().then(res => {
    list.value = res.data || []
  }).finally(() => {
    loading.value = false
  })
}

function openAdd() {
  Object.assign(form, { keyId: null, provider: '', apiKey: '', isActive: 'Y' })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { keyId: row.keyId, provider: row.provider, apiKey: '', isActive: row.isActive || 'Y' })
  dialogVisible.value = true
}

function handleSave() {
  if (!form.provider) {
    ElMessage.warning('请填写供应商')
    return
  }
  if (!form.apiKey && !form.keyId) {
    ElMessage.warning('请填写 API Key')
    return
  }
  saving.value = true
  const req = form.keyId ? updateMyKey({ ...form }) : addMyKey({ ...form })
  req.then(() => {
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  }).finally(() => {
    saving.value = false
  })
}

function handleDel(row) {
  ElMessageBox.confirm(`确认删除「${row.provider}」的 Key？`, '删除确认', { type: 'warning' })
    .then(() => delMyKey(row.keyId))
    .then(() => {
      ElMessage.success('已删除')
      load()
    })
    .catch(() => {})
}

function loadGlobal() {
  gmLoading.value = true
  getGlobalModels().then(res => {
    globalModels.value = res.data || []
  }).finally(() => {
    gmLoading.value = false
  })
}

onMounted(() => {
  load()
  loadGlobal()
})
</script>

<style scoped>
.models-page {
  min-height: 100vh;
  background: var(--c-bg);
}
.models-main {
  max-width: 1080px;
  margin: 0 auto;
  padding: 28px 24px 60px;
}
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
}
.page-head h2 {
  font-size: 22px;
  color: var(--c-text);
  margin: 0 0 6px;
}
.page-desc {
  font-size: 13px;
  color: var(--c-text-muted);
  margin: 0;
  max-width: 680px;
  line-height: 1.6;
}
.global-models {
  margin-bottom: 32px;
}
.gm-head {
  margin-bottom: 12px;
}
.gm-head h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0 0 6px;
}
.gm-tip {
  font-size: 13px;
  color: var(--c-text-muted);
  line-height: 1.6;
  margin: 0;
}
.gm-table {
  background: var(--c-surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.keys-table {
  background: var(--c-surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.masked {
  font-family: monospace;
  color: var(--c-text-subtle);
}
.form-tip {
  font-size: 12px;
  color: var(--c-text-subtle);
  line-height: 1.4;
  margin-top: 4px;
}
</style>
