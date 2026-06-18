<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listOrders, getOrderDetail } from '../services/orderApi'
import OrderDetailDrawer from '../components/OrderDetailDrawer.vue'

const orders = ref<any[]>([])
const selected = ref<any>(null)
const orderStatus = ref('')
const paymentStatus = ref('')

async function load() {
  const { data } = await listOrders({
    pageNo: 1, pageSize: 50,
    orderStatus: orderStatus.value || undefined,
    paymentStatus: paymentStatus.value || undefined,
  })
  orders.value = data.data.items
}

async function showDetail(orderNo: string) {
  const { data } = await getOrderDetail(orderNo)
  selected.value = data.data
}

onMounted(load)
</script>

<template>
  <h2>交易记录</h2>
  <div class="filters">
    <select v-model="orderStatus" @change="load">
      <option value="">全部订单状态</option>
      <option value="WAIT_PAY">待支付</option>
      <option value="PAID">已支付</option>
      <option value="CLOSED">已关闭</option>
    </select>
    <select v-model="paymentStatus" @change="load">
      <option value="">全部支付状态</option>
      <option value="UNPAID">未支付</option>
      <option value="PAID">已支付</option>
    </select>
  </div>

  <div v-if="orders.length === 0" class="empty">
    <p>暂无订单</p>
    <RouterLink to="/products">去订购</RouterLink>
  </div>

  <div v-else class="list">
    <div v-for="o in orders" :key="o.orderNo" class="item" @click="showDetail(o.orderNo)">
      <div class="row">
        <strong>{{ o.productName }}</strong>
        <span>¥{{ o.amount }}</span>
      </div>
      <div class="row sub">
        <span>{{ o.orderNo }}</span>
        <span>{{ o.orderStatus }} / {{ o.paymentStatus }}</span>
      </div>
    </div>
  </div>

  <OrderDetailDrawer :order="selected" @close="selected = null" />
</template>

<style scoped>
.filters { display: flex; gap: 8px; margin-bottom: 16px; }
.list { display: flex; flex-direction: column; gap: 8px; }
.item { border: 1px solid #eee; border-radius: 8px; padding: 12px; cursor: pointer; }
.row { display: flex; justify-content: space-between; }
.sub { color: #888; font-size: 12px; margin-top: 4px; }
.empty { text-align: center; padding: 40px; color: #888; }
</style>