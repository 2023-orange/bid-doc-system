package com.example.biddoc.dashboard.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.dashboard.dto.resp.DashboardStatsRespDTO;
import com.example.biddoc.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 查询首页仪表盘统计
     */
    @GetMapping("/stats")
    public ApiResponse<DashboardStatsRespDTO> getStats() {
        return ApiResponse.success(dashboardService.getStats());
    }
}
