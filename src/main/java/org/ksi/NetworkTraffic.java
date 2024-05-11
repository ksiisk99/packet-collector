package org.ksi;

import java.util.HashMap;
import java.util.Map;

public final class NetworkTraffic {
    private final Map<String, Long> processResponseByte = new HashMap<>();

    public void addResponseByte(String processName, long receiveByte) {
        processResponseByte.merge(processName, receiveByte, Long::sum);
    }

}
