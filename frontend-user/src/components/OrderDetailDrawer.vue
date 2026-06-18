<script setup lang="ts">
defineProps<{
  order: {
    orderNo: string
    productName: string
    amount: number
    targetAccountMasked: string
    orderStatus: string
    paymentStatus: string
  } | null
}>()

const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <div v-if="order" class="drawer">
    <h3>订单详情</h3>
    <p>订单号：{{ order.orderNo }}</p>
    <p>产品：{{ order.productName }}</p>
    <p>金额：¥{{ order.amount }}</p>
    <p>AI 账号：{{ order.targetAccountMasked }}</p>
    <p>状态：{{ order.orderStatus }} / {{ order.paymentStatus }}</p>
    <button @click="emit('close')">关闭</button>
  </div>
</template>

<style scoped>
.drawer { margin-top: 16px; border: 1px solid #ddd; border-radius: 8px; padding: 16px; background: #fafafa; }
</style>
