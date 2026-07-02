/** Phone number regex */
export const REG_PHONE = /^1[3-9]\d{9}$/;

/** Password regex: 8-20 characters, must contain letter and digit */
export const REG_PWD = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,20}$/;

/** Username regex: 4-20 characters */
export const REG_USER_NAME = /^[a-zA-Z0-9_]{4,20}$/;

/** Six digit code regex */
export const REG_CODE_SIX = /^\d{6}$/;

/** Email regex */
export const REG_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
