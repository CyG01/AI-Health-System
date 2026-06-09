<template>
  <div class="error-page">
    <div class="error-content">
      <h1>404</h1>
      <p class="desc">页面不存在</p>
      <p class="tip">请检查网址是否正确，或返回首页</p>
      <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
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
  color: #409eff;
  margin: 0;
  line-height: 1;
}
.desc {
  font-size: 20px;
  color: #606266;
  margin: 16px 0 8px;
}
.tip {
  font-size: 14px;
  color: #909399;
  margin-bottom: 24px;
}
</style>