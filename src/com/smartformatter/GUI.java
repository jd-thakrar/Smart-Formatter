package com.smartformatter;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class GUI extends JFrame {

    private final JTextArea editor;
    private boolean dark = false;

    public GUI() {
        try { FlatLightLaf.setup(); } catch (Exception ignored) {}

        setTitle("SmartFormatter - IEEE Paper Editor");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ---------- Toolbar ----------
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));

        JButton insertImg = iconButton("🖼");
        insertImg.setToolTipText("Insert Image");
        insertImg.addActionListener(this::insertImage);

        JButton insertTable = iconButton("📊");
        insertTable.setToolTipText("Insert Table");
        insertTable.addActionListener(this::insertTable);

        JButton exportPDF = iconButton("🧾");
        exportPDF.setToolTipText("Export PDF");
        exportPDF.addActionListener(this::exportPDF);

        JButton theme = iconButton("🌙");
        theme.setToolTipText("Dark/Light Mode");
        theme.addActionListener(e -> toggleTheme(theme));

        bar.add(insertImg);
        bar.add(insertTable);
        bar.add(exportPDF);
        bar.add(Box.createHorizontalStrut(20));
        bar.add(theme);

        add(bar, BorderLayout.NORTH);

        // ---------- Editor ----------
        editor = new JTextArea();
        editor.setFont(new Font("Serif", Font.PLAIN, 18));
        editor.setLineWrap(true);
        editor.setWrapStyleWord(true);
        editor.setMargin(new Insets(20, 20, 20, 20));

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        add(scroll, BorderLayout.CENTER);

        setVisible(true);
    }

    private JButton iconButton(String emoji) {
        JButton b = new JButton(emoji);
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(new Color(240,240,240)); }
            public void mouseExited(java.awt.event.MouseEvent e)  { b.setBackground(Color.WHITE); }
        });
        return b;
    }

    private void insertImage(ActionEvent e) {
        JFileChooser ch = new JFileChooser();
        if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = ch.getSelectedFile();
        String caption = JOptionPane.showInputDialog(this, "Enter caption (optional):");
        editor.insert("\n! " + f.getAbsolutePath().replace("\\","/") +
                (caption != null && !caption.isEmpty() ? " | " + caption : "") +
                " !\n\n", editor.getCaretPosition());
    }

    private void insertTable(ActionEvent e) {
        JTextArea ta = new JTextArea(7,45);
        ta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        ta.setText("Header1, Header2, Header3\nRow1A, Row1B, Row1C");
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                "Insert Table (comma separated)", JOptionPane.PLAIN_MESSAGE);

        String text = editor.getText();
        int pos = editor.getCaretPosition();
        if (pos > 0 && text.charAt(pos - 1) != '\n') editor.insert("\n", pos);

        // Automatically adds extra blank lines for clean spacing
        editor.insert("\n\n|TABLE|\n" + ta.getText().trim() + "\n|ENDTABLE|\n\n", editor.getCaretPosition());
    }

    private void exportPDF(ActionEvent e) {
        try {
            String[] options = {"Two Columns (IEEE)", "Single Column"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "Select PDF Layout:",
                    "Choose Format",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            boolean twoColumn = (choice == 0);

            JFileChooser ch = new JFileChooser();
            ch.setSelectedFile(new File("paper.pdf"));
            if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File f = ch.getSelectedFile();

            IEEEGenerator.generateFromText(editor.getText(), f.getAbsolutePath(), twoColumn);
            JOptionPane.showMessageDialog(this, "✅ PDF Exported Successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ " + ex.getMessage());
        }
    }

    private void toggleTheme(JButton theme) {
        dark = !dark;
        try {
            if (dark) {
                FlatDarkLaf.setup();
                theme.setText("☀");
            } else {
                FlatLightLaf.setup();
                theme.setText("🌙");
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUI::new);
    }
}
