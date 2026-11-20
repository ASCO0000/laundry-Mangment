package com.laundry.ui.components;

import com.laundry.ui.theme.UIConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.print.PrinterException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Modal dialog for previewing and printing customer order receipts.
 */
public class ReceiptDialog extends JDialog {

    private final JEditorPane receiptPane;

    public ReceiptDialog(Frame parent, String htmlReceipt) {
        super(parent, "Customer Laundry Receipt", true);
        setLayout(new BorderLayout());
        setSize(480, 640);
        setLocationRelativeTo(parent);

        receiptPane = new JEditorPane();
        receiptPane.setContentType("text/html");
        receiptPane.setText(htmlReceipt);
        receiptPane.setEditable(false);
        receiptPane.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(receiptPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(UIConstants.BG_LIGHT);

        JButton printBtn = UIConstants.createPrimaryButton("Print Receipt");
        printBtn.addActionListener(e -> {
            try {
                boolean done = receiptPane.print();
                if (done) {
                    JOptionPane.showMessageDialog(this, "Receipt sent to printer successfully.", "Printing", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Printing failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton closeBtn = UIConstants.createOutlineButton("Close");
        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(printBtn);
        buttonPanel.add(closeBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}
