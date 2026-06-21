// 인증 상태 — 경량 reactive 스토어(Pinia 미사용). localStorage 영속.
import { reactive } from 'vue'
import { session } from './session'

const TOKEN_KEY = 'ondo.token'
const TEACHER_KEY = 'ondo.teacher'

function loadTeacher() {
  try { return JSON.parse(localStorage.getItem(TEACHER_KEY)) } catch { return null }
}

export const auth = reactive({
  token: localStorage.getItem(TOKEN_KEY) || null,
  teacher: loadTeacher(),

  get isAuthenticated() {
    return !!this.token
  },

  setSession({ accessToken, teacher }) {
    // 다른 교사로 인증되면(가입·로그인 공통) 이전 교사의 반 선택을 비운다 —
    // 같은 브라우저에 남은 ondo.classroom 을 신규/타 교사가 재사용하는 것을 막는다.
    if (teacher?.id !== this.teacher?.id) {
      session.clear()
    }
    this.token = accessToken
    this.teacher = teacher
    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(TEACHER_KEY, JSON.stringify(teacher))
  },

  logout() {
    this.token = null
    this.teacher = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(TEACHER_KEY)
  },
})
