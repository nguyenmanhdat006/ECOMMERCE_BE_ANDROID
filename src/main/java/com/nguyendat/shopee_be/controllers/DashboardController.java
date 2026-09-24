package com.nguyendat.shopee_be.controllers;

import com.nguyendat.shopee_be.services.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/kpi")
    public Map<String, Object> getKpi() {
        return dashboardService.getFullKPI();
    }

    @PostMapping("/refresh")
    public String triggerRefresh() {
        dashboardService.pushKpiUpdate();
        dashboardService.pushHourlyRevenue();
        dashboardService.pushOrderStatusDistribution();
        return "Dashboard refreshed";
    }
}