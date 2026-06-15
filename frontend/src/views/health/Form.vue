<template>
  <div class="p-1">
    <n-spin :show="pageLoading">
      <n-card class="max-w-[860px]">
        <h2 class="page-title text-xl font-semibold mb-1">
          {{ isEdit ? '修改健康档案' : '创建健康档案' }}
        </h2>
        <p class="page-desc text-gray-400 text-sm mb-6">填写您的身体指标与健康状况，我们为您计算 BMI 与基础代谢率</p>

        <HealthFormPanel ref="formPanelRef" v-model="form" />

        <div class="mt-6 pl-[120px] flex gap-3">
          <n-button type="primary" :loading="saveLoading" :disabled="saveLoading" @click="handleSave">
            {{ isEdit ? '更新档案' : '创建档案' }}
          </n-button>
          <n-button :disabled="saveLoading" @click="handleCancel">取消</n-button>
        </div>
      </n-card>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { NButton, NCard, NSpin } from 'naive-ui';
import { fetchCreateHealth, fetchUpdateHealth, fetchGetLatestHealth } from '@/service/api';
import HealthFormPanel from './components/HealthFormPanel.vue';
import type { HealthFormData } from './components/HealthFormPanel.vue';

defineOptions({ name: 'HealthForm' });

const router = useRouter();
const formPanelRef = ref<InstanceType<typeof HealthFormPanel> | null>(null);
const pageLoading = ref(false);
const saveLoading = ref(false);
const isEdit = ref(false);

const form = reactive<HealthFormData>({
  height: null,
  weight: null,
  gender: 'MALE',
  targetWeight: null,
  goal: '',
  diseaseHistory: '',
  allergyHistory: '',
  allergyType: null,
  familyHistory: '',
  medication: '',
  exerciseHabit: '',
  dietHabit: ''
});

async function tryLoadLatest(): Promise<void> {
  try {
    const { data, error } = await fetchGetLatestHealth();
    if (error || !data) {
      isEdit.value = false;
      return;
    }
    form.height = data.height;
    form.weight = data.weight;
    form.gender = data.gender || 'MALE';
    form.targetWeight = data.targetWeight ?? null;
    form.goal = data.goal || '';
    form.diseaseHistory = data.diseaseHistory || '';
    form.allergyHistory = data.allergyHistory || '';
    form.allergyType = data.allergyType ?? null;
    form.familyHistory = (data as any).familyHistory || '';
    form.medication = (data as any).medication || '';
    form.exerciseHabit = data.exerciseHabit || '';
    form.dietHabit = data.dietHabit || '';
    isEdit.value = true;
  } catch {
    isEdit.value = false;
  }
}

onMounted(async () => {
  pageLoading.value = true;
  try {
    await tryLoadLatest();
  } finally {
    pageLoading.value = false;
  }
});

async function handleSave(): Promise<void> {
  if (!formPanelRef.value) return;
  const valid = await formPanelRef.value.validate();
  if (!valid) return;

  saveLoading.value = true;
  try {
    const payload = {
      height: form.height!,
      weight: form.weight!,
      gender: form.gender,
      targetWeight: form.targetWeight ?? undefined,
      goal: form.goal,
      diseaseHistory: form.diseaseHistory,
      allergyHistory: form.allergyHistory,
      allergyType: (form.allergyType as Api.Health.AllergyType) ?? undefined,
      familyHistory: form.familyHistory,
      medication: form.medication,
      exerciseHabit: form.exerciseHabit,
      dietHabit: form.dietHabit
    };
    if (isEdit.value) {
      await fetchUpdateHealth(payload);
      window.$message?.success('健康档案已更新');
    } else {
      await fetchCreateHealth(payload);
      window.$message?.success('健康档案已创建');
    }
    router.push('/health/view');
  } finally {
    saveLoading.value = false;
  }
}

function handleCancel(): void {
  router.back();
}
</script>
