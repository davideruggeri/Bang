package bang;

public enum Personaggio {
    BART_CASSIDY("Bart Cassidy", 4, "Ogni volta che perde un PF, pesca immediatamente una carta dal mazzo."),
    EL_GRINGO("El Gringo", 3,
            "Ogni volta che perde un PF a causa di una carta giocata da un giocatore, pesca una carta a caso dalla mano di quel giocatore."),
    JOURDONNAIS("Jourdonnais", 4, "È come se avesse sempre in gioco un Barile."),
    PAUL_REGRET("Paul Regret", 3, "È come se avesse sempre in gioco un Mustang (visto a distanza +1)."),
    ROSE_DOOLAN("Rose Doolan", 4, "È come se avesse sempre in gioco un Mirino (vede a distanza -1)."),
    SID_KETCHUM("Sid Ketchum", 4, "In qualunque momento può scartare 2 carte per recuperare 1 PF."),
    SUZY_LAFAYETTE("Suzy Lafayette", 4,
            "Non appena rimane senza carte in mano, pesca immediatamente una carta dal mazzo.");

    private final String nome;
    private final int pfBase;
    private final String descrizione;

    Personaggio(String nome, int pfBase, String descrizione) {
        this.nome = nome;
        this.pfBase = pfBase;
        this.descrizione = descrizione;
    }

    public String getNome() {
        return nome;
    }

    public int getPfBase() {
        return pfBase;
    }

    public String getDescrizione() {
        return descrizione;
    }
}
