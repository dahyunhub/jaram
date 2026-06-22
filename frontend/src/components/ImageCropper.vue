<script setup>
// 무의존 정사각(1:1) 크로퍼. file 을 받아 캔버스에 cover 로 그리고, 드래그(팬)+슬라이더(줌)로
// 영역을 맞춘 뒤 512x512 JPEG blob 으로 출력한다. 캔버스 내부 해상도=출력 해상도(512)라 그대로 export.
import { ref, onMounted, onBeforeUnmount } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({ file: { type: Object, required: true } })
const emit = defineEmits(['cropped', 'close'])

const OUT = 512 // 출력/캔버스 내부 해상도(정사각)
const canvas = ref(null)
const zoom = ref(1) // minScale 배수 (1 = cover)
const error = ref('')

let ctx = null
let img = null
let minScale = 1
let offset = { x: 0, y: 0 } // 이미지 좌상단 위치(캔버스 px)
let dragging = false
let last = { x: 0, y: 0 }
let objectUrl = ''

function draw() {
  if (!ctx || !img) return
  const scale = minScale * zoom.value
  const w = img.width * scale
  const h = img.height * scale
  clampOffset(w, h)
  ctx.clearRect(0, 0, OUT, OUT)
  ctx.drawImage(img, offset.x, offset.y, w, h)
}

function clampOffset(w, h) {
  // 이미지가 항상 캔버스를 덮도록(빈 영역 방지)
  offset.x = Math.min(0, Math.max(OUT - w, offset.x))
  offset.y = Math.min(0, Math.max(OUT - h, offset.y))
}

let prevZoom = 1
function onZoom() {
  // 중심 고정 줌: 캔버스 중앙이 가리키던 이미지 지점을 그대로 유지.
  const scaleOld = minScale * prevZoom
  const scaleNew = minScale * zoom.value
  const imgCx = (OUT / 2 - offset.x) / scaleOld
  const imgCy = (OUT / 2 - offset.y) / scaleOld
  offset.x = OUT / 2 - imgCx * scaleNew
  offset.y = OUT / 2 - imgCy * scaleNew
  prevZoom = zoom.value
  draw()
}

function toCanvasPx(clientDelta) {
  const rect = canvas.value.getBoundingClientRect()
  return clientDelta * (OUT / rect.width)
}

function onPointerDown(e) {
  dragging = true
  last = { x: e.clientX, y: e.clientY }
  canvas.value.setPointerCapture(e.pointerId)
}
function onPointerMove(e) {
  if (!dragging) return
  offset.x += toCanvasPx(e.clientX - last.x)
  offset.y += toCanvasPx(e.clientY - last.y)
  last = { x: e.clientX, y: e.clientY }
  draw()
}
function onPointerUp(e) {
  dragging = false
  try { canvas.value.releasePointerCapture(e.pointerId) } catch { /* noop */ }
}

function apply() {
  canvas.value.toBlob((blob) => {
    if (blob) emit('cropped', blob)
    else error.value = '이미지 처리에 실패했어요.'
  }, 'image/jpeg', 0.85)
}

onMounted(() => {
  ctx = canvas.value.getContext('2d')
  canvas.value.width = OUT
  canvas.value.height = OUT
  if (!props.file.type?.startsWith('image/')) { error.value = '이미지 파일만 등록할 수 있어요.'; return }
  objectUrl = URL.createObjectURL(props.file)
  img = new Image()
  img.onload = () => {
    minScale = Math.max(OUT / img.width, OUT / img.height) // cover
    zoom.value = 1
    prevZoom = 1
    const w = img.width * minScale
    const h = img.height * minScale
    offset = { x: (OUT - w) / 2, y: (OUT - h) / 2 } // 중앙 정렬
    draw()
  }
  img.onerror = () => { error.value = '이미지를 불러오지 못했어요.' }
  img.src = objectUrl
})

onBeforeUnmount(() => { if (objectUrl) URL.revokeObjectURL(objectUrl) })
</script>

<template>
  <div class="cr-overlay" @click.self="emit('close')">
    <div class="cr-sheet">
      <div class="cr-top">
        <span class="jr-h2">사진 자르기</span>
        <button class="cr-x" @click="emit('close')"><AppIcon name="x" :size="18" /></button>
      </div>

      <p v-if="error" class="cr-err">{{ error }}</p>
      <template v-else>
        <div class="cr-stage">
          <canvas
            ref="canvas" class="cr-canvas"
            @pointerdown="onPointerDown" @pointermove="onPointerMove"
            @pointerup="onPointerUp" @pointercancel="onPointerUp"
          />
          <div class="cr-ring" />
        </div>
        <div class="cr-zoom">
          <AppIcon name="search" :size="16" style="color:var(--text-faint)" />
          <input type="range" min="1" max="3" step="0.01" v-model.number="zoom" @input="onZoom" />
        </div>
        <p class="cr-hint">드래그로 위치, 슬라이더로 확대를 맞춰 주세요.</p>
      </template>

      <div class="cr-actions">
        <button class="jr-btn jr-btn--ghost" @click="emit('close')">취소</button>
        <button class="jr-btn jr-btn--primary" style="flex:1" :disabled="!!error" @click="apply">
          <AppIcon name="check" :size="20" :stroke="2.6" /> 적용
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cr-overlay { position: fixed; inset: 0; z-index: 70; background: rgba(40, 30, 20, .5); display: flex; align-items: flex-end; justify-content: center; }
@media (min-width: 520px) { .cr-overlay { align-items: center; padding: 24px; } }
.cr-sheet { background: var(--surface); width: 100%; max-width: 400px; box-shadow: var(--shadow-lg); border-radius: 24px 24px 0 0; padding: 22px 22px 26px; }
@media (min-width: 520px) { .cr-sheet { border-radius: 24px; } }
.cr-top { display: flex; align-items: center; margin-bottom: 16px; }
.cr-x { margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; color: var(--text-sub); cursor: pointer; }
.cr-stage { position: relative; width: 100%; max-width: 300px; margin: 0 auto; aspect-ratio: 1; }
.cr-canvas { width: 100%; height: 100%; display: block; border-radius: 16px; background: var(--surface-soft); touch-action: none; cursor: grab; }
.cr-canvas:active { cursor: grabbing; }
.cr-ring { position: absolute; inset: 0; border-radius: 50%; box-shadow: 0 0 0 9999px rgba(255,255,255,.04); outline: 2px solid var(--brand-500); pointer-events: none; }
.cr-zoom { display: flex; align-items: center; gap: 10px; margin-top: 16px; }
.cr-zoom input { flex: 1; accent-color: var(--brand-500); }
.cr-hint { font-size: 12.5px; color: var(--text-faint); text-align: center; margin-top: 8px; }
.cr-err { color: var(--warn); font-weight: 600; font-size: 13.5px; padding: 16px 0; text-align: center; }
.cr-actions { display: flex; gap: 10px; margin-top: 18px; }
</style>
