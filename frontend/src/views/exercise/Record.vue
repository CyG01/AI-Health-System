<template>
  <div class="exercise-record-page flex flex-col gap-5">
    <div>
      <h2 class="text-xl font-semibold">{{ $t('exercise.record') || '运动记录' }}</h2>
    </div>

    <!-- Submit Form -->
    <NCard :title="$t('exercise.recordActivity') || '记录运动'" :bordered="false">
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="80" @submit.prevent="handleSubmit">
        <div class="flex flex-wrap items-end gap-4">
          <NFormItem label="运动项目" path="itemId" class="min-w-[200px] flex-1">
            <NSelect
              v-model:value="form.itemId"
              filterable
              placeholder="搜索运动项目"
              :loading="itemsLoading"
              :options="exerciseItemOptions"
              @update:value="onItemChange"
            />
          </NFormItem>
          <NFormItem label="时长(分)" path="durationMinutes">
            <NInputNumber v-model:value="form.durationMinutes" :min="1" :max="480" class="w-[120px]" />
          </NFormItem>
          <NFormItem>
            <NButton type="primary" attr-type="submit" :loading="submitting">提交记录</NButton>
          </NFormItem>
        </div>
      </NForm>
    </NCard>

    <!-- Today Stats -->
    <div v-if="todayTotal > 0 || todayDuration > 0" class="flex gap-5">
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ todayTotal }}</div>
        <div class="text-[13px] text-secondary">今日总消耗 (kcal)</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ todayDuration }}</div>
        <div class="text-[13px] text-secondary">今日总时长 (分钟)</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ records.length }}</div>
        <div class="text-[13px] text-secondary">今日运动次数</div>
      </NCard>
    </div>

    <!-- AI Exercise Guidance -->
    <NCard v-if="selectedItem" :bordered="false">
      <template #header>
        <div class="flex items-center justify-between">
          <span>AI运动指导 - {{ selectedItem.name }}</span>
          <NButton size="small" type="primary" :loading="guidanceLoading" @click="handleGetGuidance">
            {{ guidance ? '刷新指导' : '获取AI指导' }}
          </NButton>
        </div>
      </template>

      <div v-if="guidance" class="flex flex-col gap-4">
        <div v-if="guidance.basicInfo">
          <h4 class="mb-2 border-b border-[#21262d] pb-1 text-sm text-[#58a6ff]">基本信息</h4>
          <div class="flex flex-wrap gap-4 text-[13px]">
            <span><span class="text-secondary">类型</span>：{{ guidance.basicInfo.type }}</span>
            <span><span class="text-secondary">目标肌群</span>：{{ guidance.basicInfo.targetMuscle }}</span>
            <span><span class="text-secondary">难度</span>：<NTag size="small">{{ guidance.basicInfo.difficulty }}</NTag></span>
            <span><span class="text-secondary">热量消耗</span>：~{{ selectedItem.caloriesPerUnit }}kcal/{{ selectedItem.unit }}</span>
          </div>
        </div>
        <div v-if="guidance.breathing">
          <h4 class="mb-2 border-b border-[#21262d] pb-1 text-sm text-[#58a6ff]">呼吸节奏</h4>
          <p class="text-[13px] leading-relaxed">{{ guidance.breathing }}</p>
        </div>
        <div v-if="guidance.steps?.length">
          <h4 class="mb-2 border-b border-[#21262d] pb-1 text-sm text-[#58a6ff]">动作要领</h4>
          <ul class="flex flex-col gap-2">
            <li v-for="(step, i) in guidance.steps" :key="i" class="flex items-start gap-2.5 text-[13px] leading-relaxed">
              <span class="flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#58a6ff] to-[#7c3aed] text-[11px] font-bold text-white">{{ Number(i) + 1 }}</span>
              {{ step }}
            </li>
          </ul>
        </div>
        <div v-if="guidance.commonMistakes?.length">
          <h4 class="mb-2 border-b border-[#21262d] pb-1 text-sm text-[#58a6ff]">常见错误</h4>
          <ul class="flex flex-col gap-2">
            <li v-for="(m, i) in guidance.commonMistakes" :key="i" class="flex items-start gap-2 text-[13px] leading-relaxed">
              <NTag type="error" size="small">错误{{ Number(i) + 1 }}</NTag> {{ m }}
            </li>
          </ul>
        </div>
        <div v-if="guidance.tips">
          <h4 class="mb-2 border-b border-[#21262d] pb-1 text-sm text-[#58a6ff]">小贴士</h4>
          <p class="text-[13px] leading-relaxed">{{ guidance.tips }}</p>
        </div>
      </div>
      <NEmpty v-else description="点击按钮获取AI运动指导" size="small" />
    </NCard>

    <!-- Records Table -->
    <NCard :bordered="false">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('exercise.records') || '运动记录' }}</span>
          <NPagination v-model:page="page" :page-count="Math.ceil(total / 10)" :page-size="10" size="small" @update:page="loadRecords" />
        </div>
      </template>

      <NEmpty v-if="records.length === 0 && !recordsLoading" :description="$t('exercise.empty') || '还没有运动记录，使用上方的快捷录入记录你的第一次运动'" />

      <NDataTable
        v-else
        :data="records"
        :columns="tableColumns"
        :loading="recordsLoading"
        :bordered="false"
        striped
      />
    </NCard>

    <!-- Edit Modal -->
    <NModal
      v-model:show="editModalVisible"
      preset="card"
      title="编辑运动记录"
      class="w-[500px]"
      :mask-closable="false"
    >
      <NForm ref="editFormRef" :model="editForm" :rules="editFormRules" label-placement="left" label-width="80">
        <NFormItem label="运动项目" path="itemId">
          <NSelect
            v-model:value="editForm.itemId"
            filterable
            placeholder="搜索运动项目"
            :options="exerciseItemOptions"
          />
        </NFormItem>
        <NFormItem label="时长(分)" path="durationMinutes">
          <NInputNumber v-model:value="editForm.durationMinutes" :min="1" :max="480" class="w-full" />
        </NFormItem>
        <NFormItem label="消耗热量" path="caloriesBurned">
          <NInputNumber v-model:value="editForm.caloriesBurned" :min="0" :max="10000" class="w-full" />
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
import { useMessage, useDialog, NCard, NForm, NFormItem, NSelect, NInputNumber, NButton, NTag, NPagination, NDataTable, NEmpty, NModal } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { fetchGetExerciseItems, fetchSubmitExerciseRecord, fetchGetExerciseRecordsPage, fetchGetExerciseGuidance, fetchGetTodayCheckin, fetchDeleteExerciseRecord, fetchUpdateExerciseRecord } from '@/service/api';

defineOptions({ name: 'ExerciseRecord' });
const message = useMessage();
const dialog = useDialog();

interface ExerciseItem {
  id: number;
  name: string;
  caloriesPerUnit?: number;
  unit?: string;
  exerciseType?: string;
}

const exerciseItems = ref<ExerciseItem[]>([]);
const itemsLoading = ref(false);
const records = ref<any[]>([]);
const recordsLoading = ref(false);
const submitting = ref(false);
const guidanceLoading = ref(false);
const guidance = ref<any>(null);
const selectedItem = ref<ExerciseItem | null>(null);
const page = ref(1);
const total = ref(0);
const formRef = ref();

// Edit state
const editModalVisible = ref(false);
const editSubmitting = ref(false);
const editFormRef = ref();
const editingId = ref<number | null>(null);
const editForm = ref({
  itemId: null as number | null,
  durationMinutes: 30,
  caloriesBurned: 0
});

const editFormRules = {
  itemId: { type: 'number' as const, required: true, message: '请选择运动项目', trigger: 'change' },
  durationMinutes: { type: 'number' as const, required: true, message: '请输入时长', trigger: 'blur' }
};

const form = ref({
  itemId: null as number | null,
  durationMinutes: 30
});

const formRules = {
  itemId: { type: 'number' as const, required: true, message: '请选择运动项目', trigger: 'change' },
  durationMinutes: { type: 'number' as const, required: true, message: '请输入时长', trigger: 'blur' }
};

const exerciseItemOptions = computed(() =>
  exerciseItems.value.map(item => ({
    label: `${item.name} (${item.caloriesPerUnit || 0}kcal/${item.unit || '次'})`,
    value: item.id
  }))
);

const todayTotal = computed(() =>
  records.value.reduce((sum: number, r: any) => sum + (r.caloriesBurned || 0), 0)
);

const todayDuration = computed(() =>
  records.value.reduce((sum: number, r: any) => sum + (r.durationMinutes || r.duration || 0), 0)
);

function handleDelete(id: number) {
  dialog.warning({
    title: '确认删除',
    content: '确定要删除此记录吗？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      const { error } = await fetchDeleteExerciseRecord(id);
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
    itemId: row.itemId || row.exerciseItemId || null,
    durationMinutes: row.durationMinutes || row.duration || 30,
    caloriesBurned: row.caloriesBurned || 0
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
    const { error } = await fetchUpdateExerciseRecord(editingId.value, {
      itemId: editForm.value.itemId,
      durationMinutes: editForm.value.durationMinutes,
      caloriesBurned: editForm.value.caloriesBurned
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
  { title: '运动项目', key: 'exerciseItemName', minWidth: 120 },
  {
    title: '运动类型', key: 'exerciseType', width: 100,
    render: (row: any) => h(NTag, { size: 'small' }, { default: () => row.exerciseType || '-' })
  },
  { title: '时长(分钟)', key: 'durationMinutes', width: 100, render: (row: any) => row.durationMinutes || row.duration || '-' },
  { title: '消耗热量', key: 'caloriesBurned', width: 100, render: (row: any) => h('b', {}, `${row.caloriesBurned} kcal`) },
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

function onItemChange(val: number) {
  selectedItem.value = exerciseItems.value.find(i => i.id === val) || null;
  guidance.value = null;
}

async function handleGetGuidance() {
  if (!selectedItem.value) return;
  guidanceLoading.value = true;
  try {
    const { data } = await fetchGetExerciseGuidance(selectedItem.value.id);
    if (data) {
      guidance.value = data;
    }
  } finally {
    guidanceLoading.value = false;
  }
}

async function loadItems() {
  itemsLoading.value = true;
  try {
    const { data } = await fetchGetExerciseItems();
    exerciseItems.value = data || [];
  } finally {
    itemsLoading.value = false;
  }
}

async function loadRecords() {
  recordsLoading.value = true;
  try {
    const { data } = await fetchGetExerciseRecordsPage({ page: page.value, size: 10 });
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

    const exercise = selectedItem.value;
    const caloriesBurned = exercise
      ? Math.round((exercise.caloriesPerUnit || 0) * form.value.durationMinutes / 60)
      : 0;

    const { error } = await fetchSubmitExerciseRecord({
      itemId: form.value.itemId,
      durationMinutes: form.value.durationMinutes,
      caloriesBurned,
      checkinId
    } as any);
    if (!error) {
      message.success('记录成功');
      form.value.durationMinutes = 30;
      form.value.itemId = null;
      selectedItem.value = null;
      loadRecords();
    }
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  loadItems();
  loadRecords();
});
</script>
