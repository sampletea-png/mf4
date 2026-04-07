package com.mdflib.service;

import com.mdflib.jna.MdfLibraryNative;
import com.mdflib.model.*;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.LongByReference;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public class MdfReader implements Closeable {

    private Pointer readerPtr;
    private boolean closed = false;

    public MdfReader(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path must not be null or empty");
        }
        readerPtr = MdfLibraryNative.INSTANCE.MdfReaderInit(filePath);
        if (readerPtr == null) {
            throw new RuntimeException("Failed to initialize MDF reader for: " + filePath);
        }
    }

    public boolean isOk() {
        checkOpen();
        return MdfLibraryNative.INSTANCE.MdfReaderIsOk(readerPtr);
    }

    public boolean open() {
        checkOpen();
        return MdfLibraryNative.INSTANCE.MdfReaderOpen(readerPtr);
    }

    public void close() {
        if (!closed && readerPtr != null) {
            MdfLibraryNative.INSTANCE.MdfReaderClose(readerPtr);
            MdfLibraryNative.INSTANCE.MdfReaderUnInit(readerPtr);
            readerPtr = null;
            closed = true;
        }
    }

    public boolean readHeader() {
        checkOpen();
        return MdfLibraryNative.INSTANCE.MdfReaderReadHeader(readerPtr);
    }

    public boolean readMeasurementInfo() {
        checkOpen();
        return MdfLibraryNative.INSTANCE.MdfReaderReadMeasurementInfo(readerPtr);
    }

    public boolean readEverythingButData() {
        checkOpen();
        return MdfLibraryNative.INSTANCE.MdfReaderReadEverythingButData(readerPtr);
    }

    public boolean readData(int dataGroupIndex) {
        checkOpen();
        Pointer dg = MdfLibraryNative.INSTANCE.MdfReaderGetDataGroup(readerPtr, dataGroupIndex);
        if (dg == null) return false;
        return MdfLibraryNative.INSTANCE.MdfReaderReadData(readerPtr, dg);
    }

    public boolean readAllData() {
        checkOpen();
        List<DataGroupInfo> dgs = getDataGroups();
        for (int i = 0; i < dgs.size(); i++) {
            if (!readData(i)) return false;
        }
        return true;
    }

    public FileInfo getFileInfo() {
        checkOpen();
        Pointer filePtr = MdfLibraryNative.INSTANCE.MdfReaderGetFile(readerPtr);
        if (filePtr == null) return null;

        String name = getString(buf -> MdfLibraryNative.INSTANCE.MdfFileGetName(filePtr, buf));
        String fileName = getString(buf -> MdfLibraryNative.INSTANCE.MdfFileGetFileName(filePtr, buf));
        String version = getString(buf -> MdfLibraryNative.INSTANCE.MdfFileGetVersion(filePtr, buf));
        int mainVersion = MdfLibraryNative.INSTANCE.MdfFileGetMainVersion(filePtr);
        int minorVersion = MdfLibraryNative.INSTANCE.MdfFileGetMinorVersion(filePtr);

        return new FileInfo(name, fileName, version, mainVersion, minorVersion, true);
    }

    public HeaderInfo getHeaderInfo() {
        checkOpen();
        Pointer headerPtr = MdfLibraryNative.INSTANCE.MdfReaderGetHeader(readerPtr);
        if (headerPtr == null) return null;

        String author = getString(buf -> MdfLibraryNative.INSTANCE.MdfHeaderGetAuthor(headerPtr, buf));
        String department = getString(buf -> MdfLibraryNative.INSTANCE.MdfHeaderGetDepartment(headerPtr, buf));
        String project = getString(buf -> MdfLibraryNative.INSTANCE.MdfHeaderGetProject(headerPtr, buf));
        String subject = getString(buf -> MdfLibraryNative.INSTANCE.MdfHeaderGetSubject(headerPtr, buf));
        String description = getString(buf -> MdfLibraryNative.INSTANCE.MdfHeaderGetDescription(headerPtr, buf));
        long startTime = MdfLibraryNative.INSTANCE.MdfHeaderGetStartTime(headerPtr);

        return new HeaderInfo(author, department, project, subject, description, startTime);
    }

    public List<DataGroupInfo> getDataGroups() {
        checkOpen();
        List<DataGroupInfo> result = new ArrayList<>();
        Pointer filePtr = MdfLibraryNative.INSTANCE.MdfReaderGetFile(readerPtr);
        if (filePtr == null) return result;

        long count = MdfLibraryNative.INSTANCE.MdfFileGetDataGroups(filePtr, null);
        for (int i = 0; i < (int) count; i++) {
            Pointer dg = MdfLibraryNative.INSTANCE.MdfReaderGetDataGroup(readerPtr, i);
            if (dg == null) continue;

            long cgCount = MdfLibraryNative.INSTANCE.MdfDataGroupGetChannelGroups(dg, null);
            List<ChannelGroupInfo> cgList = new ArrayList<>();
            if (cgCount > 0) {
                Pointer[] cgPtrs = new Pointer[(int) cgCount];
                MdfLibraryNative.INSTANCE.MdfDataGroupGetChannelGroups(dg, cgPtrs);
                for (Pointer cg : cgPtrs) {
                    if (cg == null) continue;
                    String cgName = getString(buf -> MdfLibraryNative.INSTANCE.MdfChannelGroupGetName(cg, buf));
                    long samples = MdfLibraryNative.INSTANCE.MdfChannelGroupGetNofSamples(cg);

                    long chCount = MdfLibraryNative.INSTANCE.MdfChannelGroupGetChannels(cg, null);
                    List<ChannelData> channels = new ArrayList<>();
                    if (chCount > 0) {
                        Pointer[] chPtrs = new Pointer[(int) chCount];
                        MdfLibraryNative.INSTANCE.MdfChannelGroupGetChannels(cg, chPtrs);
                        for (Pointer ch : chPtrs) {
                            if (ch == null) continue;
                            String chName = getString(buf -> MdfLibraryNative.INSTANCE.MdfChannelGetName(ch, buf));
                            String chUnit = "";
                            boolean unitUsed = MdfLibraryNative.INSTANCE.MdfChannelIsUnitUsed(ch);
                            if (unitUsed) {
                                chUnit = getString(buf2 -> MdfLibraryNative.INSTANCE.MdfChannelGetUnit(ch, buf2));
                            }
                            byte chType = MdfLibraryNative.INSTANCE.MdfChannelGetType(ch);
                            byte chDataType = MdfLibraryNative.INSTANCE.MdfChannelGetDataType(ch);
                            channels.add(new ChannelData(chName, chUnit, chType, chDataType));
                        }
                    }
                    cgList.add(new ChannelGroupInfo(cgName, samples, channels));
                }
            }
            result.add(new DataGroupInfo("DG" + i, cgList));
        }
        return result;
    }

    public List<String> getChannelNames() {
        List<String> names = new ArrayList<>();
        for (DataGroupInfo dg : getDataGroups()) {
            for (ChannelGroupInfo cg : dg.getChannelGroups()) {
                for (ChannelData ch : cg.getChannels()) {
                    names.add(ch.getName());
                }
            }
        }
        return names;
    }

    public List<Double> getChannelValuesAsDouble(int dataGroupIndex, String channelName) {
        checkOpen();
        List<Double> values = new ArrayList<>();

        Pointer dg = MdfLibraryNative.INSTANCE.MdfReaderGetDataGroup(readerPtr, dataGroupIndex);
        if (dg == null) return values;

        Pointer observer = MdfLibraryNative.INSTANCE.MdfChannelObserverCreateByChannelName(dg, channelName);
        if (observer == null) return values;

        try {
            long sampleCount = MdfLibraryNative.INSTANCE.MdfChannelObserverGetNofSamples(observer);
            DoubleByReference valRef = new DoubleByReference();
            for (long s = 0; s < sampleCount; s++) {
                boolean ok = MdfLibraryNative.INSTANCE.MdfChannelObserverGetEngValueAsFloat(observer, s, valRef);
                if (ok) {
                    values.add(valRef.getValue());
                }
            }
        } finally {
            MdfLibraryNative.INSTANCE.MdfChannelObserverUnInit(observer);
        }
        return values;
    }

    public List<Long> getChannelValuesAsLong(int dataGroupIndex, String channelName) {
        checkOpen();
        List<Long> values = new ArrayList<>();

        Pointer dg = MdfLibraryNative.INSTANCE.MdfReaderGetDataGroup(readerPtr, dataGroupIndex);
        if (dg == null) return values;

        Pointer observer = MdfLibraryNative.INSTANCE.MdfChannelObserverCreateByChannelName(dg, channelName);
        if (observer == null) return values;

        try {
            long sampleCount = MdfLibraryNative.INSTANCE.MdfChannelObserverGetNofSamples(observer);
            LongByReference valRef = new LongByReference();
            for (long s = 0; s < sampleCount; s++) {
                boolean ok = MdfLibraryNative.INSTANCE.MdfChannelObserverGetEngValueAsSigned(observer, s, valRef);
                if (ok) {
                    values.add(valRef.getValue());
                }
            }
        } finally {
            MdfLibraryNative.INSTANCE.MdfChannelObserverUnInit(observer);
        }
        return values;
    }

    private void checkOpen() {
        if (closed || readerPtr == null) {
            throw new IllegalStateException("MdfReader is closed");
        }
    }

    private interface StringGetter {
        long get(byte[] buf);
    }

    private String getString(StringGetter getter) {
        long lenLong = getter.get(null);
        if (lenLong <= 0) return "";
        int len = (int) lenLong;
        byte[] buf = new byte[len + 1];
        getter.get(buf);
        int actualLen = 0;
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] == 0) break;
            actualLen++;
        }
        return new String(buf, 0, actualLen);
    }
}
