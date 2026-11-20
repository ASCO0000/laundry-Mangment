package com.laundry.ui;

import com.laundry.exception.ValidationException;
import com.laundry.model.Service;
import com.laundry.service.LaundryService;
import com.laundry.ui.components.CardPanel;
import com.laundry.ui.components.ModernTable;
import com.laundry.ui.theme.UIConstants;
import com.laundry.util.ReceiptUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Laundry Service Management Module:
 * Features top summary cards (Total, Active, Inactive, Avg Price in ₹),
 * full CRUD (View, Edit, Activate/Deactivate, Delete), real-time search,
 * and green/gray status badges.
 */
public class ServicePanel extends JPanel {

    private final LaundryService laundryService = new LaundryService();

    // 4 Summary Metric Labels
    private JLabel totalServicesLabel;
    private JLabel activeServicesLabel;
    private JLabel inactiveServicesLabel;
    private JLabel avgPriceLabel;

    private ModernTable serviceTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private List<Service> serviceList;

    public ServicePanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initHeader();

        JPanel centerContainer = new JPanel(new BorderLayout(0, 14));
        centerContainer.setOpaque(false);
        centerContainer.add(buildSummaryCards(), BorderLayout.NORTH);
        centerContainer.add(initTableCard(), BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);

        loadServicesAsync("");
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        JLabel title = new JLabel("Laundry Services");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_NAVY);
        JLabel subtitle = new JLabel("Manage services and pricing");
        subtitle.setFont(UIConstants.FONT_REGULAR);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.SOUTH);
        header.add(textPanel, BorderLayout.WEST);

        // Header Toolbar: Search field, Refresh, Add Service
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        toolbar.setOpaque(false);

        searchField = UIConstants.createTextField();
        searchField.setPreferredSize(new Dimension(220, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search service name or desc...");
        searchField.addActionListener(e -> filterServices());

        JButton searchBtn = UIConstants.createOutlineButton("Search");
        searchBtn.addActionListener(e -> filterServices());

        JButton refreshBtn = UIConstants.createOutlineButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadServicesAsync("");
        });

        JButton addBtn = UIConstants.createPrimaryButton("+ Add Service");
        addBtn.addActionListener(e -> showAddOrEditDialog(null));

        toolbar.add(searchField);
        toolbar.add(searchBtn);
        toolbar.add(refreshBtn);
        toolbar.add(addBtn);

        header.add(toolbar, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private JPanel buildSummaryCards() {
        JPanel summaryGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        summaryGrid.setOpaque(false);

        totalServicesLabel = new JLabel("0");
        activeServicesLabel = new JLabel("0");
        inactiveServicesLabel = new JLabel("0");
        avgPriceLabel = new JLabel("₹0.00");

        summaryGrid.add(createCard("Total Services", totalServicesLabel, "🏷️", UIConstants.PRIMARY_NAVY));
        summaryGrid.add(createCard("Active Services", activeServicesLabel, "✅", UIConstants.SUCCESS_GREEN));
        summaryGrid.add(createCard("Inactive Services", inactiveServicesLabel, "⏸️", UIConstants.TEXT_MUTED));
        summaryGrid.add(createCard("Average Service Price", avgPriceLabel, "💰", UIConstants.SECONDARY_BLUE));

        return summaryGrid;
    }

    private JPanel createCard(String title, JLabel valueLabel, String icon, Color accentColor) {
        CardPanel card = new CardPanel(new BorderLayout(0, 6));
        card.setPreferredSize(new Dimension(170, 90));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel tLabel = new JLabel(title);
        tLabel.setFont(UIConstants.FONT_SMALL);
        tLabel.setForeground(UIConstants.TEXT_MUTED);

        JLabel iLabel = new JLabel(icon);
        iLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        top.add(tLabel, BorderLayout.WEST);
        top.add(iLabel, BorderLayout.EAST);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(accentColor);

        card.add(top, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel initTableCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 12));

        String[] columns = {"Service ID", "Service Name", "Description", "Pricing Unit", "Price", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        serviceTable = new ModernTable(tableModel);
        serviceTable.setEmptyMessage("No laundry services found matching the criteria.");

        // Custom Renderer for Status (Green for Active, Gray for Inactive)
        serviceTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(UIConstants.FONT_BOLD);

                String status = value != null ? value.toString() : "Inactive";
                if ("Active".equalsIgnoreCase(status)) {
                    lbl.setForeground(new Color(0x05, 0x96, 0x69)); // Emerald Green
                    lbl.setText("● Active");
                } else {
                    lbl.setForeground(new Color(0x64, 0x74, 0x8B)); // Slate Gray
                    lbl.setText("○ Inactive");
                }
                return lbl;
            }
        });

        JScrollPane scrollPane = new JScrollPane(serviceTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        card.add(scrollPane, BorderLayout.CENTER);

        // Action Buttons: View, Edit, Activate/Deactivate, Delete
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionPanel.setOpaque(false);

        JButton viewBtn = UIConstants.createOutlineButton("👁️ View Details");
        viewBtn.addActionListener(e -> {
            Service selected = getSelectedService();
            if (selected != null) {
                showViewDetailsDialog(selected);
            }
        });

        JButton editBtn = UIConstants.createSecondaryButton("✏️ Edit");
        editBtn.addActionListener(e -> {
            Service selected = getSelectedService();
            if (selected != null) {
                showAddOrEditDialog(selected);
            }
        });

        JButton toggleBtn = UIConstants.createOutlineButton("⚡ Activate / Deactivate");
        toggleBtn.addActionListener(e -> {
            Service selected = getSelectedService();
            if (selected != null) {
                boolean newStatus = !selected.isActive();
                laundryService.toggleServiceStatus(selected.getServiceId(), newStatus);
                String msg = newStatus ? "Service '" + selected.getServiceName() + "' is now Active."
                                       : "Service '" + selected.getServiceName() + "' is now Inactive.";
                JOptionPane.showMessageDialog(this, msg, "Status Updated", JOptionPane.INFORMATION_MESSAGE);
                loadServicesAsync(searchField.getText().trim());
            }
        });

        JButton deleteBtn = UIConstants.createOutlineButton("🗑️ Delete");
        deleteBtn.setForeground(UIConstants.ERROR_RED);
        deleteBtn.addActionListener(e -> {
            Service selected = getSelectedService();
            if (selected != null) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete service '" + selected.getServiceName() + "' (ID: #" + selected.getServiceId() + ")?\n"
                      + "If this service is referenced in past orders, it cannot be permanently deleted and should be deactivated instead.",
                        "Confirm Service Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    deleteService(selected.getServiceId());
                }
            }
        });

        actionPanel.add(viewBtn);
        actionPanel.add(editBtn);
        actionPanel.add(toggleBtn);
        actionPanel.add(deleteBtn);
        card.add(actionPanel, BorderLayout.SOUTH);

        return card;
    }

    private Service getSelectedService() {
        int row = serviceTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a service from the table first.", "Notice", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return serviceList.get(row);
    }

    private void filterServices() {
        String keyword = searchField.getText().trim();
        loadServicesAsync(keyword);
    }

    public void loadServicesAsync(String keyword) {
        SwingWorker<ServiceData, Void> worker = new SwingWorker<>() {
            @Override
            protected ServiceData doInBackground() {
                ServiceData data = new ServiceData();
                data.services = laundryService.searchServices(keyword);
                data.summary = laundryService.getServiceSummary();
                return data;
            }

            @Override
            protected void done() {
                try {
                    ServiceData data = get();
                    serviceList = data.services;

                    // 1. Update 4 Summary KPI Cards
                    totalServicesLabel.setText(String.valueOf(data.summary.get("totalServices")));
                    activeServicesLabel.setText(String.valueOf(data.summary.get("activeServices")));
                    inactiveServicesLabel.setText(String.valueOf(data.summary.get("inactiveServices")));
                    avgPriceLabel.setText(ReceiptUtil.formatCurrency((BigDecimal) data.summary.get("averagePrice")));

                    // 2. Update Table Rows
                    tableModel.setRowCount(0);
                    for (Service s : serviceList) {
                        tableModel.addRow(new Object[]{
                            "#" + s.getServiceId(),
                            s.getServiceName(),
                            s.getDescription() != null && !s.getDescription().isEmpty() ? s.getDescription() : "-",
                            s.getPricingUnit(),
                            ReceiptUtil.formatCurrency(s.getPrice()),
                            s.isActive() ? "Active" : "Inactive"
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ServicePanel.this, "Error loading services: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void deleteService(int serviceId) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                laundryService.deleteService(serviceId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(ServicePanel.this, "Service deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                    loadServicesAsync(searchField.getText().trim());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(ServicePanel.this, cause.getMessage(), "Cannot Delete", JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showViewDetailsDialog(Service service) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Service Details - " + service.getServiceName(), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(440, 340);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 15, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(6, 4, 6, 4);

        gbc.gridx = 0; gbc.gridy = 0; content.add(new JLabel("Service ID:"), gbc);
        gbc.gridx = 1; content.add(new JLabel("#" + service.getServiceId()), gbc);

        gbc.gridx = 0; gbc.gridy = 1; content.add(new JLabel("Service Name:"), gbc);
        JLabel nameLbl = new JLabel(service.getServiceName());
        nameLbl.setFont(UIConstants.FONT_BOLD);
        gbc.gridx = 1; content.add(nameLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 2; content.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; content.add(new JLabel("<html>" + (service.getDescription() != null ? service.getDescription() : "None") + "</html>"), gbc);

        gbc.gridx = 0; gbc.gridy = 3; content.add(new JLabel("Pricing Unit:"), gbc);
        gbc.gridx = 1; content.add(new JLabel(service.getPricingUnit()), gbc);

        gbc.gridx = 0; gbc.gridy = 4; content.add(new JLabel("Rate / Price:"), gbc);
        JLabel priceLbl = new JLabel(ReceiptUtil.formatCurrency(service.getPrice()));
        priceLbl.setFont(UIConstants.FONT_BOLD);
        priceLbl.setForeground(UIConstants.PRIMARY_NAVY);
        gbc.gridx = 1; content.add(priceLbl, gbc);

        gbc.gridx = 0; gbc.gridy = 5; content.add(new JLabel("Availability Status:"), gbc);
        JLabel statusLbl = new JLabel(service.isActive() ? "Active (Available to Customers)" : "Inactive (Hidden from Orders)");
        statusLbl.setForeground(service.isActive() ? UIConstants.SUCCESS_GREEN : UIConstants.TEXT_MUTED);
        statusLbl.setFont(UIConstants.FONT_BOLD);
        gbc.gridx = 1; content.add(statusLbl, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(UIConstants.BG_LIGHT);
        JButton closeBtn = UIConstants.createOutlineButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(closeBtn);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showAddOrEditDialog(Service existingService) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, existingService == null ? "Add Laundry Service" : "Edit Laundry Service", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(480, 480);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 15, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(6, 0, 4, 0);

        JTextField nameField = UIConstants.createTextField();
        JTextArea descArea = new JTextArea(3, 20);
        descArea.setFont(UIConstants.FONT_REGULAR);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(UIConstants.createFieldBorder());

        JComboBox<String> unitCombo = new JComboBox<>(new String[]{
            "Per Kg", "Per Item", "Per Load", "Per Pair", "Fixed Fee"
        });

        JTextField priceField = UIConstants.createTextField();
        JCheckBox activeCheck = new JCheckBox("Available for Customer Orders (Active)", true);
        activeCheck.setFont(UIConstants.FONT_REGULAR);
        activeCheck.setOpaque(false);

        if (existingService != null) {
            nameField.setText(existingService.getServiceName());
            descArea.setText(existingService.getDescription());
            unitCombo.setSelectedItem(existingService.getPricingUnit());
            priceField.setText(existingService.getPrice() != null ? existingService.getPrice().toPlainString() : "");
            activeCheck.setSelected(existingService.isActive());
        }

        gbc.gridy = 0; content.add(UIConstants.createFormLabel("Service Name *"), gbc);
        gbc.gridy = 1; content.add(nameField, gbc);
        gbc.gridy = 2; content.add(UIConstants.createFormLabel("Description"), gbc);
        gbc.gridy = 3; content.add(new JScrollPane(descArea), gbc);
        gbc.gridy = 4; content.add(UIConstants.createFormLabel("Pricing Unit *"), gbc);
        gbc.gridy = 5; content.add(unitCombo, gbc);
        gbc.gridy = 6; content.add(UIConstants.createFormLabel("Rate / Price (₹) *"), gbc);
        gbc.gridy = 7; content.add(priceField, gbc);
        gbc.gridy = 8; content.add(activeCheck, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(UIConstants.BG_LIGHT);

        JButton saveBtn = UIConstants.createPrimaryButton(existingService == null ? "Save Service" : "Update Service");
        JButton cancelBtn = UIConstants.createOutlineButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            try {
                String priceStr = priceField.getText().trim();
                BigDecimal price;
                try {
                    price = new BigDecimal(priceStr);
                } catch (NumberFormatException ex) {
                    throw new ValidationException("Please enter a valid numeric price amount in ₹.");
                }

                if (existingService == null) {
                    Service s = new Service();
                    s.setServiceName(nameField.getText().trim());
                    s.setDescription(descArea.getText().trim());
                    s.setPricingUnit((String) unitCombo.getSelectedItem());
                    s.setPrice(price);
                    s.setActive(activeCheck.isSelected());
                    laundryService.addService(s);
                    JOptionPane.showMessageDialog(dialog, "Service added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    existingService.setServiceName(nameField.getText().trim());
                    existingService.setDescription(descArea.getText().trim());
                    existingService.setPricingUnit((String) unitCombo.getSelectedItem());
                    existingService.setPrice(price);
                    existingService.setActive(activeCheck.isSelected());
                    laundryService.updateService(existingService);
                    JOptionPane.showMessageDialog(dialog, "Service updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
                dialog.dispose();
                loadServicesAsync("");
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(dialog, ve.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Operation failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static class ServiceData {
        List<Service> services;
        Map<String, Object> summary;
    }
}
