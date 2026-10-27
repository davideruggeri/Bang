package bang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import bang.server.BangApiServer;
import java.security.SecureRandom;

public class Partita {
    private List<Persona> listaGiocatori;
    private List<Persona> listaGiocatoriEliminati;
    private List<Carta> listaMazzo;
    private List<Carta> listaScarti;
    private int giocatoreCorrente;
    private int numGiocatori;
    // VARIABILI MODULO 3 (Classifica)
    private Persona rinnegato;
    private boolean rinnegatoMortoPerUltimoDalloSceriffo;

    public Partita(Mazzo mazzo) {
        this.listaGiocatori = new ArrayList<>();
        this.listaGiocatoriEliminati = new ArrayList<>();
        this.listaMazzo = mazzo.creaMazzo();
        this.listaScarti = new ArrayList<>(); // FIX: inizializzata qui, non null
        this.giocatoreCorrente = 0;
        this.rinnegatoMortoPerUltimoDalloSceriffo = false;
    }

    public List<Carta> getListaMazzo() {
        return listaMazzo;
    }

    public int getGiocatoreCorrente() {
        return giocatoreCorrente;
    }

    public void regole() {
        System.out.println(
                "Sceriffo: il suo compito è di eliminare tutti i Fuorilegge ed il Rinnegato, riportando così l'ordine in città.");
        System.out.println(
                "Fuorilegge: vogliono eliminare lo Sceriffo, ma non hanno scrupoli ad eliminarsi l'un l'altro per incassare le taglie sulle loro teste!");
        System.out.println(
                "Rinnegato: vuole diventare il nuovo Sceriffo; il suo compito è di rimanere l'ultimo personaggio in gioco.");
        System.out.println(
                "Vice: aiutano e proteggono lo Sceriffo, e perseguono i suoi stessi obiettivi, anche a costo della loro vita!");
    }

    public List<Carta> getListaScarti() {
        return listaScarti;
    }

    public void setListaScarti(List<Carta> listaScarti) {
        this.listaScarti = listaScarti;
    }

    public Persona getRinnegato() {
        return rinnegato;
    }

    public boolean isRinnegatoMortoPerUltimoDalloSceriffo() {
        return rinnegatoMortoPerUltimoDalloSceriffo;
    }

    public void setNumGiocatori(int n) {
        this.numGiocatori = n;
    }

    public int getNumGiocatori() {
        return numGiocatori;
    }

    public void inizializzaGiocatori(List<String> nomiGiocatori, List<Boolean> isBotList) {
        this.numGiocatori = nomiGiocatori.size();
        List<String> ruoli = new ArrayList<>();

        ruoli.add("Sceriffo");
        ruoli.add("Fuorilegge");
        ruoli.add("Fuorilegge");
        ruoli.add("Rinnegato");

        if (numGiocatori >= 5) {
            ruoli.add("Vice");
        }
        if (numGiocatori >= 6) {
            ruoli.add("Fuorilegge");
        }
        if (numGiocatori == 7) {
            ruoli.add("Vice");
        }

        // Rimescoliamo in modo completamente casuale tutti i ruoli, Sceriffo incluso
        SecureRandom sr = new SecureRandom();
        Collections.shuffle(ruoli, sr);

        // Preparazione Personaggi
        List<Personaggio> personaggiBase = new ArrayList<>(Arrays.asList(Personaggio.values()));
        Collections.shuffle(personaggiBase, sr);

        for (int i = 0; i < ruoli.size(); i++) {
            String ruolo = ruoli.get(i);
            Personaggio personaggio = personaggiBase.get(i);
            String nomeGiocatore = nomiGiocatori.get(i);
            boolean isBot = isBotList.get(i);

            Persona p;
            if (isBot) {
                p = new Bot(nomeGiocatore, ruolo, personaggio);
            } else {
                p = new Persona(nomeGiocatore, ruolo, personaggio);
            }

            listaGiocatori.add(p);

            // Salviamo il riferimento al rinnegato per il Modulo 3
            if (ruolo.equals("Rinnegato")) {
                this.rinnegato = p;
            }

            System.out.println("[SKIP] ----------------------------------------------");
            System.out.println("[SKIP] Sei: " + p.getNome() + " con il Ruolo: " + p.getRuolo());
            System.out.println("[SKIP] Personaggio assegnato: " + p.getPersonaggio().getNome());
            System.out.println("[SKIP] Abilità: " + p.getPersonaggio().getDescrizione());
            System.out.println("[SKIP] Punti Vita: " + p.getVita() + "/" + p.getVitaMax());
            System.out.println("[SKIP] ----------------------------------------------\n");

            // FIX: mostriamo il ruolo solo allo Sceriffo (rivela solo lui pubblicamente)
            if (ruolo.equals("Sceriffo")) {
                System.out.println("--- " + p.getNome() + " è lo SCERIFFO! (Personaggio: "
                        + personaggio.getNome() + ")");
            } else {
                System.out.println("--- " + p.getNome() + " ha ricevuto il suo ruolo segreto. (Personaggio: "
                        + personaggio.getNome() + ")");
            }
        }

        // Regola: lo Sceriffo gioca sempre per primo (senso orario)
        // Usiamo Collections.rotate per mantenere intatto l'ordine circolare
        // che fa sedere "Bot 1" a sinistra di "Giocatore", "Bot 2" a seguire, ecc.
        int indiceSceriffo = 0;
        for (int i = 0; i < listaGiocatori.size(); i++) {
            if (listaGiocatori.get(i).getRuolo().equals("Sceriffo")) {
                indiceSceriffo = i;
                break;
            }
        }
        if (indiceSceriffo != 0) {
            Collections.rotate(listaGiocatori, -indiceSceriffo);
        }
        this.giocatoreCorrente = 0;
        System.out.println("--- Ordine di gioco (senso orario, prima lo Sceriffo):");
        for (int i = 0; i < listaGiocatori.size(); i++) {
            System.out.println("---   " + (i + 1) + ". " + listaGiocatori.get(i).getNome()
                    + " [" + listaGiocatori.get(i).getRuolo() + "]");
        }
    }

    public void inizioPartita() {
        for (Persona giocatore : listaGiocatori) {
            for (int i = 0; i < giocatore.getVita(); i++) {
                pescaCarta(giocatore);
            }
        }
    }

    public void cambioTurno() {
        giocatoreCorrente = (giocatoreCorrente + 1) % listaGiocatori.size();
    }

    /**
     * FIX: verifica la vittoria secondo le regole ufficiali di Bang!
     * Modulo 3: Ritorna enum RisultatoPartita.
     */
    public RisultatoPartita verificaVittoria() {
        boolean sceriffoVivo = false;
        boolean fuorileggeVivo = false;
        boolean rinnegatoVivo = false;
        int giocatoriVivi = listaGiocatori.size();

        for (Persona g : listaGiocatori) {
            switch (g.getRuolo()) {
                case "Sceriffo":
                    sceriffoVivo = true;
                    break;
                case "Fuorilegge":
                    fuorileggeVivo = true;
                    break;
                case "Rinnegato":
                    rinnegatoVivo = true;
                    break;
            }
        }

        if (!sceriffoVivo) {
            // Sceriffo eliminato
            if (rinnegatoVivo && giocatoriVivi == 1) {
                System.out.println("🦅 IL RINNEGATO HA VINTO LA PARTITA! L'unico sopravvissuto.");
                return RisultatoPartita.RINNEGATO;
            } else {
                System.out.println("🐺 I FUORILEGGE HANNO VINTO LA PARTITA! Lo Sceriffo è caduto.");
                return RisultatoPartita.FUORILEGGE;
            }
        }

        if (!fuorileggeVivo && !rinnegatoVivo) {
            System.out.println("👮 LA LEGGE HA VINTO LA PARTITA! Lo Sceriffo e i Vice hanno ripulito la città.");
            return RisultatoPartita.LEGGE;
        }

        return RisultatoPartita.IN_CORSO;
    }

    /**
     * Elimina una persona dalla partita:
     * - Se lo Sceriffo elimina un Vice → perde tutte le carte (penalità)
     * - Se qualcuno elimina un Fuorilegge → chi ha sparato prende 3 carte
     * ricompensa
     */
    public void eliminaPersona(Persona eliminato, Persona eliminatore) {
        if (!listaGiocatori.contains(eliminato)) {
            return; // Già eliminato in questo turno (es. effetto a catena o Gatling)
        }
        BangApiServer.addFormattedEvent(
                "💀 {P:" + eliminato.getNome() + "} (" + eliminato.getRuolo() + ") è stato eliminato!");
        System.out.println(
                "💀 " + eliminato.getNome() + " [" + eliminato.getRuolo() + "] è stato spazzato via dalla partita!");

        // Penalità: Sceriffo elimina un Vice → perde tutte le carte
        if (eliminatore != null && eliminatore.getRuolo().equals("Sceriffo") && eliminato.getRuolo().equals("Vice")) {
            System.out.println("😱 Lo Sceriffo ha eliminato un Vice! Perde tutti i suoi averi per il rimorso...");
            eliminatore.getCarteInMano().clear();
            eliminatore.getCarteInGioco().clear();
        }

        // Ricompensa: chiunque elimina un Fuorilegge pesca 3 carte
        if (eliminatore != null && eliminato.getRuolo().equals("Fuorilegge")) {
            System.out.println(
                    "💰 " + eliminatore.getNome() + " ha eliminato un Fuorilegge e riscuote la taglia: 3 carte!");
            bang.server.BangApiServer.addEvent("💰 " + eliminatore.getNome() + " riscuote la taglia (3 carte)!");
            for (int i = 0; i < 3; i++) {
                if (!listaMazzo.isEmpty()) {
                    Carta pescata = pescaCarta(eliminatore);
                    bang.server.BangApiServer.setLastDrawnCard(pescata, false, eliminatore.getNome());
                    Persona.delay(1500);
                }
            }
        }

        // Modulo 3: Tracking se il rinnegato muore per ultimo (giocatoriVivi = 2) ed �
        // ucciso dallo Sceriffo
        if (eliminato.getRuolo().equals("Rinnegato")) {
            if (listaGiocatori.size() == 2 && eliminatore != null && eliminatore.getRuolo().equals("Sceriffo")) {
                this.rinnegatoMortoPerUltimoDalloSceriffo = true;
            }
        }

        // Le carte dell'eliminato finiscono negli scarti
        listaScarti.addAll(eliminato.getCarteInMano());
        listaScarti.addAll(eliminato.getCarteInGioco());
        eliminato.getCarteInMano().clear();
        eliminato.getCarteInGioco().clear();

        listaGiocatoriEliminati.add(eliminato);
        listaGiocatori.remove(eliminato);
    }

    // Overload senza eliminatore (per retrocompatibilità)
    public void eliminaPersona(Persona eliminato) {
        eliminaPersona(eliminato, null);
    }

    public List<Persona> getListaGiocatori() {
        return listaGiocatori;
    }

    public boolean controllaVita(Persona persona) {
        return persona.getVita() > 0;
    }

    /**
     * FIX: rimescola gli scarti quando il mazzo è vuoto, invece di lanciare
     * eccezione.
     * Supporta persona == null (estrazione "a vuoto" come per il Barile).
     */
    public Carta pescaCarta(Persona persona) {
        if (listaMazzo.isEmpty()) {
            if (listaScarti.isEmpty()) {
                System.out.println("Mazzo e scarti vuoti: nessuna carta da pescare.");
                return null;
            }
            System.out.println("Mazzo esaurito! Rimescolo gli scarti...");
            listaMazzo.addAll(listaScarti);
            listaScarti.clear();
            Collections.shuffle(listaMazzo);
        }
        Carta cartaPescata = listaMazzo.remove(0);
        if (persona != null) {
            persona.getCarteInMano().add(cartaPescata);
        }
        return cartaPescata;
    }

    /**
     * Restituisce l'indice circolare di un giocatore nella lista,
     * utile per calcolare la distanza tra due giocatori.
     */
    public int getIndice(Persona persona) {
        return listaGiocatori.indexOf(persona);
    }

    /**
     * Calcola la distanza minima (circolare) tra due giocatori nella lista,
     * tenendo conto di Mirino (riduciDistanza) e Mustang (aumentaDistanza).
     */
    public int calcolaDistanza(Persona attaccante, Persona bersaglio) {
        int n = listaGiocatori.size();
        int idx1 = getIndice(attaccante);
        int idx2 = getIndice(bersaglio);
        if (idx1 < 0 || idx2 < 0)
            return Integer.MAX_VALUE;

        int diff = Math.abs(idx1 - idx2);
        int distanzaBase = Math.min(diff, n - diff);

        // Mirino o abilità Rose Doolan: l'attaccante vede il bersaglio a -1
        if (attaccante.haMirino() || attaccante.getPersonaggio() == Personaggio.ROSE_DOOLAN) {
            distanzaBase--;
        }
        // Mustang o abilità Paul Regret: il bersaglio appare a +1 agli avversari
        if (bersaglio.haMustang() || bersaglio.getPersonaggio() == Personaggio.PAUL_REGRET) {
            distanzaBase++;
        }

        return Math.max(1, distanzaBase);
    }

    public List<Persona> getListaGiocatoriEliminati() {
        return listaGiocatoriEliminati;
    }
}
