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
  status: 'LOCKED' | 'AVAILABLE' | 'ACTIVE' | 'REWARD' | 'UPGRADE' | 'SHOP' | 'REMOVAL' | 'CLEARED'
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
  archetype: string
  name: string
  rarity: string
  description: string
  effectText: string
  upgradeLevel: number
}

export type RewardOffer = Omit<BuildCard, 'upgradeLevel'>

export type ShopOffer = Omit<RewardOffer, 'id'> & {
  id: string
  price: number
}

export type ShopState = {
  id: string
  nodeId: string
  refreshCount: number
  refreshLimit: number
  nextRefreshCost: number
  removalCost: number
  removalUsed: boolean
  offers: ShopOffer[]
}

export type RemovalState = {
  source: string
  title: string
  cost: number
  options: BuildCard[]
}

export type Synergy = {
  archetype: string
  title: string
  count: number
  active: boolean
  effectText: string
}

export type BuildStats = {
  activeCards: number
  categoryCounts: Record<string, number>
  archetypeCounts: Record<string, number>
  synergies: Synergy[]
  battleHealthBonus: number
  battleSpiritBonus: number
  battleLifespanBonus: number
  battleKarmaBonus: number
  combatDamageBonus: number
  combatBlockBonus: number
  combatSpiritGain: number
  combatPoisonBonus: number
}

export type CombatAction = {
  id: string
  label: string
  spiritCost: number
  hint: string
}

export type CombatView = {
  id: string
  enemyId: string
  enemyName: string
  enemyType: string
  enemyDescription: string
  health: number
  maxHealth: number
  enemyBlock: number
  enemyPoison: number
  playerBlock: number
  playerPoison: number
  turn: number
  intent: string
  intentValue: number
  intentText: string
  status: string
  actions: CombatAction[]
  recentLog: string[]
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
  spiritStones: number
  turn: number
  status: 'RUNNING' | 'DEAD' | 'ASCENDED'
  currentNodeId: string
  currentFloor: number
  event: GameEvent
  map: RouteMap
  ending: Ending | null
  build: BuildCard[]
  buildStats: BuildStats
  upgradeOptions: BuildCard[]
  rewardOffers: RewardOffer[]
  shop: ShopState | null
  removal: RemovalState | null
  combat: CombatView | null
  logs: string[]
}
