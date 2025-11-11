package com.smartformatter;

import javax.swing.*;
import java.awt.*;
import java.security.MessageDigest;
import java.sql.*;

public class RegisterFrame extends JFrame {
    private JTextField usernameField, emailField, fullNameField, institutionField;
    private JPasswordField passwordField;

    public RegisterFrame() {
        setTitle("Register - SmartFormatter Pro");
        setSize(400, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Create Your Account", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Fields
        JLabel userLabel = new JLabel("Username:");
        JLabel passLabel = new JLabel("Password:");
        JLabel emailLabel = new JLabel("Email:");
        JLabel nameLabel = new JLabel("Full Name:");
        JLabel instLabel = new JLabel("Institution:");

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        emailField = new JTextField();
        fullNameField = new JTextField();
        institutionField = new JTextField();

        Dimension fieldSize = new Dimension(250, 26);
        usernameField.setPreferredSize(fieldSize);
        passwordField.setPreferredSize(fieldSize);
        emailField.setPreferredSize(fieldSize);
        fullNameField.setPreferredSize(fieldSize);
        institutionField.setPreferredSize(fieldSize);

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row++; form.add(userLabel, gbc);
        gbc.gridy = row++; form.add(usernameField, gbc);
        gbc.gridy = row++; form.add(passLabel, gbc);
        gbc.gridy = row++; form.add(passwordField, gbc);
        gbc.gridy = row++; form.add(emailLabel, gbc);
        gbc.gridy = row++; form.add(emailField, gbc);
        gbc.gridy = row++; form.add(nameLabel, gbc);
        gbc.gridy = row++; form.add(fullNameField, gbc);
        gbc.gridy = row++; form.add(instLabel, gbc);
        gbc.gridy = row++; form.add(institutionField, gbc);

        add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton registerBtn = new JButton("Register");
        JButton backBtn = new JButton("Back");

        registerBtn.setBackground(new Color(0x34A853));
        registerBtn.setForeground(Color.WHITE);
        backBtn.setBackground(new Color(0xDB4437));
        backBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        backBtn.setFocusPainted(false);

        btnPanel.add(registerBtn);
        btnPanel.add(backBtn);
        add(btnPanel, BorderLayout.SOUTH);

        registerBtn.addActionListener(e -> registerUser());
        backBtn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
    }

    private void registerUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String email = emailField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String institution = institutionField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all required fields (Username, Password, Email).");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement check = conn.prepareStatement("SELECT * FROM users WHERE username=?");
            check.setString(1, username);
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Username already exists!");
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users(username, password_hash, email, full_name, institution) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, username);
            ps.setString(2, hash(password));
            ps.setString(3, email);
            ps.setString(4, fullName);
            ps.setString(5, institution);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Registration successful!");
            dispose();
            new LoginFrame().setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
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
