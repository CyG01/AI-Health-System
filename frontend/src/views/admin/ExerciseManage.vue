<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NRadioGroup,
  NRadioButton,
  NTag,
  NSpace,
  NPopconfirm,
  useMessage
} from 'naive-ui';
import type { DataTableColumns, FormInst, FormRules, SelectOption } from 'naive-ui';
import {
  fetchGetAdminExerciseItems,
  fetchCreateExerciseItem,
  fetchUpdateExerciseItem,
  fetchDeleteExerciseItem,
  executeWithApproval
} from '@/service/api';
import { useAuthStore } from '@/store/modules/auth';

defineOptions({ name: 'AdminExerciseManage' });

const message = useMessage();
const authStore = useAuthStore();

interface ExerciseRow {
  id: number;
  name: string;
  type: string;
  caloriesPerMinute: number;
  calorieCoefficient?: number | null;
  targetMuscle?: string;
  difficulty?: string;
  videoUrl?: string;
  status?: number;
}

const typeOptions: SelectOption[] = [
  { label: '有氧', value: '有氧' },
  { label: '无氧', value: '无氧' },
  { label: '拉伸', value: '拉伸' }
];

const targetMuscleOptions: SelectOption[] = [
  { label: '全身', value: '全身' },
  { label: '胸', value: '胸' },
  { label: '背', value: '背' },
  { label: '腿', value: '腿' },
  { label: '核心', value: '核心' },
  { label: '肩', value: '肩' },
  { label: '手臂', value: '手臂' }
];

// Search / filter
const searchKeyword = ref('');
const filterType = ref<string | null>(null);

const filteredItems = computed(() => {
  return items.value.filter(item => {
    const matchKeyword = !searchKeyword.value || item.name.includes(searchKeyword.value);
    const matchType = !filterType.value || item.type === filterType.value;
    return matchKeyword && matchType;
  });
});

function difficultyTagType(d: string): 'error' | 'warning' | 'success' {
  if (d === '高级') return 'error';
  if (d === '中级') return 'warning';
  return 'success';
}

// Table columns
const columns: DataTableColumns<ExerciseRow> = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '运动名称', key: 'name', minWidth: 110 },
  { title: '类型', key: 'type', width: 80 },
  {
    title: '热量系数',
    key: 'calorieCoefficient',
    width: 120,
    render(row) {
      return `${row.calorieCoefficient ?? '-'} kcal/kg/h`;
    }
  },
  {
    title: '目标肌群',
    key: 'targetMuscle',
    width: 90,
    render(row) {
      if (row.targetMuscle) {
        return h(NTag, { size: 'small', bordered: false }, { default: () => row.targetMuscle });
      }
      return '-';
    }
  },
  {
    title: '难度',
    key: 'difficulty',
    width: 70,
    render(row) {
      if (!row.difficulty) return '-';
      return h(
        NTag,
        { type: difficultyTagType(row.difficulty), size: 'small', bordered: false },
        { default: () => row.difficulty }
      );
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 70,
    render(row) {
      return h(
        NTag,
        { type: row.status === 1 ? 'success' : 'info', size: 'small', bordered: false },
        { default: () => (row.status === 1 ? '启用' : '禁用') }
      );
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    fixed: 'right',
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          h(
            NPopconfirm,
            { onPositiveClick: () => handleDelete(row) },
            {
              trigger: () =>
                h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
              default: () => `确定要删除运动「${row.name}」吗？此操作需要审批。`
            }
          )
        ]
      });
    }
  }
];

// Data
const loading = ref(false);
const items = ref<ExerciseRow[]>([]);

// Modal
const showModal = ref(false);
const isEditing = ref(false);
const saving = ref(false);
const formRef = ref<FormInst | null>(null);

interface ExerciseForm {
  id: number | null;
  name: string;
  type: string;
  calorieCoefficient: number | null;
  targetMuscle: string;
  difficulty: string;
  videoUrl: string;
}

const form = ref<ExerciseForm>({
  id: null,
  name: '',
  type: '',
  calorieCoefficient: null,
  targetMuscle: '',
  difficulty: '初级',
  videoUrl: ''
});

const formRules: FormRules = {
  name: { required: true, message: '请填写运动名称', trigger: 'blur' },
  type: { required: true, message: '请选择运动类型', trigger: 'change', type: 'string' },
  calorieCoefficient: { required: true, message: '请填写热量系数', trigger: 'blur', type: 'number' }
};

async function loadItems() {
  loading.value = true;
  try {
    const res = await fetchGetAdminExerciseItems();
    items.value = res.data || [];
  } finally {
    loading.value = false;
  }
}

function handleAdd() {
  isEditing.value = false;
  resetForm();
  showModal.value = true;
}

function handleEdit(row: ExerciseRow) {
  isEditing.value = true;
  form.value = {
    id: row.id,
    name: row.name,
    type: row.type,
    calorieCoefficient: row.calorieCoefficient ?? null,
    targetMuscle: row.targetMuscle || '',
    difficulty: row.difficulty || '初级',
    videoUrl: row.videoUrl || ''
  };
  showModal.value = true;
}

function resetForm() {
  form.value = {
    id: null,
    name: '',
    type: '',
    calorieCoefficient: null,
    targetMuscle: '',
    difficulty: '初级',
    videoUrl: ''
  };
}

async function handleSave() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  saving.value = true;
  try {
    if (isEditing.value) {
      await fetchUpdateExerciseItem(form.value as any);
      message.success('更新成功');
    } else {
      await fetchCreateExerciseItem(form.value as any);
      message.success('创建成功');
    }
    showModal.value = false;
    loadItems();
  } finally {
    saving.value = false;
  }
}

function handleDelete(row: ExerciseRow) {
  executeWithApproval(
    'delete_exercise',
    `删除运动: ${row.name} (ID: ${row.id})`,
    (approvalId: string) => fetchDeleteExerciseItem(row.id, approvalId),
    authStore.userInfo?.id
  )
    .then(() => {
      message.success('删除操作已执行');
      loadItems();
    })
    .catch(() => {
      // cancelled or error handled by interceptor
    });
}

onMounted(loadItems);
</script>

<template>
  <div class="flex-col-stretch gap-16px overflow-auto p-16px">
    <NCard :title="`运动字典管理 (${items.length}条)`">
      <template #header-extra>
        <NSpace align="center">
          <NInput
            v-model:value="searchKeyword"
            placeholder="搜索运动名称"
            clearable
            style="width: 200px"
          />
          <NSelect
            v-model:value="filterType"
            :options="typeOptions"
            placeholder="全部类型"
            clearable
            style="width: 130px"
          />
          <NButton type="primary" @click="handleAdd">新增运动</NButton>
        </NSpace>
      </template>
      <NDataTable
        :columns="columns"
        :data="filteredItems"
        :loading="loading"
        :row-key="(row: ExerciseRow) => row.id"
        :scroll-x="900"
      />
    </NCard>

    <!-- Add/Edit Modal -->
    <NModal
      v-model:show="showModal"
      preset="dialog"
      :title="isEditing ? '编辑运动' : '新增运动'"
      style="width: 540px"
      :show-icon="false"
    >
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="100">
        <NFormItem label="运动名称" path="name">
          <NInput v-model:value="form.name" placeholder="如：慢跑" />
        </NFormItem>
        <NFormItem label="运动类型" path="type">
          <NSelect v-model:value="form.type" :options="typeOptions" placeholder="请选择类型" />
        </NFormItem>
        <NFormItem label="热量系数" path="calorieCoefficient">
          <NInputNumber v-model:value="form.calorieCoefficient" :min="0" :precision="1" style="width: 100%" />
          <span style="font-size: 12px; color: #999; margin-left: 8px">单位: kcal/kg/h</span>
        </NFormItem>
        <NFormItem label="目标肌群">
          <NSelect
            v-model:value="form.targetMuscle"
            :options="targetMuscleOptions"
            clearable
            placeholder="选填"
          />
        </NFormItem>
        <NFormItem label="难度等级">
          <NRadioGroup v-model:value="form.difficulty">
            <NRadioButton value="初级">初级</NRadioButton>
            <NRadioButton value="中级">中级</NRadioButton>
            <NRadioButton value="高级">高级</NRadioButton>
          </NRadioGroup>
        </NFormItem>
        <NFormItem label="视频链接">
          <NInput v-model:value="form.videoUrl" placeholder="选填" />
        </NFormItem>
      </NForm>
      <template #action>
        <NButton @click="showModal = false">取消</NButton>
        <NButton type="primary" :loading="saving" @click="handleSave">保存</NButton>
      </template>
    </NModal>
  </div>
</template>
