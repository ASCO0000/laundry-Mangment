package com.laundry.app;

/**
 * Main application entry point for the Laundry Management System.
 */
public class Main {

    public static void main(String[] args) {
        if (args != null && args.length > 0 && ("--server".equalsIgnoreCase(args[0]) || "-s".equalsIgnoreCase(args[0]))) {
            ServerLauncher.launchServer();
        } else {
            AppLauncher.launch();
        }
    }
}
