package com.dbbackup.controller.banking;

import com.dbbackup.service.banking.AiAdvisorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAdvisorService aiAdvisorService;

    public AiController(AiAdvisorService aiAdvisorService) {
        this.aiAdvisorService = aiAdvisorService;
    }

    @GetMapping("/financial-advice")
    public ResponseEntity<?> getFinancialAdvice(@RequestParam(required = false, defaultValue = "1") Long customerId) {
        return ResponseEntity.ok(aiAdvisorService.getFinancialAdvice(customerId));
    }

    @GetMapping("/fraud-risk")
    public ResponseEntity<?> getFraudRisk(@RequestParam(required = false, defaultValue = "1") Long customerId) {
        return ResponseEntity.ok(aiAdvisorService.getFraudRiskScore(customerId));
    }

    @PostMapping("/chat")
    public ResponseEntity<?> processChat(@RequestBody Map<String, Object> request) {
        String userMessage = (String) request.get("message");
        Long customerId = request.get("customerId") != null ? Long.valueOf(request.get("customerId").toString()) : 1L;
        String role = request.get("role") != null ? (String) request.get("role") : "CUSTOMER";

        return ResponseEntity.ok(aiAdvisorService.processChatbotQuery(userMessage, customerId, role));
    }
}
