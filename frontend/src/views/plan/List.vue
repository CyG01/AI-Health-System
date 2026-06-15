<template>
  <div class="p-1">
    <n-spin :show="pageLoading">
      <MedicalDisclaimerBanner />

      <n-card class="max-w-[860px]">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-xl font-semibold m-0">我的 AI 计划</h2>
          <n-button type="primary" @click="router.push('/plan/generate')">生成新计划</n-button>
        </div>

        <!-- Summary Header -->
        <div class="flex items-center gap-4 mb-5 p-3 rounded-lg bg-gray-800/40">
          <div class="flex items-center gap-2">
            <span class="text-2xl font-bold text-blue-400">{{ totalCount }}</span>
            <span class="text-xs text-gray-400">总计划</span>
          </div>
          <span class="w-px h-8 bg-gray-700"></span>
          <div class="flex items-center gap-2">
            <span class="text-2xl font-bold text-yellow-500">{{ activeCount }}</span>
            <span class="text-xs text-gray-400">生效中</span>
          </div>
          <span class="w-px h-8 bg-gray-700"></span>
          <div class="flex items-center gap-2">
            <span class="text-2xl font-bold text-green-400">{{ completedCount }}</span>
            <span class="text-xs text-gray-400">已完成</span>
          </div>
        </div>

        <!-- Search Bar -->
        <div class="flex items-center gap-3 mb-4">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索计划名称..."
            clearable
            class="max-w-[300px]"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <n-icon :size="16"><SearchOutline /></n-icon>
            </template>
          </n-input>
          <n-button size="small" @click="handleSearch">搜索</n-button>
          <n-button v-if="searchKeyword" size="small" quaternary @click="clearSearch">清除</n-button>
        </div>

        <!-- Plan List -->
        <div v-if="list.length > 0" class="flex flex-col gap-3">
          <div
            v-for="plan in list"
            :key="plan.id"
            class="flex items-center justify-between p-4 rounded-lg cursor-pointer transition-all border"
            :class="[
              plan.status === 1
                ? 'border-yellow-600/30 bg-yellow-600/6'
                : 'border-transparent bg-blue-500/4'
            ]"
            style="border-width: 1px; border-style: solid;"
            @click="router.push(`/plan/${plan.id}`)"
          >
            <div class="flex flex-col gap-2">
              <div class="flex items-center gap-2.5">
                <n-tag
                  :type="planTypeConfig[plan.planType]?.type || 'default'"
                  size="small"
                >
                  {{ planTypeConfig[plan.planType]?.label || plan.planType }}
                </n-tag>
                <span class="text-sm font-semibold">{{ plan.planName || plan.title || '未命名计划' }}</span>
                <n-tag v-if="plan.status === 1" type="warning" size="small">
                  当前生效
                </n-tag>
              </div>
              <div class="text-xs text-gray-400">
                <span>{{ plan.durationDays }}天计划</span>
                <span class="mx-1 text-gray-700">|</span>
                <span>开始：{{ plan.startDate || '-' }}</span>
                <span class="mx-1 text-gray-700">|</span>
                <span>创建：{{ plan.createTime || plan.createdAt || '-' }}</span>
              </div>
            </div>
            <div class="flex items-center gap-1 shrink-0" @click.stop>
              <n-button
                v-if="plan.status !== 1"
                size="small"
                quaternary
                @click="handleActive(plan.id)"
              >
                启用
              </n-button>
              <n-button size="small" quaternary type="error" @click="handleDelete(plan.id)">
                删除
              </n-button>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <n-empty v-else :description="searchKeyword ? '没有找到匹配的计划' : '暂无 AI 计划'" class="py-8">
          <template #extra>
            <n-button type="primary" @click="router.push('/plan/generate')">立即生成</n-button>
          </template>
        </n-empty>

        <!-- Pagination -->
        <div v-if="totalCount > pageSize" class="flex justify-end mt-5">
          <n-pagination
            v-model:page="currentPage"
            :page-count="totalPages"
            :page-size="pageSize"
            show-quick-jumper
            @update:page="handlePageChange"
          />
        </div>
      </n-card>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { NButton, NCard, NEmpty, NIcon, NInput, NPagination, NSpin, NTag, useDialog } from 'naive-ui';
import { SearchOutline } from '@vicons/ionicons5';
import { fetchGetPlanList, fetchActivePlan, fetchDeletePlan } from '@/service/api';
import MedicalDisclaimerBanner from '@/components/MedicalDisclaimerBanner.vue';

defineOptions({ name: 'PlanList' });

interface PlanItem {
  id: number | string;
  planName?: string;
  title?: string;
  planType: string;
  status: number;
  durationDays: number;
  startDate?: string;
  createTime?: string;
  createdAt?: string;
}

interface PlanTypeConfig {
  [key: string]: { label: string; type: 'success' | 'info' | 'warning' | 'error' | 'default' };
}

const router = useRouter();
const dialog = useDialog();
const pageLoading = ref(false);
const list = ref<PlanItem[]>([]);

// Pagination state
const currentPage = ref(1);
const pageSize = ref(10);
const totalCount = ref(0);
const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / pageSize.value)));

// Search state
const searchKeyword = ref('');

// All 5 plan types
const planTypeConfig: PlanTypeConfig = {
  sport: { label: '运动', type: 'success' },
  diet: { label: '饮食', type: 'info' },
  comprehensive: { label: '综合', type: 'warning' },
  rehabilitation: { label: '康复', type: 'error' },
  meditation: { label: '冥想', type: 'info' }
};

// Summary stats
const activeCount = computed(() => list.value.filter(p => p.status === 1).length);
const completedCount = computed(() => list.value.filter(p => p.status === 2).length);

async function loadList(): Promise<void> {
  pageLoading.value = true;
  try {
    const { data } = await fetchGetPlanList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined
    });

    if (data) {
      // Handle both paginated (PageResult) and plain array responses
      if (Array.isArray(data)) {
        list.value = data as PlanItem[];
        totalCount.value = data.length;
      } else {
        // PageResult format
        list.value = (data.records || []) as PlanItem[];
        totalCount.value = data.total || data.records?.length || 0;
      }
    } else {
      list.value = [];
      totalCount.value = 0;
    }
  } finally {
    pageLoading.value = false;
  }
}

function handlePageChange(page: number): void {
  currentPage.value = page;
  loadList();
}

function handleSearch(): void {
  currentPage.value = 1;
  loadList();
}

function clearSearch(): void {
  searchKeyword.value = '';
  currentPage.value = 1;
  loadList();
}

async function handleActive(id: number | string): Promise<void> {
  try {
    await fetchActivePlan(id as number);
    window.$message?.success('计划已切换为当前生效');
    await loadList();
  } catch {
    // handled by interceptor
  }
}

async function handleDelete(id: number | string): Promise<void> {
  const confirmed = await new Promise<boolean>((resolve) => {
    dialog.warning({
      title: '提示',
      content: '确定删除该计划吗？',
      positiveText: '删除',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false)
    });
  });
  if (!confirmed) return;

  try {
    await fetchDeletePlan(id as number);
    window.$message?.success('计划已删除');
    await loadList();
  } catch {
    // cancelled or error handled by interceptor
  }
}

onMounted(() => {
  loadList();
});
</script>
