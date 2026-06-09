<template>
  <div class="admin-food-page">
    <div class="page-header">
      <h2>食物字典管理</h2>
      <el-button type="primary" @click="handleAdd">新增食物</el-button>
    </div>

    <el-table :data="items" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="食物名称" min-width="120" />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column label="每单位热量" width="130">
        <template #default="{ row }">{{ row.caloriesPerUnit }} kcal / {{ row.unit }}</template>
      </el-table-column>
      <el-table-column label="营养素 (g)" min-width="180">
        <template #default="{ row }">
          蛋白质{{ row.protein ?? '-' }} / 脂肪{{ row.fat ?? '-' }} / 碳水{{ row.carbs ?? '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该食物？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="isEditing ? '编辑食物' : '新增食物'"
      v-model="dialogVisible"
      width="500px"
      @close="resetForm"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="食物名称" required>
          <el-input v-model="form.name" placeholder="如：米饭" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" style="width: 100%">
            <el-option label="主食" value="主食" />
            <el-option label="肉类" value="肉类" />
            <el-option label="蔬菜" value="蔬菜" />
            <el-option label="水果" value="水果" />
            <el-option label="乳制品" value="乳制品" />
            <el-option label="零食" value="零食" />
            <el-option label="饮品" value="饮品" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="每单位热量" required>
          <el-input-number v-model="form.caloriesPerUnit" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位" required>
          <el-input v-model="form.unit" placeholder="如：100g、碗、个" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="蛋白质(g)">
              <el-input-number v-model="form.protein" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="脂肪(g)">
              <el-input-number v-model="form.fat" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="碳水(g)">
              <el-input-number v-model="form.carbs" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { getAdminFoodItems, createFoodItem, updateFoodItem, deleteFoodItem } from '@/api/admin'

const items = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEditing = ref(false)

const form = ref({
  id: null,
  name: '',
  category: '',
  caloriesPerUnit: null,
  unit: '',
  protein: null,
  fat: null,
  carbs: null
})

async function loadItems() {
  loading.value = true
  try {
    const res = await getAdminFoodItems()
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
  Object.assign(form.value, row)
  dialogVisible.value = true
}

function resetForm() {
  form.value = { id: null, name: '', category: '', caloriesPerUnit: null, unit: '', protein: null, fat: null, carbs: null }
}

async function handleSave() {
  if (!form.value.name || !form.value.category || !form.value.caloriesPerUnit || !form.value.unit) {
    ElMessage.warning('请填写必填项')
    return
  }
  saving.value = true
  try {
    if (isEditing.value) {
      await updateFoodItem(form.value)
      ElMessage.success('更新成功')
    } else {
      await createFoodItem(form.value)
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
    await deleteFoodItem(id)
    ElMessage.success('删除成功')
    loadItems()
  } catch {
    // handled by interceptor
  }
}

onMounted(loadItems)
</script>

<style scoped>
.admin-food-page { padding: 20px 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }
</style>