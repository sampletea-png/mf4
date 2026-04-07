package com.mdflib.service;

import com.mdflib.jna.MdfLibraryNative;
import com.sun.jna.Pointer;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public class MdfWriter implements Closeable {

    public static final int MDF4_BASIC = 1;

    private Pointer writerPtr;
    private boolean closed = false;
    private boolean measurementStarted = false;
    private boolean measurementFinalized = false;
    private List<Pointer> channelGroups = new ArrayList<>();
    private List<Pointer> channels = new ArrayList<>();

    public MdfWriter(String filePath) {
        this(MDF4_BASIC, filePath);
    }

    public MdfWriter(int type, String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path must not be null or empty");
        }
        writerPtr = MdfLibraryNative.INSTANCE.MdfWriterInit(type, filePath);
        if (writerPtr == null) {
            throw new RuntimeException("Failed to initialize MDF writer for: " + filePath);
        }
    }

    public void setAuthor(String author) {
        checkOpen();
        Pointer header = MdfLibraryNative.INSTANCE.MdfWriterGetHeader(writerPtr);
        if (header != null) {
            MdfLibraryNative.INSTANCE.MdfHeaderSetAuthor(header, author);
        }
    }

    public void setDepartment(String department) {
        checkOpen();
        Pointer header = MdfLibraryNative.INSTANCE.MdfWriterGetHeader(writerPtr);
        if (header != null) {
            MdfLibraryNative.INSTANCE.MdfHeaderSetDepartment(header, department);
        }
    }

    public void setProject(String project) {
        checkOpen();
        Pointer header = MdfLibraryNative.INSTANCE.MdfWriterGetHeader(writerPtr);
        if (header != null) {
            MdfLibraryNative.INSTANCE.MdfHeaderSetProject(header, project);
        }
    }

    public void setSubject(String subject) {
        checkOpen();
        Pointer header = MdfLibraryNative.INSTANCE.MdfWriterGetHeader(writerPtr);
        if (header != null) {
            MdfLibraryNative.INSTANCE.MdfHeaderSetSubject(header, subject);
        }
    }

    public void setDescription(String description) {
        checkOpen();
        Pointer header = MdfLibraryNative.INSTANCE.MdfWriterGetHeader(writerPtr);
        if (header != null) {
            MdfLibraryNative.INSTANCE.MdfHeaderSetDescription(header, description);
        }
    }

    public void setCompressData(boolean compress) {
        checkOpen();
        MdfLibraryNative.INSTANCE.MdfWriterSetCompressData(writerPtr, compress);
    }

    public Pointer createDataGroup() {
        checkOpen();
        Pointer dg = MdfLibraryNative.INSTANCE.MdfWriterCreateDataGroup(writerPtr);
        if (dg == null) {
            throw new RuntimeException("Failed to create data group");
        }
        return dg;
    }

    public Pointer createChannelGroup(Pointer dataGroup) {
        checkOpen();
        Pointer cg = MdfLibraryNative.INSTANCE.MdfDataGroupCreateChannelGroup(dataGroup);
        if (cg == null) {
            throw new RuntimeException("Failed to create channel group");
        }
        channelGroups.add(cg);
        return cg;
    }

    public Pointer createChannel(Pointer channelGroup) {
        checkOpen();
        Pointer ch = MdfLibraryNative.INSTANCE.MdfChannelGroupCreateChannel(channelGroup);
        if (ch == null) {
            throw new RuntimeException("Failed to create channel");
        }
        channels.add(ch);
        return ch;
    }

    public void setChannelName(Pointer channel, String name) {
        MdfLibraryNative.INSTANCE.MdfChannelSetName(channel, name);
    }

    public void setChannelUnit(Pointer channel, String unit) {
        MdfLibraryNative.INSTANCE.MdfChannelSetUnit(channel, unit);
    }

    public void setChannelType(Pointer channel, byte type) {
        MdfLibraryNative.INSTANCE.MdfChannelSetType(channel, type);
    }

    public void setChannelSyncType(Pointer channel, byte syncType) {
        MdfLibraryNative.INSTANCE.MdfChannelSetSync(channel, syncType);
    }

    public void setChannelDataType(Pointer channel, byte dataType) {
        MdfLibraryNative.INSTANCE.MdfChannelSetDataType(channel, dataType);
    }

    public void setChannelBitCount(Pointer channel, int bits) {
        MdfLibraryNative.INSTANCE.MdfChannelSetBitCount(channel, bits);
    }

    public void setChannelValueAsDouble(Pointer channel, double value) {
        MdfLibraryNative.INSTANCE.MdfChannelSetChannelValueAsFloat(channel, value, (byte) 1, 0);
    }

    public void setChannelValueAsLong(Pointer channel, long value) {
        MdfLibraryNative.INSTANCE.MdfChannelSetChannelValueAsSigned(channel, value, (byte) 1, 0);
    }

    public void setChannelValueAsString(Pointer channel, String value) {
        MdfLibraryNative.INSTANCE.MdfChannelSetChannelValueAsString(channel, value.getBytes(), (byte) 1, 0);
    }

    public boolean initMeasurement() {
        checkOpen();
        return MdfLibraryNative.INSTANCE.MdfWriterInitMeasurement(writerPtr);
    }

    public void saveSample(Pointer channelGroup, long time) {
        checkOpen();
        MdfLibraryNative.INSTANCE.MdfWriterSaveSample(writerPtr, channelGroup, time);
    }

    public void startMeasurement(long startTime) {
        checkOpen();
        MdfLibraryNative.INSTANCE.MdfWriterStartMeasurement(writerPtr, startTime);
        measurementStarted = true;
    }

    public void stopMeasurement(long stopTime) {
        checkOpen();
        MdfLibraryNative.INSTANCE.MdfWriterStopMeasurement(writerPtr, stopTime);
        measurementStarted = false;
    }

    public boolean finalizeMeasurement() {
        checkOpen();
        boolean result = MdfLibraryNative.INSTANCE.MdfWriterFinalizeMeasurement(writerPtr);
        measurementFinalized = true;
        return result;
    }

    public void close() {
        if (!closed && writerPtr != null) {
            MdfLibraryNative.INSTANCE.MdfWriterUnInit(writerPtr);
            writerPtr = null;
            closed = true;
        }
    }

    private void checkOpen() {
        if (closed || writerPtr == null) {
            throw new IllegalStateException("MdfWriter is closed");
        }
    }

    public static class ChannelTypes {
        public static final byte FIXED_LENGTH = 0;
        public static final byte VARIABLE_LENGTH = 1;
        public static final byte MASTER = 2;
        public static final byte VIRTUAL_MASTER = 3;
    }

    public static class SyncTypes {
        public static final byte NONE = 0;
        public static final byte TIME = 1;
        public static final byte ANGLE = 2;
        public static final byte DISTANCE = 3;
        public static final byte INDEX = 4;
    }

    public static class DataTypes {
        public static final byte UNSIGNED_INT_LE = 0;
        public static final byte SIGNED_INT_LE = 2;
        public static final byte FLOAT_LE = 4;
        public static final byte STRING_ASCII = 6;
        public static final byte STRING_UTF8 = 7;
        public static final byte BYTE_ARRAY = 10;
    }
}
