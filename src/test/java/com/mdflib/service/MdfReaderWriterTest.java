package com.mdflib.service;

import com.mdflib.model.*;
import com.sun.jna.Pointer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

public class MdfReaderWriterTest {

    private String tempDir;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("mdf_test").toString();
    }

    @After
    public void tearDown() {
        deleteDirectory(new File(tempDir));
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectory(f);
                }
            }
        }
        dir.delete();
    }

    private String uniquePath(String baseName) {
        return Paths.get(tempDir, baseName + "_" + System.nanoTime() + ".mf4").toString();
    }

    private void writeSimpleMf4File(String filePath, String signalName, int numSamples) {
        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setAuthor("MdfLibJavaTest");
            writer.setDepartment("TestDept");
            writer.setProject("TestProject");

            Pointer dg = writer.createDataGroup();
            Pointer cg = writer.createChannelGroup(dg);

            Pointer timeCh = writer.createChannel(cg);
            writer.setChannelName(timeCh, "t");
            writer.setChannelType(timeCh, MdfWriter.ChannelTypes.MASTER);
            writer.setChannelSyncType(timeCh, MdfWriter.SyncTypes.TIME);
            writer.setChannelDataType(timeCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelDataBytes(timeCh, 8);

            Pointer sigCh = writer.createChannel(cg);
            writer.setChannelName(sigCh, signalName);
            writer.setChannelUnit(sigCh, "V");
            writer.setChannelType(sigCh, MdfWriter.ChannelTypes.FIXED_LENGTH);
            writer.setChannelDataType(sigCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelDataBytes(sigCh, 8);

            assertTrue(writer.initMeasurement());
            writer.startMeasurement(100000000L);

            for (int i = 0; i < numSamples; i++) {
                writer.setChannelValueAsDouble(timeCh, (double) i);
                writer.setChannelValueAsDouble(sigCh, i * 10.0);
                writer.saveSample(cg, 100000000L + (long) i * 10000L);
            }

            writer.stopMeasurement(100000000L + numSamples * 10000L);
            assertTrue(writer.finalizeMeasurement());
        } finally {
            writer.close();
        }
    }

    private MdfReader openAndReadAll(String filePath) {
        MdfReader reader = new MdfReader(filePath);
        assertTrue("Reader should be OK", reader.isOk());
        assertTrue("Reader should open", reader.open());
        assertTrue("Read header should succeed", reader.readHeader());
        assertTrue("Read measurement info should succeed", reader.readMeasurementInfo());
        assertTrue("Read all data should succeed", reader.readAllData());
        return reader;
    }

    @Test
    public void testWriterCreation() {
        String filePath = uniquePath("writer_create");
        MdfWriter writer = new MdfWriter(filePath);
        assertNotNull(writer);
        writer.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriterCreationNullPath() {
        new MdfWriter((String) null);
    }

    @Test
    public void testWriteAndReadHeader() {
        String filePath = uniquePath("header_test");

        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setAuthor("TestAuthor");
            writer.setDepartment("TestDepartment");
            writer.setProject("TestProject");
            writer.setSubject("TestSubject");
            writer.setDescription("TestDescription");

            Pointer dg = writer.createDataGroup();
            Pointer cg = writer.createChannelGroup(dg);

            Pointer timeCh = writer.createChannel(cg);
            writer.setChannelName(timeCh, "t");
            writer.setChannelType(timeCh, MdfWriter.ChannelTypes.MASTER);
            writer.setChannelSyncType(timeCh, MdfWriter.SyncTypes.TIME);
            writer.setChannelDataType(timeCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelDataBytes(timeCh, 8);

            assertTrue(writer.initMeasurement());
            writer.startMeasurement(100000000L);
            writer.setChannelValueAsDouble(timeCh, 0.0);
            writer.saveSample(cg, 100000000L);
            writer.stopMeasurement(200000000L);
            assertTrue(writer.finalizeMeasurement());
        } finally {
            writer.close();
        }

        MdfReader reader = openAndReadAll(filePath);
        try {
            HeaderInfo header = reader.getHeaderInfo();
            assertNotNull(header);
            assertEquals("TestAuthor", header.getAuthor());
            assertEquals("TestDepartment", header.getDepartment());
            assertEquals("TestProject", header.getProject());
            assertEquals("TestSubject", header.getSubject());
            assertEquals("TestDescription", header.getDescription());
            assertTrue(header.getStartTime() > 0);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testWriteAndReadFileInfo() {
        String filePath = uniquePath("fileinfo_test");
        writeSimpleMf4File(filePath, "signal1", 5);

        MdfReader reader = openAndReadAll(filePath);
        try {
            FileInfo info = reader.getFileInfo();
            assertNotNull(info);
            assertNotNull(info.getVersion());
            assertTrue(info.isMdf4());
            assertTrue(info.getMainVersion() >= 4);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testWriteAndReadDataGroups() {
        String filePath = uniquePath("dg_test");
        writeSimpleMf4File(filePath, "signal1", 5);

        MdfReader reader = openAndReadAll(filePath);
        try {
            List<DataGroupInfo> dgs = reader.getDataGroups();
            assertNotNull(dgs);
            assertFalse(dgs.isEmpty());
            assertTrue(dgs.size() >= 1);

            DataGroupInfo dg = dgs.get(0);
            assertNotNull(dg.getChannelGroups());
            assertFalse(dg.getChannelGroups().isEmpty());
        } finally {
            reader.close();
        }
    }

    @Test
    public void testWriteAndReadChannelValues() {
        String filePath = uniquePath("chvals_test");
        int numSamples = 10;
        writeSimpleMf4File(filePath, "signal1", numSamples);

        MdfReader reader = openAndReadAll(filePath);
        try {
            List<Double> sigVals = reader.getChannelValuesAsDouble(0, "signal1");
            assertNotNull(sigVals);
            assertEquals(numSamples, sigVals.size());
            for (int i = 0; i < numSamples; i++) {
                assertEquals(i * 10.0, sigVals.get(i), 0.001);
            }
        } finally {
            reader.close();
        }
    }

    @Test
    public void testWriteAndReadMultipleChannels() {
        String filePath = uniquePath("multi_ch_test");
        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setAuthor("MdfLibJavaTest");

            Pointer dg = writer.createDataGroup();
            Pointer cg = writer.createChannelGroup(dg);

            Pointer timeCh = writer.createChannel(cg);
            writer.setChannelName(timeCh, "t");
            writer.setChannelType(timeCh, MdfWriter.ChannelTypes.MASTER);
            writer.setChannelSyncType(timeCh, MdfWriter.SyncTypes.TIME);
            writer.setChannelDataType(timeCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelDataBytes(timeCh, 8);

            Pointer tempCh = writer.createChannel(cg);
            writer.setChannelName(tempCh, "temperature");
            writer.setChannelUnit(tempCh, "C");
            writer.setChannelType(tempCh, MdfWriter.ChannelTypes.FIXED_LENGTH);
            writer.setChannelDataType(tempCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelDataBytes(tempCh, 8);

            Pointer pressCh = writer.createChannel(cg);
            writer.setChannelName(pressCh, "pressure");
            writer.setChannelUnit(pressCh, "Pa");
            writer.setChannelType(pressCh, MdfWriter.ChannelTypes.FIXED_LENGTH);
            writer.setChannelDataType(pressCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelDataBytes(pressCh, 8);

            assertTrue(writer.initMeasurement());
            writer.startMeasurement(100000000L);

            int numSamples = 20;
            for (int i = 0; i < numSamples; i++) {
                writer.setChannelValueAsDouble(timeCh, (double) i);
                writer.setChannelValueAsDouble(tempCh, 20.0 + i * 0.5);
                writer.setChannelValueAsDouble(pressCh, 101325.0 + i * 100.0);
                writer.saveSample(cg, 100000000L + (long) i * 10000L);
            }

            writer.stopMeasurement(100000000L + numSamples * 10000L);
            assertTrue(writer.finalizeMeasurement());
        } finally {
            writer.close();
        }

        MdfReader reader = openAndReadAll(filePath);
        try {
            List<String> names = reader.getChannelNames();
            assertTrue(names.contains("t"));
            assertTrue(names.contains("temperature"));
            assertTrue(names.contains("pressure"));

            List<Double> tempVals = reader.getChannelValuesAsDouble(0, "temperature");
            assertEquals(20, tempVals.size());
            assertEquals(20.0, tempVals.get(0), 0.001);
            assertEquals(29.5, tempVals.get(19), 0.001);

            List<Double> pressVals = reader.getChannelValuesAsDouble(0, "pressure");
            assertEquals(20, pressVals.size());
            assertEquals(101325.0, pressVals.get(0), 0.01);
            assertEquals(103225.0, pressVals.get(19), 0.01);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testWriteAndReadChannelNames() {
        String filePath = uniquePath("chnames_test");
        writeSimpleMf4File(filePath, "my_signal", 3);

        MdfReader reader = openAndReadAll(filePath);
        try {
            List<String> names = reader.getChannelNames();
            assertNotNull(names);
            assertTrue("Should contain 't'", names.contains("t"));
            assertTrue("Should contain 'my_signal'", names.contains("my_signal"));
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReaderOnNonExistentFile() {
        String filePath = Paths.get(tempDir, "nonexistent.mf4").toString();
        MdfReader reader = new MdfReader(filePath);
        assertFalse("Open should fail for non-existent file", reader.open());
        reader.close();
    }

    @Test
    public void testWriteCompressedData() {
        String filePath = uniquePath("compressed_test");
        int numSamples = 50;

        MdfWriter writer = new MdfWriter(filePath);
        try {
            writer.setAuthor("CompressTest");
            writer.setCompressData(true);

            Pointer dg = writer.createDataGroup();
            Pointer cg = writer.createChannelGroup(dg);

            Pointer timeCh = writer.createChannel(cg);
            writer.setChannelName(timeCh, "t");
            writer.setChannelType(timeCh, MdfWriter.ChannelTypes.MASTER);
            writer.setChannelSyncType(timeCh, MdfWriter.SyncTypes.TIME);
            writer.setChannelDataType(timeCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelDataBytes(timeCh, 8);

            Pointer sigCh = writer.createChannel(cg);
            writer.setChannelName(sigCh, "compressed_signal");
            writer.setChannelUnit(sigCh, "V");
            writer.setChannelType(sigCh, MdfWriter.ChannelTypes.FIXED_LENGTH);
            writer.setChannelDataType(sigCh, MdfWriter.DataTypes.FLOAT_LE);
            writer.setChannelDataBytes(sigCh, 8);

            assertTrue(writer.initMeasurement());
            writer.startMeasurement(100000000L);

            for (int i = 0; i < numSamples; i++) {
                writer.setChannelValueAsDouble(timeCh, (double) i);
                writer.setChannelValueAsDouble(sigCh, i * 100.0);
                writer.saveSample(cg, 100000000L + (long) i * 10000L);
            }

            writer.stopMeasurement(100000000L + numSamples * 10000L);
            assertTrue(writer.finalizeMeasurement());
        } finally {
            writer.close();
        }

        MdfReader reader = openAndReadAll(filePath);
        try {
            List<Double> vals = reader.getChannelValuesAsDouble(0, "compressed_signal");
            assertNotNull(vals);
            assertEquals(numSamples, vals.size());
            for (int i = 0; i < numSamples; i++) {
                assertEquals(i * 100.0, vals.get(i), 0.001);
            }
        } finally {
            reader.close();
        }
    }

    @Test
    public void testWriteAndReadLargeDataset() {
        String filePath = uniquePath("large_test");
        int numSamples = 1000;
        writeSimpleMf4File(filePath, "large_signal", numSamples);

        MdfReader reader = openAndReadAll(filePath);
        try {
            List<Double> sigVals = reader.getChannelValuesAsDouble(0, "large_signal");
            assertEquals(numSamples, sigVals.size());
            assertEquals(0.0, sigVals.get(0), 0.001);
            assertEquals(9990.0, sigVals.get(999), 0.001);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadChannelGroupInfo() {
        String filePath = uniquePath("cginfo_test");
        writeSimpleMf4File(filePath, "sig1", 5);

        MdfReader reader = openAndReadAll(filePath);
        try {
            List<DataGroupInfo> dgs = reader.getDataGroups();
            assertFalse(dgs.isEmpty());

            DataGroupInfo dg = dgs.get(0);
            List<ChannelGroupInfo> cgs = dg.getChannelGroups();
            assertFalse(cgs.isEmpty());

            ChannelGroupInfo cg = cgs.get(0);
            assertNotNull(cg.getName());

            List<ChannelData> channels = cg.getChannels();
            assertTrue("Should have at least 2 channels", channels.size() >= 2);

            boolean foundTime = false;
            boolean foundSignal = false;
            for (ChannelData ch : channels) {
                if ("t".equals(ch.getName())) {
                    foundTime = true;
                }
                if ("sig1".equals(ch.getName())) {
                    foundSignal = true;
                    assertEquals("V", ch.getUnit());
                }
            }
            assertTrue("Time channel should exist", foundTime);
            assertTrue("Signal channel should exist", foundSignal);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadNonExistingChannel() {
        String filePath = uniquePath("nonexist_ch_test");
        writeSimpleMf4File(filePath, "real_signal", 5);

        MdfReader reader = openAndReadAll(filePath);
        try {
            List<Double> vals = reader.getChannelValuesAsDouble(0, "nonexistent_channel");
            assertNotNull(vals);
            assertTrue(vals.isEmpty());
        } finally {
            reader.close();
        }
    }

    @Test
    public void testMultipleWriteReadCycles() {
        for (int cycle = 0; cycle < 3; cycle++) {
            String filePath = uniquePath("cycle_" + cycle);
            String signalName = "signal_cycle_" + cycle;
            int numSamples = 5 + cycle * 2;

            writeSimpleMf4File(filePath, signalName, numSamples);

            MdfReader reader = openAndReadAll(filePath);
            try {
                List<Double> vals = reader.getChannelValuesAsDouble(0, signalName);
                assertNotNull(vals);
                assertEquals(numSamples, vals.size());

                for (int i = 0; i < numSamples; i++) {
                    assertEquals(i * 10.0, vals.get(i), 0.001);
                }

                HeaderInfo header = reader.getHeaderInfo();
                assertNotNull(header);
                assertEquals("MdfLibJavaTest", header.getAuthor());
            } finally {
                reader.close();
            }

            assertTrue(new File(filePath).delete());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testWriterOperationAfterClose() {
        String filePath = uniquePath("writer_closed");
        MdfWriter writer = new MdfWriter(filePath);
        writer.close();
        writer.setAuthor("should fail");
    }

    @Test(expected = IllegalStateException.class)
    public void testReaderOperationAfterClose() {
        String filePath = uniquePath("reader_closed");
        writeSimpleMf4File(filePath, "sig", 3);

        MdfReader reader = new MdfReader(filePath);
        reader.close();
        reader.isOk();
    }
}
