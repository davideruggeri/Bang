package bang;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Ponte tra chi riceve gli input (server web o GUI) e i thread del gioco.
 *
 * Due canali separati:
 * - le configurazioni di nuove partite, attese dal thread principale (Main);
 * - le scelte dei giocatori, attese dal thread della partita in corso.
 *
 * Ogni partita ha una "generazione". Quando la partita viene interrotta
 * (abbandono o reset) la generazione avanza e il vecchio thread di gioco,
 * al primo punto di attesa, riceve una {@link GameAbortedException} e termina.
 */
public class AsyncGameProxy {
    private static final Object lock = new Object();
    private static Object result = null;
    private static boolean hasResult = false;
    private static boolean waiting = false;

    private static final BlockingQueue<GameConfig> configQueue = new LinkedBlockingQueue<>();

    private static volatile int generation = 0;
    private static final ThreadLocal<Integer> threadGeneration = new ThreadLocal<>();

    /** Lanciata nel thread di una partita che è stata interrotta. */
    public static class GameAbortedException extends RuntimeException {
        public GameAbortedException() {
            super("Partita interrotta");
        }
    }

    // ── Partite ──────────────────────────────────────────────────────────────

    /** Generazione corrente: va letta prima di creare il thread di una nuova partita. */
    public static int currentGeneration() {
        return generation;
    }

    /** Lega il thread corrente alla partita della generazione indicata. */
    public static void bindCurrentThread(int gen) {
        threadGeneration.set(gen);
    }

    /** Interrompe la partita in corso: il suo thread terminerà al prossimo punto di attesa. */
    public static void abortGame() {
        synchronized (lock) {
            generation++;
            hasResult = false;
            result = null;
            lock.notifyAll();
        }
    }

    /** Vero se il thread corrente appartiene a una partita interrotta. */
    public static boolean isAborted() {
        Integer gen = threadGeneration.get();
        return gen != null && gen != generation;
    }

    /** Se il thread corrente appartiene a una partita interrotta, la termina. */
    public static void checkAborted() {
        if (isAborted()) {
            throw new GameAbortedException();
        }
    }

    /** Chiamato da Main: attende la configurazione della prossima partita. */
    public static GameConfig waitForConfig() {
        try {
            return configQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // ── Input dei giocatori ──────────────────────────────────────────────────

    /**
     * Chiamato dal thread della partita. Blocca finché non arriva una risposta
     * o finché la partita non viene interrotta.
     */
    public static Object waitForInput() {
        synchronized (lock) {
            checkAborted();
            waiting = true;
            hasResult = false;
            result = null;
            try {
                while (!hasResult) {
                    lock.wait();
                    checkAborted();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                waiting = false;
            }
            return result;
        }
    }

    /**
     * Chiamato dal server o dalla GUI. Una {@link GameConfig} avvia una nuova
     * partita; qualsiasi altro valore è la risposta al prompt attivo.
     */
    public static void submitInput(Object input) {
        if (input instanceof GameConfig) {
            configQueue.offer((GameConfig) input);
            return;
        }
        synchronized (lock) {
            if (waiting) {
                result = input;
                hasResult = true;
                lock.notifyAll();
            }
        }
    }

    /** Helper per aspettare un intero (es. indice bersaglio o indice carta). */
    public static int waitForInteger() {
        Object res = waitForInput();
        if (res instanceof Integer) {
            return (Integer) res;
        }
        return -1; // Fallback di errore
    }

    /** Helper per aspettare un boolean (es. Yes/No per il Mancato!). */
    public static boolean waitForBoolean() {
        Object res = waitForInput();
        if (res instanceof Boolean) {
            return (Boolean) res;
        }
        return false;
    }
}
