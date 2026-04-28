package com.huawei.behavior.simulation.datawatch.service.mdflib.jni;

public final class MdfLibraryNativeJNI {

    private static final MdfLibraryNativeJNI INSTANCE = new MdfLibraryNativeJNI();
    private static volatile boolean libraryLoaded = false;
    private static final Object LOAD_LOCK = new Object();

    private MdfLibraryNativeJNI() {
        ensureLibraryLoaded();
    }

    public static MdfLibraryNativeJNI getInstance() {
        return INSTANCE;
    }

    private static void ensureLibraryLoaded() {
        if (libraryLoaded) {
            return;
        }
        synchronized (LOAD_LOCK) {
            if (libraryLoaded) {
                return;
            }
            try {
                loadNativeLibrary();
                libraryLoaded = true;
            } catch (Exception e) {
                throw new RuntimeException(
                    "Failed to load mdflib JNI native library. "
                    + "Ensure the native libraries are available in the classpath.", e);
            }
        }
    }

    private static void loadNativeLibrary() throws Exception {
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = osName.contains("windows");

        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("mdflib_jni");
        tempDir.toFile().deleteOnExit();

        if (isWindows) {
            String resourceDir = "native/win32-x86-64";
            String[] dependencies = {"zlib1.dll", "libexpat.dll", "mdflibrary.dll"};
            for (String dep : dependencies) {
                extractResource(resourceDir + "/" + dep, tempDir.resolve(dep));
            }
            extractResource(resourceDir + "/mdflibjni.dll", tempDir.resolve("mdflibjni.dll"));
            System.load(tempDir.resolve("mdflibjni.dll").toString());
        } else {
            String resourceDir = "native/linux-x86-64";
            String[] dependencies = {"libz.so", "libexpat.so", "libmdflibrary.so"};
            for (String dep : dependencies) {
                extractResource(resourceDir + "/" + dep, tempDir.resolve(dep));
            }
            extractResource(resourceDir + "/libmdflibjni.so", tempDir.resolve("libmdflibjni.so"));
            System.load(tempDir.resolve("libmdflibjni.so").toString());
        }
    }

    private static void extractResource(String resourcePath, java.nio.file.Path target)
            throws java.io.IOException {
        java.io.InputStream is = MdfLibraryNativeJNI.class.getClassLoader()
            .getResourceAsStream(resourcePath);
        if (is != null) {
            try {
                java.nio.file.Files.copy(is, target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                target.toFile().deleteOnExit();
            } finally {
                is.close();
            }
        }
    }

    /* MdfReader */
    public native long MdfReaderInit(String filename);
    public native void MdfReaderUnInit(long reader);
    public native long MdfReaderGetIndex(long reader);
    public native boolean MdfReaderIsOk(long reader);
    public native long MdfReaderGetFile(long reader);
    public native long MdfReaderGetHeader(long reader);
    public native long MdfReaderGetDataGroup(long reader, long index);
    public native boolean MdfReaderOpen(long reader);
    public native void MdfReaderClose(long reader);
    public native boolean MdfReaderReadHeader(long reader);
    public native boolean MdfReaderReadMeasurementInfo(long reader);
    public native boolean MdfReaderReadEverythingButData(long reader);
    public native boolean MdfReaderReadData(long reader, long group);

    /* MdfWriter */
    public native long MdfWriterInit(int type, String filename);
    public native void MdfWriterUnInit(long writer);
    public native long MdfWriterGetFile(long writer);
    public native long MdfWriterGetHeader(long writer);
    public native boolean MdfWriterIsFileNew(long writer);
    public native boolean MdfWriterGetCompressData(long writer);
    public native void MdfWriterSetCompressData(long writer, byte compress);
    public native double MdfWriterGetPreTrigTime(long writer);
    public native void MdfWriterSetPreTrigTime(long writer, double preTrigTime);
    public native long MdfWriterGetStartTime(long writer);
    public native long MdfWriterGetStopTime(long writer);
    public native short MdfWriterGetBusType(long writer);
    public native void MdfWriterSetBusType(long writer, short type);
    public native int MdfWriterGetStorageType(long writer);
    public native void MdfWriterSetStorageType(long writer, int type);
    public native int MdfWriterGetMaxLength(long writer);
    public native void MdfWriterSetMaxLength(long writer, int length);
    public native boolean MdfWriterCreateBusLogConfiguration(long writer);
    public native long MdfWriterCreateDataGroup(long writer);
    public native boolean MdfWriterInitMeasurement(long writer);
    public native void MdfWriterSaveSample(long writer, long group, long time);
    public native void MdfWriterStartMeasurement(long writer, long startTime);
    public native void MdfWriterStopMeasurement(long writer, long stopTime);
    public native boolean MdfWriterFinalizeMeasurement(long writer);

    /* MdfFile */
    public native long MdfFileGetName(long file, byte[] name);
    public native void MdfFileSetName(long file, String name);
    public native long MdfFileGetFileName(long file, byte[] filename);
    public native void MdfFileSetFileName(long file, String filename);
    public native long MdfFileGetVersion(long file, byte[] version);
    public native int MdfFileGetMainVersion(long file);
    public native int MdfFileGetMinorVersion(long file);
    public native void MdfFileSetMinorVersion(long file, int minor);
    public native long MdfFileGetProgramId(long file, byte[] programId);
    public native void MdfFileSetProgramId(long file, String programId);
    public native boolean MdfFileGetIsMdf4(long file);
    public native long MdfFileGetHeader(long file);
    public native long MdfFileGetDataGroups(long file, long[] dataGroups);
    public native long MdfFileGetAttachments(long file, long[] attachments);
    public native long MdfFileCreateAttachment(long file);
    public native long MdfFileCreateDataGroup(long file);

    /* MdfHeader */
    public native long MdfHeaderGetIndex(long header);
    public native long MdfHeaderGetDescription(long header, byte[] desc);
    public native void MdfHeaderSetDescription(long header, String desc);
    public native long MdfHeaderGetAuthor(long header, byte[] author);
    public native void MdfHeaderSetAuthor(long header, String author);
    public native long MdfHeaderGetDepartment(long header, byte[] department);
    public native void MdfHeaderSetDepartment(long header, String department);
    public native long MdfHeaderGetProject(long header, byte[] project);
    public native void MdfHeaderSetProject(long header, String project);
    public native long MdfHeaderGetSubject(long header, byte[] subject);
    public native void MdfHeaderSetSubject(long header, String subject);
    public native long MdfHeaderGetMeasurementId(long header, byte[] uuid);
    public native void MdfHeaderSetMeasurementId(long header, String uuid);
    public native long MdfHeaderGetRecorderId(long header, byte[] uuid);
    public native void MdfHeaderSetRecorderId(long header, String uuid);
    public native long MdfHeaderGetRecorderIndex(long header);
    public native void MdfHeaderSetRecorderIndex(long header, long index);
    public native long MdfHeaderGetStartTime(long header);
    public native void MdfHeaderSetStartTime(long header, long time);
    public native boolean MdfHeaderIsStartAngleUsed(long header);
    public native double MdfHeaderGetStartAngle(long header);
    public native void MdfHeaderSetStartAngle(long header, double angle);
    public native boolean MdfHeaderIsStartDistanceUsed(long header);
    public native double MdfHeaderGetStartDistance(long header);
    public native void MdfHeaderSetStartDistance(long header, double distance);
    public native long MdfHeaderGetMetaData(long header);
    public native long MdfHeaderGetAttachments(long header, long[] attachments);
    public native long MdfHeaderGetFileHistorys(long header, long[] fileHistorys);
    public native long MdfHeaderGetEvents(long header, long[] events);
    public native long MdfHeaderGetDataGroups(long header, long[] dataGroups);
    public native long MdfHeaderGetLastDataGroup(long header);
    public native long MdfHeaderCreateMetaData(long header);
    public native long MdfHeaderCreateAttachment(long header);
    public native long MdfHeaderCreateFileHistory(long header);
    public native long MdfHeaderCreateEvent(long header);
    public native long MdfHeaderCreateDataGroup(long header);

    /* MdfDataGroup */
    public native long MdfDataGroupGetIndex(long group);
    public native long MdfDataGroupGetDescription(long group, byte[] description);
    public native byte MdfDataGroupGetRecordIdSize(long group);
    public native long MdfDataGroupGetMetaData(long group);
    public native long MdfDataGroupGetChannelGroupByName(long group, String name);
    public native long MdfDataGroupGetChannelGroupByRecordId(long group, long recordId);
    public native long MdfDataGroupGetChannelGroups(long group, long[] channelGroups);
    public native boolean MdfDataGroupIsRead(long group);
    public native long MdfDataGroupCreateMetaData(long group);
    public native long MdfDataGroupCreateChannelGroup(long group);
    public native long MdfDataGroupFindParentChannelGroup(long group, long channel);

    /* MdfChannelGroup */
    public native long MdfChannelGroupGetIndex(long group);
    public native long MdfChannelGroupGetRecordId(long group);
    public native long MdfChannelGroupGetName(long group, byte[] name);
    public native void MdfChannelGroupSetName(long group, String name);
    public native long MdfChannelGroupGetDescription(long group, byte[] desc);
    public native void MdfChannelGroupSetDescription(long group, String desc);
    public native long MdfChannelGroupGetNofSamples(long group);
    public native void MdfChannelGroupSetNofSamples(long group, long samples);
    public native short MdfChannelGroupGetFlags(long group);
    public native void MdfChannelGroupSetFlags(long group, short flags);
    public native long MdfChannelGroupGetChannels(long group, long[] channels);
    public native long MdfChannelGroupGetMetaData(long group);
    public native long MdfChannelGroupCreateMetaData(long group);
    public native long MdfChannelGroupCreateChannel(long group);
    public native long MdfChannelGroupCreateSourceInformation(long group);

    /* MdfChannel */
    public native long MdfChannelGetIndex(long channel);
    public native long MdfChannelGetName(long channel, byte[] name);
    public native void MdfChannelSetName(long channel, String name);
    public native long MdfChannelGetDisplayName(long channel, byte[] name);
    public native void MdfChannelSetDisplayName(long channel, String name);
    public native long MdfChannelGetDescription(long channel, byte[] desc);
    public native void MdfChannelSetDescription(long channel, String desc);
    public native boolean MdfChannelIsUnitUsed(long channel);
    public native long MdfChannelGetUnit(long channel, byte[] unit);
    public native void MdfChannelSetUnit(long channel, String unit);
    public native byte MdfChannelGetType(long channel);
    public native void MdfChannelSetType(long channel, byte type);
    public native byte MdfChannelGetSync(long channel);
    public native void MdfChannelSetSync(long channel, byte syncType);
    public native byte MdfChannelGetDataType(long channel);
    public native void MdfChannelSetDataType(long channel, byte dataType);
    public native int MdfChannelGetFlags(long channel);
    public native void MdfChannelSetFlags(long channel, int flags);
    public native long MdfChannelGetDataBytes(long channel);
    public native void MdfChannelSetDataBytes(long channel, long bytes);
    public native boolean MdfChannelIsPrecisionUsed(long channel);
    public native byte MdfChannelGetPrecision(long channel);
    public native boolean MdfChannelIsRangeUsed(long channel);
    public native double MdfChannelGetRangeMin(long channel);
    public native double MdfChannelGetRangeMax(long channel);
    public native void MdfChannelSetRange(long channel, double min, double max);
    public native boolean MdfChannelIsLimitUsed(long channel);
    public native double MdfChannelGetLimitMin(long channel);
    public native double MdfChannelGetLimitMax(long channel);
    public native void MdfChannelSetLimit(long channel, double min, double max);
    public native boolean MdfChannelIsExtLimitUsed(long channel);
    public native double MdfChannelGetExtLimitMin(long channel);
    public native double MdfChannelGetExtLimitMax(long channel);
    public native void MdfChannelSetExtLimit(long channel, double min, double max);
    public native double MdfChannelGetSamplingRate(long channel);
    public native long MdfChannelGetVlsdRecordId(long channel);
    public native void MdfChannelSetVlsdRecordId(long channel, long recordId);
    public native int MdfChannelGetBitCount(long channel);
    public native void MdfChannelSetBitCount(long channel, int bits);
    public native short MdfChannelGetBitOffset(long channel);
    public native void MdfChannelSetBitOffset(long channel, short bits);
    public native long MdfChannelGetMetaData(long channel);
    public native long MdfChannelGetSourceInformation(long channel);
    public native long MdfChannelGetChannelConversion(long channel);
    public native long MdfChannelGetChannelArray(long channel);
    public native long MdfChannelGetChannelCompositions(long channel, long[] channels);
    public native long MdfChannelCreateSourceInformation(long channel);
    public native long MdfChannelCreateChannelConversion(long channel);
    public native long MdfChannelCreateChannelArray(long channel);
    public native long MdfChannelCreateChannelComposition(long channel);
    public native long MdfChannelCreateMetaData(long channel);
    public native void MdfChannelSetChannelValueAsSigned(long channel, long value,
            int valid, long arrayIndex);
    public native void MdfChannelSetChannelValueAsUnSigned(long channel, long value,
            int valid, long arrayIndex);
    public native void MdfChannelSetChannelValueAsFloat(long channel, double value,
            int valid, long arrayIndex);
    public native void MdfChannelSetChannelValueAsString(long channel, byte[] value,
            int valid, long arrayIndex);
    public native void MdfChannelSetChannelValueAsArray(long channel, byte[] value,
            int valid, long arrayIndex);

    /* MdfChannelObserver */
    public native long MdfChannelObserverCreate(long dataGroup, long channelGroup,
            long channel);
    public native long MdfChannelObserverCreateByChannelName(long dataGroup,
            String channelName);
    public native long MdfChannelObserverCreateForChannelGroup(long dataGroup,
            long channelGroup, long[] observers);
    public native void MdfChannelObserverUnInit(long observer);
    public native long MdfChannelObserverGetNofSamples(long observer);
    public native long MdfChannelObserverGetName(long observer, byte[] name);
    public native long MdfChannelObserverGetUnit(long observer, byte[] unit);
    public native long MdfChannelObserverGetChannel(long observer);
    public native boolean MdfChannelObserverIsMaster(long observer);
    public native boolean MdfChannelObserverGetChannelValueAsSigned(long observer,
            long sample, long[] value);
    public native boolean MdfChannelObserverGetChannelValueAsUnSigned(long observer,
            long sample, long[] value);
    public native boolean MdfChannelObserverGetChannelValueAsFloat(long observer,
            long sample, double[] value);
    public native boolean MdfChannelObserverGetChannelValueAsString(long observer,
            long sample, byte[] value, long[] size);
    public native boolean MdfChannelObserverGetChannelValueAsArray(long observer,
            long sample, byte[] value, long[] size);
    public native boolean MdfChannelObserverGetEngValueAsSigned(long observer,
            long sample, long[] value);
    public native boolean MdfChannelObserverGetEngValueAsUnSigned(long observer,
            long sample, long[] value);
    public native boolean MdfChannelObserverGetEngValueAsFloat(long observer,
            long sample, double[] value);
    public native boolean MdfChannelObserverGetEngValueAsString(long observer,
            long sample, byte[] value, long[] size);
    public native boolean MdfChannelObserverGetEngValueAsArray(long observer,
            long sample, byte[] value, long[] size);

    /* MdfFileHistory */
    public native long MdfFileHistoryGetIndex(long fh);
    public native long MdfFileHistoryGetTime(long fh);
    public native void MdfFileHistorySetTime(long fh, long time);
    public native long MdfFileHistoryGetMetaData(long fh);
    public native long MdfFileHistoryGetDescription(long fh, byte[] desc);
    public native void MdfFileHistorySetDescription(long fh, String desc);
    public native long MdfFileHistoryGetToolName(long fh, byte[] name);
    public native void MdfFileHistorySetToolName(long fh, String name);
    public native long MdfFileHistoryGetToolVendor(long fh, byte[] vendor);
    public native void MdfFileHistorySetToolVendor(long fh, String vendor);
    public native long MdfFileHistoryGetToolVersion(long fh, byte[] version);
    public native void MdfFileHistorySetToolVersion(long fh, String version);
    public native long MdfFileHistoryGetUserName(long fh, byte[] user);
    public native void MdfFileHistorySetUserName(long fh, String user);
}
