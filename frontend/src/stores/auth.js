// 인증 상태 — 경량 reactive 스토어(Pinia 미사용). localStorage 영속.
import { reactive } from 'vue'

const TOKEN_KEY = 'jaram.token'
const TEACHER_KEY = 'jaram.teacher'

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
