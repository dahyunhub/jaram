<script setup>
// 새 반 만들기 — 온보딩형 플로우. 반 이름·학년도 + 아이 한 명씩 입력.
// 백엔드: POST /classrooms → POST /classrooms/{id}/children (아이마다).
import { reactive, ref } from 'vue'
import { api, ApiError } from '../lib/api'
import AppIcon from './AppIcon.vue'

const emit = defineEmits(['close', 'created'])

const nowYear = new Date().getFullYear()
const form = reactive({ name: '', year: nowYear })
const children = ref([{ name: '', birthDate: '', gender: 'MALE' }])
const saving = ref(false)
const error = ref('')

function addChild() { children.value.push({ name: '', birthDate: '', gender: 'MALE' }) }
function removeChild(i) { children.value.splice(i, 1) }

async function submit() {
  error.value = ''
  if (!form.name.trim()) { error.value = '반 이름을 입력해 주세요.'; return }
  if (!form.year || form.year < 2000 || form.year > 2100) { error.value = '학년도를 확인해 주세요.'; return }

  // 입력된 아이만(이름 있는 행). 이름은 있는데 생년월일이 빠진 행은 막는다.
  const filled = children.value.filter((c) => c.name.trim() || c.birthDate)
  for (const c of filled) {
    if (!c.name.trim()) { error.value = '아이 이름을 입력해 주세요.'; return }
    if (!c.birthDate) { error.value = `${c.name.trim()} 아이의 생년월일을 입력해 주세요.`; return }
  }

  saving.value = true
  try {
    const classroom = await api.post('/classrooms', { name: form.name.trim(), year: form.year })
    // 아이들 순차 등록(반 생성 후). 일부 실패해도 반은 이미 생성됨.
    let failed = 0
    for (const c of filled) {
      try {
        await api.post(`/classrooms/${classroom.id}/children`,
          { name: c.name.trim(), birthDate: c.birthDate, gender: c.gender })
      } catch { failed += 1 }
    }
    emit('created', { classroom, total: filled.length, failed })
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '반 생성 중 문제가 발생했어요.'
    saving.value = false
  }
}
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="sheet">
      <div class="sheet-top">
        <span class="jr-h2">새 반 만들기</span>
        <button class="close" @click="emit('close')"><AppIcon name="x" :size="18" /></button>
      </div>

      <div class="fields">
        <div class="row2">
          <div class="grow">
            <label class="jr-field-label">반 이름</label>
            <input v-model="form.name" class="jr-input" placeholder="예: 햇살반" />
          </div>
          <div class="yearbox">
            <label class="jr-field-label">학년도</label>
            <input v-model.number="form.year" class="jr-input" type="number" inputmode="numeric" />
          </div>
        </div>

        <div class="kids-head">
          <span class="kids-t">아이 명단</span>
          <span class="kids-note">나중에 추가해도 돼요</span>
        </div>
        <div class="kids">
          <div v-for="(c, i) in children" :key="i" class="kid">
            <div class="kid-line">
              <input v-model="c.name" class="jr-input" placeholder="이름" />
              <button class="kid-x" @click="removeChild(i)" aria-label="삭제"><AppIcon name="x" :size="16" /></button>
            </div>
            <div class="kid-line">
              <input v-model="c.birthDate" class="jr-input" type="date" />
              <div class="sex">
                <button type="button" class="jr-toggle" :class="{ 'is-on': c.gender === 'MALE' }" @click="c.gender = 'MALE'">남</button>
                <button type="button" class="jr-toggle" :class="{ 'is-on': c.gender === 'FEMALE' }" @click="c.gender = 'FEMALE'">여</button>
              </div>
            </div>
          </div>
        </div>
        <button class="add-kid" @click="addChild"><AppIcon name="plus" :size="16" :stroke="2.6" /> 아이 추가</button>
      </div>

      <p v-if="error" class="err">{{ error }}</p>
      <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg save" @click="submit" :disabled="saving">
        <template v-if="!saving"><AppIcon name="check" :size="22" :stroke="2.6" /> 반 만들기</template>
        <template v-else>만드는 중…</template>
      </button>
    </div>
  </div>
</template>

<style scoped>
.overlay { position: fixed; inset: 0; z-index: 40; background: rgba(40, 30, 20, .36); display: flex; align-items: flex-end; justify-content: center; }
@media (min-width: 520px) { .overlay { align-items: center; padding: 24px; } }
.sheet { background: var(--surface); width: 100%; max-width: 460px; max-height: 90vh; overflow-y: auto; box-shadow: var(--shadow-lg); border-radius: 26px 26px 0 0; padding: 22px 22px 28px; }
@media (min-width: 520px) { .sheet { border-radius: 24px; padding: 26px 28px; } }
.sheet-top { display: flex; align-items: center; margin-bottom: 16px; }
.close { margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; color: var(--text-sub); cursor: pointer; }
.fields { display: flex; flex-direction: column; gap: 14px; }
.row2 { display: flex; gap: 12px; }
.grow { flex: 1; }
.yearbox { width: 110px; flex: 0 0 auto; }
.kids-head { display: flex; align-items: baseline; gap: 8px; margin-top: 4px; }
.kids-t { font-size: 13px; font-weight: 800; color: var(--text-sub); }
.kids-note { font-size: 11.5px; color: var(--text-faint); }
.kids { display: flex; flex-direction: column; gap: 12px; }
.kid { background: var(--surface-soft); border-radius: 14px; padding: 12px; display: flex; flex-direction: column; gap: 8px; }
.kid-line { display: flex; gap: 8px; align-items: center; }
.kid-line .jr-input { flex: 1; }
.kid-x { border: none; background: transparent; color: var(--text-faint); cursor: pointer; flex: 0 0 auto; padding: 6px; }
.sex { display: flex; gap: 6px; flex: 0 0 auto; }
.sex .jr-toggle { cursor: pointer; padding: 8px 12px; }
.add-kid { display: flex; align-items: center; justify-content: center; gap: 6px; width: 100%; padding: 11px; border: 1.5px dashed var(--hair-strong); border-radius: 12px; background: transparent; color: var(--text-sub); cursor: pointer; font-family: inherit; font-size: 13.5px; font-weight: 700; }
.err { color: var(--warn); font-weight: 600; font-size: 13.5px; margin-top: 12px; }
.save { margin-top: 16px; }
</style>
