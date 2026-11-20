package com.laundry.ui;

import com.laundry.model.Order;
import com.laundry.service.OrderService;
import com.laundry.service.ReportService;
import com.laundry.ui.components.CardPanel;
import com.laundry.ui.components.ModernTable;
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
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

/**
 * Executive Admin Dashboard Module: Live KPI metric cards, JFreeChart analytics, and recent activity.
 */
public class DashboardPanel extends JPanel {

    private final ReportService reportService = new ReportService();
    private final OrderService orderService = new OrderService();

    // 6 KPI Metric Labels
    private JLabel custCountLabel, orderCountLabel, pendingCountLabel, completedCountLabel, todayRevLabel, outstandingLabel;

    // Charts
    private JPanel chartContainer;
    private ChartPanel revenueChartPanel;
    private ChartPanel statusPieChartPanel;

    // Recent Orders Table
    private ModernTable recentOrdersTable;
    private DefaultTableModel recentOrdersModel;

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BG_LIGHT);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initHeader();

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        body.add(buildKpiCardsGrid(), BorderLayout.NORTH);
        body.add(buildChartsAndRecentOrders(), BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);

        loadDashboardDataAsync();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        JLabel title = new JLabel("Operations Dashboard");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_NAVY);
        JLabel subtitle = new JLabel("Real-time snapshot of laundry workload, revenue collection, and pending turnover");
        subtitle.setFont(UIConstants.FONT_REGULAR);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.SOUTH);
        header.add(textPanel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton refreshBtn = UIConstants.createPrimaryButton("🔄 Refresh Data");
        refreshBtn.addActionListener(e -> loadDashboardDataAsync());
        actions.add(refreshBtn);

        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private JPanel buildKpiCardsGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 6, 12, 0));
        grid.setOpaque(false);

        custCountLabel = new JLabel("0");
        orderCountLabel = new JLabel("0");
        pendingCountLabel = new JLabel("0");
        completedCountLabel = new JLabel("0");
        todayRevLabel = new JLabel("₹0.00");
        outstandingLabel = new JLabel("₹0.00");

        grid.add(createStatCard("Total Customers", custCountLabel, "👥", UIConstants.SECONDARY_BLUE));
        grid.add(createStatCard("Total Orders", orderCountLabel, "📦", UIConstants.PRIMARY_NAVY));
        grid.add(createStatCard("Pending Orders", pendingCountLabel, "⏳", UIConstants.WARNING_ORANGE));
        grid.add(createStatCard("Completed Orders", completedCountLabel, "✅", UIConstants.SUCCESS_GREEN));
        grid.add(createStatCard("Today's Revenue", todayRevLabel, "💵", UIConstants.SUCCESS_GREEN));
        grid.add(createStatCard("Outstanding Due", outstandingLabel, "⚠️", UIConstants.ERROR_RED));

        return grid;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String icon, Color accentColor) {
        CardPanel card = new CardPanel(new BorderLayout(0, 6));
        card.setPreferredSize(new Dimension(150, 95));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel tLabel = new JLabel(title);
        tLabel.setFont(UIConstants.FONT_SMALL);
        tLabel.setForeground(UIConstants.TEXT_MUTED);

        JLabel iLabel = new JLabel(icon);
        iLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        top.add(tLabel, BorderLayout.WEST);
        top.add(iLabel, BorderLayout.EAST);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueLabel.setForeground(accentColor);

        card.add(top, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildChartsAndRecentOrders() {
        JPanel container = new JPanel(new GridLayout(1, 2, 16, 0));
        container.setOpaque(false);

        // Left: Monthly Revenue Chart
        CardPanel chartCard = new CardPanel(new BorderLayout(0, 8));
        chartCard.setPreferredSize(new Dimension(500, 320));

        JLabel chartTitle = new JLabel("Monthly Revenue Inflow");
        chartTitle.setFont(UIConstants.FONT_HEADER);
        chartTitle.setForeground(UIConstants.PRIMARY_NAVY);
        chartCard.add(chartTitle, BorderLayout.NORTH);

        revenueChartPanel = new ChartPanel(null);
        revenueChartPanel.setOpaque(false);
        chartCard.add(revenueChartPanel, BorderLayout.CENTER);
        container.add(chartCard);

        // Right: Recent Orders Table
        CardPanel ordersCard = new CardPanel(new BorderLayout(0, 8));

        JLabel ordersTitle = new JLabel("Recent Laundry Orders");
        ordersTitle.setFont(UIConstants.FONT_HEADER);
        ordersTitle.setForeground(UIConstants.PRIMARY_NAVY);
        ordersCard.add(ordersTitle, BorderLayout.NORTH);

        String[] cols = {"Order ID", "Customer", "Delivery Date", "Amount", "Status"};
        recentOrdersModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        recentOrdersTable = new ModernTable(recentOrdersModel);
        recentOrdersTable.setEmptyMessage("No recent orders to show.");
        ordersCard.add(new JScrollPane(recentOrdersTable), BorderLayout.CENTER);

        container.add(ordersCard);
        return container;
    }

    public void loadDashboardDataAsync() {
        SwingWorker<DashboardData, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardData doInBackground() {
                DashboardData data = new DashboardData();
                data.metrics = reportService.getDashboardMetrics();
                data.revenueTrend = reportService.getMonthlyRevenueTrend();
                data.recentOrders = orderService.getAllOrders("All", "");
                return data;
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();

                    // 1. Update Metrics Cards
                    custCountLabel.setText(String.valueOf(data.metrics.get("totalCustomers")));
                    orderCountLabel.setText(String.valueOf(data.metrics.get("totalOrders")));
                    pendingCountLabel.setText(String.valueOf(data.metrics.get("pendingOrders")));
                    completedCountLabel.setText(String.valueOf(data.metrics.get("completedOrders")));
                    todayRevLabel.setText(ReceiptUtil.formatCurrency((BigDecimal) data.metrics.get("todayRevenue")));
                    outstandingLabel.setText(ReceiptUtil.formatCurrency((BigDecimal) data.metrics.get("outstandingPayments")));

                    // 2. Update Revenue Bar Chart
                    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                    if (data.revenueTrend.isEmpty()) {
                        dataset.addValue(0, "Revenue", "No Data");
                    } else {
                        data.revenueTrend.forEach((month, rev) -> dataset.addValue(rev.doubleValue(), "Revenue", month));
                    }

                    JFreeChart chart = ChartFactory.createBarChart(
                            null, "Month", "Revenue (₹)", dataset,
                            PlotOrientation.VERTICAL, false, true, false);

                    chart.setBackgroundPaint(Color.WHITE);
                    CategoryPlot plot = chart.getCategoryPlot();
                    plot.setBackgroundPaint(Color.WHITE);
                    plot.setRangeGridlinePaint(UIConstants.CARD_BORDER);

                    BarRenderer renderer = (BarRenderer) plot.getRenderer();
                    renderer.setSeriesPaint(0, UIConstants.SECONDARY_BLUE);
                    renderer.setMaximumBarWidth(0.1);

                    revenueChartPanel.setChart(chart);

                    // 3. Update Recent Orders Table (Max 8 rows)
                    recentOrdersModel.setRowCount(0);
                    int count = 0;
                    for (Order o : data.recentOrders) {
                        if (count++ >= 8) break;
                        recentOrdersModel.addRow(new Object[]{
                            "ORD-" + o.getOrderId(),
                            o.getCustomerName(),
                            DateUtil.formatDate(o.getExpectedDeliveryDate()),
                            ReceiptUtil.formatCurrency(o.getTotalAmount()),
                            o.getStatus()
                        });
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Error updating dashboard: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private static class DashboardData {
        Map<String, Object> metrics;
        Map<String, BigDecimal> revenueTrend;
        List<Order> recentOrders;
    }
}
