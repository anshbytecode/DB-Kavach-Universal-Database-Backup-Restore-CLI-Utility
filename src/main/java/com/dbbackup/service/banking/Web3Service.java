package com.dbbackup.service.banking;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Web3Service {

    private final SecureRandom random = new SecureRandom();
    private final List<Map<String, Object>> ledgerStore = Collections.synchronizedList(new ArrayList<>());
    private final Map<Long, Map<String, Object>> walletStore = new ConcurrentHashMap<>();

    public Web3Service() {
        seedSampleLedger();
    }

    private void seedSampleLedger() {
        Map<String, Object> tx1 = new HashMap<>();
        tx1.put("txHash", "0x9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e");
        tx1.put("blockNumber", 48912304L);
        tx1.put("fromAddress", "0x71C7656EC7ab88b098defB751B7401B5f6d8976F");
        tx1.put("toAddress", "0x38A996b758e38d94812F981775e53372c21950B6");
        tx1.put("tokenSymbol", "ETH");
        tx1.put("amount", new BigDecimal("1.50"));
        tx1.put("gasUsed", "0.0021 ETH");
        tx1.put("status", "CONFIRMED");
        tx1.put("timestamp", LocalDateTime.now().minusHours(4).toString());
        ledgerStore.add(tx1);

        Map<String, Object> tx2 = new HashMap<>();
        tx2.put("txHash", "0x1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b");
        tx2.put("blockNumber", 48911850L);
        tx2.put("fromAddress", "0x71C7656EC7ab88b098defB751B7401B5f6d8976F");
        tx2.put("toAddress", "0x98A112c448e22d94812F981775e53372c21950C1");
        tx2.put("tokenSymbol", "USDT");
        tx2.put("amount", new BigDecimal("2500.00"));
        tx2.put("gasUsed", "0.0018 ETH");
        tx2.put("status", "CONFIRMED");
        tx2.put("timestamp", LocalDateTime.now().minusHours(18).toString());
        ledgerStore.add(tx2);
    }

    public Map<String, Object> getWalletInfo(Long customerId) {
        return walletStore.computeIfAbsent(customerId != null ? customerId : 1L, id -> {
            Map<String, Object> wallet = new HashMap<>();
            wallet.put("walletAddress", "0x71C7656EC7ab88b098defB751B7401B5f6d8976F");
            wallet.put("network", "Polygon Mainnet (Web3)");
            wallet.put("ethBalance", new BigDecimal("4.2500"));
            wallet.put("usdtBalance", new BigDecimal("12450.00"));
            wallet.put("maticBalance", new BigDecimal("1850.75"));
            wallet.put("smartContractAudit", "VERIFIED_SECURE");
            wallet.put("securityScore", 98);
            wallet.put("connected", true);
            return wallet;
        });
    }

    public Map<String, Object> executeCryptoTransfer(Long customerId, String recipientAddress, String tokenSymbol, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid crypto transfer amount");
        }
        if (recipientAddress == null || !recipientAddress.startsWith("0x") || recipientAddress.length() < 10) {
            throw new IllegalArgumentException("Invalid Web3 Ethereum/Polygon 0x wallet address");
        }

        Map<String, Object> wallet = getWalletInfo(customerId);
        String tokenKey = (tokenSymbol.toLowerCase() + "Balance");

        BigDecimal currentBal = (BigDecimal) wallet.getOrDefault(tokenKey, new BigDecimal("100.00"));
        if (currentBal.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient Web3 " + tokenSymbol + " crypto balance");
        }

        BigDecimal newBal = currentBal.subtract(amount);
        wallet.put(tokenKey, newBal);

        String txHash = "0x" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        long blockNumber = 48920000L + random.nextInt(5000);

        Map<String, Object> tx = new HashMap<>();
        tx.put("txHash", txHash);
        tx.put("blockNumber", blockNumber);
        tx.put("fromAddress", wallet.get("walletAddress"));
        tx.put("toAddress", recipientAddress);
        tx.put("tokenSymbol", tokenSymbol.toUpperCase());
        tx.put("amount", amount);
        tx.put("gasUsed", "0.0015 ETH");
        tx.put("status", "CONFIRMED");
        tx.put("timestamp", LocalDateTime.now().toString());

        ledgerStore.add(0, tx);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Web3 Smart Contract transfer of " + amount + " " + tokenSymbol + " confirmed on Blockchain ledger!");
        result.put("txHash", txHash);
        result.put("blockNumber", blockNumber);
        result.put("newBalance", newBal);
        result.put("gasFee", "0.0015 ETH");
        return result;
    }

    public List<Map<String, Object>> getBlockchainLedger() {
        return new ArrayList<>(ledgerStore);
    }
}
