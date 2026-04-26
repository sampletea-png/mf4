package com.mdflib.service;

import com.huawei.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI;
import com.mdflib.model.*;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

/**
 * Simple load test verifying basic JNI library initialization and operations.
 *
 * <p>This test class provides a quick smoke test for the JNI native library,
 * verifying that:</p>
 * <ul>
 *   <li>The native library singleton can be obtained</li>
 *   <li>A basic write-then-read cycle works end-to-end</li>
 * </ul>
 *
 * @author mdflib-java contributors
 * @version 1.0.0
 * @since 1.0.0
 */
public class SimpleLoadTest {

    /**
     * Tests that the native library singleton instance can be obtained.
     *
     * <p>This verifies that the JNI library was successfully loaded
     * and the singleton pattern works correctly.</p>
     */
    @Test
    public void testLibraryLoads() {
        MdfLibraryNativeJNI instance = MdfLibraryNativeJNI.getInstance();
        assertNotNull("Native library instance should not be null", instance);
    }

    /**
     * Tests a minimal write-then-read cycle using high-level API.
     *
     * <p>Creates a simple MDF4 file with one time channel and one signal
     * channel, writes 5 samples, then reads them back and verifies
     * the data integrity.</p>
     */
    @Test
    public void testWriteThenReadBasic() {
        String file = "simple_jni_test_" + System.nanoTime() + ".mf4";
        MdfLibraryNativeJNI N = MdfLibraryNativeJNI.getInstance();

        /* === WRITE PHASE === */
        long writer = N.MdfWriterInit(1, file);
        assertNotEquals("Writer pointer should be non-zero", 0, writer);

        /* Create data structure: DG -> CG -> Channels */
        long dg = N.MdfWriterCreateDataGroup(writer);
        long cg = N.MdfDataGroupCreateChannelGroup(dg);

        /* Configure time channel (master) */
        long timeCh = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(timeCh, "t");
        N.MdfChannelSetType(timeCh, (byte) 2);
        N.MdfChannelSetSync(timeCh, (byte) 1);
        N.MdfChannelSetDataType(timeCh, (byte) 4);
        N.MdfChannelSetDataBytes(timeCh, 8);

        /* Configure signal channel */
        long sigCh = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(sigCh, "Sig");
        N.MdfChannelSetType(sigCh, (byte) 0);
        N.MdfChannelSetDataType(sigCh, (byte) 4);
        N.MdfChannelSetDataBytes(sigCh, 8);

        /* Write samples */
        assertTrue("InitMeasurement should succeed", N.MdfWriterInitMeasurement(writer));
        N.MdfWriterStartMeasurement(writer, 100000000L);

        int n = 5;
        for (int i = 0; i < n; i++) {
            N.MdfChannelSetChannelValueAsFloat(timeCh, (double) i, 1, 0L);
            N.MdfChannelSetChannelValueAsFloat(sigCh, (double) i * 10.0, 1, 0L);
            N.MdfWriterSaveSample(writer, cg, 100000000L + (long) i * 10000L);
        }

        N.MdfWriterStopMeasurement(writer, 100000000L + n * 10000L);
        assertTrue("FinalizeMeasurement should succeed", N.MdfWriterFinalizeMeasurement(writer));
        N.MdfWriterUnInit(writer);

        /* === READ PHASE === */
        long reader = N.MdfReaderInit(file);
        assertNotEquals("Reader pointer should be non-zero", 0, reader);
        assertTrue("Reader should be OK", N.MdfReaderIsOk(reader));
        assertTrue("Open should succeed", N.MdfReaderOpen(reader));
        assertTrue("ReadHeader should succeed", N.MdfReaderReadHeader(reader));
        assertTrue("ReadMeasurementInfo should succeed", N.MdfReaderReadMeasurementInfo(reader));
        assertTrue("ReadEverythingButData should succeed", N.MdfReaderReadEverythingButData(reader));

        /* Verify file structure */
        long filePtr = N.MdfReaderGetFile(reader);
        assertNotEquals("File pointer should be non-zero", 0, filePtr);

        long dgCount = N.MdfFileGetDataGroups(filePtr, null);
        assertTrue("Should have at least 1 data group", dgCount >= 1);

        long dgRead = N.MdfReaderGetDataGroup(reader, 0);
        assertNotEquals("Data group pointer should be non-zero", 0, dgRead);

        long cgCount = N.MdfDataGroupGetChannelGroups(dgRead, null);
        assertTrue("Should have at least 1 channel group", cgCount >= 1);

        /* Read channel data */
        long[] cgPtrs = new long[(int) cgCount];
        N.MdfDataGroupGetChannelGroups(dgRead, cgPtrs);

        assertTrue("ReadData should succeed", N.MdfReaderReadData(reader, dgRead));

        /* Create observer for signal channel */
        long observer = N.MdfChannelObserverCreateByChannelName(dgRead, "Sig");
        assertNotEquals("Observer should be non-zero", 0, observer);

        long samples = N.MdfChannelObserverGetNofSamples(observer);

        /* Verify signal values */
        double[] valRef = new double[2];
        for (long s = 0; s < samples; s++) {
            boolean ok = N.MdfChannelObserverGetEngValueAsFloat(observer, s, valRef);
            assertTrue("Should get value for sample " + s, ok);
            assertEquals("Sample " + s + " value",
                s * 10.0, valRef[0], 0.001);
        }

        N.MdfChannelObserverUnInit(observer);
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);

        /* Clean up test file */
        new java.io.File(file).delete();
    }
}
