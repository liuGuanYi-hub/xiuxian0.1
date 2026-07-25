import type { Account, AccountProgress, AuthResponse, GameRun, GameRunSummary, LeaderboardEntry } from './types'

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080/api'
const AUTH_TOKEN_KEY = 'xiuxian.auth.token'

export function getAuthToken() {
  return window.localStorage.getItem(AUTH_TOKEN_KEY)
}

export function setAuthToken(token: string) {
  window.localStorage.setItem(AUTH_TOKEN_KEY, token)
}

export function clearAuthToken() {
  window.localStorage.removeItem(AUTH_TOKEN_KEY)
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getAuthToken()
  const headers = new Headers(init?.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
  })
  if (!response.ok) {
    if (response.status === 401 && token) {
      clearAuthToken()
      window.dispatchEvent(new Event('xiuxian-auth-expired'))
    }
    const body = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(body?.message ?? `请求失败：${response.status}`)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export function registerAccount(username: string, password: string, characterName: string) {
  return request<AuthResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, password, characterName }),
  })
}

export function loginAccount(username: string, password: string) {
  return request<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function getCurrentAccount() {
  return request<Account>('/players/me')
}

export function createCharacter(name: string, origin: string) {
  return request<Account>('/players', {
    method: 'POST',
    body: JSON.stringify({ name, origin }),
  })
}

export function getRecentRuns() {
  return request<GameRunSummary[]>('/game/runs')
}

export function getAccountProgress() {
  return request<AccountProgress>('/account/progress')
}

export function purchaseUnlock(unlockId: string) {
  return request<AccountProgress>(`/account/unlocks/${encodeURIComponent(unlockId)}`, {
    method: 'POST',
  })
}

export function startRun(playerName: string, origin: string, characterId?: string) {
  return request<GameRun>('/game/runs', {
    method: 'POST',
    body: JSON.stringify({ playerName, origin, characterId }),
  })
}

export function chooseEvent(runId: string, choiceIndex: number) {
  return request<GameRun>(`/game/runs/${runId}/choices`, {
    method: 'POST',
    body: JSON.stringify({ choiceIndex, requestId: crypto.randomUUID() }),
  })
}

export function combatAction(runId: string, action: string) {
  return request<GameRun>(`/game/runs/${runId}/combat/actions`, {
    method: 'POST',
    body: JSON.stringify({ action }),
  })
}

export function enterNode(runId: string, nodeId: string) {
  return request<GameRun>(`/game/runs/${runId}/nodes/${nodeId}/enter`, {
    method: 'POST',
  })
}

export function claimReward(runId: string, rewardId: string) {
  return request<GameRun>(`/game/runs/${runId}/rewards/${rewardId}/claim`, {
    method: 'POST',
  })
}

export function upgradeCard(runId: string, cardId: string) {
  return request<GameRun>(`/game/runs/${runId}/upgrades/${cardId}`, {
    method: 'POST',
  })
}

export function skipUpgrade(runId: string) {
  return request<GameRun>(`/game/runs/${runId}/upgrades/skip`, {
    method: 'POST',
  })
}

export function removeSpecialCard(runId: string, cardId: string) {
  return request<GameRun>(`/game/runs/${runId}/removals/${cardId}`, {
    method: 'POST',
  })
}

export function skipSpecialRemoval(runId: string) {
  return request<GameRun>(`/game/runs/${runId}/removals/skip`, {
    method: 'POST',
  })
}

export function buyShopOffer(runId: string, offerId: string) {
  return request<GameRun>(`/game/runs/${runId}/shops/${offerId}/buy`, {
    method: 'POST',
  })
}

export function refreshShop(runId: string) {
  return request<GameRun>(`/game/runs/${runId}/shops/refresh`, {
    method: 'POST',
  })
}

export function removeShopCard(runId: string, cardId: string) {
  return request<GameRun>(`/game/runs/${runId}/shops/remove/${cardId}`, {
    method: 'POST',
  })
}

export function leaveShop(runId: string) {
  return request<GameRun>(`/game/runs/${runId}/shops/leave`, {
    method: 'POST',
  })
}

export function restoreRun(runId: string) {
  return request<GameRun>(`/game/runs/${encodeURIComponent(runId.trim())}`)
}

export function getLeaderboard(limit = 10) {
  return request<LeaderboardEntry[]>(`/leaderboard?limit=${limit}`)
}
