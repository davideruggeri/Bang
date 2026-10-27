package bang;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TavoloPanel extends JPanel {
    private Partita partita;
    private JLabel statusLabel;
    private JPanel opzioniPanel;

    private JPanel nordPanel;
    private JPanel estPanel;
    private JPanel ovestPanel;
    private JPanel centroPanel;
    private JPanel sudPanel;

    // Colori moderni
    private final Color MAIN_TABLE_COLOR = new Color(46, 139, 87); // SeaGreen scuro / Tavolo da poker
    private final Font MAIN_FONT = new Font("Segoe UI", Font.BOLD, 16);

    public TavoloPanel(Partita partita) {
        this.partita = partita;
        setLayout(new BorderLayout(20, 20)); // Gaps
        setBackground(MAIN_TABLE_COLOR);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        nordPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        nordPanel.setOpaque(false);
        estPanel = new JPanel(new GridBagLayout()); // Per centrare verticalmente
        estPanel.setOpaque(false);
        ovestPanel = new JPanel(new GridBagLayout());
        ovestPanel.setOpaque(false);

        centroPanel = new JPanel();
        centroPanel.setLayout(new BoxLayout(centroPanel, BoxLayout.Y_AXIS));
        centroPanel.setOpaque(false);

        statusLabel = new JLabel("Inizia la partita!", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        opzioniPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        opzioniPanel.setOpaque(false);
        opzioniPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel mazziPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 60, 20));
        mazziPanel.setOpaque(false);

        // Segnaposto Mazzo
        JLabel mazzoLabel = createDeckLabel("MAZZO", new Color(139, 69, 19), true);
        JLabel scartiLabel = createDeckLabel("SCARTI", new Color(200, 200, 200), false);

        mazziPanel.add(mazzoLabel);
        mazziPanel.add(scartiLabel);

        centroPanel.add(Box.createVerticalGlue());
        centroPanel.add(statusLabel);
        centroPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centroPanel.add(opzioniPanel);
        centroPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centroPanel.add(mazziPanel);
        centroPanel.add(Box.createVerticalGlue());

        sudPanel = new JPanel(new BorderLayout());
        sudPanel.setOpaque(false);
        // Assicuriamoci che l'intera area sud occupi lo spazio che le serve senza
        // sbavature bianche
        sudPanel.setBackground(MAIN_TABLE_COLOR);

        add(nordPanel, BorderLayout.NORTH);
        add(estPanel, BorderLayout.EAST);
        add(ovestPanel, BorderLayout.WEST);
        add(centroPanel, BorderLayout.CENTER);
        add(sudPanel, BorderLayout.SOUTH);
    }

    private JLabel createDeckLabel(String text, Color bg, boolean borderWhite) {
        JLabel label = new JLabel(text);
        label.setPreferredSize(new Dimension(120, 180));
        label.setBorder(BorderFactory.createLineBorder(borderWhite ? Color.WHITE : Color.DARK_GRAY, 3, true));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBackground(bg);
        label.setOpaque(true);
        if (borderWhite)
            label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 20));
        return label;
    }

    public void chiediOpzioni(String titolo, String[] opzioni) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(titolo);
            opzioniPanel.removeAll();
            for (int i = 0; i < opzioni.length; i++) {
                final int idx = i + 1;
                JButton btn = new JButton(opzioni[i]);
                btn.setFont(new Font("Segoe UI", Font.BOLD, 22));
                btn.setBackground(new Color(255, 140, 0)); // Arancione scuro
                btn.setForeground(Color.WHITE);
                btn.setFocusPainted(false);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 100, 0), 2),
                        new EmptyBorder(10, 25, 10, 25)));
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

                btn.addActionListener(e -> {
                    opzioniPanel.removeAll();
                    opzioniPanel.revalidate();
                    opzioniPanel.repaint();
                    AsyncGameProxy.submitInput(idx);
                });
                opzioniPanel.add(btn);
            }
            opzioniPanel.revalidate();
            opzioniPanel.repaint();
        });
    }

    public void aggiornaManoUmano(Persona giocatore) {
        SwingUtilities.invokeLater(() -> {
            sudPanel.removeAll();

            JPanel handContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
            handContainer.setOpaque(false);

            for (Carta c : giocatore.getCarteInMano()) {
                String imagePath = InputUtil.getImagePathForCard(c.getNome());
                java.io.File imgFile = new java.io.File(imagePath);

                JButton cardBtn = new JButton();
                if (imgFile.exists()) {
                    ImageIcon icon = new ImageIcon(imagePath);
                    Image scaled = icon.getImage().getScaledInstance(120, 180, Image.SCALE_SMOOTH);
                    cardBtn.setIcon(new ImageIcon(scaled));
                    cardBtn.setToolTipText(c.getNome());
                } else {
                    cardBtn.setText(c.getNome());
                    cardBtn.setFont(MAIN_FONT);
                }

                cardBtn.setPreferredSize(new Dimension(120, 180));
                cardBtn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true));
                cardBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

                // Hover highlight card
                cardBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        cardBtn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3, true));
                    }

                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        cardBtn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2, true));
                    }
                });

                cardBtn.addActionListener(e -> {
                    AsyncGameProxy.submitInput(c);
                });

                handContainer.add(cardBtn);
            }

            JScrollPane scroll = new JScrollPane(handContainer);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setPreferredSize(new Dimension(1000, 220));
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            sudPanel.add(scroll, BorderLayout.CENTER);
            sudPanel.revalidate();
            sudPanel.repaint();
        });
    }

    public void setStatusMessage(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

    public void aggiornaTavolo(Persona giocatoreCorrente) {
        SwingUtilities.invokeLater(() -> {
            nordPanel.removeAll();
            estPanel.removeAll();
            ovestPanel.removeAll();

            // Logica per posizionare gli avversari in nord/est/ovest
            java.util.List<Persona> giocatori = partita.getListaGiocatori();
            int myIndex = -1;
            for (int i = 0; i < giocatori.size(); i++) {
                if (!(giocatori.get(i) instanceof Bot)) {
                    myIndex = i;
                    break;
                }
            }
            if (myIndex == -1)
                myIndex = 0;

            int numAvversari = giocatori.size() - 1;
            int offset = 1;

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.gridx = 0;

            for (int panelPos = 0; panelPos < numAvversari; panelPos++) {
                int avvIdx = (myIndex + offset) % giocatori.size();
                Persona avv = giocatori.get(avvIdx);
                AvversarioPanel ap = new AvversarioPanel(avv);

                gbc.gridy = panelPos;

                if (numAvversari <= 3) {
                    if (panelPos == 0)
                        ovestPanel.add(ap, gbc);
                    else if (panelPos == 1)
                        nordPanel.add(ap);
                    else
                        estPanel.add(ap, gbc);
                } else if (numAvversari == 4) {
                    if (panelPos == 0)
                        ovestPanel.add(ap, gbc);
                    else if (panelPos == 1 || panelPos == 2)
                        nordPanel.add(ap);
                    else
                        estPanel.add(ap, gbc);
                } else {
                    if (panelPos < 2)
                        ovestPanel.add(ap, gbc);
                    else if (panelPos < 4)
                        nordPanel.add(ap);
                    else
                        estPanel.add(ap, gbc); // Riutilizzo gbc.gridy qui calcolato al volo idealmente, ma per brevità
                                               // va aggiunto come layout flow nel nord
                }
                offset++;
            }

            revalidate();
            repaint();
        });
    }

    // Pannellino per un singolo avversario (Bot)
    class AvversarioPanel extends JPanel {
        public AvversarioPanel(Persona p) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 215, 0), 2, true), // Bordo dorato base
                    new EmptyBorder(10, 15, 10, 15)));
            setBackground(new Color(40, 40, 40, 220)); // Sfondo dark molto leggibile
            setOpaque(true);

            setPreferredSize(new Dimension(180, 200));

            // Renderlo cliccabile
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.RED, 3, true),
                            new EmptyBorder(9, 14, 9, 14)));
                    setBackground(new Color(60, 40, 40, 220));
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(255, 215, 0), 2, true),
                            new EmptyBorder(10, 15, 10, 15)));
                    setBackground(new Color(40, 40, 40, 220));
                }

                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    AsyncGameProxy.submitInput(p);
                }
            });
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            JLabel nameL = new JLabel(p.getNome());
            nameL.setForeground(Color.WHITE);
            nameL.setAlignmentX(Component.CENTER_ALIGNMENT);
            nameL.setFont(new Font("Segoe UI", Font.BOLD, 18));

            // Build health string with simple text visual using safe unicode or text
            StringBuilder sb = new StringBuilder(" ");
            for (int i = 0; i < p.getVita(); i++)
                sb.append("\u2764 "); // Cuore in unicode corretto
            JLabel hpL = new JLabel(sb.toString().trim() + " (" + p.getVita() + ")");
            hpL.setForeground(new Color(255, 100, 100)); // Rosso scuro/chiaro
            hpL.setAlignmentX(Component.CENTER_ALIGNMENT);
            hpL.setFont(new Font("Segoe UI", Font.BOLD, 14));

            JLabel roleL = new JLabel(p.getRuolo().equals("Sceriffo") || p.getVita() <= 0 ? p.getRuolo() : "?");
            roleL.setForeground(new Color(200, 200, 200));
            roleL.setAlignmentX(Component.CENTER_ALIGNMENT);
            roleL.setFont(new Font("Segoe UI", Font.ITALIC, 14));

            JLabel cardL = new JLabel("Carte in mano: " + p.getCarteInMano().size());
            cardL.setForeground(Color.WHITE);
            cardL.setAlignmentX(Component.CENTER_ALIGNMENT);
            cardL.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            add(nameL);
            add(Box.createRigidArea(new Dimension(0, 5)));
            add(hpL);
            add(roleL);
            add(Box.createRigidArea(new Dimension(0, 15)));
            add(cardL);

            for (Carta c : p.getCarteInGioco()) {
                JLabel cL = new JLabel(">> " + c.getNome()); // pallino safely encoded
                cL.setForeground(new Color(135, 206, 250)); // Light Sky Blue
                cL.setFont(new Font("Segoe UI", Font.BOLD, 12));
                cL.setAlignmentX(Component.CENTER_ALIGNMENT);
                add(cL);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            // Se si vuole un background semi-trasparente e coi bordi curvi in Swing:
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
