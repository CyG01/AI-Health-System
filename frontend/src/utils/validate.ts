/**
 * 前端通用校验工具 — TypeScript 版
 * 兼容 NaiveUI FormItemRule validator 签名
 */

/** 手机号正则 */
export const PHONE_REGEX = /^1[3-9]\d{9}$/

/** 密码强度正则：8-20位，至少包含字母和数字 */
export const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,20}$/

/** 验证码正则：6位数字 */
export const CODE_REGEX = /^\d{6}$/

/** 用户名正则：4-20位字母、数字、下划线 */
export const USERNAME_REGEX = /^[a-zA-Z0-9_]{4,20}$/

type ValidationCallback = (error?: Error) => void

/** 校验手机号 */
export function validatePhone(_rule: unknown, value: string, callback: ValidationCallback) {
  if (!value) {
    callback(new Error('请输入手机号'))
    return
  }
  if (!PHONE_REGEX.test(value)) {
    callback(new Error('手机号格式不正确'))
    return
  }
  callback()
}

/** 校验密码强度 */
export function validatePassword(_rule: unknown, value: string, callback: ValidationCallback) {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (!PASSWORD_REGEX.test(value)) {
    callback(new Error('密码需8-20位，且包含字母和数字'))
    return
  }
  callback()
}

/** 校验确认密码（需传入原密码 getter） */
export function validateConfirmPassword(passwordGetter: () => string) {
  return (_rule: unknown, value: string, callback: ValidationCallback) => {
    if (!value) {
      callback(new Error('请再次输入密码'))
      return
    }
    if (value !== passwordGetter()) {
      callback(new Error('两次输入的密码不一致'))
      return
    }
    callback()
  }
}

/** 校验验证码 */
export function validateCode(_rule: unknown, value: string, callback: ValidationCallback) {
  if (!value) {
    callback(new Error('请输入验证码'))
    return
  }
  if (!CODE_REGEX.test(value)) {
    callback(new Error('验证码为6位数字'))
    return
  }
  callback()
}

/** 校验用户名 */
export function validateUsername(_rule: unknown, value: string, callback: ValidationCallback) {
  if (!value) {
    callback(new Error('请输入用户名'))
    return
  }
  if (!USERNAME_REGEX.test(value)) {
    callback(new Error('用户名需4-20位字母、数字或下划线'))
    return
  }
  callback()
}

interface FormRule {
  required?: boolean
  message?: string
  trigger?: string | string[]
  validator?: (rule: unknown, value: string, callback: ValidationCallback) => void
}

/** 必填校验规则 */
export function requiredRule(label = '此项'): FormRule {
  return { required: true, message: `请输入${label}`, trigger: 'blur' }
}

/** 手机号校验规则 */
export const phoneRule: FormRule = { validator: validatePhone, trigger: 'blur' }

/** 密码校验规则 */
export const passwordRule: FormRule = { validator: validatePassword, trigger: 'blur' }

/** 验证码校验规则 */
export const codeRule: FormRule = { validator: validateCode, trigger: 'blur' }

/** 用户名校验规则 */
export const usernameRule: FormRule = { validator: validateUsername, trigger: 'blur' }

/** 组合校验规则数组 */
export const usernameRules: FormRule[] = [
  { required: true, message: '请输入用户名', trigger: 'blur' },
  { validator: validateUsername, trigger: 'blur' }
]

export const passwordRules: FormRule[] = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { validator: validatePassword, trigger: 'blur' }
]

export const phoneRules: FormRule[] = [
  { required: true, message: '请输入手机号', trigger: 'blur' },
  { validator: validatePhone, trigger: 'blur' }
]

export const verifyCodeRules: FormRule[] = [
  { required: true, message: '请输入验证码', trigger: 'blur' },
  { validator: validateCode, trigger: 'blur' }
]

export function createConfirmPasswordRule(passwordGetter: () => string): FormRule {
  return { validator: validateConfirmPassword(passwordGetter), trigger: 'blur' }
}

export function createNewPasswordRule(oldPasswordGetter: () => string): FormRule {
  return {
    validator: (_rule: unknown, value: string, callback: ValidationCallback) => {
      if (!value) {
        callback(new Error('请输入新密码'))
        return
      }
      if (value === oldPasswordGetter()) {
        callback(new Error('新密码不能与原密码相同'))
        return
      }
      if (!PASSWORD_REGEX.test(value)) {
        callback(new Error('密码需8-20位，且包含字母和数字'))
        return
      }
      callback()
    },
    trigger: 'blur'
  }
}
