package bang;

import java.io.PrintStream;

/**
 * Intercetta tutto l'output di System.out e lo invia sia al terminale
 * originale, sia alla coda eventi del server web per essere mostrato
 * nel "Registro di Gioco" del frontend.
 */
public class OutputTracer extends PrintStream {
    private final PrintStream originalOut;
    private StringBuilder currentLine = new StringBuilder();

    public OutputTracer(PrintStream originalOut) {
        super(originalOut);
        this.originalOut = originalOut;
    }

    private void processText(String text) {
        if (text == null)
            return;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                flushCurrentLine();
            } else if (c != '\r') {
                currentLine.append(c);
            }
        }
    }

    private synchronized void flushCurrentLine() {
        if (currentLine.length() > 0) {
            String line = currentLine.toString();
            // Ignora righe puramente decorative o vuote per il frontend
            if (!line.trim().isEmpty() && !line.startsWith("===") && !line.startsWith("---")) {
                // Se contiene [SKIP], non inviarlo al log del frontend
                if (!line.contains("[SKIP]")) {
                    // Tutto ciò che arriva da System.out.println va nel registro laterale, non al
                    // centro
                    bang.server.BangApiServer.addEvent("[LOG] " + line);
                }
            }
            currentLine.setLength(0);
        }
    }

    @Override
    public void print(String s) {
        originalOut.print(s);
        processText(s);
    }

    @Override
    public void println(String s) {
        originalOut.println(s);
        processText(s + "\n");
    }

    @Override
    public void println() {
        originalOut.println();
        processText("\n");
    }

    @Override
    public void print(Object obj) {
        String s = String.valueOf(obj);
        originalOut.print(s);
        processText(s);
    }

    @Override
    public void println(Object x) {
        String s = String.valueOf(x);
        originalOut.println(s);
        processText(s + "\n");
    }
}
