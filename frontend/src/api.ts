import type { GameRun } from './types'

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080/api'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(body?.message ?? `请求失败：${response.status}`)
  }
  return response.json() as Promise<T>
}

export function startRun(playerName: string, origin: string) {
  return request<GameRun>('/game/runs', {
    method: 'POST',
    body: JSON.stringify({ playerName, origin }),
  })
}

export function chooseEvent(runId: string, choiceIndex: number) {
  return request<GameRun>(`/game/runs/${runId}/choices`, {
    method: 'POST',
    body: JSON.stringify({ choiceIndex, requestId: crypto.randomUUID() }),
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

export function restoreRun(runId: string) {
  return request<GameRun>(`/game/runs/${encodeURIComponent(runId.trim())}`)
}
