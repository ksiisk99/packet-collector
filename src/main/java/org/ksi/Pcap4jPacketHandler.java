package org.ksi;

import org.pcap4j.core.*;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;

import java.util.ArrayList;
import java.util.Arrays;
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
    public void capture(NetworkTraffic networkTraffic, int selectIndex, OSCommand osCommand) {
        executorService.execute(() -> {
            PcapNetworkInterface pcapNetworkInterface = pcapNetworkInterfaces.get(selectIndex);

            createPacketHandler(pcapNetworkInterface);

            PacketListener packetListener = createPacketListener(pcapNetworkInterface, networkTraffic, osCommand);

            doCapture(packetListener);
        });
    }

    private void doCapture(PacketListener packetListener) {
        if (!pcapHandle.isOpen()) {
            return;
        }

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

    private PacketListener createPacketListener(PcapNetworkInterface pcapNetworkInterface, NetworkTraffic networkTraffic, OSCommand osCommand) {
        packetParser.createNetworkBands(pcapNetworkInterface);

        PacketListener packetListener = packet -> {
            if (isIPPacket(packet)) {
                calculateResponseNetworkBytes(networkTraffic, osCommand, packet);
            }
        };

        return packetListener;
    }

    private void calculateResponseNetworkBytes(NetworkTraffic networkTraffic, OSCommand osCommand, Packet packet) {
        String ip = packetParser.extractSrcIP(packet);
        if (packetParser.isInLocalNetworkBand(ip)) {
            return;
        }

        String port = packetParser.extractSrcPort(packet);
        String socketIdentifier = ip + ":" + port;
        String processName = osCommand.findProcessName(socketIdentifier);

        if (processName.equals(socketIdentifier)) {
            return;
        }

        networkTraffic.addResponseByte(processName, packetParser.extractBytes(packet));
    }

    private boolean isIPPacket(Packet packet) {
        return packet.get(IpV4Packet.class) != null;
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
        executorService.shutdown();

        try {
            pcapHandle.breakLoop();
        } catch (NotOpenException e) {
            throw new RuntimeException(e);
        }

        pcapHandle.close();
    }

    private final class PacketParser {
        private Set<String> networkBands;
        private List<int[]> netMasks;

        private String extractSrcPort(Packet packet) {
            TcpPacket tcpPacket = packet.get(TcpPacket.class);
            if (tcpPacket != null) {
                return tcpPacket.getHeader().getSrcPort().toString().split(" ")[0];
            }

            UdpPacket udpPacket = packet.get(UdpPacket.class);
            return udpPacket.getHeader().getSrcPort().toString().split(" ")[0];
        }

        private String extractSrcIP(Packet packet) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();

            return ipV4Header.getSrcAddr().getHostAddress().toString();
        }

        private void createNetworkBands(PcapNetworkInterface pcapNetworkInterface) {
            List<PcapAddress> pcapAddresses = pcapNetworkInterface.getAddresses();

            netMasks = new ArrayList<>();

            networkBands = pcapAddresses.stream()
                    .filter(this::hasNetMask)
                    .map(this::convertNetworkBand)
                    .collect(Collectors.toUnmodifiableSet());
        }

        private boolean hasNetMask(PcapAddress pcapAddress) {
            if (pcapAddress.getNetmask() == null) {
                return false;
            }

            return true;
        }

        private String convertNetworkBand(PcapAddress pcapAddress) {
            int[] netMask = Arrays.stream(pcapAddress.getNetmask().getHostName().split("\\."))
                    .mapToInt(Integer::parseInt)
                    .toArray();
            netMasks.add(netMask);

            int[] addresses = Arrays.stream(pcapAddress.getAddress().getHostAddress().split("\\."))
                    .mapToInt(Integer::parseInt)
                    .toArray();

            return calculateNetworkBand(addresses, netMask);
        }

        public long extractBytes(Packet packet) {
            return packet.get(IpV4Packet.class).getHeader().getTotalLengthAsInt();
        }

        public boolean isInLocalNetworkBand(String ip) {
            int[] octets = Arrays.stream(ip.split("\\."))
                    .mapToInt(Integer::parseInt)
                    .toArray();

            for (int[] netMask : netMasks) {
                String networkBand = calculateNetworkBand(octets, netMask);

                if (networkBands.contains(networkBand)) {
                    return true;
                }
            }

            return false;

        }

        private String calculateNetworkBand(int[] octets, int[] netMask) {
            StringBuilder networkBand = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                networkBand.append(octets[i] & netMask[i]);

                if (i == 3) {
                    break;
                }

                networkBand.append(".");
            }

            return networkBand.toString();
        }
    }

}
