package com.example.app;

import java.time.LocalDate;
import java.time.LocalTime;

public class Appointment {
    String practitionerName;
    String patientNumber;
    String subject;
    LocalDate startDate;
    LocalTime startTime;
    LocalDate endDate;
    LocalTime endTime;
    int duration;
    private long countForDate;

    public Appointment(String practitionerName, String patientNumber, String subject, LocalDate startDate, LocalTime startTime, int duration, LocalDate endDate, LocalTime endTime ) {
        this.practitionerName = practitionerName;
        this.patientNumber = patientNumber;
        this.subject = subject;
        this.startDate = startDate;
        this.startTime = startTime;
        this.duration = duration;
        this.endDate = endDate;
        this.endTime = endTime;
    }
    public String getPractitionerName() {
        return practitionerName;
    }

    public void setPractitionerName(String practitionerName) {
        this.practitionerName = practitionerName;
    }

    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "Appointment{" +
                "practitionerName='" + practitionerName + '\'' +
                ", patientNumber='" + patientNumber + '\'' +
                ", subject='" + subject + '\'' +
                ", startDate=" + startDate +
                ", startTime=" + startTime +
                ", endDate=" + endDate +
                ", endTime=" + endTime +
                ", duration='" + duration + '\'' +
                ", countForDate='" + countForDate + '\'' +
                '}';
    }
}
