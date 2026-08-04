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
      <el-form-item label="产物类型(REQ需求/PRD需求文档/PROTO原型/TECH技术文档/DB数据库)" prop="bizType">
        <el-select v-model="queryParams.bizType" placeholder="请选择产物类型(REQ需求/PRD需求文档/PROTO原型/TECH技术文档/DB数据库)" clearable>
          <el-option
            v-for="dict in ai_biz_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="产物ID" prop="bizId">
        <el-input
          v-model="queryParams.bizId"
          placeholder="请输入产物ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="版本号(V1.0)" prop="versionNo">
        <el-input
          v-model="queryParams.versionNo"
          placeholder="请输入版本号(V1.0)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生成所用模型" prop="sourceModel">
        <el-input
          v-model="queryParams.sourceModel"
          placeholder="请输入生成所用模型"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="版本状态(0草稿 1正式版本)" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择版本状态(0草稿 1正式版本)" clearable>
          <el-option
            v-for="dict in ai_doc_status"
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
          v-hasPermi="['system:record:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:record:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:record:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:record:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="recordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="记录ID" align="center" prop="recordId" />
      <el-table-column label="项目ID" align="center" prop="projectId" />
      <el-table-column label="产物类型(REQ需求/PRD需求文档/PROTO原型/TECH技术文档/DB数据库)" align="center" prop="bizType">
        <template #default="scope">
          <dict-tag :options="ai_biz_type" :value="scope.row.bizType"/>
        </template>
      </el-table-column>
      <el-table-column label="产物ID" align="center" prop="bizId" />
      <el-table-column label="版本号(V1.0)" align="center" prop="versionNo" />
      <el-table-column label="确认时完整内容快照(JSON,用于回退)" align="center" prop="contentSnapshot" />
      <el-table-column label="修改备注" align="center" prop="changeRemark" />
      <el-table-column label="生成所用模型" align="center" prop="sourceModel" />
      <el-table-column label="生成参数(JSON)" align="center" prop="modelParams" />
      <el-table-column label="版本状态(0草稿 1正式版本)" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="ai_doc_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:record:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:record:remove']">删除</el-button>
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

    <!-- 添加或修改版本全链路记录对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="recordRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="项目ID" prop="projectId">
              <el-input v-model="form.projectId" placeholder="请输入项目ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="产物类型(REQ需求/PRD需求文档/PROTO原型/TECH技术文档/DB数据库)" prop="bizType">
              <el-select v-model="form.bizType" placeholder="请选择产物类型(REQ需求/PRD需求文档/PROTO原型/TECH技术文档/DB数据库)">
                <el-option
                  v-for="dict in ai_biz_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="产物ID" prop="bizId">
              <el-input v-model="form.bizId" placeholder="请输入产物ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="版本号(V1.0)" prop="versionNo">
              <el-input v-model="form.versionNo" placeholder="请输入版本号(V1.0)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="确认时完整内容快照(JSON,用于回退)" prop="contentSnapshot">
              <el-input v-model="form.contentSnapshot" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="修改备注" prop="changeRemark">
              <el-input v-model="form.changeRemark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="生成所用模型" prop="sourceModel">
              <el-input v-model="form.sourceModel" placeholder="请输入生成所用模型" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="版本状态(0草稿 1正式版本)" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in ai_doc_status"
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

<script setup name="Record">
import { listRecord, getRecord, delRecord, addRecord, updateRecord } from "@/api/ai/record"

const { proxy } = getCurrentInstance()
const { ai_biz_type, ai_doc_status } = useDict('ai_biz_type', 'ai_doc_status')

const recordList = ref([])
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
    bizType: undefined,
    bizId: undefined,
    versionNo: undefined,
    contentSnapshot: undefined,
    changeRemark: undefined,
    sourceModel: undefined,
    modelParams: undefined,
    status: undefined,
  },
  rules: {
    projectId: [
      { required: true, message: "项目ID不能为空", trigger: "blur" }
    ],
    bizType: [
      { required: true, message: "产物类型(REQ需求/PRD需求文档/PROTO原型/TECH技术文档/DB数据库)不能为空", trigger: "change" }
    ],
    bizId: [
      { required: true, message: "产物ID不能为空", trigger: "blur" }
    ],
    versionNo: [
      { required: true, message: "版本号(V1.0)不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询版本全链路记录列表 */
function getList() {
  loading.value = true
  listRecord(queryParams.value).then(response => {
    recordList.value = response.rows
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
    bizType: null,
    bizId: null,
    versionNo: null,
    contentSnapshot: null,
    changeRemark: null,
    sourceModel: null,
    modelParams: null,
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("recordRef")
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
  title.value = "添加版本全链路记录"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _recordId = row.recordId || ids.value
  getRecord(_recordId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改版本全链路记录"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["recordRef"].validate(valid => {
    if (valid) {
      if (form.value.recordId != null) {
        updateRecord(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addRecord(form.value).then(() => {
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
  proxy.$modal.confirm('是否确认删除版本全链路记录编号为"' + _recordIds + '"的数据项？').then(function() {
    return delRecord(_recordIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/record/export', {
    ...queryParams.value
  }, `record_${new Date().getTime()}.xlsx`)
}

getList()
</script>
