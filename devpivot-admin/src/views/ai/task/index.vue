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
      <el-form-item label="任务类型(CLARIFY/PRD/TECH/DB_CHECK)" prop="taskType">
        <el-select v-model="queryParams.taskType" placeholder="请选择任务类型(CLARIFY/PRD/TECH/DB_CHECK)" clearable>
          <el-option
            v-for="dict in ai_task_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="任务状态(0运行中 1完成 2部分失败 3失败)" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择任务状态(0运行中 1完成 2部分失败 3失败)" clearable>
          <el-option
            v-for="dict in ai_task_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="预估token" prop="estTokens">
        <el-input
          v-model="queryParams.estTokens"
          placeholder="请输入预估token"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="实际消耗token" prop="totalTokens">
        <el-input
          v-model="queryParams.totalTokens"
          placeholder="请输入实际消耗token"
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
      <el-table-column label="任务ID" align="center" prop="taskId" />
      <el-table-column label="项目ID" align="center" prop="projectId" />
      <el-table-column label="任务类型(CLARIFY/PRD/TECH/DB_CHECK)" align="center" prop="taskType">
        <template #default="scope">
          <dict-tag :options="ai_task_type" :value="scope.row.taskType"/>
        </template>
      </el-table-column>
      <el-table-column label="参与模型列表(JSON)" align="center" prop="modelIds" />
      <el-table-column label="请求参数(JSON)" align="center" prop="requestParams" />
      <el-table-column label="融合汇总结果(JSON)" align="center" prop="resultSummary" />
      <el-table-column label="差异比对结果(JSON)" align="center" prop="compareResult" />
      <el-table-column label="任务状态(0运行中 1完成 2部分失败 3失败)" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="ai_task_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="预估token" align="center" prop="estTokens" />
      <el-table-column label="实际消耗token" align="center" prop="totalTokens" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
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
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="taskRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="项目ID" prop="projectId">
              <el-input v-model="form.projectId" placeholder="请输入项目ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="任务类型(CLARIFY/PRD/TECH/DB_CHECK)" prop="taskType">
              <el-select v-model="form.taskType" placeholder="请选择任务类型(CLARIFY/PRD/TECH/DB_CHECK)">
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
            <el-form-item label="请求参数(JSON)" prop="requestParams">
              <el-input v-model="form.requestParams" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="融合汇总结果(JSON)" prop="resultSummary">
              <el-input v-model="form.resultSummary" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="差异比对结果(JSON)" prop="compareResult">
              <el-input v-model="form.compareResult" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="任务状态(0运行中 1完成 2部分失败 3失败)" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in ai_task_status"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="预估token" prop="estTokens">
              <el-input v-model="form.estTokens" placeholder="请输入预估token" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
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
  </div>
</template>

<script setup name="Task">
import { listTask, getTask, delTask, addTask, updateTask } from "@/api/ai/task"

const { proxy } = getCurrentInstance()
const { ai_task_status, ai_task_type } = useDict('ai_task_status', 'ai_task_type')

const taskList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    projectId: undefined,
    taskType: undefined,
    modelIds: undefined,
    requestParams: undefined,
    resultSummary: undefined,
    compareResult: undefined,
    status: undefined,
    estTokens: undefined,
    totalTokens: undefined,
  },
  rules: {
    projectId: [
      { required: true, message: "项目ID不能为空", trigger: "blur" }
    ],
    taskType: [
      { required: true, message: "任务类型(CLARIFY/PRD/TECH/DB_CHECK)不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询多模型并行任务列表 */
function getList() {
  loading.value = true
  listTask(queryParams.value).then(response => {
    taskList.value = response.rows
    total.value = response.total
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
