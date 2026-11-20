package com.laundry.ui.components;

import com.laundry.model.enums.DeliveryStatus;
import com.laundry.model.enums.OrderStatus;
import com.laundry.ui.theme.UIConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * Modern JTable with alternating row colors, badge cell rendering, and empty-state feedback.
 */
public class ModernTable extends JTable {

    private String emptyMessage = "No records found";

    public ModernTable(TableModel model) {
        super(model);
        initStyle();
    }

    public ModernTable() {
        super();
        initStyle();
    }

    public void setEmptyMessage(String message) {
        this.emptyMessage = message;
        repaint();
    }

    private void initStyle() {
        setRowHeight(36);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setSelectionBackground(new Color(0xDB, 0xEA, 0xFE));
        setSelectionForeground(UIConstants.PRIMARY_NAVY);
        setFont(UIConstants.FONT_REGULAR);

        // Header Styling
        JTableHeader header = getTableHeader();
        header.setFont(UIConstants.FONT_BOLD);
        header.setBackground(UIConstants.BG_LIGHT);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.CARD_BORDER));

        // Default cell renderer with padding
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF8, 0xFA, 0xFC));
                    c.setForeground(UIConstants.TEXT_PRIMARY);
                }

                // Center align numeric IDs and Badges
                if (value instanceof OrderStatus) {
                    return StatusBadge.of((OrderStatus) value);
                } else if (value instanceof DeliveryStatus) {
                    return StatusBadge.of((DeliveryStatus) value);
                }

                return c;
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getRowCount() == 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            g2.setColor(UIConstants.TEXT_MUTED);
            int textWidth = g2.getFontMetrics().stringWidth(emptyMessage);
            int x = (getWidth() - textWidth) / 2;
            int y = getHeight() / 2;
            g2.drawString(emptyMessage, Math.max(10, x), Math.max(20, y));
            g2.dispose();
        }
    }
}
