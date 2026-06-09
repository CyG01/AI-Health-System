<template>
  <div class="admin-exercise-page">
    <div class="page-header">
      <h2>运动字典管理 ({{ items.length }}条)</h2>
      <el-button type="primary" @click="handleAdd">新增运动</el-button>
    </div>

    <el-table :data="items" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="运动名称" min-width="110" />
      <el-table-column prop="type" label="类型" width="80" />
      <el-table-column prop="calorieCoefficient" label="热量系数" width="90">
        <template #default="{ row }">{{ row.calorieCoefficient }} kcal/kg/h</template>
      </el-table-column>
      <el-table-column prop="targetMuscle" label="目标肌群" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.targetMuscle" size="small" effect="plain">{{ row.targetMuscle }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="70">
        <template #default="{ row }">
          <el-tag :type="row.difficulty === '高级' ? 'danger' : row.difficulty === '中级' ? 'warning' : 'success'" size="small">
            {{ row.difficulty || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="70">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该运动？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      :title="isEditing ? '编辑运动' : '新增运动'"
      v-model="dialogVisible"
      width="520px"
      @close="resetForm"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="运动名称" required>
          <el-input v-model="form.name" placeholder="如：慢跑" />
        </el-form-item>
        <el-form-item label="运动类型" required>
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="有氧" value="有氧" />
            <el-option label="无氧" value="无氧" />
            <el-option label="拉伸" value="拉伸" />
          </el-select>
        </el-form-item>
        <el-form-item label="热量系数" required>
          <el-input-number v-model="form.calorieCoefficient" :min="0" :precision="1" style="width: 100%" />
          <span class="form-hint">单位: kcal/kg/h</span>
        </el-form-item>
        <el-form-item label="目标肌群">
          <el-select v-model="form.targetMuscle" style="width: 100%" clearable>
            <el-option label="全身" value="全身" />
            <el-option label="胸" value="胸" />
            <el-option label="背" value="背" />
            <el-option label="腿" value="腿" />
            <el-option label="核心" value="核心" />
            <el-option label="肩" value="肩" />
            <el-option label="手臂" value="手臂" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度等级">
          <el-radio-group v-model="form.difficulty">
            <el-radio label="初级">初级</el-radio>
            <el-radio label="中级">中级</el-radio>
            <el-radio label="高级">高级</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="视频链接">
          <el-input v-model="form.videoUrl" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminExerciseItems, createExerciseItem, updateExerciseItem, deleteExerciseItem } from '@/api/admin'

const items = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)

const form = ref({
  id: null,
  name: '',
  type: '',
  calorieCoefficient: null,
  targetMuscle: '',
  difficulty: '初级',
  videoUrl: ''
})

async function loadItems() {
  loading.value = true
  try {
    const res = await getAdminExerciseItems()
    items.value = res.data || []
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEditing.value = false
  dialogVisible.value = true
}

function handleEdit(row) {
  isEditing.value = true
  Object.assign(form.value, {
    id: row.id,
    name: row.name,
    type: row.type,
    calorieCoefficient: row.calorieCoefficient,
    targetMuscle: row.targetMuscle || '',
    difficulty: row.difficulty || '初级',
    videoUrl: row.videoUrl || ''
  })
  dialogVisible.value = true
}

function resetForm() {
  form.value = { id: null, name: '', type: '', calorieCoefficient: null, targetMuscle: '', difficulty: '初级', videoUrl: '' }
}

async function handleSave() {
  if (!form.value.name || !form.value.type || !form.value.calorieCoefficient) {
    ElMessage.warning('请填写必填项')
    return
  }
  saving.value = true
  try {
    if (isEditing.value) {
      await updateExerciseItem(form.value)
      ElMessage.success('更新成功')
    } else {
      await createExerciseItem(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadItems()
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteExerciseItem(id)
    ElMessage.success('删除成功')
    loadItems()
  } catch {
    // handled by interceptor
  }
}

onMounted(loadItems)
</script>

<style scoped>
.admin-exercise-page { padding: 20px 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }
.form-hint { font-size: 12px; color: #8b949e; margin-left: 8px; }
</style>