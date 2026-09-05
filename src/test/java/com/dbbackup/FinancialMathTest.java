package com.dbbackup;

import com.dbbackup.service.banking.FinancialMathService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class FinancialMathTest {

    private final FinancialMathService mathService = new FinancialMathService();

    @Test
    public void testMonthlyEmiCalculation() {
        BigDecimal principal = new BigDecimal("100000.00");
        BigDecimal annualInterestRate = new BigDecimal("12.00");
        int tenureMonths = 12;

        BigDecimal emi = mathService.calculateMonthlyEmi(principal, annualInterestRate, tenureMonths);
        assertNotNull(emi);
        // EMI for $100,000 at 12% for 12 months is ~$8,884.88
        assertTrue(emi.compareTo(new BigDecimal("8800.00")) > 0);
        assertTrue(emi.compareTo(new BigDecimal("9000.00")) < 0);
    }

    @Test
    public void testFdMaturityCalculation() {
        BigDecimal principal = new BigDecimal("50000.00");
        BigDecimal annualInterestRate = new BigDecimal("8.00");
        int tenureMonths = 12;

        BigDecimal maturity = mathService.calculateFdMaturity(principal, annualInterestRate, tenureMonths);
        assertNotNull(maturity);
        // Maturity for $50,000 at 8% compounding quarterly for 1 year is $54,121.61
        assertTrue(maturity.compareTo(principal) > 0);
        assertTrue(maturity.compareTo(new BigDecimal("54000.00")) > 0);
    }
}
