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
  NTag,
  NSpace,
  NPopconfirm,
  useMessage
} from 'naive-ui';
import type { DataTableColumns, FormInst, FormRules, SelectOption } from 'naive-ui';
import {
  fetchGetAdminFoodItems,
  fetchCreateFoodItem,
  fetchUpdateFoodItem,
  fetchDeleteFoodItem,
  executeWithApproval
} from '@/service/api';
import { useAuthStore } from '@/store/modules/auth';

defineOptions({ name: 'AdminFoodManage' });

const message = useMessage();
const authStore = useAuthStore();

interface FoodRow {
  id: number;
  name: string;
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
  unit: string;
  category?: string;
  caloriePer100g?: number | null;
  proteinPer100g?: number | null;
  fatPer100g?: number | null;
  carbsPer100g?: number | null;
  status?: number;
}

const categoryOptions: SelectOption[] = [
  { label: '主食', value: '主食' },
  { label: '肉类', value: '肉类' },
  { label: '蔬菜', value: '蔬菜' },
  { label: '水果', value: '水果' },
  { label: '乳制品', value: '乳制品' },
  { label: '零食', value: '零食' },
  { label: '饮品', value: '饮品' },
  { label: '其他', value: '其他' }
];

// Search / filter
const searchKeyword = ref('');
const filterCategory = ref<string | null>(null);

// Filtered data (client-side since the list is fully loaded)
const filteredItems = computed(() => {
  return items.value.filter(item => {
    const matchKeyword = !searchKeyword.value || item.name.includes(searchKeyword.value);
    const matchCategory = !filterCategory.value || item.category === filterCategory.value;
    return matchKeyword && matchCategory;
  });
});

// Table columns
const columns: DataTableColumns<FoodRow> = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '食物名称', key: 'name', minWidth: 120 },
  { title: '分类', key: 'category', width: 100 },
  {
    title: '每100g热量',
    key: 'caloriePer100g',
    width: 130,
    render(row) {
      return `${row.caloriePer100g ?? '-'} kcal`;
    }
  },
  {
    title: '营养素 (g)',
    key: 'nutrients',
    minWidth: 180,
    render(row) {
      return `蛋白质${row.proteinPer100g ?? '-'} / 脂肪${row.fatPer100g ?? '-'} / 碳水${row.carbsPer100g ?? '-'}`;
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
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
              default: () => `确定要删除食物「${row.name}」吗？此操作需要审批。`
            }
          )
        ]
      });
    }
  }
];

// Data
const loading = ref(false);
const items = ref<FoodRow[]>([]);

// Modal
const showModal = ref(false);
const isEditing = ref(false);
const saving = ref(false);
const formRef = ref<FormInst | null>(null);

interface FoodForm {
  id: number | null;
  name: string;
  category: string;
  caloriePer100g: number | null;
  proteinPer100g: number | null;
  fatPer100g: number | null;
  carbsPer100g: number | null;
}

const form = ref<FoodForm>({
  id: null,
  name: '',
  category: '',
  caloriePer100g: null,
  proteinPer100g: null,
  fatPer100g: null,
  carbsPer100g: null
});

const formRules: FormRules = {
  name: { required: true, message: '请填写食物名称', trigger: 'blur' },
  category: { required: true, message: '请选择分类', trigger: 'change', type: 'string' },
  caloriePer100g: { required: true, message: '请填写每100g热量', trigger: 'blur', type: 'number' }
};

async function loadItems() {
  loading.value = true;
  try {
    const res = await fetchGetAdminFoodItems();
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

function handleEdit(row: FoodRow) {
  isEditing.value = true;
  form.value = {
    id: row.id,
    name: row.name,
    category: row.category || '',
    caloriePer100g: row.caloriePer100g ?? null,
    proteinPer100g: row.proteinPer100g ?? null,
    fatPer100g: row.fatPer100g ?? null,
    carbsPer100g: row.carbsPer100g ?? null
  };
  showModal.value = true;
}

function resetForm() {
  form.value = {
    id: null,
    name: '',
    category: '',
    caloriePer100g: null,
    proteinPer100g: null,
    fatPer100g: null,
    carbsPer100g: null
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
      await fetchUpdateFoodItem(form.value as any);
      message.success('更新成功');
    } else {
      await fetchCreateFoodItem(form.value as any);
      message.success('创建成功');
    }
    showModal.value = false;
    loadItems();
  } finally {
    saving.value = false;
  }
}

function handleDelete(row: FoodRow) {
  executeWithApproval(
    'delete_food',
    `删除食物: ${row.name} (ID: ${row.id})`,
    (approvalId: string) => fetchDeleteFoodItem(row.id, approvalId),
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
    <NCard :title="`食物字典管理 (${items.length}条)`">
      <template #header-extra>
        <NSpace align="center">
          <NInput
            v-model:value="searchKeyword"
            placeholder="搜索食物名称"
            clearable
            style="width: 200px"
          />
          <NSelect
            v-model:value="filterCategory"
            :options="categoryOptions"
            placeholder="全部分类"
            clearable
            style="width: 130px"
          />
          <NButton type="primary" @click="handleAdd">新增食物</NButton>
        </NSpace>
      </template>
      <NDataTable
        :columns="columns"
        :data="filteredItems"
        :loading="loading"
        :row-key="(row: FoodRow) => row.id"
        :scroll-x="900"
      />
    </NCard>

    <!-- Add/Edit Modal -->
    <NModal
      v-model:show="showModal"
      preset="dialog"
      :title="isEditing ? '编辑食物' : '新增食物'"
      style="width: 520px"
      :show-icon="false"
    >
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="110">
        <NFormItem label="食物名称" path="name">
          <NInput v-model:value="form.name" placeholder="如：米饭" />
        </NFormItem>
        <NFormItem label="分类" path="category">
          <NSelect v-model:value="form.category" :options="categoryOptions" placeholder="请选择分类" />
        </NFormItem>
        <NFormItem label="每100g热量(kcal)" path="caloriePer100g">
          <NInputNumber v-model:value="form.caloriePer100g" :min="0" style="width: 100%" />
        </NFormItem>
        <NFormItem label="蛋白质(g)">
          <NInputNumber v-model:value="form.proteinPer100g" :min="0" :precision="1" style="width: 100%" />
        </NFormItem>
        <NFormItem label="脂肪(g)">
          <NInputNumber v-model:value="form.fatPer100g" :min="0" :precision="1" style="width: 100%" />
        </NFormItem>
        <NFormItem label="碳水(g)">
          <NInputNumber v-model:value="form.carbsPer100g" :min="0" :precision="1" style="width: 100%" />
        </NFormItem>
      </NForm>
      <template #action>
        <NButton @click="showModal = false">取消</NButton>
        <NButton type="primary" :loading="saving" @click="handleSave">保存</NButton>
      </template>
    </NModal>
  </div>
</template>
