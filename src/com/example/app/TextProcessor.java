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

        System.out.println(lines);
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
            if (patientNumber.equals("UN")) {
                System.err.println("Patient Do Not Have Patient Number Yet");
            } else {
                patientNumber = "Invalid";
                // Print error message to the runtime terminal
                System.err.println("Error: patientNumber is not an integer.");
            }

        }

        String dr = "";
        String patientName = "";
        List<String> visitDates = new ArrayList<>();
        List<String> fees = new ArrayList<>();
        String startTime = "";
        String endTime = "";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.toLowerCase().startsWith("practitioner info")) {
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
            } else if (line.startsWith("Patient Name")) {
                patientName = line.split(":")[1].trim();
            } else if (line.matches("^\\s*\\w{3}[.]?\\s*\\d{1,2}\\s*[,.]?\\s*\\d{4}\\s*$")) { //MMM(.) d(.,) yyyy
                visitDates.add(line.trim());
            } else if (line.matches("^\\s*\\w+\\s+(0?[1-9]|[12]\\d|3[01])\\s*[,.]?\\s*$")) { //MMMM d,
                                                                                                // yyyy (the other line)
                String date = line.trim();
                System.out.println("date: " + date);
                for (int j = i + 1; j < lines.size(); j++) {
                    if (lines.get(j).matches("^\\s*\\d{4}\\s*$")) {
                        String year = lines.get(j).trim();
                        System.out.println("year: " + year);
                        visitDates.add(date + " " + year);
                    }
                }
            }  else if (line.matches("^\\s*\\w+\\s+(0?[1-9]|[12]\\d|3[01])\\s*[,.]?\\s*\\d{4}\\s*$")) { //MMMM d, yyyy (the same line)
                visitDates.add(line.trim());
            } else if (line.matches("^\\$?\\d+\\.\\d{2}$")) {
                fees.add(line.trim());
            }
        }

        List<String> formattedDates = dateConverter(visitDates);
        List<String> convertedFees = feeConverter(fees);


        for (int i = 0; i < formattedDates.size(); i++) {
            String dateStr = formattedDates.get(i);
            //TODO: Error: patientNumber is not an integer.
            //Error occurred while trying to recognize text from the image: Index 1 out of bounds for length 1
            String duration = convertedFees.get(i*2+1);
            csvPrinter.printRecord(dr, patientNumber, patientName, dateStr, startTime, duration, dateStr, endTime);
        }
    }

    private static List<String> dateConverter(List<String> visitDates) {
        List<String> formattedDates = new ArrayList<>();

        DateTimeFormatter originalFormat = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("[MMMM][MMM]")
                .optionalStart()
                .appendPattern(".")
                .optionalEnd()
                .optionalStart()
                .appendPattern(" ")
                .optionalEnd()
                .appendPattern("d")
                .optionalStart()
                .appendPattern(" ")
                .optionalEnd()
                .optionalStart()
                .appendLiteral(",")
                .optionalEnd()
                .optionalStart()
                .appendLiteral(".")
                .optionalEnd()
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
            fee = fee.replace("$","");
            System.out.println(fees.size() + " " + fee);
            String convertedFee;
            switch (fee) {
                case "25.00":
                    convertedFee = "10";
                    break;
                case "30.00":
                    convertedFee = "15";
                    break;
                case "40.00":
                    convertedFee = "20";
                    break;
                case "44.00":
                    convertedFee = "25";
                    break;
                case "50.00":
                    convertedFee = "30";
                    break;
                case "60.00":
                case "65.00":
                case "70.00":
                case "84.00":
                case "89.00":
                case "89.25":
                    convertedFee = "50";
                    break;
                case "75.00"://
                    convertedFee = "60";
                    break;
                case "90.00"://
                    convertedFee = "90";
                    break;
                case "96.00":
                case "105.00":
                case "112.50":
                case "114.00":
                case "126.00":
                case "133.00":
                case "134.00":
                    convertedFee = "75";
                    break;
                case "140.00":
                case "178.00":
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
