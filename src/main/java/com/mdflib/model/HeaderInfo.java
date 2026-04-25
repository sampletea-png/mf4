package com.mdflib.model;

/**
 * Represents metadata from the MDF file header.
 *
 * <p>The header contains administrative and descriptive metadata about
 * the measurement file, including author information, timestamps, and
 * organizational details.</p>
 *
 * <p>All fields are immutable after construction. This class is a simple
 * data container with no business logic.</p>
 *
 * @author mdflib-java contributors
 * @version 1.0.0
 * @since 1.0.0
 */
public class HeaderInfo {

    /** The name of the person who created the measurement. */
    private final String author;

    /** The department or organizational unit. */
    private final String department;

    /** The project name this measurement belongs to. */
    private final String project;

    /** The subject or topic of the measurement. */
    private final String subject;

    /** A free-text description of the measurement. */
    private final String description;

    /** The start time of the measurement in nanoseconds since Unix epoch. */
    private final long startTime;

    /**
     * Constructs a HeaderInfo with all metadata fields.
     *
     * @param author the author name
     * @param department the department name
     * @param project the project name
     * @param subject the subject
     * @param description the description
     * @param startTime start time in nanoseconds since 1970-01-01
     */
    public HeaderInfo(String author, String department, String project,
                      String subject, String description, long startTime) {
        this.author = author;
        this.department = department;
        this.project = project;
        this.subject = subject;
        this.description = description;
        this.startTime = startTime;
    }

    /** @return the author name */
    public String getAuthor() { return author; }

    /** @return the department name */
    public String getDepartment() { return department; }

    /** @return the project name */
    public String getProject() { return project; }

    /** @return the subject */
    public String getSubject() { return subject; }

    /** @return the description text */
    public String getDescription() { return description; }

    /** @return start time in nanoseconds since epoch */
    public long getStartTime() { return startTime; }

    @Override
    public String toString() {
        return "HeaderInfo{author='" + author + "', department='" + department
            + "', project='" + project + "', subject='" + subject
            + "', description='" + description + "', startTime=" + startTime + "}";
    }
}
