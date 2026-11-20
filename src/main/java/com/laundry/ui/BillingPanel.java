package com.laundry.ui;

import com.laundry.exception.ValidationException;
import com.laundry.model.Order;
import com.laundry.model.OrderItem;
import com.laundry.model.Payment;
import com.laundry.model.enums.PaymentMethod;
import com.laundry.service.BillingService;
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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
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
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Billing and Payment Management Module: Order lookup, payment recording, balance calculations, receipts.
 */
public class BillingPanel extends JPanel {

    private final BillingService billingService = new BillingService();

    private JTextField orderIdLookupField;
    private JLabel customerNameVal, customerPhoneVal, orderDateVal, statusVal;
    private JLabel subtotalVal, discountVal, totalVal, paidVal, balanceVal;
    private ModernTable itemsTable;
    private DefaultTableModel itemsModel;
    private ModernTable paymentsTable;
    private DefaultTableModel paymentsModel;

    private Order currentOrder;

    public BillingPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initHeader();
        initContent();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        JLabel title = new JLabel("Billing & Payment Management");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_NAVY);
        JLabel subtitle = new JLabel("Process customer payments (Cash, UPI, Card), track dues, and print slips");
        subtitle.setFont(UIConstants.FONT_REGULAR);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.SOUTH);
        header.add(textPanel, BorderLayout.WEST);

        // Order Lookup Bar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchBar.setOpaque(false);

        searchBar.add(UIConstants.createFormLabel("Enter Order ID #:"));
        orderIdLookupField = UIConstants.createTextField();
        orderIdLookupField.setPreferredSize(new Dimension(140, 36));
        orderIdLookupField.setText("101"); // default for testing
        orderIdLookupField.addActionListener(e -> lookupOrder());

        JButton loadBtn = UIConstants.createPrimaryButton("🔍 Lookup Order");
        loadBtn.addActionListener(e -> lookupOrder());

        searchBar.add(orderIdLookupField);
        searchBar.add(loadBtn);

        header.add(searchBar, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initContent() {
        JPanel mainContent = new JPanel(new BorderLayout(16, 0));
        mainContent.setOpaque(false);

        // Left Panel: Order Overview & Line Items
        CardPanel leftCard = new CardPanel(new BorderLayout(0, 12));
        leftCard.setPreferredSize(new Dimension(480, 0));

        JPanel infoHeader = new JPanel(new GridBagLayout());
        infoHeader.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(3, 4, 3, 4);

        customerNameVal = new JLabel("-"); customerNameVal.setFont(UIConstants.FONT_BOLD);
        customerPhoneVal = new JLabel("-");
        orderDateVal = new JLabel("-");
        statusVal = new JLabel("-"); statusVal.setFont(UIConstants.FONT_BOLD);

        gbc.gridx = 0; gbc.gridy = 0; infoHeader.add(new JLabel("Customer:"), gbc);
        gbc.gridx = 1; infoHeader.add(customerNameVal, gbc);
        gbc.gridx = 0; gbc.gridy = 1; infoHeader.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1; infoHeader.add(customerPhoneVal, gbc);
        gbc.gridx = 0; gbc.gridy = 2; infoHeader.add(new JLabel("Order Date:"), gbc);
        gbc.gridx = 1; infoHeader.add(orderDateVal, gbc);
        gbc.gridx = 0; gbc.gridy = 3; infoHeader.add(new JLabel("Current Status:"), gbc);
        gbc.gridx = 1; infoHeader.add(statusVal, gbc);

        leftCard.add(infoHeader, BorderLayout.NORTH);

        // Items Table
        String[] itemCols = {"Service", "Description", "Qty", "Rate", "Subtotal"};
        itemsModel = new DefaultTableModel(itemCols, 0);
        itemsTable = new ModernTable(itemsModel);
        itemsTable.setEmptyMessage("Lookup an order above to view line items");
        leftCard.add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        // Right Panel: Financial Statement & Payment History
        CardPanel rightCard = new CardPanel(new BorderLayout(0, 14));

        // Ledger Summary Cards
        JPanel ledgerBox = new JPanel(new GridBagLayout());
        ledgerBox.setBackground(new Color(0xF8, 0xFA, 0xFC));
        ledgerBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        GridBagConstraints lgbc = new GridBagConstraints();
        lgbc.fill = GridBagConstraints.HORIZONTAL;
        lgbc.weightx = 0.5;
        lgbc.insets = new Insets(4, 4, 4, 4);

        subtotalVal = new JLabel("₹0.00"); subtotalVal.setFont(UIConstants.FONT_BOLD);
        discountVal = new JLabel("₹0.00");
        totalVal = new JLabel("₹0.00"); totalVal.setFont(new Font("Segoe UI", Font.BOLD, 16)); totalVal.setForeground(UIConstants.PRIMARY_NAVY);
        paidVal = new JLabel("₹0.00"); paidVal.setFont(UIConstants.FONT_BOLD); paidVal.setForeground(UIConstants.SUCCESS_GREEN);
        balanceVal = new JLabel("₹0.00"); balanceVal.setFont(new Font("Segoe UI", Font.BOLD, 18)); balanceVal.setForeground(UIConstants.ERROR_RED);

        lgbc.gridx = 0; lgbc.gridy = 0; ledgerBox.add(new JLabel("Order Subtotal:"), lgbc);
        lgbc.gridx = 1; ledgerBox.add(subtotalVal, lgbc);
        lgbc.gridx = 0; lgbc.gridy = 1; ledgerBox.add(new JLabel("Discount Applied:"), lgbc);
        lgbc.gridx = 1; ledgerBox.add(discountVal, lgbc);
        lgbc.gridx = 0; lgbc.gridy = 2; ledgerBox.add(new JLabel("Total Billed:"), lgbc);
        lgbc.gridx = 1; ledgerBox.add(totalVal, lgbc);
        lgbc.gridx = 0; lgbc.gridy = 3; ledgerBox.add(new JLabel("Total Paid:"), lgbc);
        lgbc.gridx = 1; ledgerBox.add(paidVal, lgbc);
        lgbc.gridx = 0; lgbc.gridy = 4; ledgerBox.add(new JLabel("OUTSTANDING BALANCE:"), lgbc);
        lgbc.gridx = 1; ledgerBox.add(balanceVal, lgbc);

        rightCard.add(ledgerBox, BorderLayout.NORTH);

        // Payment History Table
        String[] payCols = {"Pay ID", "Date / Time", "Amount", "Method", "Txn Reference", "Status"};
        paymentsModel = new DefaultTableModel(payCols, 0);
        paymentsTable = new ModernTable(paymentsModel);
        paymentsTable.setEmptyMessage("No payment transactions recorded for this order.");

        JPanel payTableContainer = new JPanel(new BorderLayout(0, 6));
        payTableContainer.setOpaque(false);
        JLabel payHistoryTitle = new JLabel("Transaction History");
        payHistoryTitle.setFont(UIConstants.FONT_HEADER);
        payHistoryTitle.setForeground(UIConstants.PRIMARY_NAVY);
        payTableContainer.add(payHistoryTitle, BorderLayout.NORTH);
        payTableContainer.add(new JScrollPane(paymentsTable), BorderLayout.CENTER);
        rightCard.add(payTableContainer, BorderLayout.CENTER);

        // Action Toolbar: Pay Now & Print Receipt
        JPanel botActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        botActions.setOpaque(false);

        JButton printReceiptBtn = UIConstants.createOutlineButton("🧾 Print Formal Receipt");
        printReceiptBtn.addActionListener(e -> {
            if (currentOrder == null) {
                JOptionPane.showMessageDialog(this, "Please lookup an order first.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String html = billingService.generateReceiptHtml(currentOrder.getOrderId(), null);
            JFrame top = (JFrame) SwingUtilities.getWindowAncestor(this);
            ReceiptDialog dlg = new ReceiptDialog(top, html);
            dlg.setVisible(true);
        });

        JButton payNowBtn = UIConstants.createPrimaryButton("💳 Record Payment");
        payNowBtn.addActionListener(e -> {
            if (currentOrder == null) {
                JOptionPane.showMessageDialog(this, "Please lookup an order first.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (currentOrder.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "This order is already fully paid! Outstanding balance is ₹0.00.", "Fully Paid", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            showPaymentDialog();
        });

        botActions.add(printReceiptBtn);
        botActions.add(payNowBtn);
        rightCard.add(botActions, BorderLayout.SOUTH);

        mainContent.add(leftCard, BorderLayout.WEST);
        mainContent.add(rightCard, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);
    }

    private void lookupOrder() {
        String input = orderIdLookupField.getText().trim().replace("ORD-", "");
        int orderId;
        try {
            orderId = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a numeric Order ID (e.g. 101).", "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SwingWorker<Order, Void> worker = new SwingWorker<>() {
            @Override
            protected Order doInBackground() {
                return billingService.getOrderBillingDetails(orderId);
            }

            @Override
            protected void done() {
                try {
                    currentOrder = get();
                    if (currentOrder == null) {
                        JOptionPane.showMessageDialog(BillingPanel.this, "Order ORD-" + orderId + " not found in database.", "Not Found", JOptionPane.WARNING_MESSAGE);
                        clearDisplay();
                        return;
                    }

                    // Update UI Details
                    customerNameVal.setText(currentOrder.getCustomerName());
                    customerPhoneVal.setText(currentOrder.getCustomerPhone());
                    orderDateVal.setText(DateUtil.formatDateTime(currentOrder.getOrderDate()));
                    statusVal.setText(currentOrder.getStatus().getDisplayName());

                    subtotalVal.setText(ReceiptUtil.formatCurrency(currentOrder.getSubtotal()));
                    discountVal.setText(ReceiptUtil.formatCurrency(currentOrder.getDiscount()));
                    totalVal.setText(ReceiptUtil.formatCurrency(currentOrder.getTotalAmount()));
                    paidVal.setText(ReceiptUtil.formatCurrency(currentOrder.getPaidAmount()));
                    balanceVal.setText(ReceiptUtil.formatCurrency(currentOrder.getBalanceDue()));

                    // Items Table
                    itemsModel.setRowCount(0);
                    if (currentOrder.getItems() != null) {
                        for (OrderItem it : currentOrder.getItems()) {
                            itemsModel.addRow(new Object[]{
                                it.getServiceName(),
                                it.getItemDescription(),
                                it.getQuantity(),
                                ReceiptUtil.formatCurrency(it.getUnitPrice()),
                                ReceiptUtil.formatCurrency(it.getSubtotal())
                            });
                        }
                    }

                    // Payment History
                    loadPaymentHistory(orderId);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(BillingPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void loadPaymentHistory(int orderId) {
        List<Payment> payments = billingService.getPaymentsForOrder(orderId);
        paymentsModel.setRowCount(0);
        for (Payment p : payments) {
            paymentsModel.addRow(new Object[]{
                "#" + p.getPaymentId(),
                DateUtil.formatDateTime(p.getPaymentDate()),
                ReceiptUtil.formatCurrency(p.getAmount()),
                p.getPaymentMethod().getDisplayName(),
                p.getTransactionReference() != null ? p.getTransactionReference() : "-",
                p.getStatus().getDisplayName()
            });
        }
    }

    private void clearDisplay() {
        customerNameVal.setText("-");
        customerPhoneVal.setText("-");
        orderDateVal.setText("-");
        statusVal.setText("-");
        subtotalVal.setText("₹0.00");
        discountVal.setText("₹0.00");
        totalVal.setText("₹0.00");
        paidVal.setText("₹0.00");
        balanceVal.setText("₹0.00");
        itemsModel.setRowCount(0);
        paymentsModel.setRowCount(0);
    }

    private void showPaymentDialog() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Record Payment - ORD-" + currentOrder.getOrderId(), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(440, 380);
        dialog.setLocationRelativeTo(parent);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 15, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(6, 0, 4, 0);

        JLabel dueLabel = new JLabel("Balance Due: " + ReceiptUtil.formatCurrency(currentOrder.getBalanceDue()));
        dueLabel.setFont(UIConstants.FONT_BOLD);
        dueLabel.setForeground(UIConstants.ERROR_RED);

        JTextField amountField = UIConstants.createTextField();
        amountField.setText(currentOrder.getBalanceDue().toPlainString());

        JComboBox<PaymentMethod> methodCombo = new JComboBox<>(PaymentMethod.values());

        JTextField refField = UIConstants.createTextField();
        refField.putClientProperty("JTextField.placeholderText", "UPI / Card Reference (Optional)");

        gbc.gridy = 0; content.add(dueLabel, gbc);
        gbc.gridy = 1; content.add(UIConstants.createFormLabel("Payment Amount (₹) *"), gbc);
        gbc.gridy = 2; content.add(amountField, gbc);
        gbc.gridy = 3; content.add(UIConstants.createFormLabel("Payment Method *"), gbc);
        gbc.gridy = 4; content.add(methodCombo, gbc);
        gbc.gridy = 5; content.add(UIConstants.createFormLabel("Transaction Reference"), gbc);
        gbc.gridy = 6; content.add(refField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(UIConstants.BG_LIGHT);

        JButton submitBtn = UIConstants.createPrimaryButton("💾 Submit Payment");
        JButton cancelBtn = UIConstants.createOutlineButton("✖️ Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        submitBtn.addActionListener(e -> {
            try {
                BigDecimal payAmount = new BigDecimal(amountField.getText().trim());
                PaymentMethod method = (PaymentMethod) methodCombo.getSelectedItem();
                String ref = refField.getText().trim();

                Payment p = billingService.recordPayment(currentOrder.getOrderId(), payAmount, method, ref);
                dialog.dispose();

                JOptionPane.showMessageDialog(this, "Payment of " + ReceiptUtil.formatCurrency(payAmount) + " recorded successfully!", "Payment Success", JOptionPane.INFORMATION_MESSAGE);

                // Reload order
                lookupOrder();

                // Show printed receipt
                int showSlip = JOptionPane.showConfirmDialog(this, "Print receipt for this payment now?", "Print Receipt", JOptionPane.YES_NO_OPTION);
                if (showSlip == JOptionPane.YES_OPTION) {
                    String html = billingService.generateReceiptHtml(currentOrder.getOrderId(), p);
                    ReceiptDialog rd = new ReceiptDialog(parent, html);
                    rd.setVisible(true);
                }

            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(dialog, ve.getMessage(), "Validation Notice", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Payment Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(submitBtn);
        btnPanel.add(cancelBtn);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
