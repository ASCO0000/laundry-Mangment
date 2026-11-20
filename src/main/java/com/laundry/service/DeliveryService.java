package com.laundry.service;

import com.laundry.dao.DeliveryDAO;
import com.laundry.dao.OrderDAO;
import com.laundry.exception.ValidationException;
import com.laundry.model.Delivery;
import com.laundry.model.Order;
import com.laundry.model.enums.DeliveryStatus;
import com.laundry.util.ValidationUtil;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing pickup and delivery scheduling and fulfillment statuses.
 */
public class DeliveryService {

    private final DeliveryDAO deliveryDAO;
    private final OrderDAO orderDAO;

    public DeliveryService() {
        this.deliveryDAO = new DeliveryDAO();
        this.orderDAO = new OrderDAO();
    }

    public DeliveryService(DeliveryDAO deliveryDAO, OrderDAO orderDAO) {
        this.deliveryDAO = deliveryDAO;
        this.orderDAO = orderDAO;
    }

    public Delivery scheduleDelivery(Delivery delivery) {
        validateDelivery(delivery);
        return deliveryDAO.save(delivery);
    }

    public void updateDelivery(Delivery delivery) {
        if (delivery.getDeliveryId() <= 0) {
            throw new ValidationException("Delivery ID is required for editing.");
        }
        validateDelivery(delivery);
        deliveryDAO.update(delivery);
    }

    public void updateDeliveryStatus(int deliveryId, DeliveryStatus status) {
        if (deliveryId <= 0) {
            throw new ValidationException("Invalid delivery ID.");
        }
        deliveryDAO.updateStatus(deliveryId, status);
    }

    public Delivery getDeliveryById(int deliveryId) {
        return deliveryDAO.findById(deliveryId);
    }

    public List<Delivery> getAllDeliveries(String typeFilter, String statusFilter) {
        return deliveryDAO.findAll(typeFilter, statusFilter);
    }

    private void validateDelivery(Delivery delivery) {
        if (delivery == null) {
            throw new ValidationException("Delivery schedule cannot be null.");
        }
        if (delivery.getOrderId() <= 0) {
            throw new ValidationException("A valid Laundry Order ID is required.");
        }
        Order order = orderDAO.findById(delivery.getOrderId());
        if (order == null) {
            throw new ValidationException("Referenced Order ID #" + delivery.getOrderId() + " does not exist.");
        }
        ValidationUtil.requireNonEmpty(delivery.getAddress(), "Delivery/Pickup address");
        if (delivery.getScheduledDatetime() == null) {
            throw new ValidationException("Scheduled date and time is required.");
        }
    }
}
