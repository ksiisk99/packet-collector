package org.ksi;

import java.util.List;

public interface PacketHandlerAdapter {
    List<NetworkInterfaceInfo> getNetworkInterfaces();
    void capture(NetworkTraffic networkTraffic, int selectIndex, OSCommand osCommand);
    void close();
}
