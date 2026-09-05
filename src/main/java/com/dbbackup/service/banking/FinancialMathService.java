package com.dbbackup.service.banking;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FinancialMathService {

    public BigDecimal calculateMonthlyEmi(BigDecimal principal, BigDecimal annualInterestRate, int tenureMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return BigDecimal.ZERO;
        }

        if (annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) <= 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 4, RoundingMode.HALF_UP);
        }

        // Monthly interest rate r = annualInterestRate / 12 / 100
        double annualRateDouble = annualInterestRate.doubleValue();
        double monthlyRateDouble = annualRateDouble / 12.0 / 100.0;

        double p = principal.doubleValue();
        double emiDouble = (p * monthlyRateDouble * Math.pow(1 + monthlyRateDouble, tenureMonths)) 
                         / (Math.pow(1 + monthlyRateDouble, tenureMonths) - 1);

        return BigDecimal.valueOf(emiDouble).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateFdMaturity(BigDecimal principal, BigDecimal annualInterestRate, int tenureMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) <= 0 || tenureMonths <= 0) {
            return principal;
        }

        double p = principal.doubleValue();
        double r = annualInterestRate.doubleValue() / 100.0;
        double years = tenureMonths / 12.0;

        // Compound interest calculated quarterly (n=4)
        double maturityDouble = p * Math.pow(1 + (r / 4.0), 4.0 * years);

        return BigDecimal.valueOf(maturityDouble).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateSavingsInterest(BigDecimal balance, BigDecimal annualInterestRate, int days) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0 || days <= 0) {
            return BigDecimal.ZERO;
        }

        double b = balance.doubleValue();
        double r = annualInterestRate.doubleValue() / 100.0;
        double interestDouble = (b * r * days) / 365.0;

        return BigDecimal.valueOf(interestDouble).setScale(2, RoundingMode.HALF_UP);
    }
}
