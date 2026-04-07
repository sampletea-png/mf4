package com.mdflib.model;

public class HeaderInfo {
    private final String author;
    private final String department;
    private final String project;
    private final String subject;
    private final String description;
    private final long startTime;

    public HeaderInfo(String author, String department, String project, String subject, String description, long startTime) {
        this.author = author;
        this.department = department;
        this.project = project;
        this.subject = subject;
        this.description = description;
        this.startTime = startTime;
    }

    public String getAuthor() { return author; }
    public String getDepartment() { return department; }
    public String getProject() { return project; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public long getStartTime() { return startTime; }

    @Override
    public String toString() {
        return "HeaderInfo{author='" + author + "', department='" + department + "', project='" + project + "', subject='" + subject + "', description='" + description + "', startTime=" + startTime + "}";
    }
}
