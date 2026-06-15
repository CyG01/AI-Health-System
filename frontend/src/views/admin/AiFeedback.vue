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
  NEmpty,
  useMessage
} from 'naive-ui';
import type { DataTableColumns, SelectOption } from 'naive-ui';
import { fetchGetPendingAiFeedbacks, fetchReviewAiFeedback } from '@/service/api';

defineOptions({ name: 'AdminAiFeedback' });

const message = useMessage();

type AiFeedbackItem = Api.Admin.AiFeedback;

interface FeedbackQuery {
  keyword: string;
  rating: string | null;
  reviewStatus: string | null;
}

const ratingOptions: SelectOption[] = [
  { label: '全部类型', value: '' },
  { label: '有用', value: 'useful' },
  { label: '无用', value: 'useless' },
  { label: '不正确', value: 'incorrect' }
];

const reviewStatusOptions: SelectOption[] = [
  { label: '全部状态', value: '' },
  { label: '待审核', value: 'pending' },
  { label: '有效', value: 'valid' },
  { label: '无效', value: 'invalid' },
  { label: '重复', value: 'duplicate' }
];

const query = reactive<FeedbackQuery>({
  keyword: '',
  rating: null,
  reviewStatus: null
});

const filteredItems = computed(() => {
  return items.value.filter(item => {
    const matchKeyword = !query.keyword ||
      item.comment.toLowerCase().includes(query.keyword.toLowerCase()) ||
      String(item.userId).includes(query.keyword);
    const matchRating = !query.rating || item.rating === query.rating;
    const matchStatus = !query.reviewStatus || item.reviewStatus === query.reviewStatus;
    return matchKeyword && matchRating && matchStatus;
  });
});

function ratingLabel(r: string): string {
  const map: Record<string, string> = { useful: '有用', useless: '无用', incorrect: '不正确' };
  return map[r] || r;
}

function ratingTagType(r: string): 'success' | 'error' | 'warning' {
  if (r === 'useful') return 'success';
  if (r === 'useless') return 'warning';
  return 'error';
}

function reviewStatusLabel(s: string): string {
  const map: Record<string, string> = { pending: '待审核', valid: '有效', invalid: '无效', duplicate: '重复' };
  return map[s] || s;
}

function reviewStatusTagType(s: string): 'warning' | 'success' | 'error' | 'info' {
  if (s === 'pending') return 'warning';
  if (s === 'valid') return 'success';
  if (s === 'invalid') return 'error';
  return 'info';
}

// Table columns
const columns: DataTableColumns<AiFeedbackItem> = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '用户ID', key: 'userId', width: 80 },
  {
    title: '反馈类型',
    key: 'rating',
    width: 100,
    render(row) {
      return h(
        NTag,
        { type: ratingTagType(row.rating), size: 'small', bordered: false },
        { default: () => ratingLabel(row.rating) }
      );
    }
  },
  {
    title: '反馈内容',
    key: 'comment',
    minWidth: 220,
    ellipsis: { tooltip: true },
    render(row) {
      return row.comment || '-';
    }
  },
  {
    title: 'AI响应ID',
    key: 'aiResponseId',
    width: 160,
    ellipsis: { tooltip: true }
  },
  {
    title: '状态',
    key: 'reviewStatus',
    width: 100,
    render(row) {
      return h(
        NTag,
        { type: reviewStatusTagType(row.reviewStatus), size: 'small', bordered: false },
        { default: () => reviewStatusLabel(row.reviewStatus) }
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
    width: 220,
    fixed: 'right',
    render(row) {
      if (row.reviewStatus === 'pending') {
        return h(NSpace, null, {
          default: () => [
            h(
              NButton,
              {
                type: 'success',
                size: 'small',
                loading: reviewingId.value === row.id,
                onClick: () => handleReview(row, 'valid')
              },
              { default: () => '有效' }
            ),
            h(
              NButton,
              {
                type: 'error',
                size: 'small',
                loading: reviewingId.value === row.id,
                onClick: () => handleReview(row, 'invalid')
              },
              { default: () => '无效' }
            ),
            h(
              NButton,
              {
                type: 'info',
                size: 'small',
                loading: reviewingId.value === row.id,
                onClick: () => handleReview(row, 'duplicate')
              },
              { default: () => '重复' }
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
const items = ref<AiFeedbackItem[]>([]);
const reviewingId = ref<number | null>(null);

async function loadItems() {
  loading.value = true;
  try {
    const res = await fetchGetPendingAiFeedbacks();
    let data = res.data || [];
    if (Array.isArray(data)) {
      data = data.filter((i: AiFeedbackItem) => i.reviewStatus === 'pending');
    }
    items.value = data;
  } finally {
    loading.value = false;
  }
}

async function handleReview(row: AiFeedbackItem, result: string) {
  reviewingId.value = row.id;
  try {
    await fetchReviewAiFeedback(row.id, result);
    message.success(reviewStatusLabel(result));
    loadItems();
  } catch {
    message.error('操作失败');
  } finally {
    reviewingId.value = null;
  }
}

onMounted(loadItems);
</script>

<template>
  <div class="flex-col-stretch gap-16px overflow-auto p-16px">
    <NCard title="AI反馈审核">
      <template #header-extra>
        <NSpace align="center">
          <NInput
            v-model:value="query.keyword"
            placeholder="搜索反馈内容 / 用户ID"
            clearable
            style="width: 220px"
          />
          <NSelect
            v-model:value="query.rating"
            :options="ratingOptions"
            placeholder="全部类型"
            clearable
            style="width: 130px"
          />
          <NSelect
            v-model:value="query.reviewStatus"
            :options="reviewStatusOptions"
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
        :row-key="(row: Api.Admin.AiFeedback) => row.id"
        :scroll-x="1300"
      />
      <NEmpty
        v-if="!loading && filteredItems.length === 0"
        description="暂无待审核反馈"
        class="mt-32px"
      />
    </NCard>
  </div>
</template>
