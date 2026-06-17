<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { api, ApiError } from '../lib/api'
import { session } from '../stores/session'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'

const classroomId = session.classroom?.id
const children = ref([])
const loading = ref(true)
const loadError = ref('')

// 모달 상태: null | 'add' | 'edit'
const modal = ref(null)
const editingId = ref(null)
const deleting = ref(false)
const form = reactive({ name: '', birthDate: '', gender: 'MALE' })
const saving = ref(false)
const formError = ref('')

function fmtBirth(d) {
  return d ? d.replaceAll('-', '.') : ''
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    children.value = await api.get(`/classrooms/${classroomId}/children`)
  } catch (e) {
    loadError.value = e.message || '명단을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

function openAdd() {
  modal.value = 'add'
  editingId.value = null
  deleting.value = false
  formError.value = ''
  Object.assign(form, { name: '', birthDate: '', gender: 'MALE' })
}

function openEdit(c) {
  modal.value = 'edit'
  editingId.value = c.id
  deleting.value = false
  formError.value = ''
  Object.assign(form, { name: c.name, birthDate: c.birthDate, gender: c.gender })
}

function closeModal() {
  modal.value = null
  editingId.value = null
  deleting.value = false
}

async function save() {
  formError.value = ''
  if (!form.name.trim()) { formError.value = '이름을 입력해 주세요.'; return }
  if (!form.birthDate) { formError.value = '생년월일을 입력해 주세요.'; return }
  saving.value = true
  try {
    const body = { name: form.name.trim(), birthDate: form.birthDate, gender: form.gender }
    if (modal.value === 'add') {
      await api.post(`/classrooms/${classroomId}/children`, body)
    } else {
      await api.put(`/children/${editingId.value}`, body)
    }
    closeModal()
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : '저장 중 문제가 발생했어요.'
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  saving.value = true
  formError.value = ''
  try {
    await api.del(`/children/${editingId.value}`)
    closeModal()
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : '삭제 중 문제가 발생했어요.'
  } finally {
    saving.value = false
  }
}

const count = computed(() => children.value.length)

onMounted(load)
</script>

<template>
  <div class="children">
    <header class="hdr screen">
      <div class="title-row">
        <span class="jr-h1">아이들</span>
        <span class="meta">{{ session.classroom?.name }} · {{ count }}명</span>
      </div>
      <div class="action-row">
        <span class="sort">가나다순</span>
        <button class="jr-btn jr-btn--primary add-btn" @click="openAdd">
          <AppIcon name="plus" :size="20" :stroke="2.6" /> 아이 등록하기
        </button>
      </div>
    </header>

    <div class="screen list-wrap">
      <p v-if="loading" class="muted">불러오는 중…</p>
      <p v-else-if="loadError" class="err">{{ loadError }}</p>
      <p v-else-if="!count" class="muted empty">아직 등록된 아이가 없어요. ‘아이 등록하기’로 시작해 보세요.</p>

      <div v-else class="grid">
        <button v-for="c in children" :key="c.id" class="kid jr-card" @click="openEdit(c)">
          <Avatar :name="c.name" size="lg" />
          <span class="kid-name">{{ c.name }}</span>
          <span class="kid-sub">{{ fmtBirth(c.birthDate) }} · {{ c.gender === 'MALE' ? '남' : '여' }}</span>
        </button>

        <button class="addcard" @click="openAdd">
          <span class="addcard-ic"><AppIcon name="plus" :size="26" :stroke="2.6" /></span>
          <span class="addcard-t">아이 등록하기</span>
          <span class="addcard-d">새 친구를 맞이해요</span>
        </button>
      </div>
    </div>

    <!-- 등록/수정/삭제 모달 -->
    <div v-if="modal" class="overlay" @click.self="closeModal">
      <div class="sheet">
        <!-- 삭제 확인 -->
        <template v-if="deleting">
          <div class="del-ic"><AppIcon name="heart" :size="28" /></div>
          <div class="jr-h2 del-title">{{ form.name }} 아이를 명단에서 숨길까요?</div>
          <div class="jr-banner del-banner">
            <AppIcon name="check" :size="22" :stroke="2.4" style="color:var(--brand-700);flex:0 0 auto" />
            <span>기록은 그대로 보존되며 명단에서만 숨겨져요. 완전히 삭제되지 않아요.</span>
          </div>
          <p v-if="formError" class="err mt">{{ formError }}</p>
          <div class="btn-row">
            <button class="jr-btn jr-btn--secondary f1" @click="deleting = false" :disabled="saving">취소</button>
            <button class="jr-btn jr-btn--warn f1" @click="confirmDelete" :disabled="saving">
              {{ saving ? '처리 중…' : '명단에서 숨기기' }}
            </button>
          </div>
        </template>

        <!-- 등록/수정 폼 -->
        <template v-else>
          <div class="sheet-top">
            <span class="jr-h2">{{ modal === 'add' ? '새 친구 등록' : '아이 정보 수정' }}</span>
            <button class="close" @click="closeModal"><AppIcon name="x" :size="18" /></button>
          </div>

          <div class="fields">
            <div>
              <label class="jr-field-label">이름</label>
              <input v-model="form.name" class="jr-input" placeholder="아이 이름을 입력해주세요" />
            </div>
            <div>
              <label class="jr-field-label">생년월일</label>
              <input v-model="form.birthDate" class="jr-input" type="date" />
            </div>
            <div>
              <label class="jr-field-label">성별</label>
              <div class="sex">
                <button
                  type="button" class="jr-toggle" :class="{ 'is-on': form.gender === 'MALE' }"
                  @click="form.gender = 'MALE'"
                >
                  <AppIcon v-if="form.gender === 'MALE'" name="check" :size="14" :stroke="2.6" /> 남자
                </button>
                <button
                  type="button" class="jr-toggle" :class="{ 'is-on': form.gender === 'FEMALE' }"
                  @click="form.gender = 'FEMALE'"
                >
                  <AppIcon v-if="form.gender === 'FEMALE'" name="check" :size="14" :stroke="2.6" /> 여자
                </button>
              </div>
            </div>
          </div>

          <p v-if="formError" class="err mt">{{ formError }}</p>

          <button
            v-if="modal === 'edit'" class="hide-link" @click="deleting = true"
          >
            <AppIcon name="x" :size="18" /> 명단에서 숨기기 (기록 보존)
          </button>

          <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg save" @click="save" :disabled="saving">
            <template v-if="!saving"><AppIcon name="check" :size="22" :stroke="2.6" /> 저장하기</template>
            <template v-else>저장 중…</template>
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.children { display: flex; flex-direction: column; }
.hdr { padding-top: 12px; padding-bottom: 8px; }
.title-row { display: flex; align-items: baseline; gap: 10px; }
.meta { font-size: 14px; color: var(--text-sub); }
.action-row { display: flex; align-items: center; gap: 10px; margin-top: 14px; }
.sort { font-size: 13px; color: var(--text-faint); font-weight: 600; }
.add-btn { margin-left: auto; min-height: 44px; padding: 0 18px; }
.list-wrap { padding-top: 8px; padding-bottom: 24px; flex: 1; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.kid {
  display: flex; flex-direction: column; align-items: center; gap: 9px; padding: 20px 12px;
  cursor: pointer; text-align: center; border: none; font-family: inherit; background: var(--surface);
}
.kid-name { font-size: 15px; font-weight: 800; }
.kid-sub { font-size: 11.5px; color: var(--text-sub); font-weight: 600; font-variant-numeric: tabular-nums; }
.addcard {
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 9px;
  padding: 20px 12px; cursor: pointer; text-align: center; font-family: inherit;
  border-radius: var(--r-card); border: 2px dashed var(--brand-300); background: var(--brand-100);
}
.addcard-ic {
  width: 56px; height: 56px; border-radius: 50%; background: var(--surface);
  display: flex; align-items: center; justify-content: center; color: var(--brand-700); box-shadow: var(--shadow-sm);
}
.addcard-t { font-size: 15px; font-weight: 800; color: var(--text); }
.addcard-d { font-size: 11.5px; color: var(--text-sub); font-weight: 600; }
.muted { color: var(--text-sub); }
.empty { padding: 20px 4px; line-height: 1.6; }
.err { color: var(--warn); font-weight: 600; font-size: 13.5px; }
.mt { margin-top: 4px; }

/* 모달 */
.overlay { position: fixed; inset: 0; z-index: 30; background: rgba(40, 30, 20, .36); display: flex; align-items: flex-end; justify-content: center; }
@media (min-width: 520px) { .overlay { align-items: center; padding: 24px; } }
.sheet {
  background: var(--surface); width: 100%; max-width: 440px; box-shadow: var(--shadow-lg);
  border-radius: 26px 26px 0 0; padding: 22px 22px 28px;
}
@media (min-width: 520px) { .sheet { border-radius: 24px; padding: 26px 28px; } }
.sheet-top { display: flex; align-items: center; margin-bottom: 16px; }
.close {
  margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%;
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  color: var(--text-sub); cursor: pointer;
}
.fields { display: flex; flex-direction: column; gap: 14px; }
.sex { display: flex; gap: 8px; }
.sex .jr-toggle { flex: 1; justify-content: center; cursor: pointer; }
.hide-link {
  display: flex; align-items: center; justify-content: center; gap: 7px; width: 100%;
  margin-top: 16px; padding: 12px; border: none; background: transparent; color: var(--warn);
  cursor: pointer; font-family: inherit; font-size: 14.5px; font-weight: 700;
}
.save { margin-top: 12px; }
.del-ic {
  width: 56px; height: 56px; border-radius: 50%; margin: 0 auto 16px;
  background: rgba(240, 140, 125, .14); color: var(--warn);
  display: flex; align-items: center; justify-content: center;
}
.del-title { text-align: center; }
.del-banner { margin-top: 16px; text-align: left; font-size: 13.5px; font-weight: 700; }
.btn-row { display: flex; gap: 10px; margin-top: 22px; }
.f1 { flex: 1; }
</style>
