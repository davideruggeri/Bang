# Bang — gioco di carte western, in singolo e online

Gioco di carte western a turni per **4–7 giocatori**: sceriffo, vice, fuorilegge e rinnegato si sfidano a colpi di carte, ognuno con un ruolo segreto e un personaggio con un'abilità speciale.

Si gioca **contro bot** in partita singola oppure **online con gli amici**: chi ospita crea una lobby e gli altri si uniscono dal browser con un codice di 4 cifre. I posti liberi vengono riempiti dai bot.

Il motore di gioco è scritto in **Java**, l'interfaccia web in **React + TypeScript**; i due comunicano tramite un'API REST.

<p align="center">
  <img src="https://github.com/user-attachments/assets/817068c5-4e79-4eb5-92a4-ae4bf44da6a7" alt="Menu iniziale con le modalità di gioco" width="80%">
  <br>
  <sub><i>Menu iniziale: partita singola, crea lobby o entra in lobby</i></sub>
</p>

<table align="center">
  <tr>
    <td align="center" width="50%">
      <img src="https://github.com/user-attachments/assets/e8b71239-50d7-465e-8aa1-7e6bcd8945b5" alt="Partita singola contro i bot" width="100%">
      <br>
      <sub><i>Partita singola contro i bot</i></sub>
    </td>
    <td align="center" width="50%">
      <img src="https://github.com/user-attachments/assets/c026314d-15ed-4c4f-a8e0-f40bc35cd1eb" alt="Modalità online con lobby e codice d'invito" width="100%">
      <br>
      <sub><i>Modalità online con codice d'invito</i></sub>
    </td>
  </tr>
</table>

## Funzionalità

- **Partita singola** contro avversari controllati dal computer
- **Multigiocatore online** con lobby e codice d'invito: l'host sceglie quanti giocatori umani e quanti bot, la partita parte quando tutti si sono uniti
- **Ruoli segreti e personaggi** con abilità che modificano le regole (distanza, vite, pescate)
- **Sistema di distanza e armi**: gittata delle armi, Mustang e Mirino
- **Carte azione ed equipaggiamento**, estrazioni con seme e valore (Barile, Dinamite, Prigione)
- **Abbandono della partita** in qualsiasi momento: in online la partita si chiude per tutti e gli altri giocatori vedono chi è uscito; in lobby un ospite può uscire liberando il suo posto
- **Ogni giocatore vede solo le proprie carte**: lo stato inviato a ciascun browser nasconde la mano degli altri
- **Interfaccia animata**: carte giocate e pescate, registro della partita colorato, cimitero dei giocatori eliminati
- **Carte con grafica originale** disegnate in SVG/CSS

## Architettura

```
┌──────────────────────┐   HTTP/JSON (polling)   ┌─────────────────────────┐
│  Browser (React)     │ ──────────────────────► │  Server Java (:8080)    │
│  Vite dev server     │   /api/state            │  BangApiServer          │
│  :3000, proxy /api   │   /api/start, /api/join │  └─ motore di gioco     │
│                      │   /api/action           │     Partita, Mazzo,     │
│  (un browser per     │ ◄────────────────────── │     Persona, Bot, ...   │
│   ogni giocatore)    │   stato per giocatore   │                         │
└──────────────────────┘                         └─────────────────────────┘
```

- **Motore di gioco (`src/bang/`)**: regole, mazzo, personaggi, turni e intelligenza dei bot. Le carte e i personaggi sono definiti in `listaCarte.xml` e caricati all'avvio.
- **Server (`src/bang/server/`)**: server HTTP basato su `com.sun.net.httpserver`, senza framework esterni. Gestisce la lobby, riceve le azioni dei giocatori e restituisce a ciascuno uno snapshot dello stato serializzato in JSON con Gson.
- **Ponte sincrono/asincrono (`AsyncGameProxy`)**: il motore è scritto in stile sequenziale (“chiedi una scelta al giocatore e aspetta”); il proxy sospende il thread di gioco finché la risposta non arriva dal browser.
- **Frontend (`frontend/`)**: React 19 + TypeScript con Vite. Interroga il server ogni 400 ms e mostra le scelte disponibili come pulsanti o carte cliccabili.

| Endpoint | Metodo | Descrizione |
|---|---|---|
| `/api/start` | POST | Crea una partita singola o una lobby online e restituisce il codice |
| `/api/join` | POST | Unisce un giocatore alla lobby tramite codice e nome |
| `/api/state?player=Nome` | GET | Stato della partita visto da quel giocatore |
| `/api/action` | POST | Risposta del giocatore alla scelta richiesta |
| `/api/leave` | POST | Il giocatore abbandona la lobby o la partita in corso |
| `/api/reset` | POST | Termina la partita e torna in attesa |

## Requisiti

- Java 17 o superiore (JDK, per compilare)
- Node.js 20 o superiore

## Avvio

### Windows

1. `AVVIA-SERVER.bat` — compila e avvia il server Java su `http://localhost:8080`
2. `AVVIA-FRONTEND.bat` — installa le dipendenze (la prima volta) e apre il gioco su `http://localhost:3000`

### macOS / Linux

```bash
# Server
mkdir -p bin
javac -encoding UTF-8 -cp lib/gson-2.10.1.jar -d bin $(find src -name "*.java")
java -cp "bin:lib/gson-2.10.1.jar" bang.Main

# Frontend (in un secondo terminale)
cd frontend
npm install
npm run dev
```

Poi apri `http://localhost:3000`.

## Giocare online

1. Chi ospita avvia server e frontend sul proprio computer, sceglie **Crea Lobby**, imposta il numero di giocatori umani e bot e ottiene un **codice lobby**.
2. Gli altri giocatori aprono nel browser l'indirizzo del computer che ospita (es. `http://192.168.1.20:3000` sulla stessa rete) e scelgono **Entra in Lobby** inserendo il codice e il proprio nome.
3. Quando tutti i giocatori umani sono entrati, la partita parte automaticamente.

Per giocare con persone fuori dalla rete locale bisogna rendere raggiungibile la porta `3000` del computer che ospita, per esempio con un tunnel (ngrok, Cloudflare Tunnel) o con l'inoltro della porta sul router.

## Struttura del progetto

```
├── src/bang/            motore di gioco Java
│   └── server/          API REST e DTO dello stato
├── frontend/            interfaccia web React + TypeScript
│   └── src/cards.tsx    grafica delle carte in SVG
├── listaCarte.xml       definizione di carte e personaggi
├── lib/                 Gson
├── AVVIA-SERVER.bat
└── AVVIA-FRONTEND.bat
```

## Autore

Davide Ruggeri — progetto nato all'Università degli Studi di Brescia (Ingegneria Informatica) come gioco a linea di codice e poi esteso con interfaccia web e modalità online.

## Nota

Progetto didattico non commerciale, ispirato al gioco di carte BANG!. "BANG!" è un marchio di dV Giochi; questo progetto non è affiliato né approvato da dV Giochi. Le grafiche delle carte sono originali.
