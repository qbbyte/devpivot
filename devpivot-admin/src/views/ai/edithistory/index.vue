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
      <el-form-item label="操作人" prop="operatorName">
        <el-input
          v-model="queryParams.operatorName"
          placeholder="请输入操作人"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="historyList">
      <el-table-column label="记录ID" align="center" prop="historyId" width="80" />
      <el-table-column label="项目ID" align="center" prop="projectId" width="80" />
      <el-table-column label="阶段" align="center" width="90">
        <template #default="scope">{{ stageLabel(scope.row.stage) }}</template>
      </el-table-column>
      <el-table-column label="产物类型" align="center" prop="artifactType" width="110" show-overflow-tooltip />
      <el-table-column label="版本号" align="center" prop="versionNo" width="100">
        <template #default="scope">{{ scope.row.versionNo || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" prop="actionDesc" width="100">
        <template #default="scope">
          <el-tag :type="actionTagType(scope.row.action)" size="small">{{ scope.row.actionDesc || scope.row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作人" align="center" prop="operatorName" width="100" />
      <el-table-column label="变更对象" align="left" prop="targetLabel" show-overflow-tooltip />
      <el-table-column label="变更摘要" align="left" prop="changeSummary" show-overflow-tooltip />
      <el-table-column label="IP" align="center" prop="ip" width="120" />
      <el-table-column label="操作时间" align="center" prop="createTime" width="160" />
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="Edithistory">
import { listEdithistory } from "@/api/ai/edithistory"

const { proxy } = getCurrentInstance()

const stageOptions = [
  { label: '需求', value: 'REQ' },
  { label: '澄清', value: 'CLARIFY' },
  { label: 'PRD', value: 'PRD' },
  { label: '原型', value: 'PROTO' },
  { label: '架构', value: 'ARCH' },
  { label: '技术方案', value: 'TECH' },
  { label: '数据库', value: 'DB' }
]

const historyList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    stage: undefined,
    operatorName: undefined,
  }
})

const { queryParams } = toRefs(data)

/** 阶段标签 */
function stageLabel(val) {
  const s = stageOptions.find(i => i.value === val)
  return s ? s.label : (val || '-')
}

/** 操作类型标签色 */
function actionTagType(action) {
  if (action === 'create') return 'success'
  if (action === 'delete') return 'danger'
  if (action === 'restore' || action === 'release') return 'warning'
  return 'info'
}

/** 查询编辑历史列表 */
function getList() {
  loading.value = true
  listEdithistory(queryParams.value).then(response => {
    historyList.value = response.rows
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

getList()
</script>
