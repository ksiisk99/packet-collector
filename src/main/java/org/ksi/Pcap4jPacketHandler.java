package org.ksi;

import org.pcap4j.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Pcap4jPacketHandler implements PacketHandlerAdapter {
    private static final int TIME_OUT_MILLIS = 10;
    private static final int SNAP_LEN = 65536;
    private static final int INFINITE_LOOP = -1;
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final List<PcapNetworkInterface> pcapNetworkInterfaces;
    private final PacketParser packetParser = new PacketParser();
    private PcapHandle pcapHandle;

    public Pcap4jPacketHandler() {
        try {
            pcapNetworkInterfaces = new ArrayList<>(Pcaps.findAllDevs());
        } catch (PcapNativeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<NetworkInterfaceInfo> getNetworkInterfaces() {
        return pcapNetworkInterfaces.stream()
                .map(pcapNif -> new NetworkInterfaceInfo(pcapNif.getName(), pcapNif.getDescription(), pcapNif.getLinkLayerAddresses().toString(), pcapNif.getAddresses().toString()))
                .collect(Collectors.toList());
    }

    @Override
    public void capture(NetworkTraffic networkTraffic, int selectIndex) {
        executorService.execute(() -> {
            PcapNetworkInterface pcapNetworkInterface = pcapNetworkInterfaces.get(selectIndex);

            createPacketHandler(pcapNetworkInterface);

            PacketListener packetListener = createPacketListener(pcapNetworkInterface, networkTraffic);

            doCapture(packetListener);
        });
    }

    private void doCapture(PacketListener packetListener) {
        try {
            pcapHandle.loop(INFINITE_LOOP, packetListener);
        } catch (PcapNativeException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            //throw new RuntimeException(e);
        } catch (NotOpenException e) {
            throw new RuntimeException(e);
        }
    }

    private PacketListener createPacketListener(PcapNetworkInterface pcapNetworkInterface, NetworkTraffic networkTraffic) {
        packetParser.createNetworkInterfaceAddresses(pcapNetworkInterface);

        PacketListener packetListener = packet -> {
        };

        return packetListener;
    }

    private void createPacketHandler(PcapNetworkInterface pcapNetworkInterface) {
        try {
            pcapHandle = pcapNetworkInterface.openLive(SNAP_LEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, TIME_OUT_MILLIS);
        } catch (PcapNativeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            pcapHandle.breakLoop();
        } catch (NotOpenException e) {
            throw new RuntimeException(e);
        }

        pcapHandle.close();

        executorService.shutdown();
    }

    private final class PacketParser {
        private Set<String> networkInterfaceAddresses;

        private void createNetworkInterfaceAddresses(PcapNetworkInterface pcapNetworkInterface) {
            List<PcapAddress> pcapAddresses = pcapNetworkInterface.getAddresses();
            networkInterfaceAddresses = pcapAddresses.stream()
                    .map(pcapAddress -> pcapAddress.getAddress().getHostAddress())
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

}
