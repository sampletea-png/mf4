package com.mdflib.service;

import com.huawei.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI;
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
 * Verification test suite for the JNI class rename from
 * {@code com.mdflib.jni.MdfLibraryNative} to
 * {@code com.huawei.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI}.
 *
 * <p>This test class verifies:</p>
 * <ul>
 *   <li>The new JNI class is correctly named and accessible</li>
 *   <li>The new package path resolves correctly</li>
 *   <li>The singleton pattern still works with the renamed class</li>
 *   <li>The native library loading still functions</li>
 *   <li>All native methods are still callable via the new JNI class</li>
 *   <li>End-to-end write and read operations work correctly</li>
 *   <li>The MdfReader/MdfWriter service classes use the new JNI class</li>
 * </ul>
 *
 * <p>Test isolation: Each test creates its own temporary directory and files,
 * ensuring no cross-test interference.</p>
 *
 * @author mdflib-java contributors
 * @version 1.0.0
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
     *
     * <p>The class must be accessible as
     * {@code com.huawei.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI}.
     * This test ensures the class was properly created in the new package.</p>
     */
    @Test
    public void testJNIClassCorrectName() {
        Class<?> clazz = MdfLibraryNativeJNI.class;
        assertEquals(
            "JNI class should have the correct fully qualified name",
            "com.huawei.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI",
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

    /**
     * Verifies that key native methods are declared in the new JNI class.
     *
     * <p>Checks for the existence of critical reader/writer methods to ensure
     * no methods were lost during the rename.</p>
     *
     * @throws NoSuchMethodException if a method is not found (test fails)
     */
    @Test
    public void testNativeMethodsExist() throws NoSuchMethodException {
        Class<?> clazz = MdfLibraryNativeJNI.class;

        /* Verify MdfReader methods */
        clazz.getMethod("MdfReaderInit", String.class);
        clazz.getMethod("MdfReaderUnInit", long.class);
        clazz.getMethod("MdfReaderIsOk", long.class);
        clazz.getMethod("MdfReaderGetFile", long.class);
        clazz.getMethod("MdfReaderGetHeader", long.class);
        clazz.getMethod("MdfReaderOpen", long.class);
        clazz.getMethod("MdfReaderClose", long.class);
        clazz.getMethod("MdfReaderReadHeader", long.class);
        clazz.getMethod("MdfReaderReadMeasurementInfo", long.class);
        clazz.getMethod("MdfReaderReadEverythingButData", long.class);
        clazz.getMethod("MdfReaderReadData", long.class, long.class);

        /* Verify MdfWriter methods */
        clazz.getMethod("MdfWriterInit", int.class, String.class);
        clazz.getMethod("MdfWriterUnInit", long.class);
        clazz.getMethod("MdfWriterGetFile", long.class);
        clazz.getMethod("MdfWriterGetHeader", long.class);
        clazz.getMethod("MdfWriterInitMeasurement", long.class);
        clazz.getMethod("MdfWriterStartMeasurement", long.class, long.class);
        clazz.getMethod("MdfWriterStopMeasurement", long.class, long.class);
        clazz.getMethod("MdfWriterFinalizeMeasurement", long.class);
        clazz.getMethod("MdfWriterSaveSample", long.class, long.class, long.class);

        /* Verify MdfChannel methods */
        clazz.getMethod("MdfChannelSetName", long.class, String.class);
        clazz.getMethod("MdfChannelSetType", long.class, byte.class);
        clazz.getMethod("MdfChannelSetSync", long.class, byte.class);
        clazz.getMethod("MdfChannelSetDataType", long.class, byte.class);
        clazz.getMethod("MdfChannelSetDataBytes", long.class, long.class);
        clazz.getMethod("MdfChannelSetChannelValueAsFloat", long.class, double.class,
            int.class, long.class);
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

    /* ========================================================================
     * Backward Compatibility Verification Tests
     *
     * These tests verify that the old class no longer exists.
     * ======================================================================== */

    /**
     * Verifies that the old JNI class no longer exists in the classpath.
     *
     * <p>After the rename, attempting to load
     * {@code com.mdflib.jni.MdfLibraryNative} should throw
     * ClassNotFoundException, confirming the old class was properly removed.</p>
     */
    @Test
    public void testOldJNIClassDoesNotExist() {
        boolean oldClassExists;
        try {
            Class.forName("com.mdflib.jni.MdfLibraryNative");
            oldClassExists = true;
        } catch (ClassNotFoundException e) {
            oldClassExists = false;
        }
        assertFalse(
            "Old class com.mdflib.jni.MdfLibraryNative should not exist",
            oldClassExists
        );
    }

    /**
     * Verifies that the new JNI class exists in the classpath.
     *
     * <p>The renamed class must be loadable by name to confirm proper
     * deployment.</p>
     */
    @Test
    public void testNewJNIClassExists() {
        boolean newClassExists;
        try {
            Class.forName(
                "com.huawei.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI");
            newClassExists = true;
        } catch (ClassNotFoundException e) {
            newClassExists = false;
        }
        assertTrue(
            "New class com.huawei.simulation.datawatch.service.mdflib.jni"
            + ".MdfLibraryNativeJNI should exist",
            newClassExists
        );
    }

    /* ========================================================================
     * Compression Verification Test
     *
     * Verifies compression still works after the rename.
     * ======================================================================== */

    /**
     * Verifies that compressed write-read round-trip works with renamed JNI class.
     *
     * <p>Compression is an important feature that exercises multiple JNI
     * code paths; this test ensures it still functions correctly.</p>
     */
    @Test
    public void testCompressedRoundTrip() {
        String filePath = testFilePath("compressed_rename");
        int numSamples = 50;

        /* Write compressed file */
        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setCompressData(true);
            writer.setAuthor("CompressRenameTest");

            long dg = writer.createDataGroup();
            long cg = writer.createChannelGroup(dg);

            long timeCh = writer.createChannel(cg);
            writer.setChannelName(timeCh, "t");
            writer.setChannelType(timeCh, MdfWriter.ChannelTypes.MASTER);
            writer.setChannelSyncType(timeCh, MdfWriter.SyncTypes.TIME);
            writer.setChannelDataType(timeCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(timeCh, 64);
            writer.setChannelDataBytes(timeCh, 8);

            long sigCh = writer.createChannel(cg);
            writer.setChannelName(sigCh, "CompressedSignal");
            writer.setChannelDataType(sigCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(sigCh, 64);
            writer.setChannelDataBytes(sigCh, 8);

            writer.initMeasurement();
            writer.startMeasurement(0L);

            for (int i = 0; i < numSamples; i++) {
                writer.setChannelValueAsDouble(timeCh, i * 0.01);
                writer.setChannelValueAsDouble(sigCh, i * 10.0);
                writer.saveSample(cg, i * 10000000L);
            }

            writer.stopMeasurement(numSamples * 10000000L);
            writer.finalizeMeasurement();
        } finally {
            writer.close();
        }

        /* Read back and verify compressed data integrity */
        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readAllData();

            List<Double> values = reader.getChannelValuesAsDouble(0, "CompressedSignal");
            assertNotNull("Values should not be null", values);
            assertEquals("Should have all samples", numSamples, values.size());

            for (int i = 0; i < numSamples; i++) {
                assertEquals("Compressed sample " + i + " should match",
                    i * 10.0, values.get(i), 0.01);
            }
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * Multiple Channel Verification Test
     * ======================================================================== */

    /**
     * Verifies multi-channel write-read with renamed JNI class.
     *
     * <p>Creates multiple channels with different units and verifies
     * all values are correctly preserved through the JNI bridge.</p>
     */
    @Test
    public void testMultiChannelRoundTrip() {
        String filePath = testFilePath("multi_channel_rename");

        MdfWriter writer = new MdfWriter(filePath);
        try {
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

            /* Temperature channel */
            long tempCh = writer.createChannel(cg);
            writer.setChannelName(tempCh, "Temperature");
            writer.setChannelUnit(tempCh, "degC");
            writer.setChannelDataType(tempCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(tempCh, 64);
            writer.setChannelDataBytes(tempCh, 8);

            /* Pressure channel */
            long pressCh = writer.createChannel(cg);
            writer.setChannelName(pressCh, "Pressure");
            writer.setChannelUnit(pressCh, "kPa");
            writer.setChannelDataType(pressCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(pressCh, 64);
            writer.setChannelDataBytes(pressCh, 8);

            writer.initMeasurement();
            writer.startMeasurement(0L);

            /* Write 10 samples */
            for (int i = 0; i < 10; i++) {
                writer.setChannelValueAsDouble(timeCh, i * 0.1);
                writer.setChannelValueAsDouble(tempCh, 20.0 + i * 0.5);
                writer.setChannelValueAsDouble(pressCh, 101.3 + i * 0.1);
                writer.saveSample(cg, i * 100000000L);
            }

            writer.stopMeasurement(10 * 100000000L);
            writer.finalizeMeasurement();
        } finally {
            writer.close();
        }

        /* Read back and verify */
        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readAllData();

            List<Double> tempValues = reader.getChannelValuesAsDouble(0, "Temperature");
            assertEquals("Should have 10 temperature samples", 10, tempValues.size());
            assertEquals("First temp should be 20.0", 20.0, tempValues.get(0), 0.01);
            assertEquals("Last temp should be 24.5", 24.5, tempValues.get(9), 0.01);

            List<Double> pressValues = reader.getChannelValuesAsDouble(0, "Pressure");
            assertEquals("Should have 10 pressure samples", 10, pressValues.size());
            assertEquals("First pressure should be 101.3", 101.3, pressValues.get(0), 0.01);
        } finally {
            reader.close();
        }
    }
}
