<template>
  <div class="admin-audit-page">
    <div class="page-header"><h2>操作审计日志</h2></div>
    <el-table :data="items" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="action" label="操作" width="150" />
      <el-table-column prop="target" label="操作对象" min-width="150" show-overflow-tooltip />
      <el-table-column label="详情" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.detail || '-' }}</template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column label="时间" width="160">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
    </el-table>
    <div class="pagination-wrap" v-if="total > 10">
      <el-pagination
        v-model:current-page="page"
        :total="total"
        :page-size="10"
        layout="total, prev, pager, next"
        @current-change="loadItems"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAuditLogs } from '@/api/admin'

const items = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)

async function loadItems() {
  loading.value = true
  try {
    const res = await getAuditLogs({ page: page.value, size: 10 })
    if (res.data) {
      items.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadItems)
</script>

<style scoped>
.admin-audit-page { padding: 20px 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>