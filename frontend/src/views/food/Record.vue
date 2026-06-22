<template>
  <div class="food-record-page flex flex-col gap-5">
    <div>
      <h2 class="text-xl font-semibold">{{ $t('food.record') || '饮食记录' }}</h2>
    </div>

    <!-- AI Image Recognition -->
    <NCard :title="$t('food.aiRecognition') || 'AI智能食物识别'" :bordered="false">
      <div class="flex flex-col gap-4">
        <NUpload
          :auto-upload="false"
          :show-file-list="false"
          :custom-request="handleImageUpload"
          accept="image/*"
          :multiple="false"
        >
          <NButton v-if="!recognizing && !recognitionResult" dashed size="large" class="w-full" style="height:120px">
            <div class="flex flex-col items-center gap-2">
              <span class="text-3xl">📷</span>
              <span>{{ $t('food.uploadHint') || '拍照或上传食物图片' }}</span>
              <span class="text-xs text-secondary">{{ $t('food.uploadSubHint') || 'AI自动识别食物品种并估算热量' }}</span>
            </div>
          </NButton>
          <NButton v-else-if="recognizing" loading size="large" class="w-full" style="height:80px">
            AI正在识别中...
          </NButton>
        </NUpload>

        <!-- Recognition Result -->
        <div v-if="recognitionResult" class="rounded-lg border border-[rgba(88,166,255,0.15)] bg-[rgba(88,166,255,0.06)] p-4">
          <div class="mb-3 flex items-center justify-between">
            <h3 class="text-[15px] text-[#58a6ff]">识别结果</h3>
            <NButton text size="small" @click="recognitionResult = null; uploadedImage = ''">清除</NButton>
          </div>
          <div class="flex items-center gap-2.5 rounded-lg border border-[#21262d] bg-[#0d1117] p-2.5">
            <span class="flex-1 font-medium">{{ recognitionResult.foodName }}</span>
            <NTag type="success" size="small">~{{ recognitionResult.caloriePer100g }}kcal/100g</NTag>
            <span class="text-xs text-secondary">置信度 {{ recognitionResult.confidence }}%</span>
            <NButton size="small" type="primary" @click="quickRecord(recognitionResult)">快速记录</NButton>
          </div>
          <p v-if="recognitionResult.note" class="mt-2.5 text-[13px] leading-relaxed text-secondary">{{ recognitionResult.note }}</p>
        </div>
      </div>
    </NCard>

    <!-- NLP Text Input -->
    <NCard :title="$t('food.nlpInput') || '一句话记账'" :bordered="false">
      <div class="flex flex-col gap-3">
        <div class="flex items-center gap-2">
          <NInput
            v-model:value="nlpText"
            placeholder="一句话记录，如：中午吃了一碗兰州拉面加煎蛋"
            :disabled="nlpLoading"
            clearable
            class="flex-1"
            @keyup.enter="handleNlpRecord"
          />
          <NButton type="primary" :loading="nlpLoading" @click="handleNlpRecord" :disabled="!nlpText?.trim()">
            智能识别
          </NButton>
        </div>
        <!-- NLP Result Preview -->
        <div v-if="nlpResult" class="rounded-lg border border-[rgba(63,185,80,0.15)] bg-[rgba(63,185,80,0.06)] p-3">
          <div
            v-for="(item, i) in nlpResult.items"
            :key="i"
            class="mb-2 flex items-center gap-2.5 rounded-lg border border-[#21262d] bg-[#0d1117] p-2.5 text-[13px]"
          >
            <span class="text-green-500">✓</span>
            <span class="min-w-[80px] flex-1 font-medium">{{ item.foodName }}</span>
            <NInputNumber v-model:value="item.weightG" :min="1" :max="2000" size="small" class="w-[100px]" />
            <span class="text-xs text-secondary">克</span>
            <span class="whitespace-nowrap text-xs text-[#fa8c16]">≈ {{ item.calories }} kcal</span>
            <NButton size="small" type="error" text @click="removeNlpItem(i)">删除</NButton>
          </div>
          <div class="flex justify-end gap-2.5 border-t border-[#21262d] pt-2">
            <NButton size="small" @click="nlpResult = null">取消</NButton>
            <NButton size="small" type="primary" @click="confirmNlpRecord" :loading="nlpSubmitLoading">一键录入</NButton>
          </div>
        </div>
      </div>
    </NCard>

    <!-- Quick Input -->
    <NCard :title="$t('food.quickInput') || '快捷录入'" :bordered="false">
      <div class="flex flex-col gap-3">
        <div class="flex items-center gap-2">
          <NInput v-model:value="quickText" placeholder="输入食物名称快速搜索..." clearable class="flex-1" @keyup.enter="handleQuickTextSearch" />
          <NButton type="primary" @click="handleQuickTextSearch" :loading="quickTextLoading">搜索</NButton>
        </div>
        <div v-if="frequentItems.length > 0" class="flex flex-wrap items-center gap-2">
          <span class="whitespace-nowrap text-[13px] text-secondary">常用：</span>
          <NButton
            v-for="item in frequentItems"
            :key="item.id"
            size="small"
            @click="selectFrequentItem(item)"
          >
            {{ item.name }}
          </NButton>
        </div>
      </div>
    </NCard>

    <!-- Submit Form -->
    <NCard :title="$t('food.recordMeal') || '记录饮食'" :bordered="false">
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="80" @submit.prevent="handleSubmit">
        <div class="flex flex-wrap items-end gap-4">
          <NFormItem label="食物" path="itemId" class="min-w-[200px] flex-1">
            <NSelect
              v-model:value="form.itemId"
              filterable
              placeholder="搜索食物名称"
              :loading="itemsLoading"
              :options="foodItemOptions"
              @update:value="onFoodChange"
            />
          </NFormItem>
          <NFormItem label="重量(克)" path="weightGrams">
            <NInputNumber v-model:value="form.weightGrams" :min="1" :max="10000" class="w-[120px]" />
          </NFormItem>
          <NFormItem label="餐次" path="mealType">
            <NSelect v-model:value="form.mealType" :options="mealTypeOptions" class="w-[120px]" />
          </NFormItem>
          <NFormItem>
            <NButton type="primary" attr-type="submit" :loading="submitting">提交记录</NButton>
          </NFormItem>
        </div>
        <NFormItem label="备注">
          <NInput v-model:value="form.note" placeholder="添加备注..." :maxlength="200" show-count />
        </NFormItem>
      </NForm>
    </NCard>

    <!-- Today Stats -->
    <div v-if="todayTotal > 0" class="flex gap-5">
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ todayTotal }}</div>
        <div class="text-[13px] text-secondary">今日总摄入 (kcal)</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ todayProtein }}</div>
        <div class="text-[13px] text-secondary">蛋白质 (g)</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ todayCarbs }}</div>
        <div class="text-[13px] text-secondary">碳水 (g)</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ todayFat }}</div>
        <div class="text-[13px] text-secondary">脂肪 (g)</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ records.length }}</div>
        <div class="text-[13px] text-secondary">今日记录条数</div>
      </NCard>
    </div>

    <!-- Records Table -->
    <NCard :bordered="false">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('food.records') || '饮食记录' }}</span>
          <NPagination v-model:page="page" :page-count="Math.ceil(total / 10)" :page-size="10" size="small" @update:page="loadRecords" />
        </div>
      </template>

      <NEmpty v-if="records.length === 0 && !recordsLoading" :description="$t('food.empty') || '还没有饮食记录，使用上方的AI识别或手动录入'">
        <template #extra>
          <span class="text-xs text-secondary">记录你的第一顿饮食吧</span>
        </template>
      </NEmpty>

      <NDataTable
        v-else
        :data="records"
        :columns="tableColumns"
        :loading="recordsLoading"
        :bordered="false"
        striped
        :scroll-x="1100"
      />
    </NCard>

    <!-- Edit Modal -->
    <NModal
      v-model:show="editModalVisible"
      preset="card"
      title="编辑饮食记录"
      class="w-[550px]"
      :mask-closable="false"
    >
      <NForm ref="editFormRef" :model="editForm" :rules="editFormRules" label-placement="left" label-width="80">
        <NFormItem label="食物" path="itemId">
          <NSelect
            v-model:value="editForm.itemId"
            filterable
            placeholder="搜索食物名称"
            :options="foodItemOptions"
          />
        </NFormItem>
        <NFormItem label="重量(克)" path="weightGrams">
          <NInputNumber v-model:value="editForm.weightGrams" :min="1" :max="10000" class="w-full" />
        </NFormItem>
        <NFormItem label="餐次" path="mealType">
          <NSelect v-model:value="editForm.mealType" :options="mealTypeOptions" />
        </NFormItem>
        <NFormItem label="热量(kcal)">
          <NInputNumber v-model:value="editForm.caloriesConsumed" :min="0" :max="10000" class="w-full" />
        </NFormItem>
        <NFormItem label="蛋白质(g)">
          <NInputNumber v-model:value="editForm.protein" :min="0" :max="1000" :precision="1" class="w-full" />
        </NFormItem>
        <NFormItem label="碳水(g)">
          <NInputNumber v-model:value="editForm.carbs" :min="0" :max="1000" :precision="1" class="w-full" />
        </NFormItem>
        <NFormItem label="脂肪(g)">
          <NInputNumber v-model:value="editForm.fat" :min="0" :max="1000" :precision="1" class="w-full" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput v-model:value="editForm.note" placeholder="添加备注..." :maxlength="200" show-count />
        </NFormItem>
      </NForm>
      <template #footer>
        <div class="flex justify-end gap-3">
          <NButton @click="editModalVisible = false">取消</NButton>
          <NButton type="primary" :loading="editSubmitting" @click="handleEditSubmit">保存</NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue';
import { useMessage, useDialog, NCard, NUpload, NButton, NInput, NInputNumber, NSelect, NForm, NFormItem, NTag, NPagination, NDataTable, NEmpty, NModal } from 'naive-ui';
import type { DataTableColumns, UploadCustomRequestOptions } from 'naive-ui';
import { fetchGetFoodItems, fetchGetFrequentItems, fetchParseFoodText, fetchSubmitFoodRecord, fetchGetFoodRecordsPage, fetchRecognizeByText, fetchRecognizeFood, fetchGetTodayCheckin, fetchDeleteFoodRecord, fetchUpdateFoodRecord } from '@/service/api';

defineOptions({ name: 'FoodRecord' });
const message = useMessage();
const dialog = useDialog();

interface FoodItem {
  id: number;
  name: string;
  caloriesPerUnit?: number;
  caloriePer100g?: number;
  unit?: string;
  category?: string;
  protein?: number;
  carbs?: number;
  fat?: number;
}

interface NlpFoodItem {
  foodName: string;
  weightG: number;
  calories: number;
}

const foodItems = ref<FoodItem[]>([]);
const frequentItems = ref<FoodItem[]>([]);
const itemsLoading = ref(false);
const records = ref<any[]>([]);
const recordsLoading = ref(false);
const submitting = ref(false);
const recognizing = ref(false);
const recognitionResult = ref<any>(null);
const uploadedImage = ref('');
const page = ref(1);
const total = ref(0);
const quickText = ref('');
const quickTextLoading = ref(false);
const formRef = ref();

const nlpText = ref('');
const nlpLoading = ref(false);
const nlpResult = ref<{ items: NlpFoodItem[] } | null>(null);
const nlpSubmitLoading = ref(false);

const selectedFoodItem = ref<FoodItem | null>(null);

// Edit state
const editModalVisible = ref(false);
const editSubmitting = ref(false);
const editFormRef = ref();
const editingId = ref<number | null>(null);
const editForm = ref({
  itemId: null as number | null,
  weightGrams: 100,
  mealType: 'lunch',
  caloriesConsumed: 0,
  protein: 0,
  carbs: 0,
  fat: 0,
  note: ''
});

const editFormRules = {
  itemId: { type: 'number' as const, required: true, message: '请选择食物', trigger: 'change' },
  weightGrams: { type: 'number' as const, required: true, message: '请输入重量', trigger: 'blur' },
  mealType: { type: 'string' as const, required: true, message: '请选择餐次', trigger: 'change' }
};

const form = ref({
  itemId: null as number | null,
  weightGrams: 100,
  mealType: 'lunch',
  note: ''
});

const formRules = {
  itemId: { type: 'number' as const, required: true, message: '请选择食物', trigger: 'change' },
  weightGrams: { type: 'number' as const, required: true, message: '请输入重量', trigger: 'blur' },
  mealType: { type: 'string' as const, required: true, message: '请选择餐次', trigger: 'change' }
};

const mealTypeOptions = [
  { label: '早餐', value: 'breakfast' },
  { label: '午餐', value: 'lunch' },
  { label: '晚餐', value: 'dinner' },
  { label: '加餐', value: 'snack' }
];

const foodItemOptions = computed(() =>
  foodItems.value.map(item => ({
    label: `${item.name} (${item.caloriesPerUnit || item.caloriePer100g || 0}kcal/${item.unit || '份'})`,
    value: item.id
  }))
);

const todayTotal = computed(() =>
  records.value.reduce((sum: number, r: any) => sum + (r.calories || 0), 0)
);

const todayProtein = computed(() =>
  records.value.reduce((sum: number, r: any) => sum + (r.protein || 0), 0)
);

const todayCarbs = computed(() =>
  records.value.reduce((sum: number, r: any) => sum + (r.carbs || 0), 0)
);

const todayFat = computed(() =>
  records.value.reduce((sum: number, r: any) => sum + (r.fat || 0), 0)
);

const mealTypeMap: Record<string, string> = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐' };

function handleDelete(id: number) {
  dialog.warning({
    title: '确认删除',
    content: '确定要删除此记录吗？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      const { error } = await fetchDeleteFoodRecord(id);
      if (!error) {
        window.$message?.success('删除成功');
        loadRecords();
      }
    }
  });
}

function handleEdit(row: any) {
  editingId.value = row.id;
  editForm.value = {
    itemId: row.itemId || row.foodItemId || null,
    weightGrams: row.weightGrams || row.amount || 100,
    mealType: row.mealType || 'lunch',
    caloriesConsumed: row.calories || row.caloriesConsumed || 0,
    protein: row.protein || 0,
    carbs: row.carbs || 0,
    fat: row.fat || 0,
    note: row.note || row.remark || ''
  };
  editModalVisible.value = true;
}

async function handleEditSubmit() {
  if (editingId.value === null) return;
  if (editFormRef.value) {
    try {
      await editFormRef.value.validate();
    } catch {
      return;
    }
  }
  editSubmitting.value = true;
  try {
    const { error } = await fetchUpdateFoodRecord(editingId.value, {
      itemId: editForm.value.itemId,
      weightGrams: editForm.value.weightGrams,
      mealType: editForm.value.mealType,
      caloriesConsumed: editForm.value.caloriesConsumed,
      protein: editForm.value.protein,
      carbs: editForm.value.carbs,
      fat: editForm.value.fat,
      note: editForm.value.note
    } as any);
    if (!error) {
      message.success('更新成功');
      editModalVisible.value = false;
      loadRecords();
    }
  } finally {
    editSubmitting.value = false;
  }
}

const tableColumns: DataTableColumns = [
  { title: '食物', key: 'foodItemName', minWidth: 120 },
  {
    title: '餐次', key: 'mealType', width: 80,
    render(row: any) {
      const type = row.mealType === 'breakfast' ? 'default' : row.mealType === 'lunch' ? 'success' : row.mealType === 'dinner' ? 'warning' : 'info';
      return h(NTag, { size: 'small', type: type as any }, { default: () => mealTypeMap[row.mealType] || row.mealType });
    }
  },
  { title: '份量', key: 'amount', width: 100, render: (row: any) => `${row.amount || row.weightGrams || '-'} ${row.unit || 'g'}` },
  { title: '热量', key: 'calories', width: 100, render: (row: any) => h('b', {}, `${row.calories} kcal`) },
  { title: '蛋白质', key: 'protein', width: 80, render: (row: any) => row.protein ? `${row.protein}g` : '-' },
  { title: '碳水', key: 'carbs', width: 80, render: (row: any) => row.carbs ? `${row.carbs}g` : '-' },
  { title: '脂肪', key: 'fat', width: 80, render: (row: any) => row.fat ? `${row.fat}g` : '-' },
  { title: '时间', key: 'createTime', width: 100, render: (row: any) => formatTime(row.createTime) },
  {
    title: '操作', key: 'actions', width: 140, fixed: 'right' as const,
    render: (row: any) => h('div', { class: 'flex gap-2' }, [
      h(NButton, { type: 'primary', size: 'small', text: true, onClick: () => handleEdit(row) }, { default: () => '编辑' }),
      h(NButton, { type: 'error', size: 'small', text: true, onClick: () => handleDelete(row.id) }, { default: () => '删除' })
    ])
  }
];

function formatTime(t: string): string {
  if (!t) return '-';
  return t.substring(11, 16);
}

function guessMealType(): string {
  const hour = new Date().getHours();
  if (hour < 10) return 'breakfast';
  if (hour < 14) return 'lunch';
  if (hour < 20) return 'dinner';
  return 'snack';
}

function onFoodChange(val: number) {
  selectedFoodItem.value = foodItems.value.find(i => i.id === val) || null;
}

async function handleImageUpload({ file }: UploadCustomRequestOptions) {
  if (!file.file) return;
  const isImage = file.file.type.startsWith('image/');
  if (!isImage) { message.warning('请上传图片文件'); return; }
  if (file.file.size > 10 * 1024 * 1024) { message.warning('图片不能超过10MB'); return; }

  recognizing.value = true;
  recognitionResult.value = null;
  const formData = new FormData();
  formData.append('image', file.file);

  try {
    const { data } = await fetchRecognizeFood(formData);
    recognitionResult.value = data || null;
  } catch {
    // handled by interceptor
  } finally {
    recognizing.value = false;
  }
}

async function handleNlpRecord() {
  const text = nlpText.value?.trim();
  if (!text) return;
  nlpLoading.value = true;
  nlpResult.value = null;
  try {
    const { data } = await fetchRecognizeByText({ text });
    if (data?.items && data.items.length > 0) {
      nlpResult.value = data;
      message.success(`已识别 ${data.items.length} 种食物`);
    } else {
      message.warning('未能识别出食物，请换个描述试试');
    }
  } catch {
    message.error('AI识别失败，请稍后重试');
  } finally {
    nlpLoading.value = false;
  }
}

function removeNlpItem(index: number) {
  if (nlpResult.value?.items) {
    nlpResult.value.items.splice(index, 1);
    if (nlpResult.value.items.length === 0) nlpResult.value = null;
  }
}

async function confirmNlpRecord() {
  if (!nlpResult.value?.items?.length) return;
  nlpSubmitLoading.value = true;
  try {
    let checkinId: number | null = null;
    try {
      const { data: checkinData } = await fetchGetTodayCheckin();
      checkinId = checkinData?.id ?? null;
    } catch { /* not checked in */ }

    let successCount = 0;
    for (const item of nlpResult.value.items) {
      const matched = foodItems.value.find(
        f => f.name.toLowerCase().includes(item.foodName.toLowerCase()) ||
             item.foodName.toLowerCase().includes(f.name.toLowerCase())
      );
      if (matched) {
        const weightG = item.weightG || 100;
        const caloriesConsumed = Math.round((matched.caloriePer100g || matched.caloriesPerUnit || 0) * weightG / 100);
        await fetchSubmitFoodRecord({
          itemId: matched.id, weightGrams: weightG, caloriesConsumed, checkinId,
          mealType: guessMealType(), foodName: matched.name, category: matched.category || ''
        } as any);
        successCount++;
      }
    }
    if (successCount > 0) {
      message.success(`成功录入 ${successCount} 项食物记录`);
      nlpText.value = '';
      nlpResult.value = null;
      loadRecords();
    } else {
      message.warning('食物库中未找到匹配项，请使用下方手动录入');
    }
  } catch {
    message.error('录入失败，请稍后重试');
  } finally {
    nlpSubmitLoading.value = false;
  }
}

async function handleQuickTextSearch() {
  const text = quickText.value?.trim();
  if (!text) return;
  quickTextLoading.value = true;
  try {
    const { data } = await fetchParseFoodText({ text });
    if (data) {
      const food = data;
      form.value.itemId = food.id;
      form.value.weightGrams = 100;
      selectedFoodItem.value = food;
      quickText.value = '';
      message.success(`已匹配：${food.name}`);
    }
  } catch {
    message.info('未匹配到对应食物，请在下方下拉列表中选择');
  } finally {
    quickTextLoading.value = false;
  }
}

function selectFrequentItem(item: FoodItem) {
  form.value.itemId = item.id;
  form.value.weightGrams = 100;
  selectedFoodItem.value = item;
  message.success(`已选择：${item.name}`);
}

function quickRecord(food: any) {
  const foodName = food.foodName || '';
  const matched = foodItems.value.find(
    f => f.name.toLowerCase().includes(foodName.toLowerCase()) ||
         foodName.toLowerCase().includes(f.name.toLowerCase())
  );
  if (matched) {
    form.value.itemId = matched.id;
    form.value.weightGrams = food.recommendedGrams || 100;
    selectedFoodItem.value = matched;
    message.success(`已匹配到 ${matched.name}`);
  } else {
    form.value.itemId = null;
    form.value.weightGrams = food.recommendedGrams || 100;
    selectedFoodItem.value = null;
    message.info(`未在食物库找到 ${foodName}，请手动选择`);
  }
  window.scrollTo({ top: 200, behavior: 'smooth' });
}

async function loadItems() {
  itemsLoading.value = true;
  try {
    const { data } = await fetchGetFoodItems();
    foodItems.value = data || [];
  } finally {
    itemsLoading.value = false;
  }
}

async function loadRecords() {
  recordsLoading.value = true;
  try {
    const { data } = await fetchGetFoodRecordsPage({ page: page.value, size: 10 });
    if (data) {
      records.value = data.records || [];
      total.value = data.total || 0;
    }
  } finally {
    recordsLoading.value = false;
  }
}

async function handleSubmit() {
  submitting.value = true;
  try {
    let checkinId: number | null = null;
    try {
      const { data: checkinData } = await fetchGetTodayCheckin();
      checkinId = checkinData?.id ?? null;
    } catch { /* not checked in */ }

    const food = selectedFoodItem.value;
    const caloriesConsumed = food
      ? Math.round((food.caloriePer100g || food.caloriesPerUnit || 0) * form.value.weightGrams / 100)
      : 0;

    const { error } = await fetchSubmitFoodRecord({
      itemId: form.value.itemId, weightGrams: form.value.weightGrams,
      caloriesConsumed, checkinId, mealType: form.value.mealType,
      foodName: food?.name || '', category: food?.category || '',
      note: form.value.note
    } as any);
    if (!error) {
      message.success('记录成功');
      form.value.weightGrams = 100;
      form.value.itemId = null;
      form.value.note = '';
      selectedFoodItem.value = null;
      loadRecords();
    }
  } finally {
    submitting.value = false;
  }
}

async function loadFrequentItems() {
  try {
    const { data } = await fetchGetFrequentItems({ limit: 8 });
    if (data) frequentItems.value = data;
  } catch { /* ignore */ }
}

onMounted(() => {
  loadItems();
  loadRecords();
  loadFrequentItems();
});
</script>
