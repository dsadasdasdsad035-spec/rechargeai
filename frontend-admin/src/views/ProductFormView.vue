<script setup lang="ts">
import { ref } from 'vue'
import http from '../services/http'

const form = ref({
  name: '',
  salePrice: 0,
  periodDays: 30,
  currency: 'CNY',
  complianceNotice: '',
})

async function submit() {
  await http.post('/products', form.value)
  alert('产品已创建')
}
</script>

<template>
  <h2>新建产品</h2>
  <el-form label-width="100px" style="max-width: 480px">
    <el-form-item label="名称">
      <el-input v-model="form.name" />
    </el-form-item>
    <el-form-item label="售价">
      <el-input-number v-model="form.salePrice" :min="0" />
    </el-form-item>
    <el-form-item label="周期(天)">
      <el-input-number v-model="form.periodDays" :min="1" />
    </el-form-item>
    <el-form-item label="合规提示">
      <el-input v-model="form.complianceNotice" type="textarea" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submit">保存</el-button>
    </el-form-item>
  </el-form>
</template>
