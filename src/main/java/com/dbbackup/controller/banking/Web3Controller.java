package com.dbbackup.controller.banking;

import com.dbbackup.service.banking.Web3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/web3")
public class Web3Controller {

    private final Web3Service web3Service;

    public Web3Controller(Web3Service web3Service) {
        this.web3Service = web3Service;
    }

    @GetMapping("/wallet")
    public ResponseEntity<?> getWallet(@RequestParam(required = false, defaultValue = "1") Long customerId) {
        return ResponseEntity.ok(web3Service.getWalletInfo(customerId));
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transferCrypto(@RequestBody Map<String, Object> request) {
        try {
            Long customerId = request.get("customerId") != null ? Long.valueOf(request.get("customerId").toString()) : 1L;
            String recipientAddress = (String) request.get("recipientAddress");
            String tokenSymbol = (String) request.get("tokenSymbol");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            Map<String, Object> result = web3Service.executeCryptoTransfer(customerId, recipientAddress, tokenSymbol, amount);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/ledger")
    public ResponseEntity<?> getLedger() {
        return ResponseEntity.ok(web3Service.getBlockchainLedger());
    }
}
