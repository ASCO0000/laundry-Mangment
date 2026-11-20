package com.laundry.ui;

import com.laundry.model.User;
import com.laundry.service.AuthenticationService;
import com.laundry.ui.theme.UIConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Main Application Shell: Left Navigation Sidebar, Top Control Bar, and CardLayout Modules.
 */
public class MainFrame extends JFrame {

    private final User currentUser;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    // Module Panels
    private DashboardPanel dashboardPanel;
    private CustomerPanel customerPanel;
    private ServicePanel servicePanel;
    private OrderPanel orderPanel;
    private StatusTrackingPanel statusTrackingPanel;
    private BillingPanel billingPanel;
    private DeliveryPanel deliveryPanel;
    private ReportsPanel reportsPanel;

    private JLabel topPageTitle;
    private final Map<String, JButton> navButtons = new HashMap<>();
    private String currentModule = "Dashboard";

    public MainFrame(User user) {
        this.currentUser = user;
        setTitle("Blue Wave Laundry Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);

        initModules();
        initUI();
    }

    private void initModules() {
        dashboardPanel = new DashboardPanel();
        customerPanel = new CustomerPanel();
        servicePanel = new ServicePanel();
        orderPanel = new OrderPanel();
        statusTrackingPanel = new StatusTrackingPanel();
        billingPanel = new BillingPanel();
        deliveryPanel = new DeliveryPanel();
        reportsPanel = new ReportsPanel();

        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(customerPanel, "Customers");
        contentPanel.add(servicePanel, "Services");
        contentPanel.add(orderPanel, "Orders");
        contentPanel.add(statusTrackingPanel, "Status Tracking");
        contentPanel.add(billingPanel, "Billing");
        contentPanel.add(deliveryPanel, "Pickup & Delivery");
        contentPanel.add(reportsPanel, "Reports");
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 1. Left Fixed Sidebar
        add(buildSidebar(), BorderLayout.WEST);

        // 2. Center Work Area (Top Bar + Dynamic Content Panel)
        JPanel workArea = new JPanel(new BorderLayout());
        workArea.add(buildTopBar(), BorderLayout.NORTH);
        workArea.add(contentPanel, BorderLayout.CENTER);

        add(workArea, BorderLayout.CENTER);

        switchModule("Dashboard");
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UIConstants.PRIMARY_NAVY);
        sidebar.setPreferredSize(new Dimension(240, 0));

        // Brand Logo Area
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 20));
        brandPanel.setOpaque(false);

        JLabel logoIcon = new JLabel("🧺");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JPanel brandText = new JPanel(new BorderLayout());
        brandText.setOpaque(false);
        JLabel title = new JLabel("Blue Wave");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Laundry Systems");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(new Color(0x94, 0xA3, 0xB8));

        brandText.add(title, BorderLayout.NORTH);
        brandText.add(sub, BorderLayout.SOUTH);

        brandPanel.add(logoIcon);
        brandPanel.add(brandText);
        sidebar.add(brandPanel, BorderLayout.NORTH);

        // Navigation Buttons List
        JPanel menuPanel = new JPanel(new GridBagLayout());
        menuPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.insets = new Insets(3, 12, 3, 12);

        String[][] navItems = {
            {"Dashboard", "📊"},
            {"Customers", "👥"},
            {"Services", "🏷️"},
            {"Orders", "📦"},
            {"Status Tracking", "🔄"},
            {"Billing", "💳"},
            {"Pickup & Delivery", "🚚"},
            {"Reports", "📈"}
        };

        for (int i = 0; i < navItems.length; i++) {
            gbc.gridy = i;
            String name = navItems[i][0];
            String icon = navItems[i][1];
            JButton btn = createNavButton(name, icon);
            navButtons.put(name, btn);
            menuPanel.add(btn, gbc);
        }

        sidebar.add(menuPanel, BorderLayout.CENTER);

        // Bottom User & Logout Area
        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 12, 18, 12));

        JButton logoutBtn = new JButton("🚪  Sign Out") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(0xEF, 0x44, 0x44));
                } else {
                    g2.setColor(new Color(0x24, 0x37, 0x5E));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logoutBtn.setFont(UIConstants.FONT_BOLD);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.setPreferredSize(new Dimension(logoutBtn.getPreferredSize().width, 38));
        logoutBtn.addActionListener(e -> handleLogout());

        footer.add(logoutBtn, BorderLayout.SOUTH);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createNavButton(String text, String icon) {
        JButton btn = new JButton(icon + "   " + text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (text.equals(currentModule)) {
                    g2.setColor(UIConstants.SECONDARY_BLUE);
                } else if (getModel().isRollover()) {
                    g2.setColor(UIConstants.PRIMARY_NAVY_HOVER);
                } else {
                    g2.setColor(UIConstants.PRIMARY_NAVY);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UIConstants.FONT_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 40));

        btn.addActionListener(e -> switchModule(text));
        return btn;
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setPreferredSize(new Dimension(0, 60));
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.CARD_BORDER),
                BorderFactory.createEmptyBorder(0, 24, 0, 24)
        ));

        topPageTitle = new JLabel("Dashboard Overview");
        topPageTitle.setFont(UIConstants.FONT_TITLE);
        topPageTitle.setForeground(UIConstants.PRIMARY_NAVY);
        topBar.add(topPageTitle, BorderLayout.WEST);

        // Right side: Quick Search + User Profile
        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        rightSide.setOpaque(false);

        // Server Status & Web Portal Link
        JLabel serverBadge = new JLabel("● Server Online :8080");
        serverBadge.setFont(UIConstants.FONT_SMALL);
        serverBadge.setForeground(new Color(0x05, 0x96, 0x69));

        JButton webPortalBtn = UIConstants.createOutlineButton("🌐 Open Web Server");
        webPortalBtn.addActionListener(e -> {
            try {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("http://localhost:8080/admin/"));
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Web Server is active on port 8080!\n\n" +
                            "• Admin Management Portal: http://localhost:8080/admin/\n" +
                            "• Customer Android Portal: http://localhost:8080/\n",
                            "Server Active", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please open: http://localhost:8080/admin/ in your browser.", "Web Portal", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // User badge
        String roleStr = currentUser != null ? currentUser.getRole().getDisplayName() : "Staff";
        String nameStr = currentUser != null ? currentUser.getFullName() : "User";
        JLabel userLabel = new JLabel("👤 " + nameStr + " (" + roleStr + ")");
        userLabel.setFont(UIConstants.FONT_BOLD);
        userLabel.setForeground(UIConstants.PRIMARY_NAVY);

        rightSide.add(serverBadge);
        rightSide.add(webPortalBtn);
        rightSide.add(userLabel);
        topBar.add(rightSide, BorderLayout.EAST);

        return topBar;
    }

    private void switchModule(String moduleName) {
        currentModule = moduleName;
        topPageTitle.setText(moduleName);
        cardLayout.show(contentPanel, moduleName);

        // Repaint all nav buttons for active state highlight
        for (JButton b : navButtons.values()) {
            b.repaint();
        }

        // Automatic fresh data load when switching tabs
        switch (moduleName) {
            case "Dashboard" -> dashboardPanel.loadDashboardDataAsync();
            case "Customers" -> customerPanel.loadCustomersAsync("");
            case "Services" -> servicePanel.loadServicesAsync("");
            case "Orders" -> orderPanel.loadOrdersAsync();
            case "Status Tracking" -> statusTrackingPanel.loadTrackingDataAsync();
            case "Pickup & Delivery" -> deliveryPanel.loadDeliveriesAsync();
            case "Reports" -> reportsPanel.loadReportAsync();
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out of the Laundry Management System?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            AuthenticationService.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
