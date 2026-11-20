package com.laundry.ui;

import com.laundry.exception.ValidationException;
import com.laundry.model.Customer;
import com.laundry.service.CustomerService;
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
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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
 * Customer Management Module: CRUD, real-time search, table display, and validation.
 */
public class CustomerPanel extends JPanel {

    private final CustomerService customerService = new CustomerService();
    private ModernTable customerTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private List<Customer> customerList;

    public CustomerPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initHeader();
        initTableCard();
        loadCustomersAsync("");
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        // Title and description
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        JLabel title = new JLabel("Customer Management");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_NAVY);
        JLabel subtitle = new JLabel("View, register, and manage laundry shop customer accounts");
        subtitle.setFont(UIConstants.FONT_REGULAR);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.SOUTH);
        header.add(textPanel, BorderLayout.WEST);

        // Action Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        toolbar.setOpaque(false);

        searchField = UIConstants.createTextField();
        searchField.setPreferredSize(new Dimension(220, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name or phone...");
        searchField.addActionListener(e -> filterCustomers());

        JButton searchBtn = UIConstants.createOutlineButton("🔍 Search");
        searchBtn.addActionListener(e -> filterCustomers());

        JButton refreshBtn = UIConstants.createOutlineButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadCustomersAsync("");
        });

        JButton addBtn = UIConstants.createPrimaryButton("➕ Add New Customer");
        addBtn.addActionListener(e -> showCustomerDialog(null));

        toolbar.add(searchField);
        toolbar.add(searchBtn);
        toolbar.add(refreshBtn);
        toolbar.add(addBtn);

        header.add(toolbar, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initTableCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 12));

        String[] columns = {"Customer ID", "Full Name", "Phone Number", "Email Address", "Address", "Date Registered"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customerTable = new ModernTable(tableModel);
        customerTable.setEmptyMessage("No customer accounts registered yet.");

        JScrollPane scrollPane = new JScrollPane(customerTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        card.add(scrollPane, BorderLayout.CENTER);

        // Table Action Buttons (Edit / Delete)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionPanel.setOpaque(false);

        JButton editBtn = UIConstants.createSecondaryButton("✏️ Edit Selected");
        editBtn.addActionListener(e -> {
            int selectedRow = customerTable.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a customer from the table to edit.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Customer customer = customerList.get(selectedRow);
            showCustomerDialog(customer);
        });

        JButton deleteBtn = UIConstants.createOutlineButton("🗑️ Delete Selected");
        deleteBtn.setForeground(UIConstants.ERROR_RED);
        deleteBtn.addActionListener(e -> {
            int selectedRow = customerTable.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a customer to delete.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Customer customer = customerList.get(selectedRow);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete customer '" + customer.getFullName() + "' (#" + customer.getCustomerId() + ")?\nThis action cannot be undone.",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                deleteCustomer(customer.getCustomerId());
            }
        });

        actionPanel.add(editBtn);
        actionPanel.add(deleteBtn);
        card.add(actionPanel, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    private void filterCustomers() {
        String keyword = searchField.getText().trim();
        loadCustomersAsync(keyword);
    }

    public void loadCustomersAsync(String keyword) {
        SwingWorker<List<Customer>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Customer> doInBackground() {
                return customerService.searchCustomers(keyword);
            }

            @Override
            protected void done() {
                try {
                    customerList = get();
                    tableModel.setRowCount(0);
                    for (Customer c : customerList) {
                        tableModel.addRow(new Object[]{
                            "#" + c.getCustomerId(),
                            c.getFullName(),
                            c.getPhone(),
                            c.getEmail() != null ? c.getEmail() : "-",
                            c.getAddress() != null ? c.getAddress() : "-",
                            DateUtil.formatDateTime(c.getCreatedAt())
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CustomerPanel.this, "Error loading customers: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void deleteCustomer(int customerId) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                customerService.deleteCustomer(customerId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(CustomerPanel.this, "Customer deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadCustomersAsync(searchField.getText().trim());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(CustomerPanel.this, cause.getMessage(), "Deletion Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showCustomerDialog(Customer existingCustomer) {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentFrame, existingCustomer == null ? "Add New Customer" : "Edit Customer Details", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(440, 480);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setResizable(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(6, 0, 4, 0);

        JTextField nameField = UIConstants.createTextField();
        JTextField phoneField = UIConstants.createTextField();
        JTextField emailField = UIConstants.createTextField();
        JTextArea addressArea = new JTextArea(3, 20);
        addressArea.setFont(UIConstants.FONT_REGULAR);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        addressArea.setBorder(UIConstants.createFieldBorder());

        if (existingCustomer != null) {
            nameField.setText(existingCustomer.getFullName());
            phoneField.setText(existingCustomer.getPhone());
            emailField.setText(existingCustomer.getEmail());
            addressArea.setText(existingCustomer.getAddress());
        }

        gbc.gridy = 0; content.add(UIConstants.createFormLabel("Full Name *"), gbc);
        gbc.gridy = 1; content.add(nameField, gbc);
        gbc.gridy = 2; content.add(UIConstants.createFormLabel("Phone Number * (10-15 digits)"), gbc);
        gbc.gridy = 3; content.add(phoneField, gbc);
        gbc.gridy = 4; content.add(UIConstants.createFormLabel("Email Address"), gbc);
        gbc.gridy = 5; content.add(emailField, gbc);
        gbc.gridy = 6; content.add(UIConstants.createFormLabel("Address"), gbc);
        gbc.gridy = 7; content.add(new JScrollPane(addressArea), gbc);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(UIConstants.BG_LIGHT);

        JButton saveBtn = UIConstants.createPrimaryButton(existingCustomer == null ? "Save Customer" : "Update Details");
        JButton cancelBtn = UIConstants.createOutlineButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            try {
                if (existingCustomer == null) {
                    Customer c = new Customer();
                    c.setFullName(nameField.getText().trim());
                    c.setPhone(phoneField.getText().trim());
                    c.setEmail(emailField.getText().trim());
                    c.setAddress(addressArea.getText().trim());
                    customerService.addCustomer(c);
                    JOptionPane.showMessageDialog(dialog, "Customer created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    existingCustomer.setFullName(nameField.getText().trim());
                    existingCustomer.setPhone(phoneField.getText().trim());
                    existingCustomer.setEmail(emailField.getText().trim());
                    existingCustomer.setAddress(addressArea.getText().trim());
                    customerService.updateCustomer(existingCustomer);
                    JOptionPane.showMessageDialog(dialog, "Customer updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
                dialog.dispose();
                loadCustomersAsync("");
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
}
