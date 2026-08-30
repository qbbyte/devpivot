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
      <el-form-item label="任务类型" prop="taskType">
        <el-select v-model="queryParams.taskType" placeholder="请选择任务类型" clearable style="width: 160px">
          <el-option
            v-for="dict in ai_task_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="任务状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择任务状态" clearable style="width: 160px">
          <el-option
            v-for="dict in ai_task_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['system:task:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:task:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:task:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:task:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="taskList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务ID" align="center" prop="taskId" width="80" />
      <el-table-column label="项目ID" align="center" prop="projectId" width="80" />
      <el-table-column label="任务类型" align="center" prop="taskType" width="110">
        <template #default="scope">
          <dict-tag :options="ai_task_type" :value="scope.row.taskType"/>
        </template>
      </el-table-column>
      <el-table-column label="参与模型" align="center" prop="modelIds" width="140" show-overflow-tooltip>
        <template #default="scope">{{ modelIdsText(scope.row.modelIds) }}</template>
      </el-table-column>
      <el-table-column label="任务状态" align="center" prop="status" width="90">
        <template #default="scope">
          <dict-tag :options="ai_task_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="预估token" align="center" prop="estTokens" width="100" />
      <el-table-column label="实际token" align="center" prop="totalTokens" width="100" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['system:task:query']">查看</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:task:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:task:remove']">删除</el-button>
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

    <!-- 添加或修改多模型并行任务对话框 -->
    <el-dialog :title="title" v-model="open" width="780px" append-to-body>
      <el-form ref="taskRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="项目ID" prop="projectId">
              <el-input v-model="form.projectId" placeholder="请输入项目ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务类型" prop="taskType">
              <el-select v-model="form.taskType" placeholder="请选择任务类型" style="width: 100%">
                <el-option
                  v-for="dict in ai_task_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="任务状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in ai_task_status"
                  :key="dict.value"
                  :value="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预估token" prop="estTokens">
              <el-input v-model="form.estTokens" placeholder="请输入预估token" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际消耗token" prop="totalTokens">
              <el-input v-model="form.totalTokens" placeholder="请输入实际消耗token" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看任务详情对话框 -->
    <el-dialog title="多模型任务详情" v-model="viewOpen" width="860px" append-to-body>
      <div class="view-meta">
        <span>任务ID：{{ viewForm.taskId }}</span>
        <span>项目ID：{{ viewForm.projectId }}</span>
        <span>预估token：{{ viewForm.estTokens }}</span>
        <span>实际token：{{ viewForm.totalTokens }}</span>
        <span>创建时间：{{ viewForm.createTime }}</span>
      </div>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="参与模型" name="models">
          <el-input v-model="prettyModels" type="textarea" :rows="16" readonly placeholder="暂无模型列表" />
        </el-tab-pane>
        <el-tab-pane label="请求参数" name="params">
          <el-input v-model="prettyParams" type="textarea" :rows="16" readonly placeholder="暂无请求参数" />
        </el-tab-pane>
        <el-tab-pane label="融合汇总结果" name="summary">
          <el-input v-model="prettySummary" type="textarea" :rows="16" readonly placeholder="暂无融合汇总结果" />
        </el-tab-pane>
        <el-tab-pane label="差异比对结果" name="compare">
          <el-input v-model="prettyCompare" type="textarea" :rows="16" readonly placeholder="暂无差异比对结果" />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup name="Task">
import { listTask, getTask, delTask, addTask, updateTask } from "@/api/ai/task"

const { proxy } = getCurrentInstance()
const { ai_task_status, ai_task_type } = useDict('ai_task_status', 'ai_task_type')

const taskList = ref([])
const open = ref(false)
const viewOpen = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const viewForm = ref({})
const activeTab = ref("models")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    taskType: undefined,
    status: undefined,
  },
  rules: {
    projectId: [
      { required: true, message: "项目ID不能为空", trigger: "blur" }
    ],
    taskType: [
      { required: true, message: "任务类型不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** JSON 美化展示（非 JSON 原样返回） */
function prettyJson(text) {
  if (!text) return ''
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch (e) {
    return text
  }
}

/** 参与模型列表简短展示（如 ["gpt4","claude"] → gpt4, claude） */
function modelIdsText(text) {
  if (!text) return '-'
  try {
    const arr = JSON.parse(text)
    return Array.isArray(arr) ? arr.join(', ') : text
  } catch (e) {
    return text
  }
}

const prettyModels = computed(() => prettyJson(viewForm.value.modelIds))
const prettyParams = computed(() => prettyJson(viewForm.value.requestParams))
const prettySummary = computed(() => prettyJson(viewForm.value.resultSummary))
const prettyCompare = computed(() => prettyJson(viewForm.value.compareResult))

/** 查询多模型并行任务列表 */
function getList() {
  loading.value = true
  listTask(queryParams.value).then(response => {
    taskList.value = response.rows
    total.value = response.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    taskId: null,
    projectId: null,
    taskType: null,
    modelIds: null,
    requestParams: null,
    resultSummary: null,
    compareResult: null,
    status: null,
    estTokens: null,
    totalTokens: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("taskRef")
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

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.taskId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加多模型并行任务"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _taskId = row.taskId || ids.value
  getTask(_taskId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改多模型并行任务"
  })
}

/** 查看详情按钮操作 */
function handleView(row) {
  getTask(row.taskId).then(response => {
    viewForm.value = response.data
    activeTab.value = "models"
    viewOpen.value = true
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["taskRef"].validate(valid => {
    if (valid) {
      if (form.value.taskId != null) {
        updateTask(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addTask(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _taskIds = row.taskId || ids.value
  proxy.$modal.confirm('是否确认删除多模型并行任务编号为"' + _taskIds + '"的数据项？').then(function() {
    return delTask(_taskIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/task/export', {
    ...queryParams.value
  }, `task_${new Date().getTime()}.xlsx`)
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
