<script setup>
import { useRoute } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'

const route = useRoute()
</script>

<template>
  <div class="ph screen">
    <h1 class="jr-h1">{{ route.meta.title || '준비 중' }}</h1>
    <div class="box">
      <span class="ic"><AppIcon name="sparkle" :size="30" /></span>
      <p class="t">곧 만나요</p>
      <p class="d">{{ route.meta.note || '준비 중인 기능이에요.' }}</p>
    </div>
  </div>
</template>

<style scoped>
.ph { padding-top: 18px; }
.box {
  margin-top: 24px; padding: 40px 24px; border-radius: var(--r-card);
  background: var(--surface); box-shadow: var(--shadow-sm); text-align: center;
}
.ic {
  width: 64px; height: 64px; border-radius: 20px; margin: 0 auto 14px;
  background: var(--brand-100); color: var(--brand-700);
  display: flex; align-items: center; justify-content: center;
}
.t { font-size: 18px; font-weight: 800; margin: 0; }
.d { font-size: 14px; color: var(--text-sub); margin: 6px 0 0; }
</style>
