package bang;

import java.util.Collections;
import java.util.List;

public class Mazzo {

    /**
     * Crea e restituisce un mazzo mescolato letto dall'XML.
     */
    public List<Carta> creaMazzo() {
        List<Carta> mazzo = new LettoreXML().leggiMazzo();
        Collections.shuffle(mazzo);
        return mazzo;
    }
}
