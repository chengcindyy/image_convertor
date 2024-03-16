package com.example.app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TimeFiller {
    public static void main(String[] args) {
        String path = "C:\\testfiles\\output.csv";
        List<Appointment> appointments = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/M/d");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                LocalDate startDate = LocalDate.parse(values[3], dateFormatter);
                LocalTime startTime = values[4].equals("[]") ? null : LocalTime.parse(values[4], timeFormatter);
                LocalDate endDate = LocalDate.parse(values[6], dateFormatter);
                LocalTime endTime = values[7].equals("[]") ? null : LocalTime.parse(values[7], timeFormatter);
                appointments.add(new Appointment(values[0], values[1], values[2], startDate, startTime, values[5], endDate, endTime));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Appointment> sortedAppointments = appointments.stream()
                .sorted(Comparator.comparing(Appointment::getStartDate))
                .toList();

        sortedAppointments.forEach(System.out::println);
    }


}
