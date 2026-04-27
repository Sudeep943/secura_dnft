package com.secura.dnft.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.secura.dnft.dao.AttendanceLogRepository;
import com.secura.dnft.dao.AuditLogRepository;
import com.secura.dnft.dao.EmployeeRepository;
import com.secura.dnft.dao.FaceTemplateRepository;
import com.secura.dnft.entity.AuditLogEntity;
import com.secura.dnft.entity.AttendanceLogEntity;
import com.secura.dnft.entity.EmployeeEntity;
import com.secura.dnft.entity.FaceTemplateEntity;
import com.secura.dnft.request.response.AttendanceRecordItem;
import com.secura.dnft.request.response.EmployeeAttendanceResponse;
import com.secura.dnft.request.response.OnboardEmployeeRequest;
import com.secura.dnft.request.response.OnboardEmployeeResponse;
import com.secura.dnft.request.response.TodayAttendanceResponse;

@Service
public class AdminAttendanceService {

    private static final int MIN_FACE_IMAGES = 3;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private FaceTemplateRepository faceTemplateRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    // -----------------------------------------------------------------------
    // Onboard Employee
    // -----------------------------------------------------------------------

    public OnboardEmployeeResponse onboardEmployee(OnboardEmployeeRequest request) {
        OnboardEmployeeResponse response = new OnboardEmployeeResponse();

        if (request.getImagesBase64() == null || request.getImagesBase64().size() < MIN_FACE_IMAGES) {
            response.setSuccess(false);
            response.setMessage("At least " + MIN_FACE_IMAGES + " face images required");
            return response;
        }

        if (request.getEmployeeCode() == null || request.getEmployeeCode().isBlank()) {
            response.setSuccess(false);
            response.setMessage("Employee code is required");
            return response;
        }

        if (request.getFullName() == null || request.getFullName().isBlank()) {
            response.setSuccess(false);
            response.setMessage("Full name is required");
            return response;
        }

        // Check for duplicate employee code
        if (employeeRepository.findByEmployeeCode(request.getEmployeeCode()).isPresent()) {
            response.setSuccess(false);
            response.setMessage("Employee with code " + request.getEmployeeCode() + " already exists");
            return response;
        }

        try {
            // Extract and validate all embeddings first so we fail fast
            List<float[]> embeddings = new ArrayList<>();
            for (String imageBase64 : request.getImagesBase64()) {
                embeddings.add(faceRecognitionService.extractEmbedding(imageBase64));
            }

            // Persist employee
            LocalDateTime now = LocalDateTime.now();
            EmployeeEntity employee = new EmployeeEntity();
            employee.setEmployeeCode(request.getEmployeeCode());
            employee.setFullName(request.getFullName());
            employee.setDepartment(request.getDepartment());
            employee.setPhone(request.getPhone());
            employee.setEmail(request.getEmail());
            employee.setStatus("ACTIVE");
            employee.setCreatedAt(now);
            employee = employeeRepository.save(employee);

            // Persist face templates
            for (float[] embedding : embeddings) {
                FaceTemplateEntity template = new FaceTemplateEntity();
                template.setEmployeeId(employee.getId());
                template.setEmbeddingJson(faceRecognitionService.embeddingToJson(embedding));
                template.setCreatedAt(now);
                faceTemplateRepository.save(template);
            }

            saveAuditLog("EMPLOYEE_ONBOARDED", employee.getId(),
                    "Employee " + employee.getEmployeeCode() + " onboarded with " + embeddings.size() + " templates",
                    "SUCCESS");

            response.setSuccess(true);
            response.setEmployeeId(employee.getId());
            response.setEmployeeCode(employee.getEmployeeCode());
            response.setMessage("Employee onboarded successfully");

        } catch (IllegalArgumentException e) {
            response.setSuccess(false);
            response.setMessage("Invalid image: " + e.getMessage());
            saveAuditLog("EMPLOYEE_ONBOARD_FAILED", null,
                    "Onboard failed for " + request.getEmployeeCode() + ": " + e.getMessage(), "FAILED");
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("An unexpected error occurred. Please try again.");
            saveAuditLog("EMPLOYEE_ONBOARD_FAILED", null,
                    "Onboard failed for " + request.getEmployeeCode() + ": " + e.getMessage(), "FAILED");
        }

        return response;
    }

    // -----------------------------------------------------------------------
    // Today's Attendance
    // -----------------------------------------------------------------------

    public TodayAttendanceResponse getTodayAttendance() {
        TodayAttendanceResponse response = new TodayAttendanceResponse();
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        try {
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

            List<AttendanceLogEntity> logs =
                    attendanceLogRepository.findByEntryTimeBetween(startOfDay, endOfDay);

            List<AttendanceRecordItem> records = buildRecordItems(logs);

            response.setSuccess(true);
            response.setDate(dateStr);
            response.setTotalPresent(records.size());
            response.setRecords(records);
            response.setMessage("Today's attendance fetched successfully");

        } catch (Exception e) {
            response.setSuccess(false);
            response.setDate(dateStr);
            response.setMessage("Failed to fetch attendance: " + e.getMessage());
        }

        return response;
    }

    // -----------------------------------------------------------------------
    // Employee Attendance History
    // -----------------------------------------------------------------------

    public EmployeeAttendanceResponse getEmployeeAttendance(String employeeCode) {
        EmployeeAttendanceResponse response = new EmployeeAttendanceResponse();

        Optional<EmployeeEntity> employeeOpt = employeeRepository.findByEmployeeCode(employeeCode);
        if (employeeOpt.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Employee not found: " + employeeCode);
            return response;
        }

        EmployeeEntity employee = employeeOpt.get();

        try {
            // Return last 30 days by default
            LocalDateTime from = LocalDate.now().minusDays(30).atStartOfDay();
            LocalDateTime to = LocalDateTime.now();

            List<AttendanceLogEntity> logs =
                    attendanceLogRepository.findByEmployeeIdAndEntryTimeBetween(employee.getId(), from, to);

            List<AttendanceRecordItem> records = buildRecordItems(logs);

            response.setSuccess(true);
            response.setEmployeeId(employee.getId());
            response.setEmployeeCode(employee.getEmployeeCode());
            response.setEmployeeName(employee.getFullName());
            response.setDepartment(employee.getDepartment());
            response.setRecords(records);
            response.setMessage("Attendance history fetched successfully");

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Failed to fetch attendance: " + e.getMessage());
        }

        return response;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private List<AttendanceRecordItem> buildRecordItems(List<AttendanceLogEntity> logs) {
        List<AttendanceRecordItem> items = new ArrayList<>();
        for (AttendanceLogEntity log : logs) {
            Optional<EmployeeEntity> empOpt = employeeRepository.findById(log.getEmployeeId());
            AttendanceRecordItem item = new AttendanceRecordItem();
            item.setLogId(log.getId());
            item.setEmployeeId(log.getEmployeeId());
            item.setEntryTime(log.getEntryTime());
            item.setExitTime(log.getExitTime());
            item.setDeviceId(log.getDeviceId());
            item.setMatchScoreEntry(log.getMatchScoreEntry());
            item.setMatchScoreExit(log.getMatchScoreExit());
            if (empOpt.isPresent()) {
                item.setEmployeeCode(empOpt.get().getEmployeeCode());
                item.setEmployeeName(empOpt.get().getFullName());
                item.setDepartment(empOpt.get().getDepartment());
            }
            items.add(item);
        }
        return items;
    }

    private void saveAuditLog(String eventType, Long employeeId, String detail, String status) {
        try {
            AuditLogEntity audit = new AuditLogEntity();
            audit.setEventType(eventType);
            audit.setEmployeeId(employeeId);
            audit.setDetailsJson("{\"detail\":\"" + detail.replace("\"", "'") + "\"}");
            audit.setStatus(status);
            audit.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(audit);
        } catch (Exception e) {
            // Audit log failure must not disrupt the main flow
        }
    }
}
