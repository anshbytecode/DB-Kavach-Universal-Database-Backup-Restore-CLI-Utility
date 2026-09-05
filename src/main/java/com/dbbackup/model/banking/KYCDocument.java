package com.dbbackup.model.banking;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_documents")
public class KYCDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 50)
    private String documentType; // PASSPORT, DRIVERS_LICENSE, NATIONAL_ID, AADHAAR, PAN_CARD

    @Column(nullable = false)
    private String documentNumberMasked;

    private String documentUrl;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, UNDER_REVIEW, APPROVED, REJECTED, EXPIRED

    private String rejectionReason;
    private String reviewerNotes;

    private LocalDateTime submittedAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;

    public KYCDocument() {}

    public KYCDocument(Customer customer, String documentType, String documentNumberMasked, String documentUrl) {
        this.customer = customer;
        this.documentType = documentType;
        this.documentNumberMasked = documentNumberMasked;
        this.documentUrl = documentUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentNumberMasked() { return documentNumberMasked; }
    public void setDocumentNumberMasked(String documentNumberMasked) { this.documentNumberMasked = documentNumberMasked; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String reviewerNotes) { this.reviewerNotes = reviewerNotes; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
