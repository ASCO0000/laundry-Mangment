package com.laundry.ui;

import com.laundry.exception.ValidationException;
import com.laundry.model.Customer;
import com.laundry.model.Order;
import com.laundry.model.OrderItem;
import com.laundry.model.Service;
import com.laundry.model.enums.OrderStatus;
import com.laundry.service.AuthenticationService;
import com.laundry.service.BillingService;
import com.laundry.service.CustomerService;
import com.laundry.service.LaundryService;
import com.laundry.service.OrderService;
import com.laundry.ui.components.CardPanel;
import com.laundry.ui.components.ModernTable;
import com.laundry.ui.components.ReceiptDialog;
import com.laundry.ui.theme.UIConstants;
import com.laundry.util.DateUtil;
import com.laundry.util.ReceiptUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Order Management Module: Interactive order registration, dynamic calculation, receipt preview.
 */
public class OrderPanel extends JPanel {

    private final OrderService orderService = new OrderService();
    private final CustomerService customerService = new CustomerService();
    private final LaundryService laundryService = new LaundryService();
    private final BillingService billingService = new BillingService();

    private ModernTable orderTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> statusFilterCombo;
    private List<Order> currentOrders;

    public OrderPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initHeader();
        initTableCard();
        loadOrdersAsync();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        JLabel title = new JLabel("Laundry Order Management");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_NAVY);
        JLabel subtitle = new JLabel("Register orders, track customer garments, and generate slips");
        subtitle.setFont(UIConstants.FONT_REGULAR);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.SOUTH);
        header.add(textPanel, BorderLayout.WEST);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        toolbar.setOpaque(false);

        statusFilterCombo = new JComboBox<>(new String[]{"All", "Received", "Washing", "Drying", "Ironing", "Ready for Pickup", "Delivered", "Cancelled"});
        statusFilterCombo.setPreferredSize(new Dimension(140, 36));
        statusFilterCombo.addActionListener(e -> loadOrdersAsync());

        searchField = UIConstants.createTextField();
        searchField.setPreferredSize(new Dimension(180, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Search Order # or Name");
        searchField.addActionListener(e -> loadOrdersAsync());

        JButton searchBtn = UIConstants.createOutlineButton("🔍 Filter");
        searchBtn.addActionListener(e -> loadOrdersAsync());

        JButton refreshBtn = UIConstants.createOutlineButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilterCombo.setSelectedIndex(0);
            loadOrdersAsync();
        });

        JButton newOrderBtn = UIConstants.createPrimaryButton("➕ Create New Order");
        newOrderBtn.addActionListener(e -> showCreateOrderDialog());

        toolbar.add(statusFilterCombo);
        toolbar.add(searchField);
        toolbar.add(searchBtn);
        toolbar.add(refreshBtn);
        toolbar.add(newOrderBtn);

        header.add(toolbar, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initTableCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 12));

        String[] columns = {"Order ID", "Customer", "Phone", "Order Date", "Delivery Date", "Total Amount", "Paid", "Balance Due", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        orderTable = new ModernTable(tableModel);
        orderTable.setEmptyMessage("No orders found matching the filter criteria.");

        JScrollPane scrollPane = new JScrollPane(orderTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        card.add(scrollPane, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionPanel.setOpaque(false);

        JButton receiptBtn = UIConstants.createSecondaryButton("🧾 Preview / Print Receipt");
        receiptBtn.addActionListener(e -> {
            int row = orderTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select an order from the table.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Order o = currentOrders.get(row);
            String html = billingService.generateReceiptHtml(o.getOrderId(), null);
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            ReceiptDialog dlg = new ReceiptDialog(topFrame, html);
            dlg.setVisible(true);
        });

        JButton viewDetailsBtn = UIConstants.createOutlineButton("👁️ View Order Items");
        viewDetailsBtn.addActionListener(e -> {
            int row = orderTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select an order to view items.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Order o = currentOrders.get(row);
            showOrderItemsDialog(o);
        });

        actionPanel.add(viewDetailsBtn);
        actionPanel.add(receiptBtn);
        card.add(actionPanel, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    public void loadOrdersAsync() {
        String status = (String) statusFilterCombo.getSelectedItem();
        String keyword = searchField.getText().trim();

        SwingWorker<List<Order>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Order> doInBackground() {
                return orderService.getAllOrders(status, keyword);
            }

            @Override
            protected void done() {
                try {
                    currentOrders = get();
                    tableModel.setRowCount(0);
                    for (Order o : currentOrders) {
                        tableModel.addRow(new Object[]{
                            "ORD-" + o.getOrderId(),
                            o.getCustomerName(),
                            o.getCustomerPhone(),
                            DateUtil.formatDateTime(o.getOrderDate()),
                            DateUtil.formatDate(o.getExpectedDeliveryDate()),
                            ReceiptUtil.formatCurrency(o.getTotalAmount()),
                            ReceiptUtil.formatCurrency(o.getPaidAmount()),
                            ReceiptUtil.formatCurrency(o.getBalanceDue()),
                            o.getStatus()
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(OrderPanel.this, "Error loading orders: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showOrderItemsDialog(Order orderSummary) {
        Order detailedOrder = orderService.getOrderById(orderSummary.getOrderId());
        if (detailedOrder == null) return;

        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(parent, "Order Details - ORD-" + detailedOrder.getOrderId(), true);
        dlg.setLayout(new BorderLayout());
        dlg.setSize(580, 420);
        dlg.setLocationRelativeTo(parent);

        String[] cols = {"Service", "Description", "Qty", "Unit Rate", "Subtotal"};
        DefaultTableModel itemsModel = new DefaultTableModel(cols, 0);
        for (OrderItem it : detailedOrder.getItems()) {
            itemsModel.addRow(new Object[]{
                it.getServiceName(),
                it.getItemDescription(),
                it.getQuantity(),
                ReceiptUtil.formatCurrency(it.getUnitPrice()),
                ReceiptUtil.formatCurrency(it.getSubtotal())
            });
        }
        ModernTable itemsTable = new ModernTable(itemsModel);

        JPanel topInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topInfo.setBackground(UIConstants.BG_LIGHT);
        topInfo.add(new JLabel("Customer: " + detailedOrder.getCustomerName()));
        topInfo.add(new JLabel("Expected: " + DateUtil.formatDate(detailedOrder.getExpectedDeliveryDate())));
        topInfo.add(new JLabel("Total: " + ReceiptUtil.formatCurrency(detailedOrder.getTotalAmount())));

        dlg.add(topInfo, BorderLayout.NORTH);
        dlg.add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        JButton close = UIConstants.createOutlineButton("Close");
        close.addActionListener(e -> dlg.dispose());
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bot.setBackground(UIConstants.BG_LIGHT);
        bot.add(close);
        dlg.add(bot, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void showCreateOrderDialog() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Create New Laundry Order", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(740, 620);
        dialog.setLocationRelativeTo(parent);

        List<Customer> customers = customerService.getAllCustomers();
        List<Service> services = laundryService.getActiveServices();

        if (customers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No customers found. Please add a customer first.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (services.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No active laundry services found. Please activate services first.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel formHeader = new JPanel(new GridBagLayout());
        formHeader.setBackground(Color.WHITE);
        formHeader.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);

        JComboBox<Customer> customerCombo = new JComboBox<>(customers.toArray(new Customer[0]));
        JTextField deliveryDateField = UIConstants.createTextField();
        deliveryDateField.setText(LocalDate.now().plusDays(2).toString());

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.6;
        formHeader.add(UIConstants.createFormLabel("Select Customer *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.4;
        formHeader.add(UIConstants.createFormLabel("Expected Delivery Date (YYYY-MM-DD) *"), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formHeader.add(customerCombo, gbc);
        gbc.gridx = 1;
        formHeader.add(deliveryDateField, gbc);

        // Line Items Area
        JPanel itemsPanel = new JPanel(new BorderLayout(0, 8));
        itemsPanel.setBackground(Color.WHITE);
        itemsPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        JLabel itemsTitle = new JLabel("Order Line Items");
        itemsTitle.setFont(UIConstants.FONT_HEADER);
        itemsTitle.setForeground(UIConstants.PRIMARY_NAVY);
        itemsPanel.add(itemsTitle, BorderLayout.NORTH);

        String[] itemCols = {"Service", "Description", "Quantity", "Unit Rate", "Subtotal"};
        DefaultTableModel orderItemsModel = new DefaultTableModel(itemCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        ModernTable itemsTable = new ModernTable(orderItemsModel);
        itemsPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        List<OrderItem> pendingItems = new ArrayList<>();

        // Add Item Form Strip
        JPanel addItemStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        addItemStrip.setBackground(UIConstants.BG_LIGHT);
        JComboBox<Service> serviceCombo = new JComboBox<>(services.toArray(new Service[0]));
        JTextField descField = UIConstants.createTextField();
        descField.setPreferredSize(new Dimension(140, 32));
        descField.putClientProperty("JTextField.placeholderText", "Item Description");

        JTextField qtyField = UIConstants.createTextField();
        qtyField.setPreferredSize(new Dimension(60, 32));
        qtyField.setText("1.0");

        JLabel rateLabel = new JLabel("₹0.00");
        rateLabel.setFont(UIConstants.FONT_BOLD);
        rateLabel.setForeground(UIConstants.SECONDARY_BLUE);

        serviceCombo.addActionListener(e -> {
            Service s = (Service) serviceCombo.getSelectedItem();
            if (s != null) {
                rateLabel.setText(ReceiptUtil.formatCurrency(s.getPrice()));
            }
        });
        if (!services.isEmpty()) {
            rateLabel.setText(ReceiptUtil.formatCurrency(services.get(0).getPrice()));
        }

        JLabel subtotalDisplay = new JLabel("Subtotal: ₹0.00");
        subtotalDisplay.setFont(UIConstants.FONT_BOLD);
        JLabel totalDisplay = new JLabel("Total: ₹0.00");
        totalDisplay.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        totalDisplay.setForeground(UIConstants.PRIMARY_NAVY);

        JTextField discountField = UIConstants.createTextField();
        discountField.setPreferredSize(new Dimension(80, 32));
        discountField.setText("0.00");

        Runnable updateTotalsRunnable = () -> {
            BigDecimal subtotal = BigDecimal.ZERO;
            for (OrderItem it : pendingItems) {
                subtotal = subtotal.add(it.getSubtotal());
            }
            subtotalDisplay.setText("Subtotal: " + ReceiptUtil.formatCurrency(subtotal));
            BigDecimal discount = BigDecimal.ZERO;
            try {
                discount = new BigDecimal(discountField.getText().trim());
            } catch (Exception ignored) {}
            BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);
            totalDisplay.setText("Total: " + ReceiptUtil.formatCurrency(total));
        };

        discountField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateTotalsRunnable.run(); }
            public void removeUpdate(DocumentEvent e) { updateTotalsRunnable.run(); }
            public void changedUpdate(DocumentEvent e) { updateTotalsRunnable.run(); }
        });

        JButton addItemBtn = UIConstants.createPrimaryButton("+ Add Item");
        addItemBtn.setPreferredSize(new Dimension(100, 32));
        addItemBtn.addActionListener(e -> {
            Service selectedService = (Service) serviceCombo.getSelectedItem();
            if (selectedService == null) return;

            String desc = descField.getText().trim();
            if (desc.isEmpty()) {
                desc = selectedService.getServiceName();
            }

            BigDecimal qty;
            try {
                qty = new BigDecimal(qtyField.getText().trim());
                if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid positive quantity.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            OrderItem item = new OrderItem();
            item.setServiceId(selectedService.getServiceId());
            item.setServiceName(selectedService.getServiceName());
            item.setItemDescription(desc);
            item.setQuantity(qty);
            item.setUnitPrice(selectedService.getPrice());
            item.calculateSubtotal();

            pendingItems.add(item);
            orderItemsModel.addRow(new Object[]{
                item.getServiceName(),
                item.getItemDescription(),
                item.getQuantity(),
                ReceiptUtil.formatCurrency(item.getUnitPrice()),
                ReceiptUtil.formatCurrency(item.getSubtotal())
            });

            descField.setText("");
            qtyField.setText("1.0");
            updateTotalsRunnable.run();
        });

        JButton removeItemBtn = UIConstants.createOutlineButton("Remove Item");
        removeItemBtn.setPreferredSize(new Dimension(110, 32));
        removeItemBtn.addActionListener(e -> {
            int selected = itemsTable.getSelectedRow();
            if (selected >= 0) {
                pendingItems.remove(selected);
                orderItemsModel.removeRow(selected);
                updateTotalsRunnable.run();
            }
        });

        addItemStrip.add(serviceCombo);
        addItemStrip.add(descField);
        addItemStrip.add(new JLabel("Qty:"));
        addItemStrip.add(qtyField);
        addItemStrip.add(rateLabel);
        addItemStrip.add(addItemBtn);
        addItemStrip.add(removeItemBtn);

        itemsPanel.add(addItemStrip, BorderLayout.SOUTH);

        // Footer Instructions & Summary
        JPanel footer = new JPanel(new BorderLayout(15, 0));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 20, 15, 20));

        JTextArea instructionsArea = new JTextArea(2, 20);
        instructionsArea.setFont(UIConstants.FONT_REGULAR);
        instructionsArea.setBorder(UIConstants.createFieldBorder());
        JPanel instrPanel = new JPanel(new BorderLayout());
        instrPanel.setOpaque(false);
        instrPanel.add(UIConstants.createFormLabel("Special Instructions / Garment Notes"), BorderLayout.NORTH);
        instrPanel.add(new JScrollPane(instructionsArea), BorderLayout.CENTER);
        footer.add(instrPanel, BorderLayout.CENTER);

        JPanel totalsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        totalsPanel.setOpaque(false);
        totalsPanel.add(subtotalDisplay);
        totalsPanel.add(new JLabel("Discount (₹):"));
        totalsPanel.add(discountField);
        totalsPanel.add(totalDisplay);
        footer.add(totalsPanel, BorderLayout.SOUTH);

        // Action Buttons
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        actionRow.setBackground(UIConstants.BG_LIGHT);

        JButton saveOrderBtn = UIConstants.createPrimaryButton("Save & Book Order");
        JButton cancelBtn = UIConstants.createOutlineButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveOrderBtn.addActionListener(e -> {
            try {
                if (pendingItems.isEmpty()) {
                    throw new ValidationException("Please add at least one line item to the order.");
                }

                LocalDate expDate = DateUtil.parseDate(deliveryDateField.getText().trim());
                if (expDate == null) {
                    throw new ValidationException("Please enter a valid expected delivery date (YYYY-MM-DD).");
                }

                Customer customer = (Customer) customerCombo.getSelectedItem();
                BigDecimal discount = BigDecimal.ZERO;
                try {
                    discount = new BigDecimal(discountField.getText().trim());
                } catch (Exception ex) {
                    throw new ValidationException("Invalid discount value.");
                }

                Order newOrder = new Order();
                newOrder.setCustomerId(customer.getCustomerId());
                newOrder.setCustomerName(customer.getFullName());
                newOrder.setCustomerPhone(customer.getPhone());
                newOrder.setOrderDate(LocalDateTime.now());
                newOrder.setExpectedDeliveryDate(expDate);
                newOrder.setStatus(OrderStatus.RECEIVED);
                newOrder.setDiscount(discount);
                newOrder.setSpecialInstructions(instructionsArea.getText().trim());
                newOrder.setCreatedBy(AuthenticationService.getCurrentUser() != null ? AuthenticationService.getCurrentUser().getUserId() : 1);
                newOrder.setItems(pendingItems);

                Order saved = orderService.createOrder(newOrder);

                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Order ORD-" + saved.getOrderId() + " registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadOrdersAsync();

                // Prompt to show receipt preview
                int choice = JOptionPane.showConfirmDialog(this, "Would you like to print or view the receipt now?", "Order Slip", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    String html = billingService.generateReceiptHtml(saved.getOrderId(), null);
                    ReceiptDialog rd = new ReceiptDialog(parent, html);
                    rd.setVisible(true);
                }

            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(dialog, ve.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Failed creating order: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        actionRow.add(saveOrderBtn);
        actionRow.add(cancelBtn);

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.add(formHeader, BorderLayout.NORTH);
        centerContainer.add(itemsPanel, BorderLayout.CENTER);
        centerContainer.add(footer, BorderLayout.SOUTH);

        dialog.add(centerContainer, BorderLayout.CENTER);
        dialog.add(actionRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
