package com.mdflib.model;

import java.util.List;

public class DataGroupInfo {
    private final String description;
    private final List<ChannelGroupInfo> channelGroups;

    public DataGroupInfo(String description, List<ChannelGroupInfo> channelGroups) {
        this.description = description;
        this.channelGroups = channelGroups;
    }

    public String getDescription() { return description; }
    public List<ChannelGroupInfo> getChannelGroups() { return channelGroups; }

    @Override
    public String toString() {
        return "DataGroupInfo{description='" + description + "', channelGroups=" + channelGroups + "}";
    }
}
