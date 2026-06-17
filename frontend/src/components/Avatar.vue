<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, default: '' },
  size: { type: String, default: '' }, // '', 'sm', 'lg'
})

const AVA_COLORS = ['#EF9D5E', '#62AdD0', '#E0AE3C', '#B07FD6', '#5FBA86', '#E8897C', '#8089D2', '#D483AC']

const color = computed(() => {
  const name = props.name || '?'
  let h = 0
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0
  return AVA_COLORS[h % AVA_COLORS.length]
})

// 성 제외 이름(가독)
const initial = computed(() => {
  const n = props.name
  if (!n) return '?'
  return n.slice(n.length > 2 ? 1 : 0)
})

const cls = computed(() => 'jr-avatar' + (props.size ? ' jr-avatar--' + props.size : ''))
</script>

<template>
  <span :class="cls" :style="{ background: color }">{{ initial }}</span>
</template>
