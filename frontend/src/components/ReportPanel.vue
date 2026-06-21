<script setup>
// 개인 관찰평가 패널(Epic 4) — 아이별. 목록·생성(실제 AI)·상세.
// 백엔드: GET /children/{id}/reports · POST /children/{id}/reports · GET /reports/{id}
import { ref, computed, onMounted } from 'vue'
import { api, ApiError } from '../lib/api'
import { areaMeta } from '../lib/areas'
import AppIcon from './AppIcon.vue'
import NuriChip from './NuriChip.vue'
import SproutLoader from './SproutLoader.vue'

const props = defineProps({
  childId: { type: Number, required: true },
  childName: { type: String, default: '아이' },
})

// view: list | generating | detail
const view = ref('list')
const reports = ref([])
const detail = ref(null)
const lastDetailId = ref(null)
const loading = ref(true)
const error = ref('')
const showCreate = ref(false)

const todayLabel = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric' }).format(new Date())

function fmtDate(iso) {
  if (!iso) return ''
  const [y, m, d] = iso.split('-')
  return `${Number(m)}월 ${Number(d)}일`
}
function periodLabel(r) { return `${fmtDate(r.periodStart)} ~ ${fmtDate(r.periodEnd)}` }
function isAuto(r) { return r.reportType === 'MONTHLY' }
function title(r) { return isAuto(r) ? `${r.reportMonth} 월말 평가` : '개인 관찰평가' }

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    reports.value = await api.get(`/children/${props.childId}/reports`)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '평가 목록을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

async function openDetail(id) {
  view.value = 'detail'
  lastDetailId.value = id
  detail.value = null
  error.value = ''
  try {
    detail.value = await api.get(`/reports/${id}`)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '평가를 불러오지 못했어요.'
  }
}

function backToList() { view.value = 'list'; detail.value = null; error.value = '' }

async function generate() {
  showCreate.value = false
  view.value = 'generating'
  error.value = ''
  try {
    const res = await api.post(`/children/${props.childId}/reports`)
    await loadList()
    await openDetail(res.id)
  } catch (e) {
    view.value = 'list'
    if (e instanceof ApiError && e.code === 'REPORT_NO_MEMO') {
      error.value = '직전 평가 이후 새로 쌓인 메모가 없어요. 메모가 쌓이면 평가를 만들 수 있어요.'
    } else if (e instanceof ApiError && e.code === 'ANALYSIS_IN_PROGRESS') {
      error.value = '이미 분석이 진행 중이에요. 잠시 후 다시 시도해 주세요.'
    } else {
      error.value = e instanceof ApiError ? e.message : '평가 생성 중 문제가 발생했어요.'
    }
  }
}

const detailAreas = computed(() => detail.value?.content?.areas || [])

onMounted(loadList)
</script>

<template>
  <div class="rpnl">
    <!-- 헤더 -->
    <div class="rp-head">
      <span class="rp-ic"><AppIcon name="me" :size="17" /></span>
      <span class="rp-title">개인 관찰평가</span>
      <span v-if="view === 'list'" class="rp-note">시간순 누적</span>
      <button v-if="view === 'list'" class="jr-btn jr-btn--primary jr-btn--sm rp-gen" @click="showCreate = true">
        <AppIcon name="plus" :size="16" :stroke="2.6" /> 생성
      </button>
      <button v-else-if="view === 'detail'" class="rp-back" @click="backToList"><AppIcon name="back" :size="18" /> 목록</button>
    </div>

    <!-- 생성 중 -->
    <div v-if="view === 'generating'" class="rp-loading">
      <SproutLoader :size="84" />
      <div class="rp-l-t">{{ childName }} 관찰 평가를<br>만들고 있어요</div>
      <div class="rp-l-d">잠시만요 — 약 15초 걸려요 ☀️</div>
    </div>

    <!-- 상세 -->
    <template v-else-if="view === 'detail'">
      <div v-if="error" class="rp-banner">
        <span><AppIcon name="heart" :size="16" :stroke="2" /> {{ error }}</span>
        <button class="rp-retry" @click="openDetail(lastDetailId)"><AppIcon name="swap" :size="14" /> 다시 시도</button>
      </div>
      <template v-else-if="detail">
        <div class="rp-meta">
          <span class="rp-kind" :class="isAuto(detail) ? 'auto' : 'manual'">{{ isAuto(detail) ? '월말 자동' : '수동 생성' }}</span>
          <span class="rp-period">{{ periodLabel(detail) }}</span>
        </div>
        <div class="jr-area-block rp-summary" style="--strip: var(--brand-500)">
          <div class="rp-sum-t">요약</div>
          <div class="rp-text">{{ detail.content?.summary }}</div>
        </div>
        <div v-for="a in detailAreas" :key="a.area" class="jr-area-block" :style="{ '--strip': areaMeta(a.area).color }">
          <div class="rp-block-top"><NuriChip :area="a.area" /></div>
          <div class="rp-text">{{ a.text }}</div>
        </div>
      </template>
      <p v-else class="rp-muted">불러오는 중…</p>
    </template>

    <!-- 목록 -->
    <template v-else>
      <div v-if="error" class="rp-banner"><span><AppIcon name="heart" :size="16" :stroke="2" /> {{ error }}</span></div>
      <p v-if="loading" class="rp-muted">불러오는 중…</p>
      <template v-else>
        <div v-if="reports.length" class="rp-list">
          <button v-for="r in reports" :key="r.id" class="rp-item" @click="openDetail(r.id)">
            <span class="rp-item-ic"><AppIcon name="journal" :size="18" /></span>
            <span class="rp-item-body">
              <span class="rp-item-top">
                <span class="rp-item-title">{{ title(r) }}</span>
                <span class="rp-kind" :class="isAuto(r) ? 'auto' : 'manual'">{{ isAuto(r) ? '월말 자동' : '수동 생성' }}</span>
              </span>
              <span class="rp-item-sub">{{ periodLabel(r) }}</span>
            </span>
            <AppIcon name="chevR" :size="18" style="color:var(--text-faint);flex:0 0 auto" />
          </button>
        </div>
        <div v-else-if="!error" class="rp-empty">
          <span class="rp-empty-ic"><AppIcon name="me" :size="22" /></span>
          <div class="rp-empty-t">아직 만든 평가가 없어요</div>
          <div class="rp-empty-d">위 <b>생성</b>을 눌러 {{ childName }}의 첫 관찰평가를 만들어 보세요.</div>
        </div>
      </template>
    </template>

    <!-- 생성 확인 모달 -->
    <Transition name="fade">
      <div v-if="showCreate" class="rp-mask" @click.self="showCreate = false">
        <div class="rp-modal">
          <div class="rp-m-head">
            <div class="rp-m-title">{{ childName }} 개인평가 만들기</div>
            <button class="rp-m-x" @click="showCreate = false"><AppIcon name="x" :size="18" /></button>
          </div>
          <div class="rp-m-desc">직전 평가 이후부터 오늘({{ todayLabel }})까지의 관찰 기록을 모아 분석해요.</div>
          <div class="rp-m-note"><AppIcon name="check" :size="15" :stroke="2.6" style="color:var(--success)" /> 새 평가는 기존 평가를 덮어쓰지 않고 따로 쌓여요.</div>
          <div class="rp-m-actions">
            <button class="jr-btn jr-btn--ghost" @click="showCreate = false">취소</button>
            <button class="jr-btn jr-btn--primary" style="flex:1" @click="generate"><AppIcon name="sparkle" :size="19" /> 평가 생성</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.rpnl { background: var(--surface); border-radius: 18px; box-shadow: var(--shadow-sm); padding: 16px 18px; }
.rp-head { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.rp-ic { width: 28px; height: 28px; border-radius: 9px; background: var(--brand-300); color: var(--text); display: flex; align-items: center; justify-content: center; flex: 0 0 auto; }
.rp-title { font-size: 14.5px; font-weight: 800; white-space: nowrap; }
.rp-note { font-size: 11.5px; color: var(--text-faint); font-weight: 600; }
.rp-gen { margin-left: auto; flex: 0 0 auto; }
.rp-back { margin-left: auto; display: flex; align-items: center; gap: 4px; border: none; background: transparent; color: var(--text-sub); font-family: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }

.rp-list { display: flex; flex-direction: column; gap: 8px; }
.rp-item { display: flex; align-items: center; gap: 11px; padding: 11px 12px; border-radius: 13px; background: var(--surface-soft); cursor: pointer; border: none; font-family: inherit; text-align: left; width: 100%; }
.rp-item-ic { width: 34px; height: 34px; border-radius: 10px; flex: 0 0 auto; display: flex; align-items: center; justify-content: center; background: var(--surface); color: var(--text-sub); }
.rp-item-body { min-width: 0; flex: 1; }
.rp-item-top { display: flex; align-items: center; gap: 7px; min-width: 0; }
.rp-item-title { font-size: 14px; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; min-width: 0; }
.rp-item-sub { display: block; font-size: 12px; color: var(--text-sub); margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.rp-empty { display: flex; flex-direction: column; align-items: center; text-align: center; gap: 6px; padding: 22px 12px; }
.rp-empty-ic { width: 40px; height: 40px; border-radius: 12px; background: var(--surface-soft); color: var(--text-faint); display: flex; align-items: center; justify-content: center; }
.rp-empty-t { font-size: 14px; font-weight: 800; color: var(--text-sub); margin-top: 2px; }
.rp-empty-d { font-size: 12.5px; color: var(--text-faint); line-height: 1.55; }
.rp-empty-d b { color: var(--brand-700); }

.rp-kind { font-size: 10.5px; font-weight: 700; padding: 4px 9px; border-radius: 999px; white-space: nowrap; }
.rp-kind.manual { background: var(--brand-300); color: #8a6a1f; }
.rp-kind.auto { background: rgba(127, 209, 174, .2); color: #3C8F62; }

.rp-loading { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 20px 10px; }
.rp-l-t { font-size: 16px; font-weight: 800; margin-top: 14px; line-height: 1.35; }
.rp-l-d { font-size: 13px; color: var(--text-sub); margin-top: 8px; }

.rp-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.rp-period { font-size: 12.5px; color: var(--text-sub); }
.rp-area-block, .jr-area-block { margin-bottom: 10px; }
.rp-summary { background: var(--surface-soft); }
.rp-sum-t { font-size: 12.5px; font-weight: 800; color: var(--brand-700); margin-bottom: 6px; }
.rp-block-top { margin-bottom: 7px; }
.rp-text { font-size: 14px; line-height: 1.6; color: var(--text); white-space: pre-wrap; }

.rp-muted { color: var(--text-sub); font-size: 13px; }
.rp-banner { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; background: rgba(240, 140, 125, .12); border: 1px solid rgba(240, 140, 125, .4); border-radius: 12px; padding: 11px 13px; margin-bottom: 12px; }
.rp-banner span { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: var(--text); line-height: 1.5; flex: 1; min-width: 0; }
.rp-banner span :deep(svg) { color: var(--warn); flex: 0 0 auto; }
.rp-retry { display: flex; align-items: center; gap: 4px; border: none; background: var(--surface); border-radius: 999px; padding: 6px 11px; font-family: inherit; font-size: 12.5px; font-weight: 700; color: var(--text-sub); cursor: pointer; flex: 0 0 auto; }

/* 데스크톱: 중앙 모달 / 모바일: 바텀시트(다른 모달과 일관) */
.rp-mask { position: fixed; inset: 0; z-index: 60; background: rgba(40, 30, 20, .36); display: flex; align-items: flex-end; justify-content: center; }
@media (min-width: 520px) { .rp-mask { align-items: center; padding: 24px; } }
.rp-modal { background: var(--surface); border-radius: 22px 22px 0 0; padding: 22px 22px 28px; width: 100%; max-width: 460px; box-shadow: var(--shadow-lg); }
@media (min-width: 520px) { .rp-modal { border-radius: 22px; padding: 22px 22px 24px; max-width: 420px; } }
.rp-m-head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.rp-m-title { font-size: 16.5px; font-weight: 800; }
.rp-m-x { margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; color: var(--text-sub); cursor: pointer; flex: 0 0 auto; }
.rp-m-desc { font-size: 13.5px; color: var(--text-sub); line-height: 1.5; margin-bottom: 14px; }
.rp-m-note { display: flex; align-items: center; gap: 7px; font-size: 12.5px; color: var(--text-faint); font-weight: 600; margin-bottom: 20px; }
.rp-m-actions { display: flex; gap: 10px; }
.fade-enter-active, .fade-leave-active { transition: opacity .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
