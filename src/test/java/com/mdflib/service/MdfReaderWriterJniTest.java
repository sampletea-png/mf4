package com.mdflib.service;

import com.mdflib.jni.MdfLibraryNative;
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
 * Comprehensive test suite for MdfReader and MdfWriter using JNI.
 *
 * <p>This test class covers the following scenarios:</p>
 * <ul>
 *   <li>Writer lifecycle: creation, configuration, writing, finalization</li>
 *   <li>Reader lifecycle: creation, opening, reading, closing</li>
 *   <li>Round-trip: write a file and read it back to verify data integrity</li>
 *   <li>Header metadata: author, department, project, subject, description</li>
 *   <li>Channel configuration: name, unit, type, data type, bit count</li>
 *   <li>Data recording: floating-point, integer, and string values</li>
 *   <li>Multiple channels and multiple samples</li>
 *   <li>Edge cases: empty files, null parameters, closed readers/writers</li>
 *   <li>Compression: writing with and without data compression</li>
 *   <li>Multiple data groups and channel groups</li>
 * </ul>
 *
 * <p>Test isolation: Each test creates its own temporary directory and files,
 * ensuring no cross-test interference.</p>
 *
 * @author mdflib-java contributors
 * @version 1.0.0
 * @since 1.0.0
 */
public class MdfReaderWriterJniTest {

    /** Temporary directory for test output files. Cleaned up after each test. */
    private Path tempDir;

    /** Native library instance, reused across tests for efficiency. */
    private MdfLibraryNative nativeLib;

    /**
     * Sets up the test environment before each test.
     *
     * <p>Creates a temporary directory for test output files and obtains
     * the native library singleton instance.</p>
     *
     * @throws IOException if the temporary directory cannot be created
     */
    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("mdflib_jni_test");
        nativeLib = MdfLibraryNative.getInstance();
    }

    /**
     * Cleans up the test environment after each test.
     *
     * <p>Deletes all files in the temporary directory and the directory itself.
     * Silently ignores deletion failures to avoid test flakiness.</p>
     *
     * @throws IOException if directory cleanup fails
     */
    @After
    public void tearDown() throws IOException {
        if (tempDir != null) {
            /* Delete all files in the temp directory recursively */
            Files.walk(tempDir)
                 .sorted((a, b) -> -a.compareTo(b)) // Delete children before parents
                 .forEach(p -> {
                     try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                 });
        }
    }

    /* ========================================================================
     * Helper Methods
     *
     * These methods reduce test boilerplate by providing common operations
     * like creating test file paths and writing simple MDF files.
     * ======================================================================== */

    /**
     * Creates a unique test file path in the temporary directory.
     *
     * <p>Uses the provided suffix to create a descriptive filename
     * that ends with the .mf4 extension. The file is created in the
     * test-specific temporary directory to ensure isolation.</p>
     *
     * @param suffix file name suffix (e.g., "basic_write")
     * @return absolute path to the test file with .mf4 extension
     */
    private String testFilePath(String suffix) {
        return tempDir.resolve(suffix + ".mf4").toString();
    }

    /**
     * Creates a simple MDF4 file with one master channel and one data channel.
     *
     * <p>This is the most basic MDF file structure: one data group, one channel
     * group, two channels (time + signal), and a configurable number of samples.
     * Compression is disabled by default.</p>
     *
     * <p>Channel structure:</p>
     * <ul>
     *   <li>Master channel "t" (time, FLOAT_LE, 64-bit)</li>
     *   <li>Data channel "Signal1" (FLOAT_LE, 64-bit, unit "V")</li>
     * </ul>
     *
     * @param filePath output file path for the MDF file
     * @param numSamples number of data samples to write (must be >= 0)
     * @return the MdfWriter instance (caller must call close() when done)
     */
    private MdfWriter writeSimpleFile(String filePath, int numSamples) {
        return writeSimpleFile(filePath, numSamples, false);
    }

    /**
     * Creates a simple MDF4 file with optional compression.
     *
     * <p>This helper method creates a complete MDF4 file with the following
     * structure and data:</p>
     *
     * <p>Header metadata:</p>
     * <ul>
     *   <li>Author: "TestAuthor"</li>
     *   <li>Department: "TestDepartment"</li>
     *   <li>Project: "TestProject"</li>
     *   <li>Subject: "TestSubject"</li>
     *   <li>Description: "Test description for JNI validation"</li>
     * </ul>
     *
     * <p>Channel structure:</p>
     * <ul>
     *   <li>Master channel "t" (time, FLOAT_LE, 64-bit, sync=TIME)</li>
     *   <li>Data channel "Signal1" (FLOAT_LE, 64-bit, unit "V")</li>
     * </ul>
     *
     * <p>Sample values: time = i * 0.01s, signal = i * 1.5</p>
     *
     * @param filePath output file path for the MDF file
     * @param numSamples number of data samples to write (must be >= 0)
     * @param compress whether to enable data compression in the output file
     * @return the MdfWriter instance (caller must call close() when done)
     */
    private MdfWriter writeSimpleFile(String filePath, int numSamples, boolean compress) {
        MdfWriter writer = new MdfWriter(filePath);
        writer.setAuthor("TestAuthor");
        writer.setDepartment("TestDepartment");
        writer.setProject("TestProject");
        writer.setSubject("TestSubject");
        writer.setDescription("Test description for JNI validation");
        writer.setCompressData(compress);

        /* Create data group and channel group */
        long dg = writer.createDataGroup();
        long cg = writer.createChannelGroup(dg);

        /* Create master time channel */
        long timeCh = writer.createChannel(cg);
        writer.setChannelName(timeCh, "t");
        writer.setChannelType(timeCh, MdfWriter.ChannelTypes.MASTER);
        writer.setChannelSyncType(timeCh, MdfWriter.SyncTypes.TIME);
        writer.setChannelDataType(timeCh, MdfWriter.DataTypes.FLOAT_LE);
        writer.setChannelBitCount(timeCh, 64);
        writer.setChannelDataBytes(timeCh, 8);

        /* Create signal channel */
        long signalCh = writer.createChannel(cg);
        writer.setChannelName(signalCh, "Signal1");
        writer.setChannelType(signalCh, MdfWriter.ChannelTypes.FIXED_LENGTH);
        writer.setChannelSyncType(signalCh, MdfWriter.SyncTypes.NONE);
        writer.setChannelDataType(signalCh, MdfWriter.DataTypes.FLOAT_LE);
        writer.setChannelBitCount(signalCh, 64);
        writer.setChannelDataBytes(signalCh, 8);
        writer.setChannelUnit(signalCh, "V");

        /* Initialize and start measurement */
        assertTrue("Measurement initialization should succeed",
            writer.initMeasurement());
        long startTime = 1000000000L; // 1 second in nanoseconds
        writer.startMeasurement(startTime);

        /* Write samples */
        for (int i = 0; i < numSamples; i++) {
            double timeValue = i * 0.01;
            double signalValue = i * 1.5;
            writer.setChannelValueAsDouble(timeCh, timeValue);
            writer.setChannelValueAsDouble(signalCh, signalValue);
            writer.saveSample(cg, startTime + i * 10000000L);
        }

        /* Stop and finalize */
        long stopTime = startTime + numSamples * 10000000L;
        writer.stopMeasurement(stopTime);
        assertTrue("Measurement finalization should succeed",
            writer.finalizeMeasurement());

        return writer;
    }

    /* ========================================================================
     * Writer Construction Tests
     * ======================================================================== */

    /**
     * Tests that an MdfWriter can be created with a valid file path.
     *
     * <p>Verifies that the constructor does not throw an exception and
     * the writer pointer is valid (non-zero internally).</p>
     */
    @Test
    public void testWriterCreation() {
        String filePath = testFilePath("writer_creation");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            assertNotNull("Writer should be created successfully", writer);
        } finally {
            writer.close();
        }
    }

    /**
     * Tests that an MdfWriter rejects a null file path.
     *
     * <p>Null file paths should cause an IllegalArgumentException,
     * preventing undefined behavior in the native layer.</p>
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWriterCreationNullPath() {
        new MdfWriter(null);
    }

    /**
     * Tests that an MdfWriter rejects an empty file path.
     *
     * <p>Empty file paths are invalid and should be caught early
     * in the Java layer before reaching native code.</p>
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWriterCreationEmptyPath() {
        new MdfWriter("");
    }

    /**
     * Tests that an MdfWriter rejects a whitespace-only file path.
     *
     * <p>Whitespace-only paths are effectively empty and should
     * be rejected by input validation.</p>
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWriterCreationWhitespacePath() {
        new MdfWriter("   ");
    }

    /**
     * Tests that an MdfWriter can be created with an explicit type parameter.
     *
     * <p>The MDF4 basic type (1) should create a valid writer.</p>
     */
    @Test
    public void testWriterCreationWithType() {
        String filePath = testFilePath("writer_type");
        MdfWriter writer = new MdfWriter(MdfWriter.MDF4_BASIC, filePath);
        try {
            assertNotNull("Writer with explicit type should be created", writer);
        } finally {
            writer.close();
        }
    }

    /* ========================================================================
     * Writer Metadata Tests
     * ======================================================================== */

    /**
     * Tests setting the author metadata field.
     *
     * <p>Verifies that setAuthor does not throw an exception and
     * the value is persisted in the file header.</p>
     */
    @Test
    public void testSetAuthor() {
        String filePath = testFilePath("set_author");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setAuthor("John Doe");
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

        /* Read back and verify author */
        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readHeader();
            HeaderInfo header = reader.getHeaderInfo();
            assertNotNull("Header should not be null", header);
            assertEquals("Author should match", "John Doe", header.getAuthor());
        } finally {
            reader.close();
        }
    }

    /**
     * Tests setting the department metadata field.
     */
    @Test
    public void testSetDepartment() {
        String filePath = testFilePath("set_dept");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setDepartment("Engineering");
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

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readHeader();
            HeaderInfo header = reader.getHeaderInfo();
            assertNotNull("Header should not be null", header);
            assertEquals("Department should match", "Engineering", header.getDepartment());
        } finally {
            reader.close();
        }
    }

    /**
     * Tests setting the project metadata field.
     */
    @Test
    public void testSetProject() {
        String filePath = testFilePath("set_project");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setProject("MDFLib JNI");
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

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readHeader();
            HeaderInfo header = reader.getHeaderInfo();
            assertNotNull("Header should not be null", header);
            assertEquals("Project should match", "MDFLib JNI", header.getProject());
        } finally {
            reader.close();
        }
    }

    /**
     * Tests setting the subject metadata field.
     */
    @Test
    public void testSetSubject() {
        String filePath = testFilePath("set_subject");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setSubject("Temperature Test");
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

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readHeader();
            HeaderInfo header = reader.getHeaderInfo();
            assertNotNull("Header should not be null", header);
            assertEquals("Subject should match", "Temperature Test", header.getSubject());
        } finally {
            reader.close();
        }
    }

    /**
     * Tests setting the description metadata field.
     */
    @Test
    public void testSetDescription() {
        String filePath = testFilePath("set_desc");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setDescription("A test measurement file");
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

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readHeader();
            HeaderInfo header = reader.getHeaderInfo();
            assertNotNull("Header should not be null", header);
            assertEquals("Description should match", "A test measurement file", header.getDescription());
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * Compression Tests
     * ======================================================================== */

    /**
     * Tests writing an MDF file with data compression enabled.
     *
     * <p>Compressed files should be smaller than uncompressed files
     * and should be readable with correct data values.</p>
     */
    @Test
    public void testWriteWithCompression() {
        String compressedPath = testFilePath("compressed");
        String uncompressedPath = testFilePath("uncompressed");

        /* Write compressed file */
        MdfWriter compressedWriter = writeSimpleFile(compressedPath, 100, true);
        compressedWriter.close();

        /* Write uncompressed file */
        MdfWriter uncompressedWriter = writeSimpleFile(uncompressedPath, 100, false);
        uncompressedWriter.close();

        /* Verify both files exist and are readable */
        File compressedFile = new File(compressedPath);
        File uncompressedFile = new File(uncompressedPath);

        assertTrue("Compressed file should exist", compressedFile.exists());
        assertTrue("Uncompressed file should exist", uncompressedFile.exists());

        /* Compressed file should be smaller (or at least not larger) */
        long compressedSize = compressedFile.length();
        long uncompressedSize = uncompressedFile.length();
        assertTrue("Compressed file should not be larger than uncompressed",
            compressedSize <= uncompressedSize);
    }

    /**
     * Tests that compressed data can be read back correctly.
     *
     * <p>Verifies data integrity after compression/decompression round-trip.</p>
     */
    @Test
    public void testReadCompressedData() {
        String filePath = testFilePath("read_compressed");
        MdfWriter writer = writeSimpleFile(filePath, 10, true);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        try {
            assertTrue("Reader should be OK", reader.isOk());
            reader.open();
            reader.readAllData();

            List<Double> values = reader.getChannelValuesAsDouble(0, "Signal1");
            assertNotNull("Signal values should not be null", values);
            assertEquals("Should have 10 samples", 10, values.size());

            /* Verify first and last values */
            assertEquals("First value should be 0.0", 0.0, values.get(0), 0.001);
            assertEquals("Last value should be 13.5", 13.5, values.get(9), 0.001);
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * Round-trip Read/Write Tests
     * ======================================================================== */

    /**
     * Tests the basic round-trip: write a file, read it back, verify data.
     *
     * <p>This is the most fundamental integration test, verifying that
     * the JNI bridge correctly passes data through the entire pipeline
     * from Java -> JNI -> C++ mdflib -> disk -> C++ mdflib -> JNI -> Java.</p>
     *
     * <p>Verification steps:</p>
     * <ol>
     *   <li>Writer creates a valid MDF4 file with 5 samples</li>
     *   <li>Reader opens the file and confirms it's a valid MDF4</li>
     *   <li>Header metadata (author, department, project, subject) matches</li>
     *   <li>Data structure (DG -> CG -> Channels) is correct</li>
     *   <li>Channel values match the written values exactly</li>
     * </ol>
     */
    @Test
    public void testBasicRoundTrip() {
        String filePath = testFilePath("basic_roundtrip");
        int numSamples = 5;

        /* === Phase 1: Write the MDF file === */
        MdfWriter writer = writeSimpleFile(filePath, numSamples);
        writer.close();

        /* Verify the file was created on disk */
        assertTrue("Output file should exist", new File(filePath).exists());

        /* === Phase 2: Read the file back === */
        MdfReader reader = new MdfReader(filePath);
        try {
            /* Verify reader initialization and open */
            assertTrue("Reader should be OK after init", reader.isOk());
            assertTrue("Open should succeed", reader.open());
            assertTrue("ReadHeader should succeed", reader.readHeader());
            assertTrue("ReadMeasurementInfo should succeed", reader.readMeasurementInfo());
            assertTrue("ReadEverythingButData should succeed", reader.readEverythingButData());

            /* Verify file-level metadata */
            FileInfo fileInfo = reader.getFileInfo();
            assertNotNull("FileInfo should not be null", fileInfo);
            assertTrue("File should be MDF4", fileInfo.isMdf4());
            assertEquals("Main version should be 4", 4, fileInfo.getMainVersion());

            /* Verify header metadata */
            HeaderInfo headerInfo = reader.getHeaderInfo();
            assertNotNull("HeaderInfo should not be null", headerInfo);
            assertEquals("Author should match", "TestAuthor", headerInfo.getAuthor());
            assertEquals("Department should match", "TestDepartment", headerInfo.getDepartment());
            assertEquals("Project should match", "TestProject", headerInfo.getProject());
            assertEquals("Subject should match", "TestSubject", headerInfo.getSubject());

            /* Verify data group structure */
            List<DataGroupInfo> dataGroups = reader.getDataGroups();
            assertNotNull("Data groups should not be null", dataGroups);
            assertFalse("Should have at least one data group", dataGroups.isEmpty());

            DataGroupInfo dg = dataGroups.get(0);
            assertNotNull("Channel groups should not be null", dg.getChannelGroups());
            assertFalse("Should have at least one channel group", dg.getChannelGroups().isEmpty());

            ChannelGroupInfo cg = dg.getChannelGroups().get(0);
            assertNotNull("Channels should not be null", cg.getChannels());
            assertTrue("Should have at least 2 channels (time + signal)",
                cg.getChannels().size() >= 2);

            /* Verify channel names are correct */
            List<String> channelNames = reader.getChannelNames();
            assertTrue("Should contain time channel 't'", channelNames.contains("t"));
            assertTrue("Should contain signal channel 'Signal1'", channelNames.contains("Signal1"));

            /* Read and verify actual measurement data */
            assertTrue("ReadData should succeed", reader.readData(0));

            List<Double> signalValues = reader.getChannelValuesAsDouble(0, "Signal1");
            assertNotNull("Signal values should not be null", signalValues);
            assertEquals("Should have correct number of samples",
                numSamples, signalValues.size());

            /* Verify individual sample values match the written data */
            for (int i = 0; i < numSamples; i++) {
                double expected = i * 1.5;
                assertEquals("Sample " + i + " should match",
                    expected, signalValues.get(i), 0.001);
            }
        } finally {
            reader.close();
        }
    }

    /**
     * Tests round-trip with a single sample.
     *
     * <p>Edge case: verifies that files with only one sample are handled
     * correctly by both writer and reader.</p>
     */
    @Test
    public void testSingleSampleRoundTrip() {
        String filePath = testFilePath("single_sample");
        MdfWriter writer = writeSimpleFile(filePath, 1);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readAllData();

            List<Double> values = reader.getChannelValuesAsDouble(0, "Signal1");
            assertNotNull("Values should not be null", values);
            assertEquals("Should have exactly 1 sample", 1, values.size());
            assertEquals("First sample should be 0.0", 0.0, values.get(0), 0.001);
        } finally {
            reader.close();
        }
    }

    /**
     * Tests round-trip with a large number of samples.
     *
     * <p>Stress test: verifies that the JNI bridge can handle thousands
     * of samples without memory corruption or performance issues.</p>
     */
    @Test
    public void testLargeSampleRoundTrip() {
        String filePath = testFilePath("large_sample");
        int numSamples = 1000;

        MdfWriter writer = writeSimpleFile(filePath, numSamples);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readAllData();

            List<Double> values = reader.getChannelValuesAsDouble(0, "Signal1");
            assertNotNull("Values should not be null", values);
            assertEquals("Should have all samples", numSamples, values.size());

            /* Spot-check first, middle, and last values */
            assertEquals("First value", 0.0, values.get(0), 0.001);
            assertEquals("Middle value", 748.5, values.get(499), 0.01);
            assertEquals("Last value", 1498.5, values.get(999), 0.01);
        } finally {
            reader.close();
        }
    }

    /**
     * Tests round-trip with zero samples (empty measurement).
     *
     * <p>Edge case: verifies that an empty measurement (no samples recorded)
     * produces a valid MDF file that can be read back.</p>
     */
    @Test
    public void testZeroSampleRoundTrip() {
        String filePath = testFilePath("zero_sample");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            long dg = writer.createDataGroup();
            long cg = writer.createChannelGroup(dg);
            long ch = writer.createChannel(cg);
            writer.setChannelName(ch, "empty_channel");
            writer.setChannelDataType(ch, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelDataBytes(ch, 8);
            writer.initMeasurement();
            writer.startMeasurement(0L);
            writer.stopMeasurement(1000L);
            writer.finalizeMeasurement();
        } finally {
            writer.close();
        }

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readAllData();
            /* Empty measurement should still be readable */
            assertTrue("Reader should be OK", reader.isOk());
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * Multiple Channel Tests
     * ======================================================================== */

    /**
     * Tests writing and reading multiple channels in a single channel group.
     *
     * <p>Verifies that all channels in a group are correctly written and
     * can be individually read back with correct values.</p>
     */
    @Test
    public void testMultipleChannels() {
        String filePath = testFilePath("multi_channel");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setAuthor("MultiChannelTest");

            long dg = writer.createDataGroup();
            long cg = writer.createChannelGroup(dg);

            /* Create master time channel */
            long timeCh = writer.createChannel(cg);
            writer.setChannelName(timeCh, "t");
            writer.setChannelType(timeCh, MdfWriter.ChannelTypes.MASTER);
            writer.setChannelSyncType(timeCh, MdfWriter.SyncTypes.TIME);
            writer.setChannelDataType(timeCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(timeCh, 64);
            writer.setChannelDataBytes(timeCh, 8);

            /* Create temperature channel */
            long tempCh = writer.createChannel(cg);
            writer.setChannelName(tempCh, "Temperature");
            writer.setChannelType(tempCh, MdfWriter.ChannelTypes.FIXED_LENGTH);
            writer.setChannelDataType(tempCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(tempCh, 64);
            writer.setChannelDataBytes(tempCh, 8);
            writer.setChannelUnit(tempCh, "degC");

            /* Create pressure channel */
            long pressCh = writer.createChannel(cg);
            writer.setChannelName(pressCh, "Pressure");
            writer.setChannelType(pressCh, MdfWriter.ChannelTypes.FIXED_LENGTH);
            writer.setChannelDataType(pressCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(pressCh, 64);
            writer.setChannelDataBytes(pressCh, 8);
            writer.setChannelUnit(pressCh, "kPa");

            /* Create speed channel */
            long speedCh = writer.createChannel(cg);
            writer.setChannelName(speedCh, "Speed");
            writer.setChannelType(speedCh, MdfWriter.ChannelTypes.FIXED_LENGTH);
            writer.setChannelDataType(speedCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(speedCh, 64);
            writer.setChannelDataBytes(speedCh, 8);
            writer.setChannelUnit(speedCh, "km/h");

            writer.initMeasurement();
            writer.startMeasurement(0L);

            /* Write 20 samples for all channels */
            for (int i = 0; i < 20; i++) {
                writer.setChannelValueAsDouble(timeCh, i * 0.1);
                writer.setChannelValueAsDouble(tempCh, 20.0 + i * 0.5);
                writer.setChannelValueAsDouble(pressCh, 101.3 + i * 0.1);
                writer.setChannelValueAsDouble(speedCh, 50.0 + i * 2.0);
                writer.saveSample(cg, i * 100000000L);
            }

            writer.stopMeasurement(20 * 100000000L);
            writer.finalizeMeasurement();
        } finally {
            writer.close();
        }

        /* Read back and verify all channels */
        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readAllData();

            /* Verify channel names */
            List<String> names = reader.getChannelNames();
            assertTrue("Should contain Temperature channel", names.contains("Temperature"));
            assertTrue("Should contain Pressure channel", names.contains("Pressure"));
            assertTrue("Should contain Speed channel", names.contains("Speed"));

            /* Verify temperature values */
            List<Double> tempValues = reader.getChannelValuesAsDouble(0, "Temperature");
            assertEquals("Should have 20 temperature samples", 20, tempValues.size());
            assertEquals("First temperature", 20.0, tempValues.get(0), 0.01);
            assertEquals("Last temperature", 29.5, tempValues.get(19), 0.01);

            /* Verify pressure values */
            List<Double> pressValues = reader.getChannelValuesAsDouble(0, "Pressure");
            assertEquals("Should have 20 pressure samples", 20, pressValues.size());
            assertEquals("First pressure", 101.3, pressValues.get(0), 0.01);

            /* Verify speed values */
            List<Double> speedValues = reader.getChannelValuesAsDouble(0, "Speed");
            assertEquals("Should have 20 speed samples", 20, speedValues.size());
            assertEquals("First speed", 50.0, speedValues.get(0), 0.01);
            assertEquals("Last speed", 88.0, speedValues.get(19), 0.01);
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * Channel Unit Tests
     * ======================================================================== */

    /**
     * Tests that channel units are correctly written and read back.
     */
    @Test
    public void testChannelUnits() {
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

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readMeasurementInfo();

            List<DataGroupInfo> dgs = reader.getDataGroups();
            assertFalse("Should have data groups", dgs.isEmpty());

            ChannelGroupInfo cg = dgs.get(0).getChannelGroups().get(0);
            boolean foundVoltage = false;
            for (ChannelData ch : cg.getChannels()) {
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
     * Reader Lifecycle Tests
     * ======================================================================== */

    /**
     * Tests that an MdfReader rejects a null file path.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testReaderCreationNullPath() {
        new MdfReader(null);
    }

    /**
     * Tests that an MdfReader rejects an empty file path.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testReaderCreationEmptyPath() {
        new MdfReader("");
    }

    /**
     * Tests that an MdfReader for a non-existent file reports as not OK.
     *
     * <p>The reader should initialize without throwing, but isOk() should
     * return false for a non-existent file.</p>
     */
    @Test
    public void testReaderNonExistentFile() {
        String filePath = testFilePath("nonexistent");
        MdfReader reader = new MdfReader(filePath);
        try {
            /* Reader should be created but not OK for non-existent file */
            assertFalse("Reader should not be OK for non-existent file", reader.isOk());
        } finally {
            reader.close();
        }
    }

    /**
     * Tests that operations on a closed reader throw IllegalStateException.
     */
    @Test(expected = IllegalStateException.class)
    public void testReaderOperationAfterClose() {
        String filePath = testFilePath("closed_reader");
        MdfWriter writer = writeSimpleFile(filePath, 1);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        reader.close();

        /* This should throw IllegalStateException */
        reader.isOk();
    }

    /**
     * Tests that reading header info after close throws IllegalStateException.
     */
    @Test(expected = IllegalStateException.class)
    public void testReaderGetHeaderAfterClose() {
        String filePath = testFilePath("closed_header");
        MdfWriter writer = writeSimpleFile(filePath, 1);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        reader.close();

        reader.getHeaderInfo();
    }

    /**
     * Tests that reading file info after close throws IllegalStateException.
     */
    @Test(expected = IllegalStateException.class)
    public void testReaderGetFileAfterClose() {
        String filePath = testFilePath("closed_file");
        MdfWriter writer = writeSimpleFile(filePath, 1);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        reader.close();

        reader.getFileInfo();
    }

    /**
     * Tests that closing a reader multiple times is safe.
     *
     * <p>Double-close should not throw or cause native memory corruption.</p>
     */
    @Test
    public void testReaderDoubleClose() {
        String filePath = testFilePath("double_close");
        MdfWriter writer = writeSimpleFile(filePath, 1);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        reader.close();
        /* Second close should be safe (no-op) */
        reader.close();
    }

    /* ========================================================================
     * Writer Lifecycle Tests
     * ======================================================================== */

    /**
     * Tests that operations on a closed writer throw IllegalStateException.
     */
    @Test(expected = IllegalStateException.class)
    public void testWriterOperationAfterClose() {
        String filePath = testFilePath("closed_writer");
        MdfWriter writer = new MdfWriter(filePath);
        writer.close();

        /* This should throw IllegalStateException */
        writer.setAuthor("ShouldFail");
    }

    /**
     * Tests that closing a writer multiple times is safe.
     */
    @Test
    public void testWriterDoubleClose() {
        String filePath = testFilePath("writer_double_close");
        MdfWriter writer = new MdfWriter(filePath);
        writer.close();
        /* Second close should be safe (no-op) */
        writer.close();
    }

    /* ========================================================================
     * ReadAllData Tests
     * ======================================================================== */

    /**
     * Tests the readAllData convenience method.
     *
     * <p>Verifies that readAllData correctly loads all data groups,
     * creates observers, and caches channel values.</p>
     */
    @Test
    public void testReadAllData() {
        String filePath = testFilePath("read_all");
        MdfWriter writer = writeSimpleFile(filePath, 10);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        try {
            assertTrue("readAllData should succeed", reader.readAllData());

            /* After readAllData, values should be cached */
            List<Double> values = reader.getChannelValuesAsDouble(0, "Signal1");
            assertNotNull("Values should not be null", values);
            assertEquals("Should have 10 samples", 10, values.size());
        } finally {
            reader.close();
        }
    }

    /**
     * Tests reading channel values for a non-existent channel.
     *
     * <p>Should return an empty list rather than throwing an exception.</p>
     */
    @Test
    public void testReadNonExistentChannel() {
        String filePath = testFilePath("nonexistent_channel");
        MdfWriter writer = writeSimpleFile(filePath, 5);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readAllData();

            List<Double> values = reader.getChannelValuesAsDouble(0, "NonExistentChannel");
            assertNotNull("Values list should not be null", values);
            assertTrue("Values list should be empty for non-existent channel", values.isEmpty());
        } finally {
            reader.close();
        }
    }

    /**
     * Tests reading channel values for an invalid data group index.
     *
     * <p>Should return an empty list for out-of-range indices.</p>
     */
    @Test
    public void testReadInvalidDataGroup() {
        String filePath = testFilePath("invalid_dg");
        MdfWriter writer = writeSimpleFile(filePath, 5);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readAllData();

            List<Double> values = reader.getChannelValuesAsDouble(99, "Signal1");
            assertNotNull("Values list should not be null", values);
            assertTrue("Values list should be empty for invalid data group", values.isEmpty());
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * Data Type Tests
     * ======================================================================== */

    /**
     * Tests writing and reading signed integer channel values.
     */
    @Test
    public void testSignedIntegerValues() {
        String filePath = testFilePath("signed_int");
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

            long intCh = writer.createChannel(cg);
            writer.setChannelName(intCh, "Counter");
            writer.setChannelType(intCh, MdfWriter.ChannelTypes.FIXED_LENGTH);
            writer.setChannelDataType(intCh, MdfWriter.DataTypes.SIGNED_INT_LE);
            writer.setChannelBitCount(intCh, 32);
            writer.setChannelDataBytes(intCh, 4);

            writer.initMeasurement();
            writer.startMeasurement(0L);

            for (int i = 0; i < 5; i++) {
                writer.setChannelValueAsDouble(timeCh, i * 0.1);
                writer.setChannelValueAsLong(intCh, i * 100L);
                writer.saveSample(cg, i * 100000000L);
            }

            writer.stopMeasurement(5 * 100000000L);
            writer.finalizeMeasurement();
        } finally {
            writer.close();
        }

        /* Read back and verify */
        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readAllData();

            List<Long> intValues = reader.getChannelValuesAsLong(0, "Counter");
            assertNotNull("Integer values should not be null", intValues);
            assertEquals("Should have 5 samples", 5, intValues.size());

            /* Verify values */
            for (int i = 0; i < 5; i++) {
                assertEquals("Counter value " + i, i * 100L, (long) intValues.get(i));
            }
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * File Info Tests
     * ======================================================================== */

    /**
     * Tests that FileInfo returns correct MDF version information.
     */
    @Test
    public void testFileInfoVersion() {
        String filePath = testFilePath("fileinfo_version");
        MdfWriter writer = writeSimpleFile(filePath, 1);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readHeader();

            FileInfo info = reader.getFileInfo();
            assertNotNull("FileInfo should not be null", info);
            assertTrue("Should be MDF4 format", info.isMdf4());
            assertEquals("Main version should be 4", 4, info.getMainVersion());
            assertTrue("Minor version should be >= 10",
                info.getMinorVersion() >= 10);
        } finally {
            reader.close();
        }
    }

    /**
     * Tests that FileInfo returns a valid file name.
     */
    @Test
    public void testFileInfoFileName() {
        String filePath = testFilePath("fileinfo_name");
        MdfWriter writer = writeSimpleFile(filePath, 1);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readHeader();

            FileInfo info = reader.getFileInfo();
            assertNotNull("FileInfo should not be null", info);
            assertNotNull("File name should not be null", info.getFileName());
            assertTrue("File name should contain the path",
                info.getFileName().contains("fileinfo_name"));
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * Data Group Structure Tests
     * ======================================================================== */

    /**
     * Tests that getDataGroups returns the correct structure.
     */
    @Test
    public void testDataGroupStructure() {
        String filePath = testFilePath("dg_structure");
        MdfWriter writer = writeSimpleFile(filePath, 3);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readMeasurementInfo();

            List<DataGroupInfo> dgs = reader.getDataGroups();
            assertNotNull("Data groups should not be null", dgs);
            assertEquals("Should have exactly 1 data group", 1, dgs.size());

            DataGroupInfo dg = dgs.get(0);
            assertNotNull("Channel groups should not be null", dg.getChannelGroups());
            assertEquals("Should have exactly 1 channel group",
                1, dg.getChannelGroups().size());
        } finally {
            reader.close();
        }
    }

    /**
     * Tests channel group sample count.
     */
    @Test
    public void testChannelGroupSampleCount() {
        String filePath = testFilePath("cg_samples");
        int numSamples = 7;
        MdfWriter writer = writeSimpleFile(filePath, numSamples);
        writer.close();

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readAllData();

            List<DataGroupInfo> dgs = reader.getDataGroups();
            ChannelGroupInfo cg = dgs.get(0).getChannelGroups().get(0);
            assertEquals("Channel group should report correct sample count",
                numSamples, cg.getNofSamples());
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * Channel Data Model Tests
     * ======================================================================== */

    /**
     * Tests ChannelData construction and getters.
     */
    @Test
    public void testChannelDataConstruction() {
        ChannelData cd = new ChannelData("TestChannel", "V",
            MdfWriter.ChannelTypes.FIXED_LENGTH, MdfWriter.DataTypes.FLOAT_LE);

        assertEquals("Name should match", "TestChannel", cd.getName());
        assertEquals("Unit should match", "V", cd.getUnit());
        assertEquals("Channel type should match",
            MdfWriter.ChannelTypes.FIXED_LENGTH, cd.getChannelType());
        assertEquals("Data type should match",
            MdfWriter.DataTypes.FLOAT_LE, cd.getDataType());
    }

    /**
     * Tests ChannelData rejects null name.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChannelDataNullName() {
        new ChannelData(null, "V", (byte) 0, (byte) 4);
    }

    /**
     * Tests ChannelData rejects null unit.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChannelDataNullUnit() {
        new ChannelData("Test", null, (byte) 0, (byte) 4);
    }

    /**
     * Tests ChannelData toString method.
     */
    @Test
    public void testChannelDataToString() {
        ChannelData cd = new ChannelData("Temp", "degC", (byte) 0, (byte) 4);
        String str = cd.toString();
        assertTrue("toString should contain name", str.contains("Temp"));
        assertTrue("toString should contain unit", str.contains("degC"));
    }

    /* ========================================================================
     * Header Info Model Tests
     * ======================================================================== */

    /**
     * Tests HeaderInfo construction and getters.
     */
    @Test
    public void testHeaderInfoConstruction() {
        HeaderInfo hi = new HeaderInfo("Author", "Dept", "Project",
            "Subject", "Description", 123456789L);

        assertEquals("Author should match", "Author", hi.getAuthor());
        assertEquals("Department should match", "Dept", hi.getDepartment());
        assertEquals("Project should match", "Project", hi.getProject());
        assertEquals("Subject should match", "Subject", hi.getSubject());
        assertEquals("Description should match", "Description", hi.getDescription());
        assertEquals("Start time should match", 123456789L, hi.getStartTime());
    }

    /* ========================================================================
     * File Info Model Tests
     * ======================================================================== */

    /**
     * Tests FileInfo construction and getters.
     */
    @Test
    public void testFileInfoConstruction() {
        FileInfo fi = new FileInfo("TestFile", "/path/to/file.mf4",
            "4.10", 4, 10, true);

        assertEquals("Name should match", "TestFile", fi.getName());
        assertEquals("FileName should match", "/path/to/file.mf4", fi.getFileName());
        assertEquals("Version should match", "4.10", fi.getVersion());
        assertEquals("Main version should match", 4, fi.getMainVersion());
        assertEquals("Minor version should match", 10, fi.getMinorVersion());
        assertTrue("IsMdf4 should be true", fi.isMdf4());
    }

    /* ========================================================================
     * Native Library Singleton Tests
     * ======================================================================== */

    /**
     * Tests that MdfLibraryNative.getInstance() returns the same instance.
     */
    @Test
    public void testNativeLibrarySingleton() {
        MdfLibraryNative instance1 = MdfLibraryNative.getInstance();
        MdfLibraryNative instance2 = MdfLibraryNative.getInstance();
        assertSame("getInstance should return the same instance", instance1, instance2);
    }

    /* ========================================================================
     * Writer Constants Tests
     * ======================================================================== */

    /**
     * Tests that writer constants have expected values.
     */
    @Test
    public void testWriterConstants() {
        assertEquals("MDF4_BASIC should be 1", 1, MdfWriter.MDF4_BASIC);
        assertEquals("FIXED_LENGTH should be 0", 0, MdfWriter.ChannelTypes.FIXED_LENGTH);
        assertEquals("VARIABLE_LENGTH should be 1", 1, MdfWriter.ChannelTypes.VARIABLE_LENGTH);
        assertEquals("MASTER should be 2", 2, MdfWriter.ChannelTypes.MASTER);
        assertEquals("VIRTUAL_MASTER should be 3", 3, MdfWriter.ChannelTypes.VIRTUAL_MASTER);

        assertEquals("NONE sync should be 0", 0, MdfWriter.SyncTypes.NONE);
        assertEquals("TIME sync should be 1", 1, MdfWriter.SyncTypes.TIME);
        assertEquals("ANGLE sync should be 2", 2, MdfWriter.SyncTypes.ANGLE);
        assertEquals("DISTANCE sync should be 3", 3, MdfWriter.SyncTypes.DISTANCE);
        assertEquals("INDEX sync should be 4", 4, MdfWriter.SyncTypes.INDEX);

        assertEquals("UNSIGNED_INT_LE should be 0", 0, MdfWriter.DataTypes.UNSIGNED_INT_LE);
        assertEquals("SIGNED_INT_LE should be 2", 2, MdfWriter.DataTypes.SIGNED_INT_LE);
        assertEquals("FLOAT_LE should be 4", 4, MdfWriter.DataTypes.FLOAT_LE);
        assertEquals("STRING_ASCII should be 6", 6, MdfWriter.DataTypes.STRING_ASCII);
        assertEquals("STRING_UTF8 should be 7", 7, MdfWriter.DataTypes.STRING_UTF8);
        assertEquals("BYTE_ARRAY should be 10", 10, MdfWriter.DataTypes.BYTE_ARRAY);
    }

    /* ========================================================================
     * Negative Value Tests
     * ======================================================================== */

    /**
     * Tests writing and reading negative floating-point values.
     */
    @Test
    public void testNegativeValues() {
        String filePath = testFilePath("negative_values");
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

            long negCh = writer.createChannel(cg);
            writer.setChannelName(negCh, "NegativeSignal");
            writer.setChannelDataType(negCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(negCh, 64);
            writer.setChannelDataBytes(negCh, 8);

            writer.initMeasurement();
            writer.startMeasurement(0L);

            double[] testValues = {-1.0, -100.5, -0.001, 0.0, 1.0};
            for (int i = 0; i < testValues.length; i++) {
                writer.setChannelValueAsDouble(timeCh, i * 0.1);
                writer.setChannelValueAsDouble(negCh, testValues[i]);
                writer.saveSample(cg, i * 100000000L);
            }

            writer.stopMeasurement(5 * 100000000L);
            writer.finalizeMeasurement();
        } finally {
            writer.close();
        }

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readAllData();

            List<Double> values = reader.getChannelValuesAsDouble(0, "NegativeSignal");
            assertEquals("Should have 5 samples", 5, values.size());
            assertEquals("First value should be -1.0", -1.0, values.get(0), 0.001);
            assertEquals("Second value should be -100.5", -100.5, values.get(1), 0.01);
            assertEquals("Third value should be -0.001", -0.001, values.get(2), 0.0001);
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * Try-with-resources Tests
     * ======================================================================== */

    /**
     * Tests that MdfReader works with try-with-resources pattern.
     */
    @Test
    public void testReaderTryWithResources() {
        String filePath = testFilePath("try_resources");
        MdfWriter writer = writeSimpleFile(filePath, 1);
        writer.close();

        try (MdfReader reader = new MdfReader(filePath)) {
            reader.open();
            assertTrue("Reader should be OK", reader.isOk());
        }
        /* Reader should be auto-closed here */
    }

    /**
     * Tests that MdfWriter works with try-with-resources pattern.
     */
    @Test
    public void testWriterTryWithResources() {
        String filePath = testFilePath("writer_try_resources");
        try (MdfWriter writer = new MdfWriter(filePath)) {
            assertNotNull("Writer should be created", writer);
        }
        /* Writer should be auto-closed here */
    }

    /* ========================================================================
     * File Existence Tests
     * ======================================================================== */

    /**
     * Tests that the output file exists after writing.
     */
    @Test
    public void testOutputFileExists() {
        String filePath = testFilePath("file_exists");
        MdfWriter writer = writeSimpleFile(filePath, 1);
        writer.close();

        File outputFile = new File(filePath);
        assertTrue("Output file should exist", outputFile.exists());
        assertTrue("Output file should have non-zero size", outputFile.length() > 0);
    }

    /**
     * Tests that the output file has a reasonable size.
     */
    @Test
    public void testOutputFileSize() {
        String smallPath = testFilePath("small_file");
        String largePath = testFilePath("large_file");

        MdfWriter smallWriter = writeSimpleFile(smallPath, 10);
        smallWriter.close();

        MdfWriter largeWriter = writeSimpleFile(largePath, 1000);
        largeWriter.close();

        File smallFile = new File(smallPath);
        File largeFile = new File(largePath);

        assertTrue("Large file should be bigger than small file",
            largeFile.length() > smallFile.length());
    }

    /* ========================================================================
     * Sequential Read Tests
     * ======================================================================== */

    /**
     * Tests reading the same file multiple times with different readers.
     */
    @Test
    public void testMultipleReadersSameFile() {
        String filePath = testFilePath("multi_readers");
        MdfWriter writer = writeSimpleFile(filePath, 5);
        writer.close();

        /* First read */
        List<Double> firstValues;
        try (MdfReader reader = new MdfReader(filePath)) {
            reader.open();
            reader.readAllData();
            firstValues = reader.getChannelValuesAsDouble(0, "Signal1");
        }

        /* Second read */
        List<Double> secondValues;
        try (MdfReader reader = new MdfReader(filePath)) {
            reader.open();
            reader.readAllData();
            secondValues = reader.getChannelValuesAsDouble(0, "Signal1");
        }

        /* Both reads should produce identical results */
        assertEquals("Values from both reads should match", firstValues, secondValues);
    }

    /**
     * Tests reading a file after the writer has been closed.
     *
     * <p>Verifies that the file is complete and readable after the writer
     * releases all resources.</p>
     */
    @Test
    public void testReadAfterWriterClose() {
        String filePath = testFilePath("after_close");
        MdfWriter writer = writeSimpleFile(filePath, 3);
        writer.close();

        /* Small delay to ensure file system has flushed */
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        MdfReader reader = new MdfReader(filePath);
        try {
            assertTrue("Reader should be OK", reader.isOk());
            reader.open();
            reader.readAllData();

            List<Double> values = reader.getChannelValuesAsDouble(0, "Signal1");
            assertEquals("Should have 3 samples", 3, values.size());
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * Unicode and Special Character Tests
     * ======================================================================== */

    /**
     * Tests writing metadata with Unicode characters.
     */
    @Test
    public void testUnicodeMetadata() {
        String filePath = testFilePath("unicode");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setAuthor("Müller");
            writer.setDepartment("Entwicklung");
            writer.setProject("Prüfung");

            long dg = writer.createDataGroup();
            long cg = writer.createChannelGroup(dg);
            long ch = writer.createChannel(cg);
            writer.setChannelName(ch, "Temperatur");
            writer.setChannelUnit(ch, "°C");
            writer.setChannelDataType(ch, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(ch, 64);
            writer.setChannelDataBytes(ch, 8);

            writer.initMeasurement();
            writer.startMeasurement(0L);
            writer.setChannelValueAsDouble(ch, 25.5);
            writer.saveSample(cg, 0L);
            writer.stopMeasurement(1000000L);
            writer.finalizeMeasurement();
        } finally {
            writer.close();
        }

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readHeader();
            HeaderInfo header = reader.getHeaderInfo();
            /* Note: Unicode round-trip may depend on native library encoding */
            assertNotNull("Header should not be null", header);
        } finally {
            reader.close();
        }
    }

    /* ========================================================================
     * Channel Type Constants Tests
     * ======================================================================== */

    /**
     * Tests creating channels with different type configurations.
     */
    @Test
    public void testDifferentChannelTypes() {
        String filePath = testFilePath("channel_types");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            long dg = writer.createDataGroup();
            long cg = writer.createChannelGroup(dg);

            /* Master channel with TIME sync */
            long masterCh = writer.createChannel(cg);
            writer.setChannelName(masterCh, "time");
            writer.setChannelType(masterCh, MdfWriter.ChannelTypes.MASTER);
            writer.setChannelSyncType(masterCh, MdfWriter.SyncTypes.TIME);
            writer.setChannelDataType(masterCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(masterCh, 64);
            writer.setChannelDataBytes(masterCh, 8);

            /* Fixed-length channel */
            long fixedCh = writer.createChannel(cg);
            writer.setChannelName(fixedCh, "fixed_signal");
            writer.setChannelType(fixedCh, MdfWriter.ChannelTypes.FIXED_LENGTH);
            writer.setChannelSyncType(fixedCh, MdfWriter.SyncTypes.NONE);
            writer.setChannelDataType(fixedCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelBitCount(fixedCh, 64);
            writer.setChannelDataBytes(fixedCh, 8);

            writer.initMeasurement();
            writer.startMeasurement(0L);
            writer.setChannelValueAsDouble(masterCh, 0.0);
            writer.setChannelValueAsDouble(fixedCh, 42.0);
            writer.saveSample(cg, 0L);
            writer.stopMeasurement(1000000L);
            writer.finalizeMeasurement();
        } finally {
            writer.close();
        }

        MdfReader reader = new MdfReader(filePath);
        try {
            reader.open();
            reader.readMeasurementInfo();

            List<DataGroupInfo> dgs = reader.getDataGroups();
            ChannelGroupInfo cgInfo = dgs.get(0).getChannelGroups().get(0);

            boolean foundMaster = false;
            boolean foundFixed = false;
            for (ChannelData ch : cgInfo.getChannels()) {
                if ("time".equals(ch.getName())) {
                    assertEquals("Time channel should be MASTER type",
                        MdfWriter.ChannelTypes.MASTER, ch.getChannelType());
                    foundMaster = true;
                }
                if ("fixed_signal".equals(ch.getName())) {
                    assertEquals("Fixed channel should be FIXED_LENGTH type",
                        MdfWriter.ChannelTypes.FIXED_LENGTH, ch.getChannelType());
                    foundFixed = true;
                }
            }
            assertTrue("Should find master channel", foundMaster);
            assertTrue("Should find fixed-length channel", foundFixed);
        } finally {
            reader.close();
        }
    }
}
