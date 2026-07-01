<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import {
  NCard,
  NGrid,
  NGridItem,
  NStatistic,
  NIcon,
  NSpace,
  NProgress,
  NDataTable,
  NTag,
  NButton
} from 'naive-ui';
import { Icon } from '@iconify/vue';
import echarts from '@/utils/echarts';

defineOptions({ name: 'AdminDashboard' });

const loading = ref(true);

// 统计数据
const stats = ref([
  { label: '总用户数', value: '12,586', icon: 'mdi:account-group', color: '#FF6B35', trend: '+12.5%' },
  { label: '今日活跃', value: '3,241', icon: 'mdi:run', color: '#4ECDC4', trend: '+8.3%' },
  { label: 'AI对话次数', value: '45,892', icon: 'mdi:robot', color: '#FFB800', trend: '+23.1%' },
  { label: '健康计划生成', value: '1,256', icon: 'mdi:calendar-check', color: '#d03050', trend: '+15.7%' }
]);

// 图表引用
const userChartRef = ref<HTMLElement | null>(null);
const aiChartRef = ref<HTMLElement | null>(null);
const healthChartRef = ref<HTMLElement | null>(null);

let userChart: echarts.ECharts | null = null;
let aiChart: echarts.ECharts | null = null;
let healthChart: echarts.ECharts | null = null;

// 最近用户表格数据
const recentUsers = ref([
  { id: 1, name: '张三', registerTime: '2026-06-24 10:23', status: 'active', planCount: 3 },
  { id: 2, name: '李四', registerTime: '2026-06-24 09:15', status: 'active', planCount: 1 },
  { id: 3, name: '王五', registerTime: '2026-06-24 08:47', status: 'inactive', planCount: 0 },
  { id: 4, name: '赵六', registerTime: '2026-06-23 22:30', status: 'active', planCount: 5 },
  { id: 5, name: '钱七', registerTime: '2026-06-23 20:12', status: 'active', planCount: 2 }
]);

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '用户名', key: 'name', width: 120 },
  { title: '注册时间', key: 'registerTime', width: 180 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row: any) {
      return row.status === 'active'
        ? h(NTag, { type: 'success', size: 'small' }, { default: () => '活跃' })
        : h(NTag, { type: 'default', size: 'small' }, { default: () => '未激活' });
    }
  },
  { title: '健康计划数', key: 'planCount', width: 120 }
];

function h(type: any, props?: any, children?: any) {
  return { type, props, children };
}

function initCharts() {
  // 用户增长趋势图
  if (userChartRef.value) {
    userChart = echarts.init(userChartRef.value);
    userChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['新增用户', '活跃用户'], right: 20 },
      grid: { left: 50, right: 20, top: 40, bottom: 30 },
      xAxis: {
        type: 'category',
        data: ['6/18', '6/19', '6/20', '6/21', '6/22', '6/23', '6/24']
      },
      yAxis: { type: 'value' },
      series: [
        {
          name: '新增用户',
          type: 'bar',
          data: [120, 132, 101, 134, 90, 230, 210],
          itemStyle: { color: '#FF6B35' },
          barWidth: 20
        },
        {
          name: '活跃用户',
          type: 'line',
          smooth: true,
          data: [820, 932, 901, 934, 1290, 1330, 1520],
          lineStyle: { color: '#4ECDC4', width: 2 },
          itemStyle: { color: '#4ECDC4' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(78,205,196,0.3)' },
                { offset: 1, color: 'rgba(78,205,196,0.05)' }
              ]
            }
          }
        }
      ]
    });
  }

  // AI使用统计
  if (aiChartRef.value) {
    aiChart = echarts.init(aiChartRef.value);
    aiChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', left: 'left' },
      series: [
        {
          name: 'AI功能使用',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: false },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: 'bold' }
          },
          labelLine: { show: false },
          data: [
            { value: 1048, name: '健康咨询', itemStyle: { color: '#FF6B35' } },
            { value: 735, name: '饮食建议', itemStyle: { color: '#4ECDC4' } },
            { value: 580, name: '运动指导', itemStyle: { color: '#FFB800' } },
            { value: 484, name: '计划生成', itemStyle: { color: '#d03050' } },
            { value: 300, name: '其他', itemStyle: { color: '#909399' } }
          ]
        }
      ]
    });
  }

  // 健康数据分布
  if (healthChartRef.value) {
    healthChart = echarts.init(healthChartRef.value);
    healthChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['体重记录', '饮食记录', '运动记录'], right: 20 },
      grid: { left: 50, right: 20, top: 40, bottom: 30 },
      xAxis: {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      },
      yAxis: { type: 'value' },
      series: [
        {
          name: '体重记录',
          type: 'line',
          smooth: true,
          data: [120, 132, 101, 134, 90, 230, 210],
          lineStyle: { color: '#FF6B35', width: 2 },
          itemStyle: { color: '#FF6B35' }
        },
        {
          name: '饮食记录',
          type: 'line',
          smooth: true,
          data: [220, 182, 191, 234, 290, 330, 310],
          lineStyle: { color: '#FFB800', width: 2 },
          itemStyle: { color: '#FFB800' }
        },
        {
          name: '运动记录',
          type: 'line',
          smooth: true,
          data: [150, 232, 201, 154, 190, 330, 410],
          lineStyle: { color: '#4ECDC4', width: 2 },
          itemStyle: { color: '#4ECDC4' }
        }
      ]
    });
  }
}

function handleResize() {
  userChart?.resize();
  aiChart?.resize();
  healthChart?.resize();
}

onMounted(() => {
  setTimeout(() => {
    loading.value = false;
    initCharts();
  }, 500);

  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  userChart?.dispose();
  aiChart?.dispose();
  healthChart?.dispose();
});
</script>

<template>
  <div class="admin-dashboard">
    <!-- 统计卡片 -->
    <NGrid :cols="4" :x-gap="20" :y-gap="20" class="stats-grid">
      <NGridItem v-for="item in stats" :key="item.label">
        <NCard hoverable class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <NStatistic :value="item.value" :label="item.label">
                <template #suffix>
                  <span class="trend" :style="{ color: item.color }">
                    <NIcon size="14">
                      <Icon icon="mdi:trending-up" />
                    </NIcon>
                    {{ item.trend }}
                  </span>
                </template>
              </NStatistic>
            </div>
            <div class="stat-icon" :style="{ background: item.color + '15', color: item.color }">
              <NIcon size="32">
                <Icon :icon="item.icon" />
              </NIcon>
            </div>
          </div>
        </NCard>
      </NGridItem>
    </NGrid>

    <!-- 图表区域 -->
    <NGrid :cols="2" :x-gap="20" :y-gap="20" class="charts-grid" style="margin-top: 20px;">
      <NGridItem>
        <NCard title="用户增长趋势" class="chart-card">
          <div ref="userChartRef" class="chart-container"></div>
        </NCard>
      </NGridItem>
      <NGridItem>
        <NCard title="AI功能使用分布" class="chart-card">
          <div ref="aiChartRef" class="chart-container"></div>
        </NCard>
      </NGridItem>
    </NGrid>

    <!-- 健康数据 + 最近用户 -->
    <NGrid :cols="3" :x-gap="20" :y-gap="20" class="bottom-grid" style="margin-top: 20px;">
      <NGridItem :span="2">
        <NCard title="本周健康数据记录趋势" class="chart-card">
          <div ref="healthChartRef" class="chart-container"></div>
        </NCard>
      </NGridItem>
      <NGridItem>
        <NCard title="最近注册用户" class="recent-users-card">
          <template #header-extra>
            <NButton text type="primary" size="small">查看全部</NButton>
          </template>
          <NDataTable
            :columns="columns"
            :data="recentUsers"
            :bordered="false"
            size="small"
            :pagination="false"
            :scroll-x="1100"
          />
        </NCard>
      </NGridItem>
    </NGrid>
  </div>
</template>

<style scoped lang="scss">
.admin-dashboard {
  .stats-grid {
    .stat-card {
      border-radius: 12px;
      border: none;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

      :deep(.n-card__content) {
        padding: 20px;
      }
    }

    .stat-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .stat-info {
      .trend {
        font-size: 12px;
        font-weight: 500;
        display: inline-flex;
        align-items: center;
        gap: 2px;
        margin-left: 8px;
      }
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .charts-grid {
    .chart-card {
      border-radius: 12px;
      border: none;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

      :deep(.n-card__content) {
        padding: 16px 20px 20px;
      }
    }

    .chart-container {
      width: 100%;
      height: 300px;
    }
  }

  .bottom-grid {
    .chart-card {
      border-radius: 12px;
      border: none;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

      :deep(.n-card__content) {
        padding: 16px 20px 20px;
      }
    }

    .chart-container {
      width: 100%;
      height: 280px;
    }

    .recent-users-card {
      border-radius: 12px;
      border: none;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

      :deep(.n-card__content) {
        padding: 12px 16px 16px;
      }
    }
  }
}

@media (max-width: 1024px) {
  .admin-dashboard {
    .stats-grid {
      :deep(.n-grid-item) {
        width: 50% !important;
        max-width: 50% !important;
      }
    }

    .charts-grid {
      :deep(.n-grid-item) {
        width: 100% !important;
        max-width: 100% !important;
      }
    }

    .bottom-grid {
      :deep(.n-grid-item) {
        width: 100% !important;
        max-width: 100% !important;
      }
    }
  }
}

@media (max-width: 640px) {
  .admin-dashboard {
    .stats-grid {
      :deep(.n-grid-item) {
        width: 100% !important;
        max-width: 100% !important;
      }
    }
  }
}
</style>
