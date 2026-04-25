package com.mdflib.service;

import com.mdflib.jni.MdfLibraryNative;
import com.mdflib.model.*;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * High-level MDF file reader providing convenient access to MDF measurement data.
 *
 * <p>This class wraps the low-level JNI native calls into an easy-to-use API
 * for reading MDF (Measurement Data Format) files. It supports MDF versions 3
 * and 4, with full support for reading headers, channel metadata, and
 * measurement data samples.</p>
 *
 * <p>Typical usage pattern:</p>
 * <pre>
 *   MdfReader reader = new MdfReader("measurement.mf4");
 *   try {
 *       reader.open();
 *       reader.readHeader();
 *       reader.readMeasurementInfo();
 *       reader.readAllData();
 *
 *       FileInfo info = reader.getFileInfo();
 *       HeaderInfo header = reader.getHeaderInfo();
 *       List&lt;Double&gt; values = reader.getChannelValuesAsDouble(0, "temperature");
 *   } finally {
 *       reader.close();
 *   }
 * </pre>
 *
 * <p>Thread safety: This class is NOT thread-safe. Each instance should be
 * used from a single thread, or external synchronization must be applied.</p>
 *
 * <p>Resource management: Always call {@link #close()} when done to release
 * native resources. Using try-with-resources is recommended.</p>
 *
 * @author mdflib-java contributors
 * @version 1.0.0
 * @since 1.0.0
 * @see MdfWriter
 * @see MdfLibraryNative
 */
public class MdfReader implements Closeable {

    /** Native pointer to the underlying MdfReader C++ object. */
    private long readerPtr;

    /** Flag indicating whether this reader has been closed. */
    private boolean closed = false;

    /** Flag indicating whether all data has been loaded into cache. */
    private boolean dataLoaded = false;

    /**
     * Cached double-precision channel values keyed by channel name.
     * Populated by {@link #readAllData()} for fast subsequent access.
     */
    private Map<String, List<Double>> cachedDoubleValues = new HashMap<>();

    /**
     * Cached long integer channel values keyed by channel name.
     * Populated by {@link #readAllData()} for fast subsequent access.
     */
    private Map<String, List<Long>> cachedLongValues = new HashMap<>();

    /**
     * Reference to the JNI native library singleton.
     * All native calls are routed through this instance.
     */
    private final MdfLibraryNative nativeLib;

    /**
     * Constructs a new MdfReader for the specified file path.
     *
     * <p>Initializes the native reader object. The file is not opened
     * until {@link #open()} is called.</p>
     *
     * @param filePath the path to the MDF file to read
     * @throws IllegalArgumentException if filePath is null or empty
     * @throws RuntimeException if the native reader cannot be initialized
     */
    public MdfReader(String filePath) {
        this(filePath, MdfLibraryNative.getInstance());
    }

    /**
     * Constructs a new MdfReader with a specified native library instance.
     *
     * <p>This constructor is primarily useful for testing with mocked
     * native libraries.</p>
     *
     * @param filePath the path to the MDF file to read
     * @param nativeLib the native library instance to use
     * @throws IllegalArgumentException if filePath is null or empty
     * @throws RuntimeException if the native reader cannot be initialized
     */
    public MdfReader(String filePath, MdfLibraryNative nativeLib) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path must not be null or empty");
        }
        this.nativeLib = nativeLib;
        readerPtr = nativeLib.MdfReaderInit(filePath);
        if (readerPtr == 0) {
            throw new RuntimeException("Failed to initialize MDF reader for: " + filePath);
        }
    }

    /**
     * Checks whether the reader is in a valid state.
     *
     * <p>Should be called after construction to verify the file was
     * recognized as a valid MDF file.</p>
     *
     * @return true if the reader is valid and can read the file
     * @throws IllegalStateException if the reader has been closed
     */
    public boolean isOk() {
        checkOpen();
        return nativeLib.MdfReaderIsOk(readerPtr);
    }

    /**
     * Opens the MDF file for reading.
     *
     * <p>Must be called before any read operations. Verifies file
     * existence, accessibility, and format validity.</p>
     *
     * @return true if the file was opened successfully, false otherwise
     * @throws IllegalStateException if the reader has been closed
     */
    public boolean open() {
        checkOpen();
        return nativeLib.MdfReaderOpen(readerPtr);
    }

    /**
     * Closes the MDF file and releases all associated native resources.
     *
     * <p>Safe to call multiple times. After closing, no further operations
     * are permitted on this reader instance.</p>
     */
    @Override
    public void close() {
        if (!closed && readerPtr != 0) {
            nativeLib.MdfReaderClose(readerPtr);
            nativeLib.MdfReaderUnInit(readerPtr);
            readerPtr = 0;
            closed = true;
        }
    }

    /**
     * Reads only the header section of the MDF file.
     *
     * <p>Provides fast access to file metadata (author, project, etc.)
     * without loading the complete measurement structure.</p>
     *
     * @return true if the header was read successfully, false otherwise
     * @throws IllegalStateException if the reader has been closed
     */
    public boolean readHeader() {
        checkOpen();
        return nativeLib.MdfReaderReadHeader(readerPtr);
    }

    /**
     * Reads measurement info including channel groups and channel definitions.
     *
     * <p>Loads the complete channel structure and metadata without
     * reading actual measurement data samples.</p>
     *
     * @return true if measurement info was read successfully, false otherwise
     * @throws IllegalStateException if the reader has been closed
     */
    public boolean readMeasurementInfo() {
        checkOpen();
        return nativeLib.MdfReaderReadMeasurementInfo(readerPtr);
    }

    /**
     * Reads all metadata except the actual measurement data samples.
     *
     * <p>Equivalent to reading everything except raw data samples.
     * Useful for exploring file structure and channel metadata.</p>
     *
     * @return true if successful, false otherwise
     * @throws IllegalStateException if the reader has been closed
     */
    public boolean readEverythingButData() {
        checkOpen();
        return nativeLib.MdfReaderReadEverythingButData(readerPtr);
    }

    /**
     * Reads the measurement data for a specific data group.
     *
     * <p>Loads raw data samples into memory. Must be called after
     * {@link #readEverythingButData()} or {@link #readMeasurementInfo()}.</p>
     *
     * @param dataGroupIndex the zero-based index of the data group
     * @return true if data was read successfully, false otherwise
     * @throws IllegalStateException if the reader has been closed
     */
    public boolean readData(int dataGroupIndex) {
        checkOpen();
        long dg = nativeLib.MdfReaderGetDataGroup(readerPtr, dataGroupIndex);
        if (dg == 0) return false;
        return nativeLib.MdfReaderReadData(readerPtr, dg);
    }

    /**
     * Reads all data from all data groups and caches channel values.
     *
     * <p>This convenience method reads all metadata and data, creating
     * channel observers for each channel and caching their values.
     * After calling this method, use {@link #getChannelValuesAsDouble(int, String)}
     * to retrieve cached values.</p>
     *
     * <p>The cached values are stored in {@link #cachedDoubleValues} for
     * fast subsequent access without re-reading from disk.</p>
     *
     * @return true if all data was read successfully, false on any error
     * @throws IllegalStateException if the reader has been closed
     */
    public boolean readAllData() {
        checkOpen();
        readEverythingButData();

        long filePtr = nativeLib.MdfReaderGetFile(readerPtr);
        if (filePtr == 0) return false;

        long dgCount = nativeLib.MdfFileGetDataGroups(filePtr, null);
        for (int i = 0; i < (int) dgCount; i++) {
            long dg = nativeLib.MdfReaderGetDataGroup(readerPtr, i);
            if (dg == 0) continue;

            /* Track observers and names for cleanup and value extraction */
            List<Long> observers = new ArrayList<>();
            List<String> observerNames = new ArrayList<>();

            /* Iterate through channel groups to find all channels */
            long cgCount = nativeLib.MdfDataGroupGetChannelGroups(dg, null);
            long[] cgPtrs = new long[(int) cgCount];
            nativeLib.MdfDataGroupGetChannelGroups(dg, cgPtrs);
            for (long cg : cgPtrs) {
                if (cg == 0) continue;
                long chCount = nativeLib.MdfChannelGroupGetChannels(cg, null);
                long[] chPtrs = new long[(int) chCount];
                nativeLib.MdfChannelGroupGetChannels(cg, chPtrs);
                for (long ch : chPtrs) {
                    if (ch == 0) continue;
                    String chName = getString(buf -> nativeLib.MdfChannelGetName(ch, buf));
                    if (chName != null && !chName.isEmpty()) {
                        long obs = nativeLib.MdfChannelObserverCreateByChannelName(dg, chName);
                        if (obs != 0) {
                            observers.add(obs);
                            observerNames.add(chName);
                        }
                    }
                }
            }

            /* Read the actual data for this data group */
            if (!nativeLib.MdfReaderReadData(readerPtr, dg)) {
                for (long obs : observers) {
                    nativeLib.MdfChannelObserverUnInit(obs);
                }
                return false;
            }

            /* Extract values from each observer and cache them */
            double[] valRef = new double[2];
            for (int oi = 0; oi < observers.size(); oi++) {
                long obs = observers.get(oi);
                String name = observerNames.get(oi);
                try {
                    long sampleCount = nativeLib.MdfChannelObserverGetNofSamples(obs);
                    List<Double> values = new ArrayList<>();
                    for (long s = 0; s < sampleCount; s++) {
                        boolean ok = nativeLib.MdfChannelObserverGetEngValueAsFloat(obs, s, valRef);
                        if (ok) {
                            values.add(valRef[0]);
                        }
                    }
                    cachedDoubleValues.put(name, values);
                } finally {
                    nativeLib.MdfChannelObserverUnInit(obs);
                }
            }
        }
        dataLoaded = true;
        return true;
    }

    /**
     * Gets file-level metadata including name, version, and format info.
     *
     * <p>Must be called after {@link #open()} and {@link #readHeader()}.</p>
     *
     * @return FileInfo object, or null if the file pointer is unavailable
     * @throws IllegalStateException if the reader has been closed
     */
    public FileInfo getFileInfo() {
        checkOpen();
        long filePtr = nativeLib.MdfReaderGetFile(readerPtr);
        if (filePtr == 0) return null;

        String name = getString(buf -> nativeLib.MdfFileGetName(filePtr, buf));
        String fileName = getString(buf -> nativeLib.MdfFileGetFileName(filePtr, buf));
        String version = getString(buf -> nativeLib.MdfFileGetVersion(filePtr, buf));
        int mainVersion = nativeLib.MdfFileGetMainVersion(filePtr);
        int minorVersion = nativeLib.MdfFileGetMinorVersion(filePtr);

        return new FileInfo(name, fileName, version, mainVersion, minorVersion, true);
    }

    /**
     * Gets header metadata including author, department, project, etc.
     *
     * <p>Must be called after {@link #open()} and {@link #readHeader()}.</p>
     *
     * @return HeaderInfo object, or null if the header pointer is unavailable
     * @throws IllegalStateException if the reader has been closed
     */
    public HeaderInfo getHeaderInfo() {
        checkOpen();
        long headerPtr = nativeLib.MdfReaderGetHeader(readerPtr);
        if (headerPtr == 0) return null;

        String author = getString(buf -> nativeLib.MdfHeaderGetAuthor(headerPtr, buf));
        String department = getString(buf -> nativeLib.MdfHeaderGetDepartment(headerPtr, buf));
        String project = getString(buf -> nativeLib.MdfHeaderGetProject(headerPtr, buf));
        String subject = getString(buf -> nativeLib.MdfHeaderGetSubject(headerPtr, buf));
        String description = getString(buf -> nativeLib.MdfHeaderGetDescription(headerPtr, buf));
        long startTime = nativeLib.MdfHeaderGetStartTime(headerPtr);

        return new HeaderInfo(author, department, project, subject, description, startTime);
    }

    /**
     * Gets all data groups with their channel groups and channels.
     *
     * <p>Returns a hierarchical view of the file structure:
     * DataGroup -> ChannelGroup -> Channel. Must be called after
     * reading measurement info or everything.</p>
     *
     * @return list of DataGroupInfo objects, never null but may be empty
     * @throws IllegalStateException if the reader has been closed
     */
    public List<DataGroupInfo> getDataGroups() {
        checkOpen();
        List<DataGroupInfo> result = new ArrayList<>();
        long filePtr = nativeLib.MdfReaderGetFile(readerPtr);
        if (filePtr == 0) return result;

        long count = nativeLib.MdfFileGetDataGroups(filePtr, null);
        for (int i = 0; i < (int) count; i++) {
            long dg = nativeLib.MdfReaderGetDataGroup(readerPtr, i);
            if (dg == 0) continue;

            long cgCount = nativeLib.MdfDataGroupGetChannelGroups(dg, null);
            List<ChannelGroupInfo> cgList = new ArrayList<>();
            if (cgCount > 0) {
                long[] cgPtrs = new long[(int) cgCount];
                nativeLib.MdfDataGroupGetChannelGroups(dg, cgPtrs);
                for (long cg : cgPtrs) {
                    if (cg == 0) continue;
                    String cgName = getString(buf -> nativeLib.MdfChannelGroupGetName(cg, buf));
                    long samples = nativeLib.MdfChannelGroupGetNofSamples(cg);

                    long chCount = nativeLib.MdfChannelGroupGetChannels(cg, null);
                    List<ChannelData> channels = new ArrayList<>();
                    if (chCount > 0) {
                        long[] chPtrs = new long[(int) chCount];
                        nativeLib.MdfChannelGroupGetChannels(cg, chPtrs);
                        for (long ch : chPtrs) {
                            if (ch == 0) continue;
                            String chName = getString(buf -> nativeLib.MdfChannelGetName(ch, buf));
                            String chUnit = "";
                            boolean unitUsed = nativeLib.MdfChannelIsUnitUsed(ch);
                            if (unitUsed) {
                                chUnit = getString(buf2 -> nativeLib.MdfChannelGetUnit(ch, buf2));
                            }
                            byte chType = nativeLib.MdfChannelGetType(ch);
                            byte chDataType = nativeLib.MdfChannelGetDataType(ch);
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

    /**
     * Gets the names of all channels across all data groups.
     *
     * <p>Convenience method that flattens the channel hierarchy into
     * a simple list of channel names.</p>
     *
     * @return list of channel names, never null but may be empty
     * @throws IllegalStateException if the reader has been closed
     */
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

    /**
     * Gets channel values as doubles for a specific channel.
     *
     * <p>If data was loaded via {@link #readAllData()}, returns cached values.
     * Otherwise, creates a temporary observer to read values on demand.</p>
     *
     * @param dataGroupIndex the data group index
     * @param channelName the channel name to read values from
     * @return list of double values, never null but may be empty
     * @throws IllegalStateException if the reader has been closed
     */
    public List<Double> getChannelValuesAsDouble(int dataGroupIndex, String channelName) {
        checkOpen();
        if (dataLoaded && cachedDoubleValues.containsKey(channelName)) {
            return new ArrayList<>(cachedDoubleValues.get(channelName));
        }

        List<Double> values = new ArrayList<>();
        long dg = nativeLib.MdfReaderGetDataGroup(readerPtr, dataGroupIndex);
        if (dg == 0) return values;

        long observer = nativeLib.MdfChannelObserverCreateByChannelName(dg, channelName);
        if (observer == 0) return values;

        try {
            long sampleCount = nativeLib.MdfChannelObserverGetNofSamples(observer);
            double[] valRef = new double[2];
            for (long s = 0; s < sampleCount; s++) {
                boolean ok = nativeLib.MdfChannelObserverGetEngValueAsFloat(observer, s, valRef);
                if (ok) {
                    values.add(valRef[0]);
                }
            }
        } finally {
            nativeLib.MdfChannelObserverUnInit(observer);
        }
        return values;
    }

    /**
     * Gets channel values as longs for a specific channel.
     *
     * <p>If data was loaded via {@link #readAllData()}, returns cached values.
     * Otherwise, creates a temporary observer to read values on demand.</p>
     *
     * @param dataGroupIndex the data group index
     * @param channelName the channel name to read values from
     * @return list of long values, never null but may be empty
     * @throws IllegalStateException if the reader has been closed
     */
    public List<Long> getChannelValuesAsLong(int dataGroupIndex, String channelName) {
        checkOpen();
        if (dataLoaded && cachedLongValues.containsKey(channelName)) {
            return new ArrayList<>(cachedLongValues.get(channelName));
        }

        List<Long> values = new ArrayList<>();
        long dg = nativeLib.MdfReaderGetDataGroup(readerPtr, dataGroupIndex);
        if (dg == 0) return values;

        long observer = nativeLib.MdfChannelObserverCreateByChannelName(dg, channelName);
        if (observer == 0) return values;

        try {
            long sampleCount = nativeLib.MdfChannelObserverGetNofSamples(observer);
            long[] valRef = new long[2];
            for (long s = 0; s < sampleCount; s++) {
                boolean ok = nativeLib.MdfChannelObserverGetEngValueAsSigned(observer, s, valRef);
                if (ok) {
                    values.add(valRef[0]);
                }
            }
        } finally {
            nativeLib.MdfChannelObserverUnInit(observer);
        }
        return values;
    }

    /**
     * Verifies that this reader has not been closed.
     *
     * @throws IllegalStateException if the reader has been closed or the pointer is invalid
     */
    private void checkOpen() {
        if (closed || readerPtr == 0) {
            throw new IllegalStateException("MdfReader is closed");
        }
    }

    /**
     * Functional interface for native string getter functions.
     *
     * <p>Used with the two-step string retrieval pattern: first call with null
     * to get the length, then call with a buffer to get the actual string.</p>
     */
    private interface StringGetter {
        /**
         * Gets the string length or copies the string into the buffer.
         *
         * @param buf byte buffer to receive the string, or null to query length
         * @return the string length, or 0 if empty
         */
        long get(byte[] buf);
    }

    /**
     * Retrieves a string from a native function using the two-step pattern.
     *
     * <p>Step 1: Call with null buffer to get the required buffer size.
     * Step 2: Allocate buffer and call again to get the actual string data.</p>
     *
     * @param getter the native string getter function
     * @return the retrieved string, or empty string if length is 0 or negative
     */
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
