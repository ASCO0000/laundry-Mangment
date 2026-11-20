package com.laundry.ui;

import com.laundry.dao.UserDAO;
import com.laundry.exception.ValidationException;
import com.laundry.model.Delivery;
import com.laundry.model.User;
import com.laundry.model.enums.DeliveryStatus;
import com.laundry.model.enums.DeliveryType;
import com.laundry.service.DeliveryService;
import com.laundry.ui.components.CardPanel;
import com.laundry.ui.components.ModernTable;
import com.laundry.ui.theme.UIConstants;
import com.laundry.util.DateUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
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
 * Pickup and Delivery Management Module: Transport dispatch, staff assignment, and route statuses.
 */
public class DeliveryPanel extends JPanel {

    private final DeliveryService deliveryService = new DeliveryService();
    private final UserDAO userDAO = new UserDAO();

    private ModernTable deliveryTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> typeFilterCombo;
    private JComboBox<String> statusFilterCombo;
    private List<Delivery> currentDeliveries;

    public DeliveryPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initHeader();
        initTableCard();
        loadDeliveriesAsync();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        JLabel title = new JLabel("Pickup & Delivery Logistics");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_NAVY);
        JLabel subtitle = new JLabel("Schedule customer pickups, assign drivers, and manage drop-off tasks");
        subtitle.setFont(UIConstants.FONT_REGULAR);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.SOUTH);
        header.add(textPanel, BorderLayout.WEST);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        toolbar.setOpaque(false);

        typeFilterCombo = new JComboBox<>(new String[]{"All Types", "Pickup", "Delivery"});
        typeFilterCombo.setPreferredSize(new Dimension(120, 36));
        typeFilterCombo.addActionListener(e -> loadDeliveriesAsync());

        statusFilterCombo = new JComboBox<>(new String[]{"All Statuses", "Scheduled", "Assigned", "Picked Up", "In Transit", "Delivered", "Failed"});
        statusFilterCombo.setPreferredSize(new Dimension(130, 36));
        statusFilterCombo.addActionListener(e -> loadDeliveriesAsync());

        JButton refreshBtn = UIConstants.createOutlineButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> {
            typeFilterCombo.setSelectedIndex(0);
            statusFilterCombo.setSelectedIndex(0);
            loadDeliveriesAsync();
        });

        JButton scheduleBtn = UIConstants.createPrimaryButton("➕ Schedule Transport");
        scheduleBtn.addActionListener(e -> showScheduleDialog());

        toolbar.add(typeFilterCombo);
        toolbar.add(statusFilterCombo);
        toolbar.add(refreshBtn);
        toolbar.add(scheduleBtn);

        header.add(toolbar, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initTableCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 12));

        String[] cols = {"Task ID", "Order ID", "Customer Name", "Phone", "Type", "Scheduled Time", "Assigned Staff", "Address", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        deliveryTable = new ModernTable(tableModel);
        deliveryTable.setEmptyMessage("No pickup or delivery schedules found.");

        JScrollPane scrollPane = new JScrollPane(deliveryTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        card.add(scrollPane, BorderLayout.CENTER);

        // Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionPanel.setOpaque(false);

        JButton updateStatusBtn = UIConstants.createPrimaryButton("🔄 Update Delivery Status");
        updateStatusBtn.addActionListener(e -> {
            int row = deliveryTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a delivery task from the table.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Delivery d = currentDeliveries.get(row);
            showUpdateStatusDialog(d);
        });

        actionPanel.add(updateStatusBtn);
        card.add(actionPanel, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    public void loadDeliveriesAsync() {
        String type = (String) typeFilterCombo.getSelectedItem();
        if ("All Types".equals(type)) type = "All";
        String status = (String) statusFilterCombo.getSelectedItem();
        if ("All Statuses".equals(status)) status = "All";

        final String finalType = type;
        final String finalStatus = status;

        SwingWorker<List<Delivery>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Delivery> doInBackground() {
                return deliveryService.getAllDeliveries(finalType, finalStatus);
            }

            @Override
            protected void done() {
                try {
                    currentDeliveries = get();
                    tableModel.setRowCount(0);
                    for (Delivery d : currentDeliveries) {
                        tableModel.addRow(new Object[]{
                            "#" + d.getDeliveryId(),
                            "ORD-" + d.getOrderId(),
                            d.getCustomerName(),
                            d.getCustomerPhone(),
                            d.getDeliveryType().getDisplayName(),
                            DateUtil.formatDateTime(d.getScheduledDatetime()),
                            d.getAssignedStaffName() != null ? d.getAssignedStaffName() : "Unassigned",
                            d.getAddress(),
                            d.getStatus()
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DeliveryPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showScheduleDialog() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Schedule Transport Task", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(480, 480);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 15, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(6, 0, 4, 0);

        JTextField orderIdField = UIConstants.createTextField();
        orderIdField.putClientProperty("JTextField.placeholderText", "e.g. 101");

        JComboBox<DeliveryType> typeCombo = new JComboBox<>(DeliveryType.values());
        JTextArea addressArea = new JTextArea(3, 20);
        addressArea.setFont(UIConstants.FONT_REGULAR);
        addressArea.setBorder(UIConstants.createFieldBorder());

        JTextField datetimeField = UIConstants.createTextField();
        datetimeField.setText(DateUtil.formatDateTime(LocalDateTime.now().plusHours(4)));

        List<User> staffList = userDAO.findAllStaff();
        JComboBox<User> staffCombo = new JComboBox<>(staffList.toArray(new User[0]));

        gbc.gridy = 0; content.add(UIConstants.createFormLabel("Associated Order ID # *"), gbc);
        gbc.gridy = 1; content.add(orderIdField, gbc);
        gbc.gridy = 2; content.add(UIConstants.createFormLabel("Transport Type *"), gbc);
        gbc.gridy = 3; content.add(typeCombo, gbc);
        gbc.gridy = 4; content.add(UIConstants.createFormLabel("Address *"), gbc);
        gbc.gridy = 5; content.add(new JScrollPane(addressArea), gbc);
        gbc.gridy = 6; content.add(UIConstants.createFormLabel("Assign Staff Member"), gbc);
        gbc.gridy = 7; content.add(staffCombo, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(UIConstants.BG_LIGHT);

        JButton saveBtn = UIConstants.createPrimaryButton("Save Schedule");
        JButton cancelBtn = UIConstants.createOutlineButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            try {
                int orderId = Integer.parseInt(orderIdField.getText().trim().replace("ORD-", ""));
                String addr = addressArea.getText().trim();
                DeliveryType type = (DeliveryType) typeCombo.getSelectedItem();
                User staff = (User) staffCombo.getSelectedItem();

                Delivery d = new Delivery();
                d.setOrderId(orderId);
                d.setDeliveryType(type);
                d.setAddress(addr);
                d.setScheduledDatetime(LocalDateTime.now().plusHours(4));
                d.setAssignedStaffId(staff != null ? staff.getUserId() : null);
                d.setStatus(DeliveryStatus.SCHEDULED);

                deliveryService.scheduleDelivery(d);
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Transport schedule created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadDeliveriesAsync();
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(dialog, ve.getMessage(), "Validation Notice", JOptionPane.WARNING_MESSAGE);
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

    private void showUpdateStatusDialog(Delivery delivery) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Update Transport Status - #" + delivery.getDeliveryId(), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 240);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 15, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(6, 0, 4, 0);

        JComboBox<DeliveryStatus> statusCombo = new JComboBox<>(DeliveryStatus.values());
        statusCombo.setSelectedItem(delivery.getStatus());

        gbc.gridy = 0; content.add(UIConstants.createFormLabel("Select Updated Status *"), gbc);
        gbc.gridy = 1; content.add(statusCombo, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(UIConstants.BG_LIGHT);

        JButton saveBtn = UIConstants.createPrimaryButton("Update");
        JButton cancelBtn = UIConstants.createOutlineButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            DeliveryStatus s = (DeliveryStatus) statusCombo.getSelectedItem();
            deliveryService.updateDeliveryStatus(delivery.getDeliveryId(), s);
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "Status updated to " + s.getDisplayName(), "Updated", JOptionPane.INFORMATION_MESSAGE);
            loadDeliveriesAsync();
        });

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
