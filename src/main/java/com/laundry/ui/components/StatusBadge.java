package com.laundry.ui.components;

import com.laundry.model.enums.DeliveryStatus;
import com.laundry.model.enums.OrderStatus;
import com.laundry.ui.theme.UIConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A rounded pill-style badge component that visually identifies order and delivery statuses.
 */
public class StatusBadge extends JLabel {

    private Color bgColor;
    private Color textColor;

    public StatusBadge(String text, Color textColor, Color bgColor) {
        super(text, SwingConstants.CENTER);
        this.textColor = textColor;
        this.bgColor = bgColor;
        setFont(UIConstants.FONT_BOLD);
        setForeground(textColor);
        setOpaque(false);
        setPreferredSize(new Dimension(110, 26));
    }

    public static StatusBadge of(OrderStatus status) {
        return new StatusBadge(status.getDisplayName(), status.getBadgeTextColor(), status.getBadgeBackgroundColor());
    }

    public static StatusBadge of(DeliveryStatus status) {
        return new StatusBadge(status.getDisplayName(), status.getTextColor(), status.getBgColor());
    }

    public void updateStatus(OrderStatus status) {
        setText(status.getDisplayName());
        this.textColor = status.getBadgeTextColor();
        this.bgColor = status.getBadgeBackgroundColor();
        setForeground(textColor);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(bgColor != null ? bgColor : new Color(0xEB, 0xF8, 0xFF));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

        g2.dispose();
        super.paintComponent(g);
    }
}
