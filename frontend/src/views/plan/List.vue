<template>
  <div class="list-page" v-loading="pageLoading">
    <MedicalDisclaimerBanner />
    <div class="list-card glass-card">
      <div class="card-header">
        <h2 class="page-title">我的 AI 计划</h2>
        <el-button type="primary" @click="$router.push('/plan/generate')">生成新计划</el-button>
      </div>

      <div v-if="list.length > 0" class="plan-list">
        <div
          v-for="plan in list"
          :key="plan.id"
          class="plan-item"
          :class="{ active: plan.status === 1 }"
          @click="$router.push(`/plan/${plan.id}`)"
        >
          <div class="plan-info">
            <div class="plan-name-row">
              <el-tag
                :type="plan.planType === 'sport' ? 'success' : 'primary'"
                size="small"
                effect="dark"
              >
                {{ plan.planType === 'sport' ? '运动' : '饮食' }}
              </el-tag>
              <span class="plan-name">{{ plan.planName }}</span>
              <el-tag v-if="plan.status === 1" type="warning" size="small" effect="dark">
                当前生效
              </el-tag>
            </div>
            <div class="plan-meta">
              <span>{{ plan.durationDays }}天计划</span>
              <span class="meta-divider">|</span>
              <span>开始：{{ plan.startDate }}</span>
              <span class="meta-divider">|</span>
              <span>创建：{{ plan.createTime }}</span>
            </div>
          </div>
          <div class="plan-actions" @click.stop>
            <el-button
              v-if="plan.status !== 1"
              size="small"
              text
              @click="handleActive(plan.id)"
            >
              启用
            </el-button>
            <el-button size="small" text type="danger" @click="handleDelete(plan.id)">
              删除
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无 AI 计划" class="empty-block">
        <el-button type="primary" @click="$router.push('/plan/generate')">立即生成</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPlanList, activePlan, deletePlan } from '@/api/aiPlan'
import MedicalDisclaimerBanner from '@/components/MedicalDisclaimerBanner.vue'

const router = useRouter()
const pageLoading = ref(false)
const list = ref([])

async function loadList() {
  pageLoading.value = true
  try {
    const res = await getPlanList()
    list.value = res.data || []
  } finally {
    pageLoading.value = false
  }
}

async function handleActive(id) {
  try {
    await activePlan(id)
    ElMessage.success('计划已切换为当前生效')
    await loadList()
  } catch {
    // handled by interceptor
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该计划吗？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deletePlan(id)
    ElMessage.success('计划已删除')
    await loadList()
  } catch {
    // cancelled or error handled by interceptor
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped lang="scss">
.list-page {
  padding: 4px;
}

.list-card {
  padding: 28px 32px;
  max-width: 860px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: rgba(88, 166, 255, 0.04);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;

  &:hover {
    background: rgba(88, 166, 255, 0.08);
    border-color: rgba(88, 166, 255, 0.2);
  }

  &.active {
    border-color: rgba(210, 153, 34, 0.3);
    background: rgba(210, 153, 34, 0.06);
  }
}

.plan-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.plan-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.plan-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.plan-meta {
  font-size: 12px;
  color: var(--text-secondary);
}

.meta-divider {
  margin: 0 4px;
  color: #30363d;
}

.plan-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.empty-block {
  padding: 32px 0;
}
</style>
