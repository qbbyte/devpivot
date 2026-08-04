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
      <el-form-item label="文档标题" prop="docName">
        <el-input
          v-model="queryParams.docName"
          placeholder="请输入文档标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="技术栈倾向(JAVA/PYTHON)" prop="techStack">
        <el-select v-model="queryParams.techStack" placeholder="请选择技术栈倾向(JAVA/PYTHON)" clearable>
          <el-option
            v-for="dict in ai_tech_stack"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态(0草稿 1已确认)" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态(0草稿 1已确认)" clearable>
          <el-option
            v-for="dict in ai_doc_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="生成模型" prop="sourceModel">
        <el-input
          v-model="queryParams.sourceModel"
          placeholder="请输入生成模型"
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
          v-hasPermi="['system:techdoc:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:techdoc:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:techdoc:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:techdoc:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="techdocList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文档ID" align="center" prop="docId" />
      <el-table-column label="项目ID" align="center" prop="projectId" />
      <el-table-column label="文档标题" align="center" prop="docName" />
      <el-table-column label="技术栈倾向(JAVA/PYTHON)" align="center" prop="techStack">
        <template #default="scope">
          <dict-tag :options="ai_tech_stack" :value="scope.row.techStack"/>
        </template>
      </el-table-column>
      <el-table-column label="文档内容(Markdown)" align="center" prop="content" />
      <el-table-column label="多模型对比差异结果(JSON)" align="center" prop="diffResult" />
      <el-table-column label="各模型生成结果及融合来源(JSON)" align="center" prop="multiSource" />
      <el-table-column label="状态(0草稿 1已确认)" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="ai_doc_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="生成模型" align="center" prop="sourceModel" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:techdoc:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:techdoc:remove']">删除</el-button>
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

    <!-- 添加或修改技术方案文档对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="techdocRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="项目ID" prop="projectId">
              <el-input v-model="form.projectId" placeholder="请输入项目ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="文档标题" prop="docName">
              <el-input v-model="form.docName" placeholder="请输入文档标题" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="技术栈倾向(JAVA/PYTHON)" prop="techStack">
              <el-select v-model="form.techStack" placeholder="请选择技术栈倾向(JAVA/PYTHON)">
                <el-option
                  v-for="dict in ai_tech_stack"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="文档内容(Markdown)">
              <editor v-model="form.content" :min-height="192"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="多模型对比差异结果(JSON)" prop="diffResult">
              <el-input v-model="form.diffResult" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="各模型生成结果及融合来源(JSON)" prop="multiSource">
              <el-input v-model="form.multiSource" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态(0草稿 1已确认)" prop="status">
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
            <el-form-item label="生成模型" prop="sourceModel">
              <el-input v-model="form.sourceModel" placeholder="请输入生成模型" />
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

<script setup name="Techdoc">
import { listTechdoc, getTechdoc, delTechdoc, addTechdoc, updateTechdoc } from "@/api/ai/techdoc"

const { proxy } = getCurrentInstance()
const { ai_tech_stack, ai_doc_status } = useDict('ai_tech_stack', 'ai_doc_status')

const techdocList = ref([])
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
    docName: undefined,
    techStack: undefined,
    content: undefined,
    diffResult: undefined,
    multiSource: undefined,
    status: undefined,
    sourceModel: undefined,
  },
  rules: {
    projectId: [
      { required: true, message: "项目ID不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询技术方案文档列表 */
function getList() {
  loading.value = true
  listTechdoc(queryParams.value).then(response => {
    techdocList.value = response.rows
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
    docId: null,
    projectId: null,
    docName: null,
    techStack: null,
    content: null,
    diffResult: null,
    multiSource: null,
    status: null,
    sourceModel: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("techdocRef")
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
  ids.value = selection.map(item => item.docId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加技术方案文档"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _docId = row.docId || ids.value
  getTechdoc(_docId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改技术方案文档"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["techdocRef"].validate(valid => {
    if (valid) {
      if (form.value.docId != null) {
        updateTechdoc(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addTechdoc(form.value).then(() => {
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
  const _docIds = row.docId || ids.value
  proxy.$modal.confirm('是否确认删除技术方案文档编号为"' + _docIds + '"的数据项？').then(function() {
    return delTechdoc(_docIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/techdoc/export', {
    ...queryParams.value
  }, `techdoc_${new Date().getTime()}.xlsx`)
}

getList()
</script>
