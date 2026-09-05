package com.dbbackup.service.banking;

import com.dbbackup.model.banking.*;
import com.dbbackup.repository.banking.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KYCAndSupportService {

    private final KYCDocumentRepository kycRepository;
    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final CustomerRepository customerRepository;
    private final BankingAuditService auditService;

    public KYCAndSupportService(KYCDocumentRepository kycRepository,
                                SupportTicketRepository ticketRepository,
                                TicketMessageRepository messageRepository,
                                CustomerRepository customerRepository,
                                BankingAuditService auditService) {
        this.kycRepository = kycRepository;
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @Transactional
    public KYCDocument submitKYC(Long customerId, String docType, String docNumber) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        String masked = docNumber != null && docNumber.length() > 4 ?
                "****" + docNumber.substring(docNumber.length() - 4) : "****";

        KYCDocument doc = new KYCDocument(customer, docType.toUpperCase(), masked, "/docs/demo_kyc.pdf");
        doc.setStatus("UNDER_REVIEW");
        doc = kycRepository.save(doc);

        customer.setKycStatus("UNDER_REVIEW");
        customerRepository.save(customer);

        auditService.log(customer.getUser().getUsername(), "CUSTOMER", "KYC_SUBMITTED", docType, "Submitted KYC document", "SUCCESS");
        return doc;
    }

    @Transactional
    public KYCDocument reviewKYC(Long docId, String status, String notes, String reviewerUsername) {
        KYCDocument doc = kycRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("KYC Document not found"));

        doc.setStatus(status.toUpperCase());
        doc.setReviewerNotes(notes);
        doc.setReviewedAt(LocalDateTime.now());
        kycRepository.save(doc);

        Customer customer = doc.getCustomer();
        customer.setKycStatus(status.toUpperCase());
        customerRepository.save(customer);

        auditService.log(reviewerUsername, "STAFF", "KYC_REVIEWED", "DOC_" + docId, "KYC status set to " + status, "SUCCESS");
        return doc;
    }

    public List<KYCDocument> getPendingKYC() {
        return kycRepository.findByStatus("UNDER_REVIEW");
    }

    @Transactional
    public SupportTicket createTicket(Long customerId, String subject, String category, String priority, String initialMessage) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        String ticketNo = "TCK" + (100000 + (int)(Math.random() * 900000));
        SupportTicket ticket = new SupportTicket(ticketNo, customer, subject, category, priority);
        ticket = ticketRepository.save(ticket);

        TicketMessage msg = new TicketMessage(ticket, customer.getUser().getUsername(), "CUSTOMER", initialMessage);
        messageRepository.save(msg);

        auditService.log(customer.getUser().getUsername(), "CUSTOMER", "TICKET_CREATED", ticketNo, "Created support ticket: " + subject, "SUCCESS");
        return ticket;
    }

    @Transactional
    public TicketMessage addTicketReply(Long ticketId, String username, String role, String messageText) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        TicketMessage msg = new TicketMessage(ticket, username, role, messageText);
        msg = messageRepository.save(msg);

        ticket.setUpdatedAt(LocalDateTime.now());
        if ("STAFF".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            ticket.setStatus("IN_PROGRESS");
        }
        ticketRepository.save(ticket);

        return msg;
    }

    public List<SupportTicket> getCustomerTickets(Long customerId) {
        return ticketRepository.findByCustomerId(customerId);
    }

    public List<SupportTicket> getAllTickets() {
        return ticketRepository.findAll();
    }
}
