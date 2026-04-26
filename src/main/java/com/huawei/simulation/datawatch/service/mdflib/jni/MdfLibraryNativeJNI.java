package com.huawei.simulation.datawatch.service.mdflib.jni;

/**
 * JNI-based native library wrapper for mdflib (ASAM MDF file reader/writer).
 *
 * <p>This class provides a Java Native Interface (JNI) binding to the native
 * mdflib C++ library. It replaces the previous JNA-based approach, offering
 * better performance through direct JNI calls with lower overhead.</p>
 *
 * <p>Library loading strategy:</p>
 * <ul>
 *   <li>On Windows: loads zlib1.dll, libexpat.dll, then mdflibjni.dll</li>
 *   <li>On Linux: loads libz.so, libexpat.so, then libmdflibjni.so</li>
 *   <li>Native libraries are extracted from the classpath to a temp directory</li>
 * </ul>
 *
 * <p>All native pointer values are stored as {@code long} to be compatible
 * with both 32-bit and 64-bit systems (JNI uses jlong for pointers).</p>
 *
 * <p>Thread safety: The underlying mdflib is NOT thread-safe. Each reader/writer
 * instance should be used from a single thread, or external synchronization
 * must be applied.</p>
 *
 * <p>Class naming convention: The JNI class name follows the Huawei simulation
 * framework naming convention:
 * {@code com.huawei.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI}.
 * The C++ side JNI function names are derived from this fully qualified class name,
 * replacing dots with underscores (e.g.,
 * {@code Java_com_huawei_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderInit}).</p>
 *
 * @author mdflib-java contributors
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MdfLibraryNativeJNI {

    /** Singleton instance holder pattern for lazy thread-safe initialization. */
    private static final MdfLibraryNativeJNI INSTANCE = new MdfLibraryNativeJNI();

    /**
     * Flag indicating whether the native library was successfully loaded.
     * Volatile to ensure visibility across threads.
     */
    private static volatile boolean libraryLoaded = false;

    /**
     * Lock object used for synchronized library loading to prevent
     * concurrent load attempts from multiple threads.
     */
    private static final Object LOAD_LOCK = new Object();

    /**
     * Private constructor to enforce singleton pattern.
     * Triggers native library loading on first instantiation.
     */
    private MdfLibraryNativeJNI() {
        ensureLibraryLoaded();
    }

    /**
     * Returns the singleton instance of the native library wrapper.
     *
     * <p>This method provides access to the single instance of
     * {@code MdfLibraryNativeJNI}. The native library is loaded lazily
     * on first access.</p>
     *
     * @return the singleton native library wrapper instance, never null
     */
    public static MdfLibraryNativeJNI getInstance() {
        return INSTANCE;
    }

    /**
     * Ensures the native library is loaded. Uses double-checked locking
     * for thread safety while minimizing synchronization overhead.
     *
     * <p>Loading process:</p>
     * <ol>
     *   <li>Create a temporary directory for native libraries</li>
     *   <li>Extract platform-specific dependency DLLs/SOs from classpath</li>
     *   <li>Extract the JNI bridge library from classpath</li>
     *   <li>Set java.library.path to point to the temp directory</li>
     *   <li>Load the JNI bridge library via System.loadLibrary</li>
     * </ol>
     */
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

    /**
     * Performs the actual native library loading.
     *
     * <p>This method extracts native libraries from the classpath to a
     * temporary directory, then loads the JNI bridge library using an
     * absolute path with System.load(). The temporary directory is marked
     * for deletion on JVM exit.</p>
     *
     * <p>Platform-specific dependencies:</p>
     * <ul>
     *   <li>Windows: zlib1.dll, libexpat.dll, mdflibrary.dll are required</li>
     *   <li>Linux: libz.so, libexpat.so, libmdflibrary.so are required</li>
     * </ul>
     *
     * <p>Note: We use System.load() with absolute paths instead of
     * System.loadLibrary() because modifying java.library.path at
     * runtime does not take effect in most JVM implementations.</p>
     *
     * @throws Exception if library extraction or loading fails
     */
    private static void loadNativeLibrary() throws Exception {
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = osName.contains("windows");

        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("mdflib_jni");
        tempDir.toFile().deleteOnExit();

        if (isWindows) {
            /* On Windows, extract all dependency DLLs and the JNI bridge */
            String resourceDir = "native/win32-x86-64";
            String[] dependencies = {"zlib1.dll", "libexpat.dll", "mdflibrary.dll"};
            for (String dep : dependencies) {
                extractResource(resourceDir + "/" + dep, tempDir.resolve(dep));
            }
            /* Extract the JNI bridge DLL */
            extractResource(resourceDir + "/mdflibjni.dll", tempDir.resolve("mdflibjni.dll"));
            /* Load using absolute path to avoid java.library.path issues */
            System.load(tempDir.resolve("mdflibjni.dll").toString());
        } else {
            /* On Linux, extract .so files */
            String resourceDir = "native/linux-x86-64";
            String[] dependencies = {"libz.so", "libexpat.so", "libmdflibrary.so"};
            for (String dep : dependencies) {
                extractResource(resourceDir + "/" + dep, tempDir.resolve(dep));
            }
            /* Extract the JNI bridge .so */
            extractResource(resourceDir + "/libmdflibjni.so", tempDir.resolve("libmdflibjni.so"));
            /* Load using absolute path */
            System.load(tempDir.resolve("libmdflibjni.so").toString());
        }
    }

    /**
     * Extracts a resource from the classpath to a target file path.
     *
     * <p>The resource is read as an InputStream from the classloader and
     * copied to the target path. The target file is marked for deletion
     * on JVM exit.</p>
     *
     * @param resourcePath the classpath resource path (e.g., "native/win32-x86-64/zlib1.dll")
     * @param target the target file path to extract to
     * @throws java.io.IOException if the resource cannot be found or copied
     */
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

    /* ========================================================================
     * MdfReader native methods
     *
     * These methods wrap the C++ MdfReader class for reading MDF files.
     * All pointer parameters use 'long' type to represent native pointers.
     * ======================================================================== */

    /**
     * Initializes a new MDF reader for the given file path.
     *
     * <p>Creates a native MdfReader object. The caller is responsible for
     * calling {@link #MdfReaderUnInit(long)} to free resources.</p>
     *
     * @param filename the full path to the MDF file to read
     * @return native pointer to the MdfReader object, or 0 if initialization fails
     */
    public native long MdfReaderInit(String filename);

    /**
     * Releases all resources associated with an MDF reader.
     *
     * <p>After calling this method, the reader pointer becomes invalid
     * and must not be used for any subsequent operations.</p>
     *
     * @param reader native pointer to the MdfReader object
     */
    public native void MdfReaderUnInit(long reader);

    /**
     * Checks whether the MDF reader is in a valid state.
     *
     * <p>Should be called after initialization to verify the file
     * can be read successfully.</p>
     *
     * @param reader native pointer to the MdfReader object
     * @return true if the reader is valid, false otherwise
     */
    public native boolean MdfReaderIsOk(long reader);

    /**
     * Gets the MdfFile object associated with this reader.
     *
     * <p>The returned pointer provides access to file-level metadata
     * such as version, name, and data groups.</p>
     *
     * @param reader native pointer to the MdfReader object
     * @return native pointer to the MdfFile object, or 0 if not available
     */
    public native long MdfReaderGetFile(long reader);

    /**
     * Gets the header object from the reader.
     *
     * <p>The header contains metadata such as author, department,
     * project, subject, and start time.</p>
     *
     * @param reader native pointer to the MdfReader object
     * @return native pointer to the IHeader object, or 0 if not available
     */
    public native long MdfReaderGetHeader(long reader);

    /**
     * Gets a specific data group by index from the reader.
     *
     * <p>Data groups are the top-level containers for measurement data.
     * Each data group contains one or more channel groups.</p>
     *
     * @param reader native pointer to the MdfReader object
     * @param index zero-based index of the data group
     * @return native pointer to the IDataGroup object, or 0 if index is out of range
     */
    public native long MdfReaderGetDataGroup(long reader, long index);

    /**
     * Opens the MDF file for reading.
     *
     * <p>Must be called before any read operations. Checks file
     * existence and format validity.</p>
     *
     * @param reader native pointer to the MdfReader object
     * @return true if the file was opened successfully, false otherwise
     */
    public native boolean MdfReaderOpen(long reader);

    /**
     * Closes the MDF file, releasing file handles.
     *
     * <p>The reader object itself remains valid; only the file
     * handle is released. Call {@link #MdfReaderUnInit(long)} to
     * fully destroy the reader.</p>
     *
     * @param reader native pointer to the MdfReader object
     */
    public native void MdfReaderClose(long reader);

    /**
     * Reads only the header section of the MDF file.
     *
     * <p>Provides fast access to file metadata without loading
     * the complete measurement structure.</p>
     *
     * @param reader native pointer to the MdfReader object
     * @return true if the header was read successfully, false otherwise
     */
    public native boolean MdfReaderReadHeader(long reader);

    /**
     * Reads measurement info (channel groups, channels, but not data).
     *
     * <p>Loads the complete channel structure and metadata without
     * reading actual measurement data samples.</p>
     *
     * @param reader native pointer to the MdfReader object
     * @return true if measurement info was read successfully, false otherwise
     */
    public native boolean MdfReaderReadMeasurementInfo(long reader);

    /**
     * Reads all metadata including channel definitions but not data.
     *
     * <p>Equivalent to reading everything except the raw data
     * samples. Useful for exploring file structure.</p>
     *
     * @param reader native pointer to the MdfReader object
     * @return true if successful, false otherwise
     */
    public native boolean MdfReaderReadEverythingButData(long reader);

    /**
     * Reads the actual measurement data for a specific data group.
     *
     * <p>Loads raw data samples into memory. Must be called after
     * {@link #MdfReaderReadEverythingButData(long)} or
     * {@link #MdfReaderReadMeasurementInfo(long)}.</p>
     *
     * @param reader native pointer to the MdfReader object
     * @param group native pointer to the IDataGroup to read data for
     * @return true if data was read successfully, false otherwise
     */
    public native boolean MdfReaderReadData(long reader, long group);

    /* ========================================================================
     * MdfWriter native methods
     *
     * These methods wrap the C++ MdfWriter class for creating MDF files.
     * ======================================================================== */

    /**
     * Initializes a new MDF writer for the given file path and type.
     *
     * <p>Creates a native MdfWriter object. Common types:</p>
     * <ul>
     *   <li>1 = MDF4 basic format</li>
     * </ul>
     *
     * @param type the MDF writer type (e.g., 1 for MDF4)
     * @param filename the output file path
     * @return native pointer to the MdfWriter object, or 0 if initialization fails
     */
    public native long MdfWriterInit(int type, String filename);

    /**
     * Releases all resources associated with an MDF writer.
     *
     * <p>Should be called after finalizing the measurement.
     * The writer pointer becomes invalid after this call.</p>
     *
     * @param writer native pointer to the MdfWriter object
     */
    public native void MdfWriterUnInit(long writer);

    /**
     * Gets the MdfFile object associated with this writer.
     *
     * @param writer native pointer to the MdfWriter object
     * @return native pointer to the MdfFile object, or 0 if not available
     */
    public native long MdfWriterGetFile(long writer);

    /**
     * Gets the header object for the writer.
     *
     * <p>Use this to set author, department, project, and other metadata
     * before starting measurement.</p>
     *
     * @param writer native pointer to the MdfWriter object
     * @return native pointer to the IHeader object, or 0 if not available
     */
    public native long MdfWriterGetHeader(long writer);

    /**
     * Sets whether to compress data in the output file.
     *
     * <p>Must be called before {@link #MdfWriterInitMeasurement(long)}.</p>
     *
     * @param writer native pointer to the MdfWriter object
     * @param compress 1 to enable compression, 0 to disable
     */
    public native void MdfWriterSetCompressData(long writer, byte compress);

    /**
     * Creates a new data group in the writer.
     *
     * <p>Data groups are the top-level containers. Each writer typically
     * has at least one data group.</p>
     *
     * @param writer native pointer to the MdfWriter object
     * @return native pointer to the new IDataGroup, or 0 on failure
     */
    public native long MdfWriterCreateDataGroup(long writer);

    /**
     * Initializes the measurement, preparing channels for data recording.
     *
     * <p>Must be called after all channels are configured and before
     * {@link #MdfWriterStartMeasurement(long, long)}.</p>
     *
     * @param writer native pointer to the MdfWriter object
     * @return true if initialization succeeded, false otherwise
     */
    public native boolean MdfWriterInitMeasurement(long writer);

    /**
     * Saves a sample for all channels in the specified channel group.
     *
     * <p>Channel values must be set before calling this method using
     * {@link #MdfChannelSetChannelValueAsFloat(long, double, int, long)} etc.</p>
     *
     * @param writer native pointer to the MdfWriter object
     * @param group native pointer to the IChannelGroup
     * @param time timestamp in nanoseconds for this sample
     */
    public native void MdfWriterSaveSample(long writer, long group, long time);

    /**
     * Starts the measurement at the given time.
     *
     * <p>Must be called after {@link #MdfWriterInitMeasurement(long)}.</p>
     *
     * @param writer native pointer to the MdfWriter object
     * @param startTime start timestamp in nanoseconds
     */
    public native void MdfWriterStartMeasurement(long writer, long startTime);

    /**
     * Stops the measurement at the given time.
     *
     * <p>After stopping, call {@link #MdfWriterFinalizeMeasurement(long)}
     * to complete the file.</p>
     *
     * @param writer native pointer to the MdfWriter object
     * @param stopTime stop timestamp in nanoseconds
     */
    public native void MdfWriterStopMeasurement(long writer, long stopTime);

    /**
     * Finalizes the measurement and writes all data to disk.
     *
     * <p>Must be called after {@link #MdfWriterStopMeasurement(long, long)}.
     * The output file is complete after this call.</p>
     *
     * @param writer native pointer to the MdfWriter object
     * @return true if finalization succeeded, false otherwise
     */
    public native boolean MdfWriterFinalizeMeasurement(long writer);

    /* ========================================================================
     * MdfFile native methods
     *
     * These methods provide access to file-level information.
     * ======================================================================== */

    /**
     * Gets the internal name of the MDF file.
     *
     * @param file native pointer to the MdfFile object
     * @param name byte array to receive the name (null to query length)
     * @return the length of the name string, or 0 if empty
     */
    public native long MdfFileGetName(long file, byte[] name);

    /**
     * Sets the internal name of the MDF file.
     *
     * @param file native pointer to the MdfFile object
     * @param name the name to set
     */
    public native void MdfFileSetName(long file, String name);

    /**
     * Gets the file name (path) of the MDF file.
     *
     * @param file native pointer to the MdfFile object
     * @param filename byte array to receive the filename (null to query length)
     * @return the length of the filename string, or 0 if empty
     */
    public native long MdfFileGetFileName(long file, byte[] filename);

    /**
     * Gets the MDF format version string (e.g., "4.10").
     *
     * @param file native pointer to the MdfFile object
     * @param version byte array to receive the version string (null to query length)
     * @return the length of the version string, or 0 if empty
     */
    public native long MdfFileGetVersion(long file, byte[] version);

    /**
     * Gets the main version number of the MDF format.
     *
     * <p>For MDF4 files, this returns 4.</p>
     *
     * @param file native pointer to the MdfFile object
     * @return the main version number (e.g., 3 or 4)
     */
    public native int MdfFileGetMainVersion(long file);

    /**
     * Gets the minor version number of the MDF format.
     *
     * <p>For MDF 4.10, this returns 10.</p>
     *
     * @param file native pointer to the MdfFile object
     * @return the minor version number
     */
    public native int MdfFileGetMinorVersion(long file);

    /**
     * Checks if the file is in MDF4 format.
     *
     * @param file native pointer to the MdfFile object
     * @return true if the file is MDF4 format, false otherwise
     */
    public native boolean MdfFileGetIsMdf4(long file);

    /**
     * Gets all data groups in the file.
     *
     * <p>When dataGroups is null, returns the count. Otherwise,
     * fills the array with native pointers to IDataGroup objects.</p>
     *
     * @param file native pointer to the MdfFile object
     * @param dataGroups long array to receive pointers (null to query count)
     * @return the number of data groups, or 0 if none
     */
    public native long MdfFileGetDataGroups(long file, long[] dataGroups);

    /* ========================================================================
     * MdfHeader native methods
     *
     * These methods provide access to header metadata.
     * ======================================================================== */

    /**
     * Gets the author field from the header.
     *
     * @param header native pointer to the IHeader object
     * @param author byte array to receive the author string (null to query length)
     * @return the length of the author string
     */
    public native long MdfHeaderGetAuthor(long header, byte[] author);

    /**
     * Sets the author field in the header.
     *
     * @param header native pointer to the IHeader object
     * @param author the author name to set
     */
    public native void MdfHeaderSetAuthor(long header, String author);

    /**
     * Gets the description field from the header.
     *
     * @param header native pointer to the IHeader object
     * @param desc byte array to receive the description (null to query length)
     * @return the length of the description string
     */
    public native long MdfHeaderGetDescription(long header, byte[] desc);

    /**
     * Sets the description field in the header.
     *
     * @param header native pointer to the IHeader object
     * @param desc the description to set
     */
    public native void MdfHeaderSetDescription(long header, String desc);

    /**
     * Gets the project field from the header.
     *
     * @param header native pointer to the IHeader object
     * @param project byte array to receive the project name (null to query length)
     * @return the length of the project string
     */
    public native long MdfHeaderGetProject(long header, byte[] project);

    /**
     * Sets the project field in the header.
     *
     * @param header native pointer to the IHeader object
     * @param project the project name to set
     */
    public native void MdfHeaderSetProject(long header, String project);

    /**
     * Gets the subject field from the header.
     *
     * @param header native pointer to the IHeader object
     * @param subject byte array to receive the subject (null to query length)
     * @return the length of the subject string
     */
    public native long MdfHeaderGetSubject(long header, byte[] subject);

    /**
     * Sets the subject field in the header.
     *
     * @param header native pointer to the IHeader object
     * @param subject the subject to set
     */
    public native void MdfHeaderSetSubject(long header, String subject);

    /**
     * Gets the department field from the header.
     *
     * @param header native pointer to the IHeader object
     * @param department byte array to receive the department (null to query length)
     * @return the length of the department string
     */
    public native long MdfHeaderGetDepartment(long header, byte[] department);

    /**
     * Sets the department field in the header.
     *
     * @param header native pointer to the IHeader object
     * @param department the department to set
     */
    public native void MdfHeaderSetDepartment(long header, String department);

    /**
     * Gets the start time from the header (nanoseconds since epoch).
     *
     * @param header native pointer to the IHeader object
     * @return start time in nanoseconds since 1970-01-01
     */
    public native long MdfHeaderGetStartTime(long header);

    /**
     * Gets all data groups from the header.
     *
     * @param header native pointer to the IHeader object
     * @param dataGroups long array to receive pointers (null to query count)
     * @return the number of data groups
     */
    public native long MdfHeaderGetDataGroups(long header, long[] dataGroups);

    /**
     * Creates a new data group in the header.
     *
     * @param header native pointer to the IHeader object
     * @return native pointer to the new IDataGroup, or 0 on failure
     */
    public native long MdfHeaderCreateDataGroup(long header);

    /**
     * Creates a new file history entry in the header.
     *
     * @param header native pointer to the IHeader object
     * @return native pointer to the new IFileHistory, or 0 on failure
     */
    public native long MdfHeaderCreateFileHistory(long header);

    /**
     * Gets the last data group from the header.
     *
     * <p>Useful for appending data to the most recently created group.</p>
     *
     * @param header native pointer to the IHeader object
     * @return native pointer to the last IDataGroup, or 0 if none exist
     */
    public native long MdfHeaderGetLastDataGroup(long header);

    /* ========================================================================
     * MdfDataGroup native methods
     *
     * These methods operate on data group objects.
     * ======================================================================== */

    /**
     * Gets the description of a data group.
     *
     * @param group native pointer to the IDataGroup object
     * @param description byte array to receive the description (null to query length)
     * @return the length of the description string
     */
    public native long MdfDataGroupGetDescription(long group, byte[] description);

    /**
     * Gets all channel groups within a data group.
     *
     * @param group native pointer to the IDataGroup object
     * @param channelGroups long array to receive pointers (null to query count)
     * @return the number of channel groups
     */
    public native long MdfDataGroupGetChannelGroups(long group, long[] channelGroups);

    /**
     * Creates a new channel group within a data group.
     *
     * @param group native pointer to the IDataGroup object
     * @return native pointer to the new IChannelGroup, or 0 on failure
     */
    public native long MdfDataGroupCreateChannelGroup(long group);

    /* ========================================================================
     * MdfChannelGroup native methods
     *
     * These methods operate on channel group objects.
     * ======================================================================== */

    /**
     * Gets the name of a channel group.
     *
     * @param group native pointer to the IChannelGroup object
     * @param name byte array to receive the name (null to query length)
     * @return the length of the name string
     */
    public native long MdfChannelGroupGetName(long group, byte[] name);

    /**
     * Sets the name of a channel group.
     *
     * @param group native pointer to the IChannelGroup object
     * @param name the name to set
     */
    public native void MdfChannelGroupSetName(long group, String name);

    /**
     * Gets the number of samples recorded in this channel group.
     *
     * @param group native pointer to the IChannelGroup object
     * @return the number of samples
     */
    public native long MdfChannelGroupGetNofSamples(long group);

    /**
     * Sets the expected number of samples for this channel group.
     *
     * <p>Should be set before writing data for optimal performance.</p>
     *
     * @param group native pointer to the IChannelGroup object
     * @param samples the number of samples that will be recorded
     */
    public native void MdfChannelGroupSetNofSamples(long group, long samples);

    /**
     * Gets all channels within a channel group.
     *
     * @param group native pointer to the IChannelGroup object
     * @param channels long array to receive pointers (null to query count)
     * @return the number of channels
     */
    public native long MdfChannelGroupGetChannels(long group, long[] channels);

    /**
     * Creates a new channel within a channel group.
     *
     * @param group native pointer to the IChannelGroup object
     * @return native pointer to the new IChannel, or 0 on failure
     */
    public native long MdfChannelGroupCreateChannel(long group);

    /* ========================================================================
     * MdfChannel native methods
     *
     * These methods operate on individual channel objects.
     * ======================================================================== */

    /**
     * Gets the name of a channel.
     *
     * @param channel native pointer to the IChannel object
     * @param name byte array to receive the name (null to query length)
     * @return the length of the name string
     */
    public native long MdfChannelGetName(long channel, byte[] name);

    /**
     * Sets the name of a channel.
     *
     * @param channel native pointer to the IChannel object
     * @param name the name to set
     */
    public native void MdfChannelSetName(long channel, String name);

    /**
     * Checks if the channel has a unit defined.
     *
     * @param channel native pointer to the IChannel object
     * @return true if a unit is set, false otherwise
     */
    public native boolean MdfChannelIsUnitUsed(long channel);

    /**
     * Gets the unit of a channel (e.g., "V", "m/s", "degC").
     *
     * @param channel native pointer to the IChannel object
     * @param unit byte array to receive the unit string (null to query length)
     * @return the length of the unit string
     */
    public native long MdfChannelGetUnit(long channel, byte[] unit);

    /**
     * Sets the unit of a channel.
     *
     * @param channel native pointer to the IChannel object
     * @param unit the unit string (e.g., "V", "m/s")
     */
    public native void MdfChannelSetUnit(long channel, String unit);

    /**
     * Gets the channel type.
     *
     * <p>Common types: 0=FixedLength, 1=VariableLength, 2=Master, 3=VirtualMaster</p>
     *
     * @param channel native pointer to the IChannel object
     * @return the channel type as a byte value
     */
    public native byte MdfChannelGetType(long channel);

    /**
     * Sets the channel type.
     *
     * @param channel native pointer to the IChannel object
     * @param type the channel type byte value
     */
    public native void MdfChannelSetType(long channel, byte type);

    /**
     * Gets the sync type of the channel.
     *
     * <p>Common sync types: 0=None, 1=Time, 2=Angle, 3=Distance, 4=Index</p>
     *
     * @param channel native pointer to the IChannel object
     * @return the sync type as a byte value
     */
    public native byte MdfChannelGetSync(long channel);

    /**
     * Sets the sync type of the channel.
     *
     * @param channel native pointer to the IChannel object
     * @param syncType the sync type byte value
     */
    public native void MdfChannelSetSync(long channel, byte syncType);

    /**
     * Gets the data type of the channel.
     *
     * <p>Common data types: 0=UnsignedIntLE, 2=SignedIntLE, 4=FloatLE,
     * 6=StringASCII, 7=StringUTF8, 10=ByteArray</p>
     *
     * @param channel native pointer to the IChannel object
     * @return the data type as a byte value
     */
    public native byte MdfChannelGetDataType(long channel);

    /**
     * Sets the data type of the channel.
     *
     * @param channel native pointer to the IChannel object
     * @param dataType the data type byte value
     */
    public native void MdfChannelSetDataType(long channel, byte dataType);

    /**
     * Gets the bit count (resolution) of the channel.
     *
     * <p>For example, a 64-bit double channel returns 64.</p>
     *
     * @param channel native pointer to the IChannel object
     * @return the number of bits
     */
    public native int MdfChannelGetBitCount(long channel);

    /**
     * Sets the bit count (resolution) of the channel.
     *
     * @param channel native pointer to the IChannel object
     * @param bits the number of bits
     */
    public native void MdfChannelSetBitCount(long channel, int bits);

    /**
     * Gets the data byte count for the channel.
     *
     * <p>For example, a 64-bit double channel uses 8 data bytes.</p>
     *
     * @param channel native pointer to the IChannel object
     * @return the number of data bytes
     */
    public native long MdfChannelGetDataBytes(long channel);

    /**
     * Sets the data byte count for the channel.
     *
     * @param channel native pointer to the IChannel object
     * @param bytes the number of data bytes
     */
    public native void MdfChannelSetDataBytes(long channel, long bytes);

    /**
     * Sets a signed integer value for the channel.
     *
     * <p>Typically called before {@link #MdfWriterSaveSample(long, long, long)}
     * to set channel values for a sample.</p>
     *
     * @param channel native pointer to the IChannel object
     * @param value the signed value to set
     * @param valid 1 if the value is valid, 0 if invalid
     * @param arrayIndex array index for array channels (typically 0)
     */
    public native void MdfChannelSetChannelValueAsSigned(long channel, long value,
            int valid, long arrayIndex);

    /**
     * Sets an unsigned integer value for the channel.
     *
     * @param channel native pointer to the IChannel object
     * @param value the unsigned value to set
     * @param valid 1 if the value is valid, 0 if invalid
     * @param arrayIndex array index for array channels (typically 0)
     */
    public native void MdfChannelSetChannelValueAsUnSigned(long channel, long value,
            int valid, long arrayIndex);

    /**
     * Sets a floating-point value for the channel.
     *
     * @param channel native pointer to the IChannel object
     * @param value the double value to set
     * @param valid 1 if the value is valid, 0 if invalid
     * @param arrayIndex array index for array channels (typically 0)
     */
    public native void MdfChannelSetChannelValueAsFloat(long channel, double value,
            int valid, long arrayIndex);

    /**
     * Sets a string value for the channel.
     *
     * @param channel native pointer to the IChannel object
     * @param value the string value as bytes
     * @param valid 1 if the value is valid, 0 if invalid
     * @param arrayIndex array index for array channels (typically 0)
     */
    public native void MdfChannelSetChannelValueAsString(long channel, byte[] value,
            int valid, long arrayIndex);

    /* ========================================================================
     * MdfChannelObserver native methods
     *
     * These methods create and use channel observers for reading data.
     * ======================================================================== */

    /**
     * Creates a channel observer for a specific channel.
     *
     * <p>The observer is used to read individual sample values from
     * a channel after data has been loaded.</p>
     *
     * @param dataGroup native pointer to the IDataGroup containing the channel
     * @param channelGroup native pointer to the IChannelGroup
     * @param channel native pointer to the IChannel to observe
     * @return native pointer to the new IChannelObserver, or 0 on failure
     */
    public native long MdfChannelObserverCreate(long dataGroup, long channelGroup,
            long channel);

    /**
     * Creates a channel observer by channel name.
     *
     * <p>Convenience method that finds the channel by name and creates
     * an observer for it. This is the most common way to create observers.</p>
     *
     * @param dataGroup native pointer to the IDataGroup
     * @param channelName the name of the channel to observe
     * @return native pointer to the new IChannelObserver, or 0 if not found
     */
    public native long MdfChannelObserverCreateByChannelName(long dataGroup,
            String channelName);

    /**
     * Releases a channel observer.
     *
     * <p>Must be called when the observer is no longer needed to
     * free native memory.</p>
     *
     * @param observer native pointer to the IChannelObserver
     */
    public native void MdfChannelObserverUnInit(long observer);

    /**
     * Gets the number of samples in the observer.
     *
     * <p>Only valid after data has been loaded via
     * {@link #MdfReaderReadData(long, long)}.</p>
     *
     * @param observer native pointer to the IChannelObserver
     * @return the number of samples available
     */
    public native long MdfChannelObserverGetNofSamples(long observer);

    /**
     * Gets the name of the observed channel.
     *
     * @param observer native pointer to the IChannelObserver
     * @param name byte array to receive the name (null to query length)
     * @return the length of the name string
     */
    public native long MdfChannelObserverGetName(long observer, byte[] name);

    /**
     * Checks if this observer is for a master channel (e.g., time).
     *
     * @param observer native pointer to the IChannelObserver
     * @return true if the observed channel is a master channel
     */
    public native boolean MdfChannelObserverIsMaster(long observer);

    /**
     * Gets a channel value as a signed integer.
     *
     * <p>The value is returned through a two-element long array:
     * index 0 receives the value, index 1 is set to 1 if valid.</p>
     *
     * @param observer native pointer to the IChannelObserver
     * @param sample the sample index
     * @param value output array: [value, validFlag]
     * @return true if the value was retrieved successfully
     */
    public native boolean MdfChannelObserverGetChannelValueAsSigned(long observer,
            long sample, long[] value);

    /**
     * Gets a channel value as an unsigned integer.
     *
     * @param observer native pointer to the IChannelObserver
     * @param sample the sample index
     * @param value output array: [value, validFlag]
     * @return true if the value was retrieved successfully
     */
    public native boolean MdfChannelObserverGetChannelValueAsUnSigned(long observer,
            long sample, long[] value);

    /**
     * Gets a channel value as a floating-point number.
     *
     * <p>The value is returned through a two-element double array:
     * index 0 receives the value, index 1 is set to 1.0 if valid.</p>
     *
     * @param observer native pointer to the IChannelObserver
     * @param sample the sample index
     * @param value output array: [value, validFlag]
     * @return true if the value was retrieved successfully
     */
    public native boolean MdfChannelObserverGetChannelValueAsFloat(long observer,
            long sample, double[] value);

    /**
     * Gets an engineering value as a signed integer.
     *
     * <p>Engineering values have conversion applied (e.g., linear
     * conversion, formula-based conversion).</p>
     *
     * @param observer native pointer to the IChannelObserver
     * @param sample the sample index
     * @param value output array: [value, validFlag]
     * @return true if the value was retrieved successfully
     */
    public native boolean MdfChannelObserverGetEngValueAsSigned(long observer,
            long sample, long[] value);

    /**
     * Gets an engineering value as an unsigned integer.
     *
     * @param observer native pointer to the IChannelObserver
     * @param sample the sample index
     * @param value output array: [value, validFlag]
     * @return true if the value was retrieved successfully
     */
    public native boolean MdfChannelObserverGetEngValueAsUnSigned(long observer,
            long sample, long[] value);

    /**
     * Gets an engineering value as a floating-point number.
     *
     * <p>This is the most common method for reading measurement data
     * as it returns the converted (engineering) value.</p>
     *
     * @param observer native pointer to the IChannelObserver
     * @param sample the sample index
     * @param value output array: [value, validFlag]
     * @return true if the value was retrieved successfully
     */
    public native boolean MdfChannelObserverGetEngValueAsFloat(long observer,
            long sample, double[] value);

    /* ========================================================================
     * MdfFileHistory native methods
     *
     * These methods operate on file history entries.
     * ======================================================================== */

    /**
     * Gets the tool name from a file history entry.
     *
     * @param fh native pointer to the IFileHistory object
     * @param name byte array to receive the tool name (null to query length)
     * @return the length of the tool name string
     */
    public native long MdfFileHistoryGetToolName(long fh, byte[] name);

    /**
     * Sets the tool name in a file history entry.
     *
     * @param fh native pointer to the IFileHistory object
     * @param name the tool name to set
     */
    public native void MdfFileHistorySetToolName(long fh, String name);

    /**
     * Gets the description from a file history entry.
     *
     * @param fh native pointer to the IFileHistory object
     * @param desc byte array to receive the description (null to query length)
     * @return the length of the description string
     */
    public native long MdfFileHistoryGetDescription(long fh, byte[] desc);

    /**
     * Sets the description in a file history entry.
     *
     * @param fh native pointer to the IFileHistory object
     * @param desc the description to set
     */
    public native void MdfFileHistorySetDescription(long fh, String desc);
}
