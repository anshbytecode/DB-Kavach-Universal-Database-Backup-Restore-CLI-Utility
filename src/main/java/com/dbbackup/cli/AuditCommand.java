package com.dbbackup.cli;

import com.dbbackup.service.banking.BankingAuditService;
import picocli.CommandLine.Command;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
@Command(
    name = "audit",
    description = "View banking audit logs and administrative security trails"
)
public class AuditCommand implements Callable<Integer> {

    private final BankingAuditService auditService;

    public AuditCommand(BankingAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("=========================================================================");
        System.out.println(" 📜 DB-KAVACH BANKING AUDIT LOG TRAIL                                   ");
        System.out.println("=========================================================================");
        auditService.getRecentLogs().forEach(log -> {
            System.out.printf("[%s] %-12s | User: %-10s | Action: %-22s | Target: %s (%s)\n",
                    log.getTimestamp(), log.getRole(), log.getUsername(), log.getAction(), log.getTargetResource(), log.getStatus());
        });
        System.out.println("=========================================================================");
        return 0;
    }
}
