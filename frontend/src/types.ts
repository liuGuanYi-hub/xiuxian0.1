export type Choice = {
  index: number
  label: string
  hint: string
}

export type GameEvent = {
  id: string
  title: string
  description: string
  choices: Choice[]
}

export type GameRun = {
  id: string
  playerName: string
  origin: string
  realm: string
  health: number
  spirit: number
  lifespan: number
  karma: number
  turn: number
  status: 'RUNNING' | 'DEAD' | 'ASCENDED'
  event: GameEvent
  logs: string[]
}

