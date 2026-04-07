package com.mdflib.model;

import com.sun.jna.Pointer;

public class ChannelData {
    private final String name;
    private final String unit;
    private final byte channelType;
    private final byte dataType;

    public ChannelData(String name, String unit, byte channelType, byte dataType) {
        this.name = name;
        this.unit = unit;
        this.channelType = channelType;
        this.dataType = dataType;
    }

    public String getName() { return name; }
    public String getUnit() { return unit; }
    public byte getChannelType() { return channelType; }
    public byte getDataType() { return dataType; }

    @Override
    public String toString() {
        return "ChannelData{name='" + name + "', unit='" + unit + "', channelType=" + channelType + ", dataType=" + dataType + "}";
    }
}
