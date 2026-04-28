/**
 * @file mdflibjni.cpp
 * @brief JNI bridge between Java and the mdflib C-style exported API.
 *
 * This file implements the Java Native Interface (JNI) bindings for the
 * mdflib MDF file reader/writer library. It calls the C-style exported
 * functions from mdflibrary.dll rather than using the C++ API directly.
 *
 * Architecture:
 * - Java objects store native pointers as 'long' values (jlong)
 * - All pointer parameters use jlong (64-bit) for portability
 * - The C exports use raw pointers (void* or typed pointers) stored as jlong
 * - String retrieval uses a two-step pattern: query length, then copy data
 * - Array retrieval uses a two-step pattern: query count, then fill array
 * - Observer value retrieval uses output arrays to return (value, valid) pairs
 *
 * Memory management:
 * - Reader/Writer objects are owned by Java and freed via UnInit calls
 * - Observer objects must be explicitly freed via MdfChannelObserverUnInit
 * - No garbage collection is performed on native objects
 *
 * Thread safety:
 * - The underlying mdflib is NOT thread-safe
 * - JNI methods do not add synchronization; callers must handle this
 *
 * Build requirements:
 * - JDK (for jni.h header)
 * - mdflibrary.dll (with C-style exports)
 *
 * @author mdflib-java contributors
 * @version 2.0.0
 * @since 1.0.0
 */

/* ========================================================================
 * Platform-specific export macros
 *
 * These macros ensure proper symbol visibility when building the shared
 * library on different platforms. On Windows, functions must be explicitly
 * exported using __declspec(dllexport).
 * ======================================================================== */
#if defined(_WIN32)
    #define JNI_EXPORT __declspec(dllexport)
#elif defined(__linux__) || defined(__APPLE__)
    #define JNI_EXPORT __attribute__((visibility("default")))
#else
    #define JNI_EXPORT
#endif

/* ========================================================================
 * JNI header inclusion
 *
 * The JNI header provides the function prototypes and type definitions
 * needed to implement native Java methods.
 * ======================================================================== */
#include <jni.h>

/* ========================================================================
 * Standard library includes
 * ======================================================================== */
#include <cstdint>
#include <cstring>
#include <string>
#include <vector>

/* ========================================================================
 * C-style export function declarations from mdflibrary.dll
 *
 * These are the extern "C" functions exported by mdflibrary.dll. We
 * declare them here so the JNI bridge can call them without needing
 * any mdf/*.h C++ headers. All pointer types are declared as void*
 * or as opaque pointer types, and we cast between jlong and these
 * pointer types at the JNI boundary.
 * ======================================================================== */
extern "C" {

/* ---- MdfReader exports ---- */
void* MdfReaderInit(const char* filename);
void MdfReaderUnInit(void* reader);
int64_t MdfReaderGetIndex(void* reader);
bool MdfReaderIsOk(void* reader);
const void* MdfReaderGetFile(void* reader);
const void* MdfReaderGetHeader(void* reader);
const void* MdfReaderGetDataGroup(void* reader, size_t index);
bool MdfReaderOpen(void* reader);
void MdfReaderClose(void* reader);
bool MdfReaderReadHeader(void* reader);
bool MdfReaderReadMeasurementInfo(void* reader);
bool MdfReaderReadEverythingButData(void* reader);
bool MdfReaderReadData(void* reader, void* group);

/* ---- MdfWriter exports ---- */
void* MdfWriterInit(int type, const char* filename);
void MdfWriterUnInit(void* writer);
void* MdfWriterGetFile(void* writer);
void* MdfWriterGetHeader(void* writer);
bool MdfWriterIsFileNew(void* writer);
bool MdfWriterGetCompressData(void* writer);
void MdfWriterSetCompressData(void* writer, bool compress);
double MdfWriterGetPreTrigTime(void* writer);
void MdfWriterSetPreTrigTime(void* writer, double pre_trig_time);
uint64_t MdfWriterGetStartTime(void* writer);
uint64_t MdfWriterGetStopTime(void* writer);
uint16_t MdfWriterGetBusType(void* writer);
void MdfWriterSetBusType(void* writer, uint16_t type);
int MdfWriterGetStorageType(void* writer);
void MdfWriterSetStorageType(void* writer, int type);
uint32_t MdfWriterGetMaxLength(void* writer);
void MdfWriterSetMaxLength(void* writer, uint32_t length);
bool MdfWriterCreateBusLogConfiguration(void* writer);
void* MdfWriterCreateDataGroup(void* writer);
bool MdfWriterInitMeasurement(void* writer);
void MdfWriterSaveSample(void* writer, void* group, uint64_t time);
void MdfWriterStartMeasurement(void* writer, uint64_t start_time);
void MdfWriterStopMeasurement(void* writer, uint64_t stop_time);
bool MdfWriterFinalizeMeasurement(void* writer);

/* ---- MdfFile exports ---- */
size_t MdfFileGetName(void* file, char* name);
void MdfFileSetName(void* file, const char* name);
size_t MdfFileGetFileName(void* file, char* filename);
void MdfFileSetFileName(void* file, const char* filename);
size_t MdfFileGetVersion(void* file, char* version);
int MdfFileGetMainVersion(void* file);
int MdfFileGetMinorVersion(void* file);
void MdfFileSetMinorVersion(void* file, int minor);
size_t MdfFileGetProgramId(void* file, char* program_id);
void MdfFileSetProgramId(void* file, const char* program_id);
bool MdfFileGetIsMdf4(void* file);
const void* MdfFileGetHeader(void* file);
size_t MdfFileGetDataGroups(void* file, const void* pDataGroup[]);
size_t MdfFileGetAttachments(void* file, const void* pAttachment[]);
void* MdfFileCreateAttachment(void* file);
void* MdfFileCreateDataGroup(void* file);

/* ---- MdfHeader exports ---- */
int64_t MdfHeaderGetIndex(void* header);
size_t MdfHeaderGetDescription(void* header, char* desc);
void MdfHeaderSetDescription(void* header, const char* desc);
size_t MdfHeaderGetAuthor(void* header, char* author);
void MdfHeaderSetAuthor(void* header, const char* author);
size_t MdfHeaderGetDepartment(void* header, char* department);
void MdfHeaderSetDepartment(void* header, const char* department);
size_t MdfHeaderGetProject(void* header, char* project);
void MdfHeaderSetProject(void* header, const char* project);
size_t MdfHeaderGetSubject(void* header, char* subject);
void MdfHeaderSetSubject(void* header, const char* subject);
size_t MdfHeaderGetMeasurementId(void* header, char* uuid);
void MdfHeaderSetMeasurementId(void* header, const char* uuid);
size_t MdfHeaderGetRecorderId(void* header, char* uuid);
void MdfHeaderSetRecorderId(void* header, const char* uuid);
int64_t MdfHeaderGetRecorderIndex(void* header);
void MdfHeaderSetRecorderIndex(void* header, int64_t index);
uint64_t MdfHeaderGetStartTime(void* header);
void MdfHeaderSetStartTime(void* header, uint64_t time);
bool MdfHeaderIsStartAngleUsed(void* header);
double MdfHeaderGetStartAngle(void* header);
void MdfHeaderSetStartAngle(void* header, double angle);
bool MdfHeaderIsStartDistanceUsed(void* header);
double MdfHeaderGetStartDistance(void* header);
void MdfHeaderSetStartDistance(void* header, double distance);
const void* MdfHeaderGetMetaData(void* header);
size_t MdfHeaderGetAttachments(void* header, void* pAttachments[]);
size_t MdfHeaderGetFileHistorys(void* header, void* pFileHistorys[]);
size_t MdfHeaderGetEvents(void* header, void* pEvents[]);
size_t MdfHeaderGetDataGroups(void* header, void* pDataGroups[]);
void* MdfHeaderGetLastDataGroup(void* header);
void* MdfHeaderCreateMetaData(void* header);
void* MdfHeaderCreateAttachment(void* header);
void* MdfHeaderCreateFileHistory(void* header);
void* MdfHeaderCreateEvent(void* header);
void* MdfHeaderCreateDataGroup(void* header);

/* ---- MdfDataGroup exports ---- */
int64_t MdfDataGroupGetIndex(void* group);
size_t MdfDataGroupGetDescription(void* group, char* description);
uint8_t MdfDataGroupGetRecordIdSize(void* group);
const void* MdfDataGroupGetMetaData(void* group);
void* MdfDataGroupGetChannelGroupByName(void* group, const char* name);
void* MdfDataGroupGetChannelGroupByRecordId(void* group, uint64_t record_id);
size_t MdfDataGroupGetChannelGroups(void* group, void* pChannelGroups[]);
bool MdfDataGroupIsRead(void* group);
void* MdfDataGroupCreateMetaData(void* group);
void* MdfDataGroupCreateChannelGroup(void* group);
const void* MdfDataGroupFindParentChannelGroup(void* group, const void* channel);

/* ---- MdfChannelGroup exports ---- */
int64_t MdfChannelGroupGetIndex(void* group);
uint64_t MdfChannelGroupGetRecordId(void* group);
size_t MdfChannelGroupGetName(void* group, char* name);
void MdfChannelGroupSetName(void* group, const char* name);
size_t MdfChannelGroupGetDescription(void* group, char* desc);
void MdfChannelGroupSetDescription(void* group, const char* desc);
uint64_t MdfChannelGroupGetNofSamples(void* group);
void MdfChannelGroupSetNofSamples(void* group, uint64_t samples);
uint16_t MdfChannelGroupGetFlags(void* group);
void MdfChannelGroupSetFlags(void* group, uint16_t flags);
size_t MdfChannelGroupGetChannels(void* group, void* pChannels[]);
const void* MdfChannelGroupGetMetaData(void* group);
void* MdfChannelGroupCreateMetaData(void* group);
void* MdfChannelGroupCreateChannel(void* group);
void* MdfChannelGroupCreateSourceInformation(void* group);

/* ---- MdfChannel exports ---- */
int64_t MdfChannelGetIndex(void* channel);
size_t MdfChannelGetName(void* channel, char* name);
void MdfChannelSetName(void* channel, const char* name);
size_t MdfChannelGetDisplayName(void* channel, char* name);
void MdfChannelSetDisplayName(void* channel, const char* name);
size_t MdfChannelGetDescription(void* channel, char* desc);
void MdfChannelSetDescription(void* channel, const char* desc);
bool MdfChannelIsUnitUsed(void* channel);
size_t MdfChannelGetUnit(void* channel, char* unit);
void MdfChannelSetUnit(void* channel, const char* unit);
int MdfChannelGetType(void* channel);
void MdfChannelSetType(void* channel, int type);
int MdfChannelGetSync(void* channel);
void MdfChannelSetSync(void* channel, int type);
int MdfChannelGetDataType(void* channel);
void MdfChannelSetDataType(void* channel, int type);
uint32_t MdfChannelGetFlags(void* channel);
void MdfChannelSetFlags(void* channel, uint32_t flags);
uint64_t MdfChannelGetDataBytes(void* channel);
void MdfChannelSetDataBytes(void* channel, uint64_t bytes);
bool MdfChannelIsPrecisionUsed(void* channel);
uint8_t MdfChannelGetPrecision(void* channel);
bool MdfChannelIsRangeUsed(void* channel);
double MdfChannelGetRangeMin(void* channel);
double MdfChannelGetRangeMax(void* channel);
void MdfChannelSetRange(void* channel, double min, double max);
bool MdfChannelIsLimitUsed(void* channel);
double MdfChannelGetLimitMin(void* channel);
double MdfChannelGetLimitMax(void* channel);
void MdfChannelSetLimit(void* channel, double min, double max);
bool MdfChannelIsExtLimitUsed(void* channel);
double MdfChannelGetExtLimitMin(void* channel);
double MdfChannelGetExtLimitMax(void* channel);
void MdfChannelSetExtLimit(void* channel, double min, double max);
double MdfChannelGetSamplingRate(void* channel);
uint64_t MdfChannelGetVlsdRecordId(void* channel);
void MdfChannelSetVlsdRecordId(void* channel, uint64_t record_id);
uint32_t MdfChannelGetBitCount(void* channel);
void MdfChannelSetBitCount(void* channel, uint32_t bits);
uint16_t MdfChannelGetBitOffset(void* channel);
void MdfChannelSetBitOffset(void* channel, uint16_t bits);
const void* MdfChannelGetMetaData(void* channel);
const void* MdfChannelGetSourceInformation(void* channel);
const void* MdfChannelGetChannelConversion(void* channel);
const void* MdfChannelGetChannelArray(void* channel);
size_t MdfChannelGetChannelCompositions(void* channel, void* pChannels[]);
void* MdfChannelCreateSourceInformation(void* channel);
void* MdfChannelCreateChannelConversion(void* channel);
void* MdfChannelCreateChannelArray(void* channel);
void* MdfChannelCreateChannelComposition(void* channel);
void* MdfChannelCreateMetaData(void* channel);
void MdfChannelSetChannelValueAsSigned(void* channel, int64_t value, bool valid, uint64_t array_index);
void MdfChannelSetChannelValueAsUnSigned(void* channel, uint64_t value, bool valid, uint64_t array_index);
void MdfChannelSetChannelValueAsFloat(void* channel, double value, bool valid, uint64_t array_index);
void MdfChannelSetChannelValueAsString(void* channel, const char* value, bool valid, uint64_t array_index);
void MdfChannelSetChannelValueAsArray(void* channel, const uint8_t* value, size_t size, bool valid, uint64_t array_index);

/* ---- MdfChannelObserver exports ---- */
void* MdfChannelObserverCreate(void* data_group, void* channel_group, void* channel);
void* MdfChannelObserverCreateByChannelName(void* data_group, const char* channel_name);
size_t MdfChannelObserverCreateForChannelGroup(void* data_group, void* channel_group, void* pObservers[]);
void MdfChannelObserverUnInit(void* observer);
int64_t MdfChannelObserverGetNofSamples(void* observer);
size_t MdfChannelObserverGetName(void* observer, char* name);
size_t MdfChannelObserverGetUnit(void* observer, char* unit);
const void* MdfChannelObserverGetChannel(void* observer);
bool MdfChannelObserverIsMaster(void* observer);
bool MdfChannelObserverGetChannelValueAsSigned(void* observer, uint64_t sample, int64_t& value);
bool MdfChannelObserverGetChannelValueAsUnSigned(void* observer, uint64_t sample, uint64_t& value);
bool MdfChannelObserverGetChannelValueAsFloat(void* observer, uint64_t sample, double& value);
bool MdfChannelObserverGetChannelValueAsString(void* observer, uint64_t sample, char* value, size_t& size);
bool MdfChannelObserverGetChannelValueAsArray(void* observer, uint64_t sample, uint8_t value[], size_t& size);
bool MdfChannelObserverGetEngValueAsSigned(void* observer, uint64_t sample, int64_t& value);
bool MdfChannelObserverGetEngValueAsUnSigned(void* observer, uint64_t sample, uint64_t& value);
bool MdfChannelObserverGetEngValueAsFloat(void* observer, uint64_t sample, double& value);
bool MdfChannelObserverGetEngValueAsString(void* observer, uint64_t sample, char* value, size_t& size);
bool MdfChannelObserverGetEngValueAsArray(void* observer, uint64_t sample, uint8_t value[], size_t& size);

/* ---- MdfFileHistory exports ---- */
int64_t MdfFileHistoryGetIndex(void* file_history);
uint64_t MdfFileHistoryGetTime(void* file_history);
void MdfFileHistorySetTime(void* file_history, uint64_t time);
const void* MdfFileHistoryGetMetaData(void* file_history);
size_t MdfFileHistoryGetDescription(void* file_history, char* desc);
void MdfFileHistorySetDescription(void* file_history, const char* desc);
size_t MdfFileHistoryGetToolName(void* file_history, char* name);
void MdfFileHistorySetToolName(void* file_history, const char* name);
size_t MdfFileHistoryGetToolVendor(void* file_history, char* vendor);
void MdfFileHistorySetToolVendor(void* file_history, const char* vendor);
size_t MdfFileHistoryGetToolVersion(void* file_history, char* version);
void MdfFileHistorySetToolVersion(void* file_history, const char* version);
size_t MdfFileHistoryGetUserName(void* file_history, char* user);
void MdfFileHistorySetUserName(void* file_history, const char* user);

} /* extern "C" */

/* ========================================================================
 * Helper function: Cast jlong to C-style pointer
 *
 * Converts a Java long (jlong) value to a typed C++ pointer. This is
 * the core mechanism for passing native object references between Java
 * and C++. The jlong type is guaranteed to be 64-bit, which is large
 * enough to hold any pointer value on both 32-bit and 64-bit systems.
 *
 * @param ptr the jlong value representing a native pointer
 * @return typed C++ pointer, or nullptr if ptr is 0
 * ======================================================================== */
template<typename T>
static T* ptr_from_jlong(jlong ptr) {
    if (ptr == 0) return nullptr;
    return reinterpret_cast<T*>(static_cast<intptr_t>(ptr));
}

/* ========================================================================
 * Helper function: Cast C-style pointer to jlong
 *
 * Converts a C++ pointer to a Java long value for storage in Java objects.
 * The pointer value is preserved exactly, allowing round-trip conversion.
 *
 * @param ptr the C++ pointer to convert
 * @return jlong value representing the pointer, or 0 for nullptr
 * ======================================================================== */
template<typename T>
static jlong ptr_to_jlong(T* ptr) {
    if (ptr == nullptr) return 0;
    return static_cast<jlong>(reinterpret_cast<intptr_t>(ptr));
}

/* ========================================================================
 * Helper function: Copy C-string to Java byte array
 *
 * Implements the two-step string retrieval pattern used by the C exports
 * and the Java side:
 * 1. When the byte array is null, return the string length
 * 2. When the byte array is non-null, copy the string data into it
 *
 * The string is copied as UTF-8 bytes without a null terminator, since
 * Java's String constructor handles the conversion from bytes to String.
 *
 * @param env JNI environment pointer
 * @param str the C-string to copy
 * @param len the length of the C-string
 * @param jbuf the Java byte array (null to query length, non-null to copy)
 * @return the length of the string in bytes
 * ======================================================================== */
static jlong copy_cstring_to_java(JNIEnv* env, const char* str, size_t len, jbyteArray jbuf) {
    if (jbuf == nullptr) {
        return static_cast<jlong>(len);
    }
    jsize jlen = static_cast<jsize>(len);
    env->SetByteArrayRegion(jbuf, 0, jlen, reinterpret_cast<const jbyte*>(str));
    return static_cast<jlong>(len);
}

/* ========================================================================
 * Helper function: Get string from Java byte array
 *
 * Converts a Java byte array to a C++ std::string. Used for string
 * parameters passed from Java to native code.
 *
 * @param env JNI environment pointer
 * @param jstr the Java byte array containing string data
 * @return C++ string constructed from the byte array data
 * ======================================================================== */
static std::string string_from_java_bytes(JNIEnv* env, jbyteArray jstr) {
    if (jstr == nullptr) return "";
    jsize len = env->GetArrayLength(jstr);
    if (len <= 0) return "";
    std::vector<char> buf(len);
    env->GetByteArrayRegion(jstr, 0, len, reinterpret_cast<jbyte*>(buf.data()));
    return std::string(buf.data(), static_cast<size_t>(len));
}

/* ========================================================================
 * MdfReader JNI implementations
 *
 * These functions wrap the C-style MdfReader exports for reading MDF files.
 * Each function corresponds to a native method declared in
 * com.huawei.behavior.simulation.datawatch.service.mdflib.jni.MdfLibraryNativeJNI.
 * ======================================================================== */

/**
 * Initializes a new MdfReader for the given file path.
 *
 * Creates a native MdfReader object on the heap. The caller is
 * responsible for calling MdfReaderUnInit to free the object.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param jfilename Java String containing the file path
 * @return jlong representing the MdfReader pointer, or 0 on failure
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderInit(
    JNIEnv* env, jobject obj, jstring jfilename) {
    if (jfilename == nullptr) return 0;
    const char* filename = env->GetStringUTFChars(jfilename, nullptr);
    if (filename == nullptr) return 0;
    void* reader = MdfReaderInit(filename);
    env->ReleaseStringUTFChars(jfilename, filename);
    return ptr_to_jlong(reader);
}

/**
 * Releases an MdfReader and all associated resources.
 *
 * After this call, the reader pointer is invalid and must not be used.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param readerPtr native pointer to the MdfReader
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderUnInit(
    JNIEnv* env, jobject obj, jlong readerPtr) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    if (reader != nullptr) {
        MdfReaderUnInit(reader);
    }
}

/** Checks if the MdfReader is in a valid state.
 *
 * This function verifies that the reader was able to open and parse
 * the MDF file header. A reader can be invalid if the file does not
 * exist, is not a valid MDF file, or has a corrupted header block.
 *
 * Should be called immediately after MdfReaderInit() to verify
 * successful initialization before attempting any read operations.
 *
 * @param env JNI environment pointer for JVM interaction
 * @param obj Java object reference (the MdfLibraryNative instance)
 * @param readerPtr native pointer to the MdfReader object
 * @return JNI_TRUE if the reader is in a valid state, JNI_FALSE otherwise
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderIsOk(
    JNIEnv* env, jobject obj, jlong readerPtr) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    if (reader == nullptr) return JNI_FALSE;
    return MdfReaderIsOk(reader) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Gets the MdfFile object from the reader.
 *
 * The returned pointer provides access to file-level metadata.
 * The pointer is valid as long as the reader exists.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param readerPtr native pointer to the MdfReader
 * @return jlong representing the MdfFile pointer, or 0 if unavailable
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderGetFile(
    JNIEnv* env, jobject obj, jlong readerPtr) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    if (reader == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfReaderGetFile(reader)));
}

/**
 * Gets the IHeader object from the reader.
 *
 * The header contains metadata such as author, department, project.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param readerPtr native pointer to the MdfReader
 * @return jlong representing the IHeader pointer, or 0 if unavailable
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderGetHeader(
    JNIEnv* env, jobject obj, jlong readerPtr) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    if (reader == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfReaderGetHeader(reader)));
}

/**
 * Gets a specific data group by index.
 *
 * Data groups are top-level containers for measurement data.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param readerPtr native pointer to the MdfReader
 * @param index zero-based data group index
 * @return jlong representing the IDataGroup pointer, or 0 if out of range
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderGetDataGroup(
    JNIEnv* env, jobject obj, jlong readerPtr, jlong index) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    if (reader == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfReaderGetDataGroup(reader,
        static_cast<size_t>(index))));
}

/**
 * Opens the MDF file for reading.
 *
 * Must be called before any read operations.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param readerPtr native pointer to the MdfReader
 * @return JNI_TRUE if opened successfully, JNI_FALSE otherwise
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderOpen(
    JNIEnv* env, jobject obj, jlong readerPtr) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    if (reader == nullptr) return JNI_FALSE;
    return MdfReaderOpen(reader) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Closes the MDF file, releasing file handles.
 *
 * The reader object remains valid; only the file handle is released.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param readerPtr native pointer to the MdfReader
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderClose(
    JNIEnv* env, jobject obj, jlong readerPtr) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    if (reader != nullptr) {
        MdfReaderClose(reader);
    }
}

/**
 * Reads the header section of the MDF file.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param readerPtr native pointer to the MdfReader
 * @return JNI_TRUE if successful, JNI_FALSE otherwise
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderReadHeader(
    JNIEnv* env, jobject obj, jlong readerPtr) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    if (reader == nullptr) return JNI_FALSE;
    return MdfReaderReadHeader(reader) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Reads measurement info (channel structure without data).
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param readerPtr native pointer to the MdfReader
 * @return JNI_TRUE if successful, JNI_FALSE otherwise
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderReadMeasurementInfo(
    JNIEnv* env, jobject obj, jlong readerPtr) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    if (reader == nullptr) return JNI_FALSE;
    return MdfReaderReadMeasurementInfo(reader) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Reads all metadata except actual data samples.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param readerPtr native pointer to the MdfReader
 * @return JNI_TRUE if successful, JNI_FALSE otherwise
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderReadEverythingButData(
    JNIEnv* env, jobject obj, jlong readerPtr) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    if (reader == nullptr) return JNI_FALSE;
    return MdfReaderReadEverythingButData(reader) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Reads measurement data for a specific data group.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param readerPtr native pointer to the MdfReader
 * @param groupPtr native pointer to the IDataGroup
 * @return JNI_TRUE if successful, JNI_FALSE otherwise
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderReadData(
    JNIEnv* env, jobject obj, jlong readerPtr, jlong groupPtr) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    void* group = ptr_from_jlong<void>(groupPtr);
    if (reader == nullptr || group == nullptr) return JNI_FALSE;
    return MdfReaderReadData(reader, group) ? JNI_TRUE : JNI_FALSE;
}

/* ========================================================================
 * MdfWriter JNI implementations
 * ======================================================================== */

/**
 * Initializes a new MdfWriter for the given type and file path.
 *
 * The MdfWriter is created via the C export which internally uses the
 * MdfFactory, allocating the appropriate writer implementation based on
 * the type parameter. After creation, Init() is called with the output
 * filename.
 *
 * Common writer types:
 *   - MdfWriterType::Mdf4Basic (1): Standard MDF4 file writer
 *
 * Memory ownership: The caller (Java) owns the writer and must
 * call MdfWriterUnInit() to delete it.
 *
 * @param env JNI environment pointer for JVM interaction
 * @param obj Java object reference (the MdfLibraryNative instance)
 * @param type the MDF writer type integer (cast to MdfWriterType enum)
 * @param jfilename Java String containing the output file path
 * @return jlong representing the MdfWriter pointer, or 0 on failure
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterInit(
    JNIEnv* env, jobject obj, jint type, jstring jfilename) {
    if (jfilename == nullptr) return 0;
    const char* filename = env->GetStringUTFChars(jfilename, nullptr);
    if (filename == nullptr) return 0;
    void* writer = MdfWriterInit(static_cast<int>(type), filename);
    env->ReleaseStringUTFChars(jfilename, filename);
    return ptr_to_jlong(writer);
}

/**
 * Releases an MdfWriter and all associated resources.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param writerPtr native pointer to the MdfWriter
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterUnInit(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer != nullptr) {
        MdfWriterUnInit(writer);
    }
}

/**
 * Gets the MdfFile object from the writer.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param writerPtr native pointer to the MdfWriter
 * @return jlong representing the MdfFile pointer, or 0 if unavailable
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterGetFile(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return 0;
    return ptr_to_jlong(MdfWriterGetFile(writer));
}

/**
 * Gets the IHeader object from the writer.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param writerPtr native pointer to the MdfWriter
 * @return jlong representing the IHeader pointer, or 0 if unavailable
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterGetHeader(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return 0;
    return ptr_to_jlong(MdfWriterGetHeader(writer));
}

/**
 * Sets whether data should be compressed in the output file.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param writerPtr native pointer to the MdfWriter
 * @param compress 1 to enable, 0 to disable
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterSetCompressData(
    JNIEnv* env, jobject obj, jlong writerPtr, jbyte compress) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer != nullptr) {
        MdfWriterSetCompressData(writer, compress != 0);
    }
}

/**
 * Creates a new data group in the writer.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param writerPtr native pointer to the MdfWriter
 * @return jlong representing the new IDataGroup pointer, or 0 on failure
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterCreateDataGroup(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return 0;
    return ptr_to_jlong(MdfWriterCreateDataGroup(writer));
}

/**
 * Initializes the measurement for recording.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param writerPtr native pointer to the MdfWriter
 * @return JNI_TRUE if successful, JNI_FALSE otherwise
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterInitMeasurement(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return JNI_FALSE;
    return MdfWriterInitMeasurement(writer) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Saves a sample for all channels in the specified channel group.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param writerPtr native pointer to the MdfWriter
 * @param groupPtr native pointer to the IChannelGroup
 * @param time timestamp in nanoseconds
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterSaveSample(
    JNIEnv* env, jobject obj, jlong writerPtr, jlong groupPtr, jlong time) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    void* group = ptr_from_jlong<void>(groupPtr);
    if (writer != nullptr && group != nullptr) {
        MdfWriterSaveSample(writer, group, static_cast<uint64_t>(time));
    }
}

/**
 * Starts the measurement at the given time.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param writerPtr native pointer to the MdfWriter
 * @param startTime start timestamp in nanoseconds
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterStartMeasurement(
    JNIEnv* env, jobject obj, jlong writerPtr, jlong startTime) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer != nullptr) {
        MdfWriterStartMeasurement(writer, static_cast<uint64_t>(startTime));
    }
}

/**
 * Stops the measurement at the given time.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param writerPtr native pointer to the MdfWriter
 * @param stopTime stop timestamp in nanoseconds
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterStopMeasurement(
    JNIEnv* env, jobject obj, jlong writerPtr, jlong stopTime) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer != nullptr) {
        MdfWriterStopMeasurement(writer, static_cast<uint64_t>(stopTime));
    }
}

/**
 * Finalizes the measurement and writes all data to disk.
 *
 * @param env JNI environment
 * @param obj Java object reference
 * @param writerPtr native pointer to the MdfWriter
 * @return JNI_TRUE if successful, JNI_FALSE otherwise
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterFinalizeMeasurement(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return JNI_FALSE;
    return MdfWriterFinalizeMeasurement(writer) ? JNI_TRUE : JNI_FALSE;
}

/* ========================================================================
 * MdfFile JNI implementations
 * ======================================================================== */

/**
 * Gets the internal name of the MDF file.
 * Uses the two-step string retrieval pattern.
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileGetName(
    JNIEnv* env, jobject obj, jlong filePtr, jbyteArray jname) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return 0;
    if (jname == nullptr) {
        return static_cast<jlong>(MdfFileGetName(file, nullptr));
    }
    size_t len = MdfFileGetName(file, nullptr);
    std::vector<char> buf(len + 1);
    MdfFileGetName(file, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jname);
}

/**
 * Sets the internal name of the MDF file.
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileSetName(
    JNIEnv* env, jobject obj, jlong filePtr, jstring jname) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr || jname == nullptr) return;
    const char* name = env->GetStringUTFChars(jname, nullptr);
    if (name == nullptr) return;
    MdfFileSetName(file, name);
    env->ReleaseStringUTFChars(jname, name);
}

/**
 * Gets the file name (path) of the MDF file.
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileGetFileName(
    JNIEnv* env, jobject obj, jlong filePtr, jbyteArray jfilename) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return 0;
    if (jfilename == nullptr) {
        return static_cast<jlong>(MdfFileGetFileName(file, nullptr));
    }
    size_t len = MdfFileGetFileName(file, nullptr);
    std::vector<char> buf(len + 1);
    MdfFileGetFileName(file, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jfilename);
}

/**
 * Gets the MDF format version string.
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileGetVersion(
    JNIEnv* env, jobject obj, jlong filePtr, jbyteArray jversion) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return 0;
    if (jversion == nullptr) {
        return static_cast<jlong>(MdfFileGetVersion(file, nullptr));
    }
    size_t len = MdfFileGetVersion(file, nullptr);
    std::vector<char> buf(len + 1);
    MdfFileGetVersion(file, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jversion);
}

/**
 * Gets the main version number (e.g., 4 for MDF4).
 */
extern "C" JNI_EXPORT jint JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileGetMainVersion(
    JNIEnv* env, jobject obj, jlong filePtr) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return 0;
    return static_cast<jint>(MdfFileGetMainVersion(file));
}

/**
 * Gets the minor version number (e.g., 10 for MDF 4.10).
 */
extern "C" JNI_EXPORT jint JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileGetMinorVersion(
    JNIEnv* env, jobject obj, jlong filePtr) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return 0;
    return static_cast<jint>(MdfFileGetMinorVersion(file));
}

/**
 * Checks if the file is in MDF4 format.
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileGetIsMdf4(
    JNIEnv* env, jobject obj, jlong filePtr) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return JNI_FALSE;
    return MdfFileGetIsMdf4(file) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Gets all data groups in the file.
 *
 * This function uses a two-step retrieval pattern:
 *   Step 1: Call with jdataGroups=null to get the count
 *   Step 2: Allocate a long[] in Java and call again to fill it
 *
 * The C export MdfFileGetDataGroups returns the count when the array
 * pointer is null, or fills the array when non-null. Each pointer is
 * converted to a jlong for storage in the Java long[] array.
 *
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param filePtr native pointer to the MdfFile object
 * @param jdataGroups Java long[] array (null to query count, non-null to fill)
 * @return the number of data groups in the file
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileGetDataGroups(
    JNIEnv* env, jobject obj, jlong filePtr, jlongArray jdataGroups) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return 0;

    if (jdataGroups == nullptr) {
        return static_cast<jlong>(MdfFileGetDataGroups(file, nullptr));
    }

    size_t count = MdfFileGetDataGroups(file, nullptr);
    std::vector<const void*> ptrs(count);
    MdfFileGetDataGroups(file, ptrs.data());

    jsize len = static_cast<jsize>(count);
    std::vector<jlong> jlongs(len);
    for (jsize i = 0; i < len; i++) {
        jlongs[i] = ptr_to_jlong(const_cast<void*>(ptrs[i]));
    }
    env->SetLongArrayRegion(jdataGroups, 0, len, jlongs.data());

    return static_cast<jlong>(count);
}

/* ========================================================================
 * MdfHeader JNI implementations
 *
 * These functions wrap the C-style MdfHeader export functions for reading
 * and writing file header metadata. The header contains administrative
 * information such as author, department, project, and timestamps.
 *
 * String getter pattern: All string getters use the two-step pattern
 * (null buffer = query length, non-null buffer = copy data).
 *
 * String setter pattern: All string setters convert the Java String
 * to a UTF-8 C string, call the native setter, then release the
 * Java string reference.
 * ======================================================================== */

/** Gets the author from the header.
 *
 * The author field identifies the person who created the measurement.
 * Returns the string length when jauthor is null, or copies the string
 * data when jauthor is a valid byte array.
 *
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param headerPtr native pointer to the IHeader object
 * @param jauthor byte array for output (null to query length)
 * @return the length of the author string in bytes
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetAuthor(
    JNIEnv* env, jobject obj, jlong headerPtr, jbyteArray jauthor) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    if (jauthor == nullptr) {
        return static_cast<jlong>(MdfHeaderGetAuthor(header, nullptr));
    }
    size_t len = MdfHeaderGetAuthor(header, nullptr);
    std::vector<char> buf(len + 1);
    MdfHeaderGetAuthor(header, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jauthor);
}

/** Sets the author in the header.
 *
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param headerPtr native pointer to the IHeader object
 * @param jauthor Java String with the author name to set
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderSetAuthor(
    JNIEnv* env, jobject obj, jlong headerPtr, jstring jauthor) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr || jauthor == nullptr) return;
    const char* author = env->GetStringUTFChars(jauthor, nullptr);
    if (author == nullptr) return;
    MdfHeaderSetAuthor(header, author);
    env->ReleaseStringUTFChars(jauthor, author);
}

/** Gets the description from the header.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param headerPtr native pointer to the IHeader object
 * @param jdesc byte array for output (null to query length)
 * @return the length of the description string
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetDescription(
    JNIEnv* env, jobject obj, jlong headerPtr, jbyteArray jdesc) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    if (jdesc == nullptr) {
        return static_cast<jlong>(MdfHeaderGetDescription(header, nullptr));
    }
    size_t len = MdfHeaderGetDescription(header, nullptr);
    std::vector<char> buf(len + 1);
    MdfHeaderGetDescription(header, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jdesc);
}

/** Sets the description in the header.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param headerPtr native pointer to the IHeader object
 * @param jdesc Java String with the description to set
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderSetDescription(
    JNIEnv* env, jobject obj, jlong headerPtr, jstring jdesc) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr || jdesc == nullptr) return;
    const char* desc = env->GetStringUTFChars(jdesc, nullptr);
    if (desc == nullptr) return;
    MdfHeaderSetDescription(header, desc);
    env->ReleaseStringUTFChars(jdesc, desc);
}

/** Gets the project from the header.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param headerPtr native pointer to the IHeader object
 * @param jproject byte array for output (null to query length)
 * @return the length of the project string
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetProject(
    JNIEnv* env, jobject obj, jlong headerPtr, jbyteArray jproject) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    if (jproject == nullptr) {
        return static_cast<jlong>(MdfHeaderGetProject(header, nullptr));
    }
    size_t len = MdfHeaderGetProject(header, nullptr);
    std::vector<char> buf(len + 1);
    MdfHeaderGetProject(header, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jproject);
}

/** Sets the project in the header.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param headerPtr native pointer to the IHeader object
 * @param jproject Java String with the project name to set
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderSetProject(
    JNIEnv* env, jobject obj, jlong headerPtr, jstring jproject) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr || jproject == nullptr) return;
    const char* project = env->GetStringUTFChars(jproject, nullptr);
    if (project == nullptr) return;
    MdfHeaderSetProject(header, project);
    env->ReleaseStringUTFChars(jproject, project);
}

/** Gets the subject from the header.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param headerPtr native pointer to the IHeader object
 * @param jsubject byte array for output (null to query length)
 * @return the length of the subject string
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetSubject(
    JNIEnv* env, jobject obj, jlong headerPtr, jbyteArray jsubject) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    if (jsubject == nullptr) {
        return static_cast<jlong>(MdfHeaderGetSubject(header, nullptr));
    }
    size_t len = MdfHeaderGetSubject(header, nullptr);
    std::vector<char> buf(len + 1);
    MdfHeaderGetSubject(header, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jsubject);
}

/** Sets the subject in the header.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param headerPtr native pointer to the IHeader object
 * @param jsubject Java String with the subject to set
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderSetSubject(
    JNIEnv* env, jobject obj, jlong headerPtr, jstring jsubject) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr || jsubject == nullptr) return;
    const char* subject = env->GetStringUTFChars(jsubject, nullptr);
    if (subject == nullptr) return;
    MdfHeaderSetSubject(header, subject);
    env->ReleaseStringUTFChars(jsubject, subject);
}

/** Gets the department from the header.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param headerPtr native pointer to the IHeader object
 * @param jdepartment byte array for output (null to query length)
 * @return the length of the department string
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetDepartment(
    JNIEnv* env, jobject obj, jlong headerPtr, jbyteArray jdepartment) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    if (jdepartment == nullptr) {
        return static_cast<jlong>(MdfHeaderGetDepartment(header, nullptr));
    }
    size_t len = MdfHeaderGetDepartment(header, nullptr);
    std::vector<char> buf(len + 1);
    MdfHeaderGetDepartment(header, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jdepartment);
}

/** Sets the department in the header.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param headerPtr native pointer to the IHeader object
 * @param jdepartment Java String with the department to set
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderSetDepartment(
    JNIEnv* env, jobject obj, jlong headerPtr, jstring jdepartment) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr || jdepartment == nullptr) return;
    const char* department = env->GetStringUTFChars(jdepartment, nullptr);
    if (department == nullptr) return;
    MdfHeaderSetDepartment(header, department);
    env->ReleaseStringUTFChars(jdepartment, department);
}

/** Gets the start time from the header (nanoseconds since epoch). */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetStartTime(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    return static_cast<jlong>(MdfHeaderGetStartTime(header));
}

/** Gets all data groups from the header. */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetDataGroups(
    JNIEnv* env, jobject obj, jlong headerPtr, jlongArray jdataGroups) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;

    if (jdataGroups == nullptr) {
        return static_cast<jlong>(MdfHeaderGetDataGroups(header, nullptr));
    }

    size_t count = MdfHeaderGetDataGroups(header, nullptr);
    std::vector<void*> ptrs(count);
    MdfHeaderGetDataGroups(header, ptrs.data());

    jsize len = static_cast<jsize>(count);
    std::vector<jlong> jlongs(len);
    for (jsize i = 0; i < len; i++) {
        jlongs[i] = ptr_to_jlong(ptrs[i]);
    }
    env->SetLongArrayRegion(jdataGroups, 0, len, jlongs.data());

    return static_cast<jlong>(count);
}

/** Creates a new data group in the header. */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderCreateDataGroup(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    return ptr_to_jlong(MdfHeaderCreateDataGroup(header));
}

/** Creates a new file history entry in the header. */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderCreateFileHistory(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    return ptr_to_jlong(MdfHeaderCreateFileHistory(header));
}

/** Gets the last data group from the header. */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetLastDataGroup(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    return ptr_to_jlong(MdfHeaderGetLastDataGroup(header));
}

/* ========================================================================
 * MdfDataGroup JNI implementations
 *
 * These functions wrap the C-style MdfDataGroup export functions. A data
 * group is the top-level container for measurement data in an MDF file.
 * Each data group contains one or more channel groups.
 * ======================================================================== */

/** Gets the description of a data group.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param groupPtr native pointer to the IDataGroup object
 * @param jdesc byte array for output (null to query length)
 * @return the length of the description string
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfDataGroupGetDescription(
    JNIEnv* env, jobject obj, jlong groupPtr, jbyteArray jdesc) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    if (jdesc == nullptr) {
        return static_cast<jlong>(MdfDataGroupGetDescription(group, nullptr));
    }
    size_t len = MdfDataGroupGetDescription(group, nullptr);
    std::vector<char> buf(len + 1);
    MdfDataGroupGetDescription(group, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jdesc);
}

/** Gets all channel groups within a data group.
 *
 * Uses the two-step retrieval pattern (null array = count, non-null = fill).
 * Each pointer is converted to a jlong for Java storage.
 *
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param groupPtr native pointer to the IDataGroup object
 * @param jchannelGroups long[] array (null to query count, non-null to fill)
 * @return the number of channel groups
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfDataGroupGetChannelGroups(
    JNIEnv* env, jobject obj, jlong groupPtr, jlongArray jchannelGroups) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;

    if (jchannelGroups == nullptr) {
        return static_cast<jlong>(MdfDataGroupGetChannelGroups(group, nullptr));
    }

    size_t count = MdfDataGroupGetChannelGroups(group, nullptr);
    std::vector<void*> ptrs(count);
    MdfDataGroupGetChannelGroups(group, ptrs.data());

    jsize len = static_cast<jsize>(count);
    std::vector<jlong> jlongs(len);
    for (jsize i = 0; i < len; i++) {
        jlongs[i] = ptr_to_jlong(ptrs[i]);
    }
    env->SetLongArrayRegion(jchannelGroups, 0, len, jlongs.data());

    return static_cast<jlong>(count);
}

/** Creates a new channel group within a data group.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param groupPtr native pointer to the IDataGroup object
 * @return jlong pointer to the new IChannelGroup, or 0 on failure
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfDataGroupCreateChannelGroup(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return ptr_to_jlong(MdfDataGroupCreateChannelGroup(group));
}

/* ========================================================================
 * MdfChannelGroup JNI implementations
 *
 * These functions wrap the C-style MdfChannelGroup export functions. A
 * channel group contains a set of channels that are sampled at the same
 * time stamps. All channels in a group share the same sample count.
 * ======================================================================== */

/** Gets the name of a channel group.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param groupPtr native pointer to the IChannelGroup object
 * @param jname byte array for output (null to query length)
 * @return the length of the name string
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupGetName(
    JNIEnv* env, jobject obj, jlong groupPtr, jbyteArray jname) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    if (jname == nullptr) {
        return static_cast<jlong>(MdfChannelGroupGetName(group, nullptr));
    }
    size_t len = MdfChannelGroupGetName(group, nullptr);
    std::vector<char> buf(len + 1);
    MdfChannelGroupGetName(group, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jname);
}

/** Sets the name of a channel group.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param groupPtr native pointer to the IChannelGroup object
 * @param jname Java String with the name to set
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupSetName(
    JNIEnv* env, jobject obj, jlong groupPtr, jstring jname) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr || jname == nullptr) return;
    const char* name = env->GetStringUTFChars(jname, nullptr);
    if (name == nullptr) return;
    MdfChannelGroupSetName(group, name);
    env->ReleaseStringUTFChars(jname, name);
}

/** Gets the number of samples in a channel group.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param groupPtr native pointer to the IChannelGroup object
 * @return the number of recorded samples
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupGetNofSamples(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return static_cast<jlong>(MdfChannelGroupGetNofSamples(group));
}

/** Sets the number of samples for a channel group.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param groupPtr native pointer to the IChannelGroup object
 * @param samples the expected number of samples
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupSetNofSamples(
    JNIEnv* env, jobject obj, jlong groupPtr, jlong samples) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group != nullptr) {
        MdfChannelGroupSetNofSamples(group, static_cast<uint64_t>(samples));
    }
}

/** Gets all channels within a channel group.
 * Uses the two-step retrieval pattern (null = count, non-null = fill).
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param groupPtr native pointer to the IChannelGroup object
 * @param jchannels long[] array (null to query count, non-null to fill)
 * @return the number of channels
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupGetChannels(
    JNIEnv* env, jobject obj, jlong groupPtr, jlongArray jchannels) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;

    if (jchannels == nullptr) {
        return static_cast<jlong>(MdfChannelGroupGetChannels(group, nullptr));
    }

    size_t count = MdfChannelGroupGetChannels(group, nullptr);
    std::vector<void*> ptrs(count);
    MdfChannelGroupGetChannels(group, ptrs.data());

    jsize len = static_cast<jsize>(count);
    std::vector<jlong> jlongs(len);
    for (jsize i = 0; i < len; i++) {
        jlongs[i] = ptr_to_jlong(ptrs[i]);
    }
    env->SetLongArrayRegion(jchannels, 0, len, jlongs.data());

    return static_cast<jlong>(count);
}

/** Creates a new channel within a channel group.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param groupPtr native pointer to the IChannelGroup object
 * @return jlong pointer to the new IChannel, or 0 on failure
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupCreateChannel(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return ptr_to_jlong(MdfChannelGroupCreateChannel(group));
}

/* ========================================================================
 * MdfChannel JNI implementations
 *
 * These functions wrap the C-style MdfChannel export functions. A channel
 * represents a single measurement signal within a channel group.
 * Each channel has a name, unit, type, data type, and bit count.
 *
 * Channel types (ChannelType enum):
 *   0 = FixedLength   - Normal measurement signal
 *   1 = VariableLength - Variable-length data (strings, byte arrays)
 *   2 = Master        - Time base channel for the group
 *   3 = VirtualMaster - Calculated time base (not stored)
 *
 * Data types (ChannelDataType enum):
 *   0 = UnsignedIntLE  - Unsigned integer, little-endian
 *   2 = SignedIntLE    - Signed integer, little-endian
 *   4 = FloatLE        - IEEE 754 float, little-endian
 *   6 = StringASCII    - ASCII string
 *   7 = StringUTF8     - UTF-8 string
 *   10 = ByteArray     - Raw byte array
 * ======================================================================== */

/** Gets the name of a channel.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @param jname byte array for output (null to query length)
 * @return the length of the name string
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetName(
    JNIEnv* env, jobject obj, jlong channelPtr, jbyteArray jname) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    if (jname == nullptr) {
        return static_cast<jlong>(MdfChannelGetName(channel, nullptr));
    }
    size_t len = MdfChannelGetName(channel, nullptr);
    std::vector<char> buf(len + 1);
    MdfChannelGetName(channel, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jname);
}

/** Sets the name of a channel.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @param jname Java String with the channel name to set
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetName(
    JNIEnv* env, jobject obj, jlong channelPtr, jstring jname) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr || jname == nullptr) return;
    const char* name = env->GetStringUTFChars(jname, nullptr);
    if (name == nullptr) return;
    MdfChannelSetName(channel, name);
    env->ReleaseStringUTFChars(jname, name);
}

/** Checks if the channel has a unit defined.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @return JNI_TRUE if a unit string is set, JNI_FALSE otherwise
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelIsUnitUsed(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return JNI_FALSE;
    return MdfChannelIsUnitUsed(channel) ? JNI_TRUE : JNI_FALSE;
}

/** Gets the unit of a channel (e.g., "V", "m/s", "degC").
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @param junit byte array for output (null to query length)
 * @return the length of the unit string
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetUnit(
    JNIEnv* env, jobject obj, jlong channelPtr, jbyteArray junit) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    if (junit == nullptr) {
        return static_cast<jlong>(MdfChannelGetUnit(channel, nullptr));
    }
    size_t len = MdfChannelGetUnit(channel, nullptr);
    std::vector<char> buf(len + 1);
    MdfChannelGetUnit(channel, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, junit);
}

/** Sets the unit of a channel.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @param junit Java String with the unit to set
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetUnit(
    JNIEnv* env, jobject obj, jlong channelPtr, jstring junit) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr || junit == nullptr) return;
    const char* unit = env->GetStringUTFChars(junit, nullptr);
    if (unit == nullptr) return;
    MdfChannelSetUnit(channel, unit);
    env->ReleaseStringUTFChars(junit, unit);
}

/** Gets the channel type (0=Fixed, 1=Variable, 2=Master, 3=VirtualMaster).
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @return the channel type as a jbyte
 */
extern "C" JNI_EXPORT jbyte JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetType(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return static_cast<jbyte>(MdfChannelGetType(channel));
}

/** Sets the channel type.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @param type the channel type byte value
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetType(
    JNIEnv* env, jobject obj, jlong channelPtr, jbyte type) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetType(channel, static_cast<int>(type));
    }
}

/** Gets the sync type of a channel. */
extern "C" JNI_EXPORT jbyte JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetSync(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return static_cast<jbyte>(MdfChannelGetSync(channel));
}

/** Sets the sync type of a channel. */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetSync(
    JNIEnv* env, jobject obj, jlong channelPtr, jbyte syncType) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetSync(channel, static_cast<int>(syncType));
    }
}

/** Gets the data type of a channel. */
extern "C" JNI_EXPORT jbyte JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetDataType(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return static_cast<jbyte>(MdfChannelGetDataType(channel));
}

/** Sets the data type of a channel. */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetDataType(
    JNIEnv* env, jobject obj, jlong channelPtr, jbyte dataType) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetDataType(channel, static_cast<int>(dataType));
    }
}

/** Gets the bit count (resolution) of the channel (e.g., 8, 16, 32, 64).
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @return the number of bits used to represent the channel value
 */
extern "C" JNI_EXPORT jint JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetBitCount(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return static_cast<jint>(MdfChannelGetBitCount(channel));
}

/** Sets the bit count (resolution) of the channel.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @param bits the number of bits (e.g., 8, 16, 32, 64)
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetBitCount(
    JNIEnv* env, jobject obj, jlong channelPtr, jint bits) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetBitCount(channel, static_cast<uint32_t>(bits));
    }
}

/** Gets the data byte count for the channel (e.g., 1, 2, 4, 8 bytes).
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @return the number of data bytes per sample
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetDataBytes(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return static_cast<jlong>(MdfChannelGetDataBytes(channel));
}

/** Sets the data byte count for the channel.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @param bytes the number of data bytes per sample
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetDataBytes(
    JNIEnv* env, jobject obj, jlong channelPtr, jlong bytes) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetDataBytes(channel, static_cast<uint64_t>(bytes));
    }
}

/** Sets a signed integer value for the channel (for the current sample).
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @param value the signed integer value to set
 * @param valid 1 if the value is valid, 0 if invalid
 * @param arrayIndex array index for array channels (typically 0)
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetChannelValueAsSigned(
    JNIEnv* env, jobject obj, jlong channelPtr, jlong value,
    jint valid, jlong arrayIndex) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetChannelValueAsSigned(channel, static_cast<int64_t>(value),
            valid != 0, static_cast<uint64_t>(arrayIndex));
    }
}

/** Sets an unsigned integer value for the channel (for the current sample).
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @param value the unsigned integer value to set
 * @param valid 1 if the value is valid, 0 if invalid
 * @param arrayIndex array index for array channels (typically 0)
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetChannelValueAsUnSigned(
    JNIEnv* env, jobject obj, jlong channelPtr, jlong value,
    jint valid, jlong arrayIndex) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetChannelValueAsUnSigned(channel, static_cast<uint64_t>(value),
            valid != 0, static_cast<uint64_t>(arrayIndex));
    }
}

/** Sets a floating-point value for the channel (for the current sample).
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @param value the double value to set
 * @param valid 1 if the value is valid, 0 if invalid
 * @param arrayIndex array index for array channels (typically 0)
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetChannelValueAsFloat(
    JNIEnv* env, jobject obj, jlong channelPtr, jdouble value,
    jint valid, jlong arrayIndex) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetChannelValueAsFloat(channel, static_cast<double>(value),
            valid != 0, static_cast<uint64_t>(arrayIndex));
    }
}

/** Sets a string value for the channel (for the current sample).
 *
 * Converts the Java byte array to a C-string, then sets
 * the channel value through the MdfChannelSetChannelValueAsString export.
 *
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param channelPtr native pointer to the IChannel object
 * @param jvalue Java byte array containing the string data
 * @param valid 1 if the value is valid, 0 if invalid
 * @param arrayIndex array index for array channels (typically 0)
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetChannelValueAsString(
    JNIEnv* env, jobject obj, jlong channelPtr, jbyteArray jvalue,
    jint valid, jlong arrayIndex) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr || jvalue == nullptr) return;
    std::string value = string_from_java_bytes(env, jvalue);
    MdfChannelSetChannelValueAsString(channel, value.c_str(), valid != 0,
        static_cast<uint64_t>(arrayIndex));
}

/* ========================================================================
 * MdfChannelObserver JNI implementations
 *
 * Channel observers provide read access to individual channel sample
 * values after data has been loaded. Each observer monitors one channel
 * and provides methods to retrieve raw or engineering values.
 *
 * Observer lifecycle:
 *   1. Create observer via Create or CreateByChannelName
 *   2. Read data for the parent data group (MdfReaderReadData)
 *   3. Retrieve sample values via GetChannelValue/GetEngValue methods
 *   4. Free the observer via MdfChannelObserverUnInit
 *
 * Value retrieval pattern:
 *   Output arrays are used to return (value, valid_flag) pairs:
 *   - Long arrays: [0] = value, [1] = 1 if valid else 0
 *   - Double arrays: [0] = value, [1] = 1.0 if valid else 0.0
 * ======================================================================== */

/** Creates a channel observer for a specific channel.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param dgPtr native pointer to the IDataGroup
 * @param cgPtr native pointer to the IChannelGroup
 * @param chPtr native pointer to the IChannel to observe
 * @return jlong pointer to the new IChannelObserver, or 0 on failure
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverCreate(
    JNIEnv* env, jobject obj, jlong dgPtr, jlong cgPtr, jlong chPtr) {
    void* dg = ptr_from_jlong<void>(dgPtr);
    void* cg = ptr_from_jlong<void>(cgPtr);
    void* ch = ptr_from_jlong<void>(chPtr);
    if (dg == nullptr || cg == nullptr || ch == nullptr) return 0;
    return ptr_to_jlong(MdfChannelObserverCreate(dg, cg, ch));
}

/** Creates a channel observer by channel name.
 *
 * This convenience method searches for a channel by name within the
 * data group and creates an observer for it. If multiple channels
 * share the same name, the one with the most samples is selected.
 *
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param dgPtr native pointer to the IDataGroup
 * @param jchannelName Java String with the channel name to find
 * @return jlong pointer to the new IChannelObserver, or 0 if not found
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverCreateByChannelName(
    JNIEnv* env, jobject obj, jlong dgPtr, jstring jchannelName) {
    void* dg = ptr_from_jlong<void>(dgPtr);
    if (dg == nullptr || jchannelName == nullptr) return 0;
    const char* channelName = env->GetStringUTFChars(jchannelName, nullptr);
    if (channelName == nullptr) return 0;
    void* observer = MdfChannelObserverCreateByChannelName(dg, channelName);
    env->ReleaseStringUTFChars(jchannelName, channelName);
    return ptr_to_jlong(observer);
}

/** Releases a channel observer and frees native memory.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param observerPtr native pointer to the IChannelObserver to free
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverUnInit(
    JNIEnv* env, jobject obj, jlong observerPtr) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer != nullptr) {
        MdfChannelObserverUnInit(observer);
    }
}

/** Gets the number of samples in an observer.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param observerPtr native pointer to the IChannelObserver
 * @return the number of available samples
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetNofSamples(
    JNIEnv* env, jobject obj, jlong observerPtr) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr) return 0;
    return static_cast<jlong>(MdfChannelObserverGetNofSamples(observer));
}

/** Gets the name of the observed channel.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param observerPtr native pointer to the IChannelObserver
 * @param jname byte array for output (null to query length)
 * @return the length of the name string
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetName(
    JNIEnv* env, jobject obj, jlong observerPtr, jbyteArray jname) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr) return 0;
    if (jname == nullptr) {
        return static_cast<jlong>(MdfChannelObserverGetName(observer, nullptr));
    }
    size_t len = MdfChannelObserverGetName(observer, nullptr);
    std::vector<char> buf(len + 1);
    MdfChannelObserverGetName(observer, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jname);
}

/** Checks if the observer is for a master channel (e.g., time).
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param observerPtr native pointer to the IChannelObserver
 * @return JNI_TRUE if the observed channel is a master channel
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverIsMaster(
    JNIEnv* env, jobject obj, jlong observerPtr) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr) return JNI_FALSE;
    return MdfChannelObserverIsMaster(observer) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Gets a channel value as a signed integer.
 *
 * Output array format: [0] = value, [1] = valid flag (1=valid, 0=invalid)
 * The value is retrieved as an int64_t and stored in the jlong array.
 *
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param observerPtr native pointer to the IChannelObserver
 * @param sample zero-based sample index
 * @param jvalue output long[2] array: [value, validFlag]
 * @return JNI_TRUE if the value was retrieved successfully
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetChannelValueAsSigned(
    JNIEnv* env, jobject obj, jlong observerPtr, jlong sample, jlongArray jvalue) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr || jvalue == nullptr) return JNI_FALSE;
    int64_t value = 0;
    bool valid = MdfChannelObserverGetChannelValueAsSigned(observer,
        static_cast<uint64_t>(sample), value);
    jlong result[2] = {static_cast<jlong>(value), valid ? 1 : 0};
    env->SetLongArrayRegion(jvalue, 0, 2, result);
    return valid ? JNI_TRUE : JNI_FALSE;
}

/**
 * Gets a channel value as an unsigned integer.
 * Output array format: [0] = value, [1] = valid flag
 * @param env JNI environment pointer
 * @param observerPtr native pointer to the IChannelObserver
 * @param sample zero-based sample index
 * @param jvalue output long[2] array: [value, validFlag]
 * @return JNI_TRUE if the value was retrieved successfully
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetChannelValueAsUnSigned(
    JNIEnv* env, jobject obj, jlong observerPtr, jlong sample, jlongArray jvalue) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr || jvalue == nullptr) return JNI_FALSE;
    uint64_t value = 0;
    bool valid = MdfChannelObserverGetChannelValueAsUnSigned(observer,
        static_cast<uint64_t>(sample), value);
    jlong result[2] = {static_cast<jlong>(value), valid ? 1 : 0};
    env->SetLongArrayRegion(jvalue, 0, 2, result);
    return valid ? JNI_TRUE : JNI_FALSE;
}

/**
 * Gets a channel value as a floating-point number.
 *
 * This function retrieves the raw (unconverted) channel value for a
 * specific sample index as a double-precision floating point number.
 *
 * The output array format is:
 *   - index 0: the channel value (double)
 *   - index 1: validity flag (1.0 = valid, 0.0 = invalid)
 *
 * This format avoids the need for special "output parameter" handling
 * in JNI, which would require creating wrapper objects.
 *
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param observerPtr native pointer to the IChannelObserver
 * @param sample zero-based sample index
 * @param jvalue output double[2] array: [value, validFlag]
 * @return JNI_TRUE if the value was retrieved, JNI_FALSE on error
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetChannelValueAsFloat(
    JNIEnv* env, jobject obj, jlong observerPtr, jlong sample, jdoubleArray jvalue) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr || jvalue == nullptr) return JNI_FALSE;
    double value = 0.0;
    bool valid = MdfChannelObserverGetChannelValueAsFloat(observer,
        static_cast<uint64_t>(sample), value);
    jdouble result[2] = {value, valid ? 1.0 : 0.0};
    env->SetDoubleArrayRegion(jvalue, 0, 2, result);
    return valid ? JNI_TRUE : JNI_FALSE;
}

/** Gets an engineering value as a signed integer (with conversion applied).
 * @param env JNI environment pointer
 * @param observerPtr native pointer to the IChannelObserver
 * @param sample zero-based sample index
 * @param jvalue output long[2] array: [value, validFlag]
 * @return JNI_TRUE if the value was retrieved successfully
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetEngValueAsSigned(
    JNIEnv* env, jobject obj, jlong observerPtr, jlong sample, jlongArray jvalue) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr || jvalue == nullptr) return JNI_FALSE;
    int64_t value = 0;
    bool valid = MdfChannelObserverGetEngValueAsSigned(observer,
        static_cast<uint64_t>(sample), value);
    jlong result[2] = {static_cast<jlong>(value), valid ? 1 : 0};
    env->SetLongArrayRegion(jvalue, 0, 2, result);
    return valid ? JNI_TRUE : JNI_FALSE;
}

/** Gets an engineering value as an unsigned integer (with conversion applied).
 * @param env JNI environment pointer
 * @param observerPtr native pointer to the IChannelObserver
 * @param sample zero-based sample index
 * @param jvalue output long[2] array: [value, validFlag]
 * @return JNI_TRUE if the value was retrieved successfully
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetEngValueAsUnSigned(
    JNIEnv* env, jobject obj, jlong observerPtr, jlong sample, jlongArray jvalue) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr || jvalue == nullptr) return JNI_FALSE;
    uint64_t value = 0;
    bool valid = MdfChannelObserverGetEngValueAsUnSigned(observer,
        static_cast<uint64_t>(sample), value);
    jlong result[2] = {static_cast<jlong>(value), valid ? 1 : 0};
    env->SetLongArrayRegion(jvalue, 0, 2, result);
    return valid ? JNI_TRUE : JNI_FALSE;
}

/** Gets an engineering value as a floating-point number (with conversion applied).
 *
 * This is the most commonly used method for reading measurement data,
 * as it returns the converted (engineering) value rather than the raw
 * channel value. Engineering values have CC (conversion rule) applied.
 *
 * @param env JNI environment pointer
 * @param observerPtr native pointer to the IChannelObserver
 * @param sample zero-based sample index
 * @param jvalue output double[2] array: [value, validFlag]
 * @return JNI_TRUE if the value was retrieved successfully
 */
extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetEngValueAsFloat(
    JNIEnv* env, jobject obj, jlong observerPtr, jlong sample, jdoubleArray jvalue) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr || jvalue == nullptr) return JNI_FALSE;
    double value = 0.0;
    bool valid = MdfChannelObserverGetEngValueAsFloat(observer,
        static_cast<uint64_t>(sample), value);
    jdouble result[2] = {value, valid ? 1.0 : 0.0};
    env->SetDoubleArrayRegion(jvalue, 0, 2, result);
    return valid ? JNI_TRUE : JNI_FALSE;
}

/* ========================================================================
 * MdfFileHistory JNI implementations
 *
 * File history entries record tool information and timestamps for
 * each modification of the MDF file. They are stored in the header
 * block and track the software used to create/modify the file.
 * ======================================================================== */

/** Gets the tool name from a file history entry.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param fhPtr native pointer to the IFileHistory object
 * @param jname byte array for output (null to query length)
 * @return the length of the tool name string
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistoryGetToolName(
    JNIEnv* env, jobject obj, jlong fhPtr, jbyteArray jname) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr) return 0;
    if (jname == nullptr) {
        return static_cast<jlong>(MdfFileHistoryGetToolName(fh, nullptr));
    }
    size_t len = MdfFileHistoryGetToolName(fh, nullptr);
    std::vector<char> buf(len + 1);
    MdfFileHistoryGetToolName(fh, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jname);
}

/** Sets the tool name in a file history entry.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param fhPtr native pointer to the IFileHistory object
 * @param jname Java String with the tool name to set
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistorySetToolName(
    JNIEnv* env, jobject obj, jlong fhPtr, jstring jname) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr || jname == nullptr) return;
    const char* name = env->GetStringUTFChars(jname, nullptr);
    if (name == nullptr) return;
    MdfFileHistorySetToolName(fh, name);
    env->ReleaseStringUTFChars(jname, name);
}

/** Gets the description from a file history entry.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param fhPtr native pointer to the IFileHistory object
 * @param jdesc byte array for output (null to query length)
 * @return the length of the description string
 */
extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistoryGetDescription(
    JNIEnv* env, jobject obj, jlong fhPtr, jbyteArray jdesc) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr) return 0;
    if (jdesc == nullptr) {
        return static_cast<jlong>(MdfFileHistoryGetDescription(fh, nullptr));
    }
    size_t len = MdfFileHistoryGetDescription(fh, nullptr);
    std::vector<char> buf(len + 1);
    MdfFileHistoryGetDescription(fh, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jdesc);
}

/** Sets the description in a file history entry.
 * @param env JNI environment pointer
 * @param obj Java object reference
 * @param fhPtr native pointer to the IFileHistory object
 * @param jdesc Java String with the description to set
 */
extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistorySetDescription(
    JNIEnv* env, jobject obj, jlong fhPtr, jstring jdesc) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr || jdesc == nullptr) return;
    const char* desc = env->GetStringUTFChars(jdesc, nullptr);
    if (desc == nullptr) return;
    MdfFileHistorySetDescription(fh, desc);
    env->ReleaseStringUTFChars(jdesc, desc);
}

/* ========================================================================
 * NEW: MdfReader additional JNI implementations
 * ======================================================================== */

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfReaderGetIndex(
    JNIEnv* env, jobject obj, jlong readerPtr) {
    void* reader = ptr_from_jlong<void>(readerPtr);
    if (reader == nullptr) return 0;
    return static_cast<jlong>(MdfReaderGetIndex(reader));
}

/* ========================================================================
 * NEW: MdfWriter additional JNI implementations
 * ======================================================================== */

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterIsFileNew(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return JNI_FALSE;
    return MdfWriterIsFileNew(writer) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterGetCompressData(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return JNI_FALSE;
    return MdfWriterGetCompressData(writer) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jdouble JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterGetPreTrigTime(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return 0.0;
    return static_cast<jdouble>(MdfWriterGetPreTrigTime(writer));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterSetPreTrigTime(
    JNIEnv* env, jobject obj, jlong writerPtr, jdouble preTrigTime) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer != nullptr) {
        MdfWriterSetPreTrigTime(writer, static_cast<double>(preTrigTime));
    }
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterGetStartTime(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return 0;
    return static_cast<jlong>(MdfWriterGetStartTime(writer));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterGetStopTime(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return 0;
    return static_cast<jlong>(MdfWriterGetStopTime(writer));
}

extern "C" JNI_EXPORT jshort JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterGetBusType(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return 0;
    return static_cast<jshort>(MdfWriterGetBusType(writer));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterSetBusType(
    JNIEnv* env, jobject obj, jlong writerPtr, jshort type) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer != nullptr) {
        MdfWriterSetBusType(writer, static_cast<uint16_t>(type));
    }
}

extern "C" JNI_EXPORT jint JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterGetStorageType(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return 0;
    return static_cast<jint>(MdfWriterGetStorageType(writer));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterSetStorageType(
    JNIEnv* env, jobject obj, jlong writerPtr, jint type) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer != nullptr) {
        MdfWriterSetStorageType(writer, static_cast<int>(type));
    }
}

extern "C" JNI_EXPORT jint JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterGetMaxLength(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return 0;
    return static_cast<jint>(MdfWriterGetMaxLength(writer));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterSetMaxLength(
    JNIEnv* env, jobject obj, jlong writerPtr, jint length) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer != nullptr) {
        MdfWriterSetMaxLength(writer, static_cast<uint32_t>(length));
    }
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfWriterCreateBusLogConfiguration(
    JNIEnv* env, jobject obj, jlong writerPtr) {
    void* writer = ptr_from_jlong<void>(writerPtr);
    if (writer == nullptr) return JNI_FALSE;
    return MdfWriterCreateBusLogConfiguration(writer) ? JNI_TRUE : JNI_FALSE;
}

/* ========================================================================
 * NEW: MdfFile additional JNI implementations
 * ======================================================================== */

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileSetFileName(
    JNIEnv* env, jobject obj, jlong filePtr, jstring jfilename) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr || jfilename == nullptr) return;
    const char* filename = env->GetStringUTFChars(jfilename, nullptr);
    if (filename == nullptr) return;
    MdfFileSetFileName(file, filename);
    env->ReleaseStringUTFChars(jfilename, filename);
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileSetMinorVersion(
    JNIEnv* env, jobject obj, jlong filePtr, jint minor) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file != nullptr) {
        MdfFileSetMinorVersion(file, static_cast<int>(minor));
    }
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileGetProgramId(
    JNIEnv* env, jobject obj, jlong filePtr, jbyteArray jprogramId) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return 0;
    if (jprogramId == nullptr) {
        return static_cast<jlong>(MdfFileGetProgramId(file, nullptr));
    }
    size_t len = MdfFileGetProgramId(file, nullptr);
    std::vector<char> buf(len + 1);
    MdfFileGetProgramId(file, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jprogramId);
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileSetProgramId(
    JNIEnv* env, jobject obj, jlong filePtr, jstring jprogramId) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr || jprogramId == nullptr) return;
    const char* programId = env->GetStringUTFChars(jprogramId, nullptr);
    if (programId == nullptr) return;
    MdfFileSetProgramId(file, programId);
    env->ReleaseStringUTFChars(jprogramId, programId);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileGetHeader(
    JNIEnv* env, jobject obj, jlong filePtr) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfFileGetHeader(file)));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileGetAttachments(
    JNIEnv* env, jobject obj, jlong filePtr, jlongArray jattachments) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return 0;
    if (jattachments == nullptr) {
        return static_cast<jlong>(MdfFileGetAttachments(file, nullptr));
    }
    size_t count = MdfFileGetAttachments(file, nullptr);
    std::vector<const void*> ptrs(count);
    MdfFileGetAttachments(file, ptrs.data());
    jsize len = static_cast<jsize>(count);
    std::vector<jlong> jlongs(len);
    for (jsize i = 0; i < len; i++) {
        jlongs[i] = ptr_to_jlong(const_cast<void*>(ptrs[i]));
    }
    env->SetLongArrayRegion(jattachments, 0, len, jlongs.data());
    return static_cast<jlong>(count);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileCreateAttachment(
    JNIEnv* env, jobject obj, jlong filePtr) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return 0;
    return ptr_to_jlong(MdfFileCreateAttachment(file));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileCreateDataGroup(
    JNIEnv* env, jobject obj, jlong filePtr) {
    void* file = ptr_from_jlong<void>(filePtr);
    if (file == nullptr) return 0;
    return ptr_to_jlong(MdfFileCreateDataGroup(file));
}

/* ========================================================================
 * NEW: MdfHeader additional JNI implementations
 * ======================================================================== */

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetIndex(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    return static_cast<jlong>(MdfHeaderGetIndex(header));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetMeasurementId(
    JNIEnv* env, jobject obj, jlong headerPtr, jbyteArray juuid) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    if (juuid == nullptr) {
        return static_cast<jlong>(MdfHeaderGetMeasurementId(header, nullptr));
    }
    size_t len = MdfHeaderGetMeasurementId(header, nullptr);
    std::vector<char> buf(len + 1);
    MdfHeaderGetMeasurementId(header, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, juuid);
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderSetMeasurementId(
    JNIEnv* env, jobject obj, jlong headerPtr, jstring juuid) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr || juuid == nullptr) return;
    const char* uuid = env->GetStringUTFChars(juuid, nullptr);
    if (uuid == nullptr) return;
    MdfHeaderSetMeasurementId(header, uuid);
    env->ReleaseStringUTFChars(juuid, uuid);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetRecorderId(
    JNIEnv* env, jobject obj, jlong headerPtr, jbyteArray juuid) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    if (juuid == nullptr) {
        return static_cast<jlong>(MdfHeaderGetRecorderId(header, nullptr));
    }
    size_t len = MdfHeaderGetRecorderId(header, nullptr);
    std::vector<char> buf(len + 1);
    MdfHeaderGetRecorderId(header, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, juuid);
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderSetRecorderId(
    JNIEnv* env, jobject obj, jlong headerPtr, jstring juuid) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr || juuid == nullptr) return;
    const char* uuid = env->GetStringUTFChars(juuid, nullptr);
    if (uuid == nullptr) return;
    MdfHeaderSetRecorderId(header, uuid);
    env->ReleaseStringUTFChars(juuid, uuid);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetRecorderIndex(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    return static_cast<jlong>(MdfHeaderGetRecorderIndex(header));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderSetRecorderIndex(
    JNIEnv* env, jobject obj, jlong headerPtr, jlong index) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header != nullptr) {
        MdfHeaderSetRecorderIndex(header, static_cast<int64_t>(index));
    }
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderSetStartTime(
    JNIEnv* env, jobject obj, jlong headerPtr, jlong time) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header != nullptr) {
        MdfHeaderSetStartTime(header, static_cast<uint64_t>(time));
    }
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderIsStartAngleUsed(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return JNI_FALSE;
    return MdfHeaderIsStartAngleUsed(header) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jdouble JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetStartAngle(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0.0;
    return static_cast<jdouble>(MdfHeaderGetStartAngle(header));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderSetStartAngle(
    JNIEnv* env, jobject obj, jlong headerPtr, jdouble angle) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header != nullptr) {
        MdfHeaderSetStartAngle(header, static_cast<double>(angle));
    }
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderIsStartDistanceUsed(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return JNI_FALSE;
    return MdfHeaderIsStartDistanceUsed(header) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jdouble JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetStartDistance(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0.0;
    return static_cast<jdouble>(MdfHeaderGetStartDistance(header));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderSetStartDistance(
    JNIEnv* env, jobject obj, jlong headerPtr, jdouble distance) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header != nullptr) {
        MdfHeaderSetStartDistance(header, static_cast<double>(distance));
    }
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetMetaData(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfHeaderGetMetaData(header)));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetAttachments(
    JNIEnv* env, jobject obj, jlong headerPtr, jlongArray jattachments) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    if (jattachments == nullptr) {
        return static_cast<jlong>(MdfHeaderGetAttachments(header, nullptr));
    }
    size_t count = MdfHeaderGetAttachments(header, nullptr);
    std::vector<void*> ptrs(count);
    MdfHeaderGetAttachments(header, ptrs.data());
    jsize len = static_cast<jsize>(count);
    std::vector<jlong> jlongs(len);
    for (jsize i = 0; i < len; i++) {
        jlongs[i] = ptr_to_jlong(ptrs[i]);
    }
    env->SetLongArrayRegion(jattachments, 0, len, jlongs.data());
    return static_cast<jlong>(count);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetFileHistorys(
    JNIEnv* env, jobject obj, jlong headerPtr, jlongArray jfileHistorys) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    if (jfileHistorys == nullptr) {
        return static_cast<jlong>(MdfHeaderGetFileHistorys(header, nullptr));
    }
    size_t count = MdfHeaderGetFileHistorys(header, nullptr);
    std::vector<void*> ptrs(count);
    MdfHeaderGetFileHistorys(header, ptrs.data());
    jsize len = static_cast<jsize>(count);
    std::vector<jlong> jlongs(len);
    for (jsize i = 0; i < len; i++) {
        jlongs[i] = ptr_to_jlong(ptrs[i]);
    }
    env->SetLongArrayRegion(jfileHistorys, 0, len, jlongs.data());
    return static_cast<jlong>(count);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderGetEvents(
    JNIEnv* env, jobject obj, jlong headerPtr, jlongArray jevents) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    if (jevents == nullptr) {
        return static_cast<jlong>(MdfHeaderGetEvents(header, nullptr));
    }
    size_t count = MdfHeaderGetEvents(header, nullptr);
    std::vector<void*> ptrs(count);
    MdfHeaderGetEvents(header, ptrs.data());
    jsize len = static_cast<jsize>(count);
    std::vector<jlong> jlongs(len);
    for (jsize i = 0; i < len; i++) {
        jlongs[i] = ptr_to_jlong(ptrs[i]);
    }
    env->SetLongArrayRegion(jevents, 0, len, jlongs.data());
    return static_cast<jlong>(count);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderCreateMetaData(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    return ptr_to_jlong(MdfHeaderCreateMetaData(header));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderCreateAttachment(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    return ptr_to_jlong(MdfHeaderCreateAttachment(header));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfHeaderCreateEvent(
    JNIEnv* env, jobject obj, jlong headerPtr) {
    void* header = ptr_from_jlong<void>(headerPtr);
    if (header == nullptr) return 0;
    return ptr_to_jlong(MdfHeaderCreateEvent(header));
}

/* ========================================================================
 * NEW: MdfDataGroup additional JNI implementations
 * ======================================================================== */

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfDataGroupGetIndex(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return static_cast<jlong>(MdfDataGroupGetIndex(group));
}

extern "C" JNI_EXPORT jbyte JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfDataGroupGetRecordIdSize(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return static_cast<jbyte>(MdfDataGroupGetRecordIdSize(group));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfDataGroupGetMetaData(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfDataGroupGetMetaData(group)));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfDataGroupGetChannelGroupByName(
    JNIEnv* env, jobject obj, jlong groupPtr, jstring jname) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr || jname == nullptr) return 0;
    const char* name = env->GetStringUTFChars(jname, nullptr);
    if (name == nullptr) return 0;
    void* result = MdfDataGroupGetChannelGroupByName(group, name);
    env->ReleaseStringUTFChars(jname, name);
    return ptr_to_jlong(result);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfDataGroupGetChannelGroupByRecordId(
    JNIEnv* env, jobject obj, jlong groupPtr, jlong recordId) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return ptr_to_jlong(MdfDataGroupGetChannelGroupByRecordId(group,
        static_cast<uint64_t>(recordId)));
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfDataGroupIsRead(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return JNI_FALSE;
    return MdfDataGroupIsRead(group) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfDataGroupCreateMetaData(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return ptr_to_jlong(MdfDataGroupCreateMetaData(group));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfDataGroupFindParentChannelGroup(
    JNIEnv* env, jobject obj, jlong groupPtr, jlong channelPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (group == nullptr || channel == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfDataGroupFindParentChannelGroup(group, channel)));
}

/* ========================================================================
 * NEW: MdfChannelGroup additional JNI implementations
 * ======================================================================== */

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupGetIndex(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return static_cast<jlong>(MdfChannelGroupGetIndex(group));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupGetRecordId(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return static_cast<jlong>(MdfChannelGroupGetRecordId(group));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupGetDescription(
    JNIEnv* env, jobject obj, jlong groupPtr, jbyteArray jdesc) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    if (jdesc == nullptr) {
        return static_cast<jlong>(MdfChannelGroupGetDescription(group, nullptr));
    }
    size_t len = MdfChannelGroupGetDescription(group, nullptr);
    std::vector<char> buf(len + 1);
    MdfChannelGroupGetDescription(group, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jdesc);
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupSetDescription(
    JNIEnv* env, jobject obj, jlong groupPtr, jstring jdesc) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr || jdesc == nullptr) return;
    const char* desc = env->GetStringUTFChars(jdesc, nullptr);
    if (desc == nullptr) return;
    MdfChannelGroupSetDescription(group, desc);
    env->ReleaseStringUTFChars(jdesc, desc);
}

extern "C" JNI_EXPORT jshort JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupGetFlags(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return static_cast<jshort>(MdfChannelGroupGetFlags(group));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupSetFlags(
    JNIEnv* env, jobject obj, jlong groupPtr, jshort flags) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group != nullptr) {
        MdfChannelGroupSetFlags(group, static_cast<uint16_t>(flags));
    }
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupGetMetaData(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfChannelGroupGetMetaData(group)));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupCreateMetaData(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return ptr_to_jlong(MdfChannelGroupCreateMetaData(group));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGroupCreateSourceInformation(
    JNIEnv* env, jobject obj, jlong groupPtr) {
    void* group = ptr_from_jlong<void>(groupPtr);
    if (group == nullptr) return 0;
    return ptr_to_jlong(MdfChannelGroupCreateSourceInformation(group));
}

/* ========================================================================
 * NEW: MdfChannel additional JNI implementations
 * ======================================================================== */

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetIndex(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return static_cast<jlong>(MdfChannelGetIndex(channel));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetDisplayName(
    JNIEnv* env, jobject obj, jlong channelPtr, jbyteArray jname) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    if (jname == nullptr) {
        return static_cast<jlong>(MdfChannelGetDisplayName(channel, nullptr));
    }
    size_t len = MdfChannelGetDisplayName(channel, nullptr);
    std::vector<char> buf(len + 1);
    MdfChannelGetDisplayName(channel, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jname);
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetDisplayName(
    JNIEnv* env, jobject obj, jlong channelPtr, jstring jname) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr || jname == nullptr) return;
    const char* name = env->GetStringUTFChars(jname, nullptr);
    if (name == nullptr) return;
    MdfChannelSetDisplayName(channel, name);
    env->ReleaseStringUTFChars(jname, name);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetDescription(
    JNIEnv* env, jobject obj, jlong channelPtr, jbyteArray jdesc) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    if (jdesc == nullptr) {
        return static_cast<jlong>(MdfChannelGetDescription(channel, nullptr));
    }
    size_t len = MdfChannelGetDescription(channel, nullptr);
    std::vector<char> buf(len + 1);
    MdfChannelGetDescription(channel, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jdesc);
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetDescription(
    JNIEnv* env, jobject obj, jlong channelPtr, jstring jdesc) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr || jdesc == nullptr) return;
    const char* desc = env->GetStringUTFChars(jdesc, nullptr);
    if (desc == nullptr) return;
    MdfChannelSetDescription(channel, desc);
    env->ReleaseStringUTFChars(jdesc, desc);
}

extern "C" JNI_EXPORT jint JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetFlags(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return static_cast<jint>(MdfChannelGetFlags(channel));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetFlags(
    JNIEnv* env, jobject obj, jlong channelPtr, jint flags) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetFlags(channel, static_cast<uint32_t>(flags));
    }
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelIsPrecisionUsed(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return JNI_FALSE;
    return MdfChannelIsPrecisionUsed(channel) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jbyte JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetPrecision(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return static_cast<jbyte>(MdfChannelGetPrecision(channel));
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelIsRangeUsed(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return JNI_FALSE;
    return MdfChannelIsRangeUsed(channel) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jdouble JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetRangeMin(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0.0;
    return static_cast<jdouble>(MdfChannelGetRangeMin(channel));
}

extern "C" JNI_EXPORT jdouble JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetRangeMax(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0.0;
    return static_cast<jdouble>(MdfChannelGetRangeMax(channel));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetRange(
    JNIEnv* env, jobject obj, jlong channelPtr, jdouble min, jdouble max) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetRange(channel, static_cast<double>(min), static_cast<double>(max));
    }
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelIsLimitUsed(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return JNI_FALSE;
    return MdfChannelIsLimitUsed(channel) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jdouble JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetLimitMin(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0.0;
    return static_cast<jdouble>(MdfChannelGetLimitMin(channel));
}

extern "C" JNI_EXPORT jdouble JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetLimitMax(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0.0;
    return static_cast<jdouble>(MdfChannelGetLimitMax(channel));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetLimit(
    JNIEnv* env, jobject obj, jlong channelPtr, jdouble min, jdouble max) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetLimit(channel, static_cast<double>(min), static_cast<double>(max));
    }
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelIsExtLimitUsed(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return JNI_FALSE;
    return MdfChannelIsExtLimitUsed(channel) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jdouble JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetExtLimitMin(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0.0;
    return static_cast<jdouble>(MdfChannelGetExtLimitMin(channel));
}

extern "C" JNI_EXPORT jdouble JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetExtLimitMax(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0.0;
    return static_cast<jdouble>(MdfChannelGetExtLimitMax(channel));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetExtLimit(
    JNIEnv* env, jobject obj, jlong channelPtr, jdouble min, jdouble max) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetExtLimit(channel, static_cast<double>(min), static_cast<double>(max));
    }
}

extern "C" JNI_EXPORT jdouble JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetSamplingRate(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0.0;
    return static_cast<jdouble>(MdfChannelGetSamplingRate(channel));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetVlsdRecordId(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return static_cast<jlong>(MdfChannelGetVlsdRecordId(channel));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetVlsdRecordId(
    JNIEnv* env, jobject obj, jlong channelPtr, jlong recordId) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetVlsdRecordId(channel, static_cast<uint64_t>(recordId));
    }
}

extern "C" JNI_EXPORT jshort JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetBitOffset(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return static_cast<jshort>(MdfChannelGetBitOffset(channel));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetBitOffset(
    JNIEnv* env, jobject obj, jlong channelPtr, jshort bits) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel != nullptr) {
        MdfChannelSetBitOffset(channel, static_cast<uint16_t>(bits));
    }
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetMetaData(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfChannelGetMetaData(channel)));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetSourceInformation(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfChannelGetSourceInformation(channel)));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetChannelConversion(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfChannelGetChannelConversion(channel)));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetChannelArray(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfChannelGetChannelArray(channel)));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelGetChannelCompositions(
    JNIEnv* env, jobject obj, jlong channelPtr, jlongArray jchannels) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    if (jchannels == nullptr) {
        return static_cast<jlong>(MdfChannelGetChannelCompositions(channel, nullptr));
    }
    size_t count = MdfChannelGetChannelCompositions(channel, nullptr);
    std::vector<void*> ptrs(count);
    MdfChannelGetChannelCompositions(channel, ptrs.data());
    jsize len = static_cast<jsize>(count);
    std::vector<jlong> jlongs(len);
    for (jsize i = 0; i < len; i++) {
        jlongs[i] = ptr_to_jlong(ptrs[i]);
    }
    env->SetLongArrayRegion(jchannels, 0, len, jlongs.data());
    return static_cast<jlong>(count);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelCreateSourceInformation(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return ptr_to_jlong(MdfChannelCreateSourceInformation(channel));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelCreateChannelConversion(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return ptr_to_jlong(MdfChannelCreateChannelConversion(channel));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelCreateChannelArray(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return ptr_to_jlong(MdfChannelCreateChannelArray(channel));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelCreateChannelComposition(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return ptr_to_jlong(MdfChannelCreateChannelComposition(channel));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelCreateMetaData(
    JNIEnv* env, jobject obj, jlong channelPtr) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr) return 0;
    return ptr_to_jlong(MdfChannelCreateMetaData(channel));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelSetChannelValueAsArray(
    JNIEnv* env, jobject obj, jlong channelPtr, jbyteArray jvalue,
    jint valid, jlong arrayIndex) {
    void* channel = ptr_from_jlong<void>(channelPtr);
    if (channel == nullptr || jvalue == nullptr) return;
    jsize len = env->GetArrayLength(jvalue);
    if (len <= 0) return;
    std::vector<uint8_t> buf(static_cast<size_t>(len));
    env->GetByteArrayRegion(jvalue, 0, len, reinterpret_cast<jbyte*>(buf.data()));
    MdfChannelSetChannelValueAsArray(channel, buf.data(), static_cast<size_t>(len),
        valid != 0, static_cast<uint64_t>(arrayIndex));
}

/* ========================================================================
 * NEW: MdfChannelObserver additional JNI implementations
 * ======================================================================== */

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverCreateForChannelGroup(
    JNIEnv* env, jobject obj, jlong dgPtr, jlong cgPtr, jlongArray jobservers) {
    void* dg = ptr_from_jlong<void>(dgPtr);
    void* cg = ptr_from_jlong<void>(cgPtr);
    if (dg == nullptr || cg == nullptr) return 0;
    if (jobservers == nullptr) {
        return static_cast<jlong>(MdfChannelObserverCreateForChannelGroup(dg, cg, nullptr));
    }
    size_t count = MdfChannelObserverCreateForChannelGroup(dg, cg, nullptr);
    std::vector<void*> ptrs(count);
    MdfChannelObserverCreateForChannelGroup(dg, cg, ptrs.data());
    jsize len = static_cast<jsize>(count);
    std::vector<jlong> jlongs(len);
    for (jsize i = 0; i < len; i++) {
        jlongs[i] = ptr_to_jlong(ptrs[i]);
    }
    env->SetLongArrayRegion(jobservers, 0, len, jlongs.data());
    return static_cast<jlong>(count);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetUnit(
    JNIEnv* env, jobject obj, jlong observerPtr, jbyteArray junit) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr) return 0;
    if (junit == nullptr) {
        return static_cast<jlong>(MdfChannelObserverGetUnit(observer, nullptr));
    }
    size_t len = MdfChannelObserverGetUnit(observer, nullptr);
    std::vector<char> buf(len + 1);
    MdfChannelObserverGetUnit(observer, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, junit);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetChannel(
    JNIEnv* env, jobject obj, jlong observerPtr) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfChannelObserverGetChannel(observer)));
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetChannelValueAsString(
    JNIEnv* env, jobject obj, jlong observerPtr, jlong sample,
    jbyteArray jvalue, jlongArray jsz) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr) return JNI_FALSE;
    if (jvalue == nullptr || jsz == nullptr) {
        size_t sz = 0;
        bool valid = MdfChannelObserverGetChannelValueAsString(observer,
            static_cast<uint64_t>(sample), nullptr, sz);
        if (jsz != nullptr) {
            jlong szResult[] = {static_cast<jlong>(sz)};
            env->SetLongArrayRegion(jsz, 0, 1, szResult);
        }
        return valid ? JNI_TRUE : JNI_FALSE;
    }
    size_t sz = 256;
    std::vector<char> buf(sz);
    bool valid = MdfChannelObserverGetChannelValueAsString(observer,
        static_cast<uint64_t>(sample), buf.data(), sz);
    if (valid && sz > 0) {
        env->SetByteArrayRegion(jvalue, 0, static_cast<jsize>(sz),
            reinterpret_cast<const jbyte*>(buf.data()));
    }
    jlong szResult[] = {static_cast<jlong>(sz)};
    env->SetLongArrayRegion(jsz, 0, 1, szResult);
    return valid ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetChannelValueAsArray(
    JNIEnv* env, jobject obj, jlong observerPtr, jlong sample,
    jbyteArray jvalue, jlongArray jsz) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr) return JNI_FALSE;
    size_t sz = 0;
    if (jvalue == nullptr) {
        bool valid = MdfChannelObserverGetChannelValueAsArray(observer,
            static_cast<uint64_t>(sample), nullptr, sz);
        if (jsz != nullptr) {
            jlong szResult[] = {static_cast<jlong>(sz)};
            env->SetLongArrayRegion(jsz, 0, 1, szResult);
        }
        return valid ? JNI_TRUE : JNI_FALSE;
    }
    sz = static_cast<size_t>(env->GetArrayLength(jvalue));
    std::vector<uint8_t> buf(sz);
    bool valid = MdfChannelObserverGetChannelValueAsArray(observer,
        static_cast<uint64_t>(sample), buf.data(), sz);
    if (valid) {
        env->SetByteArrayRegion(jvalue, 0, static_cast<jsize>(sz),
            reinterpret_cast<const jbyte*>(buf.data()));
    }
    if (jsz != nullptr) {
        jlong szResult[] = {static_cast<jlong>(sz)};
        env->SetLongArrayRegion(jsz, 0, 1, szResult);
    }
    return valid ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetEngValueAsString(
    JNIEnv* env, jobject obj, jlong observerPtr, jlong sample,
    jbyteArray jvalue, jlongArray jsz) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr) return JNI_FALSE;
    size_t sz = 0;
    if (jvalue == nullptr) {
        bool valid = MdfChannelObserverGetEngValueAsString(observer,
            static_cast<uint64_t>(sample), nullptr, sz);
        if (jsz != nullptr) {
            jlong szResult[] = {static_cast<jlong>(sz)};
            env->SetLongArrayRegion(jsz, 0, 1, szResult);
        }
        return valid ? JNI_TRUE : JNI_FALSE;
    }
    sz = 256;
    std::vector<char> buf(sz);
    bool valid = MdfChannelObserverGetEngValueAsString(observer,
        static_cast<uint64_t>(sample), buf.data(), sz);
    if (valid && sz > 0) {
        env->SetByteArrayRegion(jvalue, 0, static_cast<jsize>(sz),
            reinterpret_cast<const jbyte*>(buf.data()));
    }
    if (jsz != nullptr) {
        jlong szResult[] = {static_cast<jlong>(sz)};
        env->SetLongArrayRegion(jsz, 0, 1, szResult);
    }
    return valid ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNI_EXPORT jboolean JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfChannelObserverGetEngValueAsArray(
    JNIEnv* env, jobject obj, jlong observerPtr, jlong sample,
    jbyteArray jvalue, jlongArray jsz) {
    void* observer = ptr_from_jlong<void>(observerPtr);
    if (observer == nullptr) return JNI_FALSE;
    size_t sz = 0;
    if (jvalue == nullptr) {
        bool valid = MdfChannelObserverGetEngValueAsArray(observer,
            static_cast<uint64_t>(sample), nullptr, sz);
        if (jsz != nullptr) {
            jlong szResult[] = {static_cast<jlong>(sz)};
            env->SetLongArrayRegion(jsz, 0, 1, szResult);
        }
        return valid ? JNI_TRUE : JNI_FALSE;
    }
    sz = static_cast<size_t>(env->GetArrayLength(jvalue));
    std::vector<uint8_t> buf(sz);
    bool valid = MdfChannelObserverGetEngValueAsArray(observer,
        static_cast<uint64_t>(sample), buf.data(), sz);
    if (valid) {
        env->SetByteArrayRegion(jvalue, 0, static_cast<jsize>(sz),
            reinterpret_cast<const jbyte*>(buf.data()));
    }
    if (jsz != nullptr) {
        jlong szResult[] = {static_cast<jlong>(sz)};
        env->SetLongArrayRegion(jsz, 0, 1, szResult);
    }
    return valid ? JNI_TRUE : JNI_FALSE;
}

/* ========================================================================
 * NEW: MdfFileHistory additional JNI implementations
 * ======================================================================== */

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistoryGetIndex(
    JNIEnv* env, jobject obj, jlong fhPtr) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr) return 0;
    return static_cast<jlong>(MdfFileHistoryGetIndex(fh));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistoryGetTime(
    JNIEnv* env, jobject obj, jlong fhPtr) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr) return 0;
    return static_cast<jlong>(MdfFileHistoryGetTime(fh));
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistorySetTime(
    JNIEnv* env, jobject obj, jlong fhPtr, jlong time) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh != nullptr) {
        MdfFileHistorySetTime(fh, static_cast<uint64_t>(time));
    }
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistoryGetMetaData(
    JNIEnv* env, jobject obj, jlong fhPtr) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr) return 0;
    return ptr_to_jlong(const_cast<void*>(MdfFileHistoryGetMetaData(fh)));
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistoryGetToolVendor(
    JNIEnv* env, jobject obj, jlong fhPtr, jbyteArray jvendor) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr) return 0;
    if (jvendor == nullptr) {
        return static_cast<jlong>(MdfFileHistoryGetToolVendor(fh, nullptr));
    }
    size_t len = MdfFileHistoryGetToolVendor(fh, nullptr);
    std::vector<char> buf(len + 1);
    MdfFileHistoryGetToolVendor(fh, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jvendor);
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistorySetToolVendor(
    JNIEnv* env, jobject obj, jlong fhPtr, jstring jvendor) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr || jvendor == nullptr) return;
    const char* vendor = env->GetStringUTFChars(jvendor, nullptr);
    if (vendor == nullptr) return;
    MdfFileHistorySetToolVendor(fh, vendor);
    env->ReleaseStringUTFChars(jvendor, vendor);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistoryGetToolVersion(
    JNIEnv* env, jobject obj, jlong fhPtr, jbyteArray jversion) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr) return 0;
    if (jversion == nullptr) {
        return static_cast<jlong>(MdfFileHistoryGetToolVersion(fh, nullptr));
    }
    size_t len = MdfFileHistoryGetToolVersion(fh, nullptr);
    std::vector<char> buf(len + 1);
    MdfFileHistoryGetToolVersion(fh, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, jversion);
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistorySetToolVersion(
    JNIEnv* env, jobject obj, jlong fhPtr, jstring jversion) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr || jversion == nullptr) return;
    const char* version = env->GetStringUTFChars(jversion, nullptr);
    if (version == nullptr) return;
    MdfFileHistorySetToolVersion(fh, version);
    env->ReleaseStringUTFChars(jversion, version);
}

extern "C" JNI_EXPORT jlong JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistoryGetUserName(
    JNIEnv* env, jobject obj, jlong fhPtr, jbyteArray juser) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr) return 0;
    if (juser == nullptr) {
        return static_cast<jlong>(MdfFileHistoryGetUserName(fh, nullptr));
    }
    size_t len = MdfFileHistoryGetUserName(fh, nullptr);
    std::vector<char> buf(len + 1);
    MdfFileHistoryGetUserName(fh, buf.data());
    return copy_cstring_to_java(env, buf.data(), len, juser);
}

extern "C" JNI_EXPORT void JNICALL
Java_com_huawei_behavior_simulation_datawatch_service_mdflib_jni_MdfLibraryNativeJNI_MdfFileHistorySetUserName(
    JNIEnv* env, jobject obj, jlong fhPtr, jstring juser) {
    void* fh = ptr_from_jlong<void>(fhPtr);
    if (fh == nullptr || juser == nullptr) return;
    const char* user = env->GetStringUTFChars(juser, nullptr);
    if (user == nullptr) return;
    MdfFileHistorySetUserName(fh, user);
    env->ReleaseStringUTFChars(juser, user);
}
