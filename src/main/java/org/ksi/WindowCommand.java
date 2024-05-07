package org.ksi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

public class WindowCommand implements OSCommand {

    @Override
    public boolean supports(String osName) {
        return osName.contains("Windows");
    }

    @Override
    public Optional<String> findProcessId(String socketIdentifier) {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "netstat -ano | findstr " + socketIdentifier);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(builder.start().getInputStream()))) {
            String line = reader.readLine();

            if (line == null) {
                return Optional.of(socketIdentifier);
            }

            return Optional.of(extractProcessId(line));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> findProcessName(String processId) {
        ProcessBuilder builder = new ProcessBuilder("tasklist", "/FI", "PID eq " + processId, "/FO", "CSV", "/NH");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(builder.start().getInputStream()))) {
            String line = reader.readLine();

            if (line == null) {
                return Optional.of(processId);
            }

            return Optional.of(extractProcessName(line));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.of(processId);
    }

    private String extractProcessName(String line) {
        String[] tokens = line.split(",");

        return tokens[0].replaceAll("^\"|\"$", "");
    }

    private String extractProcessId(String line) {
        String[] tokens = line.trim().split("\\s+");
        String pid = tokens[tokens.length - 1];

        return pid;
    }
}
