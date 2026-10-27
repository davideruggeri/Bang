package bang;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Legge il file listaCarte.xml e restituisce un unico mazzo composto
 * da carte normali (tag <carta>) e armi (tag <arma>).
 *
 * Struttura XML attesa:
 * <arnaldoWest>
 * <armi>
 * <arma>
 * <nome>...</nome>
 * <distanza>N</distanza>
 * <copie totale="N">
 * <copia><valore>...</valore><seme>...</seme></copia>
 * </copie>
 * </arma>
 * </armi>
 * <carte>
 * <carta equipaggiabile="true|false">
 * <nome>...</nome>
 * <descrizione>...</descrizione>
 * <copie totale="N">
 * <copia><valore>...</valore><seme>...</seme></copia>
 * </copie>
 * </carta>
 * </carte>
 * </arnaldoWest>
 */
public class LettoreXML {

    private static final String FILE_XML = "listaCarte.xml";

    /**
     * Legge l'intero XML e restituisce un mazzo con tutte le carte e le armi.
     */
    public List<Carta> leggiMazzo() {
        List<Carta> mazzo = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(FILE_XML)) {
            XMLStreamReader xml = XMLInputFactory.newInstance().createXMLStreamReader(fis);

            String nome = null;
            String descrizione = null;
            boolean equipaggiabile = false;
            int distanza = 1;
            String valore = null;
            String seme = null;
            boolean inArma = false;
            boolean inCarta = false;

            while (xml.hasNext()) {
                int event = xml.next();

                if (event == XMLStreamReader.START_ELEMENT) {
                    String tag = xml.getLocalName();

                    switch (tag) {
                        case "arma":
                            inArma = true;
                            inCarta = false;
                            nome = null;
                            descrizione = null;
                            distanza = 1;
                            break;

                        case "carta":
                            inCarta = true;
                            inArma = false;
                            equipaggiabile = Boolean.parseBoolean(
                                    xml.getAttributeValue(null, "equipaggiabile"));
                            nome = null;
                            descrizione = null;
                            break;

                        case "nome":
                            nome = xml.getElementText();
                            break;

                        case "descrizione":
                            descrizione = leggiTesto(xml);
                            break;

                        case "distanza":
                            if (inArma)
                                distanza = Integer.parseInt(xml.getElementText().trim());
                            break;

                        case "valore":
                            valore = xml.getElementText();
                            break;

                        case "seme":
                            seme = xml.getElementText();
                            break;
                    }

                } else if (event == XMLStreamReader.END_ELEMENT) {
                    String tag = xml.getLocalName();

                    if (tag.equals("copia")) {
                        // Una copia = una carta nel mazzo
                        if (inArma && nome != null) {
                            mazzo.add(new Pistola(nome, descrizione, true, valore, seme, distanza));
                        } else if (inCarta && nome != null) {
                            mazzo.add(new Carta(nome, descrizione, equipaggiabile, valore, seme));
                        }
                        valore = null;
                        seme = null;

                    } else if (tag.equals("arma")) {
                        inArma = false;
                    } else if (tag.equals("carta")) {
                        inCarta = false;
                    }
                }
            }
            xml.close();

        } catch (XMLStreamException | IOException e) {
            System.err.println("Errore lettura XML: " + e.getMessage());
        }
        return mazzo;
    }

    /** Legge il testo di un tag multi-riga fino al suo tag di chiusura. */
    private String leggiTesto(XMLStreamReader xml) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        String tagChiusura = xml.getLocalName();
        while (xml.hasNext()) {
            int ev = xml.next();
            if (ev == XMLStreamReader.CHARACTERS || ev == XMLStreamReader.CDATA) {
                sb.append(xml.getText());
            } else if (ev == XMLStreamReader.END_ELEMENT &&
                    xml.getLocalName().equals(tagChiusura)) {
                break;
            }
        }
        return sb.toString().trim();
    }
}