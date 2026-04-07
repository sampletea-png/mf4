package com.mdflib.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.IntByReference;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public interface MdfLibraryNative extends Library {

    MdfLibraryNative INSTANCE = loadLibrary();

    static MdfLibraryNative loadLibrary() {
        try {
            String libName = "mdflibrary";
            if (Platform.isWindows()) {
                String resourceDir = "native/win32-x86-64";
                String[] dlls = {"zlib1.dll", "libexpat.dll", "mdflibrary.dll"};
                Path tempDir = Files.createTempDirectory("mdflib_native");
                tempDir.toFile().deleteOnExit();
                for (String dll : dlls) {
                    InputStream is = MdfLibraryNative.class.getClassLoader().getResourceAsStream(resourceDir + "/" + dll);
                    if (is != null) {
                        Path target = tempDir.resolve(dll);
                        Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                        is.close();
                        target.toFile().deleteOnExit();
                    }
                }
                System.setProperty("jna.library.path", tempDir.toString());
            }
            return Native.load(libName, MdfLibraryNative.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mdflibrary native library", e);
        }
    }

    // MdfReader
    Pointer MdfReaderInit(String filename);
    void MdfReaderUnInit(Pointer reader);
    boolean MdfReaderIsOk(Pointer reader);
    Pointer MdfReaderGetFile(Pointer reader);
    Pointer MdfReaderGetHeader(Pointer reader);
    Pointer MdfReaderGetDataGroup(Pointer reader, long index);
    boolean MdfReaderOpen(Pointer reader);
    void MdfReaderClose(Pointer reader);
    boolean MdfReaderReadHeader(Pointer reader);
    boolean MdfReaderReadMeasurementInfo(Pointer reader);
    boolean MdfReaderReadEverythingButData(Pointer reader);
    boolean MdfReaderReadData(Pointer reader, Pointer group);

    // MdfWriter
    Pointer MdfWriterInit(int type, String filename);
    void MdfWriterUnInit(Pointer writer);
    Pointer MdfWriterGetFile(Pointer writer);
    Pointer MdfWriterGetHeader(Pointer writer);
    boolean MdfWriterInitMeasurement(Pointer writer);
    Pointer MdfWriterCreateDataGroup(Pointer writer);
    void MdfWriterSaveSample(Pointer writer, Pointer group, long time);
    void MdfWriterStartMeasurement(Pointer writer, long start_time);
    void MdfWriterStopMeasurement(Pointer writer, long stop_time);
    boolean MdfWriterFinalizeMeasurement(Pointer writer);
    void MdfWriterSetCompressData(Pointer writer, boolean compress);

    // MdfFile - size_t returns mapped to long
    long MdfFileGetName(Pointer file, byte[] name);
    void MdfFileSetName(Pointer file, String name);
    long MdfFileGetFileName(Pointer file, byte[] filename);
    long MdfFileGetVersion(Pointer file, byte[] version);
    int MdfFileGetMainVersion(Pointer file);
    int MdfFileGetMinorVersion(Pointer file);
    boolean MdfFileGetIsMdf4(Pointer file);
    long MdfFileGetDataGroups(Pointer file, Pointer[] dataGroups);
    Pointer MdfFileCreateDataGroup(Pointer file);

    // MdfHeader
    long MdfHeaderGetAuthor(Pointer header, byte[] author);
    void MdfHeaderSetAuthor(Pointer header, String author);
    long MdfHeaderGetDescription(Pointer header, byte[] desc);
    void MdfHeaderSetDescription(Pointer header, String desc);
    long MdfHeaderGetProject(Pointer header, byte[] project);
    void MdfHeaderSetProject(Pointer header, String project);
    long MdfHeaderGetSubject(Pointer header, byte[] subject);
    void MdfHeaderSetSubject(Pointer header, String subject);
    long MdfHeaderGetDepartment(Pointer header, byte[] department);
    void MdfHeaderSetDepartment(Pointer header, String department);
    long MdfHeaderGetStartTime(Pointer header);
    long MdfHeaderGetDataGroups(Pointer header, Pointer[] dataGroups);
    Pointer MdfHeaderCreateDataGroup(Pointer header);
    Pointer MdfHeaderCreateFileHistory(Pointer header);
    Pointer MdfHeaderGetLastDataGroup(Pointer header);

    // MdfDataGroup
    long MdfDataGroupGetChannelGroups(Pointer group, Pointer[] channelGroups);
    Pointer MdfDataGroupCreateChannelGroup(Pointer group);

    // MdfChannelGroup
    long MdfChannelGroupGetName(Pointer group, byte[] name);
    void MdfChannelGroupSetName(Pointer group, String name);
    long MdfChannelGroupGetNofSamples(Pointer group);
    void MdfChannelGroupSetNofSamples(Pointer group, long samples);
    long MdfChannelGroupGetChannels(Pointer group, Pointer[] channels);
    Pointer MdfChannelGroupCreateChannel(Pointer group);

    // MdfChannel
    long MdfChannelGetName(Pointer channel, byte[] name);
    void MdfChannelSetName(Pointer channel, String name);
    boolean MdfChannelIsUnitUsed(Pointer channel);
    long MdfChannelGetUnit(Pointer channel, byte[] unit);
    void MdfChannelSetUnit(Pointer channel, String unit);
    byte MdfChannelGetType(Pointer channel);
    void MdfChannelSetType(Pointer channel, byte type);
    byte MdfChannelGetSync(Pointer channel);
    void MdfChannelSetSync(Pointer channel, byte syncType);
    byte MdfChannelGetDataType(Pointer channel);
    void MdfChannelSetDataType(Pointer channel, byte dataType);
    int MdfChannelGetBitCount(Pointer channel);
    void MdfChannelSetBitCount(Pointer channel, int bits);
    void MdfChannelSetChannelValueAsSigned(Pointer channel, long value, byte valid, long arrayIndex);
    void MdfChannelSetChannelValueAsUnSigned(Pointer channel, long value, byte valid, long arrayIndex);
    void MdfChannelSetChannelValueAsFloat(Pointer channel, double value, byte valid, long arrayIndex);
    void MdfChannelSetChannelValueAsString(Pointer channel, byte[] value, byte valid, long arrayIndex);

    long MdfDataGroupGetDescription(Pointer dataGroup, byte[] description);

    // MdfChannelObserver
    Pointer MdfChannelObserverCreate(Pointer dataGroup, Pointer channelGroup, Pointer channel);
    Pointer MdfChannelObserverCreateByChannelName(Pointer dataGroup, String channelName);
    void MdfChannelObserverUnInit(Pointer observer);
    long MdfChannelObserverGetNofSamples(Pointer observer);
    long MdfChannelObserverGetName(Pointer observer, byte[] name);
    long MdfChannelObserverGetUnit(Pointer observer, byte[] unit);
    boolean MdfChannelObserverIsMaster(Pointer observer);
    boolean MdfChannelObserverGetChannelValueAsSigned(Pointer observer, long sample, LongByReference value);
    boolean MdfChannelObserverGetChannelValueAsUnSigned(Pointer observer, long sample, LongByReference value);
    boolean MdfChannelObserverGetChannelValueAsFloat(Pointer observer, long sample, DoubleByReference value);

    boolean MdfChannelObserverGetEngValueAsSigned(Pointer observer, long sample, LongByReference value);
    boolean MdfChannelObserverGetEngValueAsUnSigned(Pointer observer, long sample, LongByReference value);
    boolean MdfChannelObserverGetEngValueAsFloat(Pointer observer, long sample, DoubleByReference value);

    // MdfFileHistory
    long MdfFileHistoryGetToolName(Pointer fh, byte[] name);
    void MdfFileHistorySetToolName(Pointer fh, String name);
    long MdfFileHistoryGetDescription(Pointer fh, byte[] desc);
    void MdfFileHistorySetDescription(Pointer fh, String desc);
}
