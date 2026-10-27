package bang;

import java.util.List;
import bang.server.BangApiServer;

public class Main {
    public static void main(String[] args) {
        // Redirige tutto l'output del terminale anche al frontend!
        System.setOut(new OutputTracer(System.out));

        // Avvia il server HTTP REST sulla porta 8080.
        // Il frontend Vite (su :3000) si collega qui per la grafica.
        try {
            BangApiServer.startServer(8080);
        } catch (Exception e) {
            System.err.println("Impossibile avviare API Server Web: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("=== BANG! Server avviato su http://localhost:8080 ===");
        System.out.println("=== Apri il browser su http://localhost:3000 per giocare ===");
        System.out.println("=== In attesa che il browser avvii una partita... ===");

        boolean giocaAncora = true;

        while (giocaAncora) {
            // Aspetta che il browser invii POST /api/start con la configurazione
            // Aspetta che il browser invii POST /api/start con la configurazione
            Object input = AsyncGameProxy.waitForInput();
            if (input == null || !(input instanceof GameConfig)) {
                System.out.println("--- Reset ricevuto o input invalido. In attesa di nuova configurazione... ---");
                continue; // Non uscire, aspetta ancora
            }
            GameConfig config = (GameConfig) input;

            System.out.println("=== PARTITA RICEVUTA DAL BROWSER ===");
            System.out.println("Giocatori: " + config.numGiocatori + " | Umani: " + config.numUmani);

            Classifica classifica = null;
            if (config.isTorneo) {
                classifica = new Classifica();
            }

            Mazzo mazzo = new Mazzo();
            Partita partita = new Partita(mazzo);
            partita.regole();

            List<Boolean> isBotList = new java.util.ArrayList<>();
            for (int i = 0; i < config.numGiocatori; i++) {
                boolean isBot = i >= config.numUmani;
                isBotList.add(isBot);
                if (config.isTorneo && classifica != null) {
                    classifica.iscriviGiocatore(config.nomiGiocatori.get(i));
                    classifica.pagaTassa(config.nomiGiocatori.get(i));
                }
            }

            partita.inizializzaGiocatori(config.nomiGiocatori, isBotList);

            // Agganciamo la partita al server Web
            BangApiServer.setGameStatus("IN_CORSO");
            BangApiServer.currentGame = partita;

            Classifica finalClassifica = classifica;
            Thread gameThread = new Thread(() -> {
                partita.inizioPartita();
                System.out.println("\n=== INIZIO PARTITA ===");

                RisultatoPartita risultato = RisultatoPartita.IN_CORSO;

                while (risultato == RisultatoPartita.IN_CORSO) {
                    // Controllo se la partita è stata resettata esternamente
                    if (BangApiServer.currentGame != partita) {
                        System.out.println("--- Partita interrotta (reset). ---");
                        return;
                    }

                    if (partita.getListaGiocatori().isEmpty())
                        break;

                    int idx = partita.getGiocatoreCorrente() % partita.getListaGiocatori().size();
                    Persona giocatore = partita.getListaGiocatori().get(idx);

                    BangApiServer.setCurrentPlayer(giocatore.getNome());
                    System.out.println("\n[SKIP] =============================");
                    System.out.println(
                            "[SKIP] 🔔 È il turno di: " + giocatore.getNome() + " [" + giocatore.getRuolo() + "]");
                    bang.server.BangApiServer
                            .addFormattedEvent("🔔 È il turno di: {I:PLAYER} {P:" + giocatore.getNome() + "}");

                    giocatore.turnoGiocatore(partita);

                    risultato = partita.verificaVittoria();
                    if (risultato != RisultatoPartita.IN_CORSO) {
                        // Se la partita è finita durante il turno, aggiorniamo subito lo stato
                        BangApiServer.setGameStatus("FINITA: " + risultato);
                        BangApiServer.clearPrompt();
                    }

                    if (risultato == RisultatoPartita.IN_CORSO) {
                        // FIX: Avanza il turno solo se il giocatore NON è stato eliminato.
                        // Se è stato eliminato (es. Dinamite), il prossimo giocatore è già "scivolato"
                        // all'indice corrente.
                        if (partita.getListaGiocatori().contains(giocatore)) {
                            partita.cambioTurno();
                        }
                    }
                }

                System.out.println("\n=== PARTITA CONCLUSA: " + risultato + " ===");
                BangApiServer.setGameStatus("FINITA: " + risultato);
                BangApiServer.clearPrompt();

                if (config.isTorneo && risultato != RisultatoPartita.IN_CORSO && finalClassifica != null) {
                    finalClassifica.distribuisciPremi(risultato, config.numGiocatori, partita);
                    finalClassifica.salvaSuFile("classifica.csv");
                }
            });
            gameThread.setDaemon(true); // Impostato a daemon così non blocca la chiusura se JVM termina
            gameThread.start();

            // Rimosso gameThread.join() per permettere al main loop di restare in ascolto
            // di nuovi start/reset senza essere bloccato dall'esecuzione della partita.
            // Il reset del server (StartGameHandler) invierà un notify all'AsyncGameProxy
            // che sbloccherà il main thread.
        }

        System.out.println("Server in chiusura. Grazie per aver giocato!");
    }
}
