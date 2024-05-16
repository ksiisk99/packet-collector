package org.ksi;

import java.util.HashMap;
import java.util.Map;

public final class NetworkTraffic {
    private final Map<String, Long> processResponseByte = new HashMap<>();
    private final Map<String, Long> processRequestByte = new HashMap<>();

    public void addResponseByte(String processName, long receiveByte) {
        processResponseByte.merge(processName, receiveByte, Long::sum);
    }

    public void addRequestByte(String processName, long requestByte) {
        processRequestByte.merge(processName, requestByte, Long::sum);
    }

    public Map<String, Long> getProcessResponseByte() {
        return processResponseByte;
    }

    public Map<String, Long> getProcessRequestByte() {
        return processRequestByte;
    }
}
