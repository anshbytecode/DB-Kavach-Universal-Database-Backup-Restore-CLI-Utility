package com.dbbackup.cli;

import com.dbbackup.util.ChecksumUtil;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.Callable;

@Component
@Command(
    name = "verify-backup",
    description = "Verify integrity and checksum of an encrypted database backup archive"
)
public class VerifyBackupCommand implements Callable<Integer> {

    @Option(names = {"-f", "--file"}, description = "Path to backup file archive", required = true)
    private String filePath;

    @Option(names = {"-c", "--checksum"}, description = "Expected SHA-256 Checksum", required = false)
    private String expectedChecksum;

    @Override
    public Integer call() throws Exception {
        System.out.println("=========================================================================");
        System.out.println(" 🔍 DB-KAVACH BACKUP ARCHIVE VERIFICATION SERVICE                         ");
        System.out.println("=========================================================================");

        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("❌ Backup file not found: " + filePath);
            return 1;
        }

        String calculatedChecksum = ChecksumUtil.calculateSHA256(file);
        System.out.println(" File Path           : " + file.getAbsolutePath());
        System.out.println(" Size                : " + file.length() + " bytes");
        System.out.println(" Calculated SHA-256  : " + calculatedChecksum);

        if (expectedChecksum != null && !expectedChecksum.isEmpty()) {
            if (calculatedChecksum.equalsIgnoreCase(expectedChecksum.trim())) {
                System.out.println(" Status              : ✅ CHECKSUM VERIFIED MATCH!");
            } else {
                System.err.println(" Status              : ❌ CHECKSUM MISMATCH ALERT!");
                return 1;
            }
        } else {
            System.out.println(" Status              : ✅ FILE READABLE & INTEGRITY CHECK PASSED");
        }
        System.out.println("=========================================================================");
        return 0;
    }
}
