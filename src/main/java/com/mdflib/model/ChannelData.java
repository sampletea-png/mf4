package com.mdflib.model;

/**
 * Represents metadata for a single MDF channel.
 *
 * <p>A channel corresponds to one measurement signal within a channel group.
 * Each channel has a name, optional unit, type classification, and data type.</p>
 *
 * <p>Channel types:</p>
 * <ul>
 *   <li>0 = Fixed length data</li>
 *   <li>1 = Variable length data</li>
 *   <li>2 = Master channel (typically time)</li>
 *   <li>3 = Virtual master channel</li>
 * </ul>
 *
 * <p>Data types:</p>
 * <ul>
 *   <li>0 = Unsigned integer (little-endian)</li>
 *   <li>2 = Signed integer (little-endian)</li>
 *   <li>4 = Floating point (little-endian)</li>
 *   <li>6 = ASCII string</li>
 *   <li>7 = UTF-8 string</li>
 *   <li>10 = Byte array</li>
 * </ul>
 *
 * @author mdflib-java contributors
 * @version 1.0.0
 * @since 1.0.0
 */
public class ChannelData {

    /** The display name of the channel. */
    private final String name;

    /** The engineering unit of the channel (e.g., "V", "m/s", "degC"). May be empty. */
    private final String unit;

    /** The channel type code (0=Fixed, 1=Variable, 2=Master, 3=VirtualMaster). */
    private final byte channelType;

    /** The data type code (0=UIntLE, 2=SIntLE, 4=FloatLE, 6=StringASCII, etc.). */
    private final byte dataType;

    /**
     * Constructs a ChannelData instance with the specified properties.
     *
     * @param name the channel name, must not be null
     * @param unit the engineering unit, may be empty but not null
     * @param channelType the channel type code
     * @param dataType the data type code
     * @throws IllegalArgumentException if name or unit is null
     */
    public ChannelData(String name, String unit, byte channelType, byte dataType) {
        if (name == null) {
            throw new IllegalArgumentException("Channel name must not be null");
        }
        if (unit == null) {
            throw new IllegalArgumentException("Channel unit must not be null");
        }
        this.name = name;
        this.unit = unit;
        this.channelType = channelType;
        this.dataType = dataType;
    }

    /**
     * Returns the channel name.
     *
     * @return the channel name, never null
     */
    public String getName() { return name; }

    /**
     * Returns the engineering unit string.
     *
     * @return the unit string, may be empty but never null
     */
    public String getUnit() { return unit; }

    /**
     * Returns the channel type code.
     *
     * @return the channel type byte value
     */
    public byte getChannelType() { return channelType; }

    /**
     * Returns the data type code.
     *
     * @return the data type byte value
     */
    public byte getDataType() { return dataType; }

    /**
     * Returns a string representation of this channel data.
     *
     * @return formatted string with name, unit, channelType, and dataType
     */
    @Override
    public String toString() {
        return "ChannelData{name='" + name + "', unit='" + unit
            + "', channelType=" + channelType + ", dataType=" + dataType + "}";
    }
}
