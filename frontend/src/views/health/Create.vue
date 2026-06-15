<template>
  <div class="p-1">
    <n-card class="max-w-[860px]">
      <h2 class="page-title text-xl font-semibold mb-1">创建健康档案</h2>
      <p class="page-desc text-gray-400 text-sm mb-6">填写您的身体指标与健康状况，我们为您计算 BMI 与基础代谢率</p>

      <HealthFormPanel ref="formPanelRef" v-model="form" />

      <div class="mt-6 pl-[120px] flex gap-3">
        <n-button type="primary" :loading="submitting" :disabled="submitting" @click="handleSubmit">
          创建档案
        </n-button>
        <n-button :disabled="submitting" @click="router.push('/health/view')">取消</n-button>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { NButton, NCard, useDialog } from 'naive-ui';
import { fetchCreateHealth, fetchGetLatestHealth } from '@/service/api';
import HealthFormPanel from './components/HealthFormPanel.vue';
import type { HealthFormData } from './components/HealthFormPanel.vue';

defineOptions({ name: 'HealthCreate' });

const router = useRouter();
const dialog = useDialog();
const formPanelRef = ref<InstanceType<typeof HealthFormPanel> | null>(null);
const submitting = ref(false);

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

async function checkExistingRecord(): Promise<boolean> {
  try {
    const { data } = await fetchGetLatestHealth();
    return !!data;
  } catch {
    return false;
  }
}

async function handleSubmit(): Promise<void> {
  if (!formPanelRef.value) return;
  const valid = await formPanelRef.value.validate();
  if (!valid) return;

  const exists = await checkExistingRecord();
  if (exists) {
    const confirmed = await new Promise<boolean>((resolve) => {
      dialog.warning({
        title: '提示',
        content: '您已存在健康档案，继续创建将新增一条档案记录。是否继续？',
        positiveText: '继续创建',
        negativeText: '取消',
        onPositiveClick: () => resolve(true),
        onNegativeClick: () => resolve(false)
      });
    });
    if (!confirmed) return;
  }

  submitting.value = true;
  try {
    await fetchCreateHealth({
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
    });
    window.$message?.success('健康档案创建成功');
    router.push('/health/view');
  } finally {
    submitting.value = false;
  }
}
</script>
