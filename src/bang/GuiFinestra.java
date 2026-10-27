package bang;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;

public class GuiFinestra {
    private static JFrame frame;
    private static JTextArea textArea;
    private static JPanel handPanel;
    private static JScrollPane handScrollPane;
    private static JScrollPane logScrollPane;
    private static JPanel centerContainer;

    public static void createAndShowGUI() {
        CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Boom! (Arnaldo West) - Log di Gioco");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1024, 768); // fallback size
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // start maximized
            frame.setLocationRelativeTo(null);

            textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

            logScrollPane = new JScrollPane(textArea);
            logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            centerContainer = new JPanel(new BorderLayout());
            centerContainer.setBackground(new Color(46, 139, 87));
            centerContainer.add(logScrollPane, BorderLayout.CENTER);

            frame.getContentPane().setBackground(new Color(46, 139, 87));
            frame.add(centerContainer, BorderLayout.CENTER);

            initHandPanel();

            frame.setVisible(true);

            // Reindirizza l'output standard e gli errori verso la JTextArea
            // OutputTracer customOut = new OutputTracer(textArea);
            // System.setOut(customOut);
            // System.setErr(customOut);

            latch.countDown();
        });

        try {
            // Aspetta che la GUI sia visibile prima di far continuare il main thread
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void appendText(String text) {
        if (textArea != null) {
            SwingUtilities.invokeLater(() -> {
                textArea.append(text);
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }
    }

    // Metodi aggiunti per la nuova versione Full Async UI
    private static TavoloPanel currentTavolo;

    public static void setTavoloPanel(TavoloPanel t) {
        currentTavolo = t;
    }

    public static TavoloPanel getCurrentTavolo() {
        return currentTavolo;
    }

    public static void aggiornaMano(Persona giocatore) {
        if (currentTavolo != null && !(giocatore instanceof Bot)) {
            currentTavolo.aggiornaManoUmano(giocatore);
            return;
        }

        // Logica legacy per la handPanel (nel caso serva per transizione)
        if (handPanel == null)
            return;

        SwingUtilities.invokeLater(() -> {
            handPanel.removeAll();

            // Seleziona se ci sono carte
            if (giocatore == null || giocatore.getCarteInMano().isEmpty()) {
                handPanel.revalidate();
                handPanel.repaint();
                return;
            }

            for (Carta c : giocatore.getCarteInMano()) {
                String imagePath = InputUtil.getImagePathForCard(c.getNome());

                java.io.File imgFile = new java.io.File(imagePath);
                JLabel cardLabel;
                if (imgFile.exists()) {
                    ImageIcon icon = new ImageIcon(imagePath);
                    Image scaled = icon.getImage().getScaledInstance(100, 150, Image.SCALE_SMOOTH);
                    cardLabel = new JLabel(new ImageIcon(scaled));
                    // Aggiungi un tooltip testuale caso mai l'immagine non sia chiara
                    cardLabel.setToolTipText(c.getNome());
                } else {
                    cardLabel = new JLabel(c.getNome());
                    cardLabel.setPreferredSize(new Dimension(100, 150));
                    cardLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    cardLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                }

                handPanel.add(cardLabel);
            }

            handPanel.revalidate();
            handPanel.repaint();
        });
    }

    public static void initHandPanel() {
        handPanel = new JPanel();
        handPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        handPanel.setBackground(new Color(34, 139, 34)); // Forest green

        handScrollPane = new JScrollPane(handPanel);
        handScrollPane.setPreferredSize(new Dimension(800, 190));
        handScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        handScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        frame.add(handScrollPane, BorderLayout.SOUTH);
    }

    public static void setMainPanel(JPanel panel) {
        SwingUtilities.invokeLater(() -> {
            centerContainer.removeAll();
            centerContainer.add(panel, BorderLayout.CENTER);
            centerContainer.revalidate();
            centerContainer.repaint();

            if (handScrollPane != null) {
                handScrollPane.setVisible(false); // Nascondi mani nel menu o nel tavolo custom (che ha la sua)
            }
        });
    }

    public static void resetPerPartita() {
        SwingUtilities.invokeLater(() -> {
            centerContainer.removeAll();
            textArea.setText("");
            centerContainer.add(logScrollPane, BorderLayout.CENTER);
            centerContainer.revalidate();
            centerContainer.repaint();

            if (handPanel != null) {
                handPanel.removeAll();
                if (currentTavolo == null && handScrollPane != null) {
                    handScrollPane.setVisible(true);
                }
                handPanel.revalidate();
                handPanel.repaint();
            }
        });
    }
}
