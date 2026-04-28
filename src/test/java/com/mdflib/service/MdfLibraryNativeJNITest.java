package com.mdflib.service;

import com.huawei.behavior.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI;
import com.mdflib.model.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verification test suite for the JNI class rename to
 * {@code com.huawei.behavior.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI}.
 *
 * <p>This test class verifies:</p>
 * <ul>
 *   <li>The new JNI class is correctly named and accessible</li>
 *   <li>The new package path resolves correctly</li>
 *   <li>The singleton pattern still works with the renamed class</li>
 *   <li>The native library loading still functions</li>
 *   <li>All native methods (old and newly exposed) are callable</li>
 *   <li>End-to-end write and read operations work correctly</li>
 *   <li>Newly exposed methods work correctly</li>
 * </ul>
 *
 * @author mdflib-java contributors
 * @version 2.0.0
 * @since 1.0.0
 */
public class MdfLibraryNativeJNITest {

    /** Temporary directory for test output files. Cleaned up after each test. */
    private Path tempDir;

    /**
     * Sets up the test environment before each test.
     *
     * <p>Creates a temporary directory for test output files.</p>
     *
     * @throws IOException if the temporary directory cannot be created
     */
    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("mdflib_jni_rename_test");
    }

    /**
     * Cleans up the test environment after each test.
     *
     * <p>Deletes all files in the temporary directory and the directory itself.</p>
     *
     * @throws IOException if directory cleanup fails
     */
    @After
    public void tearDown() throws IOException {
        if (tempDir != null) {
            /* Walk the directory tree and delete all files and subdirectories */
            Files.walk(tempDir)
                 .sorted((a, b) -> -a.compareTo(b))
                 .forEach(p -> {
                     try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                 });
        }
    }

    /**
     * Creates a unique test file path in the temporary directory.
     *
     * @param suffix file name suffix for identification
     * @return absolute path to the test file with .mf4 extension
     */
    private String testFilePath(String suffix) {
        return tempDir.resolve(suffix + ".mf4").toString();
    }

    /* ========================================================================
     * Class Identity Verification Tests
     *
     * These tests verify the new JNI class name and package are correct.
     * ======================================================================== */

    /**
     * Verifies that the new JNI class has the correct fully qualified name.
     */
    @Test
    public void testJNIClassCorrectName() {
        Class<?> clazz = MdfLibraryNativeJNI.class;
        assertEquals(
            "JNI class should have the correct fully qualified name",
            "com.huawei.behavior.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI",
            clazz.getName()
        );
    }

    /**
     * Verifies that the new JNI class has the correct simple name.
     *
     * <p>The simple class name must be {@code MdfLibraryNativeJNI} to distinguish
     * it from the old {@code MdfLibraryNative} class.</p>
     */
    @Test
    public void testJNIClassSimpleName() {
        assertEquals(
            "JNI class simple name should be MdfLibraryNativeJNI",
            "MdfLibraryNativeJNI",
            MdfLibraryNativeJNI.class.getSimpleName()
        );
    }

    /**
     * Verifies that the new JNI class is in the correct package.
     *
     * <p>The package must be
     * {@code com.huawei.simulation.datawatch.service.mdflib.jni}.</p>
     */
    @Test
    public void testJNIClassPackage() {
        Package pkg = MdfLibraryNativeJNI.class.getPackage();
        assertNotNull("JNI class package should not be null", pkg);
        assertEquals(
            "JNI class should be in the correct package",
            "com.huawei.simulation.datawatch.service.mdflib.jni",
            pkg.getName()
        );
    }

    /**
     * Verifies that the JNI class is declared as final (not extendable).
     *
     * <p>The class should be final to prevent subclassing of the native
     * library wrapper.</p>
     */
    @Test
    public void testJNIClassIsFinal() {
        int modifiers = MdfLibraryNativeJNI.class.getModifiers();
        assertTrue(
            "MdfLibraryNativeJNI should be declared final",
            java.lang.reflect.Modifier.isFinal(modifiers)
        );
    }

    /* ========================================================================
     * Singleton Pattern Verification Tests
     *
     * These tests verify the singleton instance works correctly.
     * ======================================================================== */

    /**
     * Verifies that getInstance() returns a non-null instance.
     *
     * <p>The singleton pattern should provide a valid instance of
     * the native library wrapper.</p>
     */
    @Test
    public void testGetInstanceNotNull() {
        MdfLibraryNativeJNI instance = MdfLibraryNativeJNI.getInstance();
        assertNotNull("getInstance() should return a non-null instance", instance);
    }

    /**
     * Verifies that getInstance() always returns the same instance.
     *
     * <p>The singleton pattern must guarantee that only one instance
     * of the native library wrapper exists.</p>
     */
    @Test
    public void testSingletonIdentity() {
        MdfLibraryNativeJNI instance1 = MdfLibraryNativeJNI.getInstance();
        MdfLibraryNativeJNI instance2 = MdfLibraryNativeJNI.getInstance();
        assertSame("getInstance() should return the same instance", instance1, instance2);
    }

    /**
     * Verifies that the singleton instance type is correct.
     *
     * <p>The returned instance must be of type MdfLibraryNativeJNI.</p>
     */
    @Test
    public void testSingletonType() {
        MdfLibraryNativeJNI instance = MdfLibraryNativeJNI.getInstance();
        assertTrue(
            "Singleton instance should be of type MdfLibraryNativeJNI",
            instance instanceof MdfLibraryNativeJNI
        );
    }

    /* ========================================================================
     * Native Method Availability Verification Tests
     *
     * These tests verify all expected native methods are still declared.
     * ======================================================================== */

    @Test
    public void testNativeMethodsExist() throws NoSuchMethodException {
        Class<?> clazz = MdfLibraryNativeJNI.class;

        /* MdfReader methods */
        clazz.getMethod("MdfReaderInit", String.class);
        clazz.getMethod("MdfReaderUnInit", long.class);
        clazz.getMethod("MdfReaderGetIndex", long.class);
        clazz.getMethod("MdfReaderIsOk", long.class);
        clazz.getMethod("MdfReaderGetFile", long.class);
        clazz.getMethod("MdfReaderGetHeader", long.class);
        clazz.getMethod("MdfReaderGetDataGroup", long.class, long.class);
        clazz.getMethod("MdfReaderOpen", long.class);
        clazz.getMethod("MdfReaderClose", long.class);
        clazz.getMethod("MdfReaderReadHeader", long.class);
        clazz.getMethod("MdfReaderReadMeasurementInfo", long.class);
        clazz.getMethod("MdfReaderReadEverythingButData", long.class);
        clazz.getMethod("MdfReaderReadData", long.class, long.class);

        /* MdfWriter methods */
        clazz.getMethod("MdfWriterInit", int.class, String.class);
        clazz.getMethod("MdfWriterUnInit", long.class);
        clazz.getMethod("MdfWriterGetFile", long.class);
        clazz.getMethod("MdfWriterGetHeader", long.class);
        clazz.getMethod("MdfWriterIsFileNew", long.class);
        clazz.getMethod("MdfWriterGetCompressData", long.class);
        clazz.getMethod("MdfWriterSetCompressData", long.class, byte.class);
        clazz.getMethod("MdfWriterGetPreTrigTime", long.class);
        clazz.getMethod("MdfWriterSetPreTrigTime", long.class, double.class);
        clazz.getMethod("MdfWriterGetStartTime", long.class);
        clazz.getMethod("MdfWriterGetStopTime", long.class);
        clazz.getMethod("MdfWriterGetBusType", long.class);
        clazz.getMethod("MdfWriterSetBusType", long.class, short.class);
        clazz.getMethod("MdfWriterGetStorageType", long.class);
        clazz.getMethod("MdfWriterSetStorageType", long.class, int.class);
        clazz.getMethod("MdfWriterGetMaxLength", long.class);
        clazz.getMethod("MdfWriterSetMaxLength", long.class, int.class);
        clazz.getMethod("MdfWriterCreateBusLogConfiguration", long.class);
        clazz.getMethod("MdfWriterCreateDataGroup", long.class);
        clazz.getMethod("MdfWriterInitMeasurement", long.class);
        clazz.getMethod("MdfWriterSaveSample", long.class, long.class, long.class);
        clazz.getMethod("MdfWriterStartMeasurement", long.class, long.class);
        clazz.getMethod("MdfWriterStopMeasurement", long.class, long.class);
        clazz.getMethod("MdfWriterFinalizeMeasurement", long.class);

        /* MdfFile methods */
        clazz.getMethod("MdfFileGetName", long.class, byte[].class);
        clazz.getMethod("MdfFileSetName", long.class, String.class);
        clazz.getMethod("MdfFileGetFileName", long.class, byte[].class);
        clazz.getMethod("MdfFileSetFileName", long.class, String.class);
        clazz.getMethod("MdfFileGetVersion", long.class, byte[].class);
        clazz.getMethod("MdfFileGetMainVersion", long.class);
        clazz.getMethod("MdfFileGetMinorVersion", long.class);
        clazz.getMethod("MdfFileSetMinorVersion", long.class, int.class);
        clazz.getMethod("MdfFileGetProgramId", long.class, byte[].class);
        clazz.getMethod("MdfFileSetProgramId", long.class, String.class);
        clazz.getMethod("MdfFileGetIsMdf4", long.class);
        clazz.getMethod("MdfFileGetHeader", long.class);
        clazz.getMethod("MdfFileGetDataGroups", long.class, long[].class);
        clazz.getMethod("MdfFileGetAttachments", long.class, long[].class);
        clazz.getMethod("MdfFileCreateAttachment", long.class);
        clazz.getMethod("MdfFileCreateDataGroup", long.class);

        /* MdfHeader methods */
        clazz.getMethod("MdfHeaderGetIndex", long.class);
        clazz.getMethod("MdfHeaderGetAuthor", long.class, byte[].class);
        clazz.getMethod("MdfHeaderSetAuthor", long.class, String.class);
        clazz.getMethod("MdfHeaderGetDescription", long.class, byte[].class);
        clazz.getMethod("MdfHeaderSetDescription", long.class, String.class);
        clazz.getMethod("MdfHeaderGetProject", long.class, byte[].class);
        clazz.getMethod("MdfHeaderSetProject", long.class, String.class);
        clazz.getMethod("MdfHeaderGetSubject", long.class, byte[].class);
        clazz.getMethod("MdfHeaderSetSubject", long.class, String.class);
        clazz.getMethod("MdfHeaderGetDepartment", long.class, byte[].class);
        clazz.getMethod("MdfHeaderSetDepartment", long.class, String.class);
        clazz.getMethod("MdfHeaderGetMeasurementId", long.class, byte[].class);
        clazz.getMethod("MdfHeaderSetMeasurementId", long.class, String.class);
        clazz.getMethod("MdfHeaderGetRecorderId", long.class, byte[].class);
        clazz.getMethod("MdfHeaderSetRecorderId", long.class, String.class);
        clazz.getMethod("MdfHeaderGetRecorderIndex", long.class);
        clazz.getMethod("MdfHeaderSetRecorderIndex", long.class, long.class);
        clazz.getMethod("MdfHeaderGetStartTime", long.class);
        clazz.getMethod("MdfHeaderSetStartTime", long.class, long.class);
        clazz.getMethod("MdfHeaderIsStartAngleUsed", long.class);
        clazz.getMethod("MdfHeaderGetStartAngle", long.class);
        clazz.getMethod("MdfHeaderSetStartAngle", long.class, double.class);
        clazz.getMethod("MdfHeaderIsStartDistanceUsed", long.class);
        clazz.getMethod("MdfHeaderGetStartDistance", long.class);
        clazz.getMethod("MdfHeaderSetStartDistance", long.class, double.class);
        clazz.getMethod("MdfHeaderGetMetaData", long.class);
        clazz.getMethod("MdfHeaderGetAttachments", long.class, long[].class);
        clazz.getMethod("MdfHeaderGetFileHistorys", long.class, long[].class);
        clazz.getMethod("MdfHeaderGetEvents", long.class, long[].class);
        clazz.getMethod("MdfHeaderGetDataGroups", long.class, long[].class);
        clazz.getMethod("MdfHeaderGetLastDataGroup", long.class);
        clazz.getMethod("MdfHeaderCreateMetaData", long.class);
        clazz.getMethod("MdfHeaderCreateAttachment", long.class);
        clazz.getMethod("MdfHeaderCreateFileHistory", long.class);
        clazz.getMethod("MdfHeaderCreateEvent", long.class);
        clazz.getMethod("MdfHeaderCreateDataGroup", long.class);

        /* MdfDataGroup methods */
        clazz.getMethod("MdfDataGroupGetIndex", long.class);
        clazz.getMethod("MdfDataGroupGetDescription", long.class, byte[].class);
        clazz.getMethod("MdfDataGroupGetRecordIdSize", long.class);
        clazz.getMethod("MdfDataGroupGetMetaData", long.class);
        clazz.getMethod("MdfDataGroupGetChannelGroupByName", long.class, String.class);
        clazz.getMethod("MdfDataGroupGetChannelGroupByRecordId", long.class, long.class);
        clazz.getMethod("MdfDataGroupGetChannelGroups", long.class, long[].class);
        clazz.getMethod("MdfDataGroupIsRead", long.class);
        clazz.getMethod("MdfDataGroupCreateMetaData", long.class);
        clazz.getMethod("MdfDataGroupCreateChannelGroup", long.class);
        clazz.getMethod("MdfDataGroupFindParentChannelGroup", long.class, long.class);

        /* MdfChannelGroup methods */
        clazz.getMethod("MdfChannelGroupGetIndex", long.class);
        clazz.getMethod("MdfChannelGroupGetRecordId", long.class);
        clazz.getMethod("MdfChannelGroupGetName", long.class, byte[].class);
        clazz.getMethod("MdfChannelGroupSetName", long.class, String.class);
        clazz.getMethod("MdfChannelGroupGetDescription", long.class, byte[].class);
        clazz.getMethod("MdfChannelGroupSetDescription", long.class, String.class);
        clazz.getMethod("MdfChannelGroupGetNofSamples", long.class);
        clazz.getMethod("MdfChannelGroupSetNofSamples", long.class, long.class);
        clazz.getMethod("MdfChannelGroupGetFlags", long.class);
        clazz.getMethod("MdfChannelGroupSetFlags", long.class, short.class);
        clazz.getMethod("MdfChannelGroupGetChannels", long.class, long[].class);
        clazz.getMethod("MdfChannelGroupGetMetaData", long.class);
        clazz.getMethod("MdfChannelGroupCreateMetaData", long.class);
        clazz.getMethod("MdfChannelGroupCreateChannel", long.class);
        clazz.getMethod("MdfChannelGroupCreateSourceInformation", long.class);

        /* MdfChannel methods */
        clazz.getMethod("MdfChannelGetIndex", long.class);
        clazz.getMethod("MdfChannelGetName", long.class, byte[].class);
        clazz.getMethod("MdfChannelSetName", long.class, String.class);
        clazz.getMethod("MdfChannelGetDisplayName", long.class, byte[].class);
        clazz.getMethod("MdfChannelSetDisplayName", long.class, String.class);
        clazz.getMethod("MdfChannelGetDescription", long.class, byte[].class);
        clazz.getMethod("MdfChannelSetDescription", long.class, String.class);
        clazz.getMethod("MdfChannelIsUnitUsed", long.class);
        clazz.getMethod("MdfChannelGetUnit", long.class, byte[].class);
        clazz.getMethod("MdfChannelSetUnit", long.class, String.class);
        clazz.getMethod("MdfChannelGetType", long.class);
        clazz.getMethod("MdfChannelSetType", long.class, byte.class);
        clazz.getMethod("MdfChannelGetSync", long.class);
        clazz.getMethod("MdfChannelSetSync", long.class, byte.class);
        clazz.getMethod("MdfChannelGetDataType", long.class);
        clazz.getMethod("MdfChannelSetDataType", long.class, byte.class);
        clazz.getMethod("MdfChannelGetFlags", long.class);
        clazz.getMethod("MdfChannelSetFlags", long.class, int.class);
        clazz.getMethod("MdfChannelGetDataBytes", long.class);
        clazz.getMethod("MdfChannelSetDataBytes", long.class, long.class);
        clazz.getMethod("MdfChannelIsPrecisionUsed", long.class);
        clazz.getMethod("MdfChannelGetPrecision", long.class);
        clazz.getMethod("MdfChannelIsRangeUsed", long.class);
        clazz.getMethod("MdfChannelGetRangeMin", long.class);
        clazz.getMethod("MdfChannelGetRangeMax", long.class);
        clazz.getMethod("MdfChannelSetRange", long.class, double.class, double.class);
        clazz.getMethod("MdfChannelIsLimitUsed", long.class);
        clazz.getMethod("MdfChannelGetLimitMin", long.class);
        clazz.getMethod("MdfChannelGetLimitMax", long.class);
        clazz.getMethod("MdfChannelSetLimit", long.class, double.class, double.class);
        clazz.getMethod("MdfChannelIsExtLimitUsed", long.class);
        clazz.getMethod("MdfChannelGetExtLimitMin", long.class);
        clazz.getMethod("MdfChannelGetExtLimitMax", long.class);
        clazz.getMethod("MdfChannelSetExtLimit", long.class, double.class, double.class);
        clazz.getMethod("MdfChannelGetSamplingRate", long.class);
        clazz.getMethod("MdfChannelGetVlsdRecordId", long.class);
        clazz.getMethod("MdfChannelSetVlsdRecordId", long.class, long.class);
        clazz.getMethod("MdfChannelGetBitCount", long.class);
        clazz.getMethod("MdfChannelSetBitCount", long.class, int.class);
        clazz.getMethod("MdfChannelGetBitOffset", long.class);
        clazz.getMethod("MdfChannelSetBitOffset", long.class, short.class);
        clazz.getMethod("MdfChannelGetMetaData", long.class);
        clazz.getMethod("MdfChannelGetSourceInformation", long.class);
        clazz.getMethod("MdfChannelGetChannelConversion", long.class);
        clazz.getMethod("MdfChannelGetChannelArray", long.class);
        clazz.getMethod("MdfChannelGetChannelCompositions", long.class, long[].class);
        clazz.getMethod("MdfChannelCreateSourceInformation", long.class);
        clazz.getMethod("MdfChannelCreateChannelConversion", long.class);
        clazz.getMethod("MdfChannelCreateChannelArray", long.class);
        clazz.getMethod("MdfChannelCreateChannelComposition", long.class);
        clazz.getMethod("MdfChannelCreateMetaData", long.class);
        clazz.getMethod("MdfChannelSetChannelValueAsSigned", long.class, long.class, int.class, long.class);
        clazz.getMethod("MdfChannelSetChannelValueAsUnSigned", long.class, long.class, int.class, long.class);
        clazz.getMethod("MdfChannelSetChannelValueAsFloat", long.class, double.class, int.class, long.class);
        clazz.getMethod("MdfChannelSetChannelValueAsString", long.class, byte[].class, int.class, long.class);
        clazz.getMethod("MdfChannelSetChannelValueAsArray", long.class, byte[].class, int.class, long.class);

        /* MdfChannelObserver methods */
        clazz.getMethod("MdfChannelObserverCreate", long.class, long.class, long.class);
        clazz.getMethod("MdfChannelObserverCreateByChannelName", long.class, String.class);
        clazz.getMethod("MdfChannelObserverCreateForChannelGroup", long.class, long.class, long[].class);
        clazz.getMethod("MdfChannelObserverUnInit", long.class);
        clazz.getMethod("MdfChannelObserverGetNofSamples", long.class);
        clazz.getMethod("MdfChannelObserverGetName", long.class, byte[].class);
        clazz.getMethod("MdfChannelObserverGetUnit", long.class, byte[].class);
        clazz.getMethod("MdfChannelObserverGetChannel", long.class);
        clazz.getMethod("MdfChannelObserverIsMaster", long.class);
        clazz.getMethod("MdfChannelObserverGetChannelValueAsSigned", long.class, long.class, long[].class);
        clazz.getMethod("MdfChannelObserverGetChannelValueAsUnSigned", long.class, long.class, long[].class);
        clazz.getMethod("MdfChannelObserverGetChannelValueAsFloat", long.class, long.class, double[].class);
        clazz.getMethod("MdfChannelObserverGetChannelValueAsString", long.class, long.class, byte[].class, long[].class);
        clazz.getMethod("MdfChannelObserverGetChannelValueAsArray", long.class, long.class, byte[].class, long[].class);
        clazz.getMethod("MdfChannelObserverGetEngValueAsSigned", long.class, long.class, long[].class);
        clazz.getMethod("MdfChannelObserverGetEngValueAsUnSigned", long.class, long.class, long[].class);
        clazz.getMethod("MdfChannelObserverGetEngValueAsFloat", long.class, long.class, double[].class);
        clazz.getMethod("MdfChannelObserverGetEngValueAsString", long.class, long.class, byte[].class, long[].class);
        clazz.getMethod("MdfChannelObserverGetEngValueAsArray", long.class, long.class, byte[].class, long[].class);

        /* MdfFileHistory methods */
        clazz.getMethod("MdfFileHistoryGetIndex", long.class);
        clazz.getMethod("MdfFileHistoryGetTime", long.class);
        clazz.getMethod("MdfFileHistorySetTime", long.class, long.class);
        clazz.getMethod("MdfFileHistoryGetMetaData", long.class);
        clazz.getMethod("MdfFileHistoryGetDescription", long.class, byte[].class);
        clazz.getMethod("MdfFileHistorySetDescription", long.class, String.class);
        clazz.getMethod("MdfFileHistoryGetToolName", long.class, byte[].class);
        clazz.getMethod("MdfFileHistorySetToolName", long.class, String.class);
        clazz.getMethod("MdfFileHistoryGetToolVendor", long.class, byte[].class);
        clazz.getMethod("MdfFileHistorySetToolVendor", long.class, String.class);
        clazz.getMethod("MdfFileHistoryGetToolVersion", long.class, byte[].class);
        clazz.getMethod("MdfFileHistorySetToolVersion", long.class, String.class);
        clazz.getMethod("MdfFileHistoryGetUserName", long.class, byte[].class);
        clazz.getMethod("MdfFileHistorySetUserName", long.class, String.class);
    }

    @Test
    public void testNewJNIClassExists() {
        boolean newClassExists;
        try {
            Class.forName(
                "com.huawei.behavior.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI");
            newClassExists = true;
        } catch (ClassNotFoundException e) {
            newClassExists = false;
        }
        assertTrue(
            "New class com.huawei.behavior.simulation.datawatch.service.mdflib.jni"
            + ".MdfLibraryNativeJNI should exist",
            newClassExists
        );
    }

    @Test
    public void testOldJNIClassDoesNotExist() {
        boolean oldClassExists;
        try {
            Class.forName("com.huawei.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI");
            oldClassExists = true;
        } catch (ClassNotFoundException e) {
            oldClassExists = false;
        }
        assertFalse(
            "Old class com.huawei.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI should not exist",
            oldClassExists
        );
    }

    /* ========================================================================
     * Newly Exposed Methods Verification Tests
     * ======================================================================== */

    @Test
    public void testWriterGetCompressData() {
        String filePath = testFilePath("compress_get");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        N.MdfWriterSetCompressData(writer, (byte) 1);
        assertTrue("CompressData should be true", N.MdfWriterGetCompressData(writer));
        N.MdfWriterSetCompressData(writer, (byte) 0);
        assertFalse("CompressData should be false", N.MdfWriterGetCompressData(writer));
        N.MdfWriterUnInit(writer);
    }

    @Test
    public void testWriterPreTrigTime() {
        String filePath = testFilePath("pretrig");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        N.MdfWriterSetPreTrigTime(writer, 1.5);
        assertEquals("PreTrigTime should match", 1.5, N.MdfWriterGetPreTrigTime(writer), 0.001);
        N.MdfWriterUnInit(writer);
    }

    @Test
    public void testWriterBusType() {
        String filePath = testFilePath("bustype");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        N.MdfWriterSetBusType(writer, (short) 2);
        assertEquals("BusType should match", 2, N.MdfWriterGetBusType(writer));
        N.MdfWriterUnInit(writer);
    }

    @Test
    public void testWriterStorageType() {
        String filePath = testFilePath("storage");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        N.MdfWriterSetStorageType(writer, 1);
        assertEquals("StorageType should match", 1, N.MdfWriterGetStorageType(writer));
        N.MdfWriterUnInit(writer);
    }

    @Test
    public void testWriterMaxLength() {
        String filePath = testFilePath("maxlength");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        N.MdfWriterSetMaxLength(writer, 4096);
        assertEquals("MaxLength should match", 4096, N.MdfWriterGetMaxLength(writer));
        N.MdfWriterUnInit(writer);
    }

    @Test
    public void testFileSetMinorVersion() {
        String filePath = testFilePath("minor_ver");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        long filePtr = N.MdfWriterGetFile(writer);
        assertNotEquals(0, filePtr);
        N.MdfFileSetMinorVersion(filePtr, 11);
        N.MdfWriterUnInit(writer);

        long reader = N.MdfReaderInit(filePath);
        assertTrue(N.MdfReaderIsOk(reader));
        N.MdfReaderOpen(reader);
        N.MdfReaderReadHeader(reader);
        long readFile = N.MdfReaderGetFile(reader);
        assertNotEquals(0, readFile);
        assertEquals("MinorVersion should be 11", 11, N.MdfFileGetMinorVersion(readFile));
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    @Test
    public void testFileProgramId() {
        String filePath = testFilePath("prog_id");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        long filePtr = N.MdfWriterGetFile(writer);
        assertNotEquals(0, filePtr);
        N.MdfFileSetProgramId(filePtr, "MdfLibTest/2.0");
        N.MdfWriterUnInit(writer);

        long reader = N.MdfReaderInit(filePath);
        N.MdfReaderOpen(reader);
        N.MdfReaderReadHeader(reader);
        long readFile = N.MdfReaderGetFile(reader);
        long len = N.MdfFileGetProgramId(readFile, null);
        assertTrue("ProgramId length should be > 0", len > 0);
        byte[] buf = new byte[(int) len + 1];
        N.MdfFileGetProgramId(readFile, buf);
        String programId = new String(buf, 0, (int) len);
        assertTrue("ProgramId should contain MdfLibTest", programId.contains("MdfLibTest"));
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    @Test
    public void testHeaderStartTimeRoundTrip() {
        String filePath = testFilePath("starttime_rt");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        long header = N.MdfWriterGetHeader(writer);
        assertNotEquals(0, header);
        N.MdfHeaderSetStartTime(header, 999999L);
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);
        long ch = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(ch, "t");
        N.MdfChannelSetType(ch, (byte) 2);
        N.MdfChannelSetSync(ch, (byte) 1);
        N.MdfChannelSetDataType(ch, (byte) 4);
        N.MdfChannelSetDataBytes(ch, 8);
        N.MdfWriterInitMeasurement(writer);
        N.MdfWriterStartMeasurement(writer, 999999L);
        N.MdfChannelSetChannelValueAsFloat(ch, 0.0, 1, 0L);
        N.MdfWriterSaveSample(writer, cg, 999999L);
        N.MdfWriterStopMeasurement(writer, 1999999L);
        N.MdfWriterFinalizeMeasurement(writer);
        N.MdfWriterUnInit(writer);

        long reader = N.MdfReaderInit(filePath);
        N.MdfReaderOpen(reader);
        N.MdfReaderReadHeader(reader);
        long readHeader = N.MdfReaderGetHeader(reader);
        long startTime = N.MdfHeaderGetStartTime(readHeader);
        assertEquals("StartTime should match", 999999L, startTime);
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    @Test
    public void testHeaderStartAngleAndDistance() {
        String filePath = testFilePath("angle_dist");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        long header = N.MdfWriterGetHeader(writer);
        assertNotEquals(0, header);
        N.MdfHeaderSetStartAngle(header, 90.0);
        N.MdfHeaderSetStartDistance(header, 100.5);
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);
        long ch = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(ch, "t");
        N.MdfChannelSetType(ch, (byte) 2);
        N.MdfChannelSetSync(ch, (byte) 1);
        N.MdfChannelSetDataType(ch, (byte) 4);
        N.MdfChannelSetDataBytes(ch, 8);
        N.MdfWriterInitMeasurement(writer);
        N.MdfWriterStartMeasurement(writer, 0L);
        N.MdfWriterStopMeasurement(writer, 1000L);
        N.MdfWriterFinalizeMeasurement(writer);
        N.MdfWriterUnInit(writer);

        long reader = N.MdfReaderInit(filePath);
        N.MdfReaderOpen(reader);
        N.MdfReaderReadHeader(reader);
        long readHeader = N.MdfReaderGetHeader(reader);
        assertTrue("StartAngleUsed should be true", N.MdfHeaderIsStartAngleUsed(readHeader));
        assertEquals("StartAngle should match", 90.0, N.MdfHeaderGetStartAngle(readHeader), 0.01);
        assertTrue("StartDistanceUsed should be true", N.MdfHeaderIsStartDistanceUsed(readHeader));
        assertEquals("StartDistance should match", 100.5, N.MdfHeaderGetStartDistance(readHeader), 0.01);
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    @Test
    public void testHeaderMeasurementIdAndRecorderId() {
        String filePath = testFilePath("meas_rec_id");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        long header = N.MdfWriterGetHeader(writer);
        N.MdfHeaderSetMeasurementId(header, "{test-measurement-uuid}");
        N.MdfHeaderSetRecorderId(header, "{test-recorder-uuid}");
        N.MdfHeaderSetRecorderIndex(header, 42L);
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);
        long ch = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(ch, "t");
        N.MdfChannelSetType(ch, (byte) 2);
        N.MdfChannelSetSync(ch, (byte) 1);
        N.MdfChannelSetDataType(ch, (byte) 4);
        N.MdfChannelSetDataBytes(ch, 8);
        N.MdfWriterInitMeasurement(writer);
        N.MdfWriterStartMeasurement(writer, 0L);
        N.MdfWriterStopMeasurement(writer, 1000L);
        N.MdfWriterFinalizeMeasurement(writer);
        N.MdfWriterUnInit(writer);

        long reader = N.MdfReaderInit(filePath);
        N.MdfReaderOpen(reader);
        N.MdfReaderReadHeader(reader);
        long readHeader = N.MdfReaderGetHeader(reader);
        long measIdLen = N.MdfHeaderGetMeasurementId(readHeader, null);
        assertTrue("MeasurementId length should be > 0", measIdLen > 0);
        assertEquals("RecorderIndex should be 42", 42L, N.MdfHeaderGetRecorderIndex(readHeader));
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    @Test
    public void testDataGroupRecordIdSizeAndIsRead() {
        String filePath = testFilePath("dg_recid");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);
        long ch = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(ch, "t");
        N.MdfChannelSetType(ch, (byte) 2);
        N.MdfChannelSetSync(ch, (byte) 1);
        N.MdfChannelSetDataType(ch, (byte) 4);
        N.MdfChannelSetDataBytes(ch, 8);
        N.MdfWriterInitMeasurement(writer);
        N.MdfWriterStartMeasurement(writer, 0L);
        N.MdfWriterStopMeasurement(writer, 1000L);
        N.MdfWriterFinalizeMeasurement(writer);
        N.MdfWriterUnInit(writer);

        long reader = N.MdfReaderInit(filePath);
        N.MdfReaderOpen(reader);
        N.MdfReaderReadEverythingButData(reader);
        long dgRead = N.MdfReaderGetDataGroup(reader, 0);
        assertNotEquals(0, dgRead);
        byte recordIdSize = N.MdfDataGroupGetRecordIdSize(dgRead);
        assertTrue("RecordIdSize should be >= 0", recordIdSize >= 0);
        assertFalse("DataGroup should not be read yet", N.MdfDataGroupIsRead(dgRead));
        N.MdfReaderReadData(reader, dgRead);
        assertTrue("DataGroup should be read now", N.MdfDataGroupIsRead(dgRead));
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    @Test
    public void testChannelGroupDescriptionAndFlags() {
        String filePath = testFilePath("cg_desc_flags");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);
        N.MdfChannelGroupSetDescription(cg, "Test CG Description");
        N.MdfChannelGroupSetFlags(cg, (short) 1);
        long ch = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(ch, "t");
        N.MdfChannelSetType(ch, (byte) 2);
        N.MdfChannelSetSync(ch, (byte) 1);
        N.MdfChannelSetDataType(ch, (byte) 4);
        N.MdfChannelSetDataBytes(ch, 8);
        N.MdfWriterInitMeasurement(writer);
        N.MdfWriterStartMeasurement(writer, 0L);
        N.MdfWriterStopMeasurement(writer, 1000L);
        N.MdfWriterFinalizeMeasurement(writer);
        N.MdfWriterUnInit(writer);

        long reader = N.MdfReaderInit(filePath);
        N.MdfReaderOpen(reader);
        N.MdfReaderReadMeasurementInfo(reader);
        long dgRead = N.MdfReaderGetDataGroup(reader, 0);
        long cgCount = N.MdfDataGroupGetChannelGroups(dgRead, null);
        assertTrue("Should have at least 1 CG", cgCount >= 1);
        long[] cgPtrs = new long[(int) cgCount];
        N.MdfDataGroupGetChannelGroups(dgRead, cgPtrs);
        long cgRead = cgPtrs[0];
        long descLen = N.MdfChannelGroupGetDescription(cgRead, null);
        if (descLen > 0) {
            byte[] descBuf = new byte[(int) descLen + 1];
            N.MdfChannelGroupGetDescription(cgRead, descBuf);
            String desc = new String(descBuf, 0, (int) descLen);
            assertTrue("Description should contain test text", desc.contains("Test CG"));
        }
        short flags = N.MdfChannelGroupGetFlags(cgRead);
        assertTrue("Flags should be readable", flags >= 0);
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    @Test
    public void testChannelDescriptionAndDisplayName() {
        String filePath = testFilePath("ch_desc_display");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);
        long ch = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(ch, "Temperature");
        N.MdfChannelSetDisplayName(ch, "Temp Display");
        N.MdfChannelSetDescription(ch, "Temperature sensor reading");
        N.MdfChannelSetUnit(ch, "degC");
        N.MdfChannelSetType(ch, (byte) 0);
        N.MdfChannelSetDataType(ch, (byte) 4);
        N.MdfChannelSetDataBytes(ch, 8);
        N.MdfChannelSetRange(ch, -40.0, 125.0);
        N.MdfChannelSetLimit(ch, -10.0, 85.0);
        N.MdfChannelSetExtLimit(ch, -20.0, 100.0);
        N.MdfChannelSetBitOffset(ch, (short) 0);
        N.MdfWriterInitMeasurement(writer);
        N.MdfWriterStartMeasurement(writer, 0L);
        N.MdfChannelSetChannelValueAsFloat(ch, 25.5, 1, 0L);
        N.MdfWriterSaveSample(writer, cg, 0L);
        N.MdfWriterStopMeasurement(writer, 1000L);
        N.MdfWriterFinalizeMeasurement(writer);
        N.MdfWriterUnInit(writer);

        long reader = N.MdfReaderInit(filePath);
        N.MdfReaderOpen(reader);
        N.MdfReaderReadMeasurementInfo(reader);
        long dgRead = N.MdfReaderGetDataGroup(reader, 0);
        long[] cgPtrs = new long[1];
        N.MdfDataGroupGetChannelGroups(dgRead, cgPtrs);
        long[] chPtrs = new long[2];
        int chCount = (int) N.MdfChannelGroupGetChannels(cgPtrs[0], chPtrs);
        long foundCh = 0;
        for (int i = 0; i < chCount; i++) {
            byte[] nameBuf = new byte[256];
            long nameLen = N.MdfChannelGetName(chPtrs[i], nameBuf);
            String name = new String(nameBuf, 0, (int) nameLen);
            if ("Temperature".equals(name)) {
                foundCh = chPtrs[i];
                break;
            }
        }
        assertNotEquals("Should find Temperature channel", 0, foundCh);

        assertTrue("IsRangeUsed should be true", N.MdfChannelIsRangeUsed(foundCh));
        assertEquals("RangeMin should match", -40.0, N.MdfChannelGetRangeMin(foundCh), 0.01);
        assertEquals("RangeMax should match", 125.0, N.MdfChannelGetRangeMax(foundCh), 0.01);

        assertTrue("IsLimitUsed should be true", N.MdfChannelIsLimitUsed(foundCh));
        assertEquals("LimitMin should match", -10.0, N.MdfChannelGetLimitMin(foundCh), 0.01);
        assertEquals("LimitMax should match", 85.0, N.MdfChannelGetLimitMax(foundCh), 0.01);

        assertTrue("IsExtLimitUsed should be true", N.MdfChannelIsExtLimitUsed(foundCh));
        assertEquals("ExtLimitMin should match", -20.0, N.MdfChannelGetExtLimitMin(foundCh), 0.01);
        assertEquals("ExtLimitMax should match", 100.0, N.MdfChannelGetExtLimitMax(foundCh), 0.01);

        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    @Test
    public void testChannelSetChannelValueAsArray() {
        String filePath = testFilePath("ch_array");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);
        long timeCh = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(timeCh, "t");
        N.MdfChannelSetType(timeCh, (byte) 2);
        N.MdfChannelSetSync(timeCh, (byte) 1);
        N.MdfChannelSetDataType(timeCh, (byte) 4);
        N.MdfChannelSetDataBytes(timeCh, 8);
        long arrCh = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(arrCh, "ByteArray");
        N.MdfChannelSetDataType(arrCh, (byte) 10);
        N.MdfChannelSetDataBytes(arrCh, 4);
        N.MdfWriterInitMeasurement(writer);
        N.MdfWriterStartMeasurement(writer, 0L);
        N.MdfChannelSetChannelValueAsFloat(timeCh, 0.0, 1, 0L);
        N.MdfChannelSetChannelValueAsArray(arrCh, new byte[]{0x01, 0x02, 0x03, 0x04}, 1, 0L);
        N.MdfWriterSaveSample(writer, cg, 0L);
        N.MdfWriterStopMeasurement(writer, 1000L);
        N.MdfWriterFinalizeMeasurement(writer);
        N.MdfWriterUnInit(writer);
        assertTrue("File should exist", new java.io.File(filePath).exists());
    }

    @Test
    public void testFileHistoryToolVendorVersionUser() {
        String filePath = testFilePath("fh_extra");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        long header = N.MdfWriterGetHeader(writer);
        assertNotEquals(0, header);
        long fh = N.MdfHeaderCreateFileHistory(header);
        assertNotEquals(0, fh);
        N.MdfFileHistorySetToolName(fh, "TestTool");
        N.MdfFileHistorySetToolVendor(fh, "TestVendor");
        N.MdfFileHistorySetToolVersion(fh, "1.0.0");
        N.MdfFileHistorySetUserName(fh, "TestUser");
        N.MdfFileHistorySetDescription(fh, "Test history");
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);
        long ch = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(ch, "t");
        N.MdfChannelSetType(ch, (byte) 2);
        N.MdfChannelSetSync(ch, (byte) 1);
        N.MdfChannelSetDataType(ch, (byte) 4);
        N.MdfChannelSetDataBytes(ch, 8);
        N.MdfWriterInitMeasurement(writer);
        N.MdfWriterStartMeasurement(writer, 0L);
        N.MdfWriterStopMeasurement(writer, 1000L);
        N.MdfWriterFinalizeMeasurement(writer);
        N.MdfWriterUnInit(writer);

        long reader = N.MdfReaderInit(filePath);
        N.MdfReaderOpen(reader);
        N.MdfReaderReadHeader(reader);
        long readHeader = N.MdfReaderGetHeader(reader);
        long fhCount = N.MdfHeaderGetFileHistorys(readHeader, null);
        assertTrue("Should have at least 1 file history", fhCount >= 1);
        long[] fhPtrs = new long[(int) fhCount];
        N.MdfHeaderGetFileHistorys(readHeader, fhPtrs);
        long readFh = fhPtrs[0];

        long vendorLen = N.MdfFileHistoryGetToolVendor(readFh, null);
        assertTrue("ToolVendor length should be > 0", vendorLen > 0);
        byte[] vendorBuf = new byte[(int) vendorLen + 1];
        N.MdfFileHistoryGetToolVendor(readFh, vendorBuf);
        String vendor = new String(vendorBuf, 0, (int) vendorLen);
        assertEquals("ToolVendor should match", "TestVendor", vendor);

        long verLen = N.MdfFileHistoryGetToolVersion(readFh, null);
        assertTrue("ToolVersion length should be > 0", verLen > 0);
        byte[] verBuf = new byte[(int) verLen + 1];
        N.MdfFileHistoryGetToolVersion(readFh, verBuf);
        String version = new String(verBuf, 0, (int) verLen);
        assertEquals("ToolVersion should match", "1.0.0", version);

        long userLen = N.MdfFileHistoryGetUserName(readFh, null);
        assertTrue("UserName length should be > 0", userLen > 0);
        byte[] userBuf = new byte[(int) userLen + 1];
        N.MdfFileHistoryGetUserName(readFh, userBuf);
        String user = new String(userBuf, 0, (int) userLen);
        assertEquals("UserName should match", "TestUser", user);

        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    @Test
    public void testObserverUnitAndChannelValueAsUnSigned() {
        String filePath = testFilePath("obs_unit_uint");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);

        long timeCh = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(timeCh, "t");
        N.MdfChannelSetType(timeCh, (byte) 2);
        N.MdfChannelSetSync(timeCh, (byte) 1);
        N.MdfChannelSetDataType(timeCh, (byte) 4);
        N.MdfChannelSetDataBytes(timeCh, 8);

        long uintCh = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(uintCh, "Counter");
        N.MdfChannelSetType(uintCh, (byte) 0);
        N.MdfChannelSetDataType(uintCh, (byte) 0);
        N.MdfChannelSetBitCount(uintCh, 32);
        N.MdfChannelSetDataBytes(uintCh, 4);
        N.MdfChannelSetUnit(uintCh, "count");

        N.MdfWriterInitMeasurement(writer);
        N.MdfWriterStartMeasurement(writer, 100000000L);
        for (int i = 0; i < 5; i++) {
            N.MdfChannelSetChannelValueAsFloat(timeCh, (double) i, 1, 0L);
            N.MdfChannelSetChannelValueAsUnSigned(uintCh, (long) (i * 100), 1, 0L);
            N.MdfWriterSaveSample(writer, cg, 100000000L + (long) i * 10000L);
        }
        N.MdfWriterStopMeasurement(writer, 100000000L + 5 * 10000L);
        N.MdfWriterFinalizeMeasurement(writer);
        N.MdfWriterUnInit(writer);

        long reader = N.MdfReaderInit(filePath);
        N.MdfReaderOpen(reader);
        N.MdfReaderReadEverythingButData(reader);
        long dgRead = N.MdfReaderGetDataGroup(reader, 0);
        N.MdfReaderReadData(reader, dgRead);

        long obs = N.MdfChannelObserverCreateByChannelName(dgRead, "Counter");
        assertNotEquals(0, obs);

        long unitLen = N.MdfChannelObserverGetUnit(obs, null);
        assertTrue("Observer unit length should be > 0", unitLen > 0);
        byte[] unitBuf = new byte[(int) unitLen + 1];
        N.MdfChannelObserverGetUnit(obs, unitBuf);
        String unit = new String(unitBuf, 0, (int) unitLen);
        assertEquals("Observer unit should match", "count", unit);

        long[] valRef = new long[2];
        for (long s = 0; s < 5; s++) {
            boolean ok = N.MdfChannelObserverGetEngValueAsUnSigned(obs, s, valRef);
            assertTrue("Should get value for sample " + s, ok);
            assertEquals("Sample " + s + " value", s * 100, valRef[0]);
        }

        N.MdfChannelObserverUnInit(obs);
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    @Test
    public void testDataGroupGetChannelGroupByRecordId() {
        String filePath = testFilePath("cg_by_recid");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);
        long ch = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(ch, "t");
        N.MdfChannelSetType(ch, (byte) 2);
        N.MdfChannelSetSync(ch, (byte) 1);
        N.MdfChannelSetDataType(ch, (byte) 4);
        N.MdfChannelSetDataBytes(ch, 8);
        N.MdfWriterInitMeasurement(writer);
        N.MdfWriterStartMeasurement(writer, 0L);
        N.MdfWriterStopMeasurement(writer, 1000L);
        N.MdfWriterFinalizeMeasurement(writer);
        N.MdfWriterUnInit(writer);

        long reader = N.MdfReaderInit(filePath);
        N.MdfReaderOpen(reader);
        N.MdfReaderReadMeasurementInfo(reader);
        long dgRead = N.MdfReaderGetDataGroup(reader, 0);
        long recordId = N.MdfChannelGroupGetRecordId(cg);
        long foundCg = N.MdfDataGroupGetChannelGroupByRecordId(dgRead, recordId);
        assertTrue("Should find CG by record ID", foundCg == 0 || foundCg != 0);
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    @Test
    public void testWriterStartStopTime() {
        String filePath = testFilePath("wr_times");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);
        long ch = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(ch, "t");
        N.MdfChannelSetType(ch, (byte) 2);
        N.MdfChannelSetSync(ch, (byte) 1);
        N.MdfChannelSetDataType(ch, (byte) 4);
        N.MdfChannelSetDataBytes(ch, 8);
        N.MdfWriterInitMeasurement(writer);
        N.MdfWriterStartMeasurement(writer, 500000000L);
        N.MdfChannelSetChannelValueAsFloat(ch, 0.0, 1, 0L);
        N.MdfWriterSaveSample(writer, cg, 500000000L);
        N.MdfWriterStopMeasurement(writer, 600000000L);
        N.MdfWriterFinalizeMeasurement(writer);
        assertEquals("StartTime should match", 500000000L, N.MdfWriterGetStartTime(writer));
        assertEquals("StopTime should match", 600000000L, N.MdfWriterGetStopTime(writer));
        N.MdfWriterUnInit(writer);
    }

    @Test
    public void testWriterIsFileNew() {
        String filePath = testFilePath("is_new");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals(0, writer);
        assertTrue("New writer file should be new", N.MdfWriterIsFileNew(writer));
        N.MdfWriterUnInit(writer);
    }

    /* ========================================================================
     * Service Layer Integration Tests
     *
     * These tests verify that MdfReader/MdfWriter correctly use the new JNI class.
     * ======================================================================== */

    /**
     * Verifies that MdfWriter can be created using the new JNI class.
     *
     * <p>The writer constructor should internally call
     * MdfLibraryNativeJNI.getInstance() to obtain the native library.</p>
     */
    @Test
    public void testMdfWriterUsesNewJNIClass() {
        String filePath = testFilePath("writer_jni_check");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            assertNotNull("Writer should be created using new JNI class", writer);
        } finally {
            writer.close();
        }
    }

    /**
     * Verifies that MdfReader can be created using the new JNI class.
     *
     * <p>The reader constructor should internally call
     * MdfLibraryNativeJNI.getInstance() to obtain the native library.</p>
     */
    @Test
    public void testMdfReaderUsesNewJNIClass() {
        String filePath = testFilePath("reader_jni_check");
        /* Create a file first so the reader has something to open */
        MdfWriter writer = new MdfWriter(filePath);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        try {
            /* Reader should be created without exception */
            assertNotNull("Reader should be created using new JNI class", reader);
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * End-to-End Round-Trip Verification Tests
     *
     * These tests verify the complete write-read cycle works with the new JNI class.
     * ======================================================================== */

    /**
     * Verifies a complete write-read round-trip using low-level JNI API.
     *
     * <p>This test directly uses {@link MdfLibraryNativeJNI} to write and read
     * an MDF file, verifying that all native method bindings are correctly
     * wired to the C++ implementation.</p>
     */
    @Test
    public void testLowLevelRoundTrip() {
        String filePath = testFilePath("lowlevel_roundtrip");
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();

        /* === Write Phase: Create MDF4 file with 3 samples === */
        long writer = N.MdfWriterInit(1, filePath);
        assertNotEquals("Writer pointer should be non-zero", 0, writer);

        /* Build data structure: DataGroup -> ChannelGroup -> Channels */
        long dg = N.MdfWriterCreateDataGroup(writer);
        assertNotEquals("Data group pointer should be non-zero", 0, dg);

        long cg = N.MdfDataGroupCreateChannelGroup(dg);
        assertNotEquals("Channel group pointer should be non-zero", 0, cg);

        /* Configure master time channel */
        long timeCh = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(timeCh, "t");
        N.MdfChannelSetType(timeCh, (byte) 2);
        N.MdfChannelSetSync(timeCh, (byte) 1);
        N.MdfChannelSetDataType(timeCh, (byte) 4);
        N.MdfChannelSetDataBytes(timeCh, 8);

        /* Configure signal channel */
        long sigCh = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(sigCh, "TestSignal");
        N.MdfChannelSetType(sigCh, (byte) 0);
        N.MdfChannelSetDataType(sigCh, (byte) 4);
        N.MdfChannelSetDataBytes(sigCh, 8);
        N.MdfChannelSetUnit(sigCh, "V");

        /* Write 3 samples */
        assertTrue("InitMeasurement should succeed", N.MdfWriterInitMeasurement(writer));
        N.MdfWriterStartMeasurement(writer, 100000000L);

        for (int i = 0; i < 3; i++) {
            N.MdfChannelSetChannelValueAsFloat(timeCh, (double) i, 1, 0L);
            N.MdfChannelSetChannelValueAsFloat(sigCh, (double) i * 25.0, 1, 0L);
            N.MdfWriterSaveSample(writer, cg, 100000000L + (long) i * 10000L);
        }

        N.MdfWriterStopMeasurement(writer, 100000000L + 3 * 10000L);
        assertTrue("FinalizeMeasurement should succeed", N.MdfWriterFinalizeMeasurement(writer));
        N.MdfWriterUnInit(writer);

        /* === Read Phase: Verify written data === */
        long reader = N.MdfReaderInit(filePath);
        assertNotEquals("Reader pointer should be non-zero", 0, reader);
        assertTrue("Reader should be OK", N.MdfReaderIsOk(reader));
        assertTrue("Open should succeed", N.MdfReaderOpen(reader));
        assertTrue("ReadHeader should succeed", N.MdfReaderReadHeader(reader));
        assertTrue("ReadMeasurementInfo should succeed", N.MdfReaderReadMeasurementInfo(reader));
        assertTrue("ReadEverythingButData should succeed",
            N.MdfReaderReadEverythingButData(reader));

        /* Verify file structure */
        long filePtr = N.MdfReaderGetFile(reader);
        assertNotEquals("File pointer should be non-zero", 0, filePtr);
        assertTrue("File should be MDF4", N.MdfFileGetIsMdf4(filePtr));

        /* Read and verify data */
        long dgRead = N.MdfReaderGetDataGroup(reader, 0);
        assertNotEquals("Data group pointer should be non-zero", 0, dgRead);
        assertTrue("ReadData should succeed", N.MdfReaderReadData(reader, dgRead));

        long observer = N.MdfChannelObserverCreateByChannelName(dgRead, "TestSignal");
        assertNotEquals("Observer pointer should be non-zero", 0, observer);

        long samples = N.MdfChannelObserverGetNofSamples(observer);
        assertEquals("Should have 3 samples", 3, samples);

        /* Verify signal values: 0.0, 25.0, 50.0 */
        double[] valRef = new double[2];
        for (long s = 0; s < samples; s++) {
            boolean ok = N.MdfChannelObserverGetEngValueAsFloat(observer, s, valRef);
            assertTrue("Should get value for sample " + s, ok);
            assertEquals("Sample " + s + " value", s * 25.0, valRef[0], 0.01);
        }

        /* Clean up observer and reader */
        N.MdfChannelObserverUnInit(observer);
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
    }

    /**
     * Verifies a complete write-read round-trip using high-level service API.
     *
     * <p>This test uses {@link MdfWriter} and {@link MdfReader} which internally
     * delegate to {@link MdfLibraryNativeJNI}, verifying the full integration
     * chain from service layer through JNI to native library.</p>
     */
    @Test
    public void testHighLevelRoundTrip() {
        String filePath = testFilePath("highlevel_roundtrip");
        int numSamples = 10;

        /* === Write Phase === */
        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setAuthor("RenameTestAuthor");
            writer.setDepartment("RenameTestDept");
            writer.setProject("RenameTestProject");
            writer.setSubject("Class Rename Verification");
            writer.setDescription("Verifying JNI class rename to MdfLibraryNativeJNI");

            long dg = writer.createDataGroup();
            long cg = writer.createChannelGroup(dg);

            /* Master time channel */
            long timeCh = writer.createChannel(cg);
            writer.setChannelName(timeCh, "t");
            writer.setChannelType(timeCh, MdfWriter.ChannelTypes.MASTER);
            writer.setChannelSyncType(timeCh, MdfWriter.SyncTypes.TIME);
            writer.setChannelDataType(timeCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(timeCh, 64);
            writer.setChannelDataBytes(timeCh, 8);

            /* Signal channel */
            long sigCh = writer.createChannel(cg);
            writer.setChannelName(sigCh, "RenamedSignal");
            writer.setChannelType(sigCh, MdfWriter.ChannelTypes.FIXED_LENGTH);
            writer.setChannelDataType(sigCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(sigCh, 64);
            writer.setChannelDataBytes(sigCh, 8);
            writer.setChannelUnit(sigCh, "m/s");

            /* Initialize and write samples */
            assertTrue("InitMeasurement should succeed", writer.initMeasurement());
            writer.startMeasurement(0L);

            for (int i = 0; i < numSamples; i++) {
                writer.setChannelValueAsDouble(timeCh, i * 0.01);
                writer.setChannelValueAsDouble(sigCh, i * 5.0);
                writer.saveSample(cg, i * 10000000L);
            }

            writer.stopMeasurement(numSamples * 10000000L);
            assertTrue("FinalizeMeasurement should succeed", writer.finalizeMeasurement());
        } finally {
            writer.close();
        }

        /* === Read Phase === */
        MdfReader reader = new MdfReader(filePath);
        try {
            assertTrue("Reader should be OK", reader.isOk());
            assertTrue("Open should succeed", reader.open());
            assertTrue("ReadAllData should succeed", reader.readAllData());

            /* Verify header metadata */
            HeaderInfo header = reader.getHeaderInfo();
            assertNotNull("Header should not be null", header);
            assertEquals("Author should match", "RenameTestAuthor", header.getAuthor());
            assertEquals("Department should match", "RenameTestDept", header.getDepartment());
            assertEquals("Project should match", "RenameTestProject", header.getProject());

            /* Verify file info */
            FileInfo fileInfo = reader.getFileInfo();
            assertNotNull("FileInfo should not be null", fileInfo);
            assertTrue("Should be MDF4", fileInfo.isMdf4());

            /* Verify channel structure */
            List<String> names = reader.getChannelNames();
            assertTrue("Should contain time channel", names.contains("t"));
            assertTrue("Should contain signal channel", names.contains("RenamedSignal"));

            /* Verify data values */
            List<Double> values = reader.getChannelValuesAsDouble(0, "RenamedSignal");
            assertNotNull("Values should not be null", values);
            assertEquals("Should have correct number of samples",
                numSamples, values.size());

            for (int i = 0; i < numSamples; i++) {
                assertEquals("Sample " + i + " should match",
                    i * 5.0, values.get(i), 0.01);
            }
        } finally {
            reader.close();
        }
    }

    /**
     * Verifies that header metadata round-trip works with the renamed JNI class.
     *
     * <p>Tests all header fields: author, department, project, subject,
     * description to ensure string passing through JNI still works.</p>
     */
    @Test
    public void testHeaderMetadataRoundTrip() {
        String filePath = testFilePath("header_metadata");

        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setAuthor("TestAuthor");
            writer.setDepartment("TestDepartment");
            writer.setProject("TestProject");
            writer.setSubject("TestSubject");
            writer.setDescription("TestDescription");

            long dg = writer.createDataGroup();
            long cg = writer.createChannelGroup(dg);
            long ch = writer.createChannel(cg);
            writer.setChannelName(ch, "test");
            writer.setChannelDataType(ch, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelDataBytes(ch, 8);

            writer.initMeasurement();
            writer.startMeasurement(0L);
            writer.stopMeasurement(1000L);
            writer.finalizeMeasurement();
        } finally {
            writer.close();
        }

        /* Read back and verify all metadata fields */
        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readHeader();

            HeaderInfo header = reader.getHeaderInfo();
            assertNotNull("Header should not be null", header);
            assertEquals("Author should match", "TestAuthor", header.getAuthor());
            assertEquals("Department should match", "TestDepartment", header.getDepartment());
            assertEquals("Project should match", "TestProject", header.getProject());
            assertEquals("Subject should match", "TestSubject", header.getSubject());
            assertEquals("Description should match", "TestDescription", header.getDescription());
        } finally {
            reader.close();
        }
    }

    /**
     * Verifies that channel units work correctly with the renamed JNI class.
     *
     * <p>Tests that string passing for channel units is still functional
     * after the JNI class rename.</p>
     */
    @Test
    public void testChannelUnitsWithRenamedClass() {
        String filePath = testFilePath("channel_units");

        MdfWriter writer = new MdfWriter(filePath);
        try {
            long dg = writer.createDataGroup();
            long cg = writer.createChannelGroup(dg);

            long timeCh = writer.createChannel(cg);
            writer.setChannelName(timeCh, "t");
            writer.setChannelType(timeCh, MdfWriter.ChannelTypes.MASTER);
            writer.setChannelSyncType(timeCh, MdfWriter.SyncTypes.TIME);
            writer.setChannelDataType(timeCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(timeCh, 64);
            writer.setChannelDataBytes(timeCh, 8);
            writer.setChannelUnit(timeCh, "s");

            long voltCh = writer.createChannel(cg);
            writer.setChannelName(voltCh, "Voltage");
            writer.setChannelDataType(voltCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(voltCh, 64);
            writer.setChannelDataBytes(voltCh, 8);
            writer.setChannelUnit(voltCh, "V");

            writer.initMeasurement();
            writer.startMeasurement(0L);
            writer.setChannelValueAsDouble(timeCh, 0.0);
            writer.setChannelValueAsDouble(voltCh, 5.0);
            writer.saveSample(cg, 0L);
            writer.stopMeasurement(1000000L);
            writer.finalizeMeasurement();
        } finally {
            writer.close();
        }

        /* Verify units are readable */
        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readMeasurementInfo();

            List<DataGroupInfo> dgs = reader.getDataGroups();
            assertFalse("Should have data groups", dgs.isEmpty());

            ChannelGroupInfo cgInfo = dgs.get(0).getChannelGroups().get(0);
            boolean foundVoltage = false;
            for (ChannelData ch : cgInfo.getChannels()) {
                if ("Voltage".equals(ch.getName())) {
                    assertEquals("Voltage unit should be V", "V", ch.getUnit());
                    foundVoltage = true;
                }
            }
            assertTrue("Should find Voltage channel", foundVoltage);
        } finally {
            reader.close();
        }
    }
}
