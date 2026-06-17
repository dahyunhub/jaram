<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, ApiError } from '../lib/api'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import Logo from '../components/Logo.vue'
import AppIcon from '../components/AppIcon.vue'

const router = useRouter()
const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!email.value || !password.value) {
    error.value = '이메일과 비밀번호를 입력해 주세요.'
    return
  }
  loading.value = true
  try {
    const res = await api.post('/auth/login', { email: email.value, password: password.value }, { auth: false })
    auth.setSession({ accessToken: res.accessToken, teacher: res.teacher })
    router.replace({ name: session.classroom ? 'home' : 'classrooms' })
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '로그인 중 문제가 발생했어요.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login screen">
    <div class="brand">
      <Logo :size="64" />
      <div class="jr-logo brand-name">자람</div>
      <div class="jr-body brand-sub">오늘도 우리 반, 함께해요 ☀️</div>
    </div>

    <form class="form" @submit.prevent="submit">
      <div>
        <label class="jr-field-label">이메일</label>
        <input v-model="email" class="jr-input" type="email" placeholder="teacher@jaram.dev" autocomplete="username" />
      </div>
      <div>
        <label class="jr-field-label">비밀번호</label>
        <input v-model="password" class="jr-input" type="password" placeholder="비밀번호를 입력해주세요" autocomplete="current-password" />
      </div>

      <p v-if="error" class="err">{{ error }}</p>

      <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" type="submit" :disabled="loading">
        <template v-if="!loading">로그인 <AppIcon name="chevR" :size="20" /></template>
        <template v-else>로그인 중…</template>
      </button>
    </form>
  </div>
</template>

<style scoped>
.login { display: flex; flex-direction: column; justify-content: center; min-height: 100%; padding-top: 24px; padding-bottom: 24px; }
.brand { text-align: center; padding: 8px 0 30px; }
.brand-name { font-size: 38px; margin-top: 12px; }
.brand-sub { color: var(--text-sub); margin-top: 6px; }
.form { display: flex; flex-direction: column; gap: 16px; }
.err { color: var(--warn); font-size: 13.5px; font-weight: 600; margin: -4px 2px 0; }
</style>
