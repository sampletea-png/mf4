package com.mdflib.service;

import com.mdflib.jna.MdfLibraryNative;
import com.sun.jna.Pointer;
import org.junit.Test;
import static org.junit.Assert.*;

public class SimpleLoadTest {
    @Test
    public void testLibraryLoads() {
        MdfLibraryNative inst = MdfLibraryNative.INSTANCE;
        assertNotNull(inst);
    }

    @Test
    public void testWriterCreateAndInit() {
        Pointer writer = MdfLibraryNative.INSTANCE.MdfWriterInit(1, "test_basic.mf4");
        assertNotNull(writer);

        Pointer header = MdfLibraryNative.INSTANCE.MdfWriterGetHeader(writer);
        assertNotNull(header);
        MdfLibraryNative.INSTANCE.MdfHeaderSetAuthor(header, "Test");

        Pointer dg = MdfLibraryNative.INSTANCE.MdfWriterCreateDataGroup(writer);
        assertNotNull(dg);

        Pointer cg = MdfLibraryNative.INSTANCE.MdfDataGroupCreateChannelGroup(dg);
        assertNotNull(cg);

        Pointer timeCh = MdfLibraryNative.INSTANCE.MdfChannelGroupCreateChannel(cg);
        assertNotNull(timeCh);
        MdfLibraryNative.INSTANCE.MdfChannelSetName(timeCh, "t");
        MdfLibraryNative.INSTANCE.MdfChannelSetType(timeCh, (byte) 2);
        MdfLibraryNative.INSTANCE.MdfChannelSetSync(timeCh, (byte) 1);
        MdfLibraryNative.INSTANCE.MdfChannelSetDataType(timeCh, (byte) 4);
        MdfLibraryNative.INSTANCE.MdfChannelSetBitCount(timeCh, 64);

        Pointer sigCh = MdfLibraryNative.INSTANCE.MdfChannelGroupCreateChannel(cg);
        assertNotNull(sigCh);
        MdfLibraryNative.INSTANCE.MdfChannelSetName(sigCh, "Signal");
        MdfLibraryNative.INSTANCE.MdfChannelSetDataType(sigCh, (byte) 4);
        MdfLibraryNative.INSTANCE.MdfChannelSetBitCount(sigCh, 64);

        boolean initOk = MdfLibraryNative.INSTANCE.MdfWriterInitMeasurement(writer);
        assertTrue("InitMeasurement should succeed", initOk);

        MdfLibraryNative.INSTANCE.MdfWriterStartMeasurement(writer, 0);

        for (int i = 0; i < 5; i++) {
            MdfLibraryNative.INSTANCE.MdfChannelSetChannelValueAsFloat(timeCh, (double) i, (byte) 1, 0);
            MdfLibraryNative.INSTANCE.MdfChannelSetChannelValueAsFloat(sigCh, (double) i * 10.0, (byte) 1, 0);
            MdfLibraryNative.INSTANCE.MdfWriterSaveSample(writer, cg, (long) i);
        }

        MdfLibraryNative.INSTANCE.MdfWriterStopMeasurement(writer, 5000000);
        boolean finalizeOk = MdfLibraryNative.INSTANCE.MdfWriterFinalizeMeasurement(writer);
        assertTrue("FinalizeMeasurement should succeed", finalizeOk);

        MdfLibraryNative.INSTANCE.MdfWriterUnInit(writer);
    }

    @Test
    public void testWriterThenReader() {
        String file = "test_read_write.mf4";

        Pointer writer = MdfLibraryNative.INSTANCE.MdfWriterInit(1, file);
        assertNotNull(writer);

        Pointer dg = MdfLibraryNative.INSTANCE.MdfWriterCreateDataGroup(writer);
        assertNotNull(dg);
        Pointer cg = MdfLibraryNative.INSTANCE.MdfDataGroupCreateChannelGroup(dg);
        assertNotNull(cg);

        Pointer timeCh = MdfLibraryNative.INSTANCE.MdfChannelGroupCreateChannel(cg);
        assertNotNull(timeCh);
        MdfLibraryNative.INSTANCE.MdfChannelSetName(timeCh, "t");
        MdfLibraryNative.INSTANCE.MdfChannelSetType(timeCh, (byte) 2);
        MdfLibraryNative.INSTANCE.MdfChannelSetSync(timeCh, (byte) 1);
        MdfLibraryNative.INSTANCE.MdfChannelSetDataType(timeCh, (byte) 4);
        MdfLibraryNative.INSTANCE.MdfChannelSetBitCount(timeCh, 64);

        Pointer sigCh = MdfLibraryNative.INSTANCE.MdfChannelGroupCreateChannel(cg);
        assertNotNull(sigCh);
        MdfLibraryNative.INSTANCE.MdfChannelSetName(sigCh, "Sig");
        MdfLibraryNative.INSTANCE.MdfChannelSetDataType(sigCh, (byte) 4);
        MdfLibraryNative.INSTANCE.MdfChannelSetBitCount(sigCh, 64);

        assertTrue(MdfLibraryNative.INSTANCE.MdfWriterInitMeasurement(writer));
        MdfLibraryNative.INSTANCE.MdfWriterStartMeasurement(writer, 0);

        int n = 10;
        for (int i = 0; i < n; i++) {
            MdfLibraryNative.INSTANCE.MdfChannelSetChannelValueAsFloat(timeCh, (double) i, (byte) 1, 0);
            MdfLibraryNative.INSTANCE.MdfChannelSetChannelValueAsFloat(sigCh, (double) i * 10.0, (byte) 1, 0);
            MdfLibraryNative.INSTANCE.MdfWriterSaveSample(writer, cg, (long) i);
        }

        MdfLibraryNative.INSTANCE.MdfWriterStopMeasurement(writer, n * 1000000);
        assertTrue(MdfLibraryNative.INSTANCE.MdfWriterFinalizeMeasurement(writer));
        MdfLibraryNative.INSTANCE.MdfWriterUnInit(writer);

        Pointer reader = MdfLibraryNative.INSTANCE.MdfReaderInit(file);
        assertNotNull(reader);
        assertTrue(MdfLibraryNative.INSTANCE.MdfReaderIsOk(reader));
        assertTrue(MdfLibraryNative.INSTANCE.MdfReaderOpen(reader));
        assertTrue(MdfLibraryNative.INSTANCE.MdfReaderReadHeader(reader));
        assertTrue(MdfLibraryNative.INSTANCE.MdfReaderReadMeasurementInfo(reader));

        Pointer filePtr = MdfLibraryNative.INSTANCE.MdfReaderGetFile(reader);
        assertNotNull(filePtr);

        long dgCount = MdfLibraryNative.INSTANCE.MdfFileGetDataGroups(filePtr, null);
        assertEquals("Should have 1 data group", 1, dgCount);

        Pointer dgRead = MdfLibraryNative.INSTANCE.MdfReaderGetDataGroup(reader, 0);
        assertNotNull(dgRead);
        assertTrue(MdfLibraryNative.INSTANCE.MdfReaderReadData(reader, dgRead));

        Pointer observer = MdfLibraryNative.INSTANCE.MdfChannelObserverCreateByChannelName(dgRead, "Sig");
        assertNotNull("Observer for Sig should not be null", observer);

        long samples = MdfLibraryNative.INSTANCE.MdfChannelObserverGetNofSamples(observer);
        assertEquals("Should have 10 samples", n, samples);

        MdfLibraryNative.INSTANCE.MdfChannelObserverUnInit(observer);
        MdfLibraryNative.INSTANCE.MdfReaderClose(reader);
        MdfLibraryNative.INSTANCE.MdfReaderUnInit(reader);
    }
}
