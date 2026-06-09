<template>
  <div class="admin-page">
    <div class="glass-card page-card">
      <div class="page-header">
        <h2 class="page-title">公告管理</h2>
        <el-button type="primary" @click="openCreateDialog">发布公告</el-button>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        class="glass-table"
        empty-text="暂无公告数据"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="dark">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" min-width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openEditDialog(row)">编辑</el-button>
            <el-button
              v-if="row.status !== 1"
              type="success"
              size="small"
              text
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑公告' : '发布公告'"
      width="560px"
      :close-on-click-modal="false"
      class="glass-dialog"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAnnouncementList, createAnnouncement, updateAnnouncement, deleteAnnouncement, publishAnnouncement } from '@/api/admin'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({
  title: '',
  content: ''
})
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAnnouncementList({
      page: page.value,
      size: size.value
    })
    if (res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.title = ''
  form.content = ''
  editingId.value = null
}

function openCreateDialog() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editingId.value = row.id
  form.title = row.title
  form.content = row.content
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await updateAnnouncement({
          id: editingId.value,
          title: form.title,
          content: form.content,
          status: undefined
        })
        ElMessage.success('公告已更新')
      } else {
        await createAnnouncement({
          title: form.title,
          content: form.content
        })
        ElMessage.success('公告已创建')
      }
      dialogVisible.value = false
      resetForm()
      fetchData()
    } finally {
      submitting.value = false
    }
  })
}

function handlePublish(row) {
  ElMessageBox.confirm(
    `确定要发布公告「${row.title}」吗？发布后将向所有用户推送系统通知。`,
    '确认发布',
    { confirmButtonText: '发布', cancelButtonText: '取消', type: 'info' }
  ).then(async () => {
    try {
      await publishAnnouncement(row.id)
      ElMessage.success('公告已发布')
      fetchData()
    } catch {
      // handled by interceptor
    }
  }).catch(() => {})
}

function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除公告「${row.title}」吗？此操作不可恢复。`,
    '确认删除',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await deleteAnnouncement(row.id)
      ElMessage.success('公告已删除')
      fetchData()
    } catch {
      // handled by interceptor
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.admin-page {
  height: 100%;
}

.page-card {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.glass-table {
  flex: 1;
  background: transparent !important;

  :deep(.el-table__header-wrapper) {
    background: transparent;
  }

  :deep(th.el-table__cell) {
    background: #161b22 !important;
    color: #8b949e;
    border-bottom-color: #30363d;
    font-weight: 500;
  }

  :deep(tr) {
    background: transparent !important;
  }

  :deep(td.el-table__cell) {
    border-bottom-color: rgba(48, 54, 61, 0.6);
    color: #c9d1d9;
  }

  :deep(.el-table__body tr:hover > td) {
    background: #21262d !important;
  }

  :deep(.el-table__empty-text) {
    color: #484f58;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
