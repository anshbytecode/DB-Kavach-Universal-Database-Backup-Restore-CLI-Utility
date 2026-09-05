package com.dbbackup.model.banking;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_messages")
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket supportTicket;

    @Column(nullable = false, length = 50)
    private String senderUsername;

    @Column(nullable = false, length = 30)
    private String senderRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private LocalDateTime timestamp = LocalDateTime.now();

    public TicketMessage() {}

    public TicketMessage(SupportTicket supportTicket, String senderUsername, String senderRole, String message) {
        this.supportTicket = supportTicket;
        this.senderUsername = senderUsername;
        this.senderRole = senderRole;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SupportTicket getSupportTicket() { return supportTicket; }
    public void setSupportTicket(SupportTicket supportTicket) { this.supportTicket = supportTicket; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
