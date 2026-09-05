package com.dbbackup.controller.banking;

import com.dbbackup.model.banking.*;
import com.dbbackup.service.banking.KYCAndSupportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
public class EmployeeApiController {

    private final KYCAndSupportService kycAndSupportService;

    public EmployeeApiController(KYCAndSupportService kycAndSupportService) {
        this.kycAndSupportService = kycAndSupportService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getEmployeeDashboard() {
        Map<String, Object> data = new HashMap<>();
        List<KYCDocument> pendingKyc = kycAndSupportService.getPendingKYC();
        List<SupportTicket> allTickets = kycAndSupportService.getAllTickets();

        data.put("pendingKyc", pendingKyc);
        data.put("pendingKycCount", pendingKyc.size());
        data.put("supportTickets", allTickets);
        data.put("openTicketsCount", allTickets.stream().filter(t -> !"CLOSED".equalsIgnoreCase(t.getStatus())).count());

        return ResponseEntity.ok(data);
    }

    @PostMapping("/tickets/{id}/reply")
    public ResponseEntity<?> replyTicket(@PathVariable Long id, @RequestBody Map<String, String> req) {
        try {
            String username = req.getOrDefault("username", "staff");
            String message = req.get("message");

            TicketMessage msg = kycAndSupportService.addTicketReply(id, username, "STAFF", message);
            return ResponseEntity.ok(msg);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
