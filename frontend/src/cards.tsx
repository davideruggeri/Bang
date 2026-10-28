// Carte del gioco base disegnate in SVG/CSS (grafica originale del progetto).
import type { CSSProperties, ReactElement } from 'react'

type Tipo = 'azione' | 'equip' | 'arma'

interface CardArt {
  tipo: Tipo
  icon: ReactElement
  gittata?: number
}

// Ogni icona usa un viewBox 0 0 100 100 e currentColor per il tratto.
const S = { fill: 'none', stroke: 'currentColor', strokeWidth: 5, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const }

const revolver = (
  <g {...S}>
    <path d="M14 38 H72 L78 32 H86 V46 H60 L56 52 H46 L40 46 H30 L24 74 H10 L16 46 Z" />
    <circle cx="50" cy="44" r="5" />
    <path d="M44 52 Q48 62 56 56" />
  </g>
)

const ART: Record<string, CardArt> = {
  'BANG!': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <path d="M50 8 L58 34 L86 22 L68 46 L94 58 L64 62 L70 92 L50 70 L30 92 L36 62 L6 58 L32 46 L14 22 L42 34 Z" />
        <circle cx="50" cy="52" r="7" fill="currentColor" />
      </g>
    ),
  },
  'Mancato!': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <circle cx="42" cy="52" r="30" />
        <circle cx="42" cy="52" r="16" />
        <path d="M60 14 L94 6" strokeDasharray="6 7" />
        <path d="M78 20 L92 34 M92 20 L78 34" />
      </g>
    ),
  },
  'Birra': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <path d="M24 30 H66 V86 Q66 92 60 92 H30 Q24 92 24 86 Z" />
        <path d="M66 42 H76 Q84 42 84 52 V62 Q84 72 76 72 H66" />
        <path d="M22 30 Q20 16 34 18 Q40 8 52 14 Q66 10 68 24 Q72 30 66 30" />
        <path d="M36 44 V80 M54 44 V80" />
      </g>
    ),
  },
  'Saloon': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <path d="M10 22 H90 M14 22 V90 M86 22 V90" />
        <path d="M20 40 Q34 34 48 40 V76 Q34 82 20 76 Z" />
        <path d="M80 40 Q66 34 52 40 V76 Q66 82 80 76 Z" />
        <path d="M26 50 H42 M26 62 H42 M58 50 H74 M58 62 H74" />
      </g>
    ),
  },
  'Panico!': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <path d="M30 92 V56 Q30 48 24 42 L16 34 Q12 28 18 26 Q24 24 30 32 L36 40 V14 Q36 8 42 8 Q48 8 48 14 V40 V10 Q48 4 54 4 Q60 4 60 10 V40 V16 Q60 10 66 10 Q72 10 72 16 V42 V26 Q72 20 78 20 Q84 20 84 26 V60 Q84 80 70 92" />
      </g>
    ),
  },
  'Cat Balou': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <rect x="16" y="22" width="40" height="58" rx="5" transform="rotate(-12 36 51)" />
        <path d="M50 54 H90 M78 42 L90 54 L78 66" />
        <path d="M24 46 L44 42 M26 58 L46 54" />
      </g>
    ),
  },
  'Diligenza': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <path d="M14 64 V36 Q14 26 24 26 H70 Q80 26 82 36 L86 64 Z" />
        <path d="M30 26 V18 H66 V26" />
        <rect x="26" y="34" width="18" height="16" rx="2" />
        <rect x="52" y="34" width="18" height="16" rx="2" />
        <circle cx="28" cy="76" r="12" />
        <circle cx="72" cy="76" r="12" />
        <path d="M28 64 V88 M16 76 H40 M72 64 V88 M60 76 H84" strokeWidth={3} />
      </g>
    ),
  },
  'Wells Fargo': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <path d="M14 48 H86 V86 H14 Z" />
        <path d="M14 48 Q14 26 50 26 Q86 26 86 48" />
        <rect x="42" y="56" width="16" height="14" rx="2" />
        <path d="M24 26 V14 M50 22 V8 M76 26 V14" strokeDasharray="4 5" />
      </g>
    ),
  },
  'Gatling': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <circle cx="30" cy="62" r="14" />
        <path d="M40 52 L86 30 M44 58 L90 42 M44 68 L90 56" />
        <path d="M30 76 L18 94 M30 76 L42 94" />
      </g>
    ),
  },
  'Indiani!': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <path d="M50 94 V30" />
        <path d="M50 30 Q28 40 30 70 Q40 60 50 62 Q60 60 70 70 Q72 40 50 30 Z" />
        <path d="M50 30 Q48 12 50 6 Q52 12 50 30" />
        <path d="M38 50 L50 44 L62 50 M36 60 L50 54 L64 60" strokeWidth={3} />
      </g>
    ),
  },
  'Duello': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <path d="M14 20 L74 80 M86 20 L26 80" />
        <path d="M66 86 L82 70 M18 70 L34 86" />
        <circle cx="50" cy="50" r="6" fill="currentColor" />
      </g>
    ),
  },
  'Emporio': {
    tipo: 'azione',
    icon: (
      <g {...S}>
        <path d="M10 36 L18 16 H82 L90 36 Z" />
        <path d="M10 36 Q20 46 30 36 Q40 46 50 36 Q60 46 70 36 Q80 46 90 36" />
        <path d="M18 44 V90 H82 V44" />
        <rect x="40" y="60" width="20" height="30" />
      </g>
    ),
  },
  'Dinamite': {
    tipo: 'equip',
    icon: (
      <g {...S}>
        <rect x="22" y="40" width="16" height="52" rx="3" />
        <rect x="42" y="40" width="16" height="52" rx="3" />
        <rect x="62" y="40" width="16" height="52" rx="3" />
        <path d="M18 60 H82" />
        <path d="M50 40 Q50 24 62 20 Q74 16 76 6" />
        <path d="M72 4 L80 8 M80 2 L76 12" strokeWidth={3} />
      </g>
    ),
  },
  'Prigione': {
    tipo: 'equip',
    icon: (
      <g {...S}>
        <rect x="14" y="14" width="72" height="76" rx="4" />
        <path d="M32 14 V90 M50 14 V90 M68 14 V90 M14 52 H86" />
      </g>
    ),
  },
  'Barile': {
    tipo: 'equip',
    icon: (
      <g {...S}>
        <path d="M28 10 H72 Q86 50 72 90 H28 Q14 50 28 10 Z" />
        <path d="M22 30 H78 M22 70 H78" />
        <path d="M40 10 Q34 50 40 90 M60 10 Q66 50 60 90" strokeWidth={3} />
      </g>
    ),
  },
  'Mirino': {
    tipo: 'equip',
    icon: (
      <g {...S}>
        <circle cx="50" cy="50" r="32" />
        <circle cx="50" cy="50" r="4" fill="currentColor" />
        <path d="M50 6 V30 M50 70 V94 M6 50 H30 M70 50 H94" />
      </g>
    ),
  },
  'Mustang': {
    tipo: 'equip',
    icon: (
      <g {...S}>
        <path d="M26 88 V48 Q26 14 50 14 Q74 14 74 48 V88" strokeWidth={9} />
        <path d="M18 88 H34 M66 88 H82" />
        <circle cx="26" cy="50" r="2" fill="currentColor" />
        <circle cx="74" cy="50" r="2" fill="currentColor" />
        <circle cx="34" cy="26" r="2" fill="currentColor" />
        <circle cx="66" cy="26" r="2" fill="currentColor" />
      </g>
    ),
  },
  'Volcanic': { tipo: 'arma', icon: revolver, gittata: 1 },
  'Schofield': { tipo: 'arma', icon: revolver, gittata: 2 },
  'Remington': { tipo: 'arma', icon: revolver, gittata: 3 },
  'Rev. Carabine': { tipo: 'arma', icon: revolver, gittata: 4 },
  'Winchester': { tipo: 'arma', icon: revolver, gittata: 5 },
}

const TIPO_LABEL: Record<Tipo, string> = {
  azione: 'Azione',
  equip: 'Equipaggiamento',
  arma: 'Arma',
}

export function CardFace({ nome }: { nome: string }) {
  const art = ART[nome]
  const tipo: Tipo = art?.tipo ?? 'azione'
  return (
    <div className={`game-card-img cardface cardface--${tipo}`} role="img" aria-label={nome}>
      <div className="cardface-name" style={{ '--len': nome.length } as CSSProperties}>{nome}</div>
      <div className="cardface-art">
        {art ? (
          <svg viewBox="0 0 100 100" aria-hidden="true">{art.icon}</svg>
        ) : (
          <span className="cardface-fallback">{nome.charAt(0)}</span>
        )}
      </div>
      <div className="cardface-foot">
        {art?.gittata ? `Gittata ${art.gittata}` : TIPO_LABEL[tipo]}
      </div>
    </div>
  )
}

export function CardBack() {
  return (
    <div className="game-card-img cardface cardback" role="img" aria-label="Mazzo">
      <svg viewBox="0 0 100 100" aria-hidden="true">
        <g fill="none" stroke="currentColor" strokeWidth={4} strokeLinejoin="round">
          <path d="M50 10 L60 38 L90 38 L66 56 L75 86 L50 68 L25 86 L34 56 L10 38 L40 38 Z" />
          <circle cx="50" cy="50" r="44" strokeDasharray="3 6" />
        </g>
      </svg>
      <div className="cardback-title">WEST</div>
    </div>
  )
}
