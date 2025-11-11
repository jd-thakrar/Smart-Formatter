package com.smartformatter;

import javax.swing.*;
import java.awt.*;
import java.security.MessageDigest;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("SmartFormatter Login");
        setSize(350, 230);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("SmartFormatter Pro", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel userLabel = new JLabel("Username:");
        JLabel passLabel = new JLabel("Password:");
        usernameField = new JTextField();
        passwordField = new JPasswordField();

        // 🔧 Compact fields (reduced height)
        Dimension fieldSize = new Dimension(200, 26);
        usernameField.setPreferredSize(fieldSize);
        passwordField.setPreferredSize(fieldSize);

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(userLabel, gbc);
        gbc.gridy++;
        form.add(usernameField, gbc);
        gbc.gridy++;
        form.add(passLabel, gbc);
        gbc.gridy++;
        form.add(passwordField, gbc);

        add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout());
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");

        // Rounded modern buttons
        loginBtn.setBackground(new Color(0x4285F4));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        registerBtn.setBackground(new Color(0x34A853));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);

        btns.add(loginBtn);
        btns.add(registerBtn);
        add(btns, BorderLayout.SOUTH);

        loginBtn.addActionListener(e -> loginUser());
        registerBtn.addActionListener(e -> {
            dispose();
            new RegisterFrame().setVisible(true);
        });
    }

    private void loginUser() {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter both fields!");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM users WHERE username=? AND password_hash=?");
            ps.setString(1, user);
            ps.setString(2, hash(pass));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Login successful!");
                dispose();
                new GUI().setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
        }
    }

    private String hash(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(s.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
