# Bang

Implementazione di un gioco di carte western a turni, con motore di gioco in Java e interfaccia web in React + TypeScript.

Autore: Davide Ruggeri

## Struttura

- `src/bang/` — motore di gioco in Java (mazzo, personaggi, bot, partita) e interfaccia Swing
- `src/bang/server/` — server HTTP che espone lo stato della partita al frontend
- `frontend/` — interfaccia web (React + Vite)
- `listaCarte.xml` — definizione delle carte
- `lib/` — dipendenze Java (Gson)

## Avvio

Su Windows:

1. `AVVIA-SERVER.bat` — compila e avvia il server Java su `http://localhost:8080`
2. `AVVIA-FRONTEND.bat` — avvia l'interfaccia web

## Nota

Progetto didattico non commerciale. "BANG!" è un marchio di dV Giochi; questo progetto non è affiliato né approvato da dV Giochi.
