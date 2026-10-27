package bang;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MenuPanel extends JPanel {

    private JComboBox<Integer> numGiocatoriCombo;
    private JComboBox<Integer> numUmaniCombo;
    private JComboBox<String> modalitaCombo;
    private JPanel nomiPanel;
    private List<JTextField> nomiFields;

    public MenuPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 222, 179)); // Colore pergamena/legno chiaro

        // Pannello Titolo
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(50, 0, 30, 0));
        JLabel title = new JLabel("BOOM! (Arnaldo West)", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 64));
        title.setForeground(new Color(60, 30, 0)); // Marrone scuro leggibile
        // Aggiungiamo una piccola ombra via HTML o CSS base
        titlePanel.add(title);
        add(titlePanel, BorderLayout.NORTH);

        // Pannello Centrale con un margine importante
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 100, 0, 100));

        Font labelFont = new Font("Segoe UI", Font.BOLD, 22);
        Font comboFont = new Font("Segoe UI", Font.PLAIN, 20);

        // Modalità
        JPanel modPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        modPanel.setOpaque(false);
        JLabel modLabel = new JLabel("Modalità di Gioco:");
        modLabel.setFont(labelFont);
        modPanel.add(modLabel);

        modalitaCombo = new JComboBox<>(new String[] { "Partita Singola", "Modalità Torneo" });
        modalitaCombo.setFont(comboFont);
        modPanel.add(modalitaCombo);
        centerPanel.add(modPanel);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Giocatori
        JPanel numPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        numPanel.setOpaque(false);

        JLabel totLabel = new JLabel("Giocatori Totali (4-7):");
        totLabel.setFont(labelFont);
        numPanel.add(totLabel);

        numGiocatoriCombo = new JComboBox<>(new Integer[] { 4, 5, 6, 7 });
        numGiocatoriCombo.setFont(comboFont);
        numPanel.add(numGiocatoriCombo);

        numPanel.add(Box.createRigidArea(new Dimension(40, 0))); // Spazio

        JLabel umaniLabel = new JLabel("Di cui Umani:");
        umaniLabel.setFont(labelFont);
        numPanel.add(umaniLabel);

        numUmaniCombo = new JComboBox<>(new Integer[] { 0, 1, 2, 3, 4 });
        numUmaniCombo.setFont(comboFont);
        numPanel.add(numUmaniCombo);

        centerPanel.add(numPanel);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        // Pannello Nomi (con scroll nel caso siano tanti)
        nomiPanel = new JPanel();
        nomiPanel.setLayout(new BoxLayout(nomiPanel, BoxLayout.Y_AXIS));
        nomiPanel.setOpaque(false);
        nomiFields = new ArrayList<>();

        JScrollPane nomiScroll = new JScrollPane(nomiPanel);
        nomiScroll.setOpaque(false);
        nomiScroll.getViewport().setOpaque(false);
        nomiScroll.setBorder(BorderFactory.createEmptyBorder());
        nomiScroll.setPreferredSize(new Dimension(600, 250));

        JPanel scrollContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        scrollContainer.setOpaque(false);
        scrollContainer.add(nomiScroll);
        centerPanel.add(scrollContainer);

        // Update listeners
        numUmaniCombo.addActionListener(e -> updateNomiPanel());
        numGiocatoriCombo.addActionListener(e -> updateUmaniCombo());

        add(centerPanel, BorderLayout.CENTER);

        // Bottoni
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 50));
        bottomPanel.setOpaque(false);

        JButton playBtn = createStyledButton("AVVIA PARTITA", new Color(46, 139, 87), Color.WHITE);
        playBtn.addActionListener(e -> avviaPartita());

        JButton exitBtn = createStyledButton("ESCI DAL GIOCO", new Color(205, 92, 92), Color.WHITE);
        exitBtn.addActionListener(e -> System.exit(0));

        bottomPanel.add(playBtn);
        bottomPanel.add(exitBtn);

        add(bottomPanel, BorderLayout.SOUTH);

        updateUmaniCombo();
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 28));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 2),
                new EmptyBorder(15, 40, 15, 40)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect base
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg);
            }
        });

        return btn;
    }

    private void updateUmaniCombo() {
        int tot = (Integer) numGiocatoriCombo.getSelectedItem();
        Integer[] umaniPossibili = new Integer[tot + 1];
        for (int i = 0; i <= tot; i++)
            umaniPossibili[i] = i;

        int prevSelected = numUmaniCombo.getSelectedItem() != null ? (Integer) numUmaniCombo.getSelectedItem() : 1;
        numUmaniCombo.setModel(new DefaultComboBoxModel<>(umaniPossibili));
        if (prevSelected <= tot) {
            numUmaniCombo.setSelectedItem(prevSelected);
        } else {
            numUmaniCombo.setSelectedItem(1);
        }
        updateNomiPanel();
    }

    private void updateNomiPanel() {
        nomiPanel.removeAll();
        nomiFields.clear();
        int umani = (Integer) numUmaniCombo.getSelectedItem();

        Font inputFont = new Font("Segoe UI", Font.PLAIN, 20);
        Font labelFont = new Font("Segoe UI", Font.BOLD, 20);

        for (int i = 0; i < umani; i++) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            p.setOpaque(false);

            JLabel lbl = new JLabel("Nome Giocatore " + (i + 1) + ": ");
            lbl.setFont(labelFont);
            p.add(lbl);

            JTextField tf = new JTextField("Giocatore " + (i + 1), 15);
            tf.setFont(inputFont);
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 1),
                    new EmptyBorder(5, 10, 5, 10)));
            nomiFields.add(tf);
            p.add(tf);

            nomiPanel.add(p);
        }
        nomiPanel.revalidate();
        nomiPanel.repaint();
    }

    private void avviaPartita() {
        boolean isTorneo = modalitaCombo.getSelectedIndex() == 1;
        int tot = (Integer) numGiocatoriCombo.getSelectedItem();
        int umani = (Integer) numUmaniCombo.getSelectedItem();

        List<String> nomi = new ArrayList<>();
        for (JTextField tf : nomiFields) {
            String n = tf.getText().trim();
            if (n.isEmpty())
                n = "Giocatore";
            nomi.add(n);
        }

        for (int i = umani; i < tot; i++) {
            nomi.add("Bot_" + (i - umani + 1));
        }

        GameConfig config = new GameConfig(isTorneo, tot, umani, nomi);

        // Passa la palla al thread del gioco che e' in attesa
        AsyncGameProxy.submitInput(config);
    }
}
