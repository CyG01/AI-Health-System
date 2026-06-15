<template>
  <div class="p-2">
    <n-spin :show="pageLoading">
      <MedicalDisclaimerBanner />

      <!-- Header with generate buttons -->
      <div class="flex justify-between items-center mb-5">
        <h2 class="text-xl font-semibold m-0">AI健康报告</h2>
        <div class="flex gap-2.5">
          <n-button
            type="primary"
            :loading="generating"
            :disabled="cooldownRemaining > 0"
            @click="handleGenerate('weekly')"
          >
            {{ cooldownRemaining > 0 ? `冷却中 (${cooldownRemaining}s)` : '生成周报' }}
          </n-button>
          <n-button
            :loading="generating"
            :disabled="cooldownRemaining > 0"
            @click="handleGenerate('monthly')"
          >
            {{ cooldownRemaining > 0 ? `冷却中 (${cooldownRemaining}s)` : '生成月报' }}
          </n-button>
        </div>
      </div>

      <!-- Rate limit alert -->
      <n-alert
        v-if="cooldownRemaining > 0"
        type="warning"
        class="mb-4"
        :show-icon="true"
      >
        报告生成有频率限制，请等待 {{ cooldownRemaining }} 秒后再试。
      </n-alert>

      <!-- Score Statistics Cards -->
      <div v-if="reports.length > 0" class="flex gap-4 mb-5">
        <n-card size="small" class="flex-1">
          <n-statistic label="报告总数" :value="total" />
        </n-card>
        <n-card size="small" class="flex-1">
          <n-statistic label="平均评分">
            <template #default>
              <span :class="avgScoreColor">{{ avgScore }}</span>
            </template>
            <template #suffix>
              <span class="text-sm text-gray-400">/ 100</span>
            </template>
          </n-statistic>
        </n-card>
        <n-card size="small" class="flex-1">
          <n-statistic label="最新评分">
            <template #default>
              <span :class="latestScoreColor">{{ latestScore }}</span>
            </template>
            <template #suffix>
              <span class="text-sm text-gray-400">/ 100</span>
            </template>
          </n-statistic>
        </n-card>
      </div>

      <!-- Filters -->
      <div class="flex items-center gap-3 mb-5 p-3 bg-gray-800/50 border border-gray-700 rounded-md flex-wrap">
        <span class="text-sm text-gray-400 whitespace-nowrap">筛选：</span>
        <n-select
          v-model:value="filterType"
          :options="reportTypeOptions"
          clearable
          placeholder="全部类型"
          class="w-[140px]"
          @update:value="handleFilterChange"
        />
        <n-date-picker
          v-model:value="filterDateRange"
          type="daterange"
          clearable
          class="w-[280px]"
          @update:value="handleFilterChange"
        />
        <n-button v-if="filterType || filterDateRange" quaternary type="info" size="small" @click="clearFilters">
          清除筛选
        </n-button>
      </div>

      <!-- Report List -->
      <div v-if="reports.length > 0" class="flex flex-col gap-3.5">
        <div
          v-for="report in reports"
          :key="report.id"
          class="p-4 rounded-lg cursor-pointer transition-colors border"
          :class="report.isRead === 0 ? 'border-blue-400/40' : 'border-gray-700'"
          style="border-width: 1px; border-style: solid;"
          @click="viewReport(report)"
        >
          <div class="flex justify-between items-center mb-2.5">
            <div class="flex items-center gap-2.5">
              <n-tag
                :type="report.reportType === 'weekly' ? 'info' : 'success'"
                size="small"
              >
                {{ report.reportType === 'weekly' ? '周报' : '月报' }}
              </n-tag>
              <span class="font-semibold text-[15px]">{{ report.reportPeriod }}</span>
              <n-tag v-if="report.score" size="small" :type="getScoreTagType(report.score)" round>
                {{ report.score }}分
              </n-tag>
            </div>
            <div class="flex items-center gap-2 text-gray-400 text-[13px]">
              <span>{{ formatTime(report.createTime) }}</span>
              <span v-if="report.isRead === 0" class="w-2 h-2 bg-blue-400 rounded-full"></span>
            </div>
          </div>
          <div v-if="report.aiContent" class="text-gray-400 text-[13px] truncate">
            {{ getPreview(report.aiContent) }}
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <n-empty v-else description="暂无健康报告" size="huge" class="py-12">
        <template #extra>
          <n-button type="primary" @click="handleGenerate('weekly')">生成第一份报告</n-button>
        </template>
      </n-empty>

      <!-- Pagination -->
      <div v-if="total > pageSize" class="flex justify-end mt-5">
        <n-pagination
          v-model:page="currentPage"
          :page-count="Math.ceil(total / pageSize)"
          :page-sizes="[10, 20, 50]"
          v-model:page-size="pageSize"
          show-size-picker
          @update:page="loadReports"
          @update:page-size="handlePageSizeChange"
        />
      </div>

      <!-- Report Detail Modal -->
      <n-modal
        v-model:show="dialogVisible"
        preset="card"
        :title="'AI健康' + (detailReport?.reportType === 'weekly' ? '周报' : '月报')"
        style="max-width: 680px"
        :bordered="true"
      >
        <div v-if="reportData" class="text-gray-200">
          <!-- Score -->
          <div v-if="reportData.score" class="text-center mb-6">
            <div class="inline-flex flex-col items-center justify-center w-[90px] h-[90px] rounded-full border-2 border-blue-400/50">
              <span class="text-[32px] font-bold bg-gradient-to-br from-green-400 to-blue-400 bg-clip-text text-transparent">
                {{ reportData.score }}
              </span>
              <span class="text-[11px] text-gray-400 mt-0.5">综合评分</span>
            </div>
          </div>

          <!-- Summary -->
          <div v-if="reportData.summary" class="mb-5">
            <h4 class="text-[15px] text-blue-400 mb-2 pb-1.5 border-b border-gray-700">总体概述</h4>
            <p class="text-sm leading-relaxed text-gray-300">{{ reportData.summary }}</p>
          </div>

          <!-- Achievements -->
          <div v-if="reportData.achievements?.length" class="mb-5">
            <h4 class="text-[15px] text-blue-400 mb-2 pb-1.5 border-b border-gray-700">亮点成就</h4>
            <ul class="pl-4 text-sm leading-relaxed text-gray-300">
              <li v-for="(a, i) in reportData.achievements" :key="i">{{ a }}</li>
            </ul>
          </div>

          <!-- Concerns -->
          <div v-if="reportData.concerns?.length" class="mb-5">
            <h4 class="text-[15px] text-blue-400 mb-2 pb-1.5 border-b border-gray-700">需要关注</h4>
            <ul class="pl-4 text-sm leading-relaxed text-orange-400">
              <li v-for="(c, i) in reportData.concerns" :key="i">{{ c }}</li>
            </ul>
          </div>

          <!-- Suggestions -->
          <div v-if="reportData.suggestions?.length" class="mb-5">
            <h4 class="text-[15px] text-blue-400 mb-2 pb-1.5 border-b border-gray-700">改善建议</h4>
            <ul class="pl-4 text-sm leading-relaxed text-green-400">
              <li v-for="(s, i) in reportData.suggestions" :key="i">{{ s }}</li>
            </ul>
          </div>
        </div>

        <!-- Raw HTML fallback -->
        <div v-else class="text-gray-300 leading-relaxed text-sm" v-html="rawContent"></div>
      </n-modal>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { NButton, NEmpty, NModal, NSpin, NTag, NAlert, NCard, NStatistic, NSelect, NDatePicker, NPagination } from 'naive-ui';
import type { SelectOption } from 'naive-ui';
import { fetchGenerateReport, fetchGetReportList, fetchGetReportDetail } from '@/service/api';
import MedicalDisclaimerBanner from '@/components/MedicalDisclaimerBanner.vue';
import { sanitizeHtml } from '@/utils/sanitize';

defineOptions({ name: 'HealthReport' });

interface ReportRecord {
  id: number;
  reportType: string;
  reportPeriod: string;
  createTime: string;
  isRead: number;
  aiContent: string;
  score: number;
  suggestions: string[];
}

interface ReportData {
  score?: number;
  summary?: string;
  achievements?: string[];
  concerns?: string[];
  suggestions?: string[];
}

const pageLoading = ref(false);
const generating = ref(false);
const reports = ref<ReportRecord[]>([]);
const dialogVisible = ref(false);
const detailReport = ref<ReportRecord | null>(null);

// Pagination
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);

// Filters
const filterType = ref<string | null>(null);
const filterDateRange = ref<[number, number] | null>(null);

const reportTypeOptions: SelectOption[] = [
  { label: '周报', value: 'weekly' },
  { label: '月报', value: 'monthly' }
];

// Rate limit cooldown
const cooldownRemaining = ref(0);
let cooldownTimer: ReturnType<typeof setInterval> | null = null;

const COOLDOWN_SECONDS = 30;

function startCooldown(): void {
  cooldownRemaining.value = COOLDOWN_SECONDS;
  cooldownTimer = setInterval(() => {
    cooldownRemaining.value -= 1;
    if (cooldownRemaining.value <= 0) {
      if (cooldownTimer) clearInterval(cooldownTimer);
      cooldownTimer = null;
    }
  }, 1000);
}

// Score statistics
const avgScore = computed<number>(() => {
  const scored = reports.value.filter(r => r.score > 0);
  if (scored.length === 0) return 0;
  return Math.round(scored.reduce((sum, r) => sum + r.score, 0) / scored.length);
});

const latestScore = computed<number>(() => {
  if (reports.value.length === 0) return 0;
  return reports.value[0]?.score || 0;
});

const avgScoreColor = computed<string>(() => {
  if (avgScore.value >= 80) return 'text-green-400';
  if (avgScore.value >= 60) return 'text-orange-400';
  return 'text-red-400';
});

const latestScoreColor = computed<string>(() => {
  if (latestScore.value >= 80) return 'text-green-400';
  if (latestScore.value >= 60) return 'text-orange-400';
  return 'text-red-400';
});

const reportData = computed<ReportData | null>(() => {
  if (!detailReport.value?.aiContent) return null;
  try {
    return JSON.parse(detailReport.value.aiContent) as ReportData;
  } catch {
    return null;
  }
});

const rawContent = computed(() => {
  if (!detailReport.value?.aiContent) return '';
  try {
    JSON.parse(detailReport.value.aiContent);
    return '';
  } catch {
    return sanitizeHtml(detailReport.value.aiContent.replace(/\n/g, '<br>'));
  }
});

function getFilterParams(): { reportType?: string; startDate?: string; endDate?: string } {
  const params: { reportType?: string; startDate?: string; endDate?: string } = {};
  if (filterType.value) {
    params.reportType = filterType.value;
  }
  if (filterDateRange.value) {
    const [start, end] = filterDateRange.value;
    params.startDate = new Date(start).toISOString().substring(0, 10);
    params.endDate = new Date(end).toISOString().substring(0, 10);
  }
  return params;
}

async function loadReports(): Promise<void> {
  pageLoading.value = true;
  try {
    const filters = getFilterParams();
    const { data, error } = await fetchGetReportList(currentPage.value, pageSize.value, filters);
    if (error || !data) {
      reports.value = [];
      total.value = 0;
      return;
    }
    reports.value = (data.records || []) as ReportRecord[];
    total.value = data.total || 0;
  } finally {
    pageLoading.value = false;
  }
}

function handleFilterChange(): void {
  currentPage.value = 1;
  loadReports();
}

function clearFilters(): void {
  filterType.value = null;
  filterDateRange.value = null;
  currentPage.value = 1;
  loadReports();
}

function handlePageSizeChange(newSize: number): void {
  pageSize.value = newSize;
  currentPage.value = 1;
  loadReports();
}

async function handleGenerate(type: string): Promise<void> {
  generating.value = true;
  try {
    const { data, error } = await fetchGenerateReport(type);
    if (error) {
      window.$message?.error('报告生成失败，请稍后重试');
      return;
    }
    window.$message?.success('报告生成成功');
    startCooldown();
    // Reload to get the new report in the list
    await loadReports();
  } finally {
    generating.value = false;
  }
}

async function viewReport(report: ReportRecord): Promise<void> {
  detailReport.value = report;
  dialogVisible.value = true;
  if (report.isRead === 0) {
    try {
      await fetchGetReportDetail(report.id);
      report.isRead = 1;
    } catch { /* ignore */ }
  }
}

function getPreview(content: string): string {
  if (!content) return '';
  try {
    const data = JSON.parse(content);
    return data.summary || content.substring(0, 100);
  } catch {
    return content.substring(0, 100);
  }
}

function formatTime(time: string): string {
  if (!time) return '';
  return time.substring(0, 10);
}

function getScoreTagType(score: number): 'success' | 'warning' | 'error' | 'info' {
  if (score >= 80) return 'success';
  if (score >= 60) return 'warning';
  return 'error';
}

onMounted(loadReports);

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer);
});
</script>
