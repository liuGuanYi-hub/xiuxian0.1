import { useState, type ReactNode } from 'react'
import { ArrowRight, Compass, Download, Flame, Heart, Hourglass, RotateCcw, Sparkles, TriangleAlert } from 'lucide-react'
import { chooseEvent, enterNode, restoreRun, startRun } from './api'
import type { GameRun, MapNode } from './types'

const origins = [
  { value: '散修', description: '自由自在，初始因果较低' },
  { value: '剑宗弃徒', description: '战意锋利，但容易卷入恩怨' },
  { value: '丹鼎童子', description: '灵力深厚，寿元稍显不足' },
]

const nodeIcons: Record<string, string> = {
  BATTLE: '⚔',
  ELITE: '✦',
  EVENT: '❖',
  REST: '☾',
  SHOP: '◇',
  TREASURE: '✧',
  BOSS: '☼',
}

function App() {
  const [name, setName] = useState('')
  const [origin, setOrigin] = useState(origins[0].value)
  const [restoreId, setRestoreId] = useState('')
  const [run, setRun] = useState<GameRun | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function begin() {
    if (!name.trim()) {
      setError('请先为自己取一个道号。')
      return
    }
    setLoading(true)
    setError('')
    try {
      setRun(await startRun(name, origin))
    } catch (err) {
      setError(err instanceof Error ? err.message : '无法连接道庭，请确认后端已经启动。')
    } finally {
      setLoading(false)
    }
  }

  async function enter(nodeId: string) {
    if (!run || run.status !== 'RUNNING' || run.currentNodeId) return
    setLoading(true)
    setError('')
    try {
      setRun(await enterNode(run.id, nodeId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '无法进入这个节点。')
    } finally {
      setLoading(false)
    }
  }

  async function choose(choiceIndex: number) {
    if (!run || run.status !== 'RUNNING' || !run.currentNodeId) return
    setLoading(true)
    setError('')
    try {
      setRun(await chooseEvent(run.id, choiceIndex))
    } catch (err) {
      setError(err instanceof Error ? err.message : '选择未能送达。')
    } finally {
      setLoading(false)
    }
  }

  async function restore() {
    if (!restoreId.trim()) {
      setError('请输入要恢复的存档 ID。')
      return
    }
    setLoading(true)
    setError('')
    try {
      setRun(await restoreRun(restoreId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '存档恢复失败，请检查 ID。')
    } finally {
      setLoading(false)
    }
  }

  function reset() {
    setRun(null)
    setError('')
  }

  if (!run) {
    return (
      <main className="page-shell landing-shell">
        <div className="mist mist-one" />
        <div className="mist mist-two" />
        <section className="landing-card">
          <div className="eyebrow"><Sparkles size={15} /> 文字修仙 · 肉鸽原型</div>
          <h1>逆命仙途</h1>
          <p className="tagline">每一次选择，都是下一世的伏笔。</p>
          <p className="intro">在寿元耗尽之前，以有限的灵力和无法重来的因果，走出属于你的飞升之路。</p>

          <label className="field-label" htmlFor="player-name">道号</label>
          <input
            id="player-name"
            className="name-input"
            value={name}
            onChange={(event) => setName(event.target.value)}
            onKeyDown={(event) => event.key === 'Enter' && void begin()}
            placeholder="例如：顾长生"
            maxLength={16}
          />

          <div className="field-label">选择出身</div>
          <div className="origin-grid">
            {origins.map((item) => (
              <button
                className={`origin-card ${origin === item.value ? 'selected' : ''}`}
                key={item.value}
                onClick={() => setOrigin(item.value)}
                type="button"
              >
                <span>{item.value}</span>
                <small>{item.description}</small>
              </button>
            ))}
          </div>

          {error && <p className="error-text"><TriangleAlert size={15} />{error}</p>}
          <button className="primary-button wide" disabled={loading} onClick={() => void begin()} type="button">
            {loading ? '正在开辟识海…' : '开始这一世'} <ArrowRight size={18} />
          </button>
          <div className="restore-divider"><span>或</span></div>
          <div className="restore-box">
            <label className="field-label" htmlFor="restore-id">恢复已有存档</label>
            <div className="restore-row">
              <input
                id="restore-id"
                className="name-input"
                value={restoreId}
                onChange={(event) => setRestoreId(event.target.value)}
                onKeyDown={(event) => event.key === 'Enter' && void restore()}
                placeholder="粘贴游戏 ID"
              />
              <button className="ghost-button restore-button" disabled={loading} onClick={() => void restore()} type="button">
                <Download size={15} />恢复
              </button>
            </div>
          </div>
          <p className="technical-note">React 前端 · Java Spring Boot · MySQL 存档</p>
        </section>
      </main>
    )
  }

  const activeNode = run.map.nodes.find((node) => node.id === run.currentNodeId)
  const statusLabel = run.status === 'RUNNING' ? '命数未定' : '旅程已结'

  return (
    <main className="page-shell game-shell">
      <header className="game-header">
        <div>
          <div className="eyebrow">
            <Compass size={15} />
            {activeNode ? `第 ${run.currentFloor + 1} 层 · ${activeNode.label}` : `第 ${run.currentFloor + 1} 层 · ${statusLabel}`}
          </div>
          <h1>{run.playerName}<span> · {run.realm}</span></h1>
        </div>
        <button className="ghost-button" onClick={reset} type="button"><RotateCcw size={16} />重开</button>
      </header>

      <section className="stats-grid">
        <Stat icon={<Heart size={17} />} label="气血" value={run.health} color="red" />
        <Stat icon={<Flame size={17} />} label="灵力" value={run.spirit} color="blue" />
        <Stat icon={<Hourglass size={17} />} label="寿元" value={run.lifespan} color="gold" />
        <Stat icon={<Sparkles size={17} />} label="因果" value={run.karma} color="purple" />
      </section>

      {!activeNode && <RouteMapPanel run={run} loading={loading} onEnter={(nodeId) => void enter(nodeId)} />}

      {activeNode && (
        <section className="event-card">
          <div className="event-mark">{nodeIcons[activeNode.type] ?? '缘'}</div>
          <div className="event-content">
            <div className="event-kicker-row">
              <p className="event-kicker">当前节点 · {activeNode.label}</p>
              <span className={`rarity-badge ${run.event.rarity}`}>{run.event.rarity}</span>
              {run.event.repeatable && <span className="repeatable-badge">可重复</span>}
            </div>
            <h2>{run.event.title}</h2>
            <p className="event-description">{run.event.description}</p>
            {run.status === 'RUNNING' ? (
              <div className="choices-list">
                {run.event.choices.map((choice) => (
                  <button className="choice-button" disabled={loading} key={choice.index} onClick={() => void choose(choice.index)} type="button">
                    <span className="choice-index">{String(choice.index + 1).padStart(2, '0')}</span>
                    <span className="choice-copy"><strong>{choice.label}</strong><small>{choice.hint}</small></span>
                    <ArrowRight size={17} />
                  </button>
                ))}
              </div>
            ) : (
              <div className={`result-banner ${run.status === 'ASCENDED' ? 'ascended' : 'dead'}`}>
                {run.status === 'ASCENDED' ? '你已完成这一轮修行。' : '此身道途已断。'}
                <button className="inline-button" onClick={reset} type="button">再走一遭</button>
              </div>
            )}
          </div>
        </section>
      )}

      {run.status !== 'RUNNING' && run.ending && (
        <section className={`ending-card ${run.status === 'ASCENDED' ? 'ascended' : 'fallen'}`}>
          <div className="ending-seal">{run.status === 'ASCENDED' ? '终' : '劫'}</div>
          <div>
            <p className="event-kicker">因果簿 · 本局结局</p>
            <h2>{run.ending.title}</h2>
            <p>{run.ending.description}</p>
          </div>
        </section>
      )}

      {error && <p className="error-text game-error"><TriangleAlert size={15} />{error}</p>}
      <section className="log-card">
        <div className="section-heading"><span>因果簿</span><small>本次操作记录</small></div>
        {run.logs.length === 0 ? <p className="empty-log">你还没有做出选择。</p> : run.logs.map((log, index) => <p key={`${log}-${index}`}><span>{String(index + 1).padStart(2, '0')}</span>{log}</p>)}
      </section>
    </main>
  )
}

function RouteMapPanel({ run, loading, onEnter }: { run: GameRun; loading: boolean; onEnter: (nodeId: string) => void }) {
  const floors = Array.from({ length: run.map.totalFloors }, (_, floor) => run.map.nodes.filter((node) => node.floor === floor))

  return (
    <section className="route-card">
      <div className="route-heading">
        <div>
          <p className="event-kicker">本局路线图 · SEED RUN</p>
          <h2>踏入相邻节点</h2>
        </div>
        <span className="route-progress">{run.currentFloor + 1} / {run.map.totalFloors} 层</span>
      </div>
      <p className="route-description">路线由本局 seed 生成。只有金色高亮节点可以进入，完成节点后，它连接的下一层路线才会解锁。</p>
      <div className="route-map" aria-label="本局修仙路线图">
        {floors.map((floorNodes, floorIndex) => {
          const nextFloorNodes = floors[floorIndex + 1] ?? []
          return (
            <div className="route-floor-group" key={floorIndex}>
              <div className="floor-label">{floorIndex === run.map.totalFloors - 1 ? '渡劫层' : `第 ${floorIndex + 1} 层`}</div>
              <div className="route-node-grid">
                {floorNodes.map((node) => <RouteNode key={node.id} node={node} loading={loading} onEnter={onEnter} />)}
              </div>
              {nextFloorNodes.length > 0 && <ConnectionBand from={floorNodes} to={nextFloorNodes} />}
            </div>
          )
        })}
      </div>
      <div className="route-legend">
        <span><i className="legend-dot available" />可进入</span>
        <span><i className="legend-dot cleared" />已完成</span>
        <span><i className="legend-dot locked" />未解锁</span>
        <span><i className="legend-dot boss" />Boss</span>
      </div>
    </section>
  )
}

function RouteNode({ node, loading, onEnter }: { node: MapNode; loading: boolean; onEnter: (nodeId: string) => void }) {
  const canEnter = node.status === 'AVAILABLE'
  return (
    <button
      className={`route-node ${node.status.toLowerCase()} ${node.type.toLowerCase()}`}
      disabled={!canEnter || loading}
      onClick={() => onEnter(node.id)}
      type="button"
      title={`${node.label} · ${node.rarity}`}
    >
      <span className="route-node-icon">{nodeIcons[node.type] ?? '·'}</span>
      <strong>{node.label}</strong>
      <small>{node.status === 'CLEARED' ? '已完成' : node.rarity}</small>
    </button>
  )
}

function ConnectionBand({ from, to }: { from: MapNode[]; to: MapNode[] }) {
  const targetById = new Map(to.map((node) => [node.id, node]))
  return (
    <svg className="route-connections" viewBox="0 0 300 48" preserveAspectRatio="none" aria-hidden="true">
      {from.flatMap((source) => source.nextNodeIds.map((targetId) => {
        const target = targetById.get(targetId)
        if (!target) return null
        const sourceX = 50 + source.position * 100
        const targetX = 50 + target.position * 100
        return <line key={`${source.id}-${target.id}`} x1={sourceX} y1="0" x2={targetX} y2="48" />
      })).filter(Boolean)}
    </svg>
  )
}

function Stat({ icon, label, value, color }: { icon: ReactNode; label: string; value: number; color: string }) {
  return <div className={`stat-card ${color}`}><span className="stat-icon">{icon}</span><div><small>{label}</small><strong>{value}</strong></div></div>
}

export default App
