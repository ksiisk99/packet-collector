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
                return Optional.empty();
            }

            String pid = extractProcessId(line);
            return Optional.of(pid);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private String extractProcessId(String line) {
        String[] tokens = line.trim().split("\\s+");
        String pid = tokens[tokens.length - 1];

        return pid;
    }
}
