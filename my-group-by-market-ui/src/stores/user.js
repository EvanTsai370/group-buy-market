import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
    refreshToken: null,
    userInfo: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state) => state.userInfo?.role || null,
    userId: (state) => state.userInfo?.userId || null,
    nickname: (state) => state.userInfo?.nickname || '用户',
    avatar: (state) => state.userInfo?.avatar || null
  },

  actions: {
    // 登录
    async login(credentials) {
      try {
        const res = await authApi.login(credentials)
        if (res.code === '00000') {
          this.token = res.data.accessToken
          this.refreshToken = res.data.refreshToken
          this.userInfo = {
            userId: res.data.userId,
            username: res.data.username,
            nickname: res.data.nickname,
            role: res.data.role,
            avatar: res.data.avatar
          }
          return { success: true }
        }
        return { success: false, message: res.msg }
      } catch (error) {
        return { success: false, message: error.message || '登录失败' }
      }
    },

    // 注册
    async register(data) {
      try {
        const res = await authApi.register(data)
        if (res.code === '00000') {
          return { success: true }
        }
        return { success: false, message: res.msg }
      } catch (error) {
        return { success: false, message: error.message || '注册失败' }
      }
    },

    // 刷新 Token
    async refreshAccessToken() {
      if (!this.refreshToken) {
        this.logout()
        return false
      }
      try {
        const res = await authApi.refreshToken(this.refreshToken)
        if (res.code === '00000') {
          this.token = res.data.accessToken
          this.refreshToken = res.data.refreshToken
          this.userInfo = {
            userId: res.data.userId,
            username: res.data.username,
            nickname: res.data.nickname,
            role: res.data.role,
            avatar: res.data.avatar
          }
          return true
        }
        this.logout()
        return false
      } catch (error) {
        this.logout()
        return false
      }
    },

    // 退出登录
    logout() {
      this.token = null
      this.refreshToken = null
      this.userInfo = null
      router.push('/login')
    },

    // 更新用户信息
    setUserInfo(info) {
      this.userInfo = { ...this.userInfo, ...info }
    }
  },

  // 持久化存储
  persist: {
    key: 'group-buy-user',
    storage: localStorage,
    paths: ['token', 'refreshToken', 'userInfo']
  }
})
