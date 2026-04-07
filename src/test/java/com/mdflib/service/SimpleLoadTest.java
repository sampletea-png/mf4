package com.mdflib.service;

import com.mdflib.jna.MdfLibraryNative;
import com.mdflib.model.*;
import com.sun.jna.Pointer;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class SimpleLoadTest {
    @Test
    public void testLibraryLoads() {
        assertNotNull(MdfLibraryNative.INSTANCE);
    }

    @Test
    public void testWriteThenReadDebug() {
        String file = "debug_test_" + System.nanoTime() + ".mf4";
        MdfLibraryNative N = MdfLibraryNative.INSTANCE;

        System.out.println("=== WRITE ===");
        Pointer writer = N.MdfWriterInit(1, file);
        assertNotNull(writer);

        Pointer dg = N.MdfWriterCreateDataGroup(writer);
        Pointer cg = N.MdfDataGroupCreateChannelGroup(dg);

        Pointer timeCh = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(timeCh, "t");
        N.MdfChannelSetType(timeCh, (byte) 2);
        N.MdfChannelSetSync(timeCh, (byte) 1);
        N.MdfChannelSetDataType(timeCh, (byte) 4);
        N.MdfChannelSetDataBytes(timeCh, 8);

        Pointer sigCh = N.MdfChannelGroupCreateChannel(cg);
        N.MdfChannelSetName(sigCh, "Sig");
        N.MdfChannelSetType(sigCh, (byte) 0);
        N.MdfChannelSetDataType(sigCh, (byte) 4);
        N.MdfChannelSetDataBytes(sigCh, 8);

        assertTrue(N.MdfWriterInitMeasurement(writer));
        N.MdfWriterStartMeasurement(writer, 100000000L);

        int n = 5;
        for (int i = 0; i < n; i++) {
            N.MdfChannelSetChannelValueAsFloat(timeCh, (double) i, 1, 0L);
            N.MdfChannelSetChannelValueAsFloat(sigCh, (double) i * 10.0, 1, 0L);
            N.MdfWriterSaveSample(writer, cg, 100000000L + (long) i * 10000L);
        }

        N.MdfWriterStopMeasurement(writer, 100000000L + n * 10000L);
        assertTrue(N.MdfWriterFinalizeMeasurement(writer));
        N.MdfWriterUnInit(writer);

        System.out.println("=== READ ===");
        Pointer reader = N.MdfReaderInit(file);
        assertNotNull(reader);
        assertTrue(N.MdfReaderIsOk(reader));
        assertTrue(N.MdfReaderOpen(reader));
        assertTrue(N.MdfReaderReadHeader(reader));
        assertTrue(N.MdfReaderReadMeasurementInfo(reader));
        assertTrue(N.MdfReaderReadEverythingButData(reader));

        Pointer filePtr = N.MdfReaderGetFile(reader);
        assertNotNull(filePtr);

        long dgCount = N.MdfFileGetDataGroups(filePtr, null);
        System.out.println("DataGroup count: " + dgCount);
        assertTrue(dgCount >= 1);

        Pointer dgRead = N.MdfReaderGetDataGroup(reader, 0);
        assertNotNull(dgRead);

        long cgCount = N.MdfDataGroupGetChannelGroups(dgRead, null);
        System.out.println("ChannelGroup count: " + cgCount);

        Pointer[] cgPtrs = new Pointer[(int) cgCount];
        N.MdfDataGroupGetChannelGroups(dgRead, cgPtrs);
        for (int ci = 0; ci < cgPtrs.length; ci++) {
            final Pointer c = cgPtrs[ci];
            String cgName = getString(buf -> N.MdfChannelGroupGetName(c, buf));
            long samples = N.MdfChannelGroupGetNofSamples(c);
            System.out.println("CG[" + ci + "] name='" + cgName + "' samples=" + samples);

            long chCount = N.MdfChannelGroupGetChannels(c, null);
            System.out.println("  Channel count: " + chCount);
            Pointer[] chPtrs = new Pointer[(int) chCount];
            N.MdfChannelGroupGetChannels(c, chPtrs);
            for (int j = 0; j < chPtrs.length; j++) {
                final Pointer chPtr = chPtrs[j];
                String chName = getString(buf -> N.MdfChannelGetName(chPtr, buf));
                System.out.println("  Ch[" + j + "] name='" + chName + "'");
            }
        }

        System.out.println("Reading data...");
        assertTrue(N.MdfReaderReadData(reader, dgRead));

        Pointer observer = N.MdfChannelObserverCreateByChannelName(dgRead, "Sig");
        System.out.println("Observer for 'Sig': " + observer);
        assertNotNull(observer);

        long samples = N.MdfChannelObserverGetNofSamples(observer);
        System.out.println("Observer sample count: " + samples);

        com.sun.jna.ptr.DoubleByReference valRef = new com.sun.jna.ptr.DoubleByReference();
        com.sun.jna.ptr.LongByReference longRef = new com.sun.jna.ptr.LongByReference();
        for (long s = 0; s < samples; s++) {
            boolean ok = N.MdfChannelObserverGetChannelValueAsFloat(observer, s, valRef);
            boolean engOk = N.MdfChannelObserverGetEngValueAsFloat(observer, s, valRef);
            boolean sigOk = N.MdfChannelObserverGetChannelValueAsSigned(observer, s, longRef);
            System.out.println("  Sample " + s + ": floatOk=" + ok + " engOk=" + engOk + " sigOk=" + sigOk + " fval=" + valRef.getValue() + " lval=" + longRef.getValue());
        }

        N.MdfChannelObserverUnInit(observer);
        N.MdfReaderClose(reader);
        N.MdfReaderUnInit(reader);
        System.out.println("ALL PASSED!");
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
