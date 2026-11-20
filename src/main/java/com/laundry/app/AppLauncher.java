package com.laundry.app;

import com.laundry.config.DatabaseConfig;
import com.laundry.config.DatabaseConnection;
import com.laundry.ui.LoginFrame;
import com.laundry.ui.theme.UITheme;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Application Bootstrap and Lifecycle Manager.
 */
public class AppLauncher {

    private static final Logger LOGGER = Logger.getLogger(AppLauncher.class.getName());

    public static void launch() {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            LOGGER.info("Headless environment detected. Redirecting to ServerLauncher.");
            ServerLauncher.launchServer();
            return;
        }

        // 1. Initialize UI Look and Feel (FlatLaf)
        UITheme.setup();

        // 2. Initialize Database and default admin/service seeds
        if (DatabaseConfig.isAutoInit()) {
            try {
                LOGGER.info("Initializing database connectivity...");
                DatabaseConnection.initializeDatabase();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed initializing database", e);
                JOptionPane.showMessageDialog(null,
                        "Database Initialization Error:\n" + e.getMessage() +
                        "\n\nPlease check MySQL or local database settings.",
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // 3. Launch Embedded Customer Mobile Server & REST API
        try {
            com.laundry.api.CustomerApiServer.start();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Customer API server start failed", e);
        }

        // 4. Launch UI on the Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
