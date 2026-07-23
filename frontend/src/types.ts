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
  rarity: string
  repeatable: boolean
}

export type MapNode = {
  id: string
  floor: number
  position: number
  type: string
  label: string
  rarity: string
  status: 'LOCKED' | 'AVAILABLE' | 'ACTIVE' | 'CLEARED'
  nextNodeIds: string[]
}

export type RouteMap = {
  totalFloors: number
  nodes: MapNode[]
}

export type Ending = {
  id: string
  title: string
  description: string
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
  currentNodeId: string
  currentFloor: number
  event: GameEvent
  map: RouteMap
  ending: Ending | null
  logs: string[]
}
