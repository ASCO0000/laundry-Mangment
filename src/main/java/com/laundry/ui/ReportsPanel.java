package com.laundry.ui;

import com.laundry.service.ReportService;
import com.laundry.ui.components.CardPanel;
import com.laundry.ui.components.ModernTable;
import com.laundry.ui.theme.UIConstants;
import com.laundry.util.CsvExporter;
import com.laundry.util.DateUtil;
import com.laundry.util.ReceiptUtil;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

/**
 * Reports and Analytics Module: Date-range financial collection audits and CSV exports.
 */
public class ReportsPanel extends JPanel {

    private final ReportService reportService = new ReportService();

    private ModernTable reportTable;
    private DefaultTableModel tableModel;
    private JTextField startDateField;
    private JTextField endDateField;
    private JComboBox<String> presetRangeCombo;
    private JLabel totalCollectedLabel;

    public ReportsPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initHeader();
        initTableCard();
        loadReportAsync();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        JLabel title = new JLabel("Financial & Operational Reports");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_NAVY);
        JLabel subtitle = new JLabel("Audit payment inflows, filter by date ranges, and export transaction journals");
        subtitle.setFont(UIConstants.FONT_REGULAR);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.SOUTH);
        header.add(textPanel, BorderLayout.WEST);

        // Date Range Controls
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterBar.setOpaque(false);

        presetRangeCombo = new JComboBox<>(new String[]{"Last 30 Days", "Today", "Last 7 Days", "This Month"});
        presetRangeCombo.setPreferredSize(new Dimension(130, 36));
        presetRangeCombo.addActionListener(e -> applyPresetRange());

        startDateField = UIConstants.createTextField();
        startDateField.setPreferredSize(new Dimension(110, 36));
        startDateField.setText(LocalDate.now().minusDays(30).toString());

        endDateField = UIConstants.createTextField();
        endDateField.setPreferredSize(new Dimension(110, 36));
        endDateField.setText(LocalDate.now().toString());

        JButton filterBtn = UIConstants.createPrimaryButton("🔍 Generate Report");
        filterBtn.addActionListener(e -> loadReportAsync());

        JButton exportBtn = UIConstants.createOutlineButton("📥 Export to CSV");
        exportBtn.addActionListener(e -> exportToCsv());

        filterBar.add(presetRangeCombo);
        filterBar.add(new JLabel("From:"));
        filterBar.add(startDateField);
        filterBar.add(new JLabel("To:"));
        filterBar.add(endDateField);
        filterBar.add(filterBtn);
        filterBar.add(exportBtn);

        header.add(filterBar, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initTableCard() {
        CardPanel card = new CardPanel(new BorderLayout(0, 12));

        String[] cols = {"Receipt ID", "Payment Date & Time", "Order ID", "Customer Name", "Amount Collected", "Payment Method", "Reference", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reportTable = new ModernTable(tableModel);
        reportTable.setEmptyMessage("No payment transactions recorded for the chosen date period.");

        JScrollPane scrollPane = new JScrollPane(reportTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        card.add(scrollPane, BorderLayout.CENTER);

        // Footer Summary
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 8));
        footer.setOpaque(false);

        totalCollectedLabel = new JLabel("Total Period Revenue: ₹0.00");
        totalCollectedLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        totalCollectedLabel.setForeground(UIConstants.PRIMARY_NAVY);
        footer.add(totalCollectedLabel);

        card.add(footer, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    private void applyPresetRange() {
        String selected = (String) presetRangeCombo.getSelectedItem();
        LocalDate now = LocalDate.now();
        if ("Today".equals(selected)) {
            startDateField.setText(now.toString());
            endDateField.setText(now.toString());
        } else if ("Last 7 Days".equals(selected)) {
            startDateField.setText(now.minusDays(7).toString());
            endDateField.setText(now.toString());
        } else if ("This Month".equals(selected)) {
            startDateField.setText(now.withDayOfMonth(1).toString());
            endDateField.setText(now.toString());
        } else {
            startDateField.setText(now.minusDays(30).toString());
            endDateField.setText(now.toString());
        }
        loadReportAsync();
    }

    public void loadReportAsync() {
        LocalDate start = DateUtil.parseDate(startDateField.getText().trim());
        LocalDate end = DateUtil.parseDate(endDateField.getText().trim());
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();

        final LocalDate fStart = start;
        final LocalDate fEnd = end;

        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                return reportService.getPaymentCollectionReport(fStart, fEnd);
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> rows = get();
                    tableModel.setRowCount(0);
                    BigDecimal total = BigDecimal.ZERO;

                    for (Map<String, Object> r : rows) {
                        BigDecimal amt = (BigDecimal) r.get("amount");
                        total = total.add(amt != null ? amt : BigDecimal.ZERO);

                        tableModel.addRow(new Object[]{
                            "REC-" + r.get("paymentId"),
                            DateUtil.formatDateTime((LocalDateTime) r.get("paymentDate")),
                            "ORD-" + r.get("orderId"),
                            r.get("customerName"),
                            ReceiptUtil.formatCurrency(amt),
                            r.get("paymentMethod"),
                            r.get("reference") != null ? r.get("reference") : "-",
                            r.get("status")
                        });
                    }

                    totalCollectedLabel.setText("Total Period Revenue: " + ReceiptUtil.formatCurrency(total));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ReportsPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void exportToCsv() {
        if (reportTable.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No records to export in the current view.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Report to CSV");
        chooser.setSelectedFile(new File("laundry_financial_report_" + LocalDate.now() + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }
            try {
                CsvExporter.exportTableToCsv(reportTable, file);
                JOptionPane.showMessageDialog(this, "Report successfully exported to:\n" + file.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
