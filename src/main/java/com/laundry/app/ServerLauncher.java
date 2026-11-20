package com.laundry.app;

import com.laundry.api.CustomerApiServer;
import com.laundry.config.DatabaseConfig;
import com.laundry.config.DatabaseConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dedicated Server Launcher for headless server environments,
 * Docker containers, cloud VPS, or local server instances.
 */
public class ServerLauncher {

    private static final Logger LOGGER = Logger.getLogger(ServerLauncher.class.getName());

    public static void main(String[] args) {
        launchServer();
    }

    public static void launchServer() {
        System.out.println("==================================================================");
        System.out.println("   🧺 BLUE WAVE LAUNDRY MANAGEMENT SYSTEM - SERVER ENGINE        ");
        System.out.println("==================================================================");

        // 1. Initialize Database
        try {
            System.out.println("[SERVER] Connecting and initializing database schema...");
            DatabaseConnection.initializeDatabase();
            System.out.println("[SERVER] Database ready (Target: " + (DatabaseConfig.isH2Active() ? "Embedded H2 (MySQL Mode)" : "MySQL") + ")");
        } catch (Exception e) {
            System.err.println("[SERVER ERROR] Database initialization failed: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Database initialization failed", e);
        }

        // 2. Start HTTP Server
        CustomerApiServer.start();
        int port = CustomerApiServer.getPort();

        // 3. Find LAN IP address for phone/network testing
        String lanIp = "localhost";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        lanIp = addr.getHostAddress();
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 4. Output Access Points
        System.out.println("------------------------------------------------------------------");
        System.out.println("🚀 SERVER ACTIVE AND LISTENING ON PORT " + port);
        System.out.println("------------------------------------------------------------------");
        System.out.println("📱 Customer Mobile App / Android:  http://localhost:" + port + "/");
        System.out.println("   (On Phone / Local WiFi):        http://" + lanIp + ":" + port + "/");
        System.out.println();
        System.out.println("🛡️ Admin Web Management Portal:    http://localhost:" + port + "/admin/");
        System.out.println("   (On LAN / Other Devices):       http://" + lanIp + ":" + port + "/admin/");
        System.out.println();
        System.out.println("🔑 Default Admin Login:            admin / admin123");
        System.out.println("🔑 Default Staff Login:            staff1 / staff123");
        System.out.println("------------------------------------------------------------------");
        System.out.println("Press Ctrl + C in this terminal to gracefully stop the server.");
        System.out.println("==================================================================");

        // Keep process alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("[SERVER] Shutting down server...");
            CustomerApiServer.stop();
        }
    }
}
