package bang;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Classifica {
    // Mappa Nome Giocatore -> Statistiche
    private Map<String, StatisticheGiocatore> mappaStatistiche;

    public Classifica() {
        this.mappaStatistiche = new HashMap<>();
    }

    /**
     * Iscrive un giocatore al torneo.
     * Se è nuovo, gli vengono assegnati 500 sbleuri iniziali.
     */
    public void iscriviGiocatore(String nome) {
        if (!mappaStatistiche.containsKey(nome)) {
            mappaStatistiche.put(nome, new StatisticheGiocatore(nome, 500, 0));
            System.out.println("Nuovo giocatore iscritto: " + nome + " (500\u00A7 iniziali)");
        }
    }

    /**
     * Paga la tassa di partecipazione per la partita (50 \u00A7).
     * Ritorna true se il giocatore ha abbastanza sbleuri o se si accetta il debito.
     */
    public void pagaTassa(String nome) {
        StatisticheGiocatore stat = mappaStatistiche.get(nome);
        if (stat != null) {
            stat.aggiungiSbleuri(-50);
            stat.incrementaPartite();
            System.out.println(
                    nome + " ha pagato la tassa di 50\u00A7. Bilancio attuale: " + stat.getSbleuri() + "\u00A7");
        }
    }

    /**
     * Aggiunge sbleuri al giocatore in base al premio vinto.
     */
    public void aggiungiPremio(String nome, int sbleuri) {
        StatisticheGiocatore stat = mappaStatistiche.get(nome);
        if (stat != null && sbleuri > 0) {
            stat.aggiungiSbleuri(sbleuri);
            System.out.println(
                    nome + " ha vinto " + sbleuri + "\u00A7! Bilancio attuale: " + stat.getSbleuri() + "\u00A7");
        }
    }

    /**
     * Calcola e distribuisce i premi a fine partita in base alla fazione vincente.
     */
    public void distribuisciPremi(RisultatoPartita risultato, int numGiocatori, Partita partita) {
        System.out.println("\n--- Distribuzione Premi ---");
        for (Persona p : partita.getListaGiocatori()) {
            int premio = 0;
            String ruolo = p.getRuolo();

            switch (risultato) {
                case LEGGE:
                    if (ruolo.equals("Sceriffo")) {
                        premio = getPremioSceriffo(numGiocatori);
                    } else if (ruolo.equals("Vice")) {
                        premio = getPremioVice(numGiocatori);
                    }
                    break;

                case FUORILEGGE:
                    if (ruolo.equals("Fuorilegge")) {
                        premio = getPremioFuorilegge(numGiocatori);
                    }
                    break;

                case RINNEGATO:
                    if (ruolo.equals("Rinnegato")) {
                        premio = getPremioRinnegato(numGiocatori);
                    }
                    break;

                default:
                    break;
            }

            aggiungiPremio(p.getNome(), premio);
        }

        // Ricompensa speciale per il Rinnegato se ha perso all'ultimo contro lo
        // Sceriffo (vittoria Legge)
        if (risultato == RisultatoPartita.LEGGE) {
            Persona rinnegato = partita.getRinnegato();
            if (rinnegato != null && partita.isRinnegatoMortoPerUltimoDalloSceriffo()) {
                int premioConsolazione = getPremioRinnegatoConsolazione(numGiocatori);
                System.out.println("Il Rinnegato (" + rinnegato.getNome()
                        + ") è morto per ultimo per mano dello Sceriffo. Riceve un premio di consolazione!");
                aggiungiPremio(rinnegato.getNome(), premioConsolazione);
            }
        }
    }

    private int getPremioSceriffo(int numGiocatori) {
        return switch (numGiocatori) {
            case 4 -> 1400;
            case 5 -> 1200;
            case 6 -> 1600;
            case 7 -> 1200;
            default -> 0;
        };
    }

    private int getPremioVice(int numGiocatori) {
        return switch (numGiocatori) {
            case 4 -> 0; // Non ci sono vice in 4
            case 5 -> 1200;
            case 6 -> 1600;
            case 7 -> 1200;
            default -> 0;
        };
    }

    private int getPremioRinnegatoConsolazione(int numGiocatori) {
        return switch (numGiocatori) {
            case 4 -> 250;
            case 5 -> 300;
            case 6 -> 350;
            case 7 -> 400;
            default -> 0;
        };
    }

    private int getPremioFuorilegge(int numGiocatori) {
        return switch (numGiocatori) {
            case 4 -> 1600;
            case 5 -> 1800;
            case 6 -> 1500;
            case 7 -> 1700;
            default -> 0;
        };
    }

    private int getPremioRinnegato(int numGiocatori) {
        return switch (numGiocatori) {
            case 4 -> 2200;
            case 5 -> 2400;
            case 6 -> 2600;
            case 7 -> 2800;
            default -> 0;
        };
    }

    /**
     * Salva la classifica e le statistiche in un file CSV.
     */
    public void salvaSuFile(String fileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("Nome;ToT. Sbleuri (\u00A7);Partite Giocate;Punteggio");
            for (StatisticheGiocatore stat : mappaStatistiche.values()) {
                writer.println(stat.getNome() + ";" +
                        stat.getSbleuri() + ";" +
                        stat.getPartiteGiocate() + ";" +
                        String.format("%.2f", stat.getPunteggio()));
            }
            System.out.println("Classifica salvata correttamente in " + fileName);
        } catch (IOException e) {
            System.err.println("Errore durante il salvataggio della classifica: " + e.getMessage());
        }
    }

    // Classe interna di supporto per le statistiche del giocatore
    private static class StatisticheGiocatore {
        private String nome;
        private int sbleuri;
        private int partiteGiocate;

        public StatisticheGiocatore(String nome, int sbleuri, int partiteGiocate) {
            this.nome = nome;
            this.sbleuri = sbleuri;
            this.partiteGiocate = partiteGiocate;
        }

        public String getNome() {
            return nome;
        }

        public int getSbleuri() {
            return sbleuri;
        }

        public int getPartiteGiocate() {
            return partiteGiocate;
        }

        public void aggiungiSbleuri(int qty) {
            this.sbleuri += qty;
        }

        public void incrementaPartite() {
            this.partiteGiocate++;
        }

        public float getPunteggio() {
            if (partiteGiocate == 0)
                return 0f;
            return (float) sbleuri / partiteGiocate;
        }
    }
}
