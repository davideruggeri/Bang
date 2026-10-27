package bang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import bang.server.BangApiServer;

public class Persona {
    private String nome;

    public static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String ruolo;
    private Personaggio personaggio;
    private int vita;
    private int vitaMax;
    private List<Carta> carteInMano;
    private List<Carta> carteInGioco;
    private Pistola pistola;

    public Persona(String nome, String ruolo, Personaggio personaggio) {
        this.nome = nome;
        this.ruolo = ruolo;
        this.personaggio = personaggio;

        // Lo Sceriffo ha 1 PF in più rispetto ai PF base del personaggio
        int pf = personaggio.getPfBase();
        if (ruolo.equals("Sceriffo")) {
            pf += 1;
        }
        this.vita = pf;
        this.vitaMax = pf;

        this.carteInMano = new ArrayList<>();
        this.carteInGioco = new ArrayList<>();
        this.pistola = new Pistola("Colt .45", "Arma di base, portata 1.", true, null, null, 1);
    }

    public String getNome() {
        return nome;
    }

    public String getRuolo() {
        return ruolo;
    }

    /**
     * Gestisce il turno del giocatore.
     * FIX CRITICO: ora riceve la Partita reale come parametro (non crea una new
     * Partita vuota).
     * FIX: aggiunto limite di 1 BANG! per turno.
     * FIX: controllo distanza prima di sparare con BANG!
     * FIX: gestione corretta sostituzione arma (scarta la vecchia).
     */
    public void turnoGiocatore(Partita partita) {
        // FASE 0: Controlli Equipaggiamenti a tempo (Dinamite, Prigione)
        boolean saltaTurno = gestisciDinamiteEPrigione(partita);
        if (saltaTurno) {
            System.out.println(nome + " salta il turno a causa della Prigione (o morte da Dinamite)!");
            return;
        }

        // Fase 1: pesca 2 carte dal mazzo della partita reale
        System.out.println("[SKIP] \n--- 🤠 È il turno di " + nome + " [" + ruolo + "] ---");
        BangApiServer.clearLastCards();

        if (!(this instanceof bang.Bot)) {
            for (int i = 0; i < 2; i++) {
                // Messaggio specifico per far capire al frontend di abilitare il click sul
                // mazzo
                InputUtil.showMenu(nome, "Pesca per " + nome + " (" + (i + 1) + "/2). Clicca sul mazzo!",
                        new String[] { "Pesca dal mazzo" });
                Carta cartaPescata = partita.pescaCarta(this);
                if (cartaPescata != null) {
                    BangApiServer.setLastDrawnCard(cartaPescata, false, nome);
                    delay(1400);
                }
            }
            BangApiServer.addEvent("📦 " + nome + " pesca 2 carte.");
        } else {
            for (int i = 0; i < 2; i++) {
                Carta cartaPescata = partita.pescaCarta(this);
                if (cartaPescata != null) {
                    System.out.println("[SKIP] 🃏 " + nome + " ha pescato: " + cartaPescata.getNome());
                }
            }
            BangApiServer.addEvent("📦 " + nome + " pesca 2 carte.");
        }

        // Stato aggiornato automaticamente via /api/state polling

        // Elimina eventuali giocatori morti durante il turno di altri
        List<Persona> daEliminare = new ArrayList<>();
        for (Persona p : partita.getListaGiocatori()) {
            if (p.getVita() <= 0) {
                daEliminare.add(p);
            }
        }
        for (Persona p : daEliminare) {
            partita.eliminaPersona(p);
        }

        // Fase 2: fase azione (il giocatore può giocare carte)
        boolean bangUsato = false; // FIX: limite 1 BANG! per turno
        boolean fineTurno = false;
        do {

            List<String> opzioniList = new ArrayList<>(
                    Arrays.asList("Gioca una carta", "Scarta una carta", "Leggi descrizione carta", "Termina turno"));
            if (personaggio == Personaggio.SID_KETCHUM && carteInMano.size() >= 2 && vita < vitaMax) {
                opzioniList.add("Usa abilità Sid Ketchum (Scarta 2 carte, +1 PF)");
            }
            String[] opzioni = opzioniList.toArray(new String[0]);
            if (partita.verificaVittoria() != RisultatoPartita.IN_CORSO) {
                break;
            }
            int scelta = InputUtil.showMenu(nome, "Scegli un'opzione", opzioni);
            String sceltaTesto = opzioni[scelta - 1];

            switch (sceltaTesto) {
                case "Gioca una carta":
                    if (carteInMano.isEmpty()) {
                        System.out.println("Non hai carte in mano!");
                        break;
                    }
                    int sceltaCarta = InputUtil.showCardSelection(nome, "Scelta carta da giocare (" + nome + "):",
                            carteInMano);
                    if (sceltaCarta == 0)
                        break;
                    Carta cartaScelta = carteInMano.get(sceltaCarta - 1);

                    switch (cartaScelta.getNome()) {
                        case "BANG!":
                            // FIX: limite 1 BANG! per turno (tranne se ha Volcanic)
                            if (bangUsato && !haVolcanicEquipaggiato()) {
                                System.out.println("Hai già usato un BANG! in questo turno o non hai una Volcanic!");
                                break;
                            }
                            // FIX: mostra solo i giocatori a portata
                            List<Persona> bersagliValidi = new ArrayList<>();
                            for (Persona p : partita.getListaGiocatori()) {
                                if (!p.equals(this)) {
                                    int distanza = partita.calcolaDistanza(this, p);
                                    if (distanza <= pistola.getDistanza()) {
                                        bersagliValidi.add(p);
                                    }
                                }
                            }
                            if (bersagliValidi.isEmpty()) {
                                System.out.println("Nessun giocatore a portata di tiro (distanza pistola: "
                                        + pistola.getDistanza() + ")!");
                                break;
                            }
                            System.out.println("[SKIP] Giocatori a portata:");
                            for (int i = 0; i < bersagliValidi.size(); i++) {
                                int dist = partita.calcolaDistanza(this, bersagliValidi.get(i));
                                System.out.println("[SKIP] " +
                                        (i + 1) + ". " + bersagliValidi.get(i).getNome() + " (distanza: " + dist + ")");
                            }
                            Persona bersaglio = InputUtil.scegliPersona(nome, "Scelta bersaglio per " + nome + ": ",
                                    bersagliValidi);
                            if (bersaglio == null) {
                                System.out.println("Azione annullata.");
                                break;
                            }
                            cartaScelta.usaBang(partita, this, bersaglio);
                            BangApiServer.addFormattedEvent(
                                    "[LOG] 🔫 {P:" + nome + "} spara a {P:" + bersaglio.getNome() + "} con {C:BANG!}");
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, bersaglio.getNome());
                            delay(1400); // Tempo per l'animazione
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            partita.getListaScarti().add(cartaScelta);
                            bangUsato = true;
                            // Verifica se il bersaglio è morto
                            if (bersaglio.getVita() <= 0) {
                                partita.eliminaPersona(bersaglio, this);
                            }
                            break;

                        case "Birra":
                            cartaScelta.usaBirra(partita, this);
                            BangApiServer
                                    .addFormattedEvent("[LOG] 🍺 {P:" + nome + "} beve una {C:Birra} e recupera 1 PF");
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, null);
                            delay(1400);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            partita.getListaScarti().add(cartaScelta);
                            break;

                        case "Saloon":
                            cartaScelta.usaSaloon(partita.getListaGiocatori());
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, "Tutti");
                            delay(1400);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            partita.getListaScarti().add(cartaScelta);
                            break;

                        case "Diligenza":
                            cartaScelta.usaDiligenza(partita, this);
                            BangApiServer
                                    .addFormattedEvent("[LOG] 📦 {P:" + nome + "} usa {C:Diligenza} e pesca 2 carte");
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, null);
                            delay(1400);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            partita.getListaScarti().add(cartaScelta);
                            break;

                        case "Wells Fargo":
                            cartaScelta.usaWellsFargo(partita, this);
                            BangApiServer
                                    .addFormattedEvent("[LOG] 📦 {P:" + nome + "} usa {C:Wells Fargo} e pesca 3 carte");
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, null);
                            delay(1400);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            partita.getListaScarti().add(cartaScelta);
                            break;

                        case "Panico!":
                            List<Persona> bersagliPanico = new ArrayList<>();
                            for (Persona p : partita.getListaGiocatori()) {
                                if (!p.equals(this)) {
                                    int distanza = partita.calcolaDistanza(this, p);
                                    if (distanza <= 1) {
                                        bersagliPanico.add(p);
                                    }
                                }
                            }
                            if (bersagliPanico.isEmpty()) {
                                System.out.println("Nessun giocatore adiacente (distanza 1) per usare Panico!");
                                break;
                            }
                            System.out.println("[SKIP] Giocatori adiacenti:");
                            for (int i = 0; i < bersagliPanico.size(); i++) {
                                System.out.println("[SKIP] " + (i + 1) + ". " + bersagliPanico.get(i).getNome());
                            }
                            Persona targetPanico = InputUtil.scegliPersona(nome, "Bersaglio Panico! per " + nome + ": ",
                                    bersagliPanico);
                            if (targetPanico == null) {
                                System.out.println("Azione annullata.");
                                break;
                            }
                            if (!cartaScelta.usaPanico(targetPanico, this)) {
                                break;
                            }
                            BangApiServer.addFormattedEvent(
                                    "[LOG] 🤌 {P:" + nome + "} usa {C:Panico!} su {P:" + targetPanico.getNome() + "}");
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, targetPanico.getNome());
                            delay(1400);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            partita.getListaScarti().add(cartaScelta);
                            break;

                        case "Cat Balou":
                            List<Persona> bersagliCat = new ArrayList<>();
                            for (Persona p : partita.getListaGiocatori()) {
                                if (!p.equals(this)
                                        && (!p.getCarteInMano().isEmpty() || !p.getCarteInGioco().isEmpty())) {
                                    bersagliCat.add(p);
                                }
                            }
                            if (bersagliCat.isEmpty()) {
                                System.out.println("Nessun giocatore con carte da scartare!");
                                break;
                            }
                            System.out.println("[SKIP] Scegli un giocatore:");
                            for (int i = 0; i < bersagliCat.size(); i++) {
                                System.out.println("[SKIP] " + (i + 1) + ". " + bersagliCat.get(i).getNome());
                            }
                            Persona targetCat = InputUtil.scegliPersona(nome, "Bersaglio Cat Balou per " + nome + ": ",
                                    bersagliCat);
                            if (targetCat == null) {
                                System.out.println("Azione annullata.");
                                break;
                            }
                            if (!cartaScelta.usaCatBalou(targetCat, partita.getListaScarti(), this)) {
                                break;
                            }
                            BangApiServer.addFormattedEvent("[LOG] 🧨 {P:" + nome + "} scarta una carta a {P:"
                                    + targetCat.getNome() + "} con {C:Cat Balou}");
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, targetCat.getNome());
                            delay(1400);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            partita.getListaScarti().add(cartaScelta);
                            break;

                        case "Gatling":
                            cartaScelta.usaGatling(partita, this);
                            BangApiServer
                                    .addFormattedEvent("[LOG] 🔥 {P:" + nome + "} scatena la {C:Gatling} su tutti!");
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, "Tutti");
                            delay(1400);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            partita.getListaScarti().add(cartaScelta);
                            break;

                        case "Barile":
                            if (haBarileEquipaggiato()) {
                                System.out.println("Hai già un Barile in gioco!");
                                break;
                            }
                            System.out.println("Hai equipaggiato il Barile.");
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, null);
                            delay(1400);
                            carteInGioco.add(cartaScelta);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            break;

                        case "Mirino":
                            if (haMirino()) {
                                System.out.println("Hai già un Mirino in gioco!");
                                break;
                            }
                            System.out.println("Hai equipaggiato il Mirino.");
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, null);
                            delay(1400);
                            carteInGioco.add(cartaScelta);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            break;

                        case "Mustang":
                            if (haMustang()) {
                                System.out.println("Hai già un Mustang in gioco!");
                                break;
                            }
                            System.out.println("Hai equipaggiato il Mustang.");
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, null);
                            delay(1400);
                            carteInGioco.add(cartaScelta);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            break;

                        case "Schofield":
                        case "Remington":
                        case "Rev. Carabine":
                        case "Winchester":
                        case "Volcanic": {
                            equipaggiaArma(cartaScelta, partita.getListaScarti());
                            verificaAbilitaSuzyLafayette(partita);
                            break;
                        }

                        case "Emporio":
                            cartaScelta.usaEmporio(partita, this);
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, "Tutti");
                            delay(1400);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            partita.getListaScarti().add(cartaScelta);
                            break;

                        case "Indiani!":
                            cartaScelta.usaIndiani(partita, this);
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, "Tutti");
                            delay(1400);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            partita.getListaScarti().add(cartaScelta);
                            break;

                        case "Duello":
                            List<Persona> bersagliDuello = new ArrayList<>();
                            for (Persona p : partita.getListaGiocatori()) {
                                if (!p.equals(this))
                                    bersagliDuello.add(p);
                            }
                            if (bersagliDuello.isEmpty()) {
                                System.out.println("Nessun bersaglio disponibile!");
                                break;
                            }
                            Persona targetDuello = InputUtil.scegliPersona(nome, "Sfidante scelto da " + nome + ": ",
                                    bersagliDuello);
                            if (targetDuello == null) {
                                System.out.println("Azione annullata.");
                                break;
                            }
                            cartaScelta.usaDuello(partita, this, targetDuello);
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, targetDuello.getNome());
                            delay(1400);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            partita.getListaScarti().add(cartaScelta);
                            break;

                        case "Prigione":
                            List<Persona> bersagliPrigione = new ArrayList<>();
                            for (Persona p : partita.getListaGiocatori()) {
                                if (!p.equals(this) && !p.getRuolo().equals("Sceriffo")) {
                                    // Non si può imprigionare chi è già in prigione
                                    boolean giaInPrigione = false;
                                    for (Carta c : p.getCarteInGioco()) {
                                        if (c.getNome().equals("Prigione"))
                                            giaInPrigione = true;
                                    }
                                    if (!giaInPrigione)
                                        bersagliPrigione.add(p);
                                }
                            }
                            if (bersagliPrigione.isEmpty()) {
                                System.out.println(
                                        "Nessuno da imprigionare (lo Sceriffo non può andare in Prigione, o tutti sono già incarcerati)!");
                                break;
                            }
                            Persona targetPrigione = InputUtil.scegliPersona(nome,
                                    "Bersaglio Prigione per " + nome + ": ",
                                    bersagliPrigione);
                            if (targetPrigione == null) {
                                System.out.println("Azione annullata.");
                                break;
                            }
                            cartaScelta.usaPrigione(targetPrigione);
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, targetPrigione.getNome());
                            delay(1400);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            targetPrigione.getCarteInGioco().add(cartaScelta);
                            break;

                        case "Dinamite":
                            boolean giaDinamite = false;
                            for (Carta c : this.getCarteInGioco()) {
                                if (c.getNome().equals("Dinamite"))
                                    giaDinamite = true;
                            }
                            if (giaDinamite) {
                                System.out.println("Hai già una Dinamite davanti a te!");
                                break;
                            }
                            cartaScelta.usaDinamite(this);
                            BangApiServer.setLastPlayedCard(cartaScelta.getNome(), nome, null);
                            delay(1400);
                            carteInGioco.add(cartaScelta);
                            rimuoviCartaDallaMano(cartaScelta, partita);
                            break;

                        default:
                            System.out.println("Questa carta non può essere utilizzata in questo momento.");
                    }
                    break;

                case "Scarta una carta": // Scarta una carta
                    if (carteInMano.isEmpty()) {
                        System.out.println("🚫 Non hai carte da scartare!");
                        break;
                    }
                    int sceltaCarta2 = InputUtil.showCardSelection(nome, "Scarta una carta (" + nome + "):",
                            carteInMano);
                    if (sceltaCarta2 == 0)
                        break;
                    Carta cartaScelta2 = carteInMano.get(sceltaCarta2 - 1);
                    rimuoviCartaDallaMano(cartaScelta2, partita);
                    partita.getListaScarti().add(cartaScelta2);
                    System.out.println("🗑️ Hai scartato: " + cartaScelta2.getNome());
                    break;

                case "Leggi descrizione carta": // Leggi descrizione carta
                    if (carteInMano.isEmpty()) {
                        System.out.println("Non hai carte in mano!");
                        break;
                    }
                    int sceltaDesc = InputUtil.showCardSelection(nome, "Descrizione carta per " + nome + ":",
                            carteInMano);
                    if (sceltaDesc == 0)
                        break;
                    Carta cartaDesc = carteInMano.get(sceltaDesc - 1);
                    String desc = cartaDesc.getDescrizione();
                    System.out.println("\n[ " + cartaDesc.getNome() + " ]");
                    System.out.println(desc != null && !desc.isEmpty() ? desc : "Nessuna descrizione disponibile.");
                    System.out.println();
                    break;

                case "Termina turno": // Termina turno
                    // FIX: deve scartare fino ad avere ≤ vita carte in mano
                    if (carteInMano.size() <= Math.max(0, vita)) {
                        fineTurno = true; // esce dal loop
                    } else {
                        int daScartare = carteInMano.size() - Math.max(0, vita);
                        System.out.println("Devi prima scartare " + daScartare + " carte!");
                    }
                    break;
                case "Usa abilità Sid Ketchum (Scarta 2 carte, +1 PF)":
                    System.out.println("🥙 Sid Ketchum decide di mangiare qualcosa per recuperare forze.");
                    int c1 = InputUtil.showCardSelection(nome, "Prima carta Sid Ketchum (" + nome + "):", carteInMano);
                    if (c1 == 0)
                        break;
                    Carta scartata1 = carteInMano.get(c1 - 1);
                    rimuoviCartaDallaMano(scartata1, partita); // Può attivare Suzy, ma lui è Sid, quindi no problem
                    partita.getListaScarti().add(scartata1);

                    int c2 = InputUtil.showCardSelection(nome, "Seconda carta Sid Ketchum (" + nome + "):",
                            carteInMano);
                    if (c2 == 0)
                        break;
                    Carta scartata2 = carteInMano.get(c2 - 1);
                    rimuoviCartaDallaMano(scartata2, partita);
                    partita.getListaScarti().add(scartata2);

                    setVita(getVita() + 1);
                    System.out.println(
                            "🥙 " + nome + " ha recuperato 1 PF! Vita attuale: " + getVita() + "/" + getVitaMax());
                    break;
            }
        } while (!fineTurno && partita.verificaVittoria() == RisultatoPartita.IN_CORSO);
    }

    /**
     * FIX: equipaggia un'arma scartando quella precedente (se presente).
     * Ogni giocatore può avere al massimo 1 arma equipaggiata alla volta.
     */
    protected void equipaggiaArma(Carta nuovaArma, List<Carta> scarti) {
        // Rimuovi la vecchia arma dalle carteInGioco
        Iterator<Carta> it = carteInGioco.iterator();
        while (it.hasNext()) {
            Carta c = it.next();
            if (c instanceof Pistola) {
                System.out.println("Hai sostituito l'arma " + c.getNome() + " con " + nuovaArma.getNome());
                scarti.add(c);
                it.remove();
                break;
            }
        }

        // FIX: sostituisce l'intero oggetto pistola per mantenerne l'identità (es.
        // Volcanic)
        if (nuovaArma instanceof Pistola) {
            this.pistola = (Pistola) nuovaArma;
        }
        BangApiServer.setLastPlayedCard(nuovaArma.getNome(), nome, null);
        delay(1400);
        carteInGioco.add(nuovaArma);
        carteInMano.remove(nuovaArma);
        System.out.println("Hai equipaggiato: " + nuovaArma.getNome() + " (portata: " + pistola.getDistanza() + ")");
    }

    @Override
    public String toString() {
        return nome + " [" + personaggio.getNome() + " - " + ruolo + "] — Vita: " + vita + "/" + vitaMax +
                ", Carte in mano: " + carteInMano.size() +
                ", Carte in gioco: " + carteInGioco.size() +
                ", Arma: " + pistola.getNome() + " (portata " + pistola.getDistanza() + ")";
    }

    // FIX: haMirino — cerca tra le carteInGioco per nome, non .equals su List
    public boolean haMirino() {
        for (Carta carta : carteInGioco) {
            if (carta.getNome().equals("Mirino")) {
                return true;
            }
        }
        return false;
    }

    // FIX: haMustang — cerca tra le carteInGioco per nome (typo "Mustung"
    // corretto)
    public boolean haMustang() {
        for (Carta carta : carteInGioco) {
            if (carta.getNome().equals("Mustang")) {
                return true;
            }
        }
        return false;
    }

    // Mantenuto per retrocompatibilità con Pistola (era haMustung con typo)
    public boolean haMustung() {
        return haMustang();
    }

    public boolean haVolcanicEquipaggiato() {
        return pistola.getNome().equals("Volcanic");
    }

    public boolean gestisciDinamiteEPrigione(Partita partita) {
        boolean saltaTurno = false;
        BangApiServer.clearLastCards();

        // 1. Check Dinamite
        Carta dinamite = null;
        for (Carta c : carteInGioco) {
            if (c.getNome().equals("Dinamite")) {
                dinamite = c;
                break;
            }
        }

        if (dinamite != null) {
            if (!(this instanceof bang.Bot)) {
                InputUtil.showMenu(nome, "🧨 Estrazione Dinamite per " + nome,
                        new String[] { "Estrai per la Dinamite" });
            }
            System.out.println(nome + " ha la Dinamite! Estrae...");
            BangApiServer.addEvent("[LOG] 🧨 " + nome + " sventola per la Dinamite...");
            Carta estratta = partita.pescaCarta(null);
            if (estratta != null) {
                BangApiServer.setLastDrawnCard(estratta, true, nome);
                delay(1400);
                partita.getListaScarti().add(estratta);
                System.out.println("  Estratta: " + estratta.getNome() + " [" + estratta.getValori() + " di "
                        + estratta.getSemi() + "]");

                String seme = estratta.getSemi();
                String valore = estratta.getValori();

                // Esplode se Picche dal 2 al 9
                boolean esplode = false;
                if (seme != null && seme.equalsIgnoreCase("Picche")) {
                    List<String> valoriEsplosivi = Arrays.asList("DUE", "TRE", "QUATTRO", "CINQUE", "SEI", "SETTE",
                            "OTTO",
                            "NOVE");
                    if (valoriEsplosivi.contains(valore.toUpperCase())) {
                        esplode = true;
                    }
                }

                if (esplode) {
                    System.out.println("BOOM! La Dinamite è esplosa!");
                    bang.server.BangApiServer.addEvent("🧨 BOOM! La Dinamite esplode su " + nome + "! (3 danni)");
                    carteInGioco.remove(dinamite);
                    partita.getListaScarti().add(dinamite);
                    subisceDanno(3, partita, this);
                    if (getVita() <= 0) {
                        partita.eliminaPersona(this, this);
                    }
                } else {
                    System.out.println("Fiuuu! La Dinamite non esplode e passa al giocatore successivo.");
                    carteInGioco.remove(dinamite);
                    // Passa a sinistra (giocatore successivo)
                    List<Persona> giocatori = partita.getListaGiocatori();
                    int myIdx = giocatori.indexOf(this);
                    if (myIdx >= 0) {
                        int nextIdx = (myIdx + 1) % giocatori.size();
                        Persona prossimo = giocatori.get(nextIdx);
                        prossimo.getCarteInGioco().add(dinamite);
                        BangApiServer
                                .addEvent("[LOG] 😅 La Dinamite non esplode e passa a " + prossimo.getNome() + ".");
                    }
                }
            }
        }

        if (vita <= 0)
            return true; // Se è morto con la dinamite non fa la prigione

        // 2. Check Prigione
        Carta prigione = null;
        for (Carta c : carteInGioco) {
            if (c.getNome().equals("Prigione")) {
                prigione = c;
                break;
            }
        }

        if (prigione != null) {
            if (!(this instanceof bang.Bot)) {
                InputUtil.showMenu(nome, "⛓️ Tentativo evasione per " + nome,
                        new String[] { "Estrai per la Prigione" });
            }
            System.out.println(nome + " è in Prigione! Estrae...");
            bang.server.BangApiServer.addEvent("[LOG] ⛓️ " + nome + " tenta l'evasione dalla prigione...");
            Carta estratta = partita.pescaCarta(null);
            if (estratta != null) {
                BangApiServer.setLastDrawnCard(estratta, true, nome);
                delay(1400);
                partita.getListaScarti().add(estratta);
                System.out.println("  Estratta: " + estratta.getNome() + " [" + estratta.getValori() + " di "
                        + estratta.getSemi() + "]");

                // La prigione si scarta SEMPRE dopo un turno
                carteInGioco.remove(prigione);
                partita.getListaScarti().add(prigione);

                if (estratta.getSemi() != null && estratta.getSemi().equalsIgnoreCase("Cuori")) {
                    System.out.println(nome + " è EVASO dalla prigione!");
                    bang.server.BangApiServer
                            .addEvent("🔓 " + nome + " è EVASO dalla prigione! (Carta: " + estratta.getNome() + ")");
                } else {
                    System.out.println(nome + " RESTERÀ IN GABBIA. Salta il turno.");
                    bang.server.BangApiServer.addEvent(
                            "[LOG] ⛓️ " + nome + " RESTERÀ IN GABBIA. Salta il turno. (Carta: " + estratta.getNome()
                                    + ")");
                    saltaTurno = true;
                }
            }
        }

        return saltaTurno;
    }

    public boolean haBarileEquipaggiato() {
        for (Carta carta : carteInGioco) {
            if (carta.getNome().equals("Barile")) {
                return true;
            }
        }
        return false;
    }

    public boolean haMancato() {
        for (Carta carta : carteInMano) {
            if (carta.getNome().equals("Mancato!")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rimuove una carta "Mancato!" dalla mano del giocatore e la restituisce (per
     * usarla).
     */
    public Carta usaMancatoDallaMano(Partita partita) {
        Iterator<Carta> it = carteInMano.iterator();
        while (it.hasNext()) {
            Carta c = it.next();
            if (c.getNome().equals("Mancato!")) {
                it.remove();
                verificaAbilitaSuzyLafayette(partita);
                return c;
            }
        }
        return null;
    }

    public boolean haBirra() {
        for (Carta carta : carteInMano) {
            if (carta.getNome().equals("Birra")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rimuove una carta "Birra" dalla mano del giocatore e la restituisce (come
     * salvavita fuori turno).
     */
    public Carta usaBirraDallaMano(Partita partita) {
        Iterator<Carta> it = carteInMano.iterator();
        while (it.hasNext()) {
            Carta c = it.next();
            if (c.getNome().equals("Birra")) {
                it.remove();
                // Anche bere una Birra fuori turno potrebbe attivare Suzy Lafayette se era
                // l'ultima carta!
                verificaAbilitaSuzyLafayette(partita);
                return c;
            }
        }
        return null;
    }

    public List<Carta> getCarteInGioco() {
        return carteInGioco;
    }

    public Carta rubaCarta(Persona bersaglio, Persona giocatore) {
        // Chiedi se rubare dalla mano o dal tavolo
        boolean haCarteMano = !bersaglio.carteInMano.isEmpty();
        boolean haCarteTavolo = !bersaglio.carteInGioco.isEmpty();

        if (!haCarteMano && !haCarteTavolo) {
            System.out.println(bersaglio.getNome() + " non ha carte da rubare.");
            return null;
        }

        if (haCarteMano && haCarteTavolo) {
            boolean manoOTavolo = InputUtil.readYesOrNo(giocatore.getNome(),
                    "Ruba dalla mano? (" + giocatore.getNome() + ")");
            if (manoOTavolo) {
                return rubaCartaDaMano(bersaglio, giocatore);
            } else {
                return rubaCartaDaTavolo(bersaglio, giocatore);
            }
        } else if (haCarteMano) {
            return rubaCartaDaMano(bersaglio, giocatore);
        } else {
            return rubaCartaDaTavolo(bersaglio, giocatore);
        }
    }

    private Carta rubaCartaDaMano(Persona bersaglio, Persona giocatore) {
        int scelta = InputUtil.showCardSelection(giocatore.getNome(),
                "Ruba dalla mano di " + bersaglio.getNome() + " (" + giocatore.getNome() + "):",
                bersaglio.getCarteInMano());
        if (scelta == 0)
            return null;
        Carta cartaRubata = bersaglio.getCarteInMano().remove(scelta - 1);
        giocatore.getCarteInMano().add(cartaRubata);
        System.out.println("Hai rubato " + cartaRubata.getNome() + " dalla mano di " + bersaglio.getNome());

        return cartaRubata;
    }

    private Carta rubaCartaDaTavolo(Persona bersaglio, Persona giocatore) {
        int scelta = InputUtil.showCardSelection(giocatore.getNome(),
                "Ruba dal tavolo di " + bersaglio.getNome() + " (" + giocatore.getNome() + "):",
                bersaglio.getCarteInGioco());
        if (scelta == 0)
            return null;

        Carta cartaRubata = bersaglio.getCarteInGioco().get(scelta - 1);
        bersaglio.scartaCartaDaGioco(cartaRubata, null); // Usa il metodo sicuro

        giocatore.getCarteInMano().add(cartaRubata);
        System.out.println("Hai rubato " + cartaRubata.getNome() + " dal tavolo di " + bersaglio.getNome());

        return cartaRubata;
    }

    public int getVita() {
        return vita;
    }

    public void setVita(int vita) {
        this.vita = vita;
    }

    public void subisceDanno(int danno, Partita partita, Persona attaccante) {
        BangApiServer.addFormattedEvent("💥 {P:" + nome + "} subisce " + danno + " danno/i da {P:"
                + (attaccante != null ? attaccante.getNome() : "Ignoto") + "}");
        setVita(getVita() - danno);
        System.out
                .println("💥 " + getNome() + " ha subito " + danno + " PF di danno! Vita: " + getVita() + "/"
                        + getVitaMax());

        // FIX: Regola salvavita della Birra. Se la vita scende a 0 o meno
        // e si ha una Birra in mano, viene chiesta conferma (per l'umano) per
        // sopravvivere.
        // NON VALE SE SONO RIMASTI SOLO 2 GIOCATORI.
        while (getVita() <= 0 && haBirra() && partita.getListaGiocatori().size() > 2) {
            boolean vuoiUsareBirra = false;
            if (this instanceof bang.Bot) {
                vuoiUsareBirra = true;
            } else {
                vuoiUsareBirra = InputUtil
                        .readYesOrNo(nome,
                                "💔 " + nome + " a " + getVita() + " PF. Birra salvavita?");
            }

            if (vuoiUsareBirra) {
                Carta birra = usaBirraDallaMano(partita);
                if (birra != null) {
                    setVita(getVita() + 1);
                    partita.getListaScarti().add(birra);
                    BangApiServer.addFormattedEvent("🍺 {P:" + nome + "} si salva con una {C:Birra}!");
                    System.out.println("🍺 " + getNome()
                            + " ha bevuto una Birra in fin di vita ed è sopravvissuto! (Vita attuale: " + getVita()
                            + ")");
                }
            } else {
                break; // Ha scelto di non usare la birra
            }
        }

        if (getVita() <= 0) {
            return; // Eliminato definitivamente, nessuna abilità viene attivata
        }

        if (personaggio == Personaggio.BART_CASSIDY) {
            System.out.println(
                    "👂 " + getNome() + " (Bart Cassidy) ha perso " + danno + " PF, pesca " + danno + " carte!");
            for (int i = 0; i < danno; i++) {
                Carta pescata = partita.pescaCarta(this);
                if (pescata != null) {
                    System.out.println("  🃏 Ha pescato una carta.");
                    if (!(this instanceof Bot)) {
                        bang.server.BangApiServer.setLastDrawnCard(pescata, false, nome);
                        delay(1400);
                    }
                }
            }
        }

        if (personaggio == Personaggio.EL_GRINGO && attaccante != null) {
            System.out.println(
                    "🧤 " + getNome() + " (El Gringo) ruba " + danno + " carte all'attaccante " + attaccante.getNome()
                            + "!");
            for (int i = 0; i < danno; i++) {
                if (!attaccante.getCarteInMano().isEmpty()) {
                    int max = attaccante.getCarteInMano().size();
                    int index = (int) (Math.random() * max);
                    Carta rubata = attaccante.getCarteInMano().remove(index);
                    attaccante.verificaAbilitaSuzyLafayette(partita);
                    this.getCarteInMano().add(rubata);
                    System.out.println("  🧤 Ha rubato " + rubata.getNome() + " dalla mano di " + attaccante.getNome());
                } else {
                    System.out
                            .println("🚫 " + attaccante.getNome() + " non ha più carte in mano! El Gringo non pesca.");
                    break;
                }
            }
        }
    }

    public void rimuoviCartaDallaMano(Carta carta, Partita partita) {
        carteInMano.remove(carta);
        verificaAbilitaSuzyLafayette(partita);
    }

    /**
     * Rimuove una carta dalle carte in gioco (es. Cat Balou o Panico).
     * Se la carta è un'arma, resetta la pistola alla Colt .45 base.
     */
    public void scartaCartaDaGioco(Carta carta, List<Carta> scarti) {
        if (carteInGioco.remove(carta)) {
            if (carta instanceof Pistola) {
                System.out.println(nome + " ha perso l'arma " + carta.getNome() + "! Torna alla Colt .45.");
                this.pistola = new Pistola("Colt .45", "Arma di base, portata 1.", true, null, null, 1);
            }
            if (scarti != null) {
                scarti.add(carta);
            }
        }
    }

    public void verificaAbilitaSuzyLafayette(Partita partita) {
        if (personaggio == Personaggio.SUZY_LAFAYETTE && carteInMano.isEmpty()) {
            System.out
                    .println(
                            "💨 " + nome
                                    + " (Suzy Lafayette) è rimasta senza carte! Pesca immediatamente 1 carta dal mazzo.");
            Carta pescata = partita.pescaCarta(this);
            if (pescata != null) {
                System.out.println("  🃏 Ha pescato una carta dal mazzo.");
                if (!(this instanceof Bot)) {
                    bang.server.BangApiServer.setLastDrawnCard(pescata, false, nome);
                    delay(1400);
                }
            }
        }
    }

    public int getVitaMax() {
        return vitaMax;
    }

    public List<Carta> getCarteInMano() {
        return carteInMano;
    }

    public Pistola getPistola() {
        return pistola;
    }

    public Personaggio getPersonaggio() {
        return personaggio;
    }

    public boolean isNome(String nome) {
        return this.nome.equals(nome);
    }
}
