package com.mdflib.model;

public class FileInfo {
    private final String name;
    private final String fileName;
    private final String version;
    private final int mainVersion;
    private final int minorVersion;
    private final boolean isMdf4;

    public FileInfo(String name, String fileName, String version, int mainVersion, int minorVersion, boolean isMdf4) {
        this.name = name;
        this.fileName = fileName;
        this.version = version;
        this.mainVersion = mainVersion;
        this.minorVersion = minorVersion;
        this.isMdf4 = isMdf4;
    }

    public String getName() { return name; }
    public String getFileName() { return fileName; }
    public String getVersion() { return version; }
    public int getMainVersion() { return mainVersion; }
    public int getMinorVersion() { return minorVersion; }
    public boolean isMdf4() { return isMdf4; }

    @Override
    public String toString() {
        return "FileInfo{name='" + name + "', fileName='" + fileName + "', version='" + version + "', isMdf4=" + isMdf4 + "}";
    }
}
