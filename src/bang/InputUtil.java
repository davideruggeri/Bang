package bang;

import bang.server.BangApiServer;
import java.util.List;

/**
 * Classe di utilità per la lettura interattiva del giocatore UMANO.
 * Tutta la comunicazione avviene via HTTP: il server espone un "prompt"
 * sul cui lo stato, il browser mostra i bottoni, e invia la risposta
 * tramite POST /api/action. AsyncGameProxy fa da bridge thread-safe.
 *
 * I Bot NON passano da qui: Bot.java sovrascrive turnoGiocatore() direttamente.
 */
public class InputUtil {

    /** Legge una stringa (nome giocatore, etc.) tramite prompt web. */
    public static String readString(String playerName, String prompt, boolean required) {
        BangApiServer.setPrompt(playerName, "STRING", prompt, new String[0]);
        Object res = AsyncGameProxy.waitForInput();
        BangApiServer.clearPrompt();
        if (res instanceof String) {
            String s = (String) res;
            if (required && s.trim().isEmpty())
                return "Giocatore";
            return s.trim();
        }
        return required ? "Giocatore" : "";
    }

    /** Legge un intero compreso tra min e max tramite menu web. */
    public static int readIntegerBetween(String playerName, String prompt, int min, int max) {
        String[] opzioni = new String[max - min + 1];
        for (int i = 0; i <= max - min; i++) {
            opzioni[i] = String.valueOf(min + i);
        }
        BangApiServer.setPrompt(playerName, "MENU", prompt, opzioni);
        Object res = AsyncGameProxy.waitForInput();
        BangApiServer.clearPrompt();
        if (res instanceof Integer) {
            int val = (Integer) res;
            // Il browser manda 1-based index (posizione nell'array)
            return min + val - 1;
        }
        return min;
    }

    /** Chiede sì/no e restituisce true per sì. */
    public static boolean readYesOrNo(String playerName, String prompt) {
        BangApiServer.setPrompt(playerName, "YESNO", prompt, new String[] { "Sì", "No" });
        Object res = AsyncGameProxy.waitForInput();
        BangApiServer.clearPrompt();
        if (res instanceof Integer) {
            return ((Integer) res) == 1; // 1 = Sì, 2 = No
        }
        if (res instanceof Boolean) {
            return (Boolean) res;
        }
        return false;
    }

    /**
     * Mostra un menu con N opzioni e restituisce la scelta 1-based.
     */
    public static int showMenu(String playerName, String title, String[] options) {
        BangApiServer.setPrompt(playerName, "MENU", title, options);
        Object res = AsyncGameProxy.waitForInput();
        BangApiServer.clearPrompt();
        if (res instanceof Integer) {
            return (Integer) res; // già 1-based dal browser
        }
        return 1;
    }

    /**
     * Mostra le carte in mano e restituisce l'indice scelto (1-based).
     */
    public static int showCardSelection(String playerName, String prompt, List<Carta> carte) {
        String[] nomiCarte = new String[carte.size() + 1];
        for (int i = 0; i < carte.size(); i++) {
            nomiCarte[i] = carte.get(i).getNome();
        }
        nomiCarte[carte.size()] = "Annulla";
        BangApiServer.setPrompt(playerName, "CARD", prompt, nomiCarte);
        Object res = AsyncGameProxy.waitForInput();
        BangApiServer.clearPrompt();
        if (res instanceof Integer) {
            int val = (Integer) res;
            if (val == carte.size() + 1)
                return 0; // Annulla
            return val; // 1-based
        }
        return 0; // default annulla
    }

    /**
     * Mostra una lista di persone e restituisce quella scelta dal browser.
     */
    public static Persona scegliPersona(String playerName, String prompt, List<Persona> bersagli) {
        String[] nomi = new String[bersagli.size() + 1];
        for (int i = 0; i < bersagli.size(); i++) {
            nomi[i] = bersagli.get(i).getNome();
        }
        nomi[bersagli.size()] = "Annulla";
        BangApiServer.setPrompt(playerName, "PERSONA", prompt, nomi);
        Object res = AsyncGameProxy.waitForInput();
        BangApiServer.clearPrompt();
        if (res instanceof Integer) {
            int idx = (Integer) res - 1; // converti da 1-based
            if (idx >= 0 && idx < bersagli.size()) {
                return bersagli.get(idx);
            }
        }
        return null; // Annulla
    }

    /**
     * Helper legacy — mappa nome file immagine per la carta (usato dal frontend).
     */
    public static String getImagePathForCard(String cardName) {
        String filename;
        switch (cardName) {
            case "BANG!":
                filename = "BANG!.png";
                break;
            case "Mancato!":
                filename = "Mancato!.png";
                break;
            case "Panico!":
                filename = "Panico.png";
                break;
            case "Cat Balou":
                filename = "CatBalou.png";
                break;
            case "Wells Fargo":
                filename = "WellsFargo.png";
                break;
            case "Rev. Carabine":
                filename = "RevCarabine.png";
                break;
            case "Winchester":
                filename = "Wichester.png";
                break;
            case "Gatling":
                filename = "Catling.png";
                break;
            case "Saloon":
                filename = "Saloon.png";
                break;
            case "Diligenza":
                filename = "Diligenza.png";
                break;
            case "Birra":
                filename = "Birra.png";
                break;
            case "Schofield":
                filename = "Schofield.png";
                break;
            case "Remington":
                filename = "Remington.png";
                break;
            case "Mustang":
                filename = "Mustang.png";
                break;
            case "Mirino":
                filename = "Mirino.png";
                break;
            case "Barile":
                filename = "Barile.png";
                break;
            default:
                filename = cardName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".png";
                break;
        }
        return "cards/" + filename;
    }
}
