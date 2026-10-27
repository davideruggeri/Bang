package bang.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import bang.AsyncGameProxy;
import bang.GameConfig;
import bang.Partita;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Server HTTP leggero che espone le API REST del motore Bang!
 * Il frontend Vite (su :3000) usa queste API per tutta la grafica.
 */
public class BangApiServer {
    private static HttpServer server;

    // Stato globale della partita
    public static volatile Partita currentGame = null;
    private static volatile String gameStatus = "IDLE";
    private static volatile String currentPlayerName = null;

    private static volatile String promptTarget = null;
    private static volatile String promptTipo = null;
    private static volatile String promptMsg = null;
    private static volatile List<String> promptOpzioni = null;

    // Lobby State
    private static volatile String lobbyCode = null;
    private static volatile int expectedHumans = 0;
    private static final List<String> joinedHumanNames = new java.util.ArrayList<>();
    private static final List<String> pendingBotNames = new java.util.ArrayList<>();
    private static volatile int totalPlayersCount = 0;

    // Log degli eventi visibili nel frontend (ultimi 1000)
    private static final int MAX_EVENTI = 1000;
    private static final ConcurrentLinkedDeque<String> eventi = new ConcurrentLinkedDeque<>();

    // Supporto per animazioni (vengono resettati dopo l'uso o periodicamente)
    private static volatile GameStateDTO.CardDTO lastPlayedCard = null;
    private static volatile String lastPlayedActor = null;
    private static volatile String lastPlayedTarget = null;
    private static volatile GameStateDTO.CardDTO lastDrawnCard = null;

    public static void setLastPlayedCard(String nome, String actor, String target) {
        lastDrawnCard = null; // Mutua esclusione: nasconde carte pescate vecchie
        if (nome == null) {
            lastPlayedCard = null;
            lastPlayedActor = null;
            lastPlayedTarget = null;
            return;
        }
        lastPlayedCard = new GameStateDTO.CardDTO();
        lastPlayedCard.nome = nome;
        lastPlayedCard.descrizione = ""; // Non serve desc per animazione
        lastPlayedCard.imagePath = bang.InputUtil.getImagePathForCard(nome);
        lastPlayedCard.timestamp = System.currentTimeMillis();
        lastPlayedActor = actor;
        lastPlayedTarget = target;
    }

    public static void setLastDrawnCard(bang.Carta carta) {
        setLastDrawnCard(carta, false, null);
    }

    public static void setLastDrawnCard(bang.Carta carta, boolean isEstrazione) {
        setLastDrawnCard(carta, isEstrazione, null);
    }

    public static void setLastDrawnCard(bang.Carta carta, boolean isEstrazione, String ownerName) {
        lastPlayedCard = null; // Mutua esclusione: nasconde carte giocate vecchie
        lastPlayedActor = null;
        lastPlayedTarget = null;
        // Privacy: non mostriamo i pescaggi privati dei bot (es. premi per
        // eliminazione)
        if (!isEstrazione && ownerName != null && currentGame != null) {
            for (bang.Persona p : currentGame.getListaGiocatori()) {
                if (p.getNome().equals(ownerName)) {
                    if (p instanceof bang.Bot) {
                        return; // Ignora l'animazione per i bot
                    }
                }
            }
        }
        lastDrawnCard = GameStateDTO.CardDTO.from(carta, isEstrazione, ownerName);
    }

    public static void clearLastCards() {
        lastPlayedCard = null;
        lastDrawnCard = null;
    }

    /** Flag per silencere gli eventi di massa */
    public static boolean isSilentMode = false;

    /**
     * Aggiunge un evento al log. Se isSilentMode è true, nasconde il log dal
     * frontend.
     */
    public static void addEvent(String msg) {
        if (msg == null || eventi.size() >= MAX_EVENTI)
            return;

        if (isSilentMode && !msg.startsWith("[SKIP]")) {
            eventi.add("[SKIP] " + msg);
        } else {
            eventi.add(msg);
        }
    }

    /** Restituisce la lista eventi corrente (copia). */
    public static java.util.List<String> getEventi() {
        return new java.util.ArrayList<>(eventi);
    }

    /** Pulisce il log (ad inizio partita). */
    public static void clearEventi() {
        eventi.clear();
    }

    /**
     * Aggiunge un evento formattato usando tag speciali:
     * {P:Nome} -> Giocatore
     * {C:Carta} -> Carta
     * {I:Icona} -> Icona specifica
     */
    public static void addFormattedEvent(String msg) {
        addEvent(msg);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Metodi di aggiornamento stato (chiamati dal game thread)
    // ──────────────────────────────────────────────────────────────────────────

    public static void setGameStatus(String status) {
        gameStatus = status;
    }

    public static void setCurrentPlayer(String name) {
        currentPlayerName = name;
    }

    /** Imposta il prompt attivo — il browser lo mostra come pulsanti. */
    public static void setPrompt(String target, String tipo, String msg, String[] opzioni) {
        promptTarget = target;
        promptTipo = tipo;
        promptMsg = msg;
        promptOpzioni = opzioni != null ? Arrays.asList(opzioni) : null;
    }

    /** Cancella il prompt dopo che il browser ha risposto. */
    public static void clearPrompt() {
        promptTarget = null;
        promptTipo = null;
        promptMsg = null;
        promptOpzioni = null;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Avvio server
    // ──────────────────────────────────────────────────────────────────────────

    public static void startServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/state", new GameStateHandler());
        server.createContext("/api/start", new StartGameHandler());
        server.createContext("/api/action", new PlayerActionHandler());
        server.createContext("/api/reset", new ResetHandler());
        server.createContext("/api/join", new JoinHandler()); // Nuovo endpoint

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();

        reset(); // Pulisce lo stato all'avvio
        System.out.println("Bang! API Server in ascolto su http://localhost:" + port);
    }

    /** Reset totale dello stato per una nuova partita. */
    public static void reset() {
        currentGame = null;
        gameStatus = "IDLE";
        lobbyCode = null;
        joinedHumanNames.clear();
        expectedHumans = 0;
        currentPlayerName = null;
        clearPrompt();
        clearEventi();
        clearLastCards();
        lastPlayedActor = null;
        lastPlayedTarget = null;
    }

    public static void stopServer() {
        if (server != null)
            server.stop(0);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CORS helper
    // ──────────────────────────────────────────────────────────────────────────

    private static void enableCORS(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/state → snapshot completo della partita
    // ──────────────────────────────────────────────────────────────────────────

    static class GameStateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            enableCORS(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            // Estrazione parametro player dalla query (es. /api/state?player=Nome)
            String query = ex.getRequestURI().getQuery();
            String playerParam = null;
            if (query != null && query.contains("player=")) {
                for (String p : query.split("&")) {
                    if (p.startsWith("player=")) {
                        playerParam = java.net.URLDecoder.decode(p.substring(7), StandardCharsets.UTF_8);
                        break;
                    }
                }
            }

            String json;
            if (currentGame == null && (gameStatus == null || gameStatus.equals("IDLE"))) {
                json = "{\"status\":\"IDLE\"}";
            } else {
                GameStateDTO dto = GameStateDTO.from(
                        currentGame, gameStatus, currentPlayerName,
                        promptTarget, promptTipo, promptMsg, promptOpzioni,
                        getEventi(),
                        lastPlayedCard,
                        lastDrawnCard,
                        lastPlayedActor,
                        lastPlayedTarget,
                        playerParam,
                        lobbyCode, expectedHumans, joinedHumanNames); // Passiamo i dati lobby
                json = new Gson().toJson(dto);
            }
            sendJson(ex, 200, json);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/start → avvia una partita con la config inviata dal browser
    // Body JSON: { "numGiocatori": 4, "numUmani": 1,
    // "nomiGiocatori": ["Io","Bot1","Bot2","Bot3"],
    // "isTorneo": false }
    // ──────────────────────────────────────────────────────────────────────────

    static class StartGameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            enableCORS(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1);
                return;
            }

            if (gameStatus != null && !gameStatus.equals("IDLE") && !gameStatus.startsWith("FINITA")) {
                sendJson(ex, 409, "{\"error\":\"Partita già in corso o in lobby\"}");
                return;
            }

            try {
                String body = readBody(ex);
                JsonObject obj = JsonParser.parseString(body).getAsJsonObject();

                int numGiocatori = obj.has("numGiocatori") ? obj.get("numGiocatori").getAsInt() : 4;
                int numUmani = obj.has("numUmani") ? obj.get("numUmani").getAsInt() : 1;

                // Inizializzazione Lobby
                lobbyCode = String.format("%04d", new java.util.Random().nextInt(10000));
                expectedHumans = numUmani;
                totalPlayersCount = numGiocatori;
                joinedHumanNames.clear();
                pendingBotNames.clear();

                // Leggi nomi giocatori
                java.util.List<String> nomiArr = new java.util.ArrayList<>();
                if (obj.has("nomiGiocatori")) {
                    for (var el : obj.get("nomiGiocatori").getAsJsonArray()) {
                        nomiArr.add(el.getAsString());
                    }
                }

                for (int i = 0; i < numGiocatori; i++) {
                    String nome = (i < nomiArr.size()) ? nomiArr.get(i)
                            : (i < numUmani ? "Giocatore " + (i + 1) : "Bot " + (i + 1));
                    if (i < numUmani) {
                        // Il primo (host) viene aggiunto subito se presente nel body
                        if (i == 0)
                            joinedHumanNames.add(nome);
                    } else {
                        pendingBotNames.add(nome);
                    }
                }

                gameStatus = "WAITING_FOR_PLAYERS";

                // Se serviva solo 1 umano (l'host), avviamo subito
                if (joinedHumanNames.size() >= expectedHumans) {
                    startGameInternal();
                }

                sendJson(ex, 200, "{\"success\":true, \"lobbyCode\":\"" + lobbyCode + "\"}");
            } catch (Exception e) {
                sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    private static void startGameInternal() {
        java.util.List<String> tuttiINomi = new java.util.ArrayList<>(joinedHumanNames);
        tuttiINomi.addAll(pendingBotNames);

        GameConfig config = new GameConfig(false, totalPlayersCount, joinedHumanNames.size(), tuttiINomi);
        gameStatus = "IN_CORSO";
        AsyncGameProxy.submitInput(config);
    }

    static class JoinHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            enableCORS(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equals(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readBody(ex);
                JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                String code = obj.has("code") ? obj.get("code").getAsString() : "";
                String name = obj.has("name") ? obj.get("name").getAsString() : "";

                if (lobbyCode == null || !lobbyCode.equals(code)) {
                    sendJson(ex, 400, "{\"error\":\"Codice lobby errato o lobby inesistente\"}");
                    return;
                }

                if (joinedHumanNames.size() >= expectedHumans) {
                    sendJson(ex, 400, "{\"error\":\"Lobby piena\"}");
                    return;
                }

                if (!joinedHumanNames.contains(name)) {
                    joinedHumanNames.add(name);
                }

                if (joinedHumanNames.size() >= expectedHumans) {
                    startGameInternal();
                }

                sendJson(ex, 200, "{\"success\":true}");
            } catch (Exception e) {
                sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/action → risponde al prompt attivo del giocatore umano
    // Body JSON: { "valore": 1 } ← indice 1-based dell'opzione scelta
    // o: { "valore": "Nome" } ← per tipo STRING
    // ──────────────────────────────────────────────────────────────────────────

    static class PlayerActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            enableCORS(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(405, -1);
                return;
            }

            if (promptTipo == null) {
                sendJson(ex, 409, "{\"error\":\"Nessun prompt attivo\"}");
                return;
            }

            try {
                String body = readBody(ex);
                JsonObject obj = JsonParser.parseString(body).getAsJsonObject();

                Object input;
                if ("STRING".equals(promptTipo)) {
                    input = obj.has("valore") ? obj.get("valore").getAsString() : "";
                } else {
                    // MENU, CARD, YESNO, PERSONA → indice numerico 1-based
                    input = obj.has("valore") ? obj.get("valore").getAsInt() : 1;
                }

                // Sblocca il game thread che è in waitForInput()
                AsyncGameProxy.submitInput(input);

                sendJson(ex, 200, "{\"success\":true}");
            } catch (Exception e) {
                sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/reset → pulisce lo stato e torna IDLE
    // ──────────────────────────────────────────────────────────────────────────

    static class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            enableCORS(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            reset();
            // Risveglia il thread principale (Main.java) se è bloccato in waitForInput
            AsyncGameProxy.submitInput(null);
            sendJson(ex, 200, "{\"success\":true}");
        }
    }
}
