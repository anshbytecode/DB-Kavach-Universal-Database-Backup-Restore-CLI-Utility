package com.dbbackup.service.banking;

import com.dbbackup.model.banking.*;
import com.dbbackup.repository.banking.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CardAndBillService {

    private final CardRepository cardRepository;
    private final BillerRepository billerRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final BankAccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final BankingAuditService auditService;

    public CardAndBillService(CardRepository cardRepository,
                             BillerRepository billerRepository,
                             BillPaymentRepository billPaymentRepository,
                             BankAccountRepository accountRepository,
                             CustomerRepository customerRepository,
                             BankingAuditService auditService) {
        this.cardRepository = cardRepository;
        this.billerRepository = billerRepository;
        this.billPaymentRepository = billPaymentRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    public List<Card> getCustomerCards(Long customerId) {
        return cardRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Card issueCard(Long customerId, Long accountId, String cardType, String cardNetwork) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        String maskedNum = (cardNetwork.equalsIgnoreCase("VISA") ? "4532 " : "5241 ") +
                           "**** **** " + (1000 + (int)(Math.random() * 9000));

        LocalDate exp = LocalDate.now().plusYears(5);
        BigDecimal limit = cardType.equalsIgnoreCase("CREDIT") ? new BigDecimal("200000.00") : new BigDecimal("50000.00");

        Card card = new Card(maskedNum, customer, account, cardType.toUpperCase(), cardNetwork.toUpperCase(), exp, limit);
        if ("CREDIT".equalsIgnoreCase(cardType)) {
            card.setCreditLimit(limit);
        }
        card = cardRepository.save(card);

        auditService.log(customer.getUser().getUsername(), "CUSTOMER", "CARD_ISSUED", maskedNum, "Issued " + cardType + " card", "SUCCESS");
        return card;
    }

    @Transactional
    public Card toggleCardFreeze(Long cardId, String username) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        card.setFrozen(!card.isFrozen());
        cardRepository.save(card);

        auditService.log(username, "CUSTOMER", "CARD_FREEZE_TOGGLE", card.getCardNumberMasked(), "Card frozen status: " + card.isFrozen(), "SUCCESS");
        return card;
    }

    public List<Biller> getAllBillers() {
        return billerRepository.findAll();
    }

    @Transactional
    public BillPayment payBill(Long customerId, Long accountId, Long billerId, String consumerNo, BigDecimal amount) {
        BankAccount acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (acc.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance to pay bill");
        }

        Biller biller = billerRepository.findById(billerId)
                .orElseThrow(() -> new IllegalArgumentException("Biller not found"));

        acc.setBalance(acc.getBalance().subtract(amount));
        acc.setAvailableBalance(acc.getAvailableBalance().subtract(amount));
        accountRepository.save(acc);

        BillPayment payment = new BillPayment(acc.getCustomer(), acc, biller, consumerNo, amount);
        payment = billPaymentRepository.save(payment);

        auditService.log(acc.getCustomer().getUser().getUsername(), "CUSTOMER", "BILL_PAYMENT", consumerNo, "Paid " + amount + " to biller " + biller.getName(), "SUCCESS");
        return payment;
    }

    public List<BillPayment> getCustomerBillPayments(Long customerId) {
        return billPaymentRepository.findByCustomerId(customerId);
    }
}
