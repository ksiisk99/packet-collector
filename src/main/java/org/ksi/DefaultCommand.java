package org.ksi;

import java.util.Optional;

public class DefaultCommand implements OSCommand {
    @Override
    public boolean supports(String osName) {
        return true;
    }

    @Override
    public Optional<String> findProcessId(String socketIdentifier) {
        return Optional.of(socketIdentifier);
    }

    @Override
    public Optional<String> findProcessName(String processId) {
        return Optional.of(processId);
    }
}
