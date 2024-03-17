package com.example.app;

import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class TextProcessor {
    public abstract void processText(List<String> lines, CSVPrinter csvPrinter) throws IOException;
    public static final int PDFTEXTPROCESSOR = 0;
    public static final int OCRTEXTPROCESSOR = 1;
    protected void extractInfo(List<String> lines, CSVPrinter csvPrinter, int processorType) throws IOException {

        //System.out.println(lines);
        String patientNumber = "";
        if (processorType == OCRTEXTPROCESSOR) {
            patientNumber = lines.get(0);
        } else if (processorType == PDFTEXTPROCESSOR) {}
        try {
            int parsedPatientNumber = Integer.parseInt(patientNumber);
            // Parsing successful, assign the parsed value to patientNumber
            patientNumber = String.valueOf(parsedPatientNumber);
        } catch (NumberFormatException e) {
            // Parsing failed, patientNumber is not an integer
            patientNumber = "Invalid";
            // Print error message to the runtime terminal
            System.err.println("Error: patientNumber is not an integer.");
        }

        String dr = "";
        String patientName = "";
        List<String> visitDates = new ArrayList<>();
        List<String> fees = new ArrayList<>();
        String startTime = "";
        String endTime = "";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("Practitioner Info" )) {
                if (processorType == OCRTEXTPROCESSOR) {
                    String[] parts = lines.get(i+2).split(":");
                    if (parts.length > 1) {
                        dr = parts[1].trim();
                    }
                } else if (processorType == PDFTEXTPROCESSOR) {

                    String[] parts = lines.get(i+1).split(":");
                    if (parts.length > 1) {
                        dr = parts[1].trim();
                    }
                }
            } else if (line.startsWith("Patient Name:")) {
                patientName = line.substring("Patient Name:".length()).trim();
            } else if (line.matches("^\\s*\\w{3}\\s*\\d{1,2}\\s*,?\\s*\\d{4}\\s*$")) {

                visitDates.add(line.trim());
            } else if (line.matches("^\\$\\d+\\.\\d{2}$")) {
                fees.add(line.trim());
            }
        }

        List<String> formattedDates = dateConverter(visitDates);
        List<String> convertedFees = feeConverter(fees);

        for (int i = 0; i < formattedDates.size(); i++) {
            String dateStr = formattedDates.get(i);
            String fee = (i < convertedFees.size()) ? convertedFees.get(i) : "";
            csvPrinter.printRecord(dr, patientNumber, patientName, dateStr, startTime, fee, dateStr, endTime);
        }
    }

    private static List<String> dateConverter(List<String> visitDates) {
        List<String> formattedDates = new ArrayList<>();

        DateTimeFormatter originalFormat = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMM")
                .optionalStart()
                .appendPattern(" ")
                .optionalEnd()
                .appendPattern("d")
                .appendLiteral(',')
                .optionalStart()
                .appendPattern(" ")
                .optionalEnd()
                .appendPattern("yyyy")
                .toFormatter(Locale.ENGLISH);

        DateTimeFormatter targetFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        for (String visitDate : visitDates) {
            try {
                LocalDate date = LocalDate.parse(visitDate.trim(), originalFormat);
                String formattedDate = date.format(targetFormat);
                formattedDates.add(formattedDate);
            } catch (DateTimeParseException e) {
                System.err.println("Error parsing date: " + e.getMessage());
                formattedDates.add("Invalid date");
            }
        }
        return formattedDates;
    }


    private static List<String> feeConverter(List<String> fees) {
        List<String> convertedFees = new ArrayList<>();
        for (String fee : fees) {
            String convertedFee;
            switch (fee) {
                case "$30.00":
                    convertedFee = "15";
                    break;
                case "$40.00":
                    convertedFee = "20";
                    break;
                case "$50.00":
                    convertedFee = "30";
                    break;
                case "$70.00":
                case "$65.00":
                case "$84.00":
                case "$89.00":
                case "$89.25":
                    convertedFee = "50";
                    break;
                case "$90.00":
                    convertedFee = "90";
                    break;
                case "$75.00":
                    convertedFee = "60";
                    break;
                case "$96.00":
                case "$105.00":
                case "$126.00":
                case "$133.00":
                    convertedFee = "75";
                    break;
                case "$178.00":
                case "$140.00":
                    convertedFee = "100";
                    break;
                default:
                    convertedFee = "Unknown";
                    break;
            }

            convertedFees.add(convertedFee);
        }
        return convertedFees;

    }

}
