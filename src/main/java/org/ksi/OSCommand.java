package org.ksi;

import java.util.Optional;

public interface OSCommand {
    boolean supports(String osName);
    Optional<String> findProcessId(String socketIdentifier);
    Optional<String> findProcessName(String processId);
}
