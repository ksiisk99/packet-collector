package org.ksi;

public class DefaultCommand implements OSCommand {
    @Override
    public boolean supports(String osName) {
        return true;
    }

    @Override
    public String findProcessName(String socketIdentifier) {
        return socketIdentifier;
    }
}
