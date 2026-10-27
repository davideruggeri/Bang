package bang;

import java.util.List;

public class GameConfig {
    public boolean isTorneo;
    public int numGiocatori;
    public int numUmani;
    public List<String> nomiGiocatori;

    public GameConfig(boolean isTorneo, int numGiocatori, int numUmani, List<String> nomiGiocatori) {
        this.isTorneo = isTorneo;
        this.numGiocatori = numGiocatori;
        this.numUmani = numUmani;
        this.nomiGiocatori = nomiGiocatori;
    }
}
