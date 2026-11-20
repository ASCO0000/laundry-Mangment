package com.laundry.ui.theme;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.Border;

/**
 * Modern design system constants, color palette, and component factories.
 */
public class UIConstants {

    // Palette
    public static final Color PRIMARY_NAVY = new Color(0x1B, 0x2A, 0x4A);       // Dark Navy Blue
    public static final Color PRIMARY_NAVY_HOVER = new Color(0x24, 0x37, 0x5E);
    public static final Color SECONDARY_BLUE = new Color(0x1E, 0x88, 0xE5);     // Bright Blue
    public static final Color SECONDARY_BLUE_HOVER = new Color(0x19, 0x76, 0xD2);
    public static final Color BG_LIGHT = new Color(0xF4, 0xF6, 0xF9);           // Slate Off-White
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(0xE2, 0xE8, 0xF0);        // Subtle Slate Border
    public static final Color TEXT_PRIMARY = new Color(0x0F, 0x17, 0x2A);       // Charcoal
    public static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);         // Slate Gray
    public static final Color SUCCESS_GREEN = new Color(0x10, 0xB9, 0x81);
    public static final Color WARNING_ORANGE = new Color(0xF5, 0x9E, 0x0B);
    public static final Color ERROR_RED = new Color(0xEF, 0x44, 0x44);

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_METRIC = new Font("Segoe UI", Font.BOLD, 24);

    public static Border createFieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        );
    }

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(PRIMARY_NAVY.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(PRIMARY_NAVY_HOVER);
                } else {
                    g2.setColor(PRIMARY_NAVY);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleButton(btn, Color.WHITE);
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(SECONDARY_BLUE_HOVER);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(0x42, 0xA5, 0xF5));
                } else {
                    g2.setColor(SECONDARY_BLUE);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleButton(btn, Color.WHITE);
        return btn;
    }

    public static JButton createOutlineButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(0xED, 0xF2, 0xF7));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.setColor(CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleButton(btn, TEXT_PRIMARY);
        return btn;
    }

    private static void styleButton(JButton btn, Color textColor) {
        btn.setFont(FONT_BOLD);
        btn.setForeground(textColor);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 20, 36));
    }

    public static JTextField createTextField() {
        JTextField tf = new JTextField();
        tf.setFont(FONT_REGULAR);
        tf.setForeground(TEXT_PRIMARY);
        tf.setBackground(Color.WHITE);
        tf.setBorder(createFieldBorder());
        tf.setPreferredSize(new Dimension(tf.getPreferredSize().width, 34));
        return tf;
    }

    public static JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BOLD);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }
}
