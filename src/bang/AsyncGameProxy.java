package bang;

/**
 * Gestore globale per l'interazione Asincrona tra la GUI (Swing Event Dispatch
 * Thread)
 * e la logica del Gioco (Main Game Thread).
 * Invece di usare JOptionPane bloccanti, il gioco chiamerà i metodi qui dentro
 * che metteranno in attesa (wait) il thread del gioco finché l'utente non fa
 * click sulla GUI.
 */
public class AsyncGameProxy {
    private static final Object lock = new Object();
    private static Object result = null;
    private static boolean waiting = false;

    /**
     * Chiamato dal Thread del Gioco. Blocca l'esecuzione finché la GUI non invia
     * una risposta.
     */
    public static Object waitForInput() {
        synchronized (lock) {
            waiting = true;
            result = null;
            try {
                // Il Game Thread si ferma qui finché qualcuno non fa notify()
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            waiting = false;
            return result;
        }
    }

    /**
     * Chiamato dalla GUI (es. al click di un bottone).
     * Sblocca il thread del gioco passandogli l'input scelto.
     */
    public static void submitInput(Object input) {
        synchronized (lock) {
            if (waiting) {
                result = input;
                lock.notify(); // Risveglia waitForInput()
            }
        }
    }

    /**
     * Helper per aspettare un intero (es. indice bersaglio o indice carta).
     */
    public static int waitForInteger() {
        Object res = waitForInput();
        if (res instanceof Integer) {
            return (Integer) res;
        }
        return -1; // Fallback di errore
    }

    /**
     * Helper per aspettare un boolean (es. Yes/No per il Mancato!).
     */
    public static boolean waitForBoolean() {
        Object res = waitForInput();
        if (res instanceof Boolean) {
            return (Boolean) res;
        }
        return false;
    }
}
