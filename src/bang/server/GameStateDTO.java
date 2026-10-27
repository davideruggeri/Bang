package bang.server;

import bang.Carta;
import bang.Partita;
import bang.Persona;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object — snapshot serializzabile in JSON dello stato della
 * partita. Gson lo serializza automaticamente, il frontend lo legge via
 * polling.
 */
public class GameStateDTO {

    public String status; // IDLE | IN_CORSO | FINITA
    public String giocatoreCorrente;
    public PromptDTO prompt;
    public List<PlayerDTO> giocatori;
    public List<PlayerDTO> giocatoriEliminati;
    public int carteRimanenti;
    public CardDTO ultimaCartaScartata;
    public String lobbyCode;
    public int expectedHumans;
    public List<String> joinedHumans;
    public CardDTO lastPlayedCard;
    public String lastPlayedActor;
    public String lastPlayedTarget;
    public CardDTO lastDrawnCard;
    public List<String> eventi;

    public static class PlayerDTO {
        public String nome;
        public String ruolo; // visibile solo se Sceriffo (o eliminato)
        public String descrizioneRuolo;
        public String personaggio;
        public String descrizionePersonaggio;
        public int vita;
        public int vitaMax;
        public boolean isUmano;
        public boolean isTurnoCorrente;
        public List<CardDTO> carteInMano;
        public List<CardDTO> carteInGioco;
        public String arma;
        public int portataArma;
    }

    public static class CardDTO {
        public String nome;
        public String descrizione;
        public String imagePath;
        public long timestamp;
        public String seme;
        public String valore;
        public boolean isEstrazione;
        public String ownerName;

        public static CardDTO from(bang.Carta c) {
            return from(c, false);
        }

        public static CardDTO from(bang.Carta c, boolean isEstrazione) {
            return from(c, isEstrazione, null);
        }

        public static CardDTO from(bang.Carta c, boolean isEstrazione, String ownerName) {
            if (c == null)
                return null;
            CardDTO dto = new CardDTO();
            dto.nome = c.getNome();
            dto.descrizione = c.getDescrizione();
            dto.imagePath = bang.InputUtil.getImagePathForCard(c.getNome());
            dto.seme = c.getSemi();
            dto.valore = c.getValori();
            dto.isEstrazione = isEstrazione;
            dto.ownerName = ownerName;
            dto.timestamp = System.currentTimeMillis();
            return dto;
        }
    }

    public static class PromptDTO {
        public String targetPlayer;
        public String tipo; // MENU | CARD | YESNO | PERSONA | STRING
        public String messaggio;
        public List<String> opzioni;

        // Added constructor for convenience as per the diff
        public PromptDTO(String target, String tipo, String messaggio, List<String> opzioni) {
            this.targetPlayer = target;
            this.tipo = tipo;
            this.messaggio = messaggio;
            this.opzioni = opzioni;
        }
    }

    /** Costruisce il DTO dalla partita in corso + stato del prompt attivo. */
    public static GameStateDTO from(Partita partita, String status,
            String currentPlayerName,
            String promptTarget, String promptTipo, String promptMsg,
            List<String> promptOpzioni,
            List<String> eventi,
            CardDTO lastPlayed,
            CardDTO lastDrawn,
            String lastActor, String lastTarget,
            String viewForPlayer,
            String lobbyCode, int expectedHumans, List<String> joinedHumans) { // Parametri lobby
        GameStateDTO dto = new GameStateDTO();
        dto.status = status;
        dto.lobbyCode = lobbyCode;
        dto.expectedHumans = expectedHumans;
        dto.joinedHumans = joinedHumans != null ? joinedHumans : new ArrayList<>();
        dto.giocatoreCorrente = currentPlayerName;
        dto.lastPlayedCard = lastPlayed;
        dto.lastPlayedActor = lastActor;
        dto.lastPlayedTarget = lastTarget;
        // Privacy per l'animazione di pescata
        if (lastDrawn != null) {
            boolean isMine = viewForPlayer != null && viewForPlayer.trim()
                    .equalsIgnoreCase(lastDrawn.ownerName != null ? lastDrawn.ownerName.trim() : "");
            if (lastDrawn.isEstrazione || isMine) {
                dto.lastDrawnCard = lastDrawn;
            } else {
                // Gli altri vedono che qualcuno ha pescato, ma vedono il Dorso
                CardDTO hidden = new CardDTO();
                hidden.nome = "Dorso";
                hidden.imagePath = "/cards/01_back.webp";
                hidden.timestamp = lastDrawn.timestamp;
                hidden.isEstrazione = false;
                hidden.ownerName = lastDrawn.ownerName;
                dto.lastDrawnCard = hidden;
            }
        }
        dto.carteRimanenti = partita != null ? partita.getListaMazzo().size() : 0;

        if (partita != null && !partita.getListaScarti().isEmpty()) {
            Carta topScarto = partita.getListaScarti().get(partita.getListaScarti().size() - 1);
            dto.ultimaCartaScartata = CardDTO.from(topScarto);
        }

        dto.eventi = eventi != null ? eventi : new ArrayList<>();

        if (promptTipo != null) {
            dto.prompt = new PromptDTO(promptTarget, promptTipo, promptMsg, promptOpzioni);
        }

        dto.giocatori = new ArrayList<>();
        dto.giocatoriEliminati = new ArrayList<>();
        if (partita != null) {
            for (Persona p : partita.getListaGiocatori()) {
                dto.giocatori.add(toPlayerDTO(p, status, currentPlayerName, viewForPlayer));
            }
            for (Persona p : partita.getListaGiocatoriEliminati()) {
                dto.giocatoriEliminati.add(toPlayerDTO(p, status, currentPlayerName, viewForPlayer));
            }
        }
        return dto;
    }

    private static PlayerDTO toPlayerDTO(Persona p, String status, String currentPlayerName, String viewForPlayer) {
        PlayerDTO pdto = new PlayerDTO();
        pdto.nome = p.getNome();
        pdto.descrizionePersonaggio = p.getPersonaggio() != null ? p.getPersonaggio().getDescrizione() : "";

        // Logica visibilità:
        // - IsMe: sono io il richiedente?
        // - Sceriffo: visibile a tutti
        // - Eliminato: ruolo rivelato
        // - FINITA: tutto rivelato
        boolean isMe = viewForPlayer != null && viewForPlayer.trim().equalsIgnoreCase(p.getNome().trim());
        boolean isVisible = isMe ||
                "Sceriffo".equals(p.getRuolo()) ||
                p.getVita() <= 0 ||
                status != null && status.startsWith("FINITA");

        pdto.ruolo = isVisible ? p.getRuolo() : "Sconosciuto";

        if ("Sconosciuto".equals(pdto.ruolo)) {
            pdto.descrizioneRuolo = "Identità segreta. Quali saranno i suoi obiettivi?";
        } else {
            pdto.descrizioneRuolo = switch (pdto.ruolo) {
                case "Sceriffo" -> "Deve eliminare tutti i Fuorilegge e il Rinnegato.";
                case "Fuorilegge" -> "Devono eliminare lo Sceriffo. Non hanno scrupoli!";
                case "Vice" -> "Aiuta colui che protegge la legge (lo Sceriffo).";
                case "Rinnegato" -> "Vuole diventare il nuovo Sceriffo rimanendo l'ultimo in vita.";
                default -> "";
            };
        }
        pdto.personaggio = p.getPersonaggio().getNome();
        pdto.vita = p.getVita();
        pdto.vitaMax = p.getVitaMax();
        pdto.isUmano = !(p instanceof bang.Bot);
        pdto.isTurnoCorrente = p.getNome().equals(currentPlayerName);
        pdto.arma = p.getPistola() != null ? p.getPistola().getNome() : "Colt .45";
        pdto.portataArma = p.getPistola() != null ? p.getPistola().getDistanza() : 1;

        pdto.carteInMano = new ArrayList<>();
        for (Carta c : p.getCarteInMano()) {
            if (isVisible) {
                pdto.carteInMano.add(CardDTO.from(c));
            } else {
                // Censura: mandiamo solo un "placeholder" per far contare le carte al frontend
                CardDTO hidden = new CardDTO();
                hidden.nome = "Dorso";
                hidden.imagePath = "/cards/01_back.webp";
                pdto.carteInMano.add(hidden);
            }
        }

        pdto.carteInGioco = new ArrayList<>();
        for (Carta c : p.getCarteInGioco()) {
            pdto.carteInGioco.add(CardDTO.from(c));
        }
        return pdto;
    }

}
