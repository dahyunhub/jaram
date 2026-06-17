// 선택한 반(반 전환) 상태. localStorage 영속.
import { reactive } from 'vue'

const KEY = 'jaram.classroom'

function load() {
  try { return JSON.parse(localStorage.getItem(KEY)) } catch { return null }
}

export const session = reactive({
  classroom: load(), // { id, name, year, startDate, childCount }

  select(classroom) {
    this.classroom = classroom
    localStorage.setItem(KEY, JSON.stringify(classroom))
  },

  clear() {
    this.classroom = null
    localStorage.removeItem(KEY)
  },
})
