package org.ksi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class WindowCommand implements OSCommand {

    @Override
    public boolean supports(String osName) {
        return osName.contains("Windows");
    }

    private String findProcessId(String socketIdentifier) {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "netstat -ano | findstr " + socketIdentifier);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(builder.start().getInputStream()))) {
            String line = reader.readLine();

            if (line == null) {
                return socketIdentifier;
            }

            return extractProcessId(line);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return socketIdentifier;
    }

    @Override
    public String findProcessName(String socketIdentifier) {
        String processId = findProcessId(socketIdentifier);

        ProcessBuilder builder = new ProcessBuilder("tasklist", "/FI", "PID eq " + processId, "/FO", "CSV", "/NH");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(builder.start().getInputStream()))) {
            String line = reader.readLine();

            if (line == null) {
                return socketIdentifier;
            }

            return extractProcessName(line);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return socketIdentifier;
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
