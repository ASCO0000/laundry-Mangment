package com.laundry.api;

import com.laundry.dao.CustomerDAO;
import com.laundry.dao.CustomerTicketDAO;
import com.laundry.dao.DeliveryDAO;
import com.laundry.dao.OrderDAO;
import com.laundry.dao.OrderStatusHistoryDAO;
import com.laundry.dao.ReportDAO;
import com.laundry.dao.ServiceDAO;
import com.laundry.dao.UserDAO;
import com.laundry.model.Customer;
import com.laundry.model.CustomerTicket;
import com.laundry.model.Delivery;
import com.laundry.model.Order;
import com.laundry.model.OrderItem;
import com.laundry.model.OrderStatusHistory;
import com.laundry.model.Payment;
import com.laundry.model.Service;
import com.laundry.model.User;
import com.laundry.model.enums.OrderStatus;
import com.laundry.model.enums.PaymentMethod;
import com.laundry.service.AuthenticationService;
import com.laundry.service.BillingService;
import com.laundry.service.CustomerService;
import com.laundry.service.LaundryService;
import com.laundry.service.OrderService;
import com.laundry.util.DateUtil;
import com.laundry.util.ReceiptUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unified Embedded HTTP Server hosting both:
 * 1. Customer Android Mobile Application & REST API
 * 2. Full Admin Management Control Center Web Portal & REST API
 */
public class CustomerApiServer {

    private static final Logger LOGGER = Logger.getLogger(CustomerApiServer.class.getName());
    private static final int PORT = 8080;
    private static HttpServer server;

    private static final AuthenticationService authService = new AuthenticationService();
    private static final ServiceDAO serviceDAO = new ServiceDAO();
    private static final LaundryService laundryService = new LaundryService(serviceDAO);
    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final CustomerService customerService = new CustomerService(customerDAO);
    private static final OrderDAO orderDAO = new OrderDAO();
    private static final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAO();
    private static final OrderService orderService = new OrderService(orderDAO, customerDAO, historyDAO);
    private static final BillingService billingService = new BillingService();
    private static final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private static final ReportDAO reportDAO = new ReportDAO();
    private static final UserDAO userDAO = new UserDAO();
    private static final CustomerTicketDAO ticketDAO = new CustomerTicketDAO();

    public static synchronized void start() {
        if (server != null) return;
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Customer & Shared API Endpoints
            server.createContext("/api/services", new ServicesHandler());
            server.createContext("/api/orders/track", new TrackOrderHandler());
            server.createContext("/api/orders/details", new OrderDetailsHandler());
            server.createContext("/api/orders/status", new OrderStatusHandler());
            server.createContext("/api/orders", new OrdersHandler());

            // Support, Missing Garments, Complaints & Refunds
            server.createContext("/api/tickets/track", new TrackTicketHandler());
            server.createContext("/api/tickets", new TicketsHandler());
            server.createContext("/api/customers/history", new CustomerHistoryHandler());

            // Admin Management Endpoints
            server.createContext("/api/auth/login", new AuthLoginHandler());
            server.createContext("/api/admin/stats", new AdminStatsHandler());
            server.createContext("/api/admin/tickets/resolve", new ResolveTicketHandler());
            server.createContext("/api/admin/tickets/stats", new TicketStatsHandler());
            server.createContext("/api/customers", new CustomersHandler());
            server.createContext("/api/billing/pay", new BillingPayHandler());
            server.createContext("/api/deliveries", new DeliveriesHandler());
            server.createContext("/api/reports", new ReportsHandler());

            // Static Web Routes:
            // "/" -> Customer Mobile App
            // "/admin" -> Admin Web Management Portal
            server.createContext("/", new StaticWebHandler());

            server.setExecutor(Executors.newFixedThreadPool(15));
            server.start();
            LOGGER.info("Laundry Unified Server (Customer + Admin Portal) active on http://localhost:" + PORT);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to start Laundry HTTP Server on port " + PORT + ": " + e.getMessage(), e);
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            LOGGER.info("Laundry Unified Server stopped.");
        }
    }

    public static int getPort() {
        return PORT;
    }

    // =============================================================
    // 1. AUTHENTICATION HANDLER (/api/auth/login)
    // =============================================================
    private static class AuthLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                String user = extractJsonString(body, "username");
                String pass = extractJsonString(body, "password");

                if (user == null || pass == null) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Username and password required\"}");
                    return;
                }

                User authenticated = authService.login(user.trim(), pass);
                if (authenticated != null) {
                    String json = "{"
                            + "\"success\":true,"
                            + "\"user\":{"
                            + "\"userId\":" + authenticated.getUserId() + ","
                            + "\"username\":\"" + escapeJson(authenticated.getUsername()) + "\","
                            + "\"fullName\":\"" + escapeJson(authenticated.getFullName()) + "\","
                            + "\"role\":\"" + authenticated.getRole().name() + "\""
                            + "}"
                            + "}";
                    sendJsonResponse(exchange, 200, json);
                } else {
                    sendJsonResponse(exchange, 401, "{\"error\":\"Invalid username or password\"}");
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 401, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 2. ADMIN STATS & KPIS (/api/admin/stats)
    // =============================================================
    private static class AdminStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                Map<String, Object> metrics = reportDAO.getDashboardMetrics();
                Map<String, Object> srvSummary = serviceDAO.getServiceSummary();
                Map<String, BigDecimal> monthly = reportDAO.getMonthlyRevenueTrend();

                StringBuilder json = new StringBuilder("{");
                json.append("\"totalOrders\":").append(metrics.get("totalOrders")).append(",")
                    .append("\"pendingOrders\":").append(metrics.get("pendingOrders")).append(",")
                    .append("\"completedOrders\":").append(metrics.get("completedOrders")).append(",")
                    .append("\"totalCustomers\":").append(metrics.get("totalCustomers")).append(",")
                    .append("\"todayRevenue\":").append(metrics.get("todayRevenue")).append(",")
                    .append("\"totalRevenue\":").append(metrics.get("totalRevenue")).append(",")
                    .append("\"outstandingPayments\":").append(metrics.get("outstandingPayments")).append(",")
                    .append("\"totalServices\":").append(srvSummary.get("totalServices")).append(",")
                    .append("\"activeServices\":").append(srvSummary.get("activeServices")).append(",")
                    .append("\"inactiveServices\":").append(srvSummary.get("inactiveServices")).append(",")
                    .append("\"avgServicePrice\":").append(srvSummary.get("averagePrice")).append(",");

                // Monthly trend
                json.append("\"monthlyTrend\":{");
                int idx = 0;
                for (Map.Entry<String, BigDecimal> entry : monthly.entrySet()) {
                    json.append("\"").append(escapeJson(entry.getKey())).append("\":").append(entry.getValue());
                    if (++idx < monthly.size()) json.append(",");
                }
                json.append("}}");

                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 3. SERVICES HANDLER (/api/services)
    // =============================================================
    private static class ServicesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equals(method)) {
                    Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                    boolean all = "true".equalsIgnoreCase(q.get("all"));
                    String keyword = q.get("keyword");

                    List<Service> services;
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        services = laundryService.searchServices(keyword);
                    } else if (all) {
                        services = laundryService.getAllServices();
                    } else {
                        services = laundryService.getActiveServices();
                    }

                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < services.size(); i++) {
                        Service s = services.get(i);
                        json.append("{")
                            .append("\"serviceId\":").append(s.getServiceId()).append(",")
                            .append("\"serviceName\":\"").append(escapeJson(s.getServiceName())).append("\",")
                            .append("\"description\":\"").append(escapeJson(s.getDescription())).append("\",")
                            .append("\"pricingUnit\":\"").append(escapeJson(s.getPricingUnit())).append("\",")
                            .append("\"price\":").append(s.getPrice()).append(",")
                            .append("\"active\":").append(s.isActive())
                            .append("}");
                        if (i < services.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());

                } else if ("POST".equals(method)) {
                    // Create Service
                    String body = readRequestBody(exchange);
                    Service s = new Service();
                    s.setServiceName(extractJsonString(body, "serviceName"));
                    s.setDescription(extractJsonString(body, "description"));
                    s.setPricingUnit(extractJsonString(body, "pricingUnit"));
                    String priceStr = extractJsonString(body, "price");
                    s.setPrice(priceStr != null ? new BigDecimal(priceStr) : BigDecimal.ZERO);
                    s.setActive(!"false".equalsIgnoreCase(extractJsonString(body, "active")));

                    Service saved = laundryService.addService(s);
                    sendJsonResponse(exchange, 201, "{\"success\":true,\"serviceId\":" + saved.getServiceId() + "}");

                } else if ("PUT".equals(method)) {
                    // Update Service
                    String body = readRequestBody(exchange);
                    String idStr = extractJsonString(body, "serviceId");
                    if (idStr == null) {
                        sendJsonResponse(exchange, 400, "{\"error\":\"serviceId is required\"}");
                        return;
                    }
                    Service s = new Service();
                    s.setServiceId(Integer.parseInt(idStr));
                    s.setServiceName(extractJsonString(body, "serviceName"));
                    s.setDescription(extractJsonString(body, "description"));
                    s.setPricingUnit(extractJsonString(body, "pricingUnit"));
                    String priceStr = extractJsonString(body, "price");
                    s.setPrice(priceStr != null ? new BigDecimal(priceStr) : BigDecimal.ZERO);
                    s.setActive(!"false".equalsIgnoreCase(extractJsonString(body, "active")));

                    laundryService.updateService(s);
                    sendJsonResponse(exchange, 200, "{\"success\":true}");

                } else if ("DELETE".equals(method)) {
                    Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                    String idStr = q.get("id");
                    if (idStr == null) {
                        sendJsonResponse(exchange, 400, "{\"error\":\"Service ID required\"}");
                        return;
                    }
                    laundryService.deleteService(Integer.parseInt(idStr));
                    sendJsonResponse(exchange, 200, "{\"success\":true}");

                } else if ("PATCH".equals(method)) {
                    Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                    String idStr = q.get("id");
                    String activeStr = q.get("active");
                    if (idStr != null && activeStr != null) {
                        laundryService.toggleServiceStatus(Integer.parseInt(idStr), Boolean.parseBoolean(activeStr));
                        sendJsonResponse(exchange, 200, "{\"success\":true}");
                    } else {
                        sendJsonResponse(exchange, 400, "{\"error\":\"id and active params required\"}");
                    }
                } else {
                    sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 4. CUSTOMERS HANDLER (/api/customers)
    // =============================================================
    private static class CustomersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equals(method)) {
                    Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                    String keyword = q.get("keyword");
                    List<Customer> list = (keyword != null && !keyword.trim().isEmpty())
                            ? customerService.searchCustomers(keyword)
                            : customerService.getAllCustomers();

                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Customer c = list.get(i);
                        json.append("{")
                            .append("\"customerId\":").append(c.getCustomerId()).append(",")
                            .append("\"fullName\":\"").append(escapeJson(c.getFullName())).append("\",")
                            .append("\"phone\":\"").append(escapeJson(c.getPhone())).append("\",")
                            .append("\"email\":\"").append(escapeJson(c.getEmail())).append("\",")
                            .append("\"address\":\"").append(escapeJson(c.getAddress())).append("\",")
                            .append("\"dateRegistered\":\"").append(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "").append("\"")
                            .append("}");
                        if (i < list.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());

                } else if ("POST".equals(method)) {
                    String body = readRequestBody(exchange);
                    Customer c = new Customer();
                    c.setFullName(extractJsonString(body, "fullName"));
                    c.setPhone(extractJsonString(body, "phone"));
                    c.setEmail(extractJsonString(body, "email"));
                    c.setAddress(extractJsonString(body, "address"));

                    Customer saved = customerService.addCustomer(c);
                    sendJsonResponse(exchange, 201, "{\"success\":true,\"customerId\":" + saved.getCustomerId() + "}");

                } else if ("PUT".equals(method)) {
                    String body = readRequestBody(exchange);
                    String idStr = extractJsonString(body, "customerId");
                    if (idStr == null) {
                        sendJsonResponse(exchange, 400, "{\"error\":\"customerId is required\"}");
                        return;
                    }
                    Customer c = new Customer();
                    c.setCustomerId(Integer.parseInt(idStr));
                    c.setFullName(extractJsonString(body, "fullName"));
                    c.setPhone(extractJsonString(body, "phone"));
                    c.setEmail(extractJsonString(body, "email"));
                    c.setAddress(extractJsonString(body, "address"));

                    customerService.updateCustomer(c);
                    sendJsonResponse(exchange, 200, "{\"success\":true}");

                } else if ("DELETE".equals(method)) {
                    Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                    String idStr = q.get("id");
                    if (idStr == null) {
                        sendJsonResponse(exchange, 400, "{\"error\":\"Customer ID required\"}");
                        return;
                    }
                    customerService.deleteCustomer(Integer.parseInt(idStr));
                    sendJsonResponse(exchange, 200, "{\"success\":true}");
                } else {
                    sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 5. ORDERS HANDLER (/api/orders)
    // =============================================================
    private static class OrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equals(method)) {
                    Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                    String status = q.getOrDefault("status", "All");
                    String keyword = q.getOrDefault("keyword", "");

                    List<Order> orders = orderService.getAllOrders(status, keyword);
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < orders.size(); i++) {
                        Order o = orders.get(i);
                        json.append("{")
                            .append("\"orderId\":").append(o.getOrderId()).append(",")
                            .append("\"customerId\":").append(o.getCustomerId()).append(",")
                            .append("\"customerName\":\"").append(escapeJson(o.getCustomerName())).append("\",")
                            .append("\"customerPhone\":\"").append(escapeJson(o.getCustomerPhone())).append("\",")
                            .append("\"orderDate\":\"").append(DateUtil.formatDateTime(o.getOrderDate())).append("\",")
                            .append("\"expectedDeliveryDate\":\"").append(DateUtil.formatDate(o.getExpectedDeliveryDate())).append("\",")
                            .append("\"status\":\"").append(o.getStatus().getDisplayName()).append("\",")
                            .append("\"totalAmount\":").append(o.getTotalAmount()).append(",")
                            .append("\"paidAmount\":").append(o.getPaidAmount()).append(",")
                            .append("\"balanceDue\":").append(o.getBalanceDue())
                            .append("}");
                        if (i < orders.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());

                } else if ("POST".equals(method)) {
                    // Create Order
                    String body = readRequestBody(exchange);
                    String customerIdStr = extractJsonString(body, "customerId");
                    String fullName = extractJsonString(body, "fullName");
                    String phone = extractJsonString(body, "phone");
                    String address = extractJsonString(body, "address");
                    String instructions = extractJsonString(body, "specialInstructions");
                    String deliveryDateStr = extractJsonString(body, "expectedDeliveryDate");
                    String discountStr = extractJsonString(body, "discount");

                    int custId;
                    if (customerIdStr != null && !customerIdStr.isEmpty()) {
                        custId = Integer.parseInt(customerIdStr);
                    } else {
                        // Customer Mobile App Flow: Find or create
                        if (phone == null || phone.trim().isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Phone number required\"}");
                            return;
                        }
                        Customer existing = customerDAO.findByPhone(phone.trim());
                        if (existing == null) {
                            Customer c = new Customer();
                            c.setFullName(fullName != null ? fullName.trim() : "Walk-in Customer");
                            c.setPhone(phone.trim());
                            c.setAddress(address != null ? address.trim() : "");
                            existing = customerDAO.save(c);
                        }
                        custId = existing.getCustomerId();
                    }

                    List<OrderItem> items = parseOrderItems(body);
                    if (items.isEmpty()) {
                        sendJsonResponse(exchange, 400, "{\"error\":\"At least one service must be selected.\"}");
                        return;
                    }

                    LocalDate expDate = LocalDate.now().plusDays(2);
                    if (deliveryDateStr != null && !deliveryDateStr.trim().isEmpty()) {
                        try {
                            expDate = LocalDate.parse(deliveryDateStr.trim());
                        } catch (Exception ignored) {}
                    }

                    Order order = new Order();
                    order.setCustomerId(custId);
                    order.setOrderDate(LocalDateTime.now());
                    order.setExpectedDeliveryDate(expDate);
                    order.setStatus(OrderStatus.RECEIVED);
                    order.setSpecialInstructions(instructions != null ? instructions.trim() : "Placed via System");
                    order.setCreatedBy(1);
                    if (discountStr != null && !discountStr.isEmpty()) {
                        try {
                            order.setDiscount(new BigDecimal(discountStr));
                        } catch (Exception ignored) {}
                    }
                    order.setItems(items);
                    order.recalculateTotals();

                    Order saved = orderService.createOrder(order);

                    String res = "{"
                            + "\"success\":true,"
                            + "\"orderId\":" + saved.getOrderId() + ","
                            + "\"customerName\":\"" + escapeJson(saved.getCustomerName()) + "\","
                            + "\"totalAmount\":" + saved.getTotalAmount() + ","
                            + "\"status\":\"" + saved.getStatus().getDisplayName() + "\""
                            + "}";
                    sendJsonResponse(exchange, 201, res);

                } else {
                    sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 6. ORDER DETAILS HANDLER (/api/orders/details?orderId=...)
    // =============================================================
    private static class OrderDetailsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String orderIdStr = params.get("orderId");
            if (orderIdStr == null || orderIdStr.isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"error\":\"orderId parameter is required\"}");
                return;
            }

            try {
                int orderId = Integer.parseInt(orderIdStr.replace("ORD-", "").trim());
                Order order = orderService.getOrderById(orderId);
                if (order == null) {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Order not found\"}");
                    return;
                }

                // Balance and paid details
                billingService.getOrderBillingDetails(orderId);
                List<OrderStatusHistory> history = historyDAO.findByOrderId(orderId);

                Customer cust = customerDAO.findById(order.getCustomerId());
                String custAddress = (cust != null && cust.getAddress() != null) ? cust.getAddress() : "";

                StringBuilder json = new StringBuilder("{");
                json.append("\"orderId\":").append(order.getOrderId()).append(",")
                    .append("\"customerId\":").append(order.getCustomerId()).append(",")
                    .append("\"customerName\":\"").append(escapeJson(order.getCustomerName())).append("\",")
                    .append("\"customerPhone\":\"").append(escapeJson(order.getCustomerPhone())).append("\",")
                    .append("\"customerAddress\":\"").append(escapeJson(custAddress)).append("\",")
                    .append("\"orderDate\":\"").append(DateUtil.formatDateTime(order.getOrderDate())).append("\",")
                    .append("\"expectedDeliveryDate\":\"").append(DateUtil.formatDate(order.getExpectedDeliveryDate())).append("\",")
                    .append("\"status\":\"").append(order.getStatus().getDisplayName()).append("\",")
                    .append("\"subtotal\":").append(order.getSubtotal()).append(",")
                    .append("\"discount\":").append(order.getDiscount()).append(",")
                    .append("\"totalAmount\":").append(order.getTotalAmount()).append(",")
                    .append("\"paidAmount\":").append(order.getPaidAmount()).append(",")
                    .append("\"balanceDue\":").append(order.getBalanceDue()).append(",")
                    .append("\"specialInstructions\":\"").append(escapeJson(order.getSpecialInstructions())).append("\",");

                // Line items
                json.append("\"items\":[");
                if (order.getItems() != null) {
                    for (int i = 0; i < order.getItems().size(); i++) {
                        OrderItem it = order.getItems().get(i);
                        json.append("{")
                            .append("\"serviceName\":\"").append(escapeJson(it.getServiceName())).append("\",")
                            .append("\"itemDescription\":\"").append(escapeJson(it.getItemDescription())).append("\",")
                            .append("\"quantity\":").append(it.getQuantity()).append(",")
                            .append("\"unitPrice\":").append(it.getUnitPrice()).append(",")
                            .append("\"subtotal\":").append(it.getSubtotal())
                            .append("}");
                        if (i < order.getItems().size() - 1) json.append(",");
                    }
                }
                json.append("],");

                // Status history
                json.append("\"history\":[");
                for (int i = 0; i < history.size(); i++) {
                    OrderStatusHistory h = history.get(i);
                    json.append("{")
                        .append("\"status\":\"").append(escapeJson(h.getNewStatus())).append("\",")
                        .append("\"timestamp\":\"").append(DateUtil.formatDateTime(h.getChangedAt())).append("\",")
                        .append("\"changedBy\":\"").append(escapeJson(h.getChangedByName() != null ? h.getChangedByName() : "Staff")).append("\",")
                        .append("\"remarks\":\"").append(escapeJson(h.getRemarks())).append("\"")
                        .append("}");
                    if (i < history.size() - 1) json.append(",");
                }
                json.append("]}");

                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 7. ORDER STATUS UPDATE (/api/orders/status)
    // =============================================================
    private static class OrderStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                String idStr = extractJsonString(body, "orderId");
                String statusStr = extractJsonString(body, "status");
                String remarks = extractJsonString(body, "remarks");
                String userIdStr = extractJsonString(body, "userId");

                if (idStr == null || statusStr == null) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"orderId and status required\"}");
                    return;
                }

                int orderId = Integer.parseInt(idStr.replace("ORD-", "").trim());
                OrderStatus status = OrderStatus.fromString(statusStr);
                int userId = userIdStr != null ? Integer.parseInt(userIdStr) : 1;

                orderService.updateOrderStatus(orderId, status, userId, remarks != null ? remarks : "Updated via Web Portal");
                sendJsonResponse(exchange, 200, "{\"success\":true,\"status\":\"" + status.getDisplayName() + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 8. BILLING PAYMENT HANDLER (/api/billing/pay)
    // =============================================================
    private static class BillingPayHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                String idStr = extractJsonString(body, "orderId");
                String amountStr = extractJsonString(body, "amount");
                String methodStr = extractJsonString(body, "paymentMethod");
                String refStr = extractJsonString(body, "transactionReference");

                if (idStr == null || amountStr == null || methodStr == null) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"orderId, amount, and paymentMethod required\"}");
                    return;
                }

                int orderId = Integer.parseInt(idStr.replace("ORD-", "").trim());
                BigDecimal amount = new BigDecimal(amountStr);
                PaymentMethod method = PaymentMethod.fromString(methodStr);

                Payment payment = billingService.recordPayment(orderId, amount, method, refStr);
                sendJsonResponse(exchange, 201, "{\"success\":true,\"paymentId\":" + payment.getPaymentId() + "}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 9. DELIVERIES HANDLER (/api/deliveries)
    // =============================================================
    private static class DeliveriesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equals(method)) {
                    List<Delivery> deliveries = deliveryDAO.findAll("All", "All");
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < deliveries.size(); i++) {
                        Delivery d = deliveries.get(i);
                        json.append("{")
                            .append("\"deliveryId\":").append(d.getDeliveryId()).append(",")
                            .append("\"orderId\":").append(d.getOrderId()).append(",")
                            .append("\"customerName\":\"").append(escapeJson(d.getCustomerName())).append("\",")
                            .append("\"customerPhone\":\"").append(escapeJson(d.getCustomerPhone())).append("\",")
                            .append("\"deliveryType\":\"").append(d.getDeliveryType().getDisplayName()).append("\",")
                            .append("\"address\":\"").append(escapeJson(d.getAddress())).append("\",")
                            .append("\"scheduledDateTime\":\"").append(d.getScheduledDatetime() != null ? DateUtil.formatDateTime(d.getScheduledDatetime()) : "").append("\",")
                            .append("\"assignedStaffName\":\"").append(escapeJson(d.getAssignedStaffName())).append("\",")
                            .append("\"status\":\"").append(d.getStatus().getDisplayName()).append("\"")
                            .append("}");
                        if (i < deliveries.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());

                } else if ("POST".equals(method)) {
                    String body = readRequestBody(exchange);
                    String idStr = extractJsonString(body, "deliveryId");
                    String statusStr = extractJsonString(body, "status");
                    if (idStr != null && statusStr != null) {
                        deliveryDAO.updateStatus(Integer.parseInt(idStr), com.laundry.model.enums.DeliveryStatus.fromString(statusStr));
                        sendJsonResponse(exchange, 200, "{\"success\":true}");
                    } else {
                        sendJsonResponse(exchange, 400, "{\"error\":\"deliveryId and status required\"}");
                    }
                } else {
                    sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 10. FINANCIAL REPORTS HANDLER (/api/reports)
    // =============================================================
    private static class ReportsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                String startStr = q.get("start");
                String endStr = q.get("end");

                LocalDate start = (startStr != null && !startStr.isEmpty()) ? LocalDate.parse(startStr) : LocalDate.now().minusDays(30);
                LocalDate end = (endStr != null && !endStr.isEmpty()) ? LocalDate.parse(endStr) : LocalDate.now();

                List<Map<String, Object>> journal = reportDAO.getPaymentCollectionReport(start, end);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < journal.size(); i++) {
                    Map<String, Object> row = journal.get(i);
                    json.append("{")
                        .append("\"paymentId\":").append(row.get("paymentId")).append(",")
                        .append("\"orderId\":").append(row.get("orderId")).append(",")
                        .append("\"paymentDate\":\"").append(row.get("paymentDate")).append("\",")
                        .append("\"customerName\":\"").append(escapeJson(String.valueOf(row.get("customerName")))).append("\",")
                        .append("\"amount\":").append(row.get("amount")).append(",")
                        .append("\"paymentMethod\":\"").append(row.get("paymentMethod")).append("\",")
                        .append("\"transactionReference\":\"").append(escapeJson(String.valueOf(row.get("reference")))).append("\",")
                        .append("\"status\":\"").append(row.get("status")).append("\"")
                        .append("}");
                    if (i < journal.size() - 1) json.append(",");
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 11. CUSTOMER ORDER TRACKING (/api/orders/track)
    // =============================================================
    private static class TrackOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String orderIdStr = params.get("orderId");
            String phone = params.get("phone");

            Order foundOrder = null;
            if (orderIdStr != null && !orderIdStr.trim().isEmpty()) {
                try {
                    int orderId = Integer.parseInt(orderIdStr.trim().replace("ORD-", ""));
                    foundOrder = orderDAO.findById(orderId);
                } catch (Exception ignored) {}
            } else if (phone != null && !phone.trim().isEmpty()) {
                Customer c = customerDAO.findByPhone(phone.trim());
                if (c != null) {
                    List<Order> orders = orderDAO.findAll("All", phone.trim());
                    if (!orders.isEmpty()) {
                        foundOrder = orderDAO.findById(orders.get(0).getOrderId());
                    }
                }
            }

            if (foundOrder == null) {
                sendJsonResponse(exchange, 404, "{\"error\":\"No active order found matching the given Order ID or Phone number.\"}");
                return;
            }

            billingService.getOrderBillingDetails(foundOrder.getOrderId());
            List<OrderStatusHistory> history = historyDAO.findByOrderId(foundOrder.getOrderId());

            StringBuilder json = new StringBuilder("{");
            json.append("\"orderId\":").append(foundOrder.getOrderId()).append(",")
                .append("\"customerName\":\"").append(escapeJson(foundOrder.getCustomerName())).append("\",")
                .append("\"customerPhone\":\"").append(escapeJson(foundOrder.getCustomerPhone())).append("\",")
                .append("\"orderDate\":\"").append(DateUtil.formatDateTime(foundOrder.getOrderDate())).append("\",")
                .append("\"expectedDeliveryDate\":\"").append(DateUtil.formatDate(foundOrder.getExpectedDeliveryDate())).append("\",")
                .append("\"status\":\"").append(foundOrder.getStatus().getDisplayName()).append("\",")
                .append("\"subtotal\":").append(foundOrder.getSubtotal()).append(",")
                .append("\"discount\":").append(foundOrder.getDiscount()).append(",")
                .append("\"totalAmount\":").append(foundOrder.getTotalAmount()).append(",")
                .append("\"paidAmount\":").append(foundOrder.getPaidAmount()).append(",")
                .append("\"balanceDue\":").append(foundOrder.getBalanceDue()).append(",")
                .append("\"specialInstructions\":\"").append(escapeJson(foundOrder.getSpecialInstructions())).append("\",");

            // Line items
            json.append("\"items\":[");
            if (foundOrder.getItems() != null) {
                for (int i = 0; i < foundOrder.getItems().size(); i++) {
                    OrderItem item = foundOrder.getItems().get(i);
                    json.append("{")
                        .append("\"serviceName\":\"").append(escapeJson(item.getServiceName())).append("\",")
                        .append("\"itemDescription\":\"").append(escapeJson(item.getItemDescription())).append("\",")
                        .append("\"quantity\":").append(item.getQuantity()).append(",")
                        .append("\"unitPrice\":").append(item.getUnitPrice()).append(",")
                        .append("\"subtotal\":").append(item.getSubtotal())
                        .append("}");
                    if (i < foundOrder.getItems().size() - 1) json.append(",");
                }
            }
            json.append("],");

            // Timeline history
            json.append("\"history\":[");
            for (int i = 0; i < history.size(); i++) {
                OrderStatusHistory h = history.get(i);
                json.append("{")
                    .append("\"status\":\"").append(escapeJson(h.getNewStatus())).append("\",")
                    .append("\"timestamp\":\"").append(DateUtil.formatDateTime(h.getChangedAt())).append("\",")
                    .append("\"remarks\":\"").append(escapeJson(h.getRemarks())).append("\"")
                    .append("}");
                if (i < history.size() - 1) json.append(",");
            }
            json.append("]}");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    // =============================================================
    // 12. TICKETS HANDLER (/api/tickets)
    // =============================================================
    private static class TicketsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equals(method)) {
                    Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                    String phone = q.get("phone");
                    String orderIdStr = q.get("orderId");
                    String type = q.getOrDefault("type", "All");
                    String status = q.getOrDefault("status", "All");
                    String keyword = q.getOrDefault("keyword", "");

                    List<CustomerTicket> list;
                    if (phone != null && !phone.trim().isEmpty()) {
                        list = ticketDAO.findByPhone(phone.trim());
                    } else if (orderIdStr != null && !orderIdStr.trim().isEmpty()) {
                        list = ticketDAO.findByOrderId(Integer.parseInt(orderIdStr.replace("ORD-", "").trim()));
                    } else {
                        list = ticketDAO.findAll(type, status, keyword);
                    }

                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        json.append(serializeTicketJson(list.get(i)));
                        if (i < list.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());

                } else if ("POST".equals(method)) {
                    String body = readRequestBody(exchange);
                    String type = extractJsonString(body, "ticketType");
                    String orderIdStr = extractJsonString(body, "orderId");
                    String name = extractJsonString(body, "customerName");
                    String phone = extractJsonString(body, "customerPhone");
                    String category = extractJsonString(body, "category");
                    String subject = extractJsonString(body, "subject");
                    String desc = extractJsonString(body, "description");
                    String itemDetails = extractJsonString(body, "itemDetails");
                    String amountStr = extractJsonString(body, "amountClaimed");
                    String payout = extractJsonString(body, "payoutDetails");
                    String priority = extractJsonString(body, "priority");

                    if (name == null || phone == null || subject == null || desc == null) {
                        sendJsonResponse(exchange, 400, "{\"error\":\"Customer name, phone, subject, and description are required.\"}");
                        return;
                    }

                    CustomerTicket t = new CustomerTicket();
                    t.setTicketType(type != null ? type.trim() : "Complaint");
                    if (orderIdStr != null && !orderIdStr.trim().isEmpty()) {
                        try {
                            t.setOrderId(Integer.parseInt(orderIdStr.replace("ORD-", "").trim()));
                        } catch (Exception ignored) {}
                    }
                    t.setCustomerName(name.trim());
                    t.setCustomerPhone(phone.trim());
                    t.setCategory(category != null ? category.trim() : "General");
                    t.setSubject(subject.trim());
                    t.setDescription(desc.trim());
                    t.setItemDetails(itemDetails != null ? itemDetails.trim() : null);
                    if (amountStr != null && !amountStr.trim().isEmpty()) {
                        try {
                            t.setAmountClaimed(new BigDecimal(amountStr.trim()));
                        } catch (Exception ignored) {}
                    }
                    t.setPayoutDetails(payout != null ? payout.trim() : null);
                    t.setPriority(priority != null ? priority.trim() : "Medium");
                    t.setStatus("Open");

                    // Link customer ID if exists
                    Customer c = customerDAO.findByPhone(phone.trim());
                    if (c != null) {
                        t.setCustomerId(c.getCustomerId());
                    }

                    CustomerTicket saved = ticketDAO.save(t);
                    String res = "{"
                            + "\"success\":true,"
                            + "\"ticketId\":" + saved.getTicketId() + ","
                            + "\"ticketNumber\":\"" + escapeJson(saved.getTicketNumber()) + "\","
                            + "\"status\":\"" + escapeJson(saved.getStatus()) + "\""
                            + "}";
                    sendJsonResponse(exchange, 201, res);

                } else {
                    sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 13. TRACK TICKET HANDLER (/api/tickets/track)
    // =============================================================
    private static class TrackTicketHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String num = params.get("ticketNumber");
            String idStr = params.get("id");
            String phone = params.get("phone");

            CustomerTicket found = null;
            if (num != null && !num.trim().isEmpty()) {
                found = ticketDAO.findByTicketNumber(num.trim());
            } else if (idStr != null && !idStr.trim().isEmpty()) {
                try {
                    found = ticketDAO.findById(Integer.parseInt(idStr.trim()));
                } catch (Exception ignored) {}
            } else if (phone != null && !phone.trim().isEmpty()) {
                List<CustomerTicket> list = ticketDAO.findByPhone(phone.trim());
                if (!list.isEmpty()) {
                    found = list.get(0);
                }
            }

            if (found == null) {
                sendJsonResponse(exchange, 404, "{\"error\":\"No ticket found matching the given Ticket Number or Phone.\"}");
                return;
            }

            sendJsonResponse(exchange, 200, serializeTicketJson(found));
        }
    }

    // =============================================================
    // 14. ADMIN RESOLVE TICKET HANDLER (/api/admin/tickets/resolve)
    // =============================================================
    private static class ResolveTicketHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                String idStr = extractJsonString(body, "ticketId");
                String status = extractJsonString(body, "status");
                String notes = extractJsonString(body, "adminNotes");

                if (idStr == null || status == null) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"ticketId and status are required\"}");
                    return;
                }

                int ticketId = Integer.parseInt(idStr.trim());
                ticketDAO.updateStatusAndNotes(ticketId, status.trim(), notes != null ? notes.trim() : "");
                sendJsonResponse(exchange, 200, "{\"success\":true,\"ticketId\":" + ticketId + ",\"status\":\"" + escapeJson(status.trim()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 15. ADMIN TICKET STATS HANDLER (/api/admin/tickets/stats)
    // =============================================================
    private static class TicketStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                Map<String, Object> stats = ticketDAO.getTicketStats();
                String json = "{"
                        + "\"totalTickets\":" + stats.getOrDefault("totalTickets", 0) + ","
                        + "\"activeTickets\":" + stats.getOrDefault("activeTickets", 0) + ","
                        + "\"openComplaints\":" + stats.getOrDefault("openComplaints", 0) + ","
                        + "\"missingItems\":" + stats.getOrDefault("missingItems", 0) + ","
                        + "\"pendingRefunds\":" + stats.getOrDefault("pendingRefunds", 0) + ","
                        + "\"totalRefundAmount\":" + stats.getOrDefault("totalRefundAmount", 0)
                        + "}";
                sendJsonResponse(exchange, 200, json);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =============================================================
    // 16. CUSTOMER HISTORY & REPORTS HANDLER (/api/customers/history)
    // =============================================================
    private static class CustomerHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
            String phone = q.get("phone");
            if (phone == null || phone.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"error\":\"Phone parameter is required\"}");
                return;
            }

            try {
                Customer cust = customerDAO.findByPhone(phone.trim());
                List<Order> orders = orderDAO.findAll("All", phone.trim());
                List<CustomerTicket> tickets = ticketDAO.findByPhone(phone.trim());

                BigDecimal totalSpent = BigDecimal.ZERO;
                int activeCount = 0;
                int completedCount = 0;

                for (Order o : orders) {
                    if (o.getTotalAmount() != null) {
                        totalSpent = totalSpent.add(o.getTotalAmount());
                    }
                    if (o.getStatus() == OrderStatus.DELIVERED) {
                        completedCount++;
                    } else if (o.getStatus() != OrderStatus.CANCELLED) {
                        activeCount++;
                    }
                }

                StringBuilder json = new StringBuilder("{");
                json.append("\"success\":true,");

                // Customer info
                json.append("\"customer\":{");
                if (cust != null) {
                    json.append("\"customerId\":").append(cust.getCustomerId()).append(",")
                        .append("\"fullName\":\"").append(escapeJson(cust.getFullName())).append("\",")
                        .append("\"phone\":\"").append(escapeJson(cust.getPhone())).append("\",")
                        .append("\"email\":\"").append(escapeJson(cust.getEmail())).append("\",")
                        .append("\"address\":\"").append(escapeJson(cust.getAddress())).append("\"");
                } else {
                    json.append("\"fullName\":\"Guest Customer\",")
                        .append("\"phone\":\"").append(escapeJson(phone.trim())).append("\"");
                }
                json.append("},");

                // Stats
                json.append("\"stats\":{")
                    .append("\"totalOrders\":").append(orders.size()).append(",")
                    .append("\"totalSpent\":").append(totalSpent).append(",")
                    .append("\"activeOrders\":").append(activeCount).append(",")
                    .append("\"completedOrders\":").append(completedCount).append(",")
                    .append("\"totalTickets\":").append(tickets.size())
                    .append("},");

                // Orders
                json.append("\"orders\":[");
                for (int i = 0; i < orders.size(); i++) {
                    Order o = orders.get(i);
                    json.append("{")
                        .append("\"orderId\":").append(o.getOrderId()).append(",")
                        .append("\"orderDate\":\"").append(DateUtil.formatDateTime(o.getOrderDate())).append("\",")
                        .append("\"expectedDeliveryDate\":\"").append(DateUtil.formatDate(o.getExpectedDeliveryDate())).append("\",")
                        .append("\"status\":\"").append(o.getStatus().getDisplayName()).append("\",")
                        .append("\"subtotal\":").append(o.getSubtotal()).append(",")
                        .append("\"discount\":").append(o.getDiscount()).append(",")
                        .append("\"totalAmount\":").append(o.getTotalAmount()).append(",")
                        .append("\"paidAmount\":").append(o.getPaidAmount()).append(",")
                        .append("\"balanceDue\":").append(o.getBalanceDue()).append(",")
                        .append("\"instructions\":\"").append(escapeJson(o.getSpecialInstructions())).append("\"")
                        .append("}");
                    if (i < orders.size() - 1) json.append(",");
                }
                json.append("],");

                // Tickets
                json.append("\"tickets\":[");
                for (int i = 0; i < tickets.size(); i++) {
                    json.append(serializeTicketJson(tickets.get(i)));
                    if (i < tickets.size() - 1) json.append(",");
                }
                json.append("]}");

                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private static String serializeTicketJson(CustomerTicket t) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"ticketId\":").append(t.getTicketId()).append(",")
            .append("\"ticketNumber\":\"").append(escapeJson(t.getTicketNumber())).append("\",")
            .append("\"ticketType\":\"").append(escapeJson(t.getTicketType())).append("\",")
            .append("\"orderId\":").append(t.getOrderId() != null ? t.getOrderId() : "null").append(",")
            .append("\"customerId\":").append(t.getCustomerId() != null ? t.getCustomerId() : "null").append(",")
            .append("\"customerName\":\"").append(escapeJson(t.getCustomerName())).append("\",")
            .append("\"customerPhone\":\"").append(escapeJson(t.getCustomerPhone())).append("\",")
            .append("\"category\":\"").append(escapeJson(t.getCategory())).append("\",")
            .append("\"subject\":\"").append(escapeJson(t.getSubject())).append("\",")
            .append("\"description\":\"").append(escapeJson(t.getDescription())).append("\",")
            .append("\"itemDetails\":\"").append(escapeJson(t.getItemDetails())).append("\",")
            .append("\"amountClaimed\":").append(t.getAmountClaimed()).append(",")
            .append("\"payoutDetails\":\"").append(escapeJson(t.getPayoutDetails())).append("\",")
            .append("\"status\":\"").append(escapeJson(t.getStatus())).append("\",")
            .append("\"priority\":\"").append(escapeJson(t.getPriority())).append("\",")
            .append("\"adminNotes\":\"").append(escapeJson(t.getAdminNotes())).append("\",")
            .append("\"createdAt\":\"").append(t.getCreatedAt() != null ? DateUtil.formatDateTime(t.getCreatedAt()) : "").append("\",")
            .append("\"updatedAt\":\"").append(t.getUpdatedAt() != null ? DateUtil.formatDateTime(t.getUpdatedAt()) : "").append("\"")
            .append("}");
        return json.toString();
    }

    // =============================================================
    // 17. STATIC WEB HANDLER
    // Serves:
    //   - Customer Mobile App at "/" or "/index.html"
    //   - Admin Management Web Portal at "/admin" or "/admin/"
    //   - All static assets at "/web/*"
    // =============================================================
    private static class StaticWebHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/web/index.html"; // Customer App
            } else if (path.equals("/admin") || path.equals("/admin/") || path.equals("/admin/index.html")) {
                path = "/web/admin.html"; // Admin Management Portal
            } else if (!path.startsWith("/web/")) {
                path = "/web" + path;
            }

            try (InputStream is = CustomerApiServer.class.getResourceAsStream(path)) {
                if (is == null) {
                    // Fallback to customer app
                    try (InputStream fallback = CustomerApiServer.class.getResourceAsStream("/web/index.html")) {
                        if (fallback != null) {
                            byte[] bytes = fallback.readAllBytes();
                            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                            exchange.sendResponseHeaders(200, bytes.length);
                            try (OutputStream os = exchange.getResponseBody()) {
                                os.write(bytes);
                            }
                            return;
                        }
                    }
                    String notFound = "<h1>404 Not Found</h1>";
                    exchange.sendResponseHeaders(404, notFound.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound.getBytes(StandardCharsets.UTF_8));
                    }
                    return;
                }

                byte[] bytes = is.readAllBytes();
                String contentType = "text/html; charset=UTF-8";
                if (path.endsWith(".css")) {
                    contentType = "text/css; charset=UTF-8";
                } else if (path.endsWith(".js")) {
                    contentType = "application/javascript; charset=UTF-8";
                    if (path.endsWith("sw.js")) {
                        exchange.getResponseHeaders().set("Service-Worker-Allowed", "/");
                        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                    }
                } else if (path.endsWith(".webmanifest") || path.endsWith("manifest.json")) {
                    contentType = "application/manifest+json; charset=UTF-8";
                } else if (path.endsWith(".json")) {
                    contentType = "application/json; charset=UTF-8";
                } else if (path.endsWith(".png")) {
                    contentType = "image/png";
                } else if (path.endsWith(".svg")) {
                    contentType = "image/svg+xml; charset=UTF-8";
                } else if (path.endsWith(".ico")) {
                    contentType = "image/x-icon";
                }

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    // =============================================================
    // Helper Methods
    // =============================================================
    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private static String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Also support numbers without quotes
        Pattern numPattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9.]+|true|false)");
        Matcher numMatcher = numPattern.matcher(json);
        if (numMatcher.find()) {
            return numMatcher.group(1);
        }
        return null;
    }

    private static List<OrderItem> parseOrderItems(String json) {
        List<OrderItem> items = new ArrayList<>();
        Pattern itemPattern = Pattern.compile("\\{\\s*\"serviceId\"\\s*:\\s*(\\d+)[^}]*?\"quantity\"\\s*:\\s*([0-9.]+)[^}]*?\\}");
        Matcher matcher = itemPattern.matcher(json);
        while (matcher.find()) {
            int serviceId = Integer.parseInt(matcher.group(1));
            BigDecimal qty = new BigDecimal(matcher.group(2));
            Service s = serviceDAO.findById(serviceId);
            if (s != null) {
                OrderItem item = new OrderItem();
                item.setServiceId(serviceId);
                item.setServiceName(s.getServiceName());
                item.setItemDescription(s.getServiceName() + " (" + qty + " " + s.getPricingUnit() + ")");
                item.setQuantity(qty);
                item.setUnitPrice(s.getPrice());
                item.calculateSubtotal();
                items.add(item);
            }
        }
        return items;
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }
}
