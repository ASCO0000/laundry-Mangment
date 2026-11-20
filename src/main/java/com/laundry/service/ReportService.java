package com.laundry.service;

import com.laundry.dao.ReportDAO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service providing aggregated reporting data and analytics.
 */
public class ReportService {

    private final ReportDAO reportDAO;

    public ReportService() {
        this.reportDAO = new ReportDAO();
    }

    public ReportService(ReportDAO reportDAO) {
        this.reportDAO = reportDAO;
    }

    public Map<String, Object> getDashboardMetrics() {
        return reportDAO.getDashboardMetrics();
    }

    public Map<String, BigDecimal> getMonthlyRevenueTrend() {
        return reportDAO.getMonthlyRevenueTrend();
    }

    public Map<String, Integer> getOrderStatusCounts() {
        return reportDAO.getOrderStatusCounts();
    }

    public List<Map<String, Object>> getPopularServices() {
        return reportDAO.getPopularServices();
    }

    public List<Map<String, Object>> getPaymentCollectionReport(LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().minusDays(30);
        if (end == null) end = LocalDate.now();
        return reportDAO.getPaymentCollectionReport(start, end);
    }
}
