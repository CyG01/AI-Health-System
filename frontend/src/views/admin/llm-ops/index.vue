<template>
  <div class="flex-col-stretch gap-16px overflow-auto p-16px">
    <!-- Dashboard Overview -->
    <NCard title="LLMOps 运维面板">
      <template #header-extra>
        <NButton size="small" @click="loadDashboard" :loading="dashboardLoading">刷新</NButton>
      </template>
      <NGrid :x-gap="16" :y-gap="16" :cols="4">
        <NGi>
          <NCard size="small">
            <NStatistic label="今日总请求" :value="(dashboard?.totalRequests as number) ?? 0" />
          </NCard>
        </NGi>
        <NGi>
          <NCard size="small">
            <NStatistic label="平均TTFT(ms)" :value="(dashboard?.avgTtftMs as number) ?? 0" :precision="0" />
          </NCard>
        </NGi>
        <NGi>
          <NCard size="small">
            <NStatistic label="今日Token消耗" :value="formatTokens((dashboard?.totalTokensToday as number) ?? 0)" />
          </NCard>
        </NGi>
        <NGi>
          <NCard size="small">
            <NStatistic label="今日费用" :value="(dashboard?.totalCostToday as number) ?? 0" :precision="4">
              <template #prefix>$</template>
            </NStatistic>
          </NCard>
        </NGi>
      </NGrid>
    </NCard>

    <!-- Model Status -->
    <NCard title="模型状态">
      <NDataTable :columns="modelColumns" :data="modelRows" :loading="modelLoading" size="small" />
    </NCard>

    <!-- Circuit Breaker Status -->
    <NCard title="安全熔断器">
      <NDescriptions v-if="circuitStatus" label-placement="left" bordered :column="3" size="small">
        <NDescriptionsItem label="当前状态">
          <NTag :type="circuitType" size="small">{{ circuitStatus.state }}</NTag>
        </NDescriptionsItem>
        <NDescriptionsItem label="平均安全评分">
          {{ (circuitStatus.avgSafetyScore ?? 0).toFixed(2) }}
        </NDescriptionsItem>
        <NDescriptionsItem label="摘要">{{ circuitStatus.summary || '-' }}</NDescriptionsItem>
      </NDescriptions>
      <NEmpty v-else description="加载中..." />
    </NCard>

    <!-- Prompt Management -->
    <NCard title="Prompt 版本管理">
      <NSpace class="mb-3" align="center">
        <NInput v-model:value="promptKey" placeholder="模板Key (如 health_plan)" style="width: 260px" />
        <NButton type="primary" size="small" @click="loadPromptInfo" :loading="promptLoading" :disabled="!promptKey">
          查询
        </NButton>
        <NButton size="small" @click="loadRunningCanaries" :loading="canaryLoading">进行中的灰度</NButton>
      </NSpace>

      <NGrid v-if="activePrompt" :x-gap="16" :y-gap="16" :cols="2">
        <NGi>
          <NDescriptions label-placement="left" bordered :column="1" size="small">
            <NDescriptionsItem label="模板Key">{{ activePrompt.templateKey }}</NDescriptionsItem>
            <NDescriptionsItem label="活跃版本">v{{ activePrompt.activeVersion }}</NDescriptionsItem>
          </NDescriptions>
        </NGi>
        <NGi>
          <NSpace vertical>
            <NButton size="small" type="warning" @click="handleRollback" :disabled="!promptKey">回滚到上一版本</NButton>
            <NPopconfirm @positive-click="handleRollback">
              <template #trigger>
                <NButton size="small" type="error" ghost>强制回滚</NButton>
              </template>
              确认回滚到上一个版本？
            </NPopconfirm>
          </NSpace>
        </NGi>
      </NGrid>

      <!-- Prompt History -->
      <NCard v-if="promptHistory.length" title="版本历史" size="small" class="mt-3">
        <NDataTable :columns="historyColumns" :data="promptHistory" size="small" />
      </NCard>

      <!-- A/B Test -->
      <NDivider>灰度发布 (Canary)</NDivider>
      <NForm label-placement="left" label-width="100" :show-feedback="false">
        <NGrid :x-gap="16" :cols="4">
          <NGi>
            <NFormItem label="版本号">
              <NInputNumber v-model:value="canaryForm.version" :min="1" style="width: 100%" />
            </NFormItem>
          </NGi>
          <NGi>
            <NFormItem label="初始比例(%)">
              <NInputNumber v-model:value="canaryForm.percentage" :min="1" :max="100" style="width: 100%" />
            </NFormItem>
          </NGi>
          <NGi>
            <NFormItem label="扩大比例(%)">
              <NInputNumber v-model:value="canaryForm.increasePercent" :min="1" :max="100" style="width: 100%" />
            </NFormItem>
          </NGi>
          <NGi>
            <NSpace>
              <NButton type="primary" size="small" @click="handleStartCanary" :loading="canaryLoading">启动灰度</NButton>
              <NButton size="small" @click="handleIncreaseCanary" :loading="canaryLoading">扩大比例</NButton>
              <NButton type="success" size="small" @click="handleCompleteCanary" :loading="canaryLoading">完成</NButton>
              <NButton type="error" size="small" @click="handleCancelCanary" :loading="canaryLoading">取消</NButton>
            </NSpace>
          </NGi>
        </NGrid>
      </NForm>

      <!-- Canary Status -->
      <NCard v-if="canaryStatus" title="灰度状态" size="small" class="mt-3">
        <NDescriptions label-placement="left" bordered :column="3" size="small">
          <NDescriptionsItem label="模板">{{ canaryStatus.templateKey }}</NDescriptionsItem>
          <NDescriptionsItem label="版本">v{{ canaryStatus.version }}</NDescriptionsItem>
          <NDescriptionsItem label="当前比例">{{ canaryStatus.currentPercentage }}%</NDescriptionsItem>
          <NDescriptionsItem label="状态">
            <NTag :type="canaryStatus.status === 'running' ? 'warning' : 'default'" size="small">
              {{ canaryStatus.status }}
            </NTag>
          </NDescriptionsItem>
        </NDescriptions>
      </NCard>

      <!-- Running Canaries -->
      <NCard v-if="runningCanaries.length" title="进行中的灰度发布" size="small" class="mt-3">
        <NDataTable :columns="runningCanaryColumns" :data="runningCanaries" size="small" />
      </NCard>
    </NCard>

    <!-- Alerts -->
    <NCard title="近期告警">
      <template #header-extra>
        <NSpace align="center">
          <NInputNumber v-model:value="alertLimit" :min="5" :max="100" size="small" style="width: 100px" />
          <NButton size="small" @click="loadAlerts" :loading="alertLoading">查询</NButton>
        </NSpace>
      </template>
      <NDataTable :columns="alertColumns" :data="alerts" :loading="alertLoading" size="small" />
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { useMessage } from 'naive-ui';
import {
  fetchLlmOpsDashboard,
  fetchLlmOpsModelStatus,
  fetchCircuitStatus,
  fetchActivePromptVersion,
  fetchPromptHistory,
  fetchRollbackPrompt,
  fetchStartCanary,
  fetchIncreaseCanary,
  fetchCompleteCanary,
  fetchCancelCanary,
  fetchCanaryStatus,
  fetchRunningCanaries,
  fetchLlmOpsAlerts
} from '@/service/api';

defineOptions({ name: 'LlmOpsManage' });
const message = useMessage();

// === Dashboard ===
const dashboardLoading = ref(false);
const dashboard = ref<Record<string, unknown> | null>(null);

async function loadDashboard() {
  dashboardLoading.value = true;
  try {
    const { data } = await fetchLlmOpsDashboard();
    dashboard.value = data as Record<string, unknown>;
  } catch (e) {
    message.error('加载面板数据失败');
  } finally {
    dashboardLoading.value = false;
  }
}

// === Model Status ===
const modelLoading = ref(false);
const modelRows = ref<Record<string, unknown>[]>([]);
const modelColumns: DataTableColumns = [
  { title: '模型', key: 'model', width: 160 },
  { title: '状态', key: 'status', width: 100 },
  { title: '可用性', key: 'available', width: 100 },
  { title: '延迟(ms)', key: 'latencyMs', width: 100 },
  { title: '备注', key: 'note' }
];

async function loadModelStatus() {
  modelLoading.value = true;
  try {
    const { data } = await fetchLlmOpsModelStatus();
    const obj = data as Record<string, unknown>;
    modelRows.value = Object.entries(obj).map(([model, info]) => {
      const i = (info ?? {}) as Record<string, unknown>;
      return { model, status: i.status ?? '-', available: i.available ?? '-', latencyMs: i.latencyMs ?? '-', note: i.note ?? '' };
    });
  } catch {
    message.error('加载模型状态失败');
  } finally {
    modelLoading.value = false;
  }
}

// === Circuit Breaker ===
const circuitStatus = ref<Api.LlmOps.CircuitStatus | null>(null);
const circuitType = computed(() => {
  const s = circuitStatus.value?.state?.toLowerCase();
  if (s === 'closed' || s === 'normal') return 'success';
  if (s === 'open' || s === 'tripped') return 'error';
  return 'warning';
});

async function loadCircuitStatus() {
  try {
    const { data } = await fetchCircuitStatus();
    circuitStatus.value = data as unknown as Api.LlmOps.CircuitStatus;
  } catch {
    /* ignore */
  }
}

// === Prompt Management ===
const promptKey = ref('');
const promptLoading = ref(false);
const activePrompt = ref<{ templateKey: string; activeVersion: number; content: string } | null>(null);
const promptHistory = ref<Record<string, unknown>[]>([]);
const historyColumns: DataTableColumns = [
  { title: '版本', key: 'version', width: 80 },
  { title: '内容预览', key: 'content', ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '状态', key: 'isActive', width: 80 }
];

async function loadPromptInfo() {
  if (!promptKey.value) return;
  promptLoading.value = true;
  try {
    const [activeRes, historyRes] = await Promise.all([
      fetchActivePromptVersion(promptKey.value),
      fetchPromptHistory(promptKey.value)
    ]);
    activePrompt.value = activeRes.data as unknown as { templateKey: string; activeVersion: number; content: string };
    const h = historyRes.data;
    promptHistory.value = Array.isArray(h) ? (h as Record<string, unknown>[]) : [];
  } catch {
    message.error('加载Prompt信息失败');
  } finally {
    promptLoading.value = false;
  }
}

async function handleRollback() {
  if (!promptKey.value) return;
  try {
    await fetchRollbackPrompt(promptKey.value);
    message.success('回滚成功');
    await loadPromptInfo();
  } catch {
    message.error('回滚失败');
  }
}

// === Canary Deployment ===
const canaryLoading = ref(false);
const canaryForm = ref({ version: 1, percentage: 10, increasePercent: 25 });
const canaryStatus = ref<Api.LlmOps.CanaryStatus | null>(null);
const runningCanaries = ref<Record<string, unknown>[]>([]);
const runningCanaryColumns: DataTableColumns = [
  { title: '模板', key: 'templateKey' },
  { title: '版本', key: 'version', width: 80 },
  { title: '比例', key: 'currentPercentage', width: 80 },
  { title: '状态', key: 'status', width: 100 }
];

async function handleStartCanary() {
  if (!promptKey.value) { message.warning('请先输入模板Key'); return; }
  canaryLoading.value = true;
  try {
    await fetchStartCanary(promptKey.value, { version: canaryForm.value.version, percentage: canaryForm.value.percentage });
    message.success('灰度发布已启动');
    await loadCanaryStatus();
  } catch { message.error('启动灰度失败'); }
  finally { canaryLoading.value = false; }
}

async function handleIncreaseCanary() {
  if (!promptKey.value) return;
  canaryLoading.value = true;
  try {
    await fetchIncreaseCanary(promptKey.value, { percentage: canaryForm.value.increasePercent });
    message.success('灰度比例已扩大');
    await loadCanaryStatus();
  } catch { message.error('扩大比例失败'); }
  finally { canaryLoading.value = false; }
}

async function handleCompleteCanary() {
  if (!promptKey.value) return;
  canaryLoading.value = true;
  try {
    await fetchCompleteCanary(promptKey.value);
    message.success('灰度发布已完成');
    await loadCanaryStatus();
  } catch { message.error('完成灰度失败'); }
  finally { canaryLoading.value = false; }
}

async function handleCancelCanary() {
  if (!promptKey.value) return;
  canaryLoading.value = true;
  try {
    await fetchCancelCanary(promptKey.value);
    message.success('灰度发布已取消');
    canaryStatus.value = null;
  } catch { message.error('取消灰度失败'); }
  finally { canaryLoading.value = false; }
}

async function loadCanaryStatus() {
  if (!promptKey.value) return;
  try {
    const { data } = await fetchCanaryStatus(promptKey.value);
    canaryStatus.value = data as unknown as Api.LlmOps.CanaryStatus;
  } catch { canaryStatus.value = null; }
}

async function loadRunningCanaries() {
  canaryLoading.value = true;
  try {
    const { data } = await fetchRunningCanaries();
    runningCanaries.value = Array.isArray(data) ? (data as Record<string, unknown>[]) : [];
  } catch { message.error('加载灰度列表失败'); }
  finally { canaryLoading.value = false; }
}

// === Alerts ===
const alertLimit = ref(20);
const alertLoading = ref(false);
const alerts = ref<Record<string, unknown>[]>([]);
const alertColumns: DataTableColumns = [
  { title: '类型', key: 'type', width: 120 },
  { title: '消息', key: 'message', ellipsis: { tooltip: true } },
  { title: '严重度', key: 'severity', width: 100 },
  { title: '时间', key: 'timestamp', width: 180 }
];

async function loadAlerts() {
  alertLoading.value = true;
  try {
    const { data } = await fetchLlmOpsAlerts(alertLimit.value);
    alerts.value = Array.isArray(data) ? (data as Record<string, unknown>[]) : [];
  } catch { message.error('加载告警失败'); }
  finally { alertLoading.value = false; }
}

// === Helpers ===
function formatTokens(n: number): string {
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(2) + 'M';
  if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K';
  return String(n);
}

// === Init ===
onMounted(async () => {
  await Promise.all([loadDashboard(), loadModelStatus(), loadCircuitStatus(), loadAlerts()]);
});
</script>
