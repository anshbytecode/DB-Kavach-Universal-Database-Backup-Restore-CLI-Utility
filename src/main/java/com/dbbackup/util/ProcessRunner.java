package com.dbbackup.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessRunner {
    private static final Logger log = LoggerFactory.getLogger(ProcessRunner.class);

    public static class ProcessResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        public ProcessResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int getExitCode() { return exitCode; }
        public String getStdout() { return stdout; }
        public String getStderr() { return stderr; }
        public boolean isSuccess() { return exitCode == 0; }
    }

    public static boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(isWindows() ? new String[]{"cmd.exe", "/c", "where", command} : new String[]{"which", command});
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static ProcessResult execute(List<String> command, File outputRedirectFile, File inputRedirectFile, long timeoutMinutes) throws IOException, InterruptedException {
        log.info("Executing command: {}", String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);

        if (outputRedirectFile != null) {
            pb.redirectOutput(outputRedirectFile);
        }
        if (inputRedirectFile != null) {
            pb.redirectInput(inputRedirectFile);
        }

        Process process = pb.start();

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Thread stdoutThread = null;
        if (outputRedirectFile == null) {
            stdoutThread = new Thread(() -> readStream(process.getInputStream(), stdout));
            stdoutThread.start();
        }

        Thread stderrThread = new Thread(() -> readStream(process.getErrorStream(), stderr));
        stderrThread.start();

        boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Process timed out after " + timeoutMinutes + " minutes");
        }

        if (stdoutThread != null) stdoutThread.join();
        stderrThread.join();

        return new ProcessResult(process.exitValue(), stdout.toString(), stderr.toString());
    }

    private static void readStream(InputStream is, StringBuilder sb) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException ignored) {}
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
