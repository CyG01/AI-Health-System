import { onMounted, onBeforeUnmount, ref, toValue, watch, type Ref, type WatchSource } from 'vue'
import { useRouter } from 'vue-router'

/**
 * Composable for form dirty-checking protection.
 * Warns users before navigating away or closing the tab when they have unsaved changes.
 *
 * Usage:
 *   const formData = ref({ name: '', age: 0 })
 *   const { isDirty, markClean } = useDirtyCheck(formData)
 *
 *   // After successful save:
 *   markClean()
 */
export function useDirtyCheck(watchTarget: WatchSource<unknown> | Ref<unknown>, options?: { enabled?: Ref<boolean> }) {
  const router = useRouter()
  let snapshot = ''
  const isDirty = ref(false)

  function takeSnapshot() {
    snapshot = JSON.stringify(toValue(watchTarget))
  }

  function markClean() {
    takeSnapshot()
    isDirty.value = false
  }

  function checkDirty() {
    isDirty.value = JSON.stringify(toValue(watchTarget)) !== snapshot
  }

  // Browser beforeunload handler
  function onBeforeUnload(e: BeforeUnloadEvent) {
    if (!isDirty.value) return
    e.preventDefault()
    e.returnValue = ''
  }

  // Router navigation guard
  let removeGuard: (() => void) | null = null

  onMounted(() => {
    takeSnapshot()
    window.addEventListener('beforeunload', onBeforeUnload)

    removeGuard = router.beforeEach((_to, _from, next) => {
      if (isDirty.value && !(options?.enabled?.value === false)) {
        const confirmed = window.confirm('您有未保存的更改，确定要离开吗？')
        if (confirmed) {
          markClean()
          next()
        } else {
          next(false)
        }
      } else {
        next()
      }
    })
  })

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', onBeforeUnload)
    removeGuard?.()
  })

  watch(watchTarget, checkDirty, { deep: true })

  return { isDirty, markClean }
}
