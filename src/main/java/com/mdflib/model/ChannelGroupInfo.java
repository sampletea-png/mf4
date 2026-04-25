package com.mdflib.model;

import java.util.List;

/**
 * Represents a channel group within an MDF data group.
 *
 * <p>A channel group contains a set of channels that are sampled together
 * at the same time stamps. All channels in a group share the same number
 * of samples.</p>
 *
 * @author mdflib-java contributors
 * @version 1.0.0
 * @since 1.0.0
 */
public class ChannelGroupInfo {

    /** The name of the channel group. */
    private final String name;

    /** The number of samples recorded in this channel group. */
    private final long sampleCount;

    /** The list of channels within this channel group. */
    private final List<ChannelData> channels;

    /**
     * Constructs a ChannelGroupInfo.
     *
     * @param name the channel group name
     * @param sampleCount the number of samples
     * @param channels the list of channels
     */
    public ChannelGroupInfo(String name, long sampleCount, List<ChannelData> channels) {
        this.name = name;
        this.sampleCount = sampleCount;
        this.channels = channels;
    }

    /** @return the channel group name */
    public String getName() { return name; }

    /** @return the number of recorded samples */
    public long getNofSamples() { return sampleCount; }

    /** @return the list of channels, may be empty but not null */
    public List<ChannelData> getChannels() { return channels; }

    @Override
    public String toString() {
        return "ChannelGroupInfo{name='" + name + "', samples=" + sampleCount
            + ", channels=" + channels + "}";
    }
}
