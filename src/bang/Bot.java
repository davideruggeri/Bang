package bang;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Bot extends Persona {

    private static final Random random = new Random();

    public Bot(String nome, String ruolo, Personaggio personaggio) {
        super(nome, ruolo, personaggio);
    }

    @Override
    public void turnoGiocatore(Partita partita) {
        System.out.println("\n--- Turno di " + getNome() + " (BOT) [" + getRuolo() + "] ---");
        System.out.println("[SKIP] 🤠 Turno di " + getNome() + " [" + getRuolo() + "]");
        pausa();

        // Fase 0: Controlli Equipaggiamenti a tempo (Dinamite, Prigione)
        boolean saltaTurno = gestisciDinamiteEPrigione(partita);
        if (saltaTurno) {
            System.out.println(getNome() + " salta il turno a causa della Prigione (o morte da Dinamite)!");
            return;
        }

        // Fase 1: Pesca
        for (int i = 0; i < 2; i++) {
            Carta pescata = partita.pescaCarta(this);
            if (pescata != null) {
                System.out.println(getNome() + " ha pescato una carta.");
            }
        }
        System.out.println("📦 " + getNome() + " pesca 2 carte");
        pausa();

        // Eliminazione eventuali morti
        partita.getListaGiocatori().removeIf(p -> p.getVita() <= 0);

        // Fase 2: Gioca carte
        boolean fineTurno = false;
        boolean bangUsato = false;

        int iterazioniMassime = 15;

        while (!fineTurno && iterazioniMassime > 0) {
            iterazioniMassime--;
            Carta cartaDaGiocare = scegliCartaDaGiocare(partita, bangUsato);

            if (cartaDaGiocare == null) {
                fineTurno = true;
                break;
            }

            switch (cartaDaGiocare.getNome()) {
                case "BANG!":
                    Persona bersaglio = scegliBersaglio(partita);
                    if (bersaglio != null) {
                        bang.server.BangApiServer.addFormattedEvent(
                                "[LOG] 🔫 {P:" + getNome() + "} spara BANG! a {P:" + bersaglio.getNome() + "}");
                        bang.server.BangApiServer.setLastPlayedCard(cartaDaGiocare.getNome(), getNome(),
                                bersaglio.getNome());
                        pausa();
                        cartaDaGiocare.usaBang(partita, this, bersaglio);
                        rimuoviCartaDaManoBot(cartaDaGiocare, partita);
                        bangUsato = true;
                    }
                    break;
                case "Birra":
                    bang.server.BangApiServer
                            .addFormattedEvent("[LOG] 🍺 {P:" + getNome() + "} beve una {C:Birra} (+1 vita)");
                    bang.server.BangApiServer.setLastPlayedCard(cartaDaGiocare.getNome(), getNome(), null);
                    pausa();
                    cartaDaGiocare.usaBirra(partita, this);
                    rimuoviCartaDaManoBot(cartaDaGiocare, partita);
                    break;
                case "Saloon":
                    bang.server.BangApiServer
                            .addFormattedEvent("[LOG] 🏠 {P:" + getNome() + "} gioca {C:Saloon} (tutti +1 vita)");
                    bang.server.BangApiServer.setLastPlayedCard(cartaDaGiocare.getNome(), getNome(), "Tutti");
                    pausa();
                    cartaDaGiocare.usaSaloon(partita.getListaGiocatori());
                    rimuoviCartaDaManoBot(cartaDaGiocare, partita);
                    break;
                case "Diligenza":
                    bang.server.BangApiServer
                            .addFormattedEvent("[LOG] 🚌 {P:" + getNome() + "} gioca {C:Diligenza} (+2 carte)");
                    bang.server.BangApiServer.setLastPlayedCard(cartaDaGiocare.getNome(), getNome(), null);
                    pausa();
                    cartaDaGiocare.usaDiligenza(partita, this);
                    rimuoviCartaDaManoBot(cartaDaGiocare, partita);
                    break;
                case "Wells Fargo":
                    bang.server.BangApiServer
                            .addFormattedEvent("[LOG] 🏦 {P:" + getNome() + "} gioca {C:Wells Fargo} (+3 carte)");
                    bang.server.BangApiServer.setLastPlayedCard(cartaDaGiocare.getNome(), getNome(), null);
                    pausa();
                    cartaDaGiocare.usaWellsFargo(partita, this);
                    rimuoviCartaDaManoBot(cartaDaGiocare, partita);
                    break;
                case "Panico!":
                    Persona bersaglioPanico = scegliBersaglioPerDistanza(partita, 1);
                    if (bersaglioPanico != null) {
                        bang.server.BangApiServer
                                .addEvent("[LOG] 😱 " + getNome() + " usa Panico! su " + bersaglioPanico.getNome());
                        bang.server.BangApiServer.setLastPlayedCard(cartaDaGiocare.getNome(), getNome(),
                                bersaglioPanico.getNome());
                        pausa();
                        cartaDaGiocare.usaPanico(bersaglioPanico, this);
                        rimuoviCartaDaManoBot(cartaDaGiocare, partita);
                    } else {
                        getCarteInMano().remove(cartaDaGiocare);
                        getCarteInMano().add(cartaDaGiocare);
                    }
                    break;
                case "Cat Balou":
                    Persona bersaglioCat = scegliBersaglioConCarte(partita);
                    if (bersaglioCat != null) {
                        bang.server.BangApiServer
                                .addEvent("[LOG] 🐱 " + getNome() + " usa Cat Balou su " + bersaglioCat.getNome());
                        bang.server.BangApiServer.setLastPlayedCard(cartaDaGiocare.getNome(), getNome(),
                                bersaglioCat.getNome());
                        pausa();
                        scartaCartaRandom(bersaglioCat, partita.getListaScarti());
                        rimuoviCartaDaManoBot(cartaDaGiocare, partita);
                    } else {
                        getCarteInMano().remove(cartaDaGiocare);
                        getCarteInMano().add(cartaDaGiocare);
                    }
                    break;
                case "Gatling":
                    bang.server.BangApiServer
                            .addFormattedEvent("[LOG] 💥 {P:" + getNome() + "} usa il {C:Gatling}! (tutti -1 vita)");
                    bang.server.BangApiServer.setLastPlayedCard(cartaDaGiocare.getNome(), getNome(), "Tutti");
                    pausa();
                    cartaDaGiocare.usaGatling(partita, this);
                    rimuoviCartaDaManoBot(cartaDaGiocare, partita);
                    partita.getListaGiocatori().removeIf(p -> p.getVita() <= 0);
                    break;
                case "Barile":
                    if (haBarileEquipaggiato())
                        break;
                    // ... resto del codice ...
                case "Mirino":
                    if (haMirino())
                        break;
                    // ... resto del codice ...
                case "Mustang":
                    if (haMustang())
                        break;
                    bang.server.BangApiServer
                            .addEvent("[LOG] ⚙️ " + getNome() + " equipaggia " + cartaDaGiocare.getNome());
                    bang.server.BangApiServer.setLastPlayedCard(cartaDaGiocare.getNome(), getNome(), null);
                    pausa();
                    getCarteInGioco().add(cartaDaGiocare);
                    rimuoviCartaDallaMano(cartaDaGiocare, partita);
                    break;
                case "Schofield":
                case "Remington":
                case "Rev. Carabine":
                case "Winchester":
                case "Volcanic":
                    System.out.println(getNome() + " prova a equipaggiare l'arma " + cartaDaGiocare.getNome());
                    equipaggiaArma(cartaDaGiocare, partita.getListaScarti());
                    break;
                case "Duello":
                    Persona bersaglioDuello = scegliBersaglioDuello(partita);
                    if (bersaglioDuello != null) {
                        bang.server.BangApiServer.addFormattedEvent(
                                "[LOG] ⚔️ {P:" + getNome() + "} sfida a Duello {P:" + bersaglioDuello.getNome() + "}");
                        bang.server.BangApiServer.setLastPlayedCard("Duello", getNome(), bersaglioDuello.getNome());
                        pausa();
                        cartaDaGiocare.usaDuello(partita, this, bersaglioDuello);
                        rimuoviCartaDaManoBot(cartaDaGiocare, partita);
                    }
                    break;
                case "Indiani!":
                    bang.server.BangApiServer.addFormattedEvent("[LOG] 🏹 {P:" + getNome() + "} usa {C:Indiani!}");
                    bang.server.BangApiServer.setLastPlayedCard("Indiani!", getNome(), "Tutti");
                    pausa();
                    cartaDaGiocare.usaIndiani(partita, this);
                    rimuoviCartaDaManoBot(cartaDaGiocare, partita);
                    break;
                case "Emporio":
                    bang.server.BangApiServer.addFormattedEvent("[LOG] 🛍️ {P:" + getNome() + "} usa {C:Emporio}");
                    bang.server.BangApiServer.setLastPlayedCard("Emporio", getNome(), "Tutti");
                    pausa();
                    cartaDaGiocare.usaEmporio(partita, this);
                    rimuoviCartaDaManoBot(cartaDaGiocare, partita);
                    break;
                case "Prigione":
                    Persona bersaglioPrigione = scegliBersaglioPrigione(partita);
                    if (bersaglioPrigione != null) {
                        bang.server.BangApiServer.addFormattedEvent("[LOG] ⛓️ {P:" + getNome()
                                + "} mette in Prigione {P:" + bersaglioPrigione.getNome() + "}");
                        bang.server.BangApiServer.setLastPlayedCard("Prigione", getNome(), bersaglioPrigione.getNome());
                        pausa();
                        cartaDaGiocare.usaPrigione(bersaglioPrigione);
                        rimuoviCartaDallaMano(cartaDaGiocare, partita);
                        bersaglioPrigione.getCarteInGioco().add(cartaDaGiocare);
                    }
                    break;
                case "Dinamite":
                    bang.server.BangApiServer
                            .addFormattedEvent("[LOG] 🧨 {P:" + getNome() + "} lancia la {C:Dinamite}!");
                    bang.server.BangApiServer.setLastPlayedCard("Dinamite", getNome(), null);
                    pausa();
                    cartaDaGiocare.usaDinamite(this);
                    rimuoviCartaDallaMano(cartaDaGiocare, partita);
                    getCarteInGioco().add(cartaDaGiocare);
                    break;
                default:
                    getCarteInMano().remove(cartaDaGiocare);
                    getCarteInMano().add(cartaDaGiocare);
            }
        }

        // Fase 3: Scarto fine turno
        int maxCarteMano = Math.max(0, getVita());
        while (getCarteInMano().size() > maxCarteMano) {
            Carta scartata = getCarteInMano().remove(0);
            partita.getListaScarti().add(scartata);
            System.out.println(getNome() + " scarta " + scartata.getNome() + " per fine turno.");
        }
        if (getCarteInMano().size() < maxCarteMano || getCarteInMano().isEmpty()) {
            // nessun evento necessario per il fine turno ordinario
        }
        System.out.println("✅ " + getNome() + " termina il turno");

        if (getPersonaggio() == Personaggio.SUZY_LAFAYETTE && getCarteInMano().isEmpty()) {
            verificaAbilitaSuzyLafayette(partita);
        }
    }

    /** Pausa leggibile per il frontend (1.2 secondi). */
    private void pausa() {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void rimuoviCartaDaManoBot(Carta carta, Partita partita) {
        rimuoviCartaDallaMano(carta, partita);
        partita.getListaScarti().add(carta);
    }

    private Carta scegliCartaDaGiocare(Partita partita, boolean bangUsato) {
        for (Carta c : getCarteInMano()) {
            if (c.getNome().equals("BANG!") && !bangUsato && scegliBersaglio(partita) != null) {
                return c;
            }
            if (c.getNome().equals("Birra") && getVita() < getVitaMax()
                    && partita.getListaGiocatori().size() > 2) {
                return c;
            }
            if (c.getNome().equals("Saloon") && getVita() < getVitaMax()) {
                return c;
            }
            if (c.getNome().equals("Diligenza") || c.getNome().equals("Wells Fargo") || c.getNome().equals("Gatling")) {
                return c;
            }
            if (c.getNome().equals("Barile") && !haBarileEquipaggiato()) {
                return c;
            }
            if (c.getNome().equals("Mustang") && !haMustang()) {
                return c;
            }
            if (c.getNome().equals("Mirino") && !haMirino()) {
                return c;
            }
            if (c.getNome().equals("Schofield") || c.getNome().equals("Remington")
                    || c.getNome().equals("Rev. Carabine") || c.getNome().equals("Winchester")) {
                return c;
            }
            if (c.getNome().equals("Panico!") && scegliBersaglioPerDistanza(partita, 1) != null) {
                return c;
            }
            if (c.getNome().equals("Cat Balou") && scegliBersaglioConCarte(partita) != null) {
                return c;
            }
            if (c.getNome().equals("Duello") && scegliBersaglioDuello(partita) != null) {
                return c;
            }
            if (c.getNome().equals("Indiani!") || c.getNome().equals("Emporio")) {
                return c;
            }
            if (c.getNome().equals("Prigione") && scegliBersaglioPrigione(partita) != null) {
                return c;
            }
            if (c.getNome().equals("Dinamite") && !haDinamite()) {
                return c;
            }
        }
        return null;
    }

    private boolean haDinamite() {
        for (Carta c : getCarteInGioco()) {
            if (c.getNome().equals("Dinamite"))
                return true;
        }
        return false;
    }

    private double rateBersaglio(Partita partita, Persona p) {
        String myRole = getRuolo();
        String targetRole = p.getRuolo();
        int numPlayers = partita.getListaGiocatori().size();

        if (myRole.equals("Sceriffo")) {
            if (targetRole.equals("Vice"))
                return -50.0;
            if (targetRole.equals("Fuorilegge"))
                return 100.0;
            if (targetRole.equals("Rinnegato"))
                return 20.0;
        } else if (myRole.equals("Vice")) {
            if (targetRole.equals("Sceriffo"))
                return -1000.0;
            if (targetRole.equals("Vice"))
                return -50.0;
            if (targetRole.equals("Fuorilegge"))
                return 100.0;
            if (targetRole.equals("Rinnegato"))
                return 50.0;
        } else if (myRole.equals("Fuorilegge")) {
            if (targetRole.equals("Sceriffo"))
                return 200.0;
            // Evita di sparare ai compagni Fuorilegge
            if (targetRole.equals("Fuorilegge"))
                return -100.0;
            if (targetRole.equals("Vice"))
                return 40.0;
            if (targetRole.equals("Rinnegato"))
                return 20.0;
        } else if (myRole.equals("Rinnegato")) {
            if (targetRole.equals("Sceriffo")) {
                // Il rinnegato vuole lo sceriffo vivo fino alla fine
                return (numPlayers > 2) ? -100.0 : 1000.0;
            }
            return 50.0; // Vuole tutti gli altri morti
        }
        return 0.0;
    }

    private Persona scegliIlMigliore(Partita partita, List<Persona> bersagliValidi) {
        if (bersagliValidi.isEmpty())
            return null;
        Persona migliore = null;
        double maxScore = -99999.0;
        for (Persona p : bersagliValidi) {
            double score = rateBersaglio(partita, p);
            if (score > maxScore) {
                maxScore = score;
                migliore = p;
            }
        }
        // Se il migliore ha uno score pessimo, forse meglio non sparare?
        // Ma per il bot BANG! è quasi sempre meglio sparare se non sei un alleato.
        if (maxScore < -80.0)
            return null;
        return migliore;
    }

    private Persona scegliBersaglioDuello(Partita partita) {
        List<Persona> bersagliValidi = new ArrayList<>();
        for (Persona p : partita.getListaGiocatori()) {
            if (!p.equals(this)) {
                bersagliValidi.add(p);
            }
        }
        return scegliIlMigliore(partita, bersagliValidi);
    }

    private Persona scegliBersaglioPrigione(Partita partita) {
        List<Persona> bersagli = new ArrayList<>();
        for (Persona p : partita.getListaGiocatori()) {
            if (!p.equals(this) && !p.getRuolo().equals("Sceriffo")) {
                boolean giaInPrigione = false;
                for (Carta c : p.getCarteInGioco()) {
                    if (c.getNome().equals("Prigione"))
                        giaInPrigione = true;
                }
                if (!giaInPrigione)
                    bersagli.add(p);
            }
        }
        return scegliIlMigliore(partita, bersagli);
    }

    private Persona scegliBersaglio(Partita partita) {
        List<Persona> bersagliValidi = new ArrayList<>();
        for (Persona p : partita.getListaGiocatori()) {
            if (!p.equals(this)) {
                int distanza = partita.calcolaDistanza(this, p);
                if (distanza <= getPistola().getDistanza()) {
                    bersagliValidi.add(p);
                }
            }
        }
        return scegliIlMigliore(partita, bersagliValidi);
    }

    private Persona scegliBersaglioPerDistanza(Partita partita, int maxDistanza) {
        List<Persona> bersagliValidi = new ArrayList<>();
        for (Persona p : partita.getListaGiocatori()) {
            if (!p.equals(this) && partita.calcolaDistanza(this, p) <= maxDistanza) {
                if (!p.getCarteInGioco().isEmpty() || !p.getCarteInMano().isEmpty()) {
                    bersagliValidi.add(p);
                }
            }
        }
        return scegliIlMigliore(partita, bersagliValidi);
    }

    private Persona scegliBersaglioConCarte(Partita partita) {
        List<Persona> bersagliValidi = new ArrayList<>();
        for (Persona p : partita.getListaGiocatori()) {
            if (!p.equals(this)) {
                if (!p.getCarteInGioco().isEmpty() || !p.getCarteInMano().isEmpty()) {
                    bersagliValidi.add(p);
                }
            }
        }
        return scegliIlMigliore(partita, bersagliValidi);
    }

    private void scartaCartaRandom(Persona bersaglio, List<Carta> scarti) {
        System.out.println("Cat Balou usato da " + getNome() + " contro " + bersaglio.getNome());
        if (!bersaglio.getCarteInGioco().isEmpty()) {
            int idx = random.nextInt(bersaglio.getCarteInGioco().size());
            Carta daScartare = bersaglio.getCarteInGioco().get(idx);
            System.out.println(
                    "  E' stata scartata una carta in gioco di " + bersaglio.getNome() + ": " + daScartare.getNome());
            bersaglio.scartaCartaDaGioco(daScartare, scarti);
        } else if (!bersaglio.getCarteInMano().isEmpty()) {
            int idx = random.nextInt(bersaglio.getCarteInMano().size());
            System.out.println("  E' stata scartata una carta dalla mano di " + bersaglio.getNome());
            scarti.add(bersaglio.getCarteInMano().remove(idx));
        }
    }

    @Override
    public Carta usaMancatoDallaMano(Partita partita) {
        // Il bot decide *sempre* di usare Mancato! se ce l'ha.
        for (int i = 0; i < getCarteInMano().size(); i++) {
            if (getCarteInMano().get(i).getNome().equals("Mancato!")) {
                System.out.println(getNome() + " sceglie automaticamente di usare Mancato!");
                Carta c = getCarteInMano().remove(i);
                verificaAbilitaSuzyLafayette(partita);
                return c;
            }
        }
        return null;
    }

    @Override
    public Carta rubaCarta(Persona bersaglio, Persona giocatore) {
        // Ruba a caso senza popup
        if (!bersaglio.getCarteInMano().isEmpty()) {
            Carta rubata = bersaglio.getCarteInMano().remove(random.nextInt(bersaglio.getCarteInMano().size()));
            giocatore.getCarteInMano().add(rubata);
            System.out.println(giocatore.getNome() + " (BOT) ruba una carta dalla mano di " + bersaglio.getNome());
            return rubata;
        } else if (!bersaglio.getCarteInGioco().isEmpty()) {
            Carta rubata = bersaglio.getCarteInGioco().get(random.nextInt(bersaglio.getCarteInGioco().size()));
            bersaglio.scartaCartaDaGioco(rubata, null); // Rimuove e resetta arma se serve
            giocatore.getCarteInMano().add(rubata);
            System.out.println(giocatore.getNome() + " (BOT) ruba una carta in gioco da " + bersaglio.getNome() + ": "
                    + rubata.getNome());
            return rubata;
        }
        return null;
    }
}
