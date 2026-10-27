package bang;

import java.util.*;
import bang.server.BangApiServer;

public class Carta {
    private String nome;
    private String descrizione;
    private boolean equipaggiabile;
    private String valori;
    private String semi;

    public Carta(String nome, String descrizione, boolean equipaggiabile, String valori, String semi) {
        this.nome = nome;
        this.descrizione = descrizione;
        this.equipaggiabile = equipaggiabile;
        this.valori = valori;
        this.semi = semi;
    }

    public String getNome() {
        return nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public boolean isEquipaggiabile() {
        return equipaggiabile;
    }

    public String getValori() {
        return valori;
    }

    public String getSemi() {
        return semi;
    }

    /**
     * FIX CRITICO: usaBang ora riceve la Partita reale (non crea new Partita).
     * FIX: controllo Barile con estrazione carta (cuori = scampato).
     * FIX: se Mancato! viene usato, viene rimosso dalla mano del bersaglio.
     */
    public void usaBang(Partita partita, Persona attaccante, Persona bersaglio) {
        System.out.println("[SKIP] 🔫 " + attaccante.getNome() + " usa BANG! su " + bersaglio.getNome());

        boolean schivato = false;

        // 1) Controlla Barile prima del Mancato!
        boolean haBarile = bersaglio.haBarileEquipaggiato();
        boolean eJourdonnais = (bersaglio.getPersonaggio() == Personaggio.JOURDONNAIS);
        int tentativiBarile = 0;
        if (haBarile)
            tentativiBarile++;
        if (eJourdonnais)
            tentativiBarile++;

        for (int b = 0; b < tentativiBarile && !schivato; b++) {
            boolean usaBarile = true;
            if (!(bersaglio instanceof Bot)) {
                int scelta = InputUtil.showMenu(bersaglio.getNome(), "🎯 Uso Barile per " + bersaglio.getNome() + "?",
                        new String[] { "Estrai per il Barile", "No" });
                usaBarile = (scelta == 1);
            }
            if (!usaBarile)
                continue;

            System.out.println("🛢️ " + bersaglio.getNome() + " tenta la sorte...");
            BangApiServer.addEvent("[LOG] 🛢️ " + bersaglio.getNome() + " sventola per il Barile...");
            Carta cartaEstratta = partita.pescaCarta(null);
            if (cartaEstratta != null) {
                BangApiServer.setLastDrawnCard(cartaEstratta, true, bersaglio.getNome());
                Persona.delay(1200);
                partita.getListaScarti().add(cartaEstratta);
                System.out.println("🃏 Carta estratta: " + cartaEstratta.getNome()
                        + " [" + cartaEstratta.getValori() + " di " + cartaEstratta.getSemi() + "]");
                if (cartaEstratta.getSemi() != null && cartaEstratta.getSemi().equalsIgnoreCase("Cuori")) {
                    schivato = true;
                    System.out.println("❤️ Schivata riuscita!");
                    BangApiServer
                            .addFormattedEvent("🛡️ {P:" + bersaglio.getNome() + "} si salva grazie al {C:Barile}!");
                    BangApiServer.addEvent("[LOG] ❤️ Barile: Schivata RIUSCITA!");
                } else {
                    System.out.println("💨 Schivata fallita.");
                    BangApiServer.addEvent("[LOG] 💨 Barile: Schivata fallita.");
                }
            }
        }

        // 2) Se non ancora schivato, offri il Mancato!
        if (!schivato && bersaglio.haMancato()) {
            boolean usaMancato = false;
            if (bersaglio instanceof Bot) {
                usaMancato = true;
            } else {
                usaMancato = InputUtil
                        .readYesOrNo(bersaglio.getNome(),
                                "🎯 Schivata (Mancato!) per " + bersaglio.getNome() + "?");
            }

            if (usaMancato) {
                Carta mancato = bersaglio.usaMancatoDallaMano(partita);
                if (mancato != null) {
                    BangApiServer.setLastPlayedCard(mancato.getNome(), bersaglio.getNome(), null);
                    Persona.delay(1400);
                    partita.getListaScarti().add(mancato);
                    schivato = true;
                    System.out.println("🛡️ " + bersaglio.getNome() + " ha usato Mancato! schivando il colpo.");
                }
            }
        }

        // 3) Se non schivato, subisce danno
        if (!schivato) {
            bersaglio.subisceDanno(1, partita, attaccante);
            if (bersaglio.getVita() <= 0) {
                partita.eliminaPersona(bersaglio, attaccante);
            }
        }
    }

    public void usaBirra(Partita partita, Persona giocatore) {
        if (partita.getListaGiocatori().size() <= 2) {
            System.out
                    .println("🚫 " + giocatore.getNome() + " beve una Birra, ma con solo 2 giocatori non ha effetto!");
            return;
        }
        int vitaMax = giocatore.getVitaMax();
        if (giocatore.getVita() < vitaMax) {
            giocatore.setVita(giocatore.getVita() + 1);
            System.out.println("🍺 " + giocatore.getNome() + " brinda e recupera 1 PF! Vita: " + giocatore.getVita()
                    + "/" + vitaMax);
        } else {
            System.out.println("🚫 " + giocatore.getNome() + " è già in ottima forma, la Birra non ha effetto.");
        }
    }

    /**
     * FIX: usaCatBalou ora riceve la lista scarti reale (non crea new Partita).
     */
    public boolean usaCatBalou(Persona bersaglio, List<Carta> scarti, Persona attaccante) {
        System.out.println("✂️ Cat Balou su " + bersaglio.getNome() + "!");

        boolean haCarteMano = !bersaglio.getCarteInMano().isEmpty();
        boolean haCarteTavolo = !bersaglio.getCarteInGioco().isEmpty();

        if (!haCarteMano && !haCarteTavolo) {
            System.out.println("🚫 " + bersaglio.getNome() + " non ha più nulla da perdere (nessuna carta)!");
            return false;
        }

        if (haCarteMano && haCarteTavolo) {
            boolean manoOTavolo = InputUtil.readYesOrNo(attaccante.getNome(),
                    "Scarta per " + attaccante.getNome() + ": Mano? (No=Tavolo)");
            if (manoOTavolo) {
                return scartaCartaDaMano(bersaglio, scarti, attaccante);
            } else {
                return scartaCartaDaTavolo(bersaglio, scarti, attaccante);
            }
        } else if (haCarteMano) {
            return scartaCartaDaMano(bersaglio, scarti, attaccante);
        } else {
            return scartaCartaDaTavolo(bersaglio, scarti, attaccante);
        }
    }

    private boolean scartaCartaDaMano(Persona bersaglio, List<Carta> scarti, Persona attaccante) {
        int scelta = InputUtil.showCardSelection(attaccante.getNome(),
                "Scarta da mano di " + bersaglio.getNome() + " (" + attaccante.getNome() + "):",
                bersaglio.getCarteInMano());
        if (scelta == 0)
            return false;
        Carta c = bersaglio.getCarteInMano().remove(scelta - 1);
        scarti.add(c);
        System.out.println("🗑️ " + bersaglio.getNome() + " ha dovuto scartare " + c.getNome() + " dalla mano.");
        return true;
    }

    private boolean scartaCartaDaTavolo(Persona bersaglio, List<Carta> scarti, Persona attaccante) {
        int scelta = InputUtil.showCardSelection(attaccante.getNome(),
                "Rimuovi dal tavolo di " + bersaglio.getNome() + " (" + attaccante.getNome() + "):",
                bersaglio.getCarteInGioco());
        if (scelta == 0)
            return false;

        Carta c = bersaglio.getCarteInGioco().get(scelta - 1);
        bersaglio.scartaCartaDaGioco(c, scarti); // Usa il metodo sicuro che resetta l'arma e scarta
        System.out.println("🗑️ " + bersaglio.getNome() + " ha perso " + c.getNome() + " che era in gioco.");
        return true;
    }

    /**
     * FIX: usaDiligenza ora riceve la Partita reale (non crea new Partita).
     */
    public void usaDiligenza(Partita partita, Persona giocatore) {
        System.out.println("[SKIP] 🚂 Diligenza: " + giocatore.getNome() + " pesca 2 carte.");
        for (int i = 0; i < 2; i++) {
            Carta c = partita.pescaCarta(giocatore);
            if (c != null) {
                System.out.println("[SKIP]   🃏 Pescata: " + c.getNome());
                if (giocatore.getNome().equals(BangApiServer.currentGame.getListaGiocatori().get(0).getNome())
                        || !(giocatore instanceof Bot)) {
                    BangApiServer.setLastDrawnCard(c, false, giocatore.getNome());
                    Persona.delay(1200);
                }
            }
        }
    }

    /**
     * FIX: usaGatling ora riceve la Partita reale per il Barile/Mancato!.
     */
    public void usaGatling(Partita partita, Persona giocatoreCorrente) {
        System.out.println("[SKIP] 💥 GATLING! " + giocatoreCorrente.getNome() + " fa fuoco su tutti i presenti!");
        BangApiServer.isSilentMode = true; // Attiva modalità silenziosa
        List<String> esiti = new ArrayList<>();

        // Copia difensiva per evitare ConcurrentModificationException se qualcuno muore
        List<Persona> bersagli = new ArrayList<>(partita.getListaGiocatori());
        for (Persona bersaglio : bersagli) {
            if (!bersaglio.equals(giocatoreCorrente)) {
                int startVita = bersaglio.getVita();
                usaBang(partita, giocatoreCorrente, bersaglio);
                if (bersaglio.getVita() < startVita) {
                    esiti.add("{" + "P:" + bersaglio.getNome() + "} perde 1 PF");
                } else {
                    esiti.add("{" + "P:" + bersaglio.getNome() + "} schiva");
                }
            }
        }
        BangApiServer.isSilentMode = false; // Disattiva modalità silenziosa

        // Invia un singolo evento riassuntivo
        String riassunto = String.join(", ", esiti);
        BangApiServer.addFormattedEvent("🔥 {C:Gatling} effetti: " + riassunto);
    }

    public void usaMancato(Persona giocatore) {
        System.out.println("[SKIP] " + giocatore.getNome() + " usa Mancato! per schivare.");
    }

    public void usaMirino(Persona giocatore) {
        System.out.println("[SKIP] Mirino equipaggiato: " + giocatore.getNome() + " vede tutti a -1 distanza.");
        // La riduzione di distanza è calcolata dinamicamente in
        // Partita.calcolaDistanza()
    }

    public void usaMustang(Persona giocatore) {
        System.out.println(
                "[SKIP] Mustang equipaggiato: " + giocatore.getNome() + " appare a +1 distanza agli avversari.");
        // L'aumento di distanza è calcolato dinamicamente in Partita.calcolaDistanza()
    }

    public boolean usaPanico(Persona bersaglio, Persona attaccante) {
        Carta rubata = attaccante.rubaCarta(bersaglio, attaccante);
        if (rubata != null) {
            System.out.println("[SKIP] 😱 " + attaccante.getNome() + " ha usato Panico! seminando il terrore presso "
                    + bersaglio.getNome());
            return true;
        }
        return false;
    }

    public void usaSaloon(List<Persona> giocatori) {
        System.out.println("[SKIP] 🍻 Saloon! Aria di festa, tutti recuperano 1 PF.");
        for (Persona giocatore : giocatori) {
            int vitaMax = giocatore.getVitaMax();
            if (giocatore.getVita() < vitaMax) {
                giocatore.setVita(giocatore.getVita() + 1);
                System.out.println(
                        "  ✨ " + giocatore.getNome() + " recupera 1 PF. Vita: " + giocatore.getVita() + "/" + vitaMax);
            }
        }
    }

    public void usaWellsFargo(Partita partita, Persona giocatore) {
        System.out.println("[SKIP] 🏦 Wells Fargo: arrivo del convoglio, " + giocatore.getNome() + " pesca 3 carte.");
        for (int i = 0; i < 3; i++) {
            Carta c = partita.pescaCarta(giocatore);
            if (c != null) {
                System.out.println("[SKIP]   🃏 Pescata: " + c.getNome());
                if (giocatore.getNome().equals(BangApiServer.currentGame.getListaGiocatori().get(0).getNome())
                        || !(giocatore instanceof Bot)) {
                    BangApiServer.setLastDrawnCard(c, false, giocatore.getNome());
                    Persona.delay(1200);
                }
            }
        }
    }

    public void usaEmporio(Partita partita, Persona giocatoreCorrente) {
        System.out.println(
                "[SKIP] 🛒 Emporio! " + giocatoreCorrente.getNome() + " apre un mercato per tutti i pistoleri!");
        List<Persona> giocatori = partita.getListaGiocatori();
        List<Carta> emporio = new ArrayList<>();

        // Pesca N carte per N giocatori vivi
        for (int i = 0; i < giocatori.size(); i++) {
            Carta c = partita.pescaCarta(null);
            if (c != null) {
                emporio.add(c);
            }
        }

        int startingIdx = giocatori.indexOf(giocatoreCorrente);
        for (int offset = 0; offset < giocatori.size(); offset++) {
            if (emporio.isEmpty())
                break;

            Persona p = giocatori.get((startingIdx + offset) % giocatori.size());
            if (p instanceof Bot) {
                // I Bot prendono la prima carta
                Carta presa = emporio.remove(0);
                p.getCarteInMano().add(presa);
                System.out.println("  🛍️ " + p.getNome() + " sceglie " + presa.getNome() + " dall'Emporio.");
            } else {
                int scelta = InputUtil.showCardSelection(p.getNome(),
                        "Scelta Emporio per " + p.getNome() + ":", emporio);
                if (scelta == 0)
                    scelta = 1; // Forza la prima se annulla (non si può annullare un emporio in corso)
                Carta presa = emporio.remove(scelta - 1);
                p.getCarteInMano().add(presa);
                System.out.println("  🛍️ " + p.getNome() + " ha preso " + presa.getNome() + ".");
            }
        }
    }

    public void usaIndiani(Partita partita, Persona giocatoreCorrente) {
        System.out.println(
                "[SKIP] 🏹 Indiani! Grida di guerra in lontananza, " + giocatoreCorrente.getNome()
                        + " lancia l'attacco!");
        BangApiServer.isSilentMode = true;
        List<String> esiti = new ArrayList<>();
        List<Persona> giocatori = new ArrayList<>(partita.getListaGiocatori());

        for (Persona bersaglio : giocatori) {
            if (bersaglio.equals(giocatoreCorrente))
                continue;

            boolean haScartatoBang = false;

            if (bersaglio instanceof Bot) {
                for (Carta c : bersaglio.getCarteInMano()) {
                    if (c.getNome().equals("BANG!")) {
                        bersaglio.getCarteInMano().remove(c);
                        partita.getListaScarti().add(c);
                        haScartatoBang = true;
                        System.out.println("  💥 " + bersaglio.getNome() + " respinge gli Indiani con un BANG!.");
                        break;
                    }
                }
            } else {
                List<Carta> bangInMano = new ArrayList<>();
                for (Carta c : bersaglio.getCarteInMano()) {
                    if (c.getNome().equals("BANG!"))
                        bangInMano.add(c);
                }
                if (!bangInMano.isEmpty()) {
                    int scelta = InputUtil.showCardSelection(bersaglio.getNome(),
                            "🏹 Difesa Indiani per " + bersaglio.getNome() + " (Scarta BANG!):",
                            bangInMano);
                    if (scelta > 0) {
                        Carta cDaScartare = bangInMano.get(scelta - 1);
                        bersaglio.getCarteInMano().remove(cDaScartare);
                        partita.getListaScarti().add(cDaScartare);
                        haScartatoBang = true;
                        System.out.println("  🛡️ " + bersaglio.getNome() + " ha respinto l'attacco.");
                    }
                }
            }

            if (!haScartatoBang) {
                System.out.println("  🏹 " + bersaglio.getNome() + " viene travolto dall'attacco!");
                bersaglio.subisceDanno(1, partita, giocatoreCorrente);
                if (bersaglio.getVita() <= 0) {
                    partita.eliminaPersona(bersaglio, giocatoreCorrente);
                }
                esiti.add("{" + "P:" + bersaglio.getNome() + "} perde 1 PF");
            } else {
                esiti.add("{" + "P:" + bersaglio.getNome() + "} si difende");
            }
        }
        BangApiServer.isSilentMode = false;
        String riassunto = String.join(", ", esiti);
        BangApiServer.addFormattedEvent("🏹 {C:Indiani} effetti: " + riassunto);
    }

    public void usaDuello(Partita partita, Persona sfidante, Persona bersaglio) {
        System.out.println(
                "[SKIP] ⚔️ Duello! " + sfidante.getNome() + " lancia il guanto di sfida a " + bersaglio.getNome()
                        + "!");

        boolean turnoDifensore = true; // Inizia sempre il bersaglio
        Persona attivoInDuello = bersaglio;

        while (true) {
            boolean scartatoBang = false;
            if (attivoInDuello instanceof Bot) {
                for (Carta c : attivoInDuello.getCarteInMano()) {
                    if (c.getNome().equals("BANG!")) {
                        attivoInDuello.getCarteInMano().remove(c);
                        partita.getListaScarti().add(c);
                        scartatoBang = true;
                        System.out.println("  💥 " + attivoInDuello.getNome() + " risponde al duello!");
                        break;
                    }
                }
            } else {
                List<Carta> suoBang = new ArrayList<>();
                for (Carta c : attivoInDuello.getCarteInMano()) {
                    if (c.getNome().equals("BANG!"))
                        suoBang.add(c);
                }
                if (!suoBang.isEmpty()) {
                    int scelta = InputUtil.showCardSelection(attivoInDuello.getNome(),
                            "⚔️ Risposta Duello per " + attivoInDuello.getNome() + " (Scarta BANG!):",
                            suoBang);
                    if (scelta > 0) {
                        Carta cb = suoBang.get(scelta - 1);
                        attivoInDuello.getCarteInMano().remove(cb);
                        partita.getListaScarti().add(cb);
                        scartatoBang = true;
                        System.out.println("  💥 " + attivoInDuello.getNome() + " risponde al fuoco!");
                    }
                }
            }

            if (!scartatoBang) {
                System.out.println(
                        "  💀 " + attivoInDuello.getNome() + " non ha più colpi da sparare e perde il duello!");
                attivoInDuello.subisceDanno(1, partita, sfidante);
                if (attivoInDuello.getVita() <= 0) {
                    partita.eliminaPersona(attivoInDuello, sfidante);
                }
                break;
            }

            // Turno passa all'altro
            turnoDifensore = !turnoDifensore;
            attivoInDuello = turnoDifensore ? bersaglio : sfidante;
        }
    }

    public void usaPrigione(Persona bersaglio) {
        System.out.println("[SKIP] ⛓️ " + bersaglio.getNome() + " è stato trascinato in Prigione!");
    }

    public void usaDinamite(Persona giocatore) {
        System.out
                .println("[SKIP] 🧨 " + giocatore.getNome() + " ha innescato la Dinamite! Il fumo inizia a salire...");
    }

    @Override
    public String toString() {
        return "Carta{" + nome + '}';
    }
}
