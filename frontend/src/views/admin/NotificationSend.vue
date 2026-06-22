<script setup lang="ts">
import { ref } from 'vue';
import {
  NButton,
  NCard,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NRadioGroup,
  NRadioButton,
  NAlert,
  NSpace,
  useMessage,
  useDialog
} from 'naive-ui';
import type { FormInst, FormRules, SelectOption } from 'naive-ui';
import { fetchSendAdminNotification, executeWithApproval } from '@/service/api';
import { useAuthStore } from '@/store/modules/auth';

defineOptions({ name: 'AdminNotificationSend' });

const message = useMessage();
const dialog = useDialog();
const authStore = useAuthStore();

const broadcastMode = ref(true);
const sending = ref(false);
const formRef = ref<FormInst | null>(null);

interface NotificationForm {
  userId: number | null;
  title: string;
  content: string;
  type: string;
}

const form = ref<NotificationForm>({
  userId: null,
  title: '',
  content: '',
  type: 'system'
});

const typeOptions: SelectOption[] = [
  { label: '系统通知', value: 'system' },
  { label: '提醒', value: 'reminder' },
  { label: '公告', value: 'announcement' }
];

const formRules: FormRules = {
  title: { required: true, message: '请输入通知标题', trigger: 'blur' },
  content: { required: true, message: '请输入通知内容', trigger: 'blur' }
};

async function handleSend() {
  if (formRef.value) {
    try {
      await formRef.value.validate();
    } catch {
      return;
    }
  }
  if (!broadcastMode.value && !form.value.userId) {
    message.warning('请输入目标用户ID');
    return;
  }

  const targetDesc = broadcastMode.value ? '全体用户' : `用户ID: ${form.value.userId}`;

  dialog.warning({
    title: '审批确认',
    content: `确定要发送通知「${form.value.title}」给${targetDesc}吗？此操作需要审批。`,
    positiveText: '发起审批并执行',
    negativeText: '取消',
    onPositiveClick: async () => {
      sending.value = true;
      try {
        const data: Record<string, any> = {
          title: form.value.title,
          content: form.value.content,
          type: form.value.type
        };
        if (!broadcastMode.value && form.value.userId) {
          data.userIds = [form.value.userId];
        }
        await executeWithApproval(
          'send_notification',
          `发送通知: ${form.value.title} -> ${targetDesc}`,
          (approvalId: string) => fetchSendAdminNotification(data as Api.Notification.SendParams, approvalId),
          authStore.userInfo?.id
        );
        message.success('通知发送成功');
        handleClear();
      } catch {
        // cancelled or error handled by interceptor
      } finally {
        sending.value = false;
      }
    }
  });
}

function handleClear() {
  form.value = { userId: null, title: '', content: '', type: 'system' };
}
</script>

<template>
  <div class="flex-col-stretch gap-16px overflow-auto p-16px">
    <NCard title="发送系统通知">
      <NForm
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-placement="left"
        label-width="120"
        style="max-width: 640px"
      >
        <NFormItem label="发送方式">
          <NRadioGroup v-model:value="broadcastMode">
            <NRadioButton :value="false">指定用户</NRadioButton>
            <NRadioButton :value="true">全体用户</NRadioButton>
          </NRadioGroup>
        </NFormItem>
        <NFormItem v-if="!broadcastMode" label="目标用户ID" path="userId">
          <NInputNumber
            v-model:value="form.userId"
            :min="1"
            placeholder="输入用户ID"
            style="width: 200px"
          />
        </NFormItem>
        <NFormItem label="通知标题" path="title">
          <NInput v-model:value="form.title" placeholder="输入通知标题" :maxlength="100" show-count />
        </NFormItem>
        <NFormItem label="通知内容" path="content">
          <NInput
            v-model:value="form.content"
            type="textarea"
            :rows="5"
            placeholder="输入通知内容"
            :maxlength="500"
            show-count
          />
        </NFormItem>
        <NFormItem label="通知类型">
          <NSelect v-model:value="form.type" :options="typeOptions" style="width: 200px" />
        </NFormItem>
        <NFormItem label=" ">
          <NSpace>
            <NButton type="primary" :loading="sending" size="large" @click="handleSend">
              发送通知
            </NButton>
            <NButton size="large" @click="handleClear">清空</NButton>
          </NSpace>
        </NFormItem>
      </NForm>
    </NCard>

    <NAlert title="提示" type="info">
      <ul style="margin: 0; padding-left: 20px; line-height: 1.8">
        <li>选择「全体用户」将向所有启用通知的用户发送消息</li>
        <li>选择「指定用户」需填写目标用户ID</li>
        <li>通知类型选择「提醒」可触发用户端推送</li>
      </ul>
    </NAlert>
  </div>
</template>
