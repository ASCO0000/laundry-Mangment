package com.laundry.ui.components;

import com.laundry.ui.theme.UIConstants;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * A modern card panel with rounded corners, white background, and subtle borders.
 */
public class CardPanel extends JPanel {

    private int cornerRadius = 12;

    public CardPanel() {
        this(new BorderLayout());
    }

    public CardPanel(java.awt.LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));
    }

    public CardPanel(int padding) {
        this(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill White Card Background
        g2.setColor(UIConstants.CARD_BG);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

        // Draw Subtle Slate Border
        g2.setColor(UIConstants.CARD_BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);
    }
}
