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
      <el-form-item label="类别(ROLE角色权限/BUSINESS业务流程/DATA数据规则/BOUNDARY边界场景)" prop="category">
        <el-input
          v-model="queryParams.category"
          placeholder="请输入类别(ROLE角色权限/BUSINESS业务流程/DATA数据规则/BOUNDARY边界场景)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态(0待回答 1已回答 2已跳过)" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态(0待回答 1已回答 2已跳过)" clearable>
          <el-option
            v-for="dict in ai_clarify_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="提出该问题的模型" prop="sourceModel">
        <el-input
          v-model="queryParams.sourceModel"
          placeholder="请输入提出该问题的模型"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="多模型对比标记(0共识 1独有 2观点差异)" prop="highlightType">
        <el-select v-model="queryParams.highlightType" placeholder="请选择多模型对比标记(0共识 1独有 2观点差异)" clearable>
          <el-option
            v-for="dict in ai_highlight_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否勾选合并进最终清单(Y/N)" prop="isMerged">
        <el-select v-model="queryParams.isMerged" placeholder="请选择是否勾选合并进最终清单(Y/N)" clearable>
          <el-option
            v-for="dict in sys_yes_no"
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
          v-hasPermi="['system:clarify:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:clarify:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:clarify:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:clarify:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="clarifyList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="记录ID" align="center" prop="recordId" />
      <el-table-column label="项目ID" align="center" prop="projectId" />
      <el-table-column label="类别(ROLE角色权限/BUSINESS业务流程/DATA数据规则/BOUNDARY边界场景)" align="center" prop="category" />
      <el-table-column label="问题内容" align="center" prop="question" />
      <el-table-column label="用户回答" align="center" prop="answer" />
      <el-table-column label="状态(0待回答 1已回答 2已跳过)" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="ai_clarify_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="提出该问题的模型" align="center" prop="sourceModel" />
      <el-table-column label="多模型对比标记(0共识 1独有 2观点差异)" align="center" prop="highlightType">
        <template #default="scope">
          <dict-tag :options="ai_highlight_type" :value="scope.row.highlightType"/>
        </template>
      </el-table-column>
      <el-table-column label="语义一致命中该问题的模型列表" align="center" prop="modelList" />
      <el-table-column label="是否勾选合并进最终清单(Y/N)" align="center" prop="isMerged">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.isMerged"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:clarify:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:clarify:remove']">删除</el-button>
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

    <!-- 添加或修改AI澄清问题记录对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="clarifyRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="项目ID" prop="projectId">
              <el-input v-model="form.projectId" placeholder="请输入项目ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类别(ROLE角色权限/BUSINESS业务流程/DATA数据规则/BOUNDARY边界场景)" prop="category">
              <el-input v-model="form.category" placeholder="请输入类别(ROLE角色权限/BUSINESS业务流程/DATA数据规则/BOUNDARY边界场景)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="问题内容" prop="question">
              <el-input v-model="form.question" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="用户回答" prop="answer">
              <el-input v-model="form.answer" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态(0待回答 1已回答 2已跳过)" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in ai_clarify_status"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="提出该问题的模型" prop="sourceModel">
              <el-input v-model="form.sourceModel" placeholder="请输入提出该问题的模型" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="多模型对比标记(0共识 1独有 2观点差异)" prop="highlightType">
              <el-select v-model="form.highlightType" placeholder="请选择多模型对比标记(0共识 1独有 2观点差异)">
                <el-option
                  v-for="dict in ai_highlight_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否勾选合并进最终清单(Y/N)" prop="isMerged">
              <el-radio-group v-model="form.isMerged">
                <el-radio
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
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

<script setup name="Clarify">
import { listClarify, getClarify, delClarify, addClarify, updateClarify } from "@/api/ai/clarify"

const { proxy } = getCurrentInstance()
const { ai_clarify_status, sys_yes_no, ai_highlight_type } = useDict('ai_clarify_status', 'sys_yes_no', 'ai_highlight_type')

const clarifyList = ref([])
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
    category: undefined,
    question: undefined,
    answer: undefined,
    status: undefined,
    sourceModel: undefined,
    highlightType: undefined,
    modelList: undefined,
    isMerged: undefined,
  },
  rules: {
    projectId: [
      { required: true, message: "项目ID不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询AI澄清问题记录列表 */
function getList() {
  loading.value = true
  listClarify(queryParams.value).then(response => {
    clarifyList.value = response.rows
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
    recordId: null,
    projectId: null,
    category: null,
    question: null,
    answer: null,
    status: null,
    sourceModel: null,
    highlightType: null,
    modelList: null,
    isMerged: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("clarifyRef")
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
  ids.value = selection.map(item => item.recordId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加AI澄清问题记录"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _recordId = row.recordId || ids.value
  getClarify(_recordId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改AI澄清问题记录"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["clarifyRef"].validate(valid => {
    if (valid) {
      if (form.value.recordId != null) {
        updateClarify(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addClarify(form.value).then(() => {
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
  const _recordIds = row.recordId || ids.value
  proxy.$modal.confirm('是否确认删除AI澄清问题记录编号为"' + _recordIds + '"的数据项？').then(function() {
    return delClarify(_recordIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/clarify/export', {
    ...queryParams.value
  }, `clarify_${new Date().getTime()}.xlsx`)
}

getList()
</script>
