package com.laundry.ui;

import com.laundry.exception.AuthenticationException;
import com.laundry.exception.ValidationException;
import com.laundry.model.User;
import com.laundry.service.AuthenticationService;
import com.laundry.ui.components.CardPanel;
import com.laundry.ui.theme.UIConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 * Modern blue-and-white Login interface for staff and administrators.
 */
public class LoginFrame extends JFrame {

    private final AuthenticationService authService = new AuthenticationService();
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private JButton loginButton;

    public LoginFrame() {
        setTitle("Blue Wave Laundry - System Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel backgroundPanel = new JPanel(new GridBagLayout());
        backgroundPanel.setBackground(UIConstants.BG_LIGHT);

        CardPanel card = new CardPanel(new BorderLayout(0, 20));
        card.setPreferredSize(new Dimension(380, 470));
        card.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Header with Branding
        JPanel headerPanel = new JPanel(new BorderLayout(0, 6));
        headerPanel.setOpaque(false);

        JLabel logoIcon = new JLabel("🧺", SwingConstants.CENTER);
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
        headerPanel.add(logoIcon, BorderLayout.NORTH);

        JLabel titleLabel = new JLabel("Blue Wave Laundry", SwingConstants.CENTER);
        titleLabel.setFont(UIConstants.FONT_TITLE);
        titleLabel.setForeground(UIConstants.PRIMARY_NAVY);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JLabel subtitleLabel = new JLabel("Operations & Management Portal", SwingConstants.CENTER);
        subtitleLabel.setFont(UIConstants.FONT_SMALL);
        subtitleLabel.setForeground(UIConstants.TEXT_MUTED);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        card.add(headerPanel, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(4, 0, 4, 0);

        // Username
        gbc.gridy = 0;
        formPanel.add(UIConstants.createFormLabel("Username"), gbc);

        gbc.gridy = 1;
        usernameField = UIConstants.createTextField();
        usernameField.setText("admin"); // convenient default for quick testing
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridy = 2;
        gbc.insets = new Insets(10, 0, 4, 0);
        formPanel.add(UIConstants.createFormLabel("Password"), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(4, 0, 4, 0);
        passwordField = new JPasswordField("admin123");
        passwordField.setFont(UIConstants.FONT_REGULAR);
        passwordField.setBorder(UIConstants.createFieldBorder());
        passwordField.setPreferredSize(new Dimension(passwordField.getPreferredSize().width, 34));
        formPanel.add(passwordField, gbc);

        // Show/Hide Password Checkbox
        gbc.gridy = 4;
        gbc.insets = new Insets(2, 0, 6, 0);
        JCheckBox showPasswordCheck = new JCheckBox("Show password");
        showPasswordCheck.setFont(UIConstants.FONT_SMALL);
        showPasswordCheck.setOpaque(false);
        showPasswordCheck.setForeground(UIConstants.TEXT_MUTED);
        showPasswordCheck.addActionListener(e -> {
            if (showPasswordCheck.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('•');
            }
        });
        formPanel.add(showPasswordCheck, gbc);

        // Error message feedback label
        gbc.gridy = 5;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UIConstants.FONT_SMALL);
        errorLabel.setForeground(UIConstants.ERROR_RED);
        formPanel.add(errorLabel, gbc);

        // Login Button
        gbc.gridy = 6;
        gbc.insets = new Insets(8, 0, 10, 0);
        loginButton = UIConstants.createPrimaryButton("Sign In to System");
        loginButton.setPreferredSize(new Dimension(loginButton.getPreferredSize().width, 42));
        loginButton.addActionListener(e -> handleLogin());
        formPanel.add(loginButton, gbc);

        // Default Credentials Reminder
        gbc.gridy = 7;
        gbc.insets = new Insets(4, 0, 0, 0);
        JLabel tipLabel = new JLabel("<html><center style='color:#64748B; font-size:11px;'>Default Logins:<br><b>admin</b> / <b>admin123</b> &nbsp;|&nbsp; <b>staff1</b> / <b>staff123</b></center></html>", SwingConstants.CENTER);
        formPanel.add(tipLabel, gbc);

        card.add(formPanel, BorderLayout.CENTER);

        // Enter key submits form
        getRootPane().setDefaultButton(loginButton);

        backgroundPanel.add(card);
        setContentPane(backgroundPanel);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        errorLabel.setText(" ");
        loginButton.setEnabled(false);
        loginButton.setText("Authenticating...");

        // Non-blocking authentication via SwingWorker
        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                loginButton.setText("Sign In to System");
                try {
                    User user = get();
                    if (user != null) {
                        openMainApplication(user);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    errorLabel.setText(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void openMainApplication(User user) {
        dispose();
        MainFrame mainFrame = new MainFrame(user);
        mainFrame.setVisible(true);
    }
}
