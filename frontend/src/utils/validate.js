/**
 * 前端通用校验工具
 * 极客暗黑风格 — 表单校验规则集
 */

/** 手机号正则 */
const PHONE_REGEX = /^1[3-9]\d{9}$/

/** 密码强度正则：8-20位，至少包含字母和数字 */
const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,20}$/

/** 验证码正则：6位数字 */
const CODE_REGEX = /^\d{6}$/

/** 用户名正则：3-20位字母、数字、下划线 */
const USERNAME_REGEX = /^[a-zA-Z0-9_]{3,20}$/

/**
 * 校验手机号
 * @param {*} _rule - Element Plus 校验规则
 * @param {string} value - 待校验值
 * @param {Function} callback - 回调
 */
export function validatePhone(_rule, value, callback) {
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

/**
 * 校验密码强度
 */
export function validatePassword(_rule, value, callback) {
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

/**
 * 校验确认密码（需传入原密码用于对比）
 * @param {string} originalPassword - 原始密码值
 */
export function validateConfirmPassword(passwordGetter) {
  return (_rule, value, callback) => {
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

/**
 * 校验验证码
 */
export function validateCode(_rule, value, callback) {
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

/**
 * 校验用户名
 */
export function validateUsername(_rule, value, callback) {
  if (!value) {
    callback(new Error('请输入用户名'))
    return
  }
  if (!USERNAME_REGEX.test(value)) {
    callback(new Error('用户名需3-20位字母、数字或下划线'))
    return
  }
  callback()
}

/**
 * 必填校验
 * @param {string} label - 字段名
 */
export function requiredRule(label = '此项') {
  return { required: true, message: `请输入${label}`, trigger: 'blur' }
}

/**
 * 手机号校验规则（Element Plus form rules 对象）
 */
export const phoneRule = { validator: validatePhone, trigger: 'blur' }

/**
 * 密码校验规则
 */
export const passwordRule = { validator: validatePassword, trigger: 'blur' }

/**
 * 验证码校验规则
 */
export const codeRule = { validator: validateCode, trigger: 'blur' }

/**
 * 用户名校验规则
 */
export const usernameRule = { validator: validateUsername, trigger: 'blur' }

// 组合校验规则数组（Element Plus form rules 格式）— 供表单规则对象直接使用
export const usernameRules = [
  { required: true, message: '请输入用户名', trigger: 'blur' },
  { validator: validateUsername, trigger: 'blur' }
]

export const passwordRules = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { validator: validatePassword, trigger: 'blur' }
]

export const phoneRules = [
  { required: true, message: '请输入手机号', trigger: 'blur' },
  { validator: validatePhone, trigger: 'blur' }
]

export const verifyCodeRules = [
  { required: true, message: '请输入验证码', trigger: 'blur' },
  { validator: validateCode, trigger: 'blur' }
]

export function createConfirmPasswordRule(passwordGetter) {
  return { validator: validateConfirmPassword(passwordGetter), trigger: 'blur' }
}

export function createNewPasswordRule(oldPasswordGetter) {
  return {
    validator: (_rule, value, callback) => {
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

export { PHONE_REGEX, PASSWORD_REGEX, CODE_REGEX, USERNAME_REGEX }
