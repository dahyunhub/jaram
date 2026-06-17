<script setup>
defineProps({ size: { type: [Number, String], default: 32 } })
</script>

<template>
  <svg :width="size" :height="size" viewBox="0 0 32 32" fill="none" style="flex:0 0 auto">
    <circle cx="16" cy="16" r="16" fill="#FFD45E" />
    <path d="M16 24 V15" stroke="#5E9C72" stroke-width="2.4" stroke-linecap="round" />
    <path d="M16 18 C10 17 7 12 8 7 C14 7 18 11 16 18 Z" fill="#7FBF8E" />
    <path d="M16 15 C22 14 25 9 24 4 C18 5 14 9 16 15 Z" fill="#A8E0BB" />
  </svg>
</template>
