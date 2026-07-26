import { useEffect, useState, type ReactNode } from 'react'
import { ArrowRight, Coins, Compass, Crosshair, Database, Download, Flame, Heart, Hourglass, Layers, RefreshCw, RotateCcw, Shield, ShoppingBag, Sparkles, Trash2, TriangleAlert, Trophy } from 'lucide-react'
import { buyShopOffer, chooseEvent, claimReward, clearAuthToken, combatAction, createCharacter, enterNode, getAccountProgress, getCurrentAccount, getAuthToken, getLeaderboard, getRecentRuns, leaveShop, loginAccount, purchaseUnlock, refreshShop, registerAccount, removeShopCard, removeSpecialCard, restoreRun, setAuthToken, skipSpecialRemoval, skipUpgrade, startRun, upgradeCard } from './api'
import type { Account, AccountProgress, BuildStats, Character, CombatView, GameRun, GameRunSummary, LeaderboardEntry, MapNode, RemovalState, RewardOffer, ShopState } from './types'

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
  const [account, setAccount] = useState<Account | null>(null)
  const [authReady, setAuthReady] = useState(false)
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [authUsername, setAuthUsername] = useState('')
  const [authPassword, setAuthPassword] = useState('')
  const [authCharacterName, setAuthCharacterName] = useState('')
  const [selectedCharacterId, setSelectedCharacterId] = useState('')
  const [newCharacterName, setNewCharacterName] = useState('')
  const [newCharacterOrigin, setNewCharacterOrigin] = useState(origins[0].value)
  const [name, setName] = useState('')
  const [origin, setOrigin] = useState(origins[0].value)
  const [restoreId, setRestoreId] = useState('')
  const [run, setRun] = useState<GameRun | null>(null)
  const [progress, setProgress] = useState<AccountProgress | null>(null)
  const [recentRuns, setRecentRuns] = useState<GameRunSummary[]>([])
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!getAuthToken()) {
      setAuthReady(true)
      return
    }
    void getCurrentAccount().then((currentAccount) => {
      setAccount(currentAccount)
      const firstCharacter = currentAccount.characters[0]
      if (firstCharacter) {
        setSelectedCharacterId(firstCharacter.id)
        setName(firstCharacter.name)
        setOrigin(firstCharacter.origin)
      }
    }).catch(() => {
      clearAuthToken()
    }).finally(() => setAuthReady(true))
  }, [])

  useEffect(() => {
    const handleAuthExpired = () => {
      setAccount(null)
      setRun(null)
      setRecentRuns([])
      setError('登录已过期，请重新登录。')
    }
    window.addEventListener('xiuxian-auth-expired', handleAuthExpired)
    return () => window.removeEventListener('xiuxian-auth-expired', handleAuthExpired)
  }, [])

  useEffect(() => {
    if (!account) {
      setRecentRuns([])
      setProgress(null)
      return
    }
    void getRecentRuns().then(setRecentRuns).catch(() => undefined)
    void getAccountProgress().then(setProgress).catch(() => undefined)
  }, [account, run?.status, run?.id])

  useEffect(() => {
    if (run?.accountProgress) setProgress(run.accountProgress)
  }, [run?.accountProgress])

  useEffect(() => {
    let cancelled = false
    void getLeaderboard().then((entries) => {
      if (!cancelled) setLeaderboard(entries)
    }).catch(() => undefined)
    return () => { cancelled = true }
  }, [run?.status])

  function selectCharacter(character: Character) {
    setSelectedCharacterId(character.id)
    setName(character.name)
    setOrigin(character.origin)
    setError('')
  }

  async function authenticate() {
    if (!authUsername.trim() || authPassword.length < 8) {
      setError('用户名不能为空，密码至少需要 8 位。')
      return
    }
    if (authMode === 'register' && !authCharacterName.trim()) {
      setError('注册时请先为角色取一个道号。')
      return
    }
    setLoading(true)
    setError('')
    try {
      const response = authMode === 'register'
        ? await registerAccount(authUsername.trim(), authPassword, authCharacterName.trim())
        : await loginAccount(authUsername.trim(), authPassword)
      setAuthToken(response.token)
      setAccount(response.account)
      const firstCharacter = response.account.characters[0]
      if (firstCharacter) selectCharacter(firstCharacter)
      setAuthPassword('')
    } catch (err) {
      setError(err instanceof Error ? err.message : '账号认证失败，请稍后重试。')
    } finally {
      setLoading(false)
    }
  }

  async function addCharacter() {
    if (!newCharacterName.trim()) {
      setError('请输入新角色道号。')
      return
    }
    setLoading(true)
    setError('')
    try {
      const updatedAccount = await createCharacter(newCharacterName.trim(), newCharacterOrigin)
      setAccount(updatedAccount)
      const createdCharacter = updatedAccount.characters[updatedAccount.characters.length - 1]
      if (createdCharacter) selectCharacter(createdCharacter)
      setNewCharacterName('')
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建角色失败。')
    } finally {
      setLoading(false)
    }
  }

  async function buyPermanentUnlock(unlockId: string) {
    setLoading(true)
    setError('')
    try {
      setProgress(await purchaseUnlock(unlockId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '永久解锁失败。')
    } finally {
      setLoading(false)
    }
  }

  function logout() {
    clearAuthToken()
    setAccount(null)
    setRun(null)
    setRecentRuns([])
    setProgress(null)
    setError('')
  }

  async function begin() {
    if (!name.trim()) {
      setError('请先为自己取一个道号。')
      return
    }
    setLoading(true)
    setError('')
    try {
      setRun(await startRun(name, origin, selectedCharacterId))
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

  async function actCombat(action: string) {
    if (!run || run.status !== 'RUNNING' || !run.combat) return
    setLoading(true)
    setError('')
    try {
      setRun(await combatAction(run.id, action))
    } catch (err) {
      setError(err instanceof Error ? err.message : '战斗行动未能送达。')
    } finally {
      setLoading(false)
    }
  }

  async function claimBuildReward(rewardId: string) {
    if (!run || run.status !== 'RUNNING' || run.rewardOffers.length === 0) return
    setLoading(true)
    setError('')
    try {
      setRun(await claimReward(run.id, rewardId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '奖励领取失败。')
    } finally {
      setLoading(false)
    }
  }

  async function upgradeBuildCard(cardId: string) {
    if (!run || run.status !== 'RUNNING' || run.upgradeOptions.length === 0) return
    setLoading(true)
    setError('')
    try {
      setRun(await upgradeCard(run.id, cardId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '卡牌升级失败。')
    } finally {
      setLoading(false)
    }
  }

  async function skipBuildUpgrade() {
    if (!run || run.status !== 'RUNNING' || run.upgradeOptions.length === 0) return
    setLoading(true)
    setError('')
    try {
      setRun(await skipUpgrade(run.id))
    } catch (err) {
      setError(err instanceof Error ? err.message : '无法结束闭关。')
    } finally {
      setLoading(false)
    }
  }

  async function buyOffer(offerId: string) {
    if (!run || run.status !== 'RUNNING' || !run.shop) return
    setLoading(true)
    setError('')
    try {
      setRun(await buyShopOffer(run.id, offerId))
    } catch (err) {
      setError(err instanceof Error ? err.message : '坊市购买失败。')
    } finally {
      setLoading(false)
    }
  }

  async function refreshCurrentShop() {
    if (!run || run.status !== 'RUNNING' || !run.shop) return
    setLoading(true)
    setError('')
    try {
      setRun(await refreshShop(run.id))
    } catch (err) {
      setError(err instanceof Error ? err.message : '坊市刷新失败。')
    } finally {
      setLoading(false)
    }
  }

  async function removeCard(cardId: string) {
    if (!run || run.status !== 'RUNNING') return
    setLoading(true)
    setError('')
    try {
      setRun(await (run.shop ? removeShopCard(run.id, cardId) : removeSpecialCard(run.id, cardId)))
    } catch (err) {
      setError(err instanceof Error ? err.message : '卡牌移除失败。')
    } finally {
      setLoading(false)
    }
  }

  async function leaveCurrentShop() {
    if (!run || run.status !== 'RUNNING' || !run.shop) return
    setLoading(true)
    setError('')
    try {
      setRun(await leaveShop(run.id))
    } catch (err) {
      setError(err instanceof Error ? err.message : '离开坊市失败。')
    } finally {
      setLoading(false)
    }
  }

  async function skipCurrentRemoval() {
    if (!run || run.status !== 'RUNNING' || !run.removal) return
    setLoading(true)
    setError('')
    try {
      setRun(await skipSpecialRemoval(run.id))
    } catch (err) {
      setError(err instanceof Error ? err.message : '暂不移除卡牌。')
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

  if (!authReady) {
    return <main className="page-shell landing-shell"><section className="landing-card auth-loading"><div className="eyebrow"><Sparkles size={15} /> 正在读取道庭身份</div><p className="intro">请稍候，正在恢复登录状态。</p></section></main>
  }

  if (!account) {
    return (
      <main className="page-shell landing-shell">
        <div className="mist mist-one" />
        <div className="mist mist-two" />
        <AuthPanel
          mode={authMode}
          username={authUsername}
          password={authPassword}
          characterName={authCharacterName}
          loading={loading}
          error={error}
          onModeChange={(mode) => { setAuthMode(mode); setError('') }}
          onUsernameChange={setAuthUsername}
          onPasswordChange={setAuthPassword}
          onCharacterNameChange={setAuthCharacterName}
          onSubmit={() => void authenticate()}
        />
        <div className="landing-card landing-leaderboard"><LeaderboardPanel entries={leaderboard} /></div>
      </main>
    )
  }

  if (!run) {
    return (
      <main className="page-shell landing-shell">
        <div className="mist mist-one" />
        <div className="mist mist-two" />
        <section className="landing-card">
          <div className="landing-account-bar"><span><Database size={14} /> {account.username}</span><button className="inline-button" onClick={logout} type="button">退出账号</button></div>
          <div className="eyebrow"><Sparkles size={15} /> 文字修仙 · 肉鸽原型</div>
          <h1>逆命仙途</h1>
          <p className="tagline">每一次选择，都是下一世的伏笔。</p>
          <p className="intro">在寿元耗尽之前，以有限的灵力和无法重来的因果，走出属于你的飞升之路。</p>

          <div className="field-label">选择角色</div>
          <div className="character-grid">
            {account.characters.map((character) => (
              <button className={`character-card ${selectedCharacterId === character.id ? 'selected' : ''}`} key={character.id} onClick={() => selectCharacter(character)} type="button">
                <strong>{character.name}</strong><small>{character.origin}</small>
              </button>
            ))}
          </div>

          <details className="new-character-box">
            <summary>创建新的角色分身</summary>
            <div className="new-character-fields">
              <input className="name-input" value={newCharacterName} onChange={(event) => setNewCharacterName(event.target.value)} placeholder="新的道号" maxLength={16} />
              <select className="name-input" value={newCharacterOrigin} onChange={(event) => setNewCharacterOrigin(event.target.value)}>
                {origins.map((item) => <option key={item.value} value={item.value}>{item.value}</option>)}
              </select>
              <button className="ghost-button" disabled={loading} onClick={() => void addCharacter()} type="button">创建角色</button>
            </div>
          </details>

          <label className="field-label" htmlFor="player-name">当前道号</label>
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
          {progress && <ProgressPanel progress={progress} loading={loading} onUnlock={(unlockId) => void buyPermanentUnlock(unlockId)} />}
          {recentRuns.length > 0 && <RecentRunsPanel runs={recentRuns} loading={loading} onRestore={(id) => { setRestoreId(id); void restoreRun(id).then(setRun).catch((err) => setError(err instanceof Error ? err.message : '存档恢复失败。')) }} />}
          <LeaderboardPanel entries={leaderboard} />
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
        <div className="game-header-actions"><span className="account-chip"><Database size={14} />{account.username}</span><button className="ghost-button" onClick={reset} type="button"><RotateCcw size={16} />重开</button><button className="ghost-button" onClick={logout} type="button">退出</button></div>
      </header>

      <section className="stats-grid">
        <Stat icon={<Heart size={17} />} label="气血" value={run.health} color="red" />
        <Stat icon={<Flame size={17} />} label="灵力" value={run.spirit} color="blue" />
        <Stat icon={<Hourglass size={17} />} label="寿元" value={run.lifespan} color="gold" />
        <Stat icon={<Sparkles size={17} />} label="因果" value={run.karma} color="purple" />
        <Stat icon={<Coins size={17} />} label="灵石" value={run.spiritStones} color="cyan" />
      </section>

      <BuildPanel cards={run.build} />
      <BuildStatsPanel stats={run.buildStats} />
      {run.accountProgress && <ProgressSummary progress={run.accountProgress} />}
      <ConfigStatusPanel status={run.configStatus} />

      {run.rewardOffers.length > 0 && (
        <RewardPanel offers={run.rewardOffers} loading={loading} onClaim={(rewardId) => void claimBuildReward(rewardId)} />
      )}

      {run.rewardOffers.length === 0 && run.upgradeOptions.length > 0 && (
        <UpgradePanel
          cards={run.upgradeOptions}
          spiritStones={run.spiritStones}
          loading={loading}
          onUpgrade={(cardId) => void upgradeBuildCard(cardId)}
          onSkip={() => void skipBuildUpgrade()}
        />
      )}

      {run.rewardOffers.length === 0 && run.upgradeOptions.length === 0 && run.shop && (
        <ShopPanel
          shop={run.shop}
          build={run.build}
          spiritStones={run.spiritStones}
          loading={loading}
          onBuy={(offerId) => void buyOffer(offerId)}
          onRefresh={() => void refreshCurrentShop()}
          onRemove={(cardId) => void removeCard(cardId)}
          onLeave={() => void leaveCurrentShop()}
        />
      )}

      {run.rewardOffers.length === 0 && run.upgradeOptions.length === 0 && !run.shop && run.removal && (
        <RemovalPanel
          removal={run.removal}
          spiritStones={run.spiritStones}
          loading={loading}
          onRemove={(cardId) => void removeCard(cardId)}
          onSkip={() => void skipCurrentRemoval()}
        />
      )}

      {run.rewardOffers.length === 0 && run.upgradeOptions.length === 0 && !run.shop && !run.removal && run.combat && (
        <CombatPanel combat={run.combat} spirit={run.spirit} loading={loading} onAction={(action) => void actCombat(action)} />
      )}

      {run.rewardOffers.length === 0 && run.upgradeOptions.length === 0 && !run.shop && !run.removal && !run.combat && !activeNode && (
        <RouteMapPanel run={run} loading={loading} onEnter={(nodeId) => void enter(nodeId)} />
      )}

      {activeNode && !run.combat && (
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

      {run.settlement && <SettlementPanel settlement={run.settlement} />}

      {error && <p className="error-text game-error"><TriangleAlert size={15} />{error}</p>}
      <section className="log-card">
        <div className="section-heading"><span>因果簿</span><small>本次操作记录</small></div>
        {run.logs.length === 0 ? <p className="empty-log">你还没有做出选择。</p> : run.logs.map((log, index) => <p key={`${log}-${index}`}><span>{String(index + 1).padStart(2, '0')}</span>{log}</p>)}
      </section>
    </main>
  )
}

function AuthPanel({
  mode, username, password, characterName, loading, error, onModeChange,
  onUsernameChange, onPasswordChange, onCharacterNameChange, onSubmit,
}: {
  mode: 'login' | 'register'
  username: string
  password: string
  characterName: string
  loading: boolean
  error: string
  onModeChange: (mode: 'login' | 'register') => void
  onUsernameChange: (value: string) => void
  onPasswordChange: (value: string) => void
  onCharacterNameChange: (value: string) => void
  onSubmit: () => void
}) {
  return (
    <section className="landing-card auth-card">
      <div className="eyebrow"><Sparkles size={15} /> 逆命仙途 · 道庭身份</div>
      <h1>{mode === 'login' ? '入道' : '立誓入世'}</h1>
      <p className="tagline">每个账号拥有独立角色与存档。</p>
      <div className="auth-tabs">
        <button className={mode === 'login' ? 'active' : ''} onClick={() => onModeChange('login')} type="button">登录</button>
        <button className={mode === 'register' ? 'active' : ''} onClick={() => onModeChange('register')} type="button">注册</button>
      </div>
      <label className="field-label" htmlFor="auth-username">账号</label>
      <input id="auth-username" className="name-input" value={username} onChange={(event) => onUsernameChange(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && onSubmit()} placeholder="3-40 位字母、数字或下划线" autoComplete="username" />
      <label className="field-label" htmlFor="auth-password">密码</label>
      <input id="auth-password" className="name-input" value={password} onChange={(event) => onPasswordChange(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && onSubmit()} type="password" placeholder="至少 8 位" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
      {mode === 'register' && <>
        <label className="field-label" htmlFor="auth-character">初始道号</label>
        <input id="auth-character" className="name-input" value={characterName} onChange={(event) => onCharacterNameChange(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && onSubmit()} placeholder="例如：顾长生" maxLength={16} />
      </>}
      {error && <p className="error-text"><TriangleAlert size={15} />{error}</p>}
      <button className="primary-button wide" disabled={loading} onClick={onSubmit} type="button">
        {loading ? '正在入道…' : mode === 'login' ? '进入道庭' : '创建账号'} <ArrowRight size={18} />
      </button>
      <p className="technical-note">密码仅以 PBKDF2 哈希保存，游戏接口使用 JWT 隔离账号存档。</p>
    </section>
  )
}

function ProgressPanel({ progress, loading, onUnlock }: { progress: AccountProgress; loading: boolean; onUnlock: (unlockId: string) => void }) {
  return (
    <section className="progress-panel">
      <div className="section-heading"><span><Sparkles size={15} />因果轮回</span><small>永久进度 · 账号共享</small></div>
      <div className="progress-metrics">
        <div><strong>{progress.causalityPoints}</strong><small>可用因果点</small></div>
        <div><strong>{progress.totalCausalityEarned}</strong><small>累计获得</small></div>
        <div><strong>{progress.completedRuns}</strong><small>完成轮回</small></div>
        <div><strong>{progress.totalRuns}</strong><small>创建存档</small></div>
        <div><strong>{progress.highestFloor}</strong><small>最高抵达层</small></div>
        <div><strong>{progress.bestScore}</strong><small>最高轮回分</small></div>
        <div><strong>{progress.ascendedRuns}</strong><small>飞升轮回</small></div>
        <div><strong>{progress.deadRuns}</strong><small>道途轮回</small></div>
        <div><strong>{progress.achievementCount}</strong><small>已解锁成就</small></div>
        <div><strong>{progress.totalAchievementRewards}</strong><small>成就因果收益</small></div>
      </div>
      <div className="unlock-list">
        {progress.unlocks.map((unlock) => (
          <div className={`unlock-row ${unlock.unlocked ? 'unlocked' : ''}`} key={unlock.id}>
            <div><strong>{unlock.name}</strong><small>{unlock.description} · {unlock.effectText}</small></div>
            {unlock.unlocked ? <span className="unlock-owned">已觉醒</span> : <button className="ghost-button" disabled={loading || progress.causalityPoints < unlock.cost} onClick={() => onUnlock(unlock.id)} type="button"><Coins size={13} />{unlock.cost} 解锁</button>}
          </div>
        ))}
      </div>
      <div className="achievement-list">
        <div className="stats-block-title"><Trophy size={14} />账号成就 <small>{progress.achievementCount}/{progress.achievements.length} 已解锁</small></div>
        <div className="achievement-grid">
          {progress.achievements.map((achievement) => (
            <article className={`achievement-row ${achievement.unlocked ? 'unlocked' : ''}`} key={achievement.id}>
              <div className="achievement-icon"><Trophy size={14} /></div>
              <div><strong>{achievement.name}</strong><small>{achievement.description}</small><em>{achievement.conditionText} · 达成奖励 +{achievement.rewardCausality} 因果</em></div>
              <span>{achievement.unlocked ? '已达成' : '未达成'}</span>
            </article>
          ))}
        </div>
      </div>
      {progress.recentSettlements.length > 0 && <div className="settlement-history"><div className="stats-block-title"><Trophy size={14} />最近结算</div>{progress.recentSettlements.slice(0, 3).map((settlement) => <div className="history-row" key={settlement.runId}><span>{settlement.playerName} · {settlement.endingTitle}</span><b>+{settlement.causalityEarned}</b></div>)}</div>}
    </section>
  )
}

function ProgressSummary({ progress }: { progress: AccountProgress }) {
  const unlocked = progress.unlocks.filter((item) => item.unlocked).length
  return <section className="progress-summary"><span><Sparkles size={14} />账号因果 {progress.causalityPoints}</span><span>永久解锁 {unlocked}/{progress.unlocks.length}</span><span>完成轮回 {progress.completedRuns}</span><span><Trophy size={14} />成就 {progress.achievementCount}/{progress.achievements.length}</span><span>最高层数 {progress.highestFloor}</span></section>
}

function RecentRunsPanel({ runs, loading, onRestore }: { runs: GameRunSummary[]; loading: boolean; onRestore: (id: string) => void }) {
  return (
    <section className="recent-runs-panel">
      <div className="section-heading"><span><Database size={15} />我的存档</span><small>仅当前账号可见</small></div>
      <div className="recent-runs-list">
        {runs.map((item) => (
          <div className="recent-run-row" key={item.id}>
            <div><strong>{item.playerName}</strong><small>{item.status === 'RUNNING' ? '修行中' : item.status === 'ASCENDED' ? '已飞升' : '已陨落'} · 第 {item.currentFloor + 1} 层 · 回合 {item.turn}</small></div>
            <button className="ghost-button" disabled={loading} onClick={() => onRestore(item.id)} type="button"><Download size={14} />恢复</button>
          </div>
        ))}
      </div>
    </section>
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

function BuildPanel({ cards }: { cards: GameRun['build'] }) {
  return (
    <section className="build-card-panel">
      <div className="section-heading">
        <span>本局构筑 <small className="build-count">{cards.length} 张</small></span>
        <small>功法 · 法宝 · 符箓</small>
      </div>
      <div className="build-card-list">
        {cards.map((card) => (
          <article className={'build-card ' + card.rarity} key={card.id}>
            <div className="build-card-topline">
              <span className="card-category">{card.category}</span>
              <span className="card-archetype">{card.archetype}</span>
              <span className="card-level">Lv.{card.upgradeLevel + 1}</span>
              <span className={'rarity-badge ' + card.rarity}>{card.rarity}</span>
            </div>
            <strong>{card.name}</strong>
            <small>{card.effectText}</small>
          </article>
        ))}
      </div>
    </section>
  )
}

function BuildStatsPanel({ stats }: { stats: BuildStats }) {
  const categories = ['功法', '法宝', '符箓']
  const archetypes = ['剑修', '丹修', '体修', '鬼修']
  return (
    <section className="build-stats-panel">
      <div className="section-heading">
        <span>构筑详情 <small className="build-count">{stats.activeCards} 张有效卡</small></span>
        <small>只统计 ACTIVE 卡牌</small>
      </div>
      <div className="build-stats-layout">
        <div className="build-counts-block">
          <div className="stats-block-title"><Layers size={15} />卡牌构成</div>
          <div className="count-grid">
            {categories.map((category) => <div className="count-item" key={category}><strong>{stats.categoryCounts[category] ?? 0}</strong><small>{category}</small></div>)}
          </div>
          <div className="archetype-counts">
            {archetypes.map((archetype) => <span key={archetype}>{archetype} <b>{stats.archetypeCounts[archetype] ?? 0}</b></span>)}
          </div>
        </div>
        <div className="synergy-block">
          <div className="stats-block-title"><Sparkles size={15} />流派协同</div>
          <div className="synergy-grid">
            {stats.synergies.map((synergy) => (
              <article className={`synergy-item ${synergy.active ? 'active' : ''}`} key={synergy.archetype}>
                <div><strong>{synergy.archetype}</strong><span>{synergy.count} 张</span></div>
                <small>{synergy.active ? synergy.effectText : '再获得一张即可激活协同'}</small>
              </article>
            ))}
          </div>
        </div>
      </div>
      <div className="battle-bonus-row">
        <span><Heart size={14} />战斗气血 +{stats.battleHealthBonus}</span>
        <span><Flame size={14} />战斗灵力 +{stats.battleSpiritBonus}</span>
        <span><Hourglass size={14} />战斗寿元 {formatSigned(stats.battleLifespanBonus)}</span>
        <span><Sparkles size={14} />战斗因果 {formatSigned(stats.battleKarmaBonus)}</span>
        <span><Crosshair size={14} />伤害 +{stats.combatDamageBonus}</span>
        <span><Shield size={14} />护盾 +{stats.combatBlockBonus}</span>
        <span><Flame size={14} />调息 +{stats.combatSpiritGain}</span>
        <span><Sparkles size={14} />战技中毒 +{stats.combatPoisonBonus}</span>
      </div>
    </section>
  )
}

function ConfigStatusPanel({ status }: { status: GameRun['configStatus'] }) {
  return (
    <section className="config-status-panel">
      <div className="section-heading">
        <span><Database size={15} />内容配置中心</span>
        <span className={`config-health ${status.valid ? 'valid' : 'invalid'}`}>{status.valid ? '校验通过' : '需要检查'}</span>
      </div>
      <div className="config-status-grid">
        <span><strong>{status.activeEvents}</strong><small>启用事件</small></span>
        <span><strong>{status.activeEndings}</strong><small>启用结局</small></span>
        <span><strong>V{status.maxVersion}</strong><small>最高版本</small></span>
      </div>
      <p className="config-status-note">本局路线、事件正文、稀有度和选项由服务端数据库配置驱动。</p>
    </section>
  )
}

function SettlementPanel({ settlement }: { settlement: NonNullable<GameRun['settlement']> }) {
  const breakdown = settlement.scoreBreakdown
  return (
    <section className={`settlement-panel ${settlement.status === 'ASCENDED' ? 'ascended' : 'fallen'}`}>
      <div className="settlement-score"><Trophy size={22} /><strong>{settlement.score}</strong><small>本局积分</small></div>
      <div>
        <p className="event-kicker">结算快照 · {settlement.status === 'ASCENDED' ? '飞升' : '道途断绝'}</p>
        <h2>{settlement.endingTitle}</h2>
        <p>抵达第 {settlement.floorReached} 层 · {settlement.turn} 回合 · {settlement.eliteCount} 次精英挑战 · {settlement.activeCards} 张有效卡</p>
        <div className="score-breakdown" aria-label="本局积分明细">
          <span><small>进度</small><b>+{breakdown.progressBonus}</b></span>
          <span><small>回合</small><b>+{breakdown.turnBonus}</b></span>
          <span><small>属性</small><b>+{breakdown.healthBonus + breakdown.spiritBonus + breakdown.lifespanBonus}</b></span>
          <span><small>因果/资源</small><b>+{breakdown.karmaBonus + breakdown.spiritStonesBonus}</b></span>
          <span><small>构筑</small><b>+{breakdown.buildBonus}</b></span>
          <span><small>精英</small><b>+{breakdown.eliteBonus}</b></span>
          <span><small>飞升</small><b>+{breakdown.ascensionBonus}</b></span>
        </div>
      </div>
    </section>
  )
}

function LeaderboardPanel({ entries }: { entries: LeaderboardEntry[] }) {
  return (
    <section className="leaderboard-panel">
      <div className="section-heading"><span><Trophy size={15} />天道榜</span><small>已完成旅程</small></div>
      {entries.length === 0 ? <p className="leaderboard-empty">还没有完成的修士，等你留下第一份结算。</p> : (
        <div className="leaderboard-list">
          {entries.slice(0, 5).map((entry) => (
            <div className="leaderboard-row" key={entry.runId}>
              <span className="leaderboard-rank">{String(entry.rank).padStart(2, '0')}</span>
              <span className="leaderboard-player"><strong>{entry.playerName}</strong><small>{entry.endingTitle} · 第 {entry.floorReached} 层</small></span>
              <strong className="leaderboard-score">{entry.score}</strong>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function CombatPanel({ combat, spirit, loading, onAction }: {
  combat: CombatView
  spirit: number
  loading: boolean
  onAction: (action: string) => void
}) {
  const enemyHealthPercent = Math.max(0, Math.round((combat.health / combat.maxHealth) * 100))
  return (
    <section className="combat-panel">
      <div className="combat-heading">
        <div>
          <p className="event-kicker"><Crosshair size={14} />回合战斗 · 第 {combat.turn} 回合</p>
          <h2>{combat.enemyName}<span> · {combat.enemyType}</span></h2>
          <p className="combat-description">{combat.enemyDescription}</p>
        </div>
        <div className="combat-intent"><small>敌人意图</small><strong>{combat.intentText}</strong></div>
      </div>
      <div className="combat-health-row">
        <div className="combat-health-label"><span>敌方气血</span><strong>{combat.health} / {combat.maxHealth}</strong></div>
        <div className="combat-health-track"><i style={{ width: `${enemyHealthPercent}%` }} /></div>
      </div>
      <div className="combat-status-row">
        <span><Shield size={14} />敌方护盾 {combat.enemyBlock}</span>
        <span><Sparkles size={14} />敌方中毒 {combat.enemyPoison}</span>
        <span><Shield size={14} />我方护盾 {combat.playerBlock}</span>
        <span><Sparkles size={14} />中毒 {combat.playerPoison}</span>
        <span><Flame size={14} />灵力 {spirit}</span>
      </div>
      <div className="combat-actions">
        {combat.actions.map((action) => (
          <button
            className={`combat-action ${action.id.toLowerCase()}`}
            disabled={loading || spirit < action.spiritCost}
            key={action.id}
            onClick={() => onAction(action.id)}
            type="button"
          >
            <strong>{action.label}</strong>
            <small>{action.spiritCost > 0 ? `消耗 ${action.spiritCost} 灵力 · ` : ''}{action.hint}</small>
          </button>
        ))}
      </div>
      <div className="combat-log">
        {combat.recentLog.map((log, index) => <p key={`${log}-${index}`}>{log}</p>)}
      </div>
    </section>
  )
}

function ShopPanel({ shop, build, spiritStones, loading, onBuy, onRefresh, onRemove, onLeave }: {
  shop: ShopState
  build: GameRun['build']
  spiritStones: number
  loading: boolean
  onBuy: (offerId: string) => void
  onRefresh: () => void
  onRemove: (cardId: string) => void
  onLeave: () => void
}) {
  const canRefresh = shop.refreshCount < shop.refreshLimit && shop.nextRefreshCost > 0 && spiritStones >= shop.nextRefreshCost
  const canRemove = !shop.removalUsed && spiritStones >= shop.removalCost && build.length > 1
  return (
    <section className="shop-panel">
      <div className="shop-heading">
        <div>
          <p className="event-kicker"><ShoppingBag size={14} />坊市 · 构筑整理</p>
          <h2>挑选你的修行资源</h2>
        </div>
        <span className="shop-stones"><Coins size={14} />{spiritStones} 灵石</span>
      </div>
      <p className="route-description">购买会立即加入构筑；刷新会替换当前未购买商品。坊市移除卡牌消耗服务端标记的灵石，且每个坊市仅限一次。</p>
      <div className="shop-offer-grid">
        {shop.offers.length === 0 ? <p className="empty-shop">商品已售罄，可以离开坊市继续赶路。</p> : shop.offers.map((offer) => (
          <article className={`shop-offer ${offer.rarity}`} key={offer.id}>
            <div className="reward-card-topline"><span>{offer.category} · {offer.archetype}</span><span className={`rarity-badge ${offer.rarity}`}>{offer.rarity}</span></div>
            <strong>{offer.name}</strong>
            <p>{offer.description}</p>
            <small>{offer.effectText}</small>
            <button className="shop-action-button" disabled={loading || spiritStones < offer.price} onClick={() => onBuy(offer.id)} type="button">
              <Coins size={14} />{offer.price} 灵石 · 购买
            </button>
          </article>
        ))}
      </div>
      <div className="shop-controls">
        <button className="ghost-button" disabled={loading || !canRefresh} onClick={onRefresh} type="button">
          <RefreshCw size={14} />{shop.refreshCount >= shop.refreshLimit ? '刷新次数已用尽' : `刷新 · ${shop.nextRefreshCost} 灵石 (${shop.refreshCount}/${shop.refreshLimit})`}
        </button>
        <button className="ghost-button" disabled={loading} onClick={onLeave} type="button">离开坊市</button>
      </div>
      <div className="shop-removal-block">
        <div className="stats-block-title"><Trash2 size={15} />坊市移除 <small>每个坊市一次 · {shop.removalCost} 灵石</small></div>
        <div className="removal-card-grid">
          {build.map((card) => (
            <button className="removal-card" disabled={loading || !canRemove} key={card.id} onClick={() => onRemove(card.id)} type="button">
              <span>{card.name}</span><small>{card.archetype} · Lv.{card.upgradeLevel + 1}</small><Trash2 size={13} />
            </button>
          ))}
        </div>
        {build.length <= 1 && <p className="shop-hint">不能移除最后一张有效卡牌。</p>}
        {shop.removalUsed && <p className="shop-hint">本次坊市已经移除过卡牌。</p>}
      </div>
    </section>
  )
}

function RemovalPanel({ removal, spiritStones, loading, onRemove, onSkip }: {
  removal: RemovalState
  spiritStones: number
  loading: boolean
  onRemove: (cardId: string) => void
  onSkip: () => void
}) {
  return (
    <section className="removal-panel">
      <div className="shop-heading">
        <div>
          <p className="event-kicker"><Trash2 size={14} />特殊事件 · 因果清理</p>
          <h2>{removal.title}</h2>
        </div>
        <span className="shop-stones">{removal.cost === 0 ? '免费一次' : `${removal.cost} 灵石`}</span>
      </div>
      <p className="route-description">选择一张卡牌移出本局构筑。记录会保留在存档历史中，但移除后的卡牌不再提供战斗加成或流派协同。</p>
      <div className="removal-card-grid special-removal-grid">
        {removal.options.map((card) => (
          <button className="removal-card" disabled={loading || spiritStones < removal.cost} key={card.id} onClick={() => onRemove(card.id)} type="button">
            <span>{card.name}</span><small>{card.archetype} · {card.category}</small><Trash2 size={14} />
          </button>
        ))}
      </div>
      {removal.options.length <= 1 && <p className="shop-hint">当前只剩一张有效卡牌，不能移除最后一张。</p>}
      <button className="ghost-button" disabled={loading} onClick={onSkip} type="button">保留这张卡，继续赶路</button>
    </section>
  )
}

function formatSigned(value: number) {
  return value > 0 ? `+${value}` : `${value}`
}

function UpgradePanel({ cards, spiritStones, loading, onUpgrade, onSkip }: {
  cards: GameRun['upgradeOptions']
  spiritStones: number
  loading: boolean
  onUpgrade: (cardId: string) => void
  onSkip: () => void
}) {
  return (
    <section className="upgrade-panel">
      <div className="upgrade-heading">
        <div>
          <p className="event-kicker">休息闭关 · 卡牌强化</p>
          <h2>选择一张卡牌升级</h2>
        </div>
        <span className="upgrade-stones"><Coins size={14} />剩余 {spiritStones} 灵石</span>
      </div>
      <p className="route-description">升级会提高卡牌的战斗加成，并根据卡牌类型返还少量属性。也可以跳过，把灵石留给之后的坊市。</p>
      <div className="upgrade-grid">
        {cards.map((card) => {
          const cost = 25 + card.upgradeLevel * 15
          return (
            <button
              className="upgrade-card"
              disabled={loading || spiritStones < cost}
              key={card.id}
              onClick={() => onUpgrade(card.id)}
              type="button"
            >
              <div className="reward-card-topline">
                <span>{card.category} · Lv.{card.upgradeLevel + 1}</span>
                <span className={'rarity-badge ' + card.rarity}>{card.rarity}</span>
              </div>
              <strong>{card.name}</strong>
              <small>{card.effectText}</small>
              <span className="upgrade-action"><Coins size={14} />{cost} 灵石 · 强化</span>
            </button>
          )
        })}
      </div>
      <button className="ghost-button upgrade-skip" disabled={loading} onClick={onSkip} type="button">暂不升级，继续赶路</button>
    </section>
  )
}

function RewardPanel({ offers, loading, onClaim }: { offers: RewardOffer[]; loading: boolean; onClaim: (rewardId: string) => void }) {
  return (
    <section className="reward-panel">
      <div className="reward-heading">
        <div>
          <p className="event-kicker">战斗结算 · 构筑奖励</p>
          <h2>选择一张加入本局</h2>
        </div>
        <span className="reward-tip">其余奖励将消散</span>
      </div>
      <p className="route-description">战斗、精英和秘境节点会改变你的卡组。选牌后路线才会继续向下一层推进。</p>
      <div className="reward-grid">
        {offers.map((offer) => (
          <button className={'reward-card ' + offer.rarity} disabled={loading} key={offer.id} onClick={() => onClaim(offer.id)} type="button">
            <div className="reward-card-topline">
              <span>{offer.category}</span>
              <span className={'rarity-badge ' + offer.rarity}>{offer.rarity}</span>
            </div>
            <strong>{offer.name}</strong>
            <p>{offer.description}</p>
            <small>{offer.effectText}</small>
            <span className="reward-claim"><ArrowRight size={15} />纳入构筑</span>
          </button>
        ))}
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
