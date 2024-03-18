package com.example.app;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TimeFiller {

    static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");
    private static final LocalTime START_TIME_PER_DAY = LocalTime.parse("10:00");
    private static final LocalTime END_TIME_PER_DAY = LocalTime.parse("21:30");

    // TODO: Connect path to GUI button
    static String path = "C:\\testfiles\\output.csv";

    public static void main(String[] args) {

        // Read CSV file
        List<Appointment> appointments = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", -1); // Use -1 to include trailing empty strings
                LocalDate startDate = LocalDate.parse(values[3], dateFormatter);
                LocalTime startTime = !values[4].isEmpty() ? LocalTime.parse(values[4], timeFormatter) : null;

                // make sure duration exists
                int duration = values.length > 5 && !values[5].isEmpty() ? Integer.parseInt(values[5]) : 0;

                LocalDate endDate = values.length > 6 && !values[6].isEmpty() ? LocalDate.parse(values[6], dateFormatter) : startDate;
                LocalTime endTime = (values.length > 7 && !values[7].isEmpty()) ? LocalTime.parse(values[7], timeFormatter) : null;

                appointments.add(new Appointment(
                        values[0], // Practitioner Name
                        values[1], // Patient Number
                        values[2], // Subject
                        startDate,
                        startTime,
                        duration,
                        endDate,
                        endTime));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Split date by dr. name
        Map<String, List<Appointment>> appointmentsByDoctor = appointments.stream()
                .collect(Collectors.groupingBy(Appointment::getPractitionerName));

        appointmentsByDoctor.forEach((doctorName, appointmentsList) -> {
            Map<LocalDate, List<Appointment>> appointmentsByDate = appointmentsList.stream()
                    .collect(Collectors.groupingBy(Appointment::getStartDate));

            appointmentsByDate.forEach((date, dailyAppointments) -> {
                LocalTime currentTime = START_TIME_PER_DAY;
                for (Appointment appointment : dailyAppointments) {
                    if (currentTime.plusMinutes(appointment.getDuration()).isAfter(END_TIME_PER_DAY)) {
                        // TODO: If excess available time, process here
                        continue;
                    }
                    // Set appointment start time and end time
                    appointment.setStartTime(currentTime);
                    LocalTime endTime = currentTime.plusMinutes(appointment.getDuration());

                    currentTime = endTime;

                    System.out.println("Processing " + appointment.getSubject() + " 's data ...");
//                    System.out.println("Doctor: " + doctorName + " ,Patient Number: " + appointment.patientNumber + " ,Subject: " + appointment.getSubject() +" ,Start Date: " + date + " ,Start Time: " + appointment.getStartTime() + " ,Duration:" + appointment.getDuration() +", End Time: " + endTime);
                }
            });
            System.out.println("Completed!");

            // Export to CSV
            String fileName = path + "appointments_" + doctorName.replaceAll("\\s+", "_") + ".csv";
            exportAppointmentsToCsv(appointmentsList, fileName);
            // TODO: All data combine to one CSV
//            exportAllAppointmentsToCsv(appointmentsByDoctor, "C:\\testfiles\\allAppointments.csv");
        });
    }

    private static void exportAppointmentsToCsv(List<Appointment> appointmentsList, String fileName) {
        createCSVPrinter(appointmentsList, fileName);
        System.out.println("CSV file is created");
    }

    private static void createCSVPrinter(List<Appointment> appointments, String outputCSVPath) {

        boolean isNewFile = !Files.exists(Paths.get(outputCSVPath));
        CSVFormat format = isNewFile ?
                CSVFormat.DEFAULT.withHeader("Practitioner Name", "Patient Number", "Subject", "Start Date", "Start Time", "Duration", "End Date", "End Time") :
                CSVFormat.DEFAULT;

        try (FileWriter out = new FileWriter(outputCSVPath, StandardCharsets.UTF_8, !isNewFile);
             CSVPrinter csvPrinter = new CSVPrinter(out, format)) {

            for (Appointment appointment : appointments) {
                LocalTime endTime = appointment.getStartTime() != null ? appointment.getStartTime().plusMinutes(appointment.getDuration()) : null;

                csvPrinter.printRecord(
                        appointment.getPractitionerName().trim(),
                        appointment.getPatientNumber().trim(),
                        appointment.getSubject().trim(),
                        appointment.getStartDate().format(dateFormatter),
                        appointment.getStartTime() != null ? appointment.getStartTime().format(timeFormatter) : "",
                        appointment.getDuration(),
                        appointment.getEndDate().format(dateFormatter),
                        endTime != null ? endTime.format(timeFormatter) : ""
                );
            }
            csvPrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void exportAllAppointmentsToCsv(Map<String, List<Appointment>> appointmentsByDoctor, String outputCSVPath) {
        boolean isNewFile = !Files.exists(Paths.get(outputCSVPath));
        CSVFormat format = CSVFormat.DEFAULT.withHeader("Practitioner Name", "Patient Number", "Subject", "Start Date", "Start Time", "Duration", "End Date", "End Time");

        try (FileWriter out = new FileWriter(outputCSVPath, !isNewFile);
             CSVPrinter csvPrinter = new CSVPrinter(out, format)) {

            for (Map.Entry<String, List<Appointment>> doctorAppointments : appointmentsByDoctor.entrySet()) {
                for (Appointment appointment : doctorAppointments.getValue()) {
                    LocalTime endTime = appointment.getStartTime() != null ? appointment.getStartTime().plusMinutes(appointment.getDuration()) : null;

                    csvPrinter.printRecord(
                            appointment.getPractitionerName(),
                            appointment.getPatientNumber(),
                            appointment.getSubject(),
                            appointment.getStartDate().toString(),
                            appointment.getStartTime() != null ? appointment.getStartTime().toString() : "",
                            appointment.getDuration(),
                            appointment.getEndDate().toString(),
                            endTime != null ? endTime.toString() : ""
                    );
                }
            }
            csvPrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
