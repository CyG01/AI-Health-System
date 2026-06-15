<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NInput,
  NSelect,
  NPagination,
  NSpace,
  NModal,
  NDescriptions,
  NDescriptionsItem,
  NTag
} from 'naive-ui';
import type { DataTableColumns, PaginationProps, SelectOption } from 'naive-ui';
import { fetchGetAuditLogs } from '@/service/api';

defineOptions({ name: 'AdminAuditLog' });

type AuditLogItem = Api.Admin.AuditLog;

interface AuditQuery {
  operatorName: string;
  action: string | null;
}

const actionOptions: SelectOption[] = [
  { label: '全部操作', value: '' },
  { label: '登录', value: 'LOGIN' },
  { label: '登出', value: 'LOGOUT' },
  { label: '创建', value: 'CREATE' },
  { label: '更新', value: 'UPDATE' },
  { label: '删除', value: 'DELETE' },
  { label: '导出', value: 'EXPORT' },
  { label: '封禁', value: 'BAN' },
  { label: '审批', value: 'APPROVE' }
];

const query = reactive<AuditQuery>({
  operatorName: '',
  action: null
});

// Table columns
const columns: DataTableColumns<AuditLogItem> = [
  { title: 'ID', key: 'id', width: 70 },
  {
    title: '操作人',
    key: 'operatorName',
    width: 120,
    ellipsis: { tooltip: true }
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    render(row) {
      return h(NTag, { size: 'small', bordered: false }, { default: () => row.action });
    }
  },
  {
    title: '对象类型',
    key: 'targetType',
    width: 120,
    ellipsis: { tooltip: true }
  },
  {
    title: '对象ID',
    key: 'targetId',
    width: 90
  },
  {
    title: '详情',
    key: 'detail',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render(row) {
      return row.detail || '-';
    }
  },
  { title: 'IP', key: 'ip', width: 140 },
  {
    title: '时间',
    key: 'createTime',
    width: 170,
    render(row) {
      return row.createTime;
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 80,
    fixed: 'right',
    render(row) {
      return h(
        NButton,
        { size: 'small', text: true, type: 'primary', onClick: () => handleViewDetail(row) },
        { default: () => '详情' }
      );
    }
  }
];

// Data
const loading = ref(false);
const items = ref<AuditLogItem[]>([]);
const pagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 15,
  pageCount: 1,
  showSizePicker: true,
  pageSizes: [15, 30, 50, 100],
  onUpdatePageSize: (size: number) => {
    pagination.pageSize = size;
    pagination.page = 1;
    fetchData();
  }
});

// Detail modal
const showDetailModal = ref(false);
const detailRow = ref<AuditLogItem | null>(null);

async function fetchData() {
  loading.value = true;
  try {
    const params: Api.Admin.AuditLogParams = {
      page: pagination.page,
      size: pagination.pageSize
    };
    if (query.operatorName) params.operatorName = query.operatorName;
    if (query.action) params.action = query.action;

    const res = await fetchGetAuditLogs(params);
    if (res.data) {
      items.value = res.data.records || [];
      const total: number = res.data.total || 0;
      pagination.pageCount = Math.max(1, Math.ceil(total / pagination.pageSize!));
    }
  } finally {
    loading.value = false;
  }
}

function handleViewDetail(row: AuditLogItem) {
  detailRow.value = row;
  showDetailModal.value = true;
}

function handleSearch() {
  pagination.page = 1;
  fetchData();
}

function handleReset() {
  query.operatorName = '';
  query.action = null;
  handleSearch();
}

function handlePageChange(page: number) {
  pagination.page = page;
  fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="flex-col-stretch gap-16px overflow-auto p-16px">
    <NCard title="操作审计日志">
      <template #header-extra>
        <NSpace align="center">
          <NInput
            v-model:value="query.operatorName"
            placeholder="搜索操作人"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
          <NSelect
            v-model:value="query.action"
            :options="actionOptions"
            placeholder="全部操作"
            clearable
            style="width: 130px"
            @update:value="handleSearch"
          />
          <NButton type="primary" @click="handleSearch">搜索</NButton>
          <NButton @click="handleReset">重置</NButton>
        </NSpace>
      </template>
      <NDataTable
        :columns="columns"
        :data="items"
        :loading="loading"
        :row-key="(row: Api.Admin.AuditLog) => row.id"
        :scroll-x="1200"
      />
      <div class="flex justify-end mt-16px">
        <NPagination
          v-model:page="pagination.page"
          :page-count="pagination.pageCount"
          :page-sizes="pagination.pageSizes"
          show-size-picker
          @update:page="handlePageChange"
        />
      </div>
    </NCard>

    <!-- Detail Modal -->
    <NModal
      v-model:show="showDetailModal"
      preset="card"
      title="审计日志详情"
      style="width: 560px"
    >
      <NDescriptions v-if="detailRow" label-placement="left" bordered :column="1">
        <NDescriptionsItem label="日志ID">{{ detailRow.id }}</NDescriptionsItem>
        <NDescriptionsItem label="操作人">{{ detailRow.operatorName }} (ID: {{ detailRow.operatorId }})</NDescriptionsItem>
        <NDescriptionsItem label="操作类型">
          <NTag size="small">{{ detailRow.action }}</NTag>
        </NDescriptionsItem>
        <NDescriptionsItem label="对象类型">{{ detailRow.targetType }}</NDescriptionsItem>
        <NDescriptionsItem label="对象ID">{{ detailRow.targetId }}</NDescriptionsItem>
        <NDescriptionsItem label="详情">
          <pre style="white-space: pre-wrap; margin: 0">{{ detailRow.detail || '无' }}</pre>
        </NDescriptionsItem>
        <NDescriptionsItem label="IP 地址">{{ detailRow.ip }}</NDescriptionsItem>
        <NDescriptionsItem label="操作时间">{{ detailRow.createTime }}</NDescriptionsItem>
      </NDescriptions>
    </NModal>
  </div>
</template>
