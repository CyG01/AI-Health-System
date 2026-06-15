<template>
  <teleport to="body">
    <transition name="disclaimer-fade">
      <div v-if="visible" class="disclaimer-overlay">
        <div class="disclaimer-modal">
          <!-- 头部 -->
          <div class="disclaimer-header">
            <div class="header-icon">
              <NIcon :size="28" color="#d29922">
                <WarningIcon />
              </NIcon>
            </div>
            <h2 class="header-title">健康管理服务免责声明</h2>
            <p class="header-subtitle">请仔细阅读以下内容后继续</p>
          </div>

          <!-- 可滚动内容区 -->
          <div ref="bodyRef" class="disclaimer-body" @scroll="handleScroll">
            <div class="disclaimer-content">
              <section>
                <h3>一、服务性质说明</h3>
                <p>
                  本平台提供的健康管理建议（包括但不限于运动计划、饮食建议、睡眠改善方案等）均由人工智能系统根据您的健康数据自动生成，<strong>仅供参考，不构成任何形式的医疗诊断、治疗方案或处方建议</strong>。
                </p>
              </section>

              <section>
                <h3>二、非医疗替代声明</h3>
                <p>本平台不能替代专业医疗人员的诊断和治疗。如您存在以下情况，请务必先咨询专业医生：</p>
                <ul>
                  <li>已被确诊患有慢性疾病（如高血压、糖尿病、心脏病等）</li>
                  <li>正在服用处方药物</li>
                  <li>存在运动禁忌或特殊身体状况（如孕期、术后恢复期等）</li>
                  <li>出现不明原因的身体不适或疼痛</li>
                </ul>
              </section>

              <section>
                <h3>三、数据使用与隐私保护</h3>
                <p>
                  依据《中华人民共和国个人信息保护法》及《互联网诊疗管理办法（试行）》的相关规定，我们将：
                </p>
                <ul>
                  <li>严格保护您的个人健康数据，不会向第三方泄露</li>
                  <li>仅将您的健康数据用于生成个性化建议，不会用于其他商业目的</li>
                  <li>您有权随时申请查看、修改或删除您的个人数据</li>
                </ul>
              </section>

              <section>
                <h3>四、安全风险告知</h3>
                <p>使用本平台服务时请注意：</p>
                <ul>
                  <li>请根据自身实际身体状况调整运动强度，切勿强行完成超出能力范围的训练</li>
                  <li>如在运动过程中出现头晕、胸闷、心悸等不适，请立即停止运动并就医</li>
                  <li>AI 生成的饮食建议仅供参考，如有食物过敏史请自行排除相关食材</li>
                  <li>请勿将 AI 建议作为用药依据，任何药物调整请咨询执业医师</li>
                </ul>
              </section>

              <section>
                <h3>五、责任限制</h3>
                <p>
                  在法律允许的最大范围内，本平台及其运营团队不对因使用或依赖本平台提供的信息而导致的任何直接或间接损害承担责任。使用本平台服务即表示您理解并同意上述声明内容，并自愿承担相关风险。
                </p>
              </section>

              <section class="disclaimer-footer-text">
                <p>
                  本声明的最终解释权归平台运营方所有。如您对本声明有任何疑问，请通过平台内的反馈渠道联系我们。
                </p>
              </section>
            </div>

            <!-- 滚动到底部指示器 -->
            <div v-if="!hasScrolledToBottom" class="scroll-indicator">
              <NIcon :size="16">
                <ChevronDownIcon />
              </NIcon>
              <span>请阅读完整内容</span>
            </div>
          </div>

          <!-- 底部操作区 -->
          <div class="disclaimer-footer">
            <label class="agree-checkbox">
              <NCheckbox v-model:checked="agreed" :disabled="!hasScrolledToBottom">
                我已阅读并同意上述免责声明
              </NCheckbox>
            </label>
            <div class="footer-actions">
              <NButton @click="handleReject">不同意</NButton>
              <NButton type="primary" :disabled="!canConfirm" @click="handleConfirm">
                确认并继续注册
              </NButton>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { NIcon, NCheckbox, NButton } from 'naive-ui'
import { Warning as WarningIcon } from '@vicons/ionicons5'
import { ChevronDown as ChevronDownIcon } from '@vicons/ionicons5'

const emit = defineEmits<{
  (e: 'confirm'): void
  (e: 'reject'): void
}>()

const props = withDefaults(defineProps<{
  visible?: boolean
}>(), {
  visible: false
})

const bodyRef = ref<HTMLDivElement | null>(null)
const hasScrolledToBottom = ref(false)
const agreed = ref(false)

const canConfirm = computed(() => hasScrolledToBottom.value && agreed.value)

/**
 * 监听弹窗打开，重置状态
 */
watch(
  () => props.visible,
  val => {
    if (val) {
      hasScrolledToBottom.value = false
      agreed.value = false
      nextTick(() => {
        if (bodyRef.value) {
          bodyRef.value.scrollTop = 0
        }
      })
    }
  }
)

/**
 * 滚动事件处理：检测是否滚动到底部（距离底部 30px 以内视为到底）
 */
function handleScroll() {
  const el = bodyRef.value
  if (!el || hasScrolledToBottom.value) return
  const threshold = 30
  const isBottom = el.scrollHeight - el.scrollTop - el.clientHeight <= threshold
  if (isBottom) {
    hasScrolledToBottom.value = true
  }
}

function handleConfirm() {
  if (canConfirm.value) {
    emit('confirm')
  }
}

function handleReject() {
  emit('reject')
}
</script>

<style scoped lang="scss">
.disclaimer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.disclaimer-modal {
  width: 90%;
  max-width: 560px;
  max-height: 85vh;
  background: #0d1117;
  border: 1px solid #30363d;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 16px 64px rgba(0, 0, 0, 0.5);
}

/* 头部 */
.disclaimer-header {
  text-align: center;
  padding: 24px 24px 16px;
  border-bottom: 1px solid #21262d;
  flex-shrink: 0;
}

.header-icon {
  margin-bottom: 8px;
}

.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #e6edf3;
}

.header-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #8b949e;
}

/* 内容区 */
.disclaimer-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  position: relative;

  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: #30363d;
    border-radius: 3px;
  }
}

.disclaimer-content {
  color: #c9d1d9;
  font-size: 13px;
  line-height: 1.8;

  section {
    margin-bottom: 20px;
  }

  h3 {
    font-size: 14px;
    font-weight: 600;
    color: #e6edf3;
    margin: 0 0 8px;
  }

  p {
    margin: 0 0 8px;
  }

  ul {
    margin: 8px 0;
    padding-left: 20px;

    li {
      margin-bottom: 6px;
    }
  }

  strong {
    color: #f0883e;
  }
}

.disclaimer-footer-text {
  padding-top: 12px;
  border-top: 1px solid #21262d;
  font-size: 12px;
  color: #8b949e;
}

/* 滚动指示器 */
.scroll-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  color: #58a6ff;
  font-size: 12px;
  animation: bounce-down 1.5s infinite;
}

@keyframes bounce-down {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(4px); }
}

/* 底部操作区 */
.disclaimer-footer {
  padding: 16px 24px;
  border-top: 1px solid #21262d;
  background: #161b22;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.agree-checkbox {
  cursor: pointer;
}

.footer-actions {
  display: flex;
  gap: 8px;
}

/* 过渡动画 */
.disclaimer-fade-enter-active {
  transition: opacity 0.3s ease;
}
.disclaimer-fade-leave-active {
  transition: opacity 0.2s ease;
}
.disclaimer-fade-enter-from,
.disclaimer-fade-leave-to {
  opacity: 0;
}

/* 响应式 */
@media (max-width: 480px) {
  .disclaimer-modal {
    width: 95%;
    max-height: 90vh;
  }

  .disclaimer-footer {
    flex-direction: column;
    gap: 12px;
  }

  .footer-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
