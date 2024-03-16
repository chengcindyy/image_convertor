package com.example.app;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

    private static final LocalTime START_TIME_PER_DAY = LocalTime.parse("10:00");
    private static final LocalTime END_TIME_PER_DAY = LocalTime.parse("18:00");
    static String path = "C:\\testfiles\\output.csv";

    public static void main(String[] args) {



        // Read CSV file
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
                appointments.add(new Appointment(
                        values[0],
                        values[1],
                        values[2],
                        startDate,
                        startTime,
                        Integer.parseInt(values[5]),
                        endDate,
                        endTime));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        appointments.forEach(System.out::println);

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
                        // 如果加上当前预约的时间会超出工作日结束时间，可以在这里处理
                        continue; // 可能选择跳过此预约或其他逻辑
                    }
                    // 设置预约的开始时间和计算结束时间
                    appointment.setStartTime(currentTime);
                    LocalTime endTime = currentTime.plusMinutes(appointment.getDuration());

                    currentTime = endTime;

                    System.out.println("Doctor: " + doctorName + " Patient Number:" + appointment.patientNumber + " ,Subject: " + appointment.getSubject() +", Start Date: " + date + ", Start Time: " + appointment.getStartTime() + "Duration:" + appointment.getDuration() +", End: " + endTime);
                }
            });

            // Export to CSV
            String fileName = path + "appointments_" + doctorName.replaceAll("\\s+", "_") + ".csv";
            exportAppointmentsToCsv(appointmentsList, fileName);
        });
    }

    private static void exportAppointmentsToCsv(List<Appointment> appointmentsList, String fileName) {
        createCSVPrinter(appointmentsList, fileName);
        System.out.println("CSV file is created");
    }

    private static void createCSVPrinter(List<Appointment> appointments, String outputCSVPath) {
        // 检查文件是否已存在来决定是否需要打印头部
        boolean isNewFile = !Files.exists(Paths.get(outputCSVPath));
        CSVFormat format = isNewFile ?
                CSVFormat.DEFAULT.withHeader("Practitioner Name", "Patient Number", "Subject", "Start Date", "Start Time", "Duration", "End Date", "End Time") :
                CSVFormat.DEFAULT;

        try (FileWriter out = new FileWriter(outputCSVPath, true);
             CSVPrinter csvPrinter = new CSVPrinter(out, format)) {

            for (Appointment appointment : appointments) {
                csvPrinter.printRecord(
                        appointment.getPractitionerName(),
                        appointment.getPatientNumber(),
                        appointment.getSubject(),
                        appointment.getStartDate().toString(),
                        appointment.getStartTime() != null ? appointment.getStartTime().toString() : "",
                        appointment.getDuration(),
                        appointment.getEndDate().toString(),
                        appointment.getEndTime() != null ? appointment.getEndTime().toString() : ""
                );
            }
            csvPrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
