<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NInput,
  NSelect,
  NTag,
  NPagination,
  NSpace,
  NModal,
  NDescriptions,
  NDescriptionsItem,
  NAvatar,
  NSpin,
  NDivider,
  NEmpty,
  useMessage,
  useDialog
} from 'naive-ui';
import type { DataTableColumns, PaginationProps, SelectOption } from 'naive-ui';
import {
  fetchGetUserList,
  fetchBanUser,
  fetchUnbanUser,
  executeWithApproval,
  fetchGetUserDetail,
  fetchExportUserList
} from '@/service/api';
import { useAuthStore } from '@/store/modules/auth';

defineOptions({ name: 'AdminUserManage' });

const message = useMessage();
const dialog = useDialog();
const authStore = useAuthStore();

interface UserRow {
  id: number;
  username: string;
  phone: string;
  createdAt: string;
  status: number;
  role: string;
}

interface UserQuery {
  keyword: string;
  role: string | null;
}

const roleOptions: SelectOption[] = [
  { label: '全部角色', value: '' },
  { label: '普通用户', value: 'user' },
  { label: '管理员', value: 'admin' },
  { label: 'VIP', value: 'vip' }
];

const query = reactive<UserQuery>({
  keyword: '',
  role: null
});

// Table columns
const columns: DataTableColumns<UserRow> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '用户名', key: 'username', minWidth: 120 },
  { title: '手机号', key: 'phone', minWidth: 140 },
  { title: '注册时间', key: 'createdAt', minWidth: 180 },
  {
    title: '角色',
    key: 'role',
    width: 100,
    render(row) {
      const roleMap: Record<string, { label: string; type: 'default' | 'warning' | 'info' }> = {
        admin: { label: '管理员', type: 'warning' },
        vip: { label: 'VIP', type: 'info' },
        user: { label: '用户', type: 'default' }
      };
      const r = roleMap[row.role] || { label: row.role, type: 'default' as const };
      return h(NTag, { size: 'small', bordered: false, type: r.type }, { default: () => r.label });
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      return h(
        NTag,
        { type: row.status === 1 ? 'success' : 'error', size: 'small', bordered: false },
        { default: () => (row.status === 1 ? '正常' : '禁用') }
      );
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    fixed: 'right',
    render(row) {
      return h(NSpace, { size: 'small' }, {
        default: () => [
          h(
            NButton,
            {
              size: 'small',
              type: 'info',
              text: true,
              onClick: () => handleViewDetail(row)
            },
            { default: () => '查看详情' }
          ),
          row.role === 'admin'
            ? h('span', { style: 'font-size: 12px; color: #999' }, '管理员')
            : h(
                NButton,
                {
                  size: 'small',
                  type: row.status === 1 ? 'error' : 'success',
                  text: true,
                  onClick: () => handleToggle(row)
                },
                { default: () => (row.status === 1 ? '封禁' : '解禁') }
              )
        ]
      });
    }
  }
];

// Data
const loading = ref(false);
const tableData = ref<UserRow[]>([]);
const pagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 10,
  pageCount: 1,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onUpdatePageSize: (size: number) => {
    pagination.pageSize = size;
    pagination.page = 1;
    fetchData();
  }
});

async function fetchData() {
  loading.value = true;
  try {
    const res = await fetchGetUserList({
      page: pagination.page,
      size: pagination.pageSize,
      keyword: query.keyword || undefined,
      role: query.role || undefined
    });
    if (res.data) {
      tableData.value = res.data.records || [];
      const total: number = res.data.total || 0;
      pagination.pageCount = Math.max(1, Math.ceil(total / (pagination.pageSize as number)));
    }
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.page = 1;
  fetchData();
}

function handleReset() {
  query.keyword = '';
  query.role = null;
  handleSearch();
}

function handlePageChange(page: number) {
  pagination.page = page;
  fetchData();
}

function handleToggle(row: UserRow) {
  const action = row.status === 1 ? '封禁' : '解禁';
  const actionType = row.status === 1 ? 'ban_user' : 'unban_user';
  const msg =
    row.status === 1
      ? `确定要封禁用户「${row.username}」吗？此操作需要审批。`
      : `确定要解禁用户「${row.username}」吗？此操作需要审批。`;

  dialog.warning({
    title: `审批确认 - ${action}`,
    content: msg,
    positiveText: '发起审批并执行',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await executeWithApproval(
          actionType,
          `${action}用户: ${row.username} (ID: ${row.id})`,
          (approvalId: string) =>
            row.status === 1
              ? fetchBanUser(row.id, approvalId)
              : fetchUnbanUser(row.id, approvalId),
          authStore.userInfo?.id
        );
        message.success(`${action}操作已执行`);
        fetchData();
      } catch {
        // error handled by interceptor
      }
    }
  });
}

// User detail modal state
const showDetailModal = ref(false);
const detailLoading = ref(false);
const userDetail = ref<Api.Admin.UserDetail | null>(null);

async function handleViewDetail(row: UserRow) {
  showDetailModal.value = true;
  detailLoading.value = true;
  userDetail.value = null;
  try {
    const { data } = await fetchGetUserDetail(row.id);
    userDetail.value = data || null;
  } catch {
    message.error('获取用户详情失败');
  } finally {
    detailLoading.value = false;
  }
}

const exporting = ref(false);

async function handleExportUsers() {
  exporting.value = true;
  try {
    const { data } = await fetchExportUserList({
      keyword: query.keyword || undefined
    });
    if (data && Array.isArray(data)) {
      const header = ['ID', '用户名', '邮箱', '手机号', '角色', '状态', '注册时间', '最后登录'];
      const rows = data.map((u: any) => [
        u.id, u.username, u.email || '', u.phone || '', u.role, u.status === 1 ? '正常' : '禁用', u.createdAt || u.createTime || '', u.lastLoginAt || ''
      ]);
      const csvContent = [header.join(','), ...rows.map(r => r.map((c: any) => `"${String(c).replace(/"/g, '""')}"`).join(','))].join('\n');
      const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `users_${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      message.success('导出成功');
    } else {
      message.warning('没有可导出的数据');
    }
  } catch {
    message.error('导出失败');
  } finally {
    exporting.value = false;
  }
}

onMounted(() => {
  fetchData();
});
</script>

<template>
  <div class="flex-col-stretch gap-16px overflow-auto p-16px">
    <NCard title="用户管理">
      <template #header-extra>
        <NSpace align="center">
          <NInput
            v-model:value="query.keyword"
            placeholder="搜索用户名 / 手机号"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
          <NSelect
            v-model:value="query.role"
            :options="roleOptions"
            placeholder="全部角色"
            clearable
            style="width: 140px"
            @update:value="handleSearch"
          />
          <NButton type="primary" @click="handleSearch">搜索</NButton>
          <NButton @click="handleReset">重置</NButton>
          <NButton type="success" :loading="exporting" @click="handleExportUsers">导出用户列表</NButton>
        </NSpace>
      </template>
      <NDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row: UserRow) => row.id"
        :scroll-x="900"
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

    <!-- User Detail Modal -->
    <NModal
      v-model:show="showDetailModal"
      preset="card"
      title="用户详情"
      style="max-width: 640px;"
    >
      <NSpin :show="detailLoading">
        <div v-if="userDetail">
          <div class="flex items-center gap-4 mb-4">
            <NAvatar
              :src="userDetail.avatar || undefined"
              :size="64"
              round
            >
              {{ userDetail.username?.charAt(0)?.toUpperCase() }}
            </NAvatar>
            <div>
              <h3 class="text-lg font-semibold m-0">{{ userDetail.username }}</h3>
              <NTag :type="String(userDetail.status) === '1' ? 'success' : 'error'" size="small" class="mt-1">
                {{ String(userDetail.status) === '1' ? '正常' : '禁用' }}
              </NTag>
            </div>
          </div>
          <NDescriptions label-placement="left" bordered :column="2" size="small">
            <NDescriptionsItem label="用户ID">{{ userDetail.userId }}</NDescriptionsItem>
            <NDescriptionsItem label="角色">
              <NTag size="small" :type="userDetail.role === 'admin' ? 'warning' : userDetail.role === 'vip' ? 'info' : 'default'">
                {{ userDetail.role }}
              </NTag>
            </NDescriptionsItem>
            <NDescriptionsItem label="邮箱">{{ userDetail.email || '-' }}</NDescriptionsItem>
            <NDescriptionsItem label="手机号">{{ userDetail.phone || '-' }}</NDescriptionsItem>
            <NDescriptionsItem label="注册时间">{{ userDetail.createdAt || userDetail.createTime || '-' }}</NDescriptionsItem>
            <NDescriptionsItem label="最后登录">{{ userDetail.lastLoginAt || '-' }}</NDescriptionsItem>
          </NDescriptions>
          <template v-if="userDetail.planStats || userDetail.checkinStats || userDetail.exerciseStats || userDetail.dietStats">
            <NDivider style="margin: 16px 0 12px" />
            <h4 class="text-sm font-semibold mb-2">统计概览</h4>
            <NDescriptions label-placement="left" bordered :column="2" size="small">
              <NDescriptionsItem v-if="userDetail.planStats" label="计划统计">
                <pre class="text-xs m-0 whitespace-pre-wrap">{{ JSON.stringify(userDetail.planStats, null, 2) }}</pre>
              </NDescriptionsItem>
              <NDescriptionsItem v-if="userDetail.checkinStats" label="打卡统计">
                <pre class="text-xs m-0 whitespace-pre-wrap">{{ JSON.stringify(userDetail.checkinStats, null, 2) }}</pre>
              </NDescriptionsItem>
              <NDescriptionsItem v-if="userDetail.exerciseStats" label="运动统计">
                <pre class="text-xs m-0 whitespace-pre-wrap">{{ JSON.stringify(userDetail.exerciseStats, null, 2) }}</pre>
              </NDescriptionsItem>
              <NDescriptionsItem v-if="userDetail.dietStats" label="饮食统计">
                <pre class="text-xs m-0 whitespace-pre-wrap">{{ JSON.stringify(userDetail.dietStats, null, 2) }}</pre>
              </NDescriptionsItem>
            </NDescriptions>
          </template>
        </div>
        <NEmpty v-else-if="!detailLoading" description="无法加载用户详情" />
      </NSpin>
    </NModal>
  </div>
</template>
