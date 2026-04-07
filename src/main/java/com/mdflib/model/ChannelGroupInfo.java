package com.mdflib.model;

import java.util.List;

public class ChannelGroupInfo {
    private final String name;
    private final long sampleCount;
    private final List<ChannelData> channels;

    public ChannelGroupInfo(String name, long sampleCount, List<ChannelData> channels) {
        this.name = name;
        this.sampleCount = sampleCount;
        this.channels = channels;
    }

    public String getName() { return name; }
    public long getSampleCount() { return sampleCount; }
    public List<ChannelData> getChannels() { return channels; }

    @Override
    public String toString() {
        return "ChannelGroupInfo{name='" + name + "', samples=" + sampleCount + ", channels=" + channels + "}";
    }
}
