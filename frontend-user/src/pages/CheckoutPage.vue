<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '../components/ui/BaseButton.vue'
import BaseInput from '../components/ui/BaseInput.vue'
import BaseCard from '../components/ui/BaseCard.vue'
import http from '../services/http'

const route = useRoute()
const router = useRouter()
const product = ref<any>(null)
const targetAccount = ref('')
const submitting = ref(false)

onMounted(async () => {
  const { data } = await http.get(`/products/${route.params.id}`)
  product.value = data.data
})

async function submit() {
  submitting.value = true
  try {
    const { data } = await http.post('/orders', {
      productId: Number(route.params.id),
      fields: { target_account: targetAccount.value },
    })
    router.push(`/payment/${data.data.orderNo}`)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <template v-if="product">
      <header class="page-header">
        <h2>确认订购</h2>
        <p>{{ product.name }} · ¥{{ product.salePrice }}</p>
      </header>

      <BaseCard class="checkout-card">
        <form class="checkout-form" @submit.prevent="submit">
          <BaseInput
            v-model="targetAccount"
            label="AI 账号邮箱"
            type="email"
            placeholder="your@email.com"
          />
          <p class="hint" role="note">请勿填写第三方服务密码</p>
          <BaseButton type="submit" block :disabled="submitting">
            {{ submitting ? '提交中…' : '提交订单' }}
          </BaseButton>
        </form>
      </BaseCard>
    </template>
  </div>
</template>

<style scoped>
.checkout-card {
  max-width: 480px;
}

.checkout-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.hint {
  font-size: 0.8125rem;
  color: var(--color-warning);
  background: var(--color-warning-bg);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
}
</style>
