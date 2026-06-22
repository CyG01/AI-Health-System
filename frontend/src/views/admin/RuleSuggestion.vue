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
  NTooltip,
  NEmpty,
  useMessage
} from 'naive-ui';
import type { DataTableColumns, SelectOption } from 'naive-ui';
import {
  fetchGetPendingRuleSuggestions,
  fetchApproveRuleSuggestion,
  fetchRejectRuleSuggestion
} from '@/service/api';
import { useAuthStore } from '@/store/modules/auth';

defineOptions({ name: 'AdminRuleSuggestion' });

const message = useMessage();
const authStore = useAuthStore();

type RuleSuggestionItem = Api.Admin.RuleSuggestion;

interface SuggestionQuery {
  keyword: string;
  status: string | null;
  suggestionType: string | null;
}

const statusOptions: SelectOption[] = [
  { label: '全部状态', value: '' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已拒绝', value: 'rejected' }
];

const suggestionTypeOptions: SelectOption[] = [
  { label: '全部类型', value: '' },
  { label: '安全规则', value: 'safety_rule' },
  { label: '合规规则', value: 'compliance_rule' }
];

const query = reactive<SuggestionQuery>({
  keyword: '',
  status: null,
  suggestionType: null
});

const filteredItems = computed(() => {
  return items.value.filter(item => {
    const matchKeyword = !query.keyword ||
      item.triggerPattern.toLowerCase().includes(query.keyword.toLowerCase()) ||
      item.reason.toLowerCase().includes(query.keyword.toLowerCase()) ||
      item.ruleCategory.toLowerCase().includes(query.keyword.toLowerCase());
    const matchStatus = !query.status || item.status === query.status;
    const matchType = !query.suggestionType || item.suggestionType === query.suggestionType;
    return matchKeyword && matchStatus && matchType;
  });
});

function statusLabel(s: string): string {
  const map: Record<string, string> = { pending: '待审核', approved: '已通过', rejected: '已拒绝' };
  return map[s] || s;
}

function statusTagType(s: string): 'warning' | 'success' | 'error' {
  if (s === 'pending') return 'warning';
  if (s === 'approved') return 'success';
  return 'error';
}

function suggestionTypeLabel(t: string): string {
  const map: Record<string, string> = { safety_rule: '安全规则', compliance_rule: '合规规则' };
  return map[t] || t;
}

function actionLabel(a: string): string {
  const map: Record<string, string> = { block: '拦截', warning: '警告', flag: '标记' };
  return map[a] || a;
}

function actionTagType(a: string): 'error' | 'warning' | 'info' {
  if (a === 'block') return 'error';
  if (a === 'warning') return 'warning';
  return 'info';
}

function priorityColor(p: number): string {
  if (p >= 8) return '#d03050';
  if (p >= 5) return '#f0a020';
  return '#18a058';
}

// Table columns
const columns: DataTableColumns<RuleSuggestionItem> = [
  { title: 'ID', key: 'id', width: 70 },
  {
    title: '建议类型',
    key: 'suggestionType',
    width: 120,
    render(row) {
      return h(NTag, { size: 'small', bordered: false }, { default: () => suggestionTypeLabel(row.suggestionType) });
    }
  },
  {
    title: '规则分类',
    key: 'ruleCategory',
    width: 120,
    ellipsis: { tooltip: true }
  },
  {
    title: '触发模式',
    key: 'triggerPattern',
    minWidth: 200,
    ellipsis: { tooltip: true }
  },
  {
    title: '动作',
    key: 'action',
    width: 90,
    render(row) {
      return h(
        NTag,
        { type: actionTagType(row.action), size: 'small', bordered: false },
        { default: () => actionLabel(row.action) }
      );
    }
  },
  {
    title: '优先级',
    key: 'priority',
    width: 90,
    render(row) {
      return h(
        NTooltip,
        null,
        {
          trigger: () =>
            h('span', { style: `color: ${priorityColor(row.priority)}; font-weight: 600` }, row.priority),
          default: () => `优先级: ${row.priority}`
        }
      );
    }
  },
  {
    title: '原因',
    key: 'reason',
    minWidth: 180,
    ellipsis: { tooltip: true },
    render(row) {
      return row.reason || '-';
    }
  },
  {
    title: '命中次数',
    key: 'hitCount',
    width: 90
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
    title: '提交时间',
    key: 'createdAt',
    width: 170,
    render(row) {
      return row.createdAt;
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
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
const items = ref<RuleSuggestionItem[]>([]);
const approvingId = ref<number | null>(null);
const rejectingId = ref<number | null>(null);

async function loadItems() {
  loading.value = true;
  try {
    const res = await fetchGetPendingRuleSuggestions();
    items.value = res.data || [];
  } finally {
    loading.value = false;
  }
}

async function handleApprove(row: RuleSuggestionItem) {
  approvingId.value = row.id;
  try {
    const reviewerName = authStore.userInfo?.username || 'admin';
    await fetchApproveRuleSuggestion(row.id, reviewerName);
    message.success('规则建议已采纳');
    loadItems();
  } catch {
    message.error('操作失败');
  } finally {
    approvingId.value = null;
  }
}

async function handleReject(row: RuleSuggestionItem) {
  rejectingId.value = row.id;
  try {
    const reviewerName = authStore.userInfo?.username || 'admin';
    await fetchRejectRuleSuggestion(row.id, reviewerName);
    message.success('已拒绝');
    loadItems();
  } catch {
    message.error('操作失败');
  } finally {
    rejectingId.value = null;
  }
}

onMounted(loadItems);
</script>

<template>
  <div class="flex-col-stretch gap-16px overflow-auto p-16px">
    <NCard title="安全规则建议审核">
      <template #header-extra>
        <NSpace align="center">
          <NInput
            v-model:value="query.keyword"
            placeholder="搜索触发模式 / 原因 / 分类"
            clearable
            style="width: 240px"
          />
          <NSelect
            v-model:value="query.suggestionType"
            :options="suggestionTypeOptions"
            placeholder="全部类型"
            clearable
            style="width: 140px"
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
        :row-key="(row: Api.Admin.RuleSuggestion) => row.id"
        :scroll-x="1500"
      />
      <NEmpty
        v-if="!loading && filteredItems.length === 0"
        description="暂无规则建议"
        class="mt-32px"
      />
    </NCard>
  </div>
</template>
