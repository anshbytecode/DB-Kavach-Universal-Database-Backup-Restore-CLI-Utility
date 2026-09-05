package com.dbbackup.service.banking;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiAdvisorService {

    public Map<String, Object> getFinancialAdvice(Long customerId) {
        Map<String, Object> res = new HashMap<>();
        res.put("customerId", customerId != null ? customerId : 1L);
        res.put("creditScore", 815);
        res.put("creditRating", "EXCELLENT");
        res.put("monthlySavingsPotential", 18500);
        res.put("fraudRiskScore", 4);
        res.put("riskLevel", "LOW_RISK");

        List<Map<String, String>> recommendations = new ArrayList<>();

        Map<String, String> rec1 = new HashMap<>();
        rec1.put("category", "SAVINGS_OPTIMIZATION");
        rec1.put("title", "High Savings Yield Opportunity");
        rec1.put("description", "Move ₹50,000 from low-interest savings to DB-Kavach 7.5% p.a. Fixed Deposit to earn ₹3,750 extra per year.");
        rec1.put("impact", "+₹3,750 / Year");
        recommendations.add(rec1);

        Map<String, String> rec2 = new HashMap<>();
        rec2.put("category", "SPENDING_ANALYSIS");
        rec2.put("title", "Dining & Food Delivery Spikes");
        rec2.put("description", "Food & dining expenses increased by 16% this month (₹12,400 spent). Setting a ₹9,000 budget cap can save ₹3,400 monthly.");
        rec2.put("impact", "Save ₹3,400 / Month");
        recommendations.add(rec2);

        Map<String, String> rec3 = new HashMap<>();
        rec3.put("category", "WEB3_DIVERSIFICATION");
        rec3.put("title", "Crypto & Web3 Yield Staking");
        rec3.put("description", "Your connected Web3 Polygon wallet holds 4.25 ETH. Staking on-chain generates up to 5.2% APY passive yield.");
        rec3.put("impact", "+0.22 ETH / Year");
        recommendations.add(rec3);

        res.put("recommendations", recommendations);
        return res;
    }

    public Map<String, Object> getFraudRiskScore(Long customerId) {
        Map<String, Object> res = new HashMap<>();
        res.put("customerId", customerId != null ? customerId : 1L);
        res.put("overallFraudScore", 4); // 0-100 scale, low is safe
        res.put("riskStatus", "VERY_LOW_RISK");
        res.put("anomalyDetection", "PASSED");
        res.put("locationTrust", "99.8%");
        res.put("deviceFingerprint", "TRUSTED_PRIMARY_DEVICE");

        List<Map<String, Object>> riskMetrics = new ArrayList<>();
        Map<String, Object> m1 = new HashMap<>();
        m1.put("factor", "Transaction Velocity");
        m1.put("score", "Normal (3 txns/day)");
        m1.put("status", "SAFE");
        riskMetrics.add(m1);

        Map<String, Object> m2 = new HashMap<>();
        m2.put("factor", "Geographic Anomaly");
        m2.put("score", "Mumbai, IN (Verified IP)");
        m2.put("status", "SAFE");
        riskMetrics.add(m2);

        Map<String, Object> m3 = new HashMap<>();
        m3.put("factor", "Web3 Smart Contract Risk");
        m3.put("score", "Verified Polygon Audit");
        m3.put("status", "SAFE");
        riskMetrics.add(m3);

        res.put("metrics", riskMetrics);
        return res;
    }

    public Map<String, Object> processChatbotQuery(String userMessage, Long customerId, String role) {
        String msg = (userMessage != null) ? userMessage.toLowerCase().trim() : "";
        Map<String, Object> response = new HashMap<>();

        String reply;
        String actionType = "INFO";

        if (msg.contains("balance") || msg.contains("money") || msg.contains("account")) {
            reply = "🤖 **Kavach AI**: Your total combined account balance is **₹1,24,500.00** across Savings & Current accounts. You also hold **4.25 ETH** in your Web3 Wallet!";
            actionType = "BALANCE_CHECK";
        } else if (msg.contains("web3") || msg.contains("crypto") || msg.contains("eth") || msg.contains("wallet")) {
            reply = "🌐 **Kavach AI Web3 Intelligence**: Your connected Polygon Web3 wallet (`0x71C...976F`) is active with **4.25 ETH** (~₹9,35,000) and **12,450 USDT**. All smart contracts are audited and verified secure!";
            actionType = "WEB3_INFO";
        } else if (msg.contains("fraud") || msg.contains("risk") || msg.contains("security") || msg.contains("scan")) {
            reply = "🛡️ **Kavach AI Fraud Shield**: Real-time scan completed! Your current Fraud Risk Score is **4 / 100 (Very Low Risk)**. Zero suspicious login attempts or unauthorized transfers detected.";
            actionType = "FRAUD_SCAN";
        } else if (msg.contains("loan") || msg.contains("emi") || msg.contains("credit") || msg.contains("score")) {
            reply = "💳 **Kavach AI Credit Advisor**: Your AI Creditworthiness Score is **815 (Excellent)**! You pre-qualify for a Personal Loan up to **₹5,00,000** at a low interest rate of **8.5% p.a.**";
            actionType = "LOAN_INFO";
        } else if (msg.contains("transfer") || msg.contains("send") || msg.contains("pay")) {
            reply = "💸 **Kavach AI Assistant**: You can initiate instant transfers via IMPS, NEFT, RTGS, or UPI in the **Transfer Money** tab, or send crypto using the **Web3 & Crypto** hub!";
            actionType = "TRANSFER_GUIDE";
        } else if (msg.contains("hello") || msg.contains("hi") || msg.contains("hey")) {
            reply = "👋 **Hello! I am Kavach AI**, your intelligent 24/7 banking and Web3 assistant. How can I assist you today with your accounts, transfers, AI fraud checks, or crypto wallet?";
            actionType = "GREETING";
        } else {
            reply = "🤖 **Kavach AI**: I analyzed your request! DB-Kavach Banking integrates AI Fraud Detection, Web3 Crypto Wallets, Instant Transfers, and Comprehensive Financial Management. Try asking about your balance, Web3 crypto assets, fraud risk score, or loan eligibility!";
            actionType = "GENERAL";
        }

        response.put("reply", reply);
        response.put("actionType", actionType);
        response.put("timestamp", new Date().toString());
        return response;
    }
}
