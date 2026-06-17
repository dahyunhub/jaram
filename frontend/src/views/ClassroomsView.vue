<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import Logo from '../components/Logo.vue'
import AppIcon from '../components/AppIcon.vue'

const router = useRouter()
const classrooms = ref([])
const loading = ref(true)
const error = ref('')
const selectedId = ref(null)

const nowYear = new Date().getFullYear()
function tagOf(year) {
  if (year === nowYear) return '올해'
  if (year === nowYear - 1) return '작년'
  return `${year}학년도`
}

const selected = computed(() => classrooms.value.find((c) => c.id === selectedId.value) || null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    classrooms.value = await api.get('/classrooms')
    if (classrooms.value.length) selectedId.value = classrooms.value[0].id
  } catch (e) {
    error.value = e.message || '반 목록을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

function start() {
  if (!selected.value) return
  session.select(selected.value)
  router.replace({ name: 'home' })
}

onMounted(load)
</script>

<template>
  <div class="cls screen">
    <div class="brand">
      <Logo :size="56" />
      <div class="jr-logo brand-name">자람</div>
      <div class="jr-body brand-sub">{{ auth.teacher?.name || '선생님' }}, 환영해요 ☀️</div>
    </div>

    <div class="label">담당 반을 선택해 주세요</div>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="error" class="err">{{ error }}</p>
    <p v-else-if="!classrooms.length" class="muted">담당하는 반이 아직 없어요.</p>

    <div v-else class="list">
      <button
        v-for="c in classrooms" :key="c.id"
        class="card" :class="{ on: c.id === selectedId }"
        @click="selectedId = c.id"
      >
        <span class="ic" :class="{ on: c.id === selectedId }">
          <AppIcon :name="tagOf(c.year) === '올해' ? 'sun' : 'leaf'" :size="26" />
        </span>
        <span class="info">
          <span class="row">
            <span class="name">{{ c.name }}</span>
            <span class="tag" :class="{ now: tagOf(c.year) === '올해' }">{{ tagOf(c.year) }}</span>
          </span>
          <span class="sub">{{ c.year }}학년도 · 원아 {{ c.childCount }}명</span>
        </span>
        <AppIcon v-if="c.id === selectedId" name="check" :size="22" :stroke="2.6" class="chk" />
      </button>
    </div>

    <button
      v-if="!loading && classrooms.length"
      class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg start"
      @click="start"
    >
      {{ selected ? selected.name + '으로 시작하기' : '시작하기' }} <AppIcon name="chevR" :size="20" />
    </button>
  </div>
</template>

<style scoped>
.cls { display: flex; flex-direction: column; min-height: 100%; padding-top: 24px; padding-bottom: 28px; }
.brand { text-align: center; padding: 20px 0 24px; }
.brand-name { font-size: 36px; margin-top: 12px; }
.brand-sub { color: var(--text-sub); margin-top: 6px; }
.label { font-size: 14px; font-weight: 800; color: var(--text-sub); margin-bottom: 14px; }
.list { display: flex; flex-direction: column; gap: 12px; flex: 1; }
.card {
  display: flex; align-items: center; gap: 14px; padding: 16px 18px; border-radius: 18px; cursor: pointer;
  background: var(--surface); border: 2px solid var(--hair-strong); box-shadow: var(--shadow-sm);
  font-family: inherit; text-align: left; width: 100%; transition: all .14s;
}
.card.on { background: var(--brand-100); border-color: var(--brand-500); box-shadow: 0 6px 18px rgba(245, 185, 64, .25); }
.ic {
  width: 52px; height: 52px; border-radius: 16px; flex: 0 0 auto;
  display: flex; align-items: center; justify-content: center;
  background: var(--surface-soft); color: var(--text-sub);
}
.ic.on { background: var(--brand-300); color: #9A6B12; }
.info { flex: 1; min-width: 0; }
.row { display: flex; align-items: center; gap: 8px; }
.name { font-size: 18px; font-weight: 800; }
.tag {
  font-size: 11px; font-weight: 700; padding: 4px 9px; border-radius: 999px;
  background: var(--surface-soft); color: var(--text-faint);
}
.tag.now { background: rgba(127, 209, 174, .22); color: #3C8F62; }
.sub { display: block; font-size: 13px; color: var(--text-sub); margin-top: 3px; font-weight: 600; }
.chk { color: var(--brand-700); flex: 0 0 auto; }
.start { margin-top: 18px; }
.muted { color: var(--text-sub); }
.err { color: var(--warn); font-weight: 600; }
</style>
