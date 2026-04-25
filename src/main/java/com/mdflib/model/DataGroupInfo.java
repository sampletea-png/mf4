package com.mdflib.model;

import java.util.List;

/**
 * Represents a data group within an MDF file.
 *
 * <p>A data group is the top-level organizational unit for measurement data.
 * Each data group contains one or more channel groups, which in turn
 * contain the actual measurement channels.</p>
 *
 * @author mdflib-java contributors
 * @version 1.0.0
 * @since 1.0.0
 */
public class DataGroupInfo {

    /** A description or identifier for this data group. */
    private final String description;

    /** The list of channel groups within this data group. */
    private final List<ChannelGroupInfo> channelGroups;

    /**
     * Constructs a DataGroupInfo.
     *
     * @param description the data group description
     * @param channelGroups the list of channel groups
     */
    public DataGroupInfo(String description, List<ChannelGroupInfo> channelGroups) {
        this.description = description;
        this.channelGroups = channelGroups;
    }

    /** @return the data group description */
    public String getDescription() { return description; }

    /** @return the list of channel groups, may be empty but not null */
    public List<ChannelGroupInfo> getChannelGroups() { return channelGroups; }

    @Override
    public String toString() {
        return "DataGroupInfo{description='" + description
            + "', channelGroups=" + channelGroups + "}";
    }
}
