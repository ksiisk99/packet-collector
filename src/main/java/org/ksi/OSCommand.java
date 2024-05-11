package org.ksi;

public interface OSCommand {
    boolean supports(String osName);
    String findProcessName(String socketIdentifier);
}
