<template>
  <div class="admin-notification-page">
    <div class="page-header">
      <h2>发送系统通知</h2>
    </div>

    <el-card shadow="hover">
      <el-form :model="form" label-width="120px" @submit.prevent="handleSend">
        <el-form-item label="发送方式" required>
          <el-radio-group v-model="broadcastMode">
            <el-radio :value="false">指定用户</el-radio>
            <el-radio :value="true">全体用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!broadcastMode" label="目标用户ID">
          <el-input v-model="form.userId" type="number" placeholder="输入用户ID" />
        </el-form-item>
        <el-form-item label="通知标题" required>
          <el-input v-model="form.title" placeholder="输入通知标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="通知内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="输入通知内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="通知类型">
          <el-select v-model="form.type" style="width: 200px">
            <el-option label="系统通知" value="system" />
            <el-option label="提醒" value="reminder" />
            <el-option label="公告" value="announcement" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="sending" size="large">
            发送通知
          </el-button>
          <el-button @click="handleClear">清空</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="tips" style="margin-top: 20px; color: #8c8c8c; font-size: 13px">
      提示：
      <ul>
        <li>选择「全体用户」将向所有启用通知的用户发送消息</li>
        <li>选择「指定用户」需填写目标用户ID</li>
        <li>通知类型选择「提醒」可触发用户端推送</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { sendAdminNotification } from '@/api/admin'

const broadcastMode = ref(true)
const sending = ref(false)

const form = ref({
  userId: null,
  title: '',
  content: '',
  type: 'system'
})

async function handleSend() {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  if (!broadcastMode.value && !form.value.userId) {
    ElMessage.warning('请输入目标用户ID')
    return
  }
  sending.value = true
  try {
    const data = { ...form.value }
    if (broadcastMode.value) {
      delete data.userId
    }
    await sendAdminNotification(data)
    ElMessage.success('通知发送成功')
    handleClear()
  } finally {
    sending.value = false
  }
}

function handleClear() {
  form.value = { userId: null, title: '', content: '', type: 'system' }
}
</script>

<style scoped>
.admin-notification-page { padding: 20px 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }
</style>