package com.laundry.service;

import com.laundry.dao.CustomerDAO;
import com.laundry.exception.ValidationException;
import com.laundry.model.Customer;
import com.laundry.util.ValidationUtil;
import java.util.List;

/**
 * Business service handling Customer management, validation, and lookup.
 */
public class CustomerService {

    private final CustomerDAO customerDAO;

    public CustomerService() {
        this.customerDAO = new CustomerDAO();
    }

    public CustomerService(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    public Customer addCustomer(Customer customer) {
        validateCustomer(customer);

        // Check duplicate phone
        Customer existing = customerDAO.findByPhone(customer.getPhone());
        if (existing != null) {
            throw new ValidationException("A customer with phone number '" + customer.getPhone() + "' already exists (ID: #" + existing.getCustomerId() + ").");
        }

        return customerDAO.save(customer);
    }

    public void updateCustomer(Customer customer) {
        if (customer.getCustomerId() <= 0) {
            throw new ValidationException("Customer ID is required for updating.");
        }
        validateCustomer(customer);

        // Check if phone is used by another customer
        Customer existing = customerDAO.findByPhone(customer.getPhone());
        if (existing != null && existing.getCustomerId() != customer.getCustomerId()) {
            throw new ValidationException("Phone number '" + customer.getPhone() + "' is already registered to another customer.");
        }

        customerDAO.update(customer);
    }

    public void deleteCustomer(int customerId) {
        if (customerId <= 0) {
            throw new ValidationException("Invalid customer ID.");
        }
        customerDAO.delete(customerId);
    }

    public Customer getCustomerById(int customerId) {
        return customerDAO.findById(customerId);
    }

    public List<Customer> getAllCustomers() {
        return customerDAO.findAll();
    }

    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerDAO.search(keyword.trim());
    }

    private void validateCustomer(Customer customer) {
        if (customer == null) {
            throw new ValidationException("Customer data cannot be null.");
        }
        ValidationUtil.requireNonEmpty(customer.getFullName(), "Customer name");
        ValidationUtil.validatePhone(customer.getPhone());
        ValidationUtil.validateEmail(customer.getEmail());
    }
}
