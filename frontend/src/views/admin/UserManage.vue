<template>
  <div class="admin-page">
    <div class="glass-card page-card">
      <div class="page-header">
        <h2 class="page-title">用户管理</h2>
        <div class="search-bar">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名 / 手机号"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </div>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        class="glass-table"
        empty-text="暂无用户数据"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="createTime" label="注册时间" min-width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.role !== 'admin'"
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              text
              @click="handleToggle(row)"
            >
              {{ row.status === 1 ? '封禁' : '解禁' }}
            </el-button>
            <span v-else class="admin-label">管理员</span>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, banUser, unbanUser } from '@/api/admin'

const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const tableData = ref([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserList({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    })
    if (res.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function handleToggle(row) {
  const action = row.status === 1 ? '封禁' : '解禁'
  const msg = row.status === 1
    ? `确定要封禁用户「${row.username}」吗？封禁后将无法登录使用系统。`
    : `确定要解禁用户「${row.username}」吗？解禁后账号将恢复正常使用。`

  ElMessageBox.confirm(msg, `确认${action}`, {
    confirmButtonText: action,
    cancelButtonText: '取消',
    type: row.status === 1 ? 'warning' : 'info'
  }).then(async () => {
    try {
      if (row.status === 1) {
        await banUser(row.id)
      } else {
        await unbanUser(row.id)
      }
      ElMessage.success(`${action}成功`)
      fetchData()
    } catch {
      // error handled by interceptor
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
  flex-wrap: wrap;
  gap: 16px;
}

.search-bar {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input {
  width: 260px;
}

.glass-table {
  flex: 1;
  background: transparent !important;

  :deep(.el-table__header-wrapper) {
    background: transparent;
  }

  :deep(.el-table__header) {
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

.admin-label {
  font-size: 12px;
  color: #484f58;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
