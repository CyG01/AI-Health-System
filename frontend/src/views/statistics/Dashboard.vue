<template>
  <div class="dashboard-page">
    <NSpin :show="dataLoading">
      <!-- ========== Greeting Card ========== -->
      <NCard v-if="greetingCard" class="greeting-card" :class="`card-${greetingCard.type || 'default'}`">
        <div class="greeting-content">
          <div class="greeting-header">
            <span class="greeting-emoji">{{ greetingCard.icon }}</span>
            <span class="greeting-time">{{ greetingCard.greeting }}</span>
            <NTag v-if="greetingCard.type === 'reminder'" type="error" size="small" round>
              {{ $t('page.dashboard.notCheckedIn') || '尚未打卡' }}
            </NTag>
            <NTag v-if="greetingCard.type === 'celebration'" type="success" size="small" round>
              {{ $t('page.dashboard.isCheckedIn') || '已打卡' }}
            </NTag>
          </div>
          <h3 class="greeting-message">{{ greetingCard.message }}</h3>
          <p v-if="greetingCard.detail" class="greeting-detail">{{ greetingCard.detail }}</p>
          <div v-if="(greetingCard.actions as unknown[])?.length" class="greeting-actions">
            <NButton
              v-for="action in (greetingCard.actions as Array<Record<string, unknown>>)"
              :key="action.label as string"
              :type="action.primary ? 'primary' : 'default'"
              size="small"
              @click="handleGreetingAction(action)"
            >
              {{ action.label }}
            </NButton>
          </div>
        </div>
        <div v-if="greetingCard.progress != null" class="greeting-progress">
          <NProgress
            type="line"
            :percentage="Number(greetingCard.progress)"
            :show-indicator="false"
            :color="progressColor"
          />
          <span class="progress-label">{{ greetingCard.progress }}%</span>
        </div>
      </NCard>

      <!-- ========== Health Overview Stats Grid ========== -->
      <NGrid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" item-responsive class="stats-grid">
        <NGi span="4 m:2 l:1">
          <NCard class="stat-card" hoverable>
            <NStatistic :label="$t('page.dashboard.weight') || '体重'">
              <span class="stat-value">{{ latestHealth.weight ?? '--' }}</span>
              <template #suffix>{{ $t('page.dashboard.kg') || 'kg' }}</template>
            </NStatistic>
          </NCard>
        </NGi>
        <NGi span="4 m:2 l:1">
          <NCard class="stat-card" hoverable>
            <NStatistic :label="$t('page.dashboard.bmi') || 'BMI'">
              <span class="stat-value">{{ latestHealth.bmi ?? '--' }}</span>
            </NStatistic>
          </NCard>
        </NGi>
        <NGi span="4 m:2 l:1">
          <NCard class="stat-card" hoverable>
            <NStatistic :label="$t('page.dashboard.exerciseCaloriesBurned') || '今日运动消耗'">
              <span class="stat-value">{{ today.exerciseCaloriesBurned ?? '--' }}</span>
              <template #suffix>{{ $t('page.dashboard.kcal') || 'kcal' }}</template>
            </NStatistic>
          </NCard>
        </NGi>
        <NGi span="4 m:2 l:1">
          <NCard class="stat-card" hoverable>
            <NStatistic :label="$t('page.dashboard.dietCaloriesConsumed') || '今日饮食热量'">
              <span class="stat-value">{{ today.dietCaloriesConsumed ?? '--' }}</span>
              <template #suffix>{{ $t('page.dashboard.kcal') || 'kcal' }}</template>
            </NStatistic>
          </NCard>
        </NGi>
      </NGrid>

      <!-- ========== Progress Summary Bar ========== -->
      <NCard :bordered="false" class="progress-bar-card">
        <div class="flex flex-wrap items-center justify-center gap-6">
          <div class="flex flex-col items-center gap-1">
            <span class="text-2xl font-bold font-tabular text-[#3fb950]">{{ statsProgress.totalCheckinRate }}%</span>
            <span class="text-xs text-secondary">{{ $t('stats.totalCheckinRate') || '总完成率' }}</span>
          </div>
          <div class="h-9 w-px bg-[#30363d]" />
          <div class="flex flex-col items-center gap-1">
            <span class="text-2xl font-bold font-tabular text-[#58a6ff]">{{ statsProgress.exerciseCompleteRate }}%</span>
            <span class="text-xs text-secondary">{{ $t('stats.exerciseCompleteRate') || '运动完成率' }}</span>
          </div>
          <div class="h-9 w-px bg-[#30363d]" />
          <div class="flex flex-col items-center gap-1">
            <span class="text-2xl font-bold font-tabular text-[#d29922]">{{ statsProgress.dietCompleteRate }}%</span>
            <span class="text-xs text-secondary">{{ $t('stats.dietCompleteRate') || '饮食完成率' }}</span>
          </div>
          <div class="h-9 w-px bg-[#30363d]" />
          <div class="flex flex-col items-center gap-1">
            <span class="text-2xl font-bold font-tabular" :class="weightChangeClass">{{ weightChangeDisplay }}</span>
            <span class="text-xs text-secondary">{{ $t('stats.weightChange') || '体重变化(近30天)' }}</span>
          </div>
          <template v-if="statsProgress.goal">
            <div class="h-9 w-px bg-[#30363d]" />
            <div class="flex flex-col items-center gap-1">
              <span class="text-2xl font-bold font-tabular">{{ statsProgress.goal }}</span>
              <span class="text-xs text-secondary">{{ $t('stats.currentGoal') || '当前目标' }}</span>
            </div>
          </template>
          <div class="h-9 w-px bg-[#30363d]" />
          <NDropdown :options="exportOptions" @select="handleExport">
            <NButton size="small">{{ $t('stats.exportData') || '导出数据' }} &#9662;</NButton>
          </NDropdown>
        </div>
      </NCard>

      <!-- ========== Tab Section (Today / Week / Month) ========== -->
      <NTabs v-model:value="activeTab" type="line" animated @update:value="handleTabChange">
        <!-- TODAY TAB -->
        <NTabPane name="today" :tab="$t('page.dashboard.today') || '今日'">
          <!-- Checkin Status -->
          <NCard v-if="today.isCheckedIn !== undefined" class="section-card">
            <div class="checkin-row">
              <NTag :type="today.isCheckedIn ? 'success' : 'warning'" size="large" round>
                {{ today.isCheckedIn ? ($t('page.dashboard.isCheckedIn') || '今日已打卡') : ($t('page.dashboard.notCheckedIn') || '今日尚未打卡') }}
              </NTag>
              <NTag v-if="today.streakDays" type="success" round>
                {{ $t('page.dashboard.streakDays', { days: today.streakDays }) || `连续打卡 ${today.streakDays} 天` }}
              </NTag>
            </div>
          </NCard>

          <!-- Plan Progress -->
          <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive v-if="today.planName || today.totalTasks">
            <NGi v-if="today.planName" span="2 m:1">
              <NCard class="section-card">
                <div class="plan-progress">
                  <div class="plan-header">
                    <strong>{{ today.planName as string }}</strong>
                  </div>
                  <NProgress
                    type="line"
                    :percentage="today.totalTasks ? Math.round(((today.completedTasks as number) || 0) / (today.totalTasks as number) * 100) : 0"
                    :color="progressColor"
                    :rail-color="'rgba(0,0,0,0.08)'"
                  />
                  <div class="plan-hint">
                    {{ $t('page.dashboard.completedTasks', { completed: today.completedTasks ?? 0, total: today.totalTasks ?? 0 }) || `已完成 ${today.completedTasks ?? 0}/${today.totalTasks ?? 0} 项任务` }}
                  </div>
                </div>
              </NCard>
            </NGi>
            <NGi span="2 m:1">
              <NCard class="section-card">
                <NGrid :cols="2" :x-gap="16">
                  <NGi>
                    <NStatistic :label="$t('page.dashboard.exerciseRecords') || '运动记录'" :value="(today.exerciseRecordsCount as number) ?? 0">
                      <template #suffix>{{ $t('page.dashboard.items') || '项' }}</template>
                    </NStatistic>
                  </NGi>
                  <NGi>
                    <NStatistic :label="$t('page.dashboard.dietRecords') || '饮食记录'" :value="(today.dietRecordsCount as number) ?? 0">
                      <template #suffix>{{ $t('page.dashboard.items') || '项' }}</template>
                    </NStatistic>
                  </NGi>
                </NGrid>
              </NCard>
            </NGi>
          </NGrid>

          <!-- Today Tasks Table -->
          <NCard v-if="(today.tasks as unknown[])?.length" :title="$t('page.dashboard.todayTasks') || '今日任务'" class="section-card">
            <NDataTable
              :data="(today.tasks as Array<Record<string, unknown>>)"
              :columns="todayTaskColumns"
              size="small"
              striped
            />
          </NCard>

          <!-- Today Charts -->
          <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
            <NGi span="2 m:1">
              <NCard :title="`${$t('page.dashboard.weightTrend') || '体重趋势'} (${$t('page.dashboard.recent30Days') || '近30天'})`" class="chart-card">
                <div ref="todayWeightChartRef" class="chart-container" />
              </NCard>
            </NGi>
            <NGi span="2 m:1">
              <NCard :title="`${$t('page.dashboard.checkinChart') || '打卡统计'} (${$t('page.dashboard.recent30Days') || '近30天'})`" class="chart-card">
                <div ref="todayCheckinChartRef" class="chart-container" />
              </NCard>
            </NGi>
          </NGrid>
        </NTabPane>

        <!-- WEEK TAB -->
        <NTabPane name="week" :tab="$t('page.dashboard.week') || '本周'">
          <template v-if="weekData">
            <NGrid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.checkinDaysWeek') || '本周打卡'">
                    {{ weekData.checkinDays ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.days') || '天' }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.exerciseCaloriesWeek') || '运动消耗'">
                    {{ weekData.exerciseCalories ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.kcal') || 'kcal' }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.dietCaloriesWeek') || '饮食摄入'">
                    {{ weekData.dietCalories ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.kcal') || 'kcal' }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.recordsCount') || '运动/饮食记录'">
                    {{ weekData.exerciseRecordsCount ?? 0 }}/{{ weekData.dietRecordsCount ?? 0 }}
                  </NStatistic>
                </NCard>
              </NGi>
            </NGrid>

            <!-- Daily Detail Table -->
            <NCard v-if="weekData.dailySummary" :title="$t('page.dashboard.dailyDetail') || '每日明细'" class="section-card">
              <NDataTable
                :data="(weekData.dailySummary as Array<Record<string, unknown>>)"
                :columns="weekDailyColumns"
                size="small"
                striped
              />
            </NCard>

            <!-- Diet Comparison Chart -->
            <NCard :title="$t('page.dashboard.dietComparisonWeek') || '饮食热量周对比'" class="chart-card">
              <template #header-extra>
                <span v-if="dietComparison" class="text-xs text-[#8b949e]">
                  {{ (dietComparison as unknown as Record<string, unknown>).currentTotalCalories }}kcal vs {{ (dietComparison as unknown as Record<string, unknown>).previousTotalCalories }}kcal
                  <NTag :type="((dietComparison as unknown as Record<string, unknown>).calorieChangePercent as number) > 0 ? 'warning' : 'success'" size="small" style="margin-left:8px">
                    {{ ((dietComparison as unknown as Record<string, unknown>).calorieChangePercent as number) > 0 ? '+' : '' }}{{ (dietComparison as unknown as Record<string, unknown>).calorieChangePercent }}%
                  </NTag>
                </span>
              </template>
              <div ref="dietCompChartRef" class="chart-container" />
            </NCard>
          </template>
        </NTabPane>

        <!-- MONTH TAB -->
        <NTabPane name="month" :tab="$t('page.dashboard.month') || '本月'">
          <template v-if="monthData">
            <NGrid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.checkinDaysMonth') || '本月打卡'">
                    {{ monthData.checkinDays ?? 0 }} / {{ monthData.totalDays ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.days') || '天' }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.checkinRate') || '打卡率'">
                    {{ monthData.checkinRate ?? 0 }}
                    <template #suffix>%</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.monthlyExerciseCalories') || '运动消耗'">
                    {{ monthData.exerciseCalories ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.kcal') || 'kcal' }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.monthlyDietCalories') || '饮食摄入'">
                    {{ monthData.dietCalories ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.kcal') || 'kcal' }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
            </NGrid>

            <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
              <NGi span="2 m:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.monthlyExerciseRecords') || '运动记录'">
                    {{ monthData.exerciseRecordsCount ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.items') || '项' }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="2 m:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.monthlyDietRecords') || '饮食记录'">
                    {{ monthData.dietRecordsCount ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.items') || '项' }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
            </NGrid>

            <!-- Weekly Summary Table -->
            <NCard v-if="(monthData.weeklySummary as unknown[])?.length" :title="$t('page.dashboard.weeklySummary') || '按周汇总'" class="section-card">
              <NDataTable
                :data="(monthData.weeklySummary as Array<Record<string, unknown>>)"
                :columns="monthWeeklyColumns"
                size="small"
                striped
              />
            </NCard>
          </template>
        </NTabPane>
      </NTabs>

      <!-- ========== Statistics Charts Section ========== -->
      <NCard :bordered="false" class="stats-section-card">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="text-[15px] font-semibold">{{ $t('stats.detailedStats') || '详细数据统计' }}</span>
            <NRadioGroup v-model:value="chartDays" size="small" @update:value="loadStatisticsCharts">
              <NRadioButton :value="7">7天</NRadioButton>
              <NRadioButton :value="30">30天</NRadioButton>
              <NRadioButton :value="90">90天</NRadioButton>
            </NRadioGroup>
          </div>
        </template>

        <div class="stats-charts-grid">
          <!-- Weight Trend -->
          <div class="stats-chart-item full-width">
            <NCard :bordered="false" :title="$t('stats.weightTrend') || '体重变化趋势'">
              <div v-if="!statErrors.weight && !statEmpty.weight" ref="weightChartRef" class="min-h-[220px] w-full" />
              <NEmpty v-else-if="statEmpty.weight" description="暂无体重数据" size="small" />
              <div v-else class="flex min-h-[220px] items-center justify-center gap-3 text-[#f85149]">
                <span>加载失败</span><NButton size="small" @click="loadStatisticsCharts">重试</NButton>
              </div>
            </NCard>
          </div>

          <!-- BMI Trend -->
          <div class="stats-chart-item full-width">
            <NCard :bordered="false" title="BMI 变化趋势">
              <div v-if="!statErrors.bmi && !statEmpty.bmi" ref="bmiChartRef" class="min-h-[220px] w-full" />
              <NEmpty v-else-if="statEmpty.bmi" description="暂无BMI数据" size="small" />
              <div v-else class="flex min-h-[220px] items-center justify-center gap-3 text-[#f85149]">
                <span>加载失败</span><NButton size="small" @click="loadStatisticsCharts">重试</NButton>
              </div>
            </NCard>
          </div>

          <!-- Checkin Trend -->
          <div class="stats-chart-item half-width">
            <NCard :bordered="false" title="打卡完成率">
              <div v-if="!statErrors.checkin && !statEmpty.checkin" ref="checkinChartRef" class="min-h-[220px] w-full" />
              <NEmpty v-else-if="statEmpty.checkin" description="暂无打卡数据" size="small" />
              <div v-else class="flex min-h-[220px] items-center justify-center gap-3 text-[#f85149]">
                <span>加载失败</span><NButton size="small" @click="loadStatisticsCharts">重试</NButton>
              </div>
            </NCard>
          </div>

          <!-- Exercise Trend -->
          <div class="stats-chart-item half-width">
            <NCard :bordered="false" title="每日运动时长">
              <div v-if="!statErrors.exercise && !statEmpty.exercise" ref="exerciseChartRef" class="min-h-[220px] w-full" />
              <NEmpty v-else-if="statEmpty.exercise" description="暂无运动数据" size="small" />
              <div v-else class="flex min-h-[220px] items-center justify-center gap-3 text-[#f85149]">
                <span>加载失败</span><NButton size="small" @click="loadStatisticsCharts">重试</NButton>
              </div>
            </NCard>
          </div>

          <!-- Calorie Trend -->
          <div class="stats-chart-item full-width">
            <NCard :bordered="false" title="每日推荐热量摄入">
              <div v-if="!statErrors.calorie && !statEmpty.calorie" ref="calorieChartRef" class="min-h-[220px] w-full" />
              <NEmpty v-else-if="statEmpty.calorie" description="暂无热量数据" size="small" />
              <div v-else class="flex min-h-[220px] items-center justify-center gap-3 text-[#f85149]">
                <span>加载失败</span><NButton size="small" @click="loadStatisticsCharts">重试</NButton>
              </div>
            </NCard>
          </div>

          <!-- Calorie Deficit -->
          <div class="stats-chart-item full-width">
            <NCard :bordered="false" title="热量缺口分析（摄入 vs 消耗）">
              <div v-if="!statErrors.deficit && !statEmpty.deficit" ref="deficitChartRef" class="min-h-[220px] w-full" />
              <NEmpty v-else-if="statEmpty.deficit" description="暂无热量缺口数据" size="small" />
              <div v-else class="flex min-h-[220px] items-center justify-center gap-3 text-[#f85149]">
                <span>加载失败</span><NButton size="small" @click="loadStatisticsCharts">重试</NButton>
              </div>
            </NCard>
          </div>

          <!-- Nutrient Ratio -->
          <div class="stats-chart-item half-width">
            <NCard :bordered="false" :title="`营养素占比（近${chartDays}天）`">
              <div v-if="!statErrors.nutrient && !statEmpty.nutrient" ref="nutrientChartRef" class="min-h-[220px] w-full" />
              <NEmpty v-else-if="statEmpty.nutrient" description="暂无营养数据" size="small" />
              <div v-else class="flex min-h-[220px] items-center justify-center gap-3 text-[#f85149]">
                <span>加载失败</span><NButton size="small" @click="loadStatisticsCharts">重试</NButton>
              </div>
            </NCard>
          </div>

          <!-- Exercise Distribution -->
          <div class="stats-chart-item half-width">
            <NCard :bordered="false" :title="`运动类型分布（近${chartDays}天）`">
              <div v-if="!statErrors.exDist && !statEmpty.exDist" ref="exDistChartRef" class="min-h-[220px] w-full" />
              <NEmpty v-else-if="statEmpty.exDist" description="暂无运动分布数据" size="small" />
              <div v-else class="flex min-h-[220px] items-center justify-center gap-3 text-[#f85149]">
                <span>加载失败</span><NButton size="small" @click="loadStatisticsCharts">重试</NButton>
              </div>
            </NCard>
          </div>
        </div>
      </NCard>

      <!-- ========== Error Alert ========== -->
      <NAlert v-if="tabError" type="error" closable class="error-alert">
        <div class="flex items-center gap-2">
          <span>{{ tabError }}</span>
          <NButton size="small" type="primary" quaternary @click="handleTabChange(activeTab)">
            {{ $t('page.dashboard.retry') || '重试' }}
          </NButton>
        </div>
      </NAlert>

      <!-- ========== Bottom Section: Progress + Assessment ========== -->
      <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive class="bottom-section">
        <!-- Health Goal Progress -->
        <NGi span="2 m:1">
          <NCard :title="$t('page.dashboard.healthGoalProgress') || '健康目标进度'" class="section-card">
            <template v-if="onProgress">
              <div class="progress-content">
                <div class="progress-item">
                  <span>{{ $t('page.dashboard.goalRate') || '目标进度' }}</span>
                  <NProgress type="line" :percentage="progressPercent" :color="progressColor" :rail-color="'rgba(0,0,0,0.08)'" />
                </div>
                <div class="progress-detail">
                  <div class="detail-item">
                    {{ $t('page.dashboard.checkinRateLabel') || '打卡率' }} <strong>{{ onProgress.checkinRate ?? '--' }}%</strong>
                  </div>
                  <div class="detail-item">
                    {{ $t('page.dashboard.exerciseRate') || '运动完成率' }} <strong>{{ onProgress.exerciseRate ?? '--' }}%</strong>
                  </div>
                  <div class="detail-item">
                    {{ $t('page.dashboard.dietRate') || '饮食完成率' }} <strong>{{ onProgress.dietRate ?? '--' }}%</strong>
                  </div>
                  <div class="detail-item">
                    {{ $t('page.dashboard.weightChange') || '体重变化' }} <strong>{{ onProgress.weightChange ?? '--' }} kg</strong>
                  </div>
                </div>
              </div>
            </template>
            <NEmpty v-else :description="$t('page.dashboard.noProgressData') || '暂无数据'" />
          </NCard>
        </NGi>

        <!-- Health Assessment -->
        <NGi span="2 m:1">
          <NCard :title="$t('page.dashboard.healthAssessment') || '健康评估'" class="section-card">
            <template v-if="assessment">
              <div class="assessment-tags">
                <NTag :type="assessment.bmiLevel === '正常' ? 'success' : 'warning'" round>
                  {{ $t('page.dashboard.bmiLevel') || 'BMI' }}: {{ assessment.bmiLevel ?? '--' }}
                </NTag>
                <NTag type="info" round>
                  {{ $t('page.dashboard.healthScore') || '健康评分' }}: {{ assessment.healthScore ?? '--' }} {{ $t('page.dashboard.score') || '分' }}
                </NTag>
                <NTag v-if="(assessment.risks as string[])?.length" type="error" round>
                  {{ $t('page.dashboard.risk') || '风险' }}: {{ (assessment.risks as string[])[0] }}
                </NTag>
              </div>
            </template>
            <NEmpty v-else :description="$t('page.dashboard.noHealthData') || '请先完善健康档案'" />
          </NCard>
        </NGi>
      </NGrid>

      <!-- ========== AI Recommendations ========== -->
      <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive class="bottom-section">
        <!-- AI Exercise -->
        <NGi span="2 m:1">
          <NCard :title="$t('page.dashboard.aiExercise') || 'AI推荐运动'" class="section-card">
            <div v-if="(recommends.exercises as unknown[])?.length" class="recommend-list">
              <div
                v-for="ex in (recommends.exercises as Array<Record<string, unknown>>).slice(0, 4)"
                :key="(ex.id as string)"
                class="recommend-item"
              >
                <NTag size="small" :type="ex.type === '有氧' ? 'success' : 'warning'" round>
                  {{ ex.type || ex.reason || '推荐' }}
                </NTag>
                <span class="item-name">{{ ex.name }}</span>
                <span v-if="ex.caloriePerHour" class="item-meta">~{{ ex.caloriePerHour }}kcal/h</span>
                <span v-if="ex.score" class="item-meta">{{ $t('page.dashboard.matchScore', { score: ex.score }) || `匹配度 ${ex.score}%` }}</span>
                <NTag v-if="ex.targetMuscle" size="small" round>{{ ex.targetMuscle }}</NTag>
              </div>
            </div>
            <NEmpty v-else :description="$t('page.dashboard.noRecommend') || '暂无推荐'" size="small" />
          </NCard>
        </NGi>

        <!-- AI Food -->
        <NGi span="2 m:1">
          <NCard :title="$t('page.dashboard.aiFood') || '推荐健康饮食'" class="section-card">
            <div v-if="(recommends.foods as unknown[])?.length" class="recommend-list">
              <div
                v-for="f in (recommends.foods as Array<Record<string, unknown>>).slice(0, 4)"
                :key="(f.id as string)"
                class="recommend-item"
              >
                <NTag size="small" round>{{ f.category || f.reason || '推荐' }}</NTag>
                <span class="item-name">{{ f.name }}</span>
                <span v-if="f.caloriePer100g" class="item-meta">{{ f.caloriePer100g }}kcal/100g</span>
                <span v-if="f.proteinPer100g" class="item-meta">蛋白{{ f.proteinPer100g }}g</span>
                <span v-if="f.score && !f.caloriePer100g" class="item-meta">{{ $t('page.dashboard.matchScore', { score: f.score }) || `匹配度 ${f.score}%` }}</span>
              </div>
            </div>
            <NEmpty v-else :description="$t('page.dashboard.noRecommend') || '暂无推荐'" size="small" />
          </NCard>
        </NGi>
      </NGrid>

      <!-- ========== Health Tips ========== -->
      <NCard v-if="(recommends.tips as string[])?.length || (recommends.healthTips as string[])?.length" :title="$t('page.dashboard.healthTips') || '健康小贴士'" class="section-card">
        <div class="tips-list">
          <div v-for="(tip, i) in ((recommends.tips || recommends.healthTips) as string[])" :key="i" class="tip-item">
            <NTag size="tiny" type="info" round>Tip</NTag>
            <span>{{ tip }}</span>
          </div>
        </div>
      </NCard>

      <!-- ========== AI Suggestions ========== -->
      <NCard v-if="recommends.aiSuggestions" :title="$t('page.dashboard.aiSuggestions') || 'AI个性化建议'" class="section-card ai-suggestion-card">
        <p class="ai-suggestion-text" v-html="formatSuggestions(recommends.aiSuggestions as string)" />
      </NCard>
    </NSpin>
  </div>
</template>

<script setup lang="ts">
import { computed, h, nextTick, onMounted, onBeforeUnmount, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage, NDataTable, NTag } from 'naive-ui';
import { $t } from '@/locales';
import {
  fetchGetLatestHealth,
  fetchGetHealthAssessment,
  fetchDashboardToday,
  fetchDashboardWeek,
  fetchDashboardMonth,
  fetchGreeting,
  fetchGetWeightTrend,
  fetchGetBmiTrend,
  fetchGetCheckinTrend,
  fetchGetExerciseTrend,
  fetchGetCalorieTrend,
  fetchGetProgress,
  fetchGetCalorieDeficit,
  fetchGetNutrientRatio,
  fetchGetExerciseDistribution,
  fetchGetDietTrendComparison,
  fetchGetRecommendations
} from '@/service/api';
import echarts from '@/utils/echarts';
import { sanitizeHtml } from '@/utils/sanitize';

defineOptions({ name: 'StatisticsDashboard' });

const router = useRouter();
const message = useMessage();

// ===================== Dashboard State =====================
const activeTab = ref('today');
const dataLoading = ref(true);
const tabError = ref<string | null>(null);

const latestHealth = ref<Record<string, unknown>>({});
const today = ref<Record<string, unknown>>({});
const weekData = ref<Record<string, unknown> | null>(null);
const monthData = ref<Record<string, unknown> | null>(null);
const assessment = ref<Record<string, unknown> | null>(null);
const onProgress = ref<Record<string, unknown> | null>(null);
const dietComparison = ref<Record<string, unknown> | null>(null);
const recommends = ref<Record<string, unknown>>({});
const greetingCard = ref<Record<string, unknown> | null>(null);

// Today tab chart refs
const todayWeightChartRef = ref<HTMLElement | null>(null);
const todayCheckinChartRef = ref<HTMLElement | null>(null);
const dietCompChartRef = ref<HTMLElement | null>(null);

let todayWeightChart: echarts.ECharts | null = null;
let todayCheckinChart: echarts.ECharts | null = null;
let dietCompChart: echarts.ECharts | null = null;

// ===================== Statistics Charts State =====================
const chartDays = ref(30);
const CC = { green: '#3fb950', blue: '#58a6ff', amber: '#d29922' };
const AX = { textStyle: { color: '#8b949e' }, axisLine: { lineStyle: { color: '#30363d' } }, splitLine: { lineStyle: { color: 'rgba(48,54,61,0.5)' } } };

const weightChartRef = ref<HTMLDivElement | null>(null);
const bmiChartRef = ref<HTMLDivElement | null>(null);
const checkinChartRef = ref<HTMLDivElement | null>(null);
const exerciseChartRef = ref<HTMLDivElement | null>(null);
const calorieChartRef = ref<HTMLDivElement | null>(null);
const deficitChartRef = ref<HTMLDivElement | null>(null);
const nutrientChartRef = ref<HTMLDivElement | null>(null);
const exDistChartRef = ref<HTMLDivElement | null>(null);

const statsCharts: echarts.ECharts[] = [];

const statErrors = reactive({ weight: false, bmi: false, checkin: false, exercise: false, calorie: false, deficit: false, nutrient: false, exDist: false });
const statEmpty = reactive({ weight: false, bmi: false, checkin: false, exercise: false, calorie: false, deficit: false, nutrient: false, exDist: false });

// ===================== Progress Summary Bar State =====================
const statsProgress = reactive({
  totalCheckinRate: 0,
  exerciseCompleteRate: 0,
  dietCompleteRate: 0,
  weightChange: null as number | null,
  goal: ''
});

const weightChangeDisplay = computed(() => {
  const v = statsProgress.weightChange;
  if (v == null) return '--';
  return v > 0 ? `+${v} kg` : `${v} kg`;
});

const weightChangeClass = computed(() => {
  const v = statsProgress.weightChange;
  if (v == null || v === 0) return '';
  return v > 0 ? 'text-[#f85149]' : 'text-[#3fb950]';
});

const exportOptions = [
  { label: '导出 CSV', key: 'csv' },
  { label: '导出 Excel', key: 'excel' }
];

// ===================== Computed =====================
const progressPercent = computed(() => {
  return onProgress.value?.progressPercent ? Number(onProgress.value.progressPercent) : 0;
});

const progressColor = computed(() => {
  const rate = progressPercent.value;
  if (rate >= 80) return '#18a058';
  if (rate >= 50) return '#2080f0';
  return '#f0a020';
});

// ===================== Table Columns =====================
const todayTaskColumns = [
  { title: $t('page.dashboard.taskName') || '任务名称', key: 'itemName' },
  {
    title: $t('page.dashboard.taskType') || '类型', key: 'itemType', width: 100,
    render: (row: Record<string, unknown>) => h(NTag, { size: 'small', type: row.itemType === 'sport' ? 'success' : 'info' }, () => row.itemType === 'sport' ? ($t('page.dashboard.sportType') || '运动') : ($t('page.dashboard.dietType') || '饮食'))
  },
  { title: $t('page.dashboard.taskTarget') || '目标量', key: 'targetAmount', width: 100 },
  {
    title: $t('page.dashboard.taskStatus') || '状态', key: 'status', width: 100,
    render: (row: Record<string, unknown>) => h(NTag, { size: 'small', type: row.status === 1 ? 'success' : 'default' }, () => row.status === 1 ? ($t('page.dashboard.completed') || '已完成') : ($t('page.dashboard.uncompleted') || '未完成'))
  }
];

const weekDailyColumns = [
  { title: $t('page.dashboard.date') || '日期', key: 'date', width: 100 },
  {
    title: $t('page.dashboard.checkinStatus') || '打卡', key: 'checkedIn', width: 100,
    render: (row: Record<string, unknown>) => h(NTag, { size: 'small', type: row.checkedIn ? 'success' : 'default' }, () => row.checkedIn ? ($t('page.dashboard.checkedIn') || '已打卡') : ($t('page.dashboard.notChecked') || '未打卡'))
  },
  { title: `${$t('page.dashboard.exerciseCaloriesWeek') || '运动消耗'} (${$t('page.dashboard.kcal') || 'kcal'})`, key: 'exerciseCalories' },
  { title: `${$t('page.dashboard.dietCaloriesWeek') || '饮食摄入'} (${$t('page.dashboard.kcal') || 'kcal'})`, key: 'dietCalories' },
  { title: $t('page.dashboard.exerciseCount') || '运动次数', key: 'exerciseCount' },
  { title: $t('page.dashboard.dietCount') || '饮食次数', key: 'dietCount' }
];

const monthWeeklyColumns = [
  { title: $t('page.dashboard.weekLabel') || '周', key: 'weekLabel' },
  { title: $t('page.dashboard.checkinDaysCount') || '打卡天数', key: 'checkinDays' },
  { title: `${$t('page.dashboard.exerciseCaloriesWeek') || '运动消耗'} (${$t('page.dashboard.kcal') || 'kcal'})`, key: 'exerciseCalories' },
  { title: `${$t('page.dashboard.dietCaloriesWeek') || '饮食摄入'} (${$t('page.dashboard.kcal') || 'kcal'})`, key: 'dietCalories' }
];

// ===================== Dashboard Chart Helpers =====================
function initTodayWeightChart(data: Record<string, unknown>) {
  if (!todayWeightChartRef.value) return;
  if (!todayWeightChart) {
    todayWeightChart = echarts.init(todayWeightChartRef.value);
  }
  const dates = (data.xAxis as string[]) || [];
  const weights = (data.yAxis as number[]) || [];
  todayWeightChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value', name: 'kg' },
    series: [{
      data: weights,
      type: 'line',
      smooth: true,
      lineStyle: { color: '#2080f0', width: 2 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(32,128,240,0.3)' },
            { offset: 1, color: 'rgba(32,128,240,0.05)' }
          ]
        }
      },
      itemStyle: { color: '#2080f0' }
    }]
  });
}

function initTodayCheckinChart(data: Record<string, unknown>) {
  if (!todayCheckinChartRef.value) return;
  if (!todayCheckinChart) {
    todayCheckinChart = echarts.init(todayCheckinChartRef.value);
  }
  const dates = (data.xAxis as string[]) || [];
  const counts = (data.completeRate as number[]) || [];
  todayCheckinChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', name: '%', max: 100 },
    series: [{
      data: counts,
      type: 'bar',
      itemStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#18a058' },
            { offset: 1, color: '#b7eb8f' }
          ]
        },
        borderRadius: [4, 4, 0, 0]
      }
    }]
  });
}

function initDietComparisonChart() {
  if (!dietCompChartRef.value || !dietComparison.value) return;
  if (!dietCompChart) {
    dietCompChart = echarts.init(dietCompChartRef.value);
  }
  const data = dietComparison.value;
  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const currentDaily = (data.currentDaily as Array<{ dayLabel: string; calories: number }>) || [];
  const previousDaily = (data.previousDaily as Array<{ dayLabel: string; calories: number }>) || [];
  const xLabels = currentDaily.map(d => d.dayLabel).length ? currentDaily.map(d => d.dayLabel) : days;
  const currentData = currentDaily.map(d => d.calories);
  const previousData = previousDaily.map(d => d.calories);

  dietCompChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: [data.currentPeriodLabel as string, data.previousPeriodLabel as string], textStyle: { color: '#8b949e' } },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: xLabels, axisLabel: { color: '#8b949e' } },
    yAxis: { type: 'value', name: 'kcal', axisLabel: { color: '#8b949e' } },
    series: [
      {
        name: data.currentPeriodLabel as string,
        type: 'line', smooth: true, data: currentData,
        lineStyle: { color: '#2080f0', width: 2 },
        itemStyle: { color: '#2080f0' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(32,128,240,0.3)' },
              { offset: 1, color: 'rgba(32,128,240,0.03)' }
            ]
          }
        }
      },
      {
        name: data.previousPeriodLabel as string,
        type: 'line', smooth: true, data: previousData,
        lineStyle: { color: '#8b949e', width: 2, type: 'dashed' },
        itemStyle: { color: '#8b949e' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(139,148,158,0.2)' },
              { offset: 1, color: 'rgba(139,148,158,0.02)' }
            ]
          }
        }
      }
    ]
  });
}

// ===================== Statistics Chart Builders =====================
function dt() {
  return { backgroundColor: 'rgba(22,27,34,0.95)', borderColor: '#30363d', textStyle: { color: '#c9d1d9', fontSize: 12 } };
}

function initStatChart(el: HTMLDivElement | null): echarts.ECharts | null {
  if (!el) return null;
  const c = echarts.init(el);
  statsCharts.push(c);
  return c;
}

function buildLineChart(el: HTMLDivElement | null, x: string[], y: number[], unit: string, color: string) {
  const c = initStatChart(el);
  if (!c) return;
  c.setOption({
    tooltip: { ...dt(), trigger: 'axis' },
    grid: { top: 12, right: 20, bottom: 28, left: 48 },
    xAxis: { type: 'category', data: x, ...AX },
    yAxis: { type: 'value', name: unit, nameTextStyle: { color: '#8b949e', fontSize: 11 }, ...AX },
    series: [{
      type: 'line', data: y, smooth: true, symbol: 'circle', symbolSize: 4,
      lineStyle: { color, width: 2 }, itemStyle: { color },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: color + '30' },
          { offset: 1, color: color + '05' }
        ])
      },
      connectNulls: true
    }]
  });
}

function buildBarChart(el: HTMLDivElement | null, x: string[], y: number[], unit: string, color: string) {
  const c = initStatChart(el);
  if (!c) return;
  c.setOption({
    tooltip: { ...dt(), trigger: 'axis' },
    grid: { top: 12, right: 20, bottom: 28, left: 48 },
    xAxis: { type: 'category', data: x, ...AX },
    yAxis: { type: 'value', name: unit, nameTextStyle: { color: '#8b949e', fontSize: 11 }, ...AX, min: 0, max: 100 },
    series: [{
      type: 'bar', data: y, barWidth: '60%',
      itemStyle: { color, borderRadius: [4, 4, 0, 0], opacity: 0.85 }
    }]
  });
}

function buildDeficitChart(el: HTMLDivElement | null, x: string[], con: number[], bur: number[], net: number[]) {
  const c = initStatChart(el);
  if (!c) return;
  c.setOption({
    tooltip: { ...dt(), trigger: 'axis' },
    legend: { data: ['摄入', '消耗', '净差值'], textStyle: { color: '#8b949e', fontSize: 11 }, top: 0 },
    grid: { top: 32, right: 20, bottom: 28, left: 48 },
    xAxis: { type: 'category', data: x, ...AX },
    yAxis: { type: 'value', name: 'kcal', ...AX },
    series: [
      { name: '摄入', type: 'bar', data: con, itemStyle: { color: '#f85149', borderRadius: [4, 4, 0, 0] } },
      { name: '消耗', type: 'bar', data: bur, itemStyle: { color: '#58a6ff', borderRadius: [4, 4, 0, 0] } },
      { name: '净差值', type: 'line', data: net, smooth: true, lineStyle: { color: '#3fb950', width: 2 }, itemStyle: { color: '#3fb950' } }
    ]
  });
}

function buildPieChart(el: HTMLDivElement | null, names: string[], values: number[], title: string) {
  const c = initStatChart(el);
  if (!c) return;
  c.setOption({
    tooltip: { ...dt(), trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 8, top: 'center', textStyle: { color: '#8b949e', fontSize: 11 } },
    series: [{
      name: title, type: 'pie', radius: ['40%', '70%'], center: ['35%', '50%'],
      itemStyle: { borderRadius: 4, borderColor: '#0d1117', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: names.map((nm, i) => ({ name: nm, value: values[i] || 0 }))
    }]
  });
}

// ===================== Load Statistics Charts =====================
async function loadStatisticsCharts() {
  // Reset error/empty states
  Object.keys(statErrors).forEach(k => { (statErrors as Record<string, boolean>)[k] = false; });
  Object.keys(statEmpty).forEach(k => { (statEmpty as Record<string, boolean>)[k] = false; });
  statsCharts.forEach(c => c.dispose());
  statsCharts.length = 0;

  const p = { days: chartDays.value };
  const R = await Promise.allSettled([
    fetchGetWeightTrend(p),
    fetchGetBmiTrend(p),
    fetchGetCheckinTrend(p),
    fetchGetExerciseTrend(p),
    fetchGetCalorieTrend(p),
    fetchGetCalorieDeficit(p),
    fetchGetNutrientRatio(p),
    fetchGetExerciseDistribution(p)
  ]);

  const [wR, bR, cR, eR, calR, dR, nR, edR] = R;
  await nextTick();

  // Weight trend
  if (wR.status === 'fulfilled' && wR.value.data) {
    const w = wR.value.data as unknown as Record<string, unknown>;
    if (!((w.xAxis as string[]) || []).length) statEmpty.weight = true;
    else buildLineChart(weightChartRef.value, w.xAxis as string[], (w.yAxis as number[]) || [], '体重 (kg)', CC.green);
  } else statErrors.weight = true;

  // BMI trend
  if (bR.status === 'fulfilled' && bR.value.data) {
    const b = bR.value.data as unknown as Record<string, unknown>;
    if (!((b.xAxis as string[]) || []).length) statEmpty.bmi = true;
    else buildLineChart(bmiChartRef.value, b.xAxis as string[], (b.yAxis as number[]) || [], 'BMI', CC.blue);
  } else statErrors.bmi = true;

  // Checkin trend
  if (cR.status === 'fulfilled' && cR.value.data) {
    const c = cR.value.data as unknown as Record<string, unknown>;
    if (!((c.xAxis as string[]) || []).length) statEmpty.checkin = true;
    else buildBarChart(checkinChartRef.value, c.xAxis as string[], ((c.completeRate as number[]) || []).map((v: number) => v), '完成率 (%)', CC.green);
  } else statErrors.checkin = true;

  // Exercise trend
  if (eR.status === 'fulfilled' && eR.value.data) {
    const e = eR.value.data as unknown as Record<string, unknown>;
    if (!((e.xAxis as string[]) || []).length) statEmpty.exercise = true;
    else buildLineChart(exerciseChartRef.value, e.xAxis as string[], (e.minutesPerDay as number[]) || [], '分钟/天', CC.green);
  } else statErrors.exercise = true;

  // Calorie trend
  if (calR.status === 'fulfilled' && calR.value.data) {
    const cl = calR.value.data as unknown as Record<string, unknown>;
    if (!((cl.xAxis as string[]) || []).length) statEmpty.calorie = true;
    else buildLineChart(calorieChartRef.value, cl.xAxis as string[], (cl.dailyCalories as number[]) || [], '千卡/天', CC.amber);
  } else statErrors.calorie = true;

  // Calorie deficit
  if (dR.status === 'fulfilled' && dR.value.data) {
    const df = dR.value.data as unknown as Record<string, unknown>;
    if (!((df.xAxis as string[]) || []).length) statEmpty.deficit = true;
    else buildDeficitChart(deficitChartRef.value, df.xAxis as string[], (df.consumed as number[]) || [], (df.burned as number[]) || [], (df.net as number[]) || []);
  } else statErrors.deficit = true;

  // Nutrient ratio
  if (nR.status === 'fulfilled' && nR.value.data) {
    const nr = nR.value.data as unknown as Record<string, unknown>;
    if (!((nr.names as string[]) || []).length) statEmpty.nutrient = true;
    else buildPieChart(nutrientChartRef.value, nr.names as string[], (nr.values as number[]) || [], '营养素占比 (g)');
  } else statErrors.nutrient = true;

  // Exercise distribution
  if (edR.status === 'fulfilled' && edR.value.data) {
    const ed = edR.value.data as unknown as Record<string, unknown>;
    if (!((ed.names as string[]) || []).length) statEmpty.exDist = true;
    else buildPieChart(exDistChartRef.value, ed.names as string[], ((ed.values as number[]) || []).map((v: unknown) => Number(v)), '运动次数');
  } else statErrors.exDist = true;
}

// ===================== Tab Change =====================
async function handleTabChange(tabName: string) {
  tabError.value = null;
  await nextTick();
  setTimeout(() => handleResize(), 50);

  if (tabName === 'week' && !weekData.value) {
    try {
      const { data, error } = await fetchDashboardWeek();
      if (data && !error) {
        weekData.value = data as unknown as Record<string, unknown>;
      }
    } catch {
      tabError.value = $t('page.dashboard.dataLoadFailed') || '数据加载失败';
    }
    try {
      const { data, error } = await fetchGetDietTrendComparison();
      if (data && !error) {
        dietComparison.value = data as unknown as Record<string, unknown>;
        await nextTick();
        initDietComparisonChart();
      }
    } catch {
      tabError.value = $t('page.dashboard.dataLoadFailed') || '数据加载失败';
    }
  } else if (tabName === 'month' && !monthData.value) {
    try {
      const { data, error } = await fetchDashboardMonth();
      if (data && !error) {
        monthData.value = data as unknown as Record<string, unknown>;
      }
    } catch {
      tabError.value = $t('page.dashboard.dataLoadFailed') || '数据加载失败';
    }
  }
}

// ===================== Greeting Actions =====================
function handleGreetingAction(action: Record<string, unknown>) {
  if (action.url) {
    router.push(action.url as string);
  } else if (action.path) {
    router.push(action.path as string);
  }
}

// ===================== Export =====================
function handleExport(key: string) {
  const API_BASE = import.meta.env.VITE_API_BASE_URL || '';
  const token = localStorage.getItem('token');
  const link = document.createElement('a');
  link.href = `${API_BASE}/api/export/${key}?token=${encodeURIComponent(token || '')}`;
  link.download = `health-data-${new Date().toISOString().split('T')[0]}.${key === 'excel' ? 'xlsx' : 'csv'}`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  message.success(`正在下载${key.toUpperCase()}文件...`);
}

// ===================== Format Helpers =====================
function formatSuggestions(text: string) {
  if (!text) return '';
  return sanitizeHtml(text.replace(/\n/g, '<br>'));
}

// ===================== Resize =====================
function handleResize() {
  todayWeightChart?.resize();
  todayCheckinChart?.resize();
  dietCompChart?.resize();
  statsCharts.forEach(c => c.resize());
}

// ===================== Lifecycle =====================
onMounted(async () => {
  try {
    const [healthRes, todayRes, assessmentRes, progressRes, weightRes, checkinRes, recommendRes, greetingRes] =
      await Promise.allSettled([
        fetchGetLatestHealth(),
        fetchDashboardToday(),
        fetchGetHealthAssessment(),
        fetchGetProgress(),
        fetchGetWeightTrend({ days: 30 }),
        fetchGetCheckinTrend({ days: 30 }),
        fetchGetRecommendations(),
        fetchGreeting()
      ]);

    // Health record
    if (healthRes.status === 'fulfilled' && healthRes.value.data && !healthRes.value.error) {
      latestHealth.value = healthRes.value.data as unknown as Record<string, unknown>;
    }
    // Today stats
    if (todayRes.status === 'fulfilled' && todayRes.value.data && !todayRes.value.error) {
      today.value = todayRes.value.data as unknown as Record<string, unknown>;
    }
    // Assessment
    if (assessmentRes.status === 'fulfilled' && assessmentRes.value.data && !assessmentRes.value.error) {
      assessment.value = assessmentRes.value.data as unknown as Record<string, unknown>;
    }
    // Progress (used by both dashboard progress section and summary bar)
    if (progressRes.status === 'fulfilled' && progressRes.value.data && !progressRes.value.error) {
      const p = progressRes.value.data as unknown as Record<string, unknown>;
      // Dashboard progress section
      onProgress.value = {
        progressPercent: p.targetProgressPercent ? Number(p.targetProgressPercent) : 0,
        checkinRate: p.totalCheckinRate ? Number(p.totalCheckinRate) : 0,
        exerciseRate: p.exerciseCompleteRate ? Number(p.exerciseCompleteRate) : 0,
        dietRate: p.dietCompleteRate ? Number(p.dietCompleteRate) : 0,
        weightChange: p.weightChange ? Number(p.weightChange) : 0
      };
      // Summary bar
      Object.assign(statsProgress, {
        totalCheckinRate: (p.totalCheckinRate as number) ?? 0,
        exerciseCompleteRate: (p.exerciseCompleteRate as number) ?? 0,
        dietCompleteRate: (p.dietCompleteRate as number) ?? 0,
        weightChange: (p.weightChange as number) ?? null,
        goal: (p.goal as string) || ''
      });
    }

    await nextTick();

    // Today tab charts
    if (weightRes.status === 'fulfilled' && weightRes.value.data && !weightRes.value.error) {
      initTodayWeightChart(weightRes.value.data as unknown as Record<string, unknown>);
    }
    if (checkinRes.status === 'fulfilled' && checkinRes.value.data && !checkinRes.value.error) {
      initTodayCheckinChart(checkinRes.value.data as unknown as Record<string, unknown>);
    }
    // Recommendations
    if (recommendRes.status === 'fulfilled' && recommendRes.value.data && !recommendRes.value.error) {
      recommends.value = recommendRes.value.data as unknown as Record<string, unknown>;
    }
    // Greeting
    if (greetingRes.status === 'fulfilled' && greetingRes.value.data && !greetingRes.value.error) {
      greetingCard.value = greetingRes.value.data as unknown as Record<string, unknown>;
    }
  } catch {
    // silent
  }

  dataLoading.value = false;

  // Load statistics charts (the 8 detailed charts)
  loadStatisticsCharts();

  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  todayWeightChart?.dispose();
  todayCheckinChart?.dispose();
  dietCompChart?.dispose();
  statsCharts.forEach(c => c.dispose());
});
</script>

<style scoped>
.dashboard-page {
  padding: 16px 0;
}

/* Greeting Card */
.greeting-card {
  border-radius: 14px;
  margin-bottom: 16px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}
.greeting-card:hover {
  transform: translateY(-2px);
}
.card-morning {
  background: linear-gradient(135deg, #1a237e 0%, #0d47a1 50%, #01579b 100%) !important;
  color: #fff;
}
.card-noon {
  background: linear-gradient(135deg, #1b5e20 0%, #2e7d32 50%, #388e3c 100%) !important;
  color: #fff;
}
.card-reminder {
  background: linear-gradient(135deg, #b71c1c 0%, #c62828 50%, #d32f2f 100%) !important;
  color: #fff;
}
.card-celebration {
  background: linear-gradient(135deg, #e65100 0%, #f57c00 50%, #ff9800 100%) !important;
  color: #fff;
}
.card-afternoon {
  background: linear-gradient(135deg, #4a148c 0%, #6a1b9a 50%, #7b1fa2 100%) !important;
  color: #fff;
}
.card-default {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%) !important;
  color: #fff;
}
.greeting-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.greeting-emoji { font-size: 28px; }
.greeting-time { font-size: 16px; font-weight: 600; opacity: 0.9; }
.greeting-message { font-size: 18px; font-weight: 600; margin: 0 0 6px; line-height: 1.5; }
.greeting-detail { font-size: 14px; opacity: 0.7; margin: 0 0 14px; line-height: 1.6; }
.greeting-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.greeting-progress {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  gap: 12px;
}
.greeting-progress :deep(.n-progress) { flex: 1; }
.progress-label { font-size: 13px; opacity: 0.8; white-space: nowrap; }

/* Stats Grid */
.stats-grid { margin-bottom: 16px; }
.stat-card .stat-value { font-size: 24px; font-weight: 700; }

/* Progress Bar */
.progress-bar-card { margin-bottom: 16px; }
.text-secondary { color: var(--n-text-color-3, #8b949e); }

/* Sections */
.section-card { margin-bottom: 16px; }
.checkin-row { display: flex; align-items: center; gap: 12px; }

/* Plan Progress */
.plan-progress { padding: 4px 0; }
.plan-header { font-size: 14px; margin-bottom: 12px; }
.plan-hint { font-size: 12px; color: var(--n-text-color-3, #999); margin-top: 6px; }

/* Charts */
.chart-card { margin-bottom: 16px; }
.chart-container { height: 280px; }

/* Statistics Charts Grid */
.stats-section-card { margin-bottom: 16px; }
.stats-charts-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.stats-chart-item { min-width: 0; }
.stats-chart-item.full-width { width: 100%; }
.stats-chart-item.half-width { width: calc(50% - 8px); }
@media (max-width: 768px) {
  .stats-chart-item.half-width { width: 100%; }
}

/* Error */
.error-alert { margin-bottom: 16px; }

/* Bottom */
.bottom-section { margin-top: 16px; margin-bottom: 16px; }

.progress-content { padding: 8px 0; }
.progress-item { margin-bottom: 20px; }
.progress-item span {
  display: block;
  font-size: 13px;
  color: var(--n-text-color-3, #999);
  margin-bottom: 8px;
}
.progress-detail { display: flex; gap: 24px; flex-wrap: wrap; }
.detail-item { font-size: 13px; color: var(--n-text-color-3, #999); }
.detail-item strong { color: var(--n-text-color, #333); }

/* Assessment */
.assessment-tags { display: flex; gap: 12px; flex-wrap: wrap; padding: 8px 0; }

/* Recommendations */
.recommend-list { display: flex; flex-direction: column; gap: 10px; }
.recommend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--n-color-modal, #f5f5f5);
  border: 1px solid var(--n-border-color, #e8e8e8);
  font-size: 13px;
}
.recommend-item .item-name { font-weight: 500; flex: 1; }
.recommend-item .item-meta { font-size: 12px; color: var(--n-text-color-3, #999); }

/* Tips */
.tips-list { display: flex; flex-direction: column; gap: 8px; }
.tip-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 13px; }

/* AI Suggestions */
.ai-suggestion-card { border-left: 3px solid #2080f0; }
.ai-suggestion-text { color: var(--n-text-color-2, #666); font-size: 14px; line-height: 1.8; margin: 0; }

/* Font tabular */
.font-tabular { font-variant-numeric: tabular-nums; }
</style>
