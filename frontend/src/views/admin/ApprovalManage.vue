<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NInput,
  NSelect,
  NTag,
  NSpace,
  NModal,
  NForm,
  NFormItem,
  NEmpty,
  useMessage,
  useDialog
} from 'naive-ui';
import type { DataTableColumns, FormInst, SelectOption } from 'naive-ui';
import {
  fetchGetPendingApprovals,
  fetchApproveRequest,
  fetchRejectRequest
} from '@/service/api';
import { useAuthStore } from '@/store/modules/auth';

defineOptions({ name: 'AdminApprovalManage' });

const message = useMessage();
const dialog = useDialog();
const authStore = useAuthStore();

type ApprovalItem = Api.Admin.Approval;

interface ApprovalQuery {
  keyword: string;
  status: string | null;
}

const statusOptions: SelectOption[] = [
  { label: '全部状态', value: '' },
  { label: '待审批', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已拒绝', value: 'rejected' }
];

const query = reactive<ApprovalQuery>({
  keyword: '',
  status: null
});

const filteredItems = computed(() => {
  return items.value.filter(item => {
    const matchKeyword = !query.keyword ||
      item.actionType.toLowerCase().includes(query.keyword.toLowerCase()) ||
      item.targetDescription.toLowerCase().includes(query.keyword.toLowerCase()) ||
      item.operatorName.toLowerCase().includes(query.keyword.toLowerCase());
    const matchStatus = !query.status || item.status === query.status;
    return matchKeyword && matchStatus;
  });
});

function actionTagType(action: string): 'error' | 'warning' | 'primary' | 'info' {
  const map: Record<string, 'error' | 'warning' | 'primary' | 'info'> = {
    DELETE_ACCOUNT: 'error',
    DATA_EXPORT: 'warning',
    REFUND: 'warning',
    SUBSCRIPTION_UPGRADE: 'primary'
  };
  return map[action] || 'info';
}

function statusLabel(s: string): string {
  const map: Record<string, string> = { pending: '待审批', approved: '已通过', rejected: '已拒绝' };
  return map[s] || s;
}

function statusTagType(s: string): 'warning' | 'success' | 'error' {
  if (s === 'pending') return 'warning';
  if (s === 'approved') return 'success';
  return 'error';
}

// Table columns
const columns: DataTableColumns<ApprovalItem> = [
  { title: 'ID', key: 'id', width: 70 },
  {
    title: '操作人',
    key: 'operatorName',
    width: 120,
    ellipsis: { tooltip: true }
  },
  {
    title: '操作类型',
    key: 'actionType',
    width: 160,
    render(row) {
      return h(
        NTag,
        { type: actionTagType(row.actionType), size: 'small', bordered: false },
        { default: () => row.actionType }
      );
    }
  },
  {
    title: '目标描述',
    key: 'targetDescription',
    minWidth: 220,
    ellipsis: { tooltip: true }
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      return h(
        NTag,
        { type: statusTagType(row.status), size: 'small', bordered: false },
        { default: () => statusLabel(row.status) }
      );
    }
  },
  {
    title: '申请时间',
    key: 'requestedAt',
    width: 170,
    render(row) {
      return row.requestedAt;
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render(row) {
      if (row.status === 'pending') {
        return h(NSpace, null, {
          default: () => [
            h(
              NButton,
              {
                type: 'success',
                size: 'small',
                loading: approvingId.value === row.id,
                onClick: () => handleApprove(row)
              },
              { default: () => '通过' }
            ),
            h(
              NButton,
              {
                type: 'error',
                size: 'small',
                loading: rejectingId.value === row.id,
                onClick: () => handleReject(row)
              },
              { default: () => '拒绝' }
            )
          ]
        });
      }
      return h('span', { style: 'color: #999; font-size: 13px' }, '已处理');
    }
  }
];

// Data
const loading = ref(false);
const items = ref<ApprovalItem[]>([]);
const approvingId = ref<number | null>(null);
const rejectingId = ref<number | null>(null);

// Reject modal with remarks
const showRejectModal = ref(false);
const rejectFormRef = ref<FormInst | null>(null);
const rejectTarget = ref<ApprovalItem | null>(null);
const rejectForm = reactive({
  reason: ''
});

async function loadItems() {
  loading.value = true;
  try {
    const res = await fetchGetPendingApprovals();
    items.value = res.data || [];
  } finally {
    loading.value = false;
  }
}

async function handleApprove(row: ApprovalItem) {
  approvingId.value = row.id;
  try {
    const approverName = authStore.userInfo?.username || 'admin';
    await fetchApproveRequest(row.id, { approverName });
    message.success('审批通过');
    loadItems();
  } catch {
    message.error('审批失败');
  } finally {
    approvingId.value = null;
  }
}

function handleReject(row: ApprovalItem) {
  rejectTarget.value = row;
  rejectForm.reason = '';
  showRejectModal.value = true;
}

async function handleRejectConfirm() {
  if (!rejectTarget.value) return;
  rejectingId.value = rejectTarget.value.id;
  showRejectModal.value = false;
  try {
    const approverName = authStore.userInfo?.username || 'admin';
    await fetchRejectRequest(rejectTarget.value.id, {
      approverName,
      reason: rejectForm.reason || '管理员拒绝'
    });
    message.success('已拒绝');
    loadItems();
  } catch {
    message.error('操作失败');
  } finally {
    rejectingId.value = null;
    rejectTarget.value = null;
  }
}

onMounted(loadItems);
</script>

<template>
  <div class="flex-col-stretch gap-16px overflow-auto p-16px">
    <NCard title="操作审批管理">
      <template #header-extra>
        <NSpace align="center">
          <NInput
            v-model:value="query.keyword"
            placeholder="搜索操作类型 / 目标描述 / 操作人"
            clearable
            style="width: 260px"
          />
          <NSelect
            v-model:value="query.status"
            :options="statusOptions"
            placeholder="全部状态"
            clearable
            style="width: 130px"
          />
        </NSpace>
      </template>
      <NDataTable
        :columns="columns"
        :data="filteredItems"
        :loading="loading"
        :row-key="(row: Api.Admin.Approval) => row.id"
        :scroll-x="1100"
      />
      <NEmpty v-if="!loading && filteredItems.length === 0" description="暂无审批记录" class="mt-32px" />
    </NCard>

    <!-- Reject Modal with Remarks -->
    <NModal
      v-model:show="showRejectModal"
      preset="dialog"
      title="拒绝审批"
      style="width: 480px"
      :show-icon="false"
      positive-text="确认拒绝"
      negative-text="取消"
      @positive-click="handleRejectConfirm"
    >
      <p v-if="rejectTarget" style="margin-bottom: 12px; color: #666">
        确认拒绝 {{ rejectTarget.operatorName }} 的「{{ rejectTarget.actionType }}」申请？
      </p>
      <NForm label-placement="top">
        <NFormItem label="拒绝理由">
          <NInput
            v-model:value="rejectForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝理由（选填）"
            :maxlength="500"
          />
        </NFormItem>
      </NForm>
    </NModal>
  </div>
</template>
