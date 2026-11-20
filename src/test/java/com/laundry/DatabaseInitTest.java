package com.laundry;

import com.laundry.config.DatabaseConfig;
import com.laundry.config.DatabaseConnection;
import com.laundry.dao.CustomerDAO;
import com.laundry.dao.OrderDAO;
import com.laundry.dao.ServiceDAO;
import com.laundry.dao.UserDAO;
import com.laundry.model.Customer;
import com.laundry.model.Service;
import com.laundry.model.User;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseInitTest {

    @BeforeAll
    public static void setup() {
        // Force H2 in-memory mode for standalone automated integration testing
        DatabaseConfig.activateH2Fallback();
        DatabaseConnection.initializeDatabase();
    }

    @Test
    @DisplayName("Should initialize schema and create initial admin account")
    public void testAdminUserSeeded() {
        UserDAO userDAO = new UserDAO();
        User admin = userDAO.findByUsername("admin");
        assertNotNull(admin, "Admin account should be seeded automatically");
        assertTrue(admin.isAdmin());
        assertTrue(admin.isActive());
    }

    @Test
    @DisplayName("Should seed default laundry services")
    public void testDefaultServicesSeeded() {
        ServiceDAO serviceDAO = new ServiceDAO();
        List<Service> services = serviceDAO.findAll(true);
        assertNotNull(services);
        assertFalse(services.isEmpty(), "Default services should be pre-populated");

        boolean hasWashing = services.stream().anyMatch(s -> s.getServiceName().equalsIgnoreCase("Washing"));
        boolean hasIroning = services.stream().anyMatch(s -> s.getServiceName().equalsIgnoreCase("Ironing"));
        assertTrue(hasWashing);
        assertTrue(hasIroning);
    }

    @Test
    @DisplayName("Should support customer CRUD operations in database")
    public void testCustomerCrud() {
        CustomerDAO customerDAO = new CustomerDAO();
        Customer c = new Customer("Test Customer", "9998887771", "test@test.com", "101 Test Road");
        Customer saved = customerDAO.save(c);

        assertNotNull(saved);
        assertTrue(saved.getCustomerId() > 0);

        Customer found = customerDAO.findById(saved.getCustomerId());
        assertNotNull(found);
        assertNotNull(found.getFullName());

        customerDAO.delete(saved.getCustomerId());
    }

    @Test
    @DisplayName("Should calculate service summary metrics accurately")
    public void testServiceSummary() {
        ServiceDAO serviceDAO = new ServiceDAO();
        java.util.Map<String, Object> summary = serviceDAO.getServiceSummary();
        assertNotNull(summary);
        assertTrue((int) summary.get("totalServices") >= 8);
        assertTrue((int) summary.get("activeServices") >= 8);
        assertNotNull(summary.get("averagePrice"));
    }
}
