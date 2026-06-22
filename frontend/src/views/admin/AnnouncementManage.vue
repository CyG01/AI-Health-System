<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NTag,
  NPagination,
  NSpace,
  NPopconfirm,
  useMessage,
  useDialog
} from 'naive-ui';
import type { DataTableColumns, FormInst, FormRules, PaginationProps, SelectOption } from 'naive-ui';
import {
  fetchGetAdminAnnouncementList,
  fetchCreateAnnouncement,
  fetchUpdateAnnouncement,
  fetchDeleteAnnouncement,
  fetchPublishAnnouncement
} from '@/service/api';

defineOptions({ name: 'AdminAnnouncementManage' });

const message = useMessage();
const dialog = useDialog();

interface AnnouncementRow {
  id: number;
  title: string;
  content: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

interface AnnouncementQuery {
  keyword: string;
  status: string | null;
}

const statusOptions: SelectOption[] = [
  { label: '全部状态', value: '' },
  { label: '已发布', value: '1' },
  { label: '草稿', value: '0' }
];

const query = reactive<AnnouncementQuery>({
  keyword: '',
  status: null
});

// Table columns
const columns: DataTableColumns<AnnouncementRow> = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '标题', key: 'title', minWidth: 200, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      return h(
        NTag,
        { type: row.status === '1' ? 'success' : 'info', size: 'small', bordered: false },
        { default: () => (row.status === '1' ? '已发布' : '草稿') }
      );
    }
  },
  { title: '发布时间', key: 'createdAt', minWidth: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    fixed: 'right',
    render(row) {
      const buttons = [
        h(
          NButton,
          { size: 'small', type: 'primary', text: true, onClick: () => openEditDialog(row) },
          { default: () => '编辑' }
        )
      ];
      if (row.status !== '1') {
        buttons.push(
          h(
            NButton,
            { size: 'small', type: 'success', text: true, onClick: () => handlePublish(row) },
            { default: () => '发布' }
          )
        );
      }
      buttons.push(
        h(
          NPopconfirm,
          { onPositiveClick: () => handleDelete(row) },
          {
            trigger: () =>
              h(NButton, { size: 'small', type: 'error', text: true }, { default: () => '删除' }),
            default: () => `确定要删除公告「${row.title}」吗？此操作不可恢复。`
          }
        )
      );
      return h(NSpace, null, { default: () => buttons });
    }
  }
];

// Data
const loading = ref(false);
const tableData = ref<AnnouncementRow[]>([]);
const pagination = reactive<PaginationProps>({
  page: 1,
  pageSize: 10,
  pageCount: 1,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onUpdatePageSize: (size: number) => {
    pagination.pageSize = size;
    pagination.page = 1;
    fetchData();
  }
});

// Modal
const showModal = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
const editingId = ref<number | null>(null);
const formRef = ref<FormInst | null>(null);
const form = reactive({
  title: '',
  content: ''
});
const rules: FormRules = {
  title: { required: true, message: '请输入标题', trigger: 'blur' },
  content: { required: true, message: '请输入内容', trigger: 'blur' }
};

async function fetchData() {
  loading.value = true;
  try {
    const res = await fetchGetAdminAnnouncementList({
      page: pagination.page,
      size: pagination.pageSize,
      status: query.status || undefined
    });
    if (res.data) {
      tableData.value = res.data.records || [];
      const total: number = res.data.total || 0;
      pagination.pageCount = Math.max(1, Math.ceil(total / (pagination.pageSize as number)));
    }
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.title = '';
  form.content = '';
  editingId.value = null;
}

function openCreateDialog() {
  isEdit.value = false;
  resetForm();
  showModal.value = true;
}

function openEditDialog(row: AnnouncementRow) {
  isEdit.value = true;
  editingId.value = row.id;
  form.title = row.title;
  form.content = row.content;
  showModal.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    if (isEdit.value) {
      await fetchUpdateAnnouncement({
        id: editingId.value!,
        title: form.title,
        content: form.content
      });
      message.success('公告已更新');
    } else {
      await fetchCreateAnnouncement({
        title: form.title,
        content: form.content
      });
      message.success('公告已创建');
    }
    showModal.value = false;
    resetForm();
    fetchData();
  } finally {
    submitting.value = false;
  }
}

function handlePublish(row: AnnouncementRow) {
  dialog.info({
    title: '确认发布',
    content: `确定要发布公告「${row.title}」吗？发布后将向所有用户推送系统通知。`,
    positiveText: '发布',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await fetchPublishAnnouncement(row.id);
        message.success('公告已发布');
        fetchData();
      } catch {
        // handled by interceptor
      }
    }
  });
}

async function handleDelete(row: AnnouncementRow) {
  try {
    await fetchDeleteAnnouncement(row.id);
    message.success('公告已删除');
    fetchData();
  } catch {
    // handled by interceptor
  }
}

function handleSearch() {
  pagination.page = 1;
  fetchData();
}

function handlePageChange(page: number) {
  pagination.page = page;
  fetchData();
}

onMounted(() => {
  fetchData();
});
</script>

<template>
  <div class="flex-col-stretch gap-16px overflow-auto p-16px">
    <NCard title="公告管理">
      <template #header-extra>
        <NSpace align="center">
          <NInput
            v-model:value="query.keyword"
            placeholder="搜索公告标题"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
          <NSelect
            v-model:value="query.status"
            :options="statusOptions"
            placeholder="全部状态"
            clearable
            style="width: 130px"
            @update:value="handleSearch"
          />
          <NButton type="primary" @click="handleSearch">搜索</NButton>
          <NButton type="primary" @click="openCreateDialog">发布公告</NButton>
        </NSpace>
      </template>
      <NDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row: AnnouncementRow) => row.id"
        :scroll-x="800"
      />
      <div class="flex justify-end mt-16px">
        <NPagination
          v-model:page="pagination.page"
          :page-count="pagination.pageCount"
          :page-sizes="pagination.pageSizes"
          show-size-picker
          @update:page="handlePageChange"
        />
      </div>
    </NCard>

    <!-- Add/Edit Modal -->
    <NModal
      v-model:show="showModal"
      preset="dialog"
      :title="isEdit ? '编辑公告' : '发布公告'"
      style="width: 560px"
      :show-icon="false"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="top">
        <NFormItem label="标题" path="title">
          <NInput v-model:value="form.title" placeholder="请输入公告标题" :maxlength="200" show-count />
        </NFormItem>
        <NFormItem label="内容" path="content">
          <NInput
            v-model:value="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
            :maxlength="5000"
            show-count
          />
        </NFormItem>
      </NForm>
      <template #action>
        <NButton @click="showModal = false">取消</NButton>
        <NButton type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '发布' }}
        </NButton>
      </template>
    </NModal>
  </div>
</template>
