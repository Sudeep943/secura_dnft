package com.secura.dnft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secura.dnft.request.response.AttendanceLodgeEntryRequest;
import com.secura.dnft.request.response.AttendanceLodgeEntryResponse;
import com.secura.dnft.request.response.AttendanceMarkExitRequest;
import com.secura.dnft.request.response.AttendanceMarkExitResponse;
import com.secura.dnft.request.response.FaceScoreRequest;
import com.secura.dnft.request.response.FaceScoreResponse;
import com.secura.dnft.service.AttendanceService;

/**
 * Public attendance endpoints — NO authentication required.
 * These are intended to be called from a kiosk / Flutter UI.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/attendance")
public class PublicAttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /**
     * POST /api/v1/attendance/lodge-entry
     * Employee stands in front of the camera and clicks "Submit Attendance".
     */
    @PostMapping("/lodge-entry")
    public AttendanceLodgeEntryResponse lodgeEntry(@RequestBody AttendanceLodgeEntryRequest request) {
        return attendanceService.lodgeEntry(request);
    }

    /**
     * POST /api/v1/attendance/mark-exit
     * Employee stands in front of the camera and clicks "Mark Exit".
     */
    @PostMapping("/mark-exit")
    public AttendanceMarkExitResponse markExit(@RequestBody AttendanceMarkExitRequest request) {
        return attendanceService.markExit(request);
    }

    /**
     * POST /api/v1/attendance/face-score
     * Diagnostic endpoint — returns the best face-match score for the submitted
     * image WITHOUT logging any attendance record. Use this during setup and
     * troubleshooting to verify the face recognition service is working and
     * to check whether a photo would pass the match threshold.
     */
    @PostMapping("/face-score")
    public FaceScoreResponse getFaceScore(@RequestBody FaceScoreRequest request) {
        return attendanceService.getFaceScore(request);
    }
}
