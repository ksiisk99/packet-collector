package org.ksi;

final class NetworkInterfaceInfo {
    private String nifName;
    private String description;
    private String linkLayerAddress;
    private String address;

    NetworkInterfaceInfo(String nifName, String description, String linkLayerAddress, String address) {
        this.nifName = nifName;
        this.description = description;
        this.linkLayerAddress = linkLayerAddress;
        this.address = address;
    }

    @Override
    public String toString() {
        return "org.ksi.NetworkInterfaceInfo{" +
                "nifName='" + nifName + '\'' +
                ", description='" + description + '\'' +
                ", linkLayerAddress='" + linkLayerAddress + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
