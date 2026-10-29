import { useState, useEffect, useCallback, useRef } from 'react'
import './App.css'
import { CardFace, CardBack, hasCardArt } from './cards'

// ─── Tipi ────────────────────────────────────────────────────────────────────

interface CardDTO {
  nome: string
  descrizione: string
  imagePath: string
  timestamp: number
  seme?: string
  valore?: string
  isEstrazione?: boolean
  ownerName?: string
}

interface PlayerDTO {
  nome: string
  ruolo: string
  descrizioneRuolo?: string
  personaggio: string
  descrizionePersonaggio?: string
  vita: number
  vitaMax: number
  isUmano: boolean
  isTurnoCorrente: boolean
  carteInMano: CardDTO[]
  carteInGioco: CardDTO[]
  arma: string
  portataArma: number
}

interface PromptDTO {
  targetPlayer?: string
  tipo: 'MENU' | 'CARD' | 'YESNO' | 'PERSONA' | 'STRING'
  messaggio: string
  opzioni: string[]
}

interface GameState {
  status: 'IDLE' | 'IN_CORSO' | string
  giocatoreCorrente?: string
  prompt?: PromptDTO
  giocatori?: PlayerDTO[]
  giocatoriEliminati?: PlayerDTO[]
  carteRimanenti?: number
  ultimaCartaScartata?: CardDTO
  lastPlayedCard?: CardDTO
  lastPlayedActor?: string
  lastPlayedTarget?: string
  lastDrawnCard?: CardDTO
  eventi?: string[]
  lobbyCode?: string
  expectedHumans?: number
  joinedHumans?: string[]
  abortMessage?: string
}

// ─── Icone ────────────────────────────────────────────────────────────────────

function Heart({ filled }: { filled: boolean }) {
  return (
    <span className={`heart ${filled ? 'filled' : 'empty'}`}>♥</span>
  )
}
// ─── Rendering Messaggi Formattati ──────────────────────────────────────────
function RichText({ text }: { text: string }) {
  if (!text) return null;

  // Regex per trovare {P:Nome}, {C:Carta} o {I:Icona}
  const parts = text.split(/({[PCI]:[^{}]+})/g);

  return (
    <>
      {parts.map((part, i) => {
        if (part.startsWith('{P:') && part.endsWith('}')) {
          const name = part.substring(3, part.length - 1);
          return <span key={i} className="log-player">{name}</span>;
        }
        if (part.startsWith('{C:') && part.endsWith('}')) {
          const name = part.substring(3, part.length - 1);
          return <span key={i} className="log-card">{name}</span>;
        }
        if (part.startsWith('{I:') && part.endsWith('}')) {
          const icon = part.substring(3, part.length - 1);
          return <span key={i} className="log-icon">{icon}</span>;
        }
        return part;
      })}
    </>
  );
}

const getEventClass = (ev: string): string => {
  const e = ev.toLowerCase()
  if (ev.includes('💥') || e.includes('danno') || e.includes('perso')) return 'log-damage'
  if (ev.includes('🍺') || ev.includes('🍻') || ev.includes('✨') || e.includes('recupera') || e.includes(' PF')) return 'log-heal'
  if (ev.includes('🃏') || ev.includes('🛍️') || e.includes('pesca') || e.includes('prende')) return 'log-card'
  if (ev.includes('🤠') || ev.includes('---')) return 'log-turn'
  if (ev.includes('💀') || e.includes('eliminato') || e.includes('ucciso')) return 'log-death'
  if (ev.includes('🧨') || ev.includes('⛓️') || ev.includes('⚔️') || ev.includes('🏹')) return 'log-special'
  return ''
}

function GameLog({ eventi }: { eventi: string[] }) {
  // Invertiamo l'array in modo che i nuovi messaggi compaiano IN CIMA
  const reversedEvents = [...eventi].reverse()

  return (
    <div className="game-log">
      <div className="game-log-title">📜 Registro di Gioco</div>
      <div className="game-log-entries">
        {reversedEvents.map((ev, i) => (
          <div key={eventi.length - i} className={`log-entry ${getEventClass(ev)}`}>
            <RichText text={ev} />
          </div>
        ))}
      </div>
    </div>
  )
}
// ─── Pre-Lobby ──────────────────────────────────────────────────────────────

function PreLobby({ onSelect }: { onSelect: (mode: 'SINGLE' | 'ONLINE' | 'JOIN') => void }) {
  return (
    <div className="setup-screen pre-lobby-screen">
      <div className="pre-lobby-container">
        <h2 className="setup-title">🤠 Benvenuto su BANG!</h2>
        <p className="setup-subtitle">Scegli come vuoi giocare oggi</p>

        <div className="lobby-cards-container">
          <div className="lobby-card theme-law" onClick={() => onSelect('SINGLE')}>
            <div className="lobby-card-icon">🤖</div>
            <h3>Single Player</h3>
            <p>Sfida 3 o più temibili Bot in una partita classica veloce.</p>
            <button className="btn-primary">Gioca Ora</button>
          </div>

          <div className="lobby-card theme-outlaws" onClick={() => onSelect('ONLINE')}>
            <div className="lobby-card-icon">👑</div>
            <h3>Crea Lobby</h3>
            <p>Crea una nuova partita e invita i tuoi amici con un codice.</p>
            <button className="btn-primary">Crea</button>
          </div>

          <div className="lobby-card theme-renegade" onClick={() => onSelect('JOIN')}>
            <div className="lobby-card-icon">🏇</div>
            <h3>Entra in Lobby</h3>
            <p>Hai ricevuto un codice? Inseriscilo per unirti alla sfida.</p>
            <button className="btn-primary">Entra</button>
          </div>
        </div>

        <p className="pre-lobby-disclaimer">
          Progetto didattico non commerciale, ispirato al gioco di carte BANG!. "BANG!" è un marchio di dV Giochi; questo progetto non è affiliato né approvato da dV Giochi. Le grafiche delle carte sono originali.
        </p>
      </div>
    </div>
  );
}

// ─── Join Screen ────────────────────────────────────────────────────────────

function JoinScreen({ onJoin, onBack }: { onJoin: (code: string, name: string) => Promise<void>, onBack: () => void }) {
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleJoin = async () => {
    if (!code || !name) {
      setError('Inserisci codice e nome!')
      return
    }
    setLoading(true)
    setError('')
    try {
      await onJoin(code, name)
    } catch (e: any) {
      setError(e.message || 'Errore durante il join')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="setup-screen">
      <div className="setup-card">
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: '20px' }}>
          <button className="btn-back" onClick={onBack}>⬅</button>
          <h2 className="setup-title" style={{ marginLeft: '20px', marginBottom: 0 }}>🌵 Entra in Lobby</h2>
        </div>

        <div className="field-group">
          <label>Codice Lobby (4 cifre):</label>
          <input
            className="lobby-input"
            value={code}
            onChange={e => setCode(e.target.value.substring(0, 4))}
            placeholder="0000"
            maxLength={4}
          />
        </div>

        <div className="field-group">
          <label>Il tuo Nome:</label>
          <input
            className="lobby-input"
            value={name}
            onChange={e => setName(e.target.value)}
            placeholder="Tuco"
          />
        </div>

        {error && <p className="error-msg">❌ {error}</p>}

        <button className="btn-primary" onClick={handleJoin} disabled={loading}>
          {loading ? '⏳ Entrando...' : '🚪 Entra'}
        </button>
      </div>
    </div>
  )
}

// ─── Lobby Screen ───────────────────────────────────────────────────────────

function LobbyScreen({ state, myName, isHost, onBack }: { state: GameState, myName: string, isHost: boolean, onBack: () => void }) {
  const joinedCount = state.joinedHumans?.length || 0;
  const expectedCount = state.expectedHumans || 0;

  return (
    <div className="setup-screen">
      <div className="setup-card lobby-view">
        <h2 className="setup-title">📡 {isHost ? 'Gestisci Lobby' : 'In Attesa di Giocatori'}</h2>

        <div className="lobby-code-display">
          <span className="code-label">CODICE LOBBY:</span>
          <span className="code-value">{state.lobbyCode}</span>
        </div>

        <div className="lobby-progress">
          <div className="progress-text">Giocatori Umani: {joinedCount} / {expectedCount}</div>
          <div className="progress-bar-bg">
            <div className="progress-bar-fill" style={{ width: `${(joinedCount / expectedCount) * 100}%` }}></div>
          </div>
        </div>

        <div className="nomi-list lobby-players">
          {state.joinedHumans?.map((nome, i) => (
            <div key={i} className="nome-row joined-player pulse-in">
              <span className="badge umano">👤</span>
              <span className="joined-name">{nome} {nome === myName ? '(Tu)' : ''} {i === 0 ? '👑' : ''}</span>
              <span className="joined-status">{i === 0 ? 'HOST' : 'PRONTO'}</span>
            </div>
          ))}
          {Array.from({ length: expectedCount - joinedCount }).map((_, i) => (
            <div key={`wait-${i}`} className="nome-row waiting-slot">
              <span className="badge bot">⏳</span>
              <span className="joined-name italic">In attesa...</span>
            </div>
          ))}
        </div>

        <p className="lobby-hint">La partita inizierà automaticamente quando tutti saranno connessi.</p>

        <button className="btn-no" style={{ width: '100%', marginTop: '20px' }} onClick={onBack}>
          Abbandona Lobby
        </button>
      </div>
    </div>
  )
}

// ─── Schermata Setup ─────────────────────────────────────────────────────────

function SetupScreen({ onStart, lobbyMode, onBack }: { onStart: (hostName: string) => void, lobbyMode: 'SINGLE' | 'ONLINE' | 'JOIN', onBack: () => void }) {
  const [numGiocatori, setNumGiocatori] = useState(4)
  const [numUmani, setNumUmani] = useState(lobbyMode === 'SINGLE' ? 1 : 2)
  const [nomi, setNomi] = useState<string[]>(['Giocatore', 'Bot 1', 'Bot 2', 'Bot 3'])
  const [loading, setLoading] = useState(false)
  const [errore, setErrore] = useState('')

  const updateNumGiocatori = (n: number) => {
    setNumGiocatori(n)
    if (lobbyMode === 'ONLINE' && numUmani > n) setNumUmani(n)
    setNomi(prev => {
      const nuovi = [...prev]
      while (nuovi.length < n) nuovi.push('Bot ' + nuovi.length)
      return nuovi.slice(0, n)
    })
  }

  const avviaPartita = async () => {
    setLoading(true)
    setErrore('')
    try {
      const res = await fetch('/api/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          numGiocatori,
          numUmani: lobbyMode === 'SINGLE' ? 1 : numUmani,
          nomiGiocatori: nomi,
          isTorneo: false
        })
      })
      if (!res.ok) {
        const err = await res.json()
        setErrore(err.error || 'Errore avvio partita')
      } else {
        onStart(nomi[0])
      }
    } catch {
      setErrore('Impossibile connettersi al server Java (porta 8080)')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="setup-screen">
      <div className="setup-card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <button className="btn-back" onClick={onBack}>⬅</button>
          <h2 className="setup-title" style={{ margin: 0 }}>⚙️ {lobbyMode === 'SINGLE' ? 'Partita Singola' : 'Configura Online'}</h2>
          <div style={{ width: '40px' }}></div>
        </div>

        <div className="field-group">
          <label>Numero giocatori: <strong>{numGiocatori}</strong></label>
          <input type="range" min={4} max={7} value={numGiocatori}
            onChange={e => updateNumGiocatori(Number(e.target.value))} />
        </div>

        {lobbyMode === 'ONLINE' && (
          <div className="field-group">
            <label>Giocatori umani: <strong>{numUmani}</strong></label>
            <input type="range" min={2} max={numGiocatori} value={numUmani}
              onChange={e => setNumUmani(Number(e.target.value))} />
          </div>
        )}

        <div className="nomi-list">
          {nomi.map((nome, i) => (
            <div key={i} className="nome-row">
              <span className={`badge ${i < (lobbyMode === 'SINGLE' ? 1 : numUmani) ? 'umano' : 'bot'}`}>
                {i < (lobbyMode === 'SINGLE' ? 1 : numUmani) ? '👤' : '🤖'}
              </span>
              <input
                value={nome}
                onChange={e => {
                  const nuovi = [...nomi]; nuovi[i] = e.target.value; setNomi(nuovi)
                }}
                placeholder={i < (lobbyMode === 'SINGLE' ? 1 : numUmani) ? `Nome Giocatore ${i + 1}` : 'Nome bot'}
              />
            </div>
          ))}
        </div>

        {errore && <p className="error-msg">❌ {errore}</p>}

        <button className="btn-primary" onClick={avviaPartita} disabled={loading}>
          {loading ? '⏳ Avvio...' : '🎮 Avvia Partita!'}
        </button>
      </div>
    </div>
  )
}

// ─── Card Component ───────────────────────────────────────────────────────────

function getSuitEmoji(seme?: string): string {
  if (!seme) return '';
  switch (seme.toUpperCase()) {
    case 'CUORI': return '❤️';
    case 'QUADRI': return '♦️';
    case 'FIORI': return '♣️';
    case 'PICCHE': return '♠️';
    default: return '';
  }
}

function getValueLabel(valore?: string): string {
  if (!valore) return '';
  const v = valore.toUpperCase();
  switch (v) {
    case 'ASSO': return 'A';
    case 'DUE': return '2';
    case 'TRE': return '3';
    case 'QUATTRO': return '4';
    case 'CINQUE': return '5';
    case 'SEI': return '6';
    case 'SETTE': return '7';
    case 'OTTO': return '8';
    case 'NOVE': return '9';
    case 'DIECI': return '10';
    case 'JACK': return 'J';
    case 'DONNA': return 'Q';
    case 'RE': return 'K';
    default: return valore;
  }
}

function GameCard({ card, onClick, selected, imageMode = true, showOverlay = false }: { card: CardDTO; onClick?: () => void; selected?: boolean; imageMode?: boolean; showOverlay?: boolean }) {
  const suitEmoji = getSuitEmoji(card.seme);
  const valueLabel = getValueLabel(card.valore);

  return (
    <div className={`game-card ${onClick ? 'clickable' : ''} ${selected ? 'selected' : ''}`}
      onClick={onClick} title={card.descrizione}>
      {imageMode ? (
        <div className="game-card-img-container">
          <CardFace nome={card.nome} />
          {(showOverlay && card.isEstrazione && card.seme && card.valore) && (
            <div className={`card-suit-overlay ${card.seme.toLowerCase()}`}>
              {valueLabel} <span className="suit-emoji">{suitEmoji}</span>
            </div>
          )}
        </div>
      ) : (
        <div className="card-name-text">
          {card.nome}
        </div>
      )}
    </div>
  )
}

// ─── Prompt Panel ─────────────────────────────────────────────────────────────

function PromptPanel({ prompt }: { prompt: PromptDTO }) {
  const [strInput, setStrInput] = useState('')
  const [sending, setSending] = useState(false)

  const invia = async (valore: number | string) => {
    setSending(true)
    try {
      await fetch('/api/action', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ valore })
      })
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="prompt-panel">
      <p className="prompt-msg">🎯 {prompt.messaggio}</p>

      {prompt.tipo === 'YESNO' && (
        <div className="prompt-btns">
          <button className="btn-yes" onClick={() => invia(1)} disabled={sending}>✅ Sì</button>
          <button className="btn-no" onClick={() => invia(2)} disabled={sending}>❌ No</button>
        </div>
      )}

      {(prompt.tipo === 'MENU' || prompt.tipo === 'CARD' || prompt.tipo === 'PERSONA') && (
        <>
          <div className="prompt-btns">
            {prompt.opzioni.map((op, i) => {
              // Nascondiamo l'opzione "Leggi descrizione carta" come richiesto
              if (op.toLowerCase().includes('leggi descrizione carta')) return null;

              const isAnnulla = op.toLowerCase().includes('annulla');
              if (isAnnulla) return null; // Lo renderizziamo a parte fuori dalla griglia

              let extraClass = '';
              if (op.toLowerCase().includes('gioca')) extraClass = 'btn-azione-gioca';
              else if (op.toLowerCase().includes('scarta')) extraClass = 'btn-azione-scarta';
              else if (op.toLowerCase().includes('termina') || op.toLowerCase().includes('passa')) extraClass = 'btn-azione-termina';

              if (prompt.tipo === 'CARD' && hasCardArt(op)) {
                const fakeCard = { nome: op, descrizione: '' } as CardDTO;
                return (
                  <div key={i} style={{ display: 'flex', justifyContent: 'center' }}>
                    <GameCard card={fakeCard} onClick={() => invia(i + 1)} />
                  </div>
                )
              }

              return (
                <button key={i} className={`btn-option ${extraClass}`} onClick={() => invia(i + 1)} disabled={sending}>
                  {prompt.tipo === 'CARD' ? '🃏 ' : prompt.tipo === 'PERSONA' ? '👤 ' : ''}
                  {op}
                </button>
              )
            })}
          </div>
          {/* Renderizza il tasto annulla fuori dalla griglia principale in basso */}
          {prompt.opzioni.some(op => op.toLowerCase().includes('annulla')) && (
            <div className="prompt-footer" style={{ marginTop: '12px' }}>
              {prompt.opzioni.map((op, i) => {
                if (op.toLowerCase().includes('annulla')) {
                  return (
                    <button key={i} className="btn-no" style={{ width: '100%' }} onClick={() => invia(i + 1)} disabled={sending}>
                      {op}
                    </button>
                  )
                }
                return null;
              })}
            </div>
          )}
        </>
      )}

      {prompt.tipo === 'STRING' && (
        <div className="prompt-string">
          <input value={strInput} onChange={e => setStrInput(e.target.value)}
            placeholder="Scrivi qui..." onKeyDown={e => e.key === 'Enter' && invia(strInput)} />
          <button className="btn-primary" onClick={() => invia(strInput)} disabled={sending}>→ Invia</button>
        </div>
      )}
    </div>
  )
}

// ─── Animations ───────────────────────────────────────────────────────────────

function DrawnCardOverlay({ card }: { card: CardDTO }) {
  const label = card.isEstrazione ? 'ESTRAZIONE' : 'PESCATA';
  const name = card.ownerName ? (card.ownerName === 'Giocatore' ? 'per TE' : `per ${card.ownerName}`) : '';

  return (
    <div className="drawn-card-overlay">
      <div className="drawn-card-container">
        <div className="drawn-card-title">{label} {name}</div>
        <GameCard card={card} imageMode={true} showOverlay={true} />
        {/* Rimosso il testo sotto la carta dato che ora è sull'immagine */}
      </div>
    </div>
  )
}

function ThrownCard({ card, actor, target }: { card: CardDTO; actor?: string; target?: string }) {
  const actorName = actor === 'Giocatore' ? 'Tu' : actor;
  const targetText = target ? (target === 'Tutti' ? ' contro TUTTI' : ` contro {P:${target === 'Giocatore' ? 'te' : target}}`) : '';
  const actionText = actorName ? `{P:${actorName}} gioc${actorName === 'Tu' ? 'hi' : 'a'} {C:${card.nome}}${targetText}` : '';

  return (
    <div className="thrown-card-animation">
      {actionText && <div className="thrown-card-desc"><RichText text={actionText} /></div>}
      <GameCard card={card} imageMode={true} showOverlay={true} />
    </div>
  )
}

// ─── Player Card ──────────────────────────────────────────────────────────────

// Costanti per le armi note
const WEAPON_NAMES = ['Volcanic', 'Schofield', 'Remington', 'Rev. Carabine', 'Winchester'];

function getEquippedWeaponName(player: PlayerDTO): string {
  // Cerca se tra le carte in gioco c'è un'arma
  if (player.carteInGioco && player.carteInGioco.length > 0) {
    for (const card of player.carteInGioco) {
      if (WEAPON_NAMES.includes(card.nome)) {
        return card.nome;
      }
    }
  }
  // Se non c'è nessuna arma equipaggiata, si ha sempre la Colt .45 di base
  return 'Colt .45';
}

function getVisualRange(player: PlayerDTO): number {
  let range = player.portataArma;
  if (player.personaggio === 'Rose Doolan' || (player.carteInGioco && player.carteInGioco.some(c => c.nome === 'Mirino'))) {
    range += 1;
  }
  return range;
}

function PlayerCard({ player, isMe, onSelect, canBeTargeted, isDead }: { player: PlayerDTO; isMe: boolean; onSelect?: () => void; canBeTargeted?: boolean; isDead?: boolean }) {
  const specialCardNames = ['Prigione', 'Dinamite'];
  const specialCards = player.carteInGioco.filter(c => specialCardNames.includes(c.nome));
  const regularCards = player.carteInGioco.filter(c => !specialCardNames.includes(c.nome));

  return (
    <div
      className={`player-card ${player.isTurnoCorrente ? 'active-turn' : ''} ${isMe ? 'is-me' : ''} ${canBeTargeted ? 'can-be-targeted' : ''} ${isDead ? 'is-dead' : ''}`}
      onClick={onSelect}
    >
      {/* Badge per Prigione / Dinamite */}
      {specialCards.length > 0 && (
        <div className="player-special-effects">
          {specialCards.map((c, i) => (
            <div key={i} className={`special-effect-badge ${c.nome.toLowerCase()}`} title={c.nome}>
              {c.nome === 'Prigione' ? '⛓️' : '🧨'}
            </div>
          ))}
        </div>
      )}

      <div className="player-info-columns">
        <div className="player-col-left">
          <div className="player-header">
            <span className="player-name">{player.nome}</span>
            <span className="player-tag">{isMe ? '👤' : '🤖'}</span>
          </div>
          <div className="player-role" title={player.descrizioneRuolo}>
            {player.ruolo === 'Sceriffo' ? '⭐ Sceriffo' : player.ruolo === 'Sconosciuto' ? '❓ Identità Segreta' : isDead ? `💀 ${player.ruolo}` : `🤫 ${player.ruolo}`}
          </div>
          <div className="player-char" title={player.descrizionePersonaggio}>{player.personaggio}</div>
        </div>

        <div className="player-col-right">
          <div className="vita-bar">
            {Array.from({ length: player.vitaMax }).map((_, i) => (
              <Heart key={i} filled={i < player.vita} />
            ))}
          </div>
          {!isDead && (
            <>
              <div className="player-info">
                {(player.personaggio === 'Rose Doolan' || (player.carteInGioco && player.carteInGioco.some(c => c.nome === 'Mirino'))) && '🎯 '}
                🔫 {getEquippedWeaponName(player)} (Tiro utile: {getVisualRange(player)})
              </div>
              <div className="player-info">
                🃏 {player.carteInMano.length} in mano
              </div>
            </>
          )}
        </div>
      </div>
      {(!isDead && regularCards.length > 0) && (
        <div className="cards-in-play" title="Carte equipaggiate/in gioco">
          {regularCards.map((c, i) => (
            <div key={i} className="equipped-card-wrapper">
              <span className="equipped-card-label">{c.nome}</span>
              <div className="card-preview-hover">
                <GameCard card={c} showOverlay={true} />
              </div>
            </div>
          ))}
        </div>
      )}
      {isMe && player.carteInMano.length > 0 && (
        <div className="my-hand" title="La tua mano">
          {player.carteInMano.map((c, i) => <GameCard key={i} card={c} imageMode={false} showOverlay={true} />)}
        </div>
      )}
    </div>
  )
}
function Graveyard({ players }: { players: PlayerDTO[] }) {
  const [currentIndex, setCurrentIndex] = useState(0);

  // Se un giocatore viene rimosso (es. nuova partita), resetta l'indice
  useEffect(() => {
    if (currentIndex >= players.length) {
      setCurrentIndex(Math.max(0, players.length - 1));
    }
  }, [players.length, currentIndex]);

  if (!players || players.length === 0) return null;

  const next = () => setCurrentIndex((prev) => (prev + 1) % players.length);
  const prev = () => setCurrentIndex((prev) => (prev - 1 + players.length) % players.length);

  return (
    <div className="graveyard">
      <div className="graveyard-title">
        <span>🪦 Cimitero ({players.length})</span>
        {players.length > 1 && (
          <span className="graveyard-counter">{currentIndex + 1} / {players.length}</span>
        )}
      </div>

      <div className="graveyard-stack-container">
        {players.length > 1 && (
          <button className="nav-btn prev" onClick={prev} title="Precedente">◀</button>
        )}

        <div className="stack-wrapper">
          {/* Mostriamo solo il giocatore corrente, ma con un effetto di "pila" via CSS */}
          <PlayerCard
            player={players[currentIndex]}
            isMe={false}
            isDead={true}
          />
        </div>

        {players.length > 1 && (
          <button className="nav-btn next" onClick={next} title="Successivo">▶</button>
        )}
      </div>
    </div>
  )
}

// ─── Tavolo ────────────────────────────────────────────────────────────────────

function TavoloScreen({ state, events, activeAction, onBackToMenu, myPlayerName }: { state: GameState, events: string[], activeAction?: string | null, onBackToMenu: () => void, myPlayerName: string }) {
  const giocatori = state.giocatori ?? []
  const meIdx = giocatori.findIndex(g => g.nome.trim().toLowerCase() === myPlayerName.trim().toLowerCase())
  const eventi = events // Usa gli eventi passati dai props (animati)

  // Supporto animazioni locali
  const [shownDrawnCard, setShownDrawnCard] = useState<CardDTO | null>(null);
  const [shownPlayedCard, setShownPlayedCard] = useState<{ card: CardDTO, actor?: string, target?: string } | null>(null);
  const [lastDrawnTs, setLastDrawnTs] = useState<number>(0);
  const [lastPlayedTs, setLastPlayedTs] = useState<number>(0);

  const drawnCardTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const playedCardTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (drawnCardTimer.current) clearTimeout(drawnCardTimer.current);
      if (playedCardTimer.current) clearTimeout(playedCardTimer.current);
    };
  }, []);

  useEffect(() => {
    if (state.lastDrawnCard && state.lastDrawnCard.timestamp > lastDrawnTs) {
      setShownDrawnCard(state.lastDrawnCard);
      setLastDrawnTs(state.lastDrawnCard.timestamp);
      setShownPlayedCard(null); // Mutua esclusione lato client

      if (drawnCardTimer.current) clearTimeout(drawnCardTimer.current);
      drawnCardTimer.current = setTimeout(() => setShownDrawnCard(null), 1200);
    } else if (!state.lastDrawnCard) {
      setShownDrawnCard(null);
    }
  }, [state.lastDrawnCard, lastDrawnTs]);

  useEffect(() => {
    if (state.lastPlayedCard && state.lastPlayedCard.timestamp > lastPlayedTs) {
      setShownPlayedCard({
        card: state.lastPlayedCard,
        actor: state.lastPlayedActor,
        target: state.lastPlayedTarget
      });
      setLastPlayedTs(state.lastPlayedCard.timestamp);
      setShownDrawnCard(null); // Mutua esclusione lato client

      if (playedCardTimer.current) clearTimeout(playedCardTimer.current);
      playedCardTimer.current = setTimeout(() => setShownPlayedCard(null), 1200);
    } else if (!state.lastPlayedCard) {
      setShownPlayedCard(null);
    }
  }, [state.lastPlayedCard, lastPlayedTs, state.lastPlayedActor, state.lastPlayedTarget]);

  if (state.status.startsWith('FINITA')) {
    // const { giocatori } = state; // Rimosso perché già definito a riga 560

    let resultTitle = "FINE PARTITA";
    let subtitle = "La partita è terminata.";
    let themeClass = "theme-default";
    let icon = "🏁";

    if (state.status.includes('LEGGE')) {
      resultTitle = "LA LEGGE TRIONFA!";
      subtitle = "Lo Sceriffo e i suoi Vice hanno ripulito la città dal crimine.";
      themeClass = "theme-law";
      icon = "⭐";
    } else if (state.status.includes('FUORILEGGE')) {
      resultTitle = "I FUORILEGGE HANNO VINTO!";
      subtitle = "Lo Sceriffo è caduto. Il caos e il crimine ora dominano il West.";
      themeClass = "theme-outlaws";
      icon = "💀";
    } else if (state.status.includes('RINNEGATO')) {
      resultTitle = "IL RINNEGATO VINCE!";
      subtitle = "Tutti sono morsi nel polverone. Il Rinnegato è l'ultimo sopravvissuto.";
      themeClass = "theme-renegade";
      icon = "🦅";
    }

    const allPlayers = [...giocatori, ...(state.giocatoriEliminati || [])];
    const winners = allPlayers.filter(p => {
      if (state.status.includes('LEGGE')) return p.ruolo === 'Sceriffo' || p.ruolo === 'Vice';
      if (state.status.includes('FUORILEGGE')) return p.ruolo === 'Fuorilegge';
      if (state.status.includes('RINNEGATO')) return p.ruolo === 'Rinnegato';
      return false;
    });

    return (
      <div className={`end-screen-overlay ${themeClass}`}>
        <div className="victory-card">
          <div className="victory-icon">{icon}</div>
          <h2 className="victory-title">{resultTitle}</h2>
          <p className="victory-subtitle">{subtitle}</p>

          <div className="winners-section">
            <h3 className="section-title">✨ I VINCITORI ✨</h3>
            <div className="winners-list">
              {winners.map(p => (
                <div key={p.nome} className="winner-item">
                  <span className="winner-name">{p.nome}</span>
                  <span className="winner-role">({p.ruolo})</span>
                </div>
              ))}
            </div>
          </div>

          <div className="final-summary">
            <table className="summary-table">
              <thead>
                <tr>
                  <th>Giocatore</th>
                  <th>Ruolo</th>
                  <th>Stato</th>
                </tr>
              </thead>
              <tbody>
                {allPlayers.map(p => (
                  <tr key={p.nome} className={giocatori.some(v => v.nome === p.nome) ? 'status-alive' : 'status-dead'}>
                    <td>{p.nome}</td>
                    <td>{p.ruolo}</td>
                    <td>{giocatori.some(v => v.nome === p.nome) ? 'Vivo' : 'Eliminato'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="victory-divider"></div>
          <button className="btn-primary start-btn" onClick={onBackToMenu}>
            🔄 Nuova Avventura
          </button>
        </div>
      </div>
    )
  }

  const isMyPrompt = state.prompt && (state.prompt.targetPlayer?.trim().toLowerCase() === myPlayerName.trim().toLowerCase() || !state.prompt.targetPlayer);

  const canDrawFromDeck = isMyPrompt && state.prompt && (state.prompt.messaggio.includes('Pesca') || state.prompt.messaggio.includes('Estrai'));

  const onDeckClick = async () => {
    if (!canDrawFromDeck) return;
    // Cerca l'opzione "Pesca" o "Estrai" nel prompt e invia l'indice corrispondente
    const optIdx = state.prompt?.opzioni.findIndex(o => o.includes('Pesca') || o.includes('Estrai'));
    if (optIdx !== undefined && optIdx !== -1) {
      try {
        await fetch('/api/action', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ valore: optIdx + 1 }),
        });
      } catch (err) {
        console.error("Errore deck click:", err);
      }
    }
  };

  // Se è un prompt di pesca, nascondiamo i bottoni dal PromptPanel filtrato
  const filteredPrompt = state.prompt ? {
    ...state.prompt,
    opzioni: state.prompt.messaggio.includes('Pesca') || state.prompt.messaggio.includes('Estrai')
      ? state.prompt.opzioni.filter(o => !o.includes('Pesca') && !o.includes('Estrai'))
      : state.prompt.opzioni
  } : null;

  return (
    <div className="tavolo-screen">
      <Graveyard players={state.giocatoriEliminati ?? []} />
      {shownDrawnCard && <DrawnCardOverlay card={shownDrawnCard} />}
      {shownPlayedCard && <ThrownCard card={shownPlayedCard.card} actor={shownPlayedCard.actor} target={shownPlayedCard.target} />}

      <div className="tavolo-body">
        <div className="tavolo-main">
          {activeAction && (
            <div className="action-overlay">
              <RichText text={activeAction} />
            </div>
          )}

          <div className="table-area">
            <div className="players-circle">
              <div className="center-table">
                <div className={`center-deck ${canDrawFromDeck ? 'can-draw' : ''}`} onClick={onDeckClick}>
                  <CardBack />
                  <div className="deck-count">{state.carteRimanenti} carte</div>
                </div>
                {state.ultimaCartaScartata && (
                  <div className="center-discard">
                    <div className="discard-label">Scarti</div>
                    <GameCard card={state.ultimaCartaScartata} showOverlay={true} />
                  </div>
                )}
              </div>

              {/* Giocatori posizionati manualmente */}
              {giocatori.map((p, i) => {
                const total = giocatori.length;

                // Se c'è un giocatore umano, usiamo il suo indice come 0 relativo
                let relativeIndex = i;
                if (meIdx !== -1) {
                  relativeIndex = (i - meIdx + total) % total;
                }

                // Logica per rendere la scheda cliccabile se è un bersaglio valido
                let onSelectPlayer = undefined;
                let canBeTargeted = false;

                const currentPrompt = state.prompt;
                if (currentPrompt && currentPrompt.tipo === 'PERSONA' && isMyPrompt) {
                  // Cerca se il nome di questo giocatore è tra le opzioni del prompt
                  // Le opzioni sono stringhe tipo "1. Nome (distanza: 1)" o solo nomi
                  const optIdx = currentPrompt.opzioni.findIndex(opt =>
                    opt.includes(p.nome) || opt === p.nome
                  );

                  if (optIdx !== -1) {
                    canBeTargeted = true;
                    onSelectPlayer = async () => {
                      // Invia l'indice 1-based al server
                      await fetch('/api/action', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ valore: optIdx + 1 })
                      });
                    };
                  }
                }

                // Posizionamenti hardcoded per ogni configurazione (da 2 a 7 giocatori)
                // L'indice 0 è sempre il giocatore in basso al centro.
                let pos = { left: '50%', top: '75%' }; // Default a fondo pagina se qualcosa fallisce

                if (total === 2) {
                  const positions = [
                    { left: '50%', top: '75%' }, // 0: Bottom
                    { left: '50%', top: '15%' }  // 1: Top
                  ];
                  pos = positions[relativeIndex] || pos;
                } else if (total === 3) {
                  const positions = [
                    { left: '50%', top: '75%' }, // 0: Bottom
                    { left: '25%', top: '22%' }, // 1: Top Left (Clockwise 1)
                    { left: '75%', top: '22%' }  // 2: Top Right (Clockwise 2)
                  ];
                  pos = positions[relativeIndex] || pos;
                } else if (total === 4) {
                  const positions = [
                    { left: '50%', top: '80%' }, // 0: Bottom
                    { left: '18%', top: '45%' }, // 1: Left (Clockwise 1)
                    { left: '50%', top: '15%' }, // 2: Top (Clockwise 2)
                    { left: '82%', top: '45%' }  // 3: Right (Clockwise 3)
                  ];
                  pos = positions[relativeIndex] || pos;
                } else if (total === 5) {
                  const positions = [
                    { left: '50%', top: '80%' }, // 0: Bottom
                    { left: '18%', top: '60%' }, // 1: Bottom Left (Clockwise 1)
                    { left: '30%', top: '17%' }, // 2: Top Left (Clockwise 2)
                    { left: '70%', top: '17%' }, // 3: Top Right (Clockwise 3)
                    { left: '82%', top: '60%' }  // 4: Bottom Right (Clockwise 4)
                  ];
                  pos = positions[relativeIndex] || pos;
                } else if (total === 6) {
                  const positions = [
                    { left: '50%', top: '75%' }, // 0: Bottom
                    { left: '18%', top: '55%' }, // 1: Bottom Left (Clockwise 1)
                    { left: '18%', top: '20%' }, // 2: Top Left (Clockwise 2)
                    { left: '50%', top: '18%' }, // 3: Top Center (Clockwise 3)
                    { left: '82%', top: '20%' }, // 4: Top Right (Clockwise 4)
                    { left: '82%', top: '55%' }  // 5: Bottom Right (Clockwise 5)
                  ];
                  pos = positions[relativeIndex] || pos;
                } else if (total === 7) {
                  const positions = [
                    { left: '50%', top: '78%' }, // 0: Bottom
                    { left: '20%', top: '70%' }, // 1: Bottom Left (Clockwise 1)
                    { left: '15%', top: '42%' }, // 2: Mid Left (Clockwise 2)
                    { left: '32%', top: '15%' }, // 3: Top Left (Clockwise 3)
                    { left: '68%', top: '15%' }, // 4: Top Right (Clockwise 4)
                    { left: '85%', top: '42%' }, // 5: Mid Right (Clockwise 5)
                    { left: '80%', top: '70%' }  // 6: Bottom Right (Clockwise 6)
                  ];
                  pos = positions[relativeIndex] || pos;
                }

                return (
                  <div key={p.nome} className="player-circle-wrapper" style={pos}>
                    <PlayerCard
                      player={p}
                      isMe={p.nome.trim().toLowerCase() === myPlayerName.trim().toLowerCase()}
                      onSelect={onSelectPlayer}
                      canBeTargeted={canBeTargeted}
                    />
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        <div className="sidebar-right">
          {filteredPrompt && isMyPrompt && (
            <div className="sidebar-prompt-container">
              <PromptPanel prompt={filteredPrompt} />
            </div>
          )}
          <GameLog eventi={eventi} />
        </div>
      </div>
    </div>
  )
}

// ─── Header Roles ─────────────────────────────────────────────────────────────

function RoleBadge({ count, type, icon }: { count: number; type: string; icon: string }) {
  if (count === 0) return null;
  return (
    <div className={`role-header-badge role-${type.toLowerCase()}`} title={`${count}x ${type}`}>
      <span className="badge-icon">{icon}</span>
      <span className="badge-count">x{count}</span>
    </div>
  );
}

function GameRoles({ count }: { count: number }) {
  if (count < 4) return null;
  const sheriffs = 1;
  const outlaws = count >= 6 ? 3 : 2;
  const renegades = 1;
  const deputies = count === 7 ? 2 : count >= 5 ? 1 : 0;

  return (
    <div className="game-roles-header">
      <RoleBadge count={sheriffs} type="sceriffo" icon="⭐" />
      <RoleBadge count={deputies} type="vice" icon="🎖️" />
      <RoleBadge count={outlaws} type="fuorilegge" icon="🐺" />
      <RoleBadge count={renegades} type="rinnegato" icon="🦅" />
    </div>
  );
}

// ─── Schermata Finale ──────────────────────────────────────────────────────────

function GameEndScreen({ state, onRestart }: { state: GameState, onRestart: () => void }) {
  const resultText = state.status.replace('FINITA: ', '');
  let winnerTitle = "Fine Partita!";
  let winnerMascot = "🏆";
  let desc = "";
  let themeClass = "theme-default";

  if (resultText === 'LEGGE') {
    winnerTitle = "La Legge Trionfa!";
    winnerMascot = "👮‍♂️";
    desc = "Lo Sceriffo e i suoi Vice hanno ripulito la città da ogni minaccia.";
    themeClass = "theme-law";
  } else if (resultText === 'FUORILEGGE') {
    winnerTitle = "I Fuorilegge Dominano!";
    winnerMascot = "🐺";
    desc = "Lo Sceriffo è caduto. I Fuorilegge prendono il controllo della città.";
    themeClass = "theme-outlaws";
  } else if (resultText === 'RINNEGATO') {
    winnerTitle = "Il Rinnegato ha Vinto!";
    winnerMascot = "🦅";
    desc = "Tradimenti e inganni hanno pagato. Il Rinnegato è l'ultimo rimasto in vita.";
    themeClass = "theme-renegade";
  }

  return (
    <div className={`end-screen-overlay ${themeClass}`}>
      <div className="victory-card">
        <div className="victory-icon">{winnerMascot}</div>
        <h2 className="victory-title">{winnerTitle}</h2>
        <p className="victory-subtitle">{desc}</p>

        <div className="winners-section">
          <h3 className="section-title">✨ Sopravvissuti</h3>
          <div className="winners-list">
            {state.giocatori?.map(g => (
              <div key={g.nome} className="winner-item">
                <span className="winner-name">{g.nome}</span>
                <span className="winner-role">({g.ruolo})</span>
                <span className="winner-char">- {g.personaggio}</span>
              </div>
            ))}
            {(!state.giocatori || state.giocatori.length === 0) && <div className="winner-item">Nessuno...</div>}
          </div>
        </div>

        <div className="final-summary">
          <h3 className="section-title">💀 Caduti in Battaglia</h3>
          <div className="dead-list">
            {state.giocatoriEliminati?.map(g => (
              <div key={g.nome} className="dead-item">
                <span className="dead-name">{g.nome}</span>
                <span className="dead-role">({g.ruolo})</span>
                <span className="dead-char">- {g.personaggio}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="victory-divider"></div>
        <button className="btn-primary start-btn pulse" onClick={onRestart}>
          🔄 Torna al Menu Principale
        </button>
      </div>
    </div>
  );
}


// ─── Abbandono partita ────────────────────────────────────────────────────────

function LeaveConfirm({ online, onConfirm, onCancel }: { online: boolean, onConfirm: () => void, onCancel: () => void }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onCancel() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onCancel])

  return (
    <div className="leave-backdrop" onClick={onCancel}>
      <div className="leave-dialog" role="dialog" aria-modal="true" aria-labelledby="leave-title"
        onClick={e => e.stopPropagation()}>
        <h2 id="leave-title">Abbandonare la partita?</h2>
        <p>
          {online
            ? 'La partita terminerà per tutti i giocatori collegati.'
            : 'La partita verrà chiusa e tornerai al menu.'}
        </p>
        <div className="leave-actions">
          <button className="btn-option" onClick={onCancel} autoFocus>Continua a giocare</button>
          <button className="btn-no" onClick={onConfirm}>Abbandona</button>
        </div>
      </div>
    </div>
  )
}

function GameAbortedScreen({ message, onBack }: { message?: string, onBack: () => void }) {
  return (
    <div className="end-screen-overlay">
      <div className="victory-card">
        <div className="victory-icon">🏳️</div>
        <h2 className="victory-title">Partita interrotta</h2>
        <p className="victory-subtitle">{message || 'Un giocatore ha abbandonato la partita.'}</p>
        <button className="btn-primary start-btn" onClick={onBack}>
          Torna al menu
        </button>
      </div>
    </div>
  )
}

// ─── App Principale ────────────────────────────────────────────────────────────

function App() {
  const [gameState, setGameState] = useState<GameState | null>(null)
  const [started, setStarted] = useState(false)
  const [lobbyMode, setLobbyMode] = useState<'SINGLE' | 'ONLINE' | 'JOIN' | null>(null)
  const [myPlayerName, setMyPlayerName] = useState<string>('')
  const [isLobbyCreator, setIsLobbyCreator] = useState(false)
  const [eventQueue, setEventQueue] = useState<string[]>([])
  const [visibleEvents, setVisibleEvents] = useState<string[]>([])
  const [activeAction, setActiveAction] = useState<string | null>(null)
  const [confirmLeave, setConfirmLeave] = useState(false)
  const handledEventsCount = useRef(0)

  const fetchState = useCallback(async () => {
    try {
      const url = myPlayerName ? `/api/state?player=${encodeURIComponent(myPlayerName)}` : '/api/state'
      const res = await fetch(url)
      const data: GameState = await res.json()

      setGameState(prev => {
        if (!prev) {
          if (data.eventi) setVisibleEvents(data.eventi.filter(e => !e.toUpperCase().includes("[SKIP]") && !e.toUpperCase().includes("[LOG]")));
        }
        return data;
      })
      if (data.status && data.status !== 'IDLE' && data.status !== 'WAITING_FOR_PLAYERS' && data.status !== 'ANNULLATA') {
        // Auto-start solo se il gioco è già in corso e siamo un giocatore noto o Single Player
        if (lobbyMode === 'SINGLE' || myPlayerName) {
          setStarted(true)
        }
      }

      // Se la partita riparte da zero, resettiamo le code
      if (!data.eventi || data.eventi.length === 0) {
        handledEventsCount.current = 0;
        setEventQueue([]);
        setVisibleEvents([]);
      } else if (data.eventi.length > handledEventsCount.current) {
        // Estrai solo i nuovi eventi
        const newEvents = data.eventi.slice(handledEventsCount.current)
        handledEventsCount.current = data.eventi.length

        // Aggiungili alla coda da smaltire
        setEventQueue(prev => [...prev, ...newEvents])
      }
    } catch {
      // server non ancora raggiungibile, silenzioso
    }
  }, [myPlayerName, lobbyMode])

  useEffect(() => {
    fetchState()
    const id = setInterval(fetchState, 400) // Polling raddoppiato in frequenza per non perdere animazioni
    return () => clearInterval(id)
  }, [fetchState])

  // Processo la coda eventi uno alla volta
  useEffect(() => {
    if (eventQueue.length === 0) return;

    const nextEvent = eventQueue[0];

    // Se l'evento è marchiato come SKIP, nascondilo
    if (nextEvent.toUpperCase().includes("[SKIP]")) {
      setEventQueue(prev => prev.slice(1));
      return;
    }

    const isSilenced = nextEvent.toUpperCase().includes("[LOG]");
    // Se la coda è lunga, andiamo molto veloci
    const fastForward = eventQueue.length > 2;

    const eventText = nextEvent.replace(/\[LOG\]\s*/gi, '').trim();

    const processDelay = isSilenced ? 50 : (fastForward ? 400 : 1200);
    const visibleTime = fastForward ? 300 : 1000;

    const timer = setTimeout(() => {
      setVisibleEvents(prev => [...prev, eventText]);
      setEventQueue(prev => prev.slice(1));

      if (!isSilenced) {
        setActiveAction(eventText);
        setTimeout(() => setActiveAction(null), visibleTime);
      }
    }, processDelay);

    return () => clearTimeout(timer);
  }, [eventQueue]);


  const visibleGameState = gameState ? {
    ...gameState,
    eventi: visibleEvents,
    prompt: eventQueue.length > 0 ? undefined : gameState.prompt
  } : null;

  const handleJoinLobby = async (code: string, name: string) => {
    const res = await fetch('/api/join', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, name })
    });
    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.error || 'Errore nel join');
    }
    setMyPlayerName(name);
    setLobbyMode('JOIN');
    setStarted(true);
    setIsLobbyCreator(false);
  };

  // Torna al menu solo su questo browser, senza toccare il server
  const resetLocal = () => {
    setStarted(false);
    setLobbyMode(null);
    setMyPlayerName('');
    setIsLobbyCreator(false);
    setGameState(null);
    setVisibleEvents([]);
    setEventQueue([]);
    setActiveAction(null);
    setConfirmLeave(false);
    handledEventsCount.current = 0;
  };

  const handleBackToMenu = async () => {
    // Resettiamo subito lo stato locale per evitare flicker grafici
    resetLocal();

    try {
      await fetch('/api/reset', { method: 'POST' });
    } catch (e) {
      console.error("Errore durante il reset:", e);
    }
  };

  // Abbandona la lobby o la partita in corso
  const handleLeave = async () => {
    const name = myPlayerName;
    const isOnline = lobbyMode !== 'SINGLE';
    const eliminated = !!gameState?.giocatori && !gameState.giocatori.some(
      p => p.nome.trim().toLowerCase() === name.trim().toLowerCase());
    const inGame = gameState?.status === 'IN_CORSO';
    resetLocal();

    // In online, chi è già eliminato esce senza fermare la partita degli altri
    if (isOnline && inGame && eliminated) return;

    try {
      await fetch('/api/leave', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ player: name })
      });
    } catch (e) {
      console.error("Errore durante l'abbandono:", e);
    }
  };

  const isPlaying = started && gameState?.status === 'IN_CORSO';

  return (
    <div className="app">
      <header className="app-header">
        <h1 className="app-title">BANG!! </h1>
        {started && gameState && (
          <div className="header-controls">
            <GameRoles count={(gameState.giocatori?.length || 0) + (gameState.giocatoriEliminati?.length || 0)} />
          </div>
        )}
        <div className="server-dot" title="Server Java connesso" />
        {isPlaying && (
          <button className="btn-leave" onClick={() => setConfirmLeave(true)}>
            🏳️ Abbandona
          </button>
        )}
      </header>

      {confirmLeave && isPlaying && (
        <LeaveConfirm
          online={lobbyMode !== 'SINGLE'}
          onConfirm={handleLeave}
          onCancel={() => setConfirmLeave(false)}
        />
      )}

      <main className="app-main">
        {!started
          ? (lobbyMode === null
            ? <PreLobby onSelect={setLobbyMode} />
            : lobbyMode === 'JOIN'
              ? <JoinScreen onBack={() => setLobbyMode(null)} onJoin={handleJoinLobby} />
              : <SetupScreen
                lobbyMode={lobbyMode}
                onStart={(hostName) => {
                  setMyPlayerName(hostName);
                  setStarted(true);
                  setIsLobbyCreator(true);
                }}
                onBack={() => setLobbyMode(null)}
              />
          )
          : gameState && gameState.status === 'WAITING_FOR_PLAYERS'
            ? <LobbyScreen state={gameState} myName={myPlayerName} isHost={isLobbyCreator} onBack={handleLeave} />
            : gameState && gameState.status === 'ANNULLATA'
              ? <GameAbortedScreen message={gameState.abortMessage} onBack={resetLocal} />
            : visibleGameState && visibleGameState.status.startsWith('FINITA')
              ? <GameEndScreen state={visibleGameState} onRestart={handleBackToMenu} />
              : visibleGameState && visibleGameState.status !== 'IDLE'
                ? <TavoloScreen
                  state={visibleGameState}
                  events={visibleEvents}
                  activeAction={activeAction}
                  onBackToMenu={handleBackToMenu}
                  myPlayerName={myPlayerName}
                />
                : <div className="spinner-wrap"><div className="spinner" /></div>
        }
      </main>
    </div>
  )
}

export default App
