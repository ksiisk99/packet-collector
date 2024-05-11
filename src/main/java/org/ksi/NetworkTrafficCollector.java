package org.ksi;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class NetworkTrafficCollector {
    private static final int SCHEDULE_INITIAL_DELAY = 0;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final NetworkTraffic networkTraffic = new NetworkTraffic();
    private final PacketHandlerAdapter packetHandler;
    private final List<OSCommand> osCommands = List.of(new WindowCommand(), new DefaultCommand());

    public NetworkTrafficCollector(PacketHandlerAdapter packetHandler) {
        this.packetHandler = packetHandler;
    }

    public void collect(NetworkListener listener, int networkInterfaceSelectIndex, int second) {
        OSCommand osCommand = getOSCommand();

        packetHandler.capture(networkTraffic, networkInterfaceSelectIndex, osCommand);

        scheduler.scheduleAtFixedRate(() -> listener.getTraffic(networkTraffic), SCHEDULE_INITIAL_DELAY, second, TimeUnit.SECONDS);
    }

    private OSCommand getOSCommand() {
        for (OSCommand osCommand : osCommands) {
            if(osCommand.supports(System.getProperty("os.name"))) {
                return osCommand;
            }
        }

        return null;
    }

    public List<NetworkInterfaceInfo> getNetworkInterfaces() {
        return packetHandler.getNetworkInterfaces();
    }

    public void shutdown() {
        packetHandler.close();
        scheduler.shutdown();
    }

}