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
  status: 'LOCKED' | 'AVAILABLE' | 'ACTIVE' | 'REWARD' | 'CLEARED'
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

export type BuildCard = {
  id: string
  cardId: string
  category: string
  name: string
  rarity: string
  description: string
  effectText: string
}

export type RewardOffer = BuildCard

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
  build: BuildCard[]
  rewardOffers: RewardOffer[]
  logs: string[]
}
