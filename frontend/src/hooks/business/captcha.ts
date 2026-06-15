import { computed, ref } from 'vue';
import { REG_PHONE } from '@/constants/reg';
import { $t } from '@/locales';
import { fetchSendCode } from '@/service/api';

export function useCaptcha() {
  const loading = ref(false);
  const isCounting = ref(false);
  const count = ref(0);
  let countdownTimer: ReturnType<typeof setInterval> | null = null;

  const label = computed(() => {
    if (loading.value) return '';
    if (isCounting.value) return $t('page.login.codeLogin.reGetCode', { time: count.value });
    return $t('page.login.codeLogin.getCode');
  });

  function isPhoneValid(phone: string) {
    if (phone.trim() === '') {
      window.$message?.error?.($t('form.phone.required'));
      return false;
    }
    if (!REG_PHONE.test(phone)) {
      window.$message?.error?.($t('form.phone.invalid'));
      return false;
    }
    return true;
  }

  function startCountdown() {
    count.value = 60;
    isCounting.value = true;
    countdownTimer = setInterval(() => {
      count.value -= 1;
      if (count.value <= 0) {
        stopCountdown();
      }
    }, 1000);
  }

  function stopCountdown() {
    if (countdownTimer) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
    isCounting.value = false;
  }

  async function getCaptcha(phone: string) {
    const valid = isPhoneValid(phone);
    if (!valid || loading.value) return;

    loading.value = true;
    try {
      const { error } = await fetchSendCode({ phone });
      if (!error) {
        window.$message?.success?.($t('page.login.codeLogin.sendCodeSuccess'));
        startCountdown();
      }
    } catch {
      // error handled by interceptor
    } finally {
      loading.value = false;
    }
  }

  return {
    label,
    isCounting,
    loading,
    getCaptcha,
    startCountdown,
    stopCountdown
  };
}
