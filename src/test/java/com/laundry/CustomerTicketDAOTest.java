package com.laundry;

import com.laundry.config.DatabaseConfig;
import com.laundry.config.DatabaseConnection;
import com.laundry.dao.CustomerTicketDAO;
import com.laundry.model.CustomerTicket;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomerTicketDAOTest {

    private static CustomerTicketDAO ticketDAO;

    @BeforeAll
    public static void setup() {
        DatabaseConfig.activateH2Fallback();
        DatabaseConnection.initializeDatabase();
        ticketDAO = new CustomerTicketDAO();
    }

    @Test
    @DisplayName("Should save and retrieve a complaint ticket")
    public void testSaveAndRetrieveComplaintTicket() {
        CustomerTicket ticket = new CustomerTicket();
        ticket.setTicketType("Complaint");
        ticket.setCustomerName("Deepak Patel");
        ticket.setCustomerPhone("9811223344");
        ticket.setCategory("Wash Quality");
        ticket.setSubject("Shirt wrinkles after wash");
        ticket.setDescription("Garment arrived heavily wrinkled.");
        ticket.setPriority("Medium");

        CustomerTicket saved = ticketDAO.save(ticket);
        assertNotNull(saved);
        assertTrue(saved.getTicketId() > 0);
        assertNotNull(saved.getTicketNumber());
        assertTrue(saved.getTicketNumber().startsWith("CMP-"));

        CustomerTicket found = ticketDAO.findById(saved.getTicketId());
        assertNotNull(found);
        assertEquals("Deepak Patel", found.getCustomerName());
        assertEquals("Open", found.getStatus());
    }

    @Test
    @DisplayName("Should save a missing item claim and retrieve by ticket number")
    public void testSaveAndRetrieveMissingItemClaim() {
        CustomerTicket ticket = new CustomerTicket();
        ticket.setTicketType("Missing_Item");
        ticket.setOrderId(1);
        ticket.setCustomerName("Sunita Rao");
        ticket.setCustomerPhone("9822334455");
        ticket.setCategory("Missing Garment");
        ticket.setSubject("Missing Silk Scarf");
        ticket.setDescription("Scarf was in the basket but missing from the delivery pack.");
        ticket.setItemDetails("1 Red Silk Scarf (Fabindia)");
        ticket.setAmountClaimed(new BigDecimal("650.00"));

        CustomerTicket saved = ticketDAO.save(ticket);
        assertNotNull(saved);
        assertTrue(saved.getTicketNumber().startsWith("MIS-"));

        CustomerTicket found = ticketDAO.findByTicketNumber(saved.getTicketNumber());
        assertNotNull(found);
        assertEquals("1 Red Silk Scarf (Fabindia)", found.getItemDetails());
        assertEquals(0, new BigDecimal("650.00").compareTo(found.getAmountClaimed()));
    }

    @Test
    @DisplayName("Should save a refund request and update resolution notes")
    public void testRefundRequestAndResolution() {
        CustomerTicket ticket = new CustomerTicket();
        ticket.setTicketType("Refund_Request");
        ticket.setCustomerName("Karan Mehta");
        ticket.setCustomerPhone("9833445566");
        ticket.setCategory("Damaged Fabric");
        ticket.setSubject("Torn pocket on linen trousers");
        ticket.setDescription("Pocket ripped during spin cycle.");
        ticket.setAmountClaimed(new BigDecimal("500.00"));
        ticket.setPayoutDetails("karan@okaxis");

        CustomerTicket saved = ticketDAO.save(ticket);
        assertTrue(saved.getTicketNumber().startsWith("REF-"));

        ticketDAO.updateStatusAndNotes(saved.getTicketId(), "Resolved", "Refund of ₹500 credited via UPI Ref #UPI12345");

        CustomerTicket updated = ticketDAO.findById(saved.getTicketId());
        assertEquals("Resolved", updated.getStatus());
        assertTrue(updated.getAdminNotes().contains("UPI12345"));
    }

    @Test
    @DisplayName("Should calculate ticket summary statistics")
    public void testTicketStats() {
        Map<String, Object> stats = ticketDAO.getTicketStats();
        assertNotNull(stats);
        assertTrue(((Number) stats.get("totalTickets")).intValue() > 0);
    }
}
