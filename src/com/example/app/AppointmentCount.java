package com.example.app;

import java.time.LocalDate;

public class AppointmentCount {
    private LocalDate startDate;
    private long count;

    // Constructor
    public AppointmentCount(LocalDate startDate, long count) {
        this.startDate = startDate;
        this.count = count;
    }

    // Getter for startDate
    public LocalDate getStartDate() {
        return startDate;
    }

    // Getter for count
    public long getCount() {
        return count;
    }
}
