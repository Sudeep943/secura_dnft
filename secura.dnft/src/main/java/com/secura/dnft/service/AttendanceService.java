package com.secura.dnft.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secura.dnft.dao.AttendanceLogRepository;
import com.secura.dnft.dao.AuditLogRepository;
import com.secura.dnft.dao.EmployeeRepository;
import com.secura.dnft.dao.FaceTemplateRepository;
import com.secura.dnft.entity.AttendanceLogEntity;
import com.secura.dnft.entity.AuditLogEntity;
import com.secura.dnft.entity.EmployeeEntity;
import com.secura.dnft.entity.FaceTemplateEntity;
import com.secura.dnft.request.response.AttendanceLodgeEntryRequest;
import com.secura.dnft.request.response.AttendanceLodgeEntryResponse;
import com.secura.dnft.request.response.AttendanceMarkExitRequest;
import com.secura.dnft.request.response.AttendanceMarkExitResponse;

@Service
public class AttendanceService {

    @Value("${attendance.face.match.threshold:0.65}")
    private double matchThreshold;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    // Lodge Entry
    // -----------------------------------------------------------------------

    public AttendanceLodgeEntryResponse lodgeEntry(AttendanceLodgeEntryRequest request) {
        AttendanceLodgeEntryResponse response = new AttendanceLodgeEntryResponse();
        String deviceId = request.getDeviceId();

        float[] queryEmbedding;
        try {
            queryEmbedding = faceRecognitionService.extractEmbedding(request.getImageBase64());
        } catch (IllegalArgumentException e) {
            response.setMatched(false);
            response.setMessage(e.getMessage());
            saveAuditLog("ENTRY_ATTEMPT", null, deviceId, "Image validation failed: " + e.getMessage(), "FAILED");
            return response;
        }

        MatchResult match = findBestMatch(queryEmbedding);

        if (match == null || match.score < matchThreshold) {
            response.setMatched(false);
            response.setMatchScore(match != null ? round2(match.score) : null);
            response.setMessage("Face not recognized. Please contact admin.");
            saveAuditLog("ENTRY_ATTEMPT", null, deviceId,
                    "No match found, best score=" + (match != null ? match.score : "N/A"), "FAILED");
            return response;
        }

        LocalDateTime now = LocalDateTime.now();
        AttendanceLogEntity log = new AttendanceLogEntity();
        log.setEmployeeId(match.employee.getId());
        log.setEntryTime(now);
        log.setDeviceId(deviceId);
        log.setMatchScoreEntry(round2(match.score));
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        attendanceLogRepository.save(log);

        saveAuditLog("ENTRY_SUCCESS", match.employee.getId(), deviceId,
                "Entry logged for " + match.employee.getEmployeeCode() + ", score=" + round2(match.score), "SUCCESS");

        response.setMatched(true);
        response.setEmployeeId(match.employee.getId());
        response.setEmployeeCode(match.employee.getEmployeeCode());
        response.setEmployeeName(match.employee.getFullName());
        response.setEntryTime(now);
        response.setMatchScore(round2(match.score));
        response.setMessage("Entry logged successfully");
        return response;
    }

    // -----------------------------------------------------------------------
    // Mark Exit
    // -----------------------------------------------------------------------

    public AttendanceMarkExitResponse markExit(AttendanceMarkExitRequest request) {
        AttendanceMarkExitResponse response = new AttendanceMarkExitResponse();
        String deviceId = request.getDeviceId();

        float[] queryEmbedding;
        try {
            queryEmbedding = faceRecognitionService.extractEmbedding(request.getImageBase64());
        } catch (IllegalArgumentException e) {
            response.setMatched(false);
            response.setMessage(e.getMessage());
            saveAuditLog("EXIT_ATTEMPT", null, deviceId, "Image validation failed: " + e.getMessage(), "FAILED");
            return response;
        }

        MatchResult match = findBestMatch(queryEmbedding);

        if (match == null || match.score < matchThreshold) {
            response.setMatched(false);
            response.setMatchScore(match != null ? round2(match.score) : null);
            response.setMessage("Face not recognized. Please contact admin.");
            saveAuditLog("EXIT_ATTEMPT", null, deviceId,
                    "No match found, best score=" + (match != null ? match.score : "N/A"), "FAILED");
            return response;
        }

        Optional<AttendanceLogEntity> openRecord =
                attendanceLogRepository.findFirstByEmployeeIdAndExitTimeIsNullOrderByEntryTimeDesc(
                        match.employee.getId());

        if (openRecord.isEmpty()) {
            response.setMatched(true);
            response.setEmployeeId(match.employee.getId());
            response.setEmployeeCode(match.employee.getEmployeeCode());
            response.setEmployeeName(match.employee.getFullName());
            response.setMatchScore(round2(match.score));
            response.setMessage("No entry recorded today. Cannot mark exit.");
            saveAuditLog("EXIT_ATTEMPT", match.employee.getId(), deviceId,
                    "No open entry for " + match.employee.getEmployeeCode(), "FAILED");
            return response;
        }

        LocalDateTime now = LocalDateTime.now();
        AttendanceLogEntity log = openRecord.get();
        log.setExitTime(now);
        log.setMatchScoreExit(round2(match.score));
        log.setUpdatedAt(now);
        attendanceLogRepository.save(log);

        saveAuditLog("EXIT_SUCCESS", match.employee.getId(), deviceId,
                "Exit marked for " + match.employee.getEmployeeCode() + ", score=" + round2(match.score), "SUCCESS");

        response.setMatched(true);
        response.setEmployeeId(match.employee.getId());
        response.setEmployeeCode(match.employee.getEmployeeCode());
        response.setEmployeeName(match.employee.getFullName());
        response.setExitTime(now);
        response.setMatchScore(round2(match.score));
        response.setMessage("Exit marked successfully");
        return response;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Compares queryEmbedding against all active employee face templates.
     * Loads all templates for active employees in two queries (batch) to avoid N+1.
     * Returns the best-matching employee with their highest similarity score,
     * or null if there are no templates in the database.
     */
    private MatchResult findBestMatch(float[] queryEmbedding) {
        List<EmployeeEntity> activeEmployees = employeeRepository.findByStatus("ACTIVE");
        if (activeEmployees.isEmpty()) {
            return null;
        }

        // Collect all active employee IDs and load all their templates in one query
        List<Long> employeeIds = activeEmployees.stream()
                .map(EmployeeEntity::getId)
                .toList();
        List<FaceTemplateEntity> allTemplates = faceTemplateRepository.findByEmployeeIdIn(employeeIds);

        // Build a map for O(1) employee lookup
        Map<Long, EmployeeEntity> employeeMap = new HashMap<>();
        for (EmployeeEntity emp : activeEmployees) {
            employeeMap.put(emp.getId(), emp);
        }

        MatchResult best = null;
        for (FaceTemplateEntity template : allTemplates) {
            float[] stored;
            try {
                stored = faceRecognitionService.parseEmbedding(template.getEmbeddingJson());
            } catch (Exception e) {
                continue;
            }
            double score = faceRecognitionService.cosineSimilarity(queryEmbedding, stored);
            EmployeeEntity employee = employeeMap.get(template.getEmployeeId());
            if (employee != null && (best == null || score > best.score)) {
                best = new MatchResult(employee, score);
            }
        }
        return best;
    }

    private void saveAuditLog(String eventType, Long employeeId, String deviceId, String detail, String status) {
        try {
            Map<String, String> details = new HashMap<>();
            details.put("deviceId", deviceId != null ? deviceId : "");
            details.put("detail", detail != null ? detail : "");
            AuditLogEntity audit = new AuditLogEntity();
            audit.setEventType(eventType);
            audit.setEmployeeId(employeeId);
            audit.setDetailsJson(OBJECT_MAPPER.writeValueAsString(details));
            audit.setStatus(status);
            audit.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(audit);
        } catch (Exception e) {
            // Audit log failure must not disrupt the main flow
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static class MatchResult {
        final EmployeeEntity employee;
        final double score;

        MatchResult(EmployeeEntity employee, double score) {
            this.employee = employee;
            this.score = score;
        }
    }
}
