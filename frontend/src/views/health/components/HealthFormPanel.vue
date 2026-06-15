<template>
  <n-form ref="formRef" :model="modelValue" :rules="rules" label-placement="left" label-width="120" class="health-form">
    <!-- Section: Basic Info -->
    <n-divider title-placement="left" class="section-divider">
      <n-icon :component="PersonOutline" :size="18" class="mr-1" />
      基本信息
    </n-divider>

    <div class="flex gap-5">
      <div class="flex-1">
        <n-form-item label="身高(cm)" path="height">
          <n-input-number
            :value="modelValue.height"
            @update:value="(v: number | null) => emit('update:modelValue', { ...modelValue, height: v })"
            :min="100"
            :max="250"
            :precision="1"
            :step="0.5"
            placeholder="请输入身高"
            class="w-full"
          />
        </n-form-item>
      </div>
      <div class="flex-1">
        <n-form-item label="体重(kg)" path="weight">
          <n-input-number
            :value="modelValue.weight"
            @update:value="(v: number | null) => emit('update:modelValue', { ...modelValue, weight: v })"
            :min="30"
            :max="300"
            :precision="1"
            :step="0.5"
            placeholder="请输入体重"
            class="w-full"
          />
        </n-form-item>
      </div>
    </div>

    <div class="flex gap-5">
      <div class="flex-1">
        <n-form-item label="性别" path="gender">
          <n-radio-group
            :value="modelValue.gender"
            @update:value="(v: string) => emit('update:modelValue', { ...modelValue, gender: v })"
          >
            <n-radio value="MALE">男</n-radio>
            <n-radio value="FEMALE">女</n-radio>
          </n-radio-group>
        </n-form-item>
      </div>
      <div class="flex-1">
        <n-form-item label="目标体重(kg)" path="targetWeight">
          <n-input-number
            :value="modelValue.targetWeight"
            @update:value="(v: number | null) => emit('update:modelValue', { ...modelValue, targetWeight: v })"
            :min="30"
            :max="300"
            :precision="1"
            :step="0.5"
            placeholder="您的目标体重"
            class="w-full"
          />
        </n-form-item>
      </div>
    </div>

    <!-- BMI/BMR Calculation Preview -->
    <div v-if="bmi !== null || bmr !== null" class="mb-5 p-4 bg-gray-800/50 border border-gray-700 rounded-lg">
      <div class="flex gap-8 items-center">
        <div v-if="bmi !== null" class="flex flex-col items-center">
          <span class="text-2xl font-bold" :class="bmiColorClass">{{ bmi }}</span>
          <span class="text-xs text-gray-400 mt-1">BMI</span>
          <n-tag :type="bmiTagType" size="small" class="mt-1">{{ bmiLabel }}</n-tag>
        </div>
        <div v-if="bmr !== null" class="flex flex-col items-center">
          <span class="text-2xl font-bold text-blue-400">{{ bmr }}</span>
          <span class="text-xs text-gray-400 mt-1">基础代谢率</span>
          <span class="text-xs text-gray-500 mt-1">kcal/天</span>
        </div>
      </div>
    </div>

    <!-- Section: Health Info -->
    <n-divider title-placement="left" class="section-divider">
      <n-icon :component="HeartOutline" :size="18" class="mr-1" />
      健康信息
    </n-divider>

    <n-form-item label="健康目标" path="goal">
      <n-input
        :value="modelValue.goal"
        @update:value="(v: string) => emit('update:modelValue', { ...modelValue, goal: v })"
        type="textarea"
        :rows="2"
        placeholder="例如：减重5kg、每周运动3次、增肌塑形"
        :maxlength="200"
        show-count
      />
    </n-form-item>

    <n-form-item label="既往病史" path="diseaseHistory">
      <n-input
        :value="modelValue.diseaseHistory"
        @update:value="(v: string) => emit('update:modelValue', { ...modelValue, diseaseHistory: v })"
        type="textarea"
        :rows="2"
        placeholder="例如：高血压、糖尿病等（无可不填）"
        :maxlength="500"
        show-count
      />
    </n-form-item>

    <div class="flex gap-5">
      <div class="flex-1">
        <n-form-item label="过敏史" path="allergyHistory">
          <n-input
            :value="modelValue.allergyHistory"
            @update:value="(v: string) => emit('update:modelValue', { ...modelValue, allergyHistory: v })"
            type="textarea"
            :rows="2"
            placeholder="例如：青霉素过敏、花粉过敏等（无可不填）"
            :maxlength="500"
            show-count
          />
        </n-form-item>
      </div>
      <div class="w-[240px]">
        <n-form-item label="过敏类型" path="allergyType">
          <n-select
            :value="modelValue.allergyType"
            @update:value="(v: string | null) => emit('update:modelValue', { ...modelValue, allergyType: v })"
            :options="allergyTypeOptions"
            clearable
            placeholder="选择过敏类型"
          />
        </n-form-item>
      </div>
    </div>

    <n-form-item label="家族病史" path="familyHistory">
      <n-input
        :value="modelValue.familyHistory"
        @update:value="(v: string) => emit('update:modelValue', { ...modelValue, familyHistory: v })"
        type="textarea"
        :rows="2"
        placeholder="例如：父母有高血压、家族糖尿病史等（无可不填）"
        :maxlength="500"
        show-count
      />
    </n-form-item>

    <n-form-item label="当前用药" path="medication">
      <n-input
        :value="modelValue.medication"
        @update:value="(v: string) => emit('update:modelValue', { ...modelValue, medication: v })"
        type="textarea"
        :rows="2"
        placeholder="例如：降压药、胰岛素等（无可不填）"
        :maxlength="500"
        show-count
      />
    </n-form-item>

    <!-- Section: Lifestyle -->
    <n-divider title-placement="left" class="section-divider">
      <n-icon :component="BicycleOutline" :size="18" class="mr-1" />
      生活习惯
    </n-divider>

    <n-form-item label="运动习惯" path="exerciseHabit">
      <n-input
        :value="modelValue.exerciseHabit"
        @update:value="(v: string) => emit('update:modelValue', { ...modelValue, exerciseHabit: v })"
        type="textarea"
        :rows="2"
        placeholder="例如：每周慢跑3次、每天步行30分钟"
        :maxlength="500"
        show-count
      />
    </n-form-item>

    <n-form-item label="饮食习惯" path="dietHabit">
      <n-input
        :value="modelValue.dietHabit"
        @update:value="(v: string) => emit('update:modelValue', { ...modelValue, dietHabit: v })"
        type="textarea"
        :rows="2"
        placeholder="例如：偏好清淡、不挑食、每天三餐规律"
        :maxlength="500"
        show-count
      />
    </n-form-item>
  </n-form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { NForm, NFormItem, NInput, NInputNumber, NRadioGroup, NRadio, NSelect, NDivider, NIcon, NTag } from 'naive-ui';
import { PersonOutline, HeartOutline, BicycleOutline } from '@vicons/ionicons5';
import type { FormInst, FormRules, SelectOption } from 'naive-ui';

defineOptions({ name: 'HealthFormPanel' });

export interface HealthFormData {
  height: number | null;
  weight: number | null;
  gender: string;
  targetWeight: number | null;
  goal: string;
  diseaseHistory: string;
  allergyHistory: string;
  allergyType: string | null;
  familyHistory: string;
  medication: string;
  exerciseHabit: string;
  dietHabit: string;
}

const props = withDefaults(defineProps<{
  modelValue: HealthFormData;
}>(), {
  modelValue: () => ({
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
  })
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: HealthFormData): void;
}>();

const allergyTypeOptions: SelectOption[] = [
  { label: '食物过敏', value: 'FOOD' },
  { label: '药物过敏', value: 'DRUG' },
  { label: '环境过敏', value: 'ENVIRONMENT' }
];

// BMI calculation: weight(kg) / height(m)^2
const bmi = computed<number | null>(() => {
  const { height, weight } = props.modelValue;
  if (!height || !weight || height <= 0) return null;
  const heightM = height / 100;
  return Number((weight / (heightM * heightM)).toFixed(1));
});

// BMR calculation (Mifflin-St Jeor equation)
const bmr = computed<number | null>(() => {
  const { height, weight, gender } = props.modelValue;
  if (!height || !weight || !gender) return null;
  // Male: 10 * weight + 6.25 * height - 5 * age + 5
  // Female: 10 * weight + 6.25 * height - 5 * age - 161
  // Without age, use simplified version
  if (gender === 'MALE') {
    return Math.round(10 * weight + 6.25 * height + 5);
  } else {
    return Math.round(10 * weight + 6.25 * height - 161);
  }
});

const bmiLabel = computed<string>(() => {
  if (bmi.value === null) return '';
  if (bmi.value < 18.5) return '偏瘦';
  if (bmi.value < 24) return '正常';
  if (bmi.value < 28) return '偏胖';
  return '肥胖';
});

const bmiColorClass = computed<string>(() => {
  if (bmi.value === null) return '';
  if (bmi.value < 18.5) return 'text-blue-400';
  if (bmi.value < 24) return 'text-green-400';
  if (bmi.value < 28) return 'text-orange-400';
  return 'text-red-400';
});

const bmiTagType = computed<'info' | 'success' | 'warning' | 'error'>(() => {
  if (bmi.value === null) return 'info';
  if (bmi.value < 18.5) return 'info';
  if (bmi.value < 24) return 'success';
  if (bmi.value < 28) return 'warning';
  return 'error';
});

const rules: FormRules = {
  height: [
    { required: true, type: 'number', message: '请输入身高', trigger: ['blur', 'change'] },
    {
      type: 'number',
      min: 100,
      max: 250,
      message: '身高必须在100-250cm之间',
      trigger: 'change'
    }
  ],
  weight: [
    { required: true, type: 'number', message: '请输入体重', trigger: ['blur', 'change'] },
    {
      type: 'number',
      min: 30,
      max: 300,
      message: '体重必须在30-300kg之间',
      trigger: 'change'
    }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  targetWeight: [
    {
      type: 'number',
      min: 30,
      max: 300,
      message: '目标体重必须在30-300kg之间',
      trigger: 'change'
    }
  ],
  goal: [
    { required: true, message: '请输入健康目标', trigger: ['blur'] },
    { max: 200, message: '健康目标不能超过200个字符', trigger: 'blur' }
  ],
  diseaseHistory: [{ max: 500, message: '既往病史不能超过500个字符', trigger: 'blur' }],
  allergyHistory: [{ max: 500, message: '过敏史不能超过500个字符', trigger: 'blur' }],
  familyHistory: [{ max: 500, message: '家族病史不能超过500个字符', trigger: 'blur' }],
  medication: [{ max: 500, message: '当前用药不能超过500个字符', trigger: 'blur' }],
  exerciseHabit: [{ max: 500, message: '运动习惯不能超过500个字符', trigger: 'blur' }],
  dietHabit: [{ max: 500, message: '饮食习惯不能超过500个字符', trigger: 'blur' }]
};

const formRef = ref<FormInst | null>(null);

async function validate(): Promise<boolean> {
  if (!formRef.value) return false;
  try {
    await formRef.value.validate();
    return true;
  } catch {
    return false;
  }
}

defineExpose({ validate, bmi, bmr });
</script>

<style scoped lang="scss">
.health-form {
  margin-top: 24px;
}

.section-divider {
  margin: 28px 0 20px;
  font-weight: 600;
  font-size: 15px;
}
</style>
