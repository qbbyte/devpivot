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
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
          <el-option label="进行中" value="0" />
          <el-option label="已提交" value="1" />
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

    <el-table v-loading="loading" :data="sessionList">
      <el-table-column label="会话ID" align="center" prop="sessionId" width="80" />
      <el-table-column label="项目ID" align="center" prop="projectId" width="80" />
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === '1' ? 'success' : 'info'" size="small">
            {{ scope.row.status === '1' ? '已提交' : '进行中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建人" align="center" prop="createBy" width="110" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="提交时间" align="center" prop="submitTime" width="160">
        <template #default="scope">{{ scope.row.submitTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['system:clarifysession:query']">查看</el-button>
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

    <!-- 查看澄清会话详情对话框 -->
    <el-dialog title="澄清会话详情" v-model="viewOpen" width="860px" append-to-body>
      <div class="view-meta">
        <span>会话ID：{{ viewForm.sessionId }}</span>
        <span>项目ID：{{ viewForm.projectId }}</span>
        <span>状态：{{ viewForm.status === '1' ? '已提交' : '进行中' }}</span>
        <span>提交时间：{{ viewForm.submitTime || '-' }}</span>
      </div>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="对话记录" name="conversation">
          <el-input v-model="prettyConversation" type="textarea" :rows="14" readonly />
        </el-tab-pane>
        <el-tab-pane label="澄清结论" name="conclusion">
          <el-input v-model="prettyConclusion" type="textarea" :rows="14" readonly placeholder="暂无结论（未提交）" />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup name="Clarifysession">
import { listClarifysession, getClarifysession } from "@/api/ai/clarifysession"

const { proxy } = getCurrentInstance()

const sessionList = ref([])
const viewOpen = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const viewForm = ref({})
const activeTab = ref('conversation')

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    status: undefined,
  }
})

const { queryParams } = toRefs(data)

/** JSON 美化展示 */
const prettyConversation = computed(() => prettyJson(viewForm.value.conversation))
const prettyConclusion = computed(() => prettyJson(viewForm.value.conclusion))

function prettyJson(text) {
  if (!text) return ''
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch (e) {
    return text
  }
}

/** 查询澄清会话列表 */
function getList() {
  loading.value = true
  listClarifysession(queryParams.value).then(response => {
    sessionList.value = response.rows
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

/** 查看详情 */
function handleView(row) {
  getClarifysession(row.sessionId).then(response => {
    viewForm.value = response.data
    activeTab.value = 'conversation'
    viewOpen.value = true
  })
}

getList()
</script>

<style lang="scss" scoped>
.view-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #909399;
}
</style>
