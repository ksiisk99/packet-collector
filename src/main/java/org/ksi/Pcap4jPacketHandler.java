package org.ksi;

import org.pcap4j.core.*;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;

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
            if (packet.get(IpV4Packet.class) != null) {
                String socket = packetParser.extractSocketIdentifier(packet);
                System.out.println(socket);
            }
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

        private String extractSocketIdentifier(Packet packet) {
            String ip = extractIP(packet);
            String port = extractPort(packet);

            return ip + ":" + port;
        }

        private String extractPort(Packet packet) {
            TcpPacket tcpPacket = packet.get(TcpPacket.class);
            if (tcpPacket != null) {
                return tcpPacket.getHeader().getSrcPort().toString().split("")[0];
            }

            UdpPacket udpPacket = packet.get(UdpPacket.class);
            return udpPacket.getHeader().getSrcPort().toString().split("")[0];
        }

        private String extractIP(Packet packet) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();

            return ipV4Header.getSrcAddr().getHostAddress().toString();
        }

        private void createNetworkInterfaceAddresses(PcapNetworkInterface pcapNetworkInterface) {
            List<PcapAddress> pcapAddresses = pcapNetworkInterface.getAddresses();
            networkInterfaceAddresses = pcapAddresses.stream()
                    .map(pcapAddress -> pcapAddress.getAddress().getHostAddress())
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

}
