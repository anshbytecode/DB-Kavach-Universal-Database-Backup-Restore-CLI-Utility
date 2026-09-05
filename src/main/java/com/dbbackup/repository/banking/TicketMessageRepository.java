package com.dbbackup.repository.banking;

import com.dbbackup.model.banking.TicketMessage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
    List<TicketMessage> findBySupportTicketIdOrderByTimestampAsc(Long ticketId);
}
