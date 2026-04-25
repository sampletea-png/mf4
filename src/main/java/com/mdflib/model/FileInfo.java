package com.mdflib.model;

/**
 * Represents file-level metadata for an MDF file.
 *
 * <p>Contains information about the file name, MDF format version,
 * and whether the file is in MDF4 format.</p>
 *
 * @author mdflib-java contributors
 * @version 1.0.0
 * @since 1.0.0
 */
public class FileInfo {

    /** The internal name of the MDF file. */
    private final String name;

    /** The full file path of the MDF file. */
    private final String fileName;

    /** The MDF format version string (e.g., "4.10"). */
    private final String version;

    /** The main version number (e.g., 4 for MDF4). */
    private final int mainVersion;

    /** The minor version number (e.g., 10 for MDF 4.10). */
    private final int minorVersion;

    /** Whether this file uses the MDF4 format. */
    private final boolean isMdf4;

    /**
     * Constructs a FileInfo with all metadata fields.
     *
     * @param name the internal file name
     * @param fileName the full file path
     * @param version the version string
     * @param mainVersion the main version number
     * @param minorVersion the minor version number
     * @param isMdf4 true if the file is MDF4 format
     */
    public FileInfo(String name, String fileName, String version,
                    int mainVersion, int minorVersion, boolean isMdf4) {
        this.name = name;
        this.fileName = fileName;
        this.version = version;
        this.mainVersion = mainVersion;
        this.minorVersion = minorVersion;
        this.isMdf4 = isMdf4;
    }

    /** @return the internal file name */
    public String getName() { return name; }

    /** @return the full file path */
    public String getFileName() { return fileName; }

    /** @return the version string */
    public String getVersion() { return version; }

    /** @return the main version number */
    public int getMainVersion() { return mainVersion; }

    /** @return the minor version number */
    public int getMinorVersion() { return minorVersion; }

    /** @return true if the file is MDF4 format */
    public boolean isMdf4() { return isMdf4; }

    @Override
    public String toString() {
        return "FileInfo{name='" + name + "', fileName='" + fileName
            + "', version='" + version + "', isMdf4=" + isMdf4 + "}";
    }
}
