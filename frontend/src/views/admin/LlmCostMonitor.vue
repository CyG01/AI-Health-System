<template>
  <div class="flex-col-stretch gap-16px overflow-auto p-16px">
    <!-- Global Daily Cost Overview -->
    <NCard title="LLM 费用监控">
      <template #header-extra>
        <NSpace align="center">
          <NButton size="small" @click="refreshAll" :loading="globalLoading">刷新</NButton>
        </NSpace>
      </template>

      <NGrid :x-gap="16" :y-gap="16" :cols="4">
        <NGi>
          <NCard size="small">
            <NStatistic label="今日总费用" :value="globalCost.totalCost" :precision="4">
              <template #prefix>$</template>
            </NStatistic>
          </NCard>
        </NGi>
        <NGi>
          <NCard size="small">
            <NStatistic label="今日总请求" :value="totalRequests" />
          </NCard>
        </NGi>
        <NGi>
          <NCard size="small">
            <NStatistic label="超预算用户" :value="overBudgetUsers.length">
              <template #suffix>人</template>
            </NStatistic>
          </NCard>
        </NGi>
        <NGi>
          <NCard size="small">
            <NStatistic label="监控日期" :value="globalCost.date || '-'" />
          </NCard>
        </NGi>
      </NGrid>

      <!-- Cost by Tier Chart -->
      <div class="mt-4" v-if="globalCost.tierCosts && globalCost.tierCosts.length > 0">
        <h4 class="text-sm font-semibold mb-2">各层级费用分布</h4>
        <div ref="tierChartRef" style="width: 100%; height: 300px;"></div>
      </div>
    </NCard>

    <!-- Per-User Daily Cost -->
    <NCard title="用户每日费用查询">
      <NSpace align="center" class="mb-3">
        <NInput
          v-model:value="userCostQueryId"
          placeholder="输入用户 ID"
          style="width: 200px"
          @keyup.enter="loadUserDailyCost"
        />
        <NButton type="primary" @click="loadUserDailyCost" :loading="userCostLoading">查询</NButton>
      </NSpace>

      <div v-if="userDailyCost">
        <NDescriptions label-placement="left" bordered :column="3" size="small" class="mb-4">
          <NDescriptionsItem label="用户 ID">{{ userDailyCost.userId }}</NDescriptionsItem>
          <NDescriptionsItem label="日期">{{ userDailyCost.date }}</NDescriptionsItem>
          <NDescriptionsItem label="总费用">
            <NTag :type="userDailyCost.totalCost > 1 ? 'error' : 'success'" size="small">
              ${{ userDailyCost.totalCost?.toFixed(4) }}
            </NTag>
          </NDescriptionsItem>
          <NDescriptionsItem label="状态">
            <NTag :type="userDailyCost.isPaused ? 'error' : 'success'" size="small">
              {{ userDailyCost.isPaused ? '已暂停' : '正常' }}
            </NTag>
          </NDescriptionsItem>
        </NDescriptions>

        <NGrid :x-gap="16" :cols="2" v-if="userDailyCost.costByIntent?.length || userDailyCost.costByModel?.length">
          <NGi v-if="userDailyCost.costByIntent?.length">
            <h4 class="text-sm font-semibold mb-2">按意图分类</h4>
            <NDataTable
              :columns="intentColumns"
              :data="userDailyCost.costByIntent"
              :bordered="true"
              size="small"
            />
          </NGi>
          <NGi v-if="userDailyCost.costByModel?.length">
            <h4 class="text-sm font-semibold mb-2">按模型分类</h4>
            <NDataTable
              :columns="modelCostColumns"
              :data="userDailyCost.costByModel"
              :bordered="true"
              size="small"
            />
          </NGi>
        </NGrid>
      </div>
    </NCard>

    <!-- Over-budget Users -->
    <NCard title="超预算用户列表">
      <NDataTable
        :columns="overBudgetColumns"
        :data="overBudgetUsers"
        :loading="overBudgetLoading"
        :bordered="true"
        size="small"
        :row-key="(row: Api.LlmCost.OverBudgetUser) => row.userId"
      />
    </NCard>

    <!-- Model Status Cards -->
    <NCard title="模型路由状态">
      <NSpin :show="modelStatusLoading">
        <div v-if="modelStatusData" class="flex flex-col gap-3">
          <NCard
            v-for="(value, key) in modelStatusData"
            :key="String(key)"
            size="small"
          >
            <div class="flex items-center justify-between">
              <span class="font-semibold">{{ key }}</span>
              <NTag size="small" :type="getStatusType(value)">
                {{ formatStatus(value) }}
              </NTag>
            </div>
            <pre class="text-xs text-gray-400 mt-2 mb-0 whitespace-pre-wrap">{{ JSON.stringify(value, null, 2) }}</pre>
          </NCard>
        </div>
        <NEmpty v-else-if="!modelStatusLoading" description="暂无模型状态数据" />
      </NSpin>
    </NCard>

    <!-- Tier Circuit Breaker Status -->
    <NCard title="层级熔断器状态">
      <NSpin :show="circuitBreakerLoading">
        <div v-if="circuitBreakerData" class="flex flex-col gap-3">
          <NCard
            v-for="(value, key) in circuitBreakerData"
            :key="String(key)"
            size="small"
          >
            <div class="flex items-center justify-between">
              <span class="font-semibold">{{ key }}</span>
              <NTag size="small" :type="getStatusType(value)">
                {{ formatStatus(value) }}
              </NTag>
            </div>
            <pre class="text-xs text-gray-400 mt-2 mb-0 whitespace-pre-wrap">{{ JSON.stringify(value, null, 2) }}</pre>
          </NCard>
        </div>
        <NEmpty v-else-if="!circuitBreakerLoading" description="暂无熔断器状态数据" />
      </NSpin>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h, onMounted, onBeforeUnmount, nextTick } from 'vue';
import {
  NCard, NDataTable, NInput, NButton, NTag, NSpace, NGrid, NGi,
  NStatistic, NDescriptions, NDescriptionsItem, NSpin, NEmpty,
  useMessage
} from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import {
  fetchGetGlobalDailyCost,
  fetchGetUserDailyCost,
  fetchGetOverBudgetUsers,
  fetchPauseUserLlm,
  fetchResumeUserLlm,
  fetchGetModelStatus,
  fetchGetTierCircuitBreakerStatus
} from '@/service/api';
import echarts from '@/utils/echarts';

defineOptions({ name: 'LlmCostMonitor' });

const message = useMessage();

// Global daily cost
const globalLoading = ref(false);
const globalCost = ref<Api.LlmCost.GlobalDailyCost>({ totalCost: 0, tierCosts: [], date: '' });
const tierChartRef = ref<HTMLElement | null>(null);
let tierChart: echarts.ECharts | null = null;

const totalRequests = computed(() => {
  return (globalCost.value.tierCosts || []).reduce((sum, t) => sum + (t.requestCount || 0), 0);
});

// User daily cost
const userCostQueryId = ref('');
const userCostLoading = ref(false);
const userDailyCost = ref<Api.LlmCost.UserDailyCost | null>(null);

// Over-budget users
const overBudgetLoading = ref(false);
const overBudgetUsers = ref<Api.LlmCost.OverBudgetUser[]>([]);

// Model status
const modelStatusLoading = ref(false);
const modelStatusData = ref<Api.LlmCost.ModelStatus | null>(null);

// Circuit breaker
const circuitBreakerLoading = ref(false);
const circuitBreakerData = ref<Api.LlmCost.TierCircuitBreakerStatus | null>(null);

// Table columns for intent costs
const intentColumns: DataTableColumns<Api.LlmCost.IntentCost> = [
  { title: '意图', key: 'intent', minWidth: 100 },
  {
    title: '费用',
    key: 'cost',
    width: 100,
    render(row) { return h('span', `$${row.cost.toFixed(4)}`); }
  },
  { title: '请求数', key: 'requestCount', width: 80 }
];

// Table columns for model costs
const modelCostColumns: DataTableColumns<Api.LlmCost.ModelCost> = [
  { title: '模型', key: 'model', minWidth: 120 },
  {
    title: '费用',
    key: 'cost',
    width: 100,
    render(row) { return h('span', `$${row.cost.toFixed(4)}`); }
  },
  { title: '请求数', key: 'requestCount', width: 80 }
];

// Over-budget users columns
const overBudgetColumns: DataTableColumns<Api.LlmCost.OverBudgetUser> = [
  { title: '用户 ID', key: 'userId', width: 100 },
  { title: '用户名', key: 'username', minWidth: 120 },
  {
    title: '费用',
    key: 'totalCost',
    width: 120,
    render(row) {
      return h(NTag, { type: 'error', size: 'small' }, { default: () => `$${row.totalCost.toFixed(4)}` });
    }
  },
  {
    title: '预算',
    key: 'budget',
    width: 120,
    render(row) {
      return h('span', `$${row.budget.toFixed(4)}`);
    }
  },
  {
    title: '超出比例',
    key: 'overPercent',
    width: 120,
    render(row) {
      const pct = row.budget > 0 ? ((row.totalCost - row.budget) / row.budget * 100).toFixed(1) : '-';
      return h(NTag, { type: 'warning', size: 'small' }, { default: () => `${pct}%` });
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render(row) {
      return h(NSpace, { size: 'small' }, {
        default: () => [
          h(NButton, {
            size: 'small',
            type: 'error',
            onClick: () => handlePauseUser(row.userId)
          }, { default: () => '暂停' }),
          h(NButton, {
            size: 'small',
            type: 'success',
            onClick: () => handleResumeUser(row.userId)
          }, { default: () => '恢复' })
        ]
      });
    }
  }
];

// Helper: determine status type
function getStatusType(value: unknown): 'success' | 'error' | 'warning' | 'info' | 'default' {
  if (typeof value === 'object' && value !== null) {
    const obj = value as Record<string, unknown>;
    if (obj.status === 'open' || obj.status === 'active' || obj.enabled === true) return 'success';
    if (obj.status === 'tripped' || obj.status === 'open_circuit' || obj.enabled === false) return 'error';
  }
  return 'info';
}

function formatStatus(value: unknown): string {
  if (typeof value === 'object' && value !== null) {
    const obj = value as Record<string, unknown>;
    if (obj.status) return String(obj.status);
    if (obj.enabled !== undefined) return obj.enabled ? '启用' : '禁用';
  }
  return String(value);
}

// Data loading functions
async function loadGlobalCost() {
  globalLoading.value = true;
  try {
    const { data } = await fetchGetGlobalDailyCost();
    globalCost.value = data || { totalCost: 0, tierCosts: [], date: '' };
    await nextTick();
    renderTierChart();
  } catch {
    message.error('获取全局费用概览失败');
  } finally {
    globalLoading.value = false;
  }
}

function renderTierChart() {
  if (!tierChartRef.value || !globalCost.value.tierCosts?.length) return;

  if (!tierChart) {
    tierChart = echarts.init(tierChartRef.value);
  }

  const tiers = globalCost.value.tierCosts.map(t => t.tier);
  const costs = globalCost.value.tierCosts.map(t => t.cost);
  const requests = globalCost.value.tierCosts.map(t => t.requestCount);

  tierChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['费用 ($)', '请求数'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: tiers },
    yAxis: [
      { type: 'value', name: '费用 ($)', position: 'left' },
      { type: 'value', name: '请求数', position: 'right' }
    ],
    series: [
      {
        name: '费用 ($)',
        type: 'bar',
        data: costs,
        itemStyle: { color: '#58a6ff' }
      },
      {
        name: '请求数',
        type: 'bar',
        yAxisIndex: 1,
        data: requests,
        itemStyle: { color: '#3fb950' }
      }
    ]
  });
}

async function loadUserDailyCost() {
  const userId = Number(userCostQueryId.value);
  if (!userId || isNaN(userId)) {
    message.warning('请输入有效的用户 ID');
    return;
  }
  userCostLoading.value = true;
  userDailyCost.value = null;
  try {
    const { data } = await fetchGetUserDailyCost(userId);
    userDailyCost.value = data || null;
  } catch {
    message.error('获取用户费用详情失败');
  } finally {
    userCostLoading.value = false;
  }
}

async function loadOverBudgetUsers() {
  overBudgetLoading.value = true;
  try {
    const { data } = await fetchGetOverBudgetUsers();
    overBudgetUsers.value = data || [];
  } catch {
    message.error('获取超预算用户列表失败');
  } finally {
    overBudgetLoading.value = false;
  }
}

async function handlePauseUser(userId: number) {
  try {
    await fetchPauseUserLlm(userId);
    message.success(`用户 ${userId} 的 LLM 调用已暂停`);
    loadOverBudgetUsers();
  } catch {
    message.error('操作失败');
  }
}

async function handleResumeUser(userId: number) {
  try {
    await fetchResumeUserLlm(userId);
    message.success(`用户 ${userId} 的 LLM 调用已恢复`);
    loadOverBudgetUsers();
  } catch {
    message.error('操作失败');
  }
}

async function loadModelStatus() {
  modelStatusLoading.value = true;
  try {
    const { data } = await fetchGetModelStatus();
    modelStatusData.value = data || null;
  } catch {
    message.error('获取模型状态失败');
  } finally {
    modelStatusLoading.value = false;
  }
}

async function loadCircuitBreakerStatus() {
  circuitBreakerLoading.value = true;
  try {
    const { data } = await fetchGetTierCircuitBreakerStatus();
    circuitBreakerData.value = data || null;
  } catch {
    message.error('获取熔断器状态失败');
  } finally {
    circuitBreakerLoading.value = false;
  }
}

function refreshAll() {
  loadGlobalCost();
  loadOverBudgetUsers();
  loadModelStatus();
  loadCircuitBreakerStatus();
}

function handleResize() {
  tierChart?.resize();
}

onMounted(() => {
  refreshAll();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  tierChart?.dispose();
  tierChart = null;
});
</script>
