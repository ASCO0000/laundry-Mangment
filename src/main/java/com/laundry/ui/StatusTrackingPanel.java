package com.laundry.ui;

import com.laundry.exception.ValidationException;
import com.laundry.model.Order;
import com.laundry.model.OrderStatusHistory;
import com.laundry.model.enums.OrderStatus;
import com.laundry.service.AuthenticationService;
import com.laundry.service.OrderService;
import com.laundry.ui.components.CardPanel;
import com.laundry.ui.components.ModernTable;
import com.laundry.ui.theme.UIConstants;
import com.laundry.util.DateUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Laundry Status Tracking Module: Stage monitoring, transition validation, audit timeline.
 */
public class StatusTrackingPanel extends JPanel {

    private final OrderService orderService = new OrderService();
    private ModernTable trackingTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> statusFilterCombo;
    private JPanel delayedAlertBar;
    private JLabel delayedAlertText;
    private List<Order> currentOrders;

    public StatusTrackingPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(UIConstants.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initHeader();
        initAlertBanner();
        initTableCard();
        loadTrackingDataAsync();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        JLabel title = new JLabel("Order Progress & Status Tracking");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_NAVY);
        JLabel subtitle = new JLabel("Track wash cycles, identify delayed orders, and record staff transitions");
        subtitle.setFont(UIConstants.FONT_REGULAR);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.SOUTH);
        header.add(textPanel, BorderLayout.WEST);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        toolbar.setOpaque(false);

        statusFilterCombo = new JComboBox<>(new String[]{"All", "Received", "Washing", "Drying", "Ironing", "Ready for Pickup", "Delivered", "Cancelled"});
        statusFilterCombo.setPreferredSize(new Dimension(140, 36));
        statusFilterCombo.addActionListener(e -> loadTrackingDataAsync());

        searchField = UIConstants.createTextField();
        searchField.setPreferredSize(new Dimension(180, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search Order # or Name");
        searchField.addActionListener(e -> loadTrackingDataAsync());

        JButton searchBtn = UIConstants.createOutlineButton("🔍 Filter");
        searchBtn.addActionListener(e -> loadTrackingDataAsync());

        JButton refreshBtn = UIConstants.createOutlineButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilterCombo.setSelectedIndex(0);
            loadTrackingDataAsync();
        });

        toolbar.add(statusFilterCombo);
        toolbar.add(searchField);
        toolbar.add(searchBtn);
        toolbar.add(refreshBtn);

        header.add(toolbar, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initAlertBanner() {
        delayedAlertBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        delayedAlertBar.setBackground(new Color(0xFE, 0xE2, 0xE2)); // Light red alert background
        delayedAlertBar.setBorder(BorderFactory.createLineBorder(new Color(0xEF, 0x44, 0x44), 1, true));
        delayedAlertBar.setVisible(false);

        JLabel warningIcon = new JLabel("⚠️");
        warningIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        delayedAlertText = new JLabel("Notice: Orders are past their expected delivery date!");
        delayedAlertText.setFont(UIConstants.FONT_BOLD);
        delayedAlertText.setForeground(new Color(0xB9, 0x1C, 0x1C));

        delayedAlertBar.add(warningIcon);
        delayedAlertBar.add(delayedAlertText);

        add(delayedAlertBar, BorderLayout.BEFORE_FIRST_LINE);
    }

    private void initTableCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 12));

        String[] columns = {"Order ID", "Customer", "Phone", "Order Date", "Expected Delivery", "Schedule Status", "Current Progress"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        trackingTable = new ModernTable(tableModel);
        trackingTable.setEmptyMessage("No orders currently under tracking.");

        JScrollPane scrollPane = new JScrollPane(trackingTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        card.add(scrollPane, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionPanel.setOpaque(false);

        JButton historyBtn = UIConstants.createOutlineButton("📜 View Transition History");
        historyBtn.addActionListener(e -> {
            int row = trackingTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select an order to view its status timeline.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Order o = currentOrders.get(row);
            showHistoryDialog(o);
        });

        JButton updateStatusBtn = UIConstants.createPrimaryButton("🔄 Update Progress Status");
        updateStatusBtn.addActionListener(e -> {
            int row = trackingTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select an order to advance its status.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Order o = currentOrders.get(row);
            showUpdateStatusDialog(o);
        });

        actionPanel.add(historyBtn);
        actionPanel.add(updateStatusBtn);
        card.add(actionPanel, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    public void loadTrackingDataAsync() {
        String status = (String) statusFilterCombo.getSelectedItem();
        String keyword = searchField.getText().trim();

        SwingWorker<List<Order>, Void> worker = new SwingWorker<>() {
            private List<Order> delayed;

            @Override
            protected List<Order> doInBackground() {
                delayed = orderService.getDelayedOrders();
                return orderService.getAllOrders(status, keyword);
            }

            @Override
            protected void done() {
                try {
                    currentOrders = get();
                    tableModel.setRowCount(0);

                    // Update Delayed Banner
                    if (delayed != null && !delayed.isEmpty()) {
                        delayedAlertText.setText("Alert: " + delayed.size() + " active order(s) past expected delivery date! Prioritize processing.");
                        delayedAlertBar.setVisible(true);
                    } else {
                        delayedAlertBar.setVisible(false);
                    }

                    for (Order o : currentOrders) {
                        boolean isDelayed = o.getExpectedDeliveryDate().isBefore(LocalDate.now()) &&
                                           o.getStatus() != OrderStatus.DELIVERED &&
                                           o.getStatus() != OrderStatus.CANCELLED;

                        String scheduleDesc = isDelayed ? "⚠️ OVERDUE" : "On Schedule";

                        tableModel.addRow(new Object[]{
                            "ORD-" + o.getOrderId(),
                            o.getCustomerName(),
                            o.getCustomerPhone(),
                            DateUtil.formatDateTime(o.getOrderDate()),
                            DateUtil.formatDate(o.getExpectedDeliveryDate()),
                            scheduleDesc,
                            o.getStatus()
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StatusTrackingPanel.this, "Error loading status data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showUpdateStatusDialog(Order order) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Update Order Status - ORD-" + order.getOrderId(), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(440, 360);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 15, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(6, 0, 4, 0);

        JLabel currentLabel = new JLabel("Current Status: " + order.getStatus().getDisplayName());
        currentLabel.setFont(UIConstants.FONT_BOLD);
        currentLabel.setForeground(UIConstants.PRIMARY_NAVY);

        JComboBox<OrderStatus> nextStatusCombo = new JComboBox<>(OrderStatus.values());
        nextStatusCombo.setSelectedItem(order.getStatus());

        JTextArea remarksArea = new JTextArea(3, 20);
        remarksArea.setFont(UIConstants.FONT_REGULAR);
        remarksArea.setBorder(UIConstants.createFieldBorder());

        gbc.gridy = 0; content.add(currentLabel, gbc);
        gbc.gridy = 1; content.add(UIConstants.createFormLabel("Select Next Status *"), gbc);
        gbc.gridy = 2; content.add(nextStatusCombo, gbc);
        gbc.gridy = 3; content.add(UIConstants.createFormLabel("Status Change Remarks"), gbc);
        gbc.gridy = 4; content.add(new JScrollPane(remarksArea), gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(UIConstants.BG_LIGHT);

        JButton saveBtn = UIConstants.createPrimaryButton("Confirm & Update");
        JButton cancelBtn = UIConstants.createOutlineButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            OrderStatus selectedStatus = (OrderStatus) nextStatusCombo.getSelectedItem();
            String remarks = remarksArea.getText().trim();
            int currentUserId = AuthenticationService.getCurrentUser() != null ? AuthenticationService.getCurrentUser().getUserId() : 1;

            try {
                orderService.updateOrderStatus(order.getOrderId(), selectedStatus, currentUserId, remarks);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Order status advanced to '" + selectedStatus.getDisplayName() + "' successfully.", "Updated", JOptionPane.INFORMATION_MESSAGE);
                loadTrackingDataAsync();
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(dialog, ve.getMessage(), "Invalid Transition", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showHistoryDialog(Order order) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Order Status Audit Trail - ORD-" + order.getOrderId(), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(620, 420);
        dialog.setLocationRelativeTo(parent);

        List<OrderStatusHistory> history = orderService.getOrderStatusHistory(order.getOrderId());
        String[] cols = {"Changed At", "Previous Stage", "New Stage", "Staff", "Remarks"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);

        for (OrderStatusHistory h : history) {
            model.addRow(new Object[]{
                DateUtil.formatDateTime(h.getChangedAt()),
                h.getOldStatus() != null ? h.getOldStatus() : "-",
                h.getNewStatus(),
                h.getChangedByName() != null ? h.getChangedByName() : "System",
                h.getRemarks() != null ? h.getRemarks() : "-"
            });
        }

        ModernTable table = new ModernTable(model);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bot.setBackground(UIConstants.BG_LIGHT);
        JButton close = UIConstants.createOutlineButton("Close");
        close.addActionListener(e -> dialog.dispose());
        bot.add(close);
        dialog.add(bot, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}
