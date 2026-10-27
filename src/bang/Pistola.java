package bang;

public class Pistola extends Carta {
    private int distanza;

    public Pistola(String nome, String descrizione, boolean equipaggiabile, String valori, String semi, int distanza) {
        super(nome, descrizione, equipaggiabile, valori, semi);
        this.distanza = distanza;
    }

    public void setDistanza(int distanza) {
        this.distanza = distanza;
    }

    // FIX: aggiunto getDistanza() mancante
    public int getDistanza() {
        return distanza;
    }

    // FIX: riduciDistanza ora funziona correttamente (rimuove dipendenza da
    // haMirino rotto)
    public void riduciDistanza() {
        distanza = Math.max(1, distanza - 1);
    }

    // FIX: aumentaDistanza ora funziona correttamente
    public void aumentaDistanza() {
        distanza++;
    }

    @Override
    public String toString() {
        return "Pistola{" + getNome() + '\'' +
                ", distanza=" + distanza +
                '}';
    }
}
