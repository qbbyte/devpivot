<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="项目ID" prop="projectId">
        <el-input
          v-model="queryParams.projectId"
          placeholder="请输入项目ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="阶段" prop="stage">
        <el-select v-model="queryParams.stage" placeholder="请选择阶段" clearable style="width: 160px">
          <el-option v-for="s in stageOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="RELEASED" />
          <el-option label="已归档" value="ARCHIVED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="artifactList">
      <el-table-column label="版本ID" align="center" prop="versionId" width="80" />
      <el-table-column label="项目ID" align="center" prop="projectId" width="80" />
      <el-table-column label="阶段" align="center" width="90">
        <template #default="scope">{{ stageLabel(scope.row.stage) }}</template>
      </el-table-column>
      <el-table-column label="产物类型" align="center" prop="artifactType" width="120" show-overflow-tooltip />
      <el-table-column label="版本号" align="center" prop="versionNo" width="110" />
      <el-table-column label="版本名称" align="left" prop="versionName" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" align="center" prop="sourceType" width="90" />
      <el-table-column label="生成模型" align="center" prop="sourceModel" width="130" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="230">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['system:artifact:query']">快照</el-button>
          <el-button link type="primary" icon="Upload" @click="handleRelease(scope.row)" v-if="scope.row.status === 'DRAFT'" v-hasPermi="['system:artifact:release']">发布</el-button>
          <el-button link type="primary" icon="RefreshLeft" @click="handleRestore(scope.row)" v-if="scope.row.status === 'RELEASED'" v-hasPermi="['system:artifact:restore']">恢复</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:artifact:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 查看版本快照对话框 -->
    <el-dialog title="版本快照" v-model="viewOpen" width="860px" append-to-body>
      <div class="view-meta">
        <span>版本ID：{{ viewForm.versionId }}</span>
        <span>项目ID：{{ viewForm.projectId }}</span>
        <span>阶段：{{ stageLabel(viewForm.stage) }}</span>
        <span>产物类型：{{ viewForm.artifactType }}</span>
        <span>版本号：{{ viewForm.versionNo }}</span>
        <span>快照哈希：{{ viewForm.snapshotHash || '-' }}</span>
      </div>
      <el-input
        v-model="viewForm.snapshot"
        type="textarea"
        :rows="18"
        readonly
      />
    </el-dialog>
  </div>
</template>

<script setup name="Artifact">
import { listArtifact, getArtifact, delArtifact, releaseArtifact, restoreArtifact } from "@/api/ai/artifact"

const { proxy } = getCurrentInstance()

/** 版本状态展示（后端枚举 DRAFT/RELEASED/ARCHIVED） */
function statusText(s) {
  if (s === 'RELEASED') return '已发布'
  if (s === 'ARCHIVED') return '已归档'
  return '草稿'
}

function statusTagType(s) {
  if (s === 'RELEASED') return 'success'
  if (s === 'ARCHIVED') return 'info'
  return 'warning'
}

const stageOptions = [
  { label: '需求', value: 'REQ' },
  { label: '澄清', value: 'CLARIFY' },
  { label: 'PRD', value: 'PRD' },
  { label: '原型', value: 'PROTO' },
  { label: '架构', value: 'ARCH' },
  { label: '技术方案', value: 'TECH' },
  { label: '数据库', value: 'DB' }
]

const artifactList = ref([])
const viewOpen = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const viewForm = ref({})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    stage: undefined,
    status: undefined,
  }
})

const { queryParams } = toRefs(data)

/** 阶段标签 */
function stageLabel(val) {
  const s = stageOptions.find(i => i.value === val)
  return s ? s.label : (val || '-')
}

/** 查询产物版本列表 */
function getList() {
  loading.value = true
  listArtifact(queryParams.value).then(response => {
    artifactList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 查看快照 */
function handleView(row) {
  getArtifact(row.versionId).then(response => {
    viewForm.value = response.data
    viewOpen.value = true
  })
}

/** 发布版本 */
function handleRelease(row) {
  proxy.$modal.confirm('是否确认发布版本"' + row.versionNo + '"？发布后将成为该产物的当前版本。').then(function() {
    return releaseArtifact(row.versionId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("发布成功")
  }).catch(() => {})
}

/** 恢复版本 */
function handleRestore(row) {
  proxy.$modal.confirm('是否确认将版本"' + row.versionNo + '"恢复为当前版本？').then(function() {
    return restoreArtifact(row.versionId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("恢复成功")
  }).catch(() => {})
}

/** 删除版本 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除版本编号为"' + row.versionId + '"的数据项？').then(function() {
    return delArtifact(row.versionId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

getList()
</script>

<style lang="scss" scoped>
.view-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #909399;
}
</style>
