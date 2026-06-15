<template>
  <div class="error-page">
    <div class="error-content">
      <h1>500</h1>
      <p class="desc">服务器异常</p>
      <p class="tip">系统繁忙，请稍后重试</p>
      <el-button type="primary" @click="$router.go(0)">刷新页面</el-button>
      <el-button @click="$router.push('/dashboard')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
}
.error-content h1 {
  font-size: 80px;
  color: var(--color-danger);
  margin: 0;
  line-height: 1;
}
.desc {
  font-size: var(--text-xl);
  color: var(--text-primary);
  margin: var(--space-base) 0 var(--space-sm);
}
.tip {
  font-size: var(--text-base);
  color: var(--text-secondary);
  margin-bottom: var(--space-xl);
}
</style>