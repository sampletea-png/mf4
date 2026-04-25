package com.mdflib.service;

import com.mdflib.jni.MdfLibraryNative;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * High-level MDF file writer for creating MDF measurement files.
 *
 * <p>This class wraps the low-level JNI native calls into an easy-to-use API
 * for writing MDF (Measurement Data Format) files. It supports creating
 * data groups, channel groups, channels, and recording measurement samples.</p>
 *
 * <p>Typical usage pattern:</p>
 * <pre>
 *   MdfWriter writer = new MdfWriter("output.mf4");
 *   try {
 *       writer.setAuthor("TestAuthor");
 *       writer.setProject("TestProject");
 *
 *       long dg = writer.createDataGroup();
 *       long cg = writer.createChannelGroup(dg);
 *
 *       long timeCh = writer.createChannel(cg);
 *       writer.setChannelName(timeCh, "t");
 *       writer.setChannelType(timeCh, MdfWriter.ChannelTypes.MASTER);
 *       writer.setChannelSyncType(timeCh, MdfWriter.SyncTypes.TIME);
 *       writer.setChannelDataType(timeCh, MdfWriter.DataTypes.FLOAT_LE);
 *       writer.setChannelDataBytes(timeCh, 8);
 *
 *       writer.initMeasurement();
 *       writer.startMeasurement(startTime);
 *
 *       for (int i = 0; i &lt; numSamples; i++) {
 *           writer.setChannelValueAsDouble(timeCh, i * 0.01);
 *           writer.saveSample(cg, startTime + i * 10000L);
 *       }
 *
 *       writer.stopMeasurement(stopTime);
 *       writer.finalizeMeasurement();
 *   } finally {
 *       writer.close();
 *   }
 * </pre>
 *
 * <p>Thread safety: This class is NOT thread-safe. Each instance should be
 * used from a single thread, or external synchronization must be applied.</p>
 *
 * <p>Resource management: Always call {@link #close()} when done. The close
 * method will properly release all native resources.</p>
 *
 * @author mdflib-java contributors
 * @version 1.0.0
 * @since 1.0.0
 * @see MdfReader
 */
public class MdfWriter implements Closeable {

    /** MDF4 basic file format type constant. */
    public static final int MDF4_BASIC = 1;

    /** Native pointer to the underlying MdfWriter C++ object. */
    private long writerPtr;

    /** Flag indicating whether this writer has been closed. */
    private boolean closed = false;

    /** Flag indicating whether a measurement is currently in progress. */
    private boolean measurementStarted = false;

    /** Flag indicating whether the measurement has been finalized. */
    private boolean measurementFinalized = false;

    /**
     * List of channel group pointers created by this writer.
     * Used for tracking and potential cleanup.
     */
    private List<Long> channelGroups = new ArrayList<>();

    /**
     * List of channel pointers created by this writer.
     * Used for tracking and potential cleanup.
     */
    private List<Long> channels = new ArrayList<>();

    /** Reference to the JNI native library singleton. */
    private final MdfLibraryNative nativeLib;

    /**
     * Constructs a new MdfWriter for MDF4 basic format.
     *
     * <p>Equivalent to {@code MdfWriter(MDF4_BASIC, filePath)}.</p>
     *
     * @param filePath the output file path for the MDF file
     * @throws IllegalArgumentException if filePath is null or empty
     * @throws RuntimeException if the native writer cannot be initialized
     */
    public MdfWriter(String filePath) {
        this(MDF4_BASIC, filePath);
    }

    /**
     * Constructs a new MdfWriter with the specified format type.
     *
     * @param type the MDF writer type (e.g., MDF4_BASIC = 1)
     * @param filePath the output file path for the MDF file
     * @throws IllegalArgumentException if filePath is null or empty
     * @throws RuntimeException if the native writer cannot be initialized
     */
    public MdfWriter(int type, String filePath) {
        this(type, filePath, MdfLibraryNative.getInstance());
    }

    /**
     * Constructs a new MdfWriter with a specified native library instance.
     *
     * <p>This constructor is primarily useful for testing with mocked
     * native libraries.</p>
     *
     * @param type the MDF writer type
     * @param filePath the output file path
     * @param nativeLib the native library instance to use
     * @throws IllegalArgumentException if filePath is null or empty
     * @throws RuntimeException if the native writer cannot be initialized
     */
    public MdfWriter(int type, String filePath, MdfLibraryNative nativeLib) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path must not be null or empty");
        }
        this.nativeLib = nativeLib;
        writerPtr = nativeLib.MdfWriterInit(type, filePath);
        if (writerPtr == 0) {
            throw new RuntimeException("Failed to initialize MDF writer for: " + filePath);
        }
    }

    /**
     * Sets the author metadata field in the file header.
     *
     * <p>Must be called before {@link #initMeasurement()}.</p>
     *
     * @param author the author name to set
     * @throws IllegalStateException if the writer has been closed
     */
    public void setAuthor(String author) {
        checkOpen();
        long header = nativeLib.MdfWriterGetHeader(writerPtr);
        if (header != 0) {
            nativeLib.MdfHeaderSetAuthor(header, author);
        }
    }

    /**
     * Sets the department metadata field in the file header.
     *
     * @param department the department name to set
     * @throws IllegalStateException if the writer has been closed
     */
    public void setDepartment(String department) {
        checkOpen();
        long header = nativeLib.MdfWriterGetHeader(writerPtr);
        if (header != 0) {
            nativeLib.MdfHeaderSetDepartment(header, department);
        }
    }

    /**
     * Sets the project metadata field in the file header.
     *
     * @param project the project name to set
     * @throws IllegalStateException if the writer has been closed
     */
    public void setProject(String project) {
        checkOpen();
        long header = nativeLib.MdfWriterGetHeader(writerPtr);
        if (header != 0) {
            nativeLib.MdfHeaderSetProject(header, project);
        }
    }

    /**
     * Sets the subject metadata field in the file header.
     *
     * @param subject the subject description to set
     * @throws IllegalStateException if the writer has been closed
     */
    public void setSubject(String subject) {
        checkOpen();
        long header = nativeLib.MdfWriterGetHeader(writerPtr);
        if (header != 0) {
            nativeLib.MdfHeaderSetSubject(header, subject);
        }
    }

    /**
     * Sets the description metadata field in the file header.
     *
     * @param description the description text to set
     * @throws IllegalStateException if the writer has been closed
     */
    public void setDescription(String description) {
        checkOpen();
        long header = nativeLib.MdfWriterGetHeader(writerPtr);
        if (header != 0) {
            nativeLib.MdfHeaderSetDescription(header, description);
        }
    }

    /**
     * Sets whether data should be compressed in the output file.
     *
     * <p>Compression can significantly reduce file size, especially for
     * signals with constant or slowly changing values.</p>
     *
     * @param compress true to enable compression, false to disable
     * @throws IllegalStateException if the writer has been closed
     */
    public void setCompressData(boolean compress) {
        checkOpen();
        nativeLib.MdfWriterSetCompressData(writerPtr, compress ? (byte) 1 : (byte) 0);
    }

    /**
     * Creates a new data group in the writer.
     *
     * <p>Data groups are the top-level organizational unit. Each writer
     * must have at least one data group to contain measurement data.</p>
     *
     * @return native pointer (long) to the new IDataGroup
     * @throws RuntimeException if the data group cannot be created
     * @throws IllegalStateException if the writer has been closed
     */
    public long createDataGroup() {
        checkOpen();
        long dg = nativeLib.MdfWriterCreateDataGroup(writerPtr);
        if (dg == 0) {
            throw new RuntimeException("Failed to create data group");
        }
        return dg;
    }

    /**
     * Creates a new channel group within the specified data group.
     *
     * <p>A channel group contains a set of channels that are sampled
     * together (at the same time stamps).</p>
     *
     * @param dataGroup native pointer to the parent IDataGroup
     * @return native pointer (long) to the new IChannelGroup
     * @throws RuntimeException if the channel group cannot be created
     * @throws IllegalStateException if the writer has been closed
     */
    public long createChannelGroup(long dataGroup) {
        checkOpen();
        long cg = nativeLib.MdfDataGroupCreateChannelGroup(dataGroup);
        if (cg == 0) {
            throw new RuntimeException("Failed to create channel group");
        }
        channelGroups.add(cg);
        return cg;
    }

    /**
     * Creates a new channel within the specified channel group.
     *
     * <p>After creating a channel, configure it with name, unit, type,
     * data type, and data bytes before starting measurement.</p>
     *
     * @param channelGroup native pointer to the parent IChannelGroup
     * @return native pointer (long) to the new IChannel
     * @throws RuntimeException if the channel cannot be created
     * @throws IllegalStateException if the writer has been closed
     */
    public long createChannel(long channelGroup) {
        checkOpen();
        long ch = nativeLib.MdfChannelGroupCreateChannel(channelGroup);
        if (ch == 0) {
            throw new RuntimeException("Failed to create channel");
        }
        channels.add(ch);
        return ch;
    }

    /**
     * Sets the name of a channel.
     *
     * @param channel native pointer to the IChannel
     * @param name the channel name (e.g., "temperature", "pressure")
     */
    public void setChannelName(long channel, String name) {
        nativeLib.MdfChannelSetName(channel, name);
    }

    /**
     * Sets the engineering unit of a channel.
     *
     * @param channel native pointer to the IChannel
     * @param unit the unit string (e.g., "V", "m/s", "degC")
     */
    public void setChannelUnit(long channel, String unit) {
        nativeLib.MdfChannelSetUnit(channel, unit);
    }

    /**
     * Sets the type of a channel.
     *
     * <p>See {@link ChannelTypes} for predefined type constants.</p>
     *
     * @param channel native pointer to the IChannel
     * @param type the channel type byte value
     */
    public void setChannelType(long channel, byte type) {
        nativeLib.MdfChannelSetType(channel, type);
    }

    /**
     * Sets the sync type of a channel.
     *
     * <p>See {@link SyncTypes} for predefined sync type constants.</p>
     *
     * @param channel native pointer to the IChannel
     * @param syncType the sync type byte value
     */
    public void setChannelSyncType(long channel, byte syncType) {
        nativeLib.MdfChannelSetSync(channel, syncType);
    }

    /**
     * Sets the data type of a channel.
     *
     * <p>See {@link DataTypes} for predefined data type constants.</p>
     *
     * @param channel native pointer to the IChannel
     * @param dataType the data type byte value
     */
    public void setChannelDataType(long channel, byte dataType) {
        nativeLib.MdfChannelSetDataType(channel, dataType);
    }

    /**
     * Sets the bit count (resolution) of a channel.
     *
     * <p>Common values: 8, 16, 32, 64 bits.</p>
     *
     * @param channel native pointer to the IChannel
     * @param bits the number of bits
     */
    public void setChannelBitCount(long channel, int bits) {
        nativeLib.MdfChannelSetBitCount(channel, bits);
    }

    /**
     * Sets the data byte count of a channel.
     *
     * <p>Common values: 1, 2, 4, 8 bytes.</p>
     *
     * @param channel native pointer to the IChannel
     * @param bytes the number of data bytes
     */
    public void setChannelDataBytes(long channel, long bytes) {
        nativeLib.MdfChannelSetDataBytes(channel, bytes);
    }

    /**
     * Sets a floating-point value for a channel for the current sample.
     *
     * <p>Must be called before {@link #saveSample(long, long)} to set
     * the value for this channel in the current sample.</p>
     *
     * @param channel native pointer to the IChannel
     * @param value the double value to record
     */
    public void setChannelValueAsDouble(long channel, double value) {
        nativeLib.MdfChannelSetChannelValueAsFloat(channel, value, 1, 0);
    }

    /**
     * Sets a signed long value for a channel for the current sample.
     *
     * @param channel native pointer to the IChannel
     * @param value the long value to record
     */
    public void setChannelValueAsLong(long channel, long value) {
        nativeLib.MdfChannelSetChannelValueAsSigned(channel, value, 1, 0);
    }

    /**
     * Sets a string value for a channel for the current sample.
     *
     * @param channel native pointer to the IChannel
     * @param value the string value to record
     */
    public void setChannelValueAsString(long channel, String value) {
        nativeLib.MdfChannelSetChannelValueAsString(channel, value.getBytes(), 1, 0);
    }

    /**
     * Initializes the measurement, preparing all configured channels for recording.
     *
     * <p>Must be called after all data groups, channel groups, and channels
     * are configured, and before {@link #startMeasurement(long)}.</p>
     *
     * @return true if initialization succeeded, false otherwise
     * @throws IllegalStateException if the writer has been closed
     */
    public boolean initMeasurement() {
        checkOpen();
        return nativeLib.MdfWriterInitMeasurement(writerPtr);
    }

    /**
     * Saves a sample for all channels in the specified channel group.
     *
     * <p>Channel values must be set (via setChannelValue* methods) before
     * calling this method. Each call records one time step of data.</p>
     *
     * @param channelGroup native pointer to the IChannelGroup
     * @param time the timestamp for this sample in nanoseconds
     * @throws IllegalStateException if the writer has been closed
     */
    public void saveSample(long channelGroup, long time) {
        checkOpen();
        nativeLib.MdfWriterSaveSample(writerPtr, channelGroup, time);
    }

    /**
     * Starts the measurement at the specified time.
     *
     * <p>Must be called after {@link #initMeasurement()} and before
     * any {@link #saveSample(long, long)} calls.</p>
     *
     * @param startTime start timestamp in nanoseconds since epoch
     * @throws IllegalStateException if the writer has been closed
     */
    public void startMeasurement(long startTime) {
        checkOpen();
        nativeLib.MdfWriterStartMeasurement(writerPtr, startTime);
        measurementStarted = true;
    }

    /**
     * Stops the measurement at the specified time.
     *
     * <p>After stopping, call {@link #finalizeMeasurement()} to complete
     * the file and write all data to disk.</p>
     *
     * @param stopTime stop timestamp in nanoseconds since epoch
     * @throws IllegalStateException if the writer has been closed
     */
    public void stopMeasurement(long stopTime) {
        checkOpen();
        nativeLib.MdfWriterStopMeasurement(writerPtr, stopTime);
        measurementStarted = false;
    }

    /**
     * Finalizes the measurement and writes all data to the output file.
     *
     * <p>Must be called after {@link #stopMeasurement(long)}. After this
     * call, the MDF file is complete and can be read by other tools.</p>
     *
     * @return true if finalization succeeded, false otherwise
     * @throws IllegalStateException if the writer has been closed
     */
    public boolean finalizeMeasurement() {
        checkOpen();
        boolean result = nativeLib.MdfWriterFinalizeMeasurement(writerPtr);
        measurementFinalized = true;
        return result;
    }

    /**
     * Closes the writer and releases all native resources.
     *
     * <p>Safe to call multiple times. If the measurement was not finalized,
     * this will still release resources but the output file may be incomplete.</p>
     */
    @Override
    public void close() {
        if (!closed && writerPtr != 0) {
            nativeLib.MdfWriterUnInit(writerPtr);
            writerPtr = 0;
            closed = true;
        }
    }

    /**
     * Verifies that this writer has not been closed.
     *
     * @throws IllegalStateException if the writer has been closed or the pointer is invalid
     */
    private void checkOpen() {
        if (closed || writerPtr == 0) {
            throw new IllegalStateException("MdfWriter is closed");
        }
    }

    /**
     * Predefined channel type constants for MDF channels.
     *
     * <p>These constants define the type of data a channel represents:</p>
     * <ul>
     *   <li>{@link #FIXED_LENGTH} - Fixed-length data channel</li>
     *   <li>{@link #VARIABLE_LENGTH} - Variable-length data channel</li>
     *   <li>{@link #MASTER} - Master channel (time base)</li>
     *   <li>{@link #VIRTUAL_MASTER} - Virtual master channel</li>
     * </ul>
     */
    public static class ChannelTypes {
        /** Fixed-length data channel (normal measurement signal). */
        public static final byte FIXED_LENGTH = 0;
        /** Variable-length data channel (e.g., strings, byte arrays). */
        public static final byte VARIABLE_LENGTH = 1;
        /** Master channel providing the time base for the channel group. */
        public static final byte MASTER = 2;
        /** Virtual master channel (calculated, not stored). */
        public static final byte VIRTUAL_MASTER = 3;
    }

    /**
     * Predefined synchronization type constants for MDF channels.
     *
     * <p>These constants define how channel data is synchronized:</p>
     * <ul>
     *   <li>{@link #NONE} - No synchronization</li>
     *   <li>{@link #TIME} - Time-based synchronization</li>
     *   <li>{@link #ANGLE} - Angle-based synchronization</li>
     *   <li>{@link #DISTANCE} - Distance-based synchronization</li>
     *   <li>{@link #INDEX} - Index-based synchronization</li>
     * </ul>
     */
    public static class SyncTypes {
        /** No synchronization type specified. */
        public static final byte NONE = 0;
        /** Time-based synchronization (most common for measurement data). */
        public static final byte TIME = 1;
        /** Angle-based synchronization (e.g., crankshaft angle). */
        public static final byte ANGLE = 2;
        /** Distance-based synchronization (e.g., road distance). */
        public static final byte DISTANCE = 3;
        /** Index-based synchronization (sample index). */
        public static final byte INDEX = 4;
    }

    /**
     * Predefined data type constants for MDF channel values.
     *
     * <p>These constants define the binary representation of channel data:</p>
     * <ul>
     *   <li>{@link #UNSIGNED_INT_LE} - Unsigned integer, little-endian</li>
     *   <li>{@link #SIGNED_INT_LE} - Signed integer, little-endian</li>
     *   <li>{@link #FLOAT_LE} - IEEE 754 floating point, little-endian</li>
     *   <li>{@link #STRING_ASCII} - ASCII string</li>
     *   <li>{@link #STRING_UTF8} - UTF-8 encoded string</li>
     *   <li>{@link #BYTE_ARRAY} - Raw byte array</li>
     * </ul>
     */
    public static class DataTypes {
        /** Unsigned integer, little-endian byte order. */
        public static final byte UNSIGNED_INT_LE = 0;
        /** Signed integer, little-endian byte order. */
        public static final byte SIGNED_INT_LE = 2;
        /** IEEE 754 floating point, little-endian byte order. */
        public static final byte FLOAT_LE = 4;
        /** ASCII-encoded string. */
        public static final byte STRING_ASCII = 6;
        /** UTF-8 encoded string. */
        public static final byte STRING_UTF8 = 7;
        /** Raw byte array. */
        public static final byte BYTE_ARRAY = 10;
    }
}
