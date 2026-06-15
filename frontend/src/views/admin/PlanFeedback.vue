<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NInput,
  NSelect,
  NTag,
  NPagination,
  NSpace,
  NRate,
  NModal,
  NDescriptions,
  NDescriptionsItem,
  useMessage
} from 'naive-ui';
import type { DataTableColumns, PaginationProps, SelectOption } from 'naive-ui';
import {
  fetchGetAdminPlanFeedbacks,
  fetchTriggerPlanAdjust,
  fetchGetAdminPlanFeedbackDetail
} from '@/service/api';

defineOptions({ name: 'AdminPlanFeedback' });

const message = useMessage();

type PlanFeedbackItem = Api.Admin.PlanFeedbackVO;

interface FeedbackQuery {
  keyword: string;
  score: string | null;
}

const scoreOptions: SelectOption[] = [
  { label: '全部评分', value: '' },
  { label: '5星', value: '5' },
  { label: '4星', value: '4' },
  { label: '3星', value: '3' },
  { label: '2星', value: '2' },
  { label: '1星', value: '1' }
];

const query = reactive<FeedbackQuery>({
  keyword: '',
  score: null
});

const filteredItems = computed(() => {
  return items.value.filter(item => {
    const matchKeyword = !query.keyword ||
      item.content.toLowerCase().includes(query.keyword.toLowerCase()) ||
      String(item.userId).includes(query.keyword) ||
      String(item.planId).includes(query.keyword);
    const matchScore = !query.score || item.satisfactionScore === Number(query.score);
    return matchKeyword && matchScore;
  });
});

function feedbackTypeLabel(t: string): string {
  const map: Record<string, string> = { positive: '好评', negative: '差评', suggestion: '建议', complaint: '投诉' };
  return map[t] || t;
}

function feedbackTypeTagType(t: string): 'success' | 'error' | 'warning' | 'info' {
  if (t === 'positive') return 'success';
  if (t === 'negative') return 'error';
  if (t === 'complaint') return 'error';
  return 'info';
}

// Table columns
const columns: DataTableColumns<PlanFeedbackItem> = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '计划ID', key: 'planId', width: 80 },
  { title: '用户ID', key: 'userId', width: 80 },
  {
    title: '反馈类型',
    key: 'feedbackType',
    width: 100,
    render(row) {
      return h(
        NTag,
        { type: feedbackTypeTagType(row.feedbackType), size: 'small', bordered: false },
        { default: () => feedbackTypeLabel(row.feedbackType) }
      );
    }
  },
  {
    title: '满意度',
    key: 'satisfactionScore',
    width: 140,
    render(row) {
      if (row.satisfactionScore != null) {
        return h(NRate, { value: row.satisfactionScore, readonly: true, size: 'small' });
      }
      return '-';
    }
  },
  {
    title: '反馈内容',
    key: 'content',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render(row) {
      return row.content || '-';
    }
  },
  {
    title: '调整建议',
    key: 'adjustmentSuggestion',
    minWidth: 160,
    ellipsis: { tooltip: true },
    render(row) {
      return row.adjustmentSuggestion || '-';
    }
  },
  {
    title: '已调整',
    key: 'isAdjusted',
    width: 80,
    render(row) {
      return h(
        NTag,
        { type: row.isAdjusted ? 'success' : 'default', size: 'small', bordered: false },
        { default: () => (row.isAdjusted ? '是' : '否') }
      );
    }
  },
  {
    title: '提交时间',
    key: 'createTime',
    width: 170,
    render(row) {
      return row.createTime;
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    fixed: 'right',
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(
            NButton,
            { size: 'small', text: true, type: 'primary', onClick: () => handleViewDetail(row) },
            { default: () => '详情' }
          ),
          h(
            NButton,
            {
              size: 'small',
              type: 'warning',
              loading: adjustingId.value === row.id,
              onClick: () => handleAdjust(row.id)
            },
            { default: () => '触发调整' }
          )
        ]
      });
    }
  }
];

// Data
const loading = ref(false);
const items = ref<PlanFeedbackItem[]>([]);
const adjustingId = ref<number | null>(null);
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

// Detail modal
const showDetailModal = ref(false);
const detailLoading = ref(false);
const detailData = ref<PlanFeedbackItem | null>(null);

async function fetchData() {
  loading.value = true;
  try {
    const res = await fetchGetAdminPlanFeedbacks({
      page: pagination.page,
      size: pagination.pageSize
    });
    if (res.data) {
      items.value = res.data.records || [];
      const total: number = res.data.total || 0;
      pagination.pageCount = Math.max(1, Math.ceil(total / (pagination.pageSize as number)));
    }
  } finally {
    loading.value = false;
  }
}

async function handleViewDetail(row: PlanFeedbackItem) {
  detailData.value = row;
  showDetailModal.value = true;
  detailLoading.value = true;
  try {
    const res = await fetchGetAdminPlanFeedbackDetail(row.id);
    if (res.data) {
      detailData.value = res.data;
    }
  } catch {
    // fallback to row data
  } finally {
    detailLoading.value = false;
  }
}

async function handleAdjust(id: number) {
  adjustingId.value = id;
  try {
    await fetchTriggerPlanAdjust(id);
    message.success('已触发 AI 计划重新调整');
    fetchData();
  } finally {
    adjustingId.value = null;
  }
}

function handleSearch() {
  // Client-side filtering only — no need to re-fetch
}

function handlePageChange(page: number) {
  pagination.page = page;
  fetchData();
}

onMounted(fetchData);
</script>

<template>
  <div class="flex-col-stretch gap-16px overflow-auto p-16px">
    <NCard title="计划反馈管理">
      <template #header-extra>
        <NSpace align="center">
          <NInput
            v-model:value="query.keyword"
            placeholder="搜索内容 / 用户ID / 计划ID"
            clearable
            style="width: 240px"
          />
          <NSelect
            v-model:value="query.score"
            :options="scoreOptions"
            placeholder="全部评分"
            clearable
            style="width: 130px"
          />
        </NSpace>
      </template>
      <NDataTable
        :columns="columns"
        :data="filteredItems"
        :loading="loading"
        :row-key="(row: Api.Admin.PlanFeedbackVO) => row.id"
        :scroll-x="1400"
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
      title="反馈详情"
      style="width: 560px"
    >
      <NDescriptions v-if="detailData" label-placement="left" bordered :column="1">
        <NDescriptionsItem label="反馈ID">{{ detailData.id }}</NDescriptionsItem>
        <NDescriptionsItem label="计划ID">{{ detailData.planId }}</NDescriptionsItem>
        <NDescriptionsItem label="用户ID">{{ detailData.userId }}</NDescriptionsItem>
        <NDescriptionsItem label="反馈类型">
          <NTag size="small" :type="feedbackTypeTagType(detailData.feedbackType)">{{ feedbackTypeLabel(detailData.feedbackType) }}</NTag>
        </NDescriptionsItem>
        <NDescriptionsItem label="满意度">
          <NRate :value="detailData.satisfactionScore" readonly size="small" />
        </NDescriptionsItem>
        <NDescriptionsItem label="反馈内容">
          {{ detailData.content || '无' }}
        </NDescriptionsItem>
        <NDescriptionsItem label="调整建议">
          {{ detailData.adjustmentSuggestion || '无' }}
        </NDescriptionsItem>
        <NDescriptionsItem label="是否已调整">
          <NTag :type="detailData.isAdjusted ? 'success' : 'default'" size="small">{{ detailData.isAdjusted ? '是' : '否' }}</NTag>
        </NDescriptionsItem>
        <NDescriptionsItem v-if="detailData.newPlanId" label="新计划ID">{{ detailData.newPlanId }}</NDescriptionsItem>
        <NDescriptionsItem label="提交时间">{{ detailData.createTime }}</NDescriptionsItem>
      </NDescriptions>
    </NModal>
  </div>
</template>
