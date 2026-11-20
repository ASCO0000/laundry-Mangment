package com.laundry.service;

import com.laundry.dao.ServiceDAO;
import com.laundry.exception.ValidationException;
import com.laundry.model.Service;
import com.laundry.util.ValidationUtil;
import java.util.List;

/**
 * Service managing laundry catalog offerings, units, and rates.
 */
public class LaundryService {

    private final ServiceDAO serviceDAO;

    public LaundryService() {
        this.serviceDAO = new ServiceDAO();
    }

    public LaundryService(ServiceDAO serviceDAO) {
        this.serviceDAO = serviceDAO;
    }

    public Service addService(Service service) {
        validateService(service);
        return serviceDAO.save(service);
    }

    public void updateService(Service service) {
        if (service.getServiceId() <= 0) {
            throw new ValidationException("Service ID is required for editing.");
        }
        validateService(service);
        serviceDAO.update(service);
    }

    public void toggleServiceStatus(int serviceId, boolean active) {
        Service s = serviceDAO.findById(serviceId);
        if (s == null) {
            throw new ValidationException("Service not found with ID: " + serviceId);
        }
        s.setActive(active);
        serviceDAO.update(s);
    }

    public Service getServiceById(int serviceId) {
        return serviceDAO.findById(serviceId);
    }

    public List<Service> getActiveServices() {
        return serviceDAO.findAll(true);
    }

    public List<Service> getAllServices() {
        return serviceDAO.findAll(false);
    }

    public List<Service> searchServices(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllServices();
        }
        return serviceDAO.search(keyword.trim());
    }

    public void deleteService(int serviceId) {
        if (serviceId <= 0) {
            throw new ValidationException("Invalid service ID.");
        }
        serviceDAO.delete(serviceId);
    }

    public java.util.Map<String, Object> getServiceSummary() {
        return serviceDAO.getServiceSummary();
    }

    private void validateService(Service service) {
        if (service == null) {
            throw new ValidationException("Service details cannot be null.");
        }
        ValidationUtil.requireNonEmpty(service.getServiceName(), "Service name");
        ValidationUtil.requireNonEmpty(service.getPricingUnit(), "Pricing unit");
        ValidationUtil.validatePositivePrice(service.getPrice(), "Service price");
    }
}
