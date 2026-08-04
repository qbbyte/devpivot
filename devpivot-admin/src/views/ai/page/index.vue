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
      <el-form-item label="页面名称" prop="pageName">
        <el-input
          v-model="queryParams.pageName"
          placeholder="请输入页面名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="页面说明" prop="pageDesc">
        <el-input
          v-model="queryParams.pageDesc"
          placeholder="请输入页面说明"
          clearable
          @keyup.enter="handleQuery"
        />
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
      <el-form-item label="生成来源(人工/AI生成)" prop="sourceModel">
        <el-input
          v-model="queryParams.sourceModel"
          placeholder="请输入生成来源(人工/AI生成)"
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
          v-hasPermi="['system:page:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:page:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:page:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:page:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="pageList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="页面ID" align="center" prop="pageId" />
      <el-table-column label="项目ID" align="center" prop="projectId" />
      <el-table-column label="页面名称" align="center" prop="pageName" />
      <el-table-column label="页面说明" align="center" prop="pageDesc" />
      <el-table-column label="画布布局数据(JSON: 组件树/栅格/坐标)" align="center" prop="layout" />
      <el-table-column label="状态(0草稿 1已确认)" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="ai_doc_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="生成来源(人工/AI生成)" align="center" prop="sourceModel" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:page:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:page:remove']">删除</el-button>
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

    <!-- 添加或修改原型页面对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="pageRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="项目ID" prop="projectId">
              <el-input v-model="form.projectId" placeholder="请输入项目ID" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="页面名称" prop="pageName">
              <el-input v-model="form.pageName" placeholder="请输入页面名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="页面说明" prop="pageDesc">
              <el-input v-model="form.pageDesc" placeholder="请输入页面说明" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="画布布局数据(JSON: 组件树/栅格/坐标)" prop="layout">
              <el-input v-model="form.layout" type="textarea" placeholder="请输入内容" />
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
            <el-form-item label="生成来源(人工/AI生成)" prop="sourceModel">
              <el-input v-model="form.sourceModel" placeholder="请输入生成来源(人工/AI生成)" />
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

<script setup name="Page">
import { listPage, getPage, delPage, addPage, updatePage } from "@/api/ai/page"

const { proxy } = getCurrentInstance()
const { ai_doc_status } = useDict('ai_doc_status')

const pageList = ref([])
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
    pageName: undefined,
    pageDesc: undefined,
    layout: undefined,
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

/** 查询原型页面列表 */
function getList() {
  loading.value = true
  listPage(queryParams.value).then(response => {
    pageList.value = response.rows
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
    pageId: null,
    projectId: null,
    pageName: null,
    pageDesc: null,
    layout: null,
    status: null,
    sourceModel: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("pageRef")
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
  ids.value = selection.map(item => item.pageId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加原型页面"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _pageId = row.pageId || ids.value
  getPage(_pageId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改原型页面"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["pageRef"].validate(valid => {
    if (valid) {
      if (form.value.pageId != null) {
        updatePage(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addPage(form.value).then(() => {
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
  const _pageIds = row.pageId || ids.value
  proxy.$modal.confirm('是否确认删除原型页面编号为"' + _pageIds + '"的数据项？').then(function() {
    return delPage(_pageIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/page/export', {
    ...queryParams.value
  }, `page_${new Date().getTime()}.xlsx`)
}

getList()
</script>
