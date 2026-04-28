package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.request.response.EmployeeAttendanceResponse;
import com.secura.dnft.request.response.OnboardEmployeeRequest;
import com.secura.dnft.request.response.OnboardEmployeeResponse;
import com.secura.dnft.request.response.ReEnrollEmployeeRequest;
import com.secura.dnft.request.response.ReEnrollEmployeeResponse;
import com.secura.dnft.request.response.TodayAttendanceResponse;
import com.secura.dnft.service.AdminAttendanceService;

/**
 * Admin attendance endpoints — JWT authentication required.
 * The JWT is validated by JwtFilter; no role check is performed beyond token validity.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/employees")
public class AdminAttendanceController {

    @Autowired
    private AdminAttendanceService adminAttendanceService;

    /**
     * POST /api/v1/admin/employees/onboard
     * Onboards a new employee with face photos.
     */
    @PostMapping("/onboard")
    public OnboardEmployeeResponse onboardEmployee(@RequestBody OnboardEmployeeRequest request) {
        return adminAttendanceService.onboardEmployee(request);
    }

    /**
     * GET /api/v1/admin/attendance/today
     * Returns today's attendance records.
     */
    @GetMapping("/attendance/today")
    public TodayAttendanceResponse getTodayAttendance() {
        return adminAttendanceService.getTodayAttendance();
    }

    /**
     * GET /api/v1/admin/attendance/employee/{employeeCode}
     * Returns attendance history for a specific employee (last 30 days).
     */
    @GetMapping("/attendance/{employeeCode}")
    public EmployeeAttendanceResponse getEmployeeAttendance(@PathVariable String employeeCode) {
        return adminAttendanceService.getEmployeeAttendance(employeeCode);
    }

    /**
     * POST /api/v1/admin/employees/{employeeCode}/re-enroll
     * Replaces stored face templates for an existing employee with fresh ones.
     * Attendance history is preserved.
     *
     * Use this after switching from the SHA-256 stub to the real face-recognition
     * service, or when an employee's appearance has changed significantly.
     */
    @PostMapping("/{employeeCode}/re-enroll")
    public ReEnrollEmployeeResponse reEnrollEmployee(
            @PathVariable String employeeCode,
            @RequestBody ReEnrollEmployeeRequest request) {
        return adminAttendanceService.reEnrollEmployee(employeeCode, request);
    }
}
