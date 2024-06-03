package org.ksi;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class NetworkTrafficCollector {
    private static final int SCHEDULE_INITIAL_DELAY = 0;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final NetworkTraffic networkTraffic = new NetworkTraffic();
    private final PacketHandlerAdapter packetHandler;
    private final List<OSCommand> osCommands = List.of(new WindowCommand(), new DefaultCommand());
    private Semaphore collectLock = new Semaphore(1);
    private Thread collectThread;

    public NetworkTrafficCollector(PacketHandlerAdapter packetHandler) {
        this.packetHandler = packetHandler;
    }

    public void collect(NetworkListener listener, int networkInterfaceSelectIndex, int second) {
        if (!collectLock.tryAcquire()) {
            throw new IllegalStateException("다른 스레드가 사용 중입니다...");
        }

        collectThread = Thread.currentThread();

        OSCommand osCommand = getOSCommand();

        packetHandler.capture(networkTraffic, networkInterfaceSelectIndex, osCommand);

        scheduler.scheduleAtFixedRate(() -> listener.getTraffic(networkTraffic), SCHEDULE_INITIAL_DELAY, second, TimeUnit.SECONDS);
    }

    private OSCommand getOSCommand() {
        for (OSCommand osCommand : osCommands) {
            if (osCommand.supports(System.getProperty("os.name"))) {
                return osCommand;
            }
        }

        return null;
    }

    public List<NetworkInterfaceInfo> getNetworkInterfaces() {
        return packetHandler.getNetworkInterfaces();
    }

    public void shutdown() {
        if (collectThread != Thread.currentThread()) {
            throw new IllegalStateException("collect() 를 호출한 스레드만 shutdown() 를 호출할 수 있습니다.");
        }

        collectThread = null;

        packetHandler.close();
        scheduler.shutdown();

        collectLock.release();
    }

}