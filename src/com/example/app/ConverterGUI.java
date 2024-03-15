package com.example.app;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.azure.ai.vision.imageanalysis.ImageAnalysisClient;
import com.azure.ai.vision.imageanalysis.ImageAnalysisClientBuilder;
import com.azure.ai.vision.imageanalysis.models.DetectedTextLine;
import com.azure.ai.vision.imageanalysis.models.ImageAnalysisResult;
import com.azure.ai.vision.imageanalysis.models.VisualFeatures;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.formdev.flatlaf.FlatLightLaf;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class ConverterGUI extends JFrame {

    private static final PathPreference pathPreference = new PathPreference();
    private final JTextField inputTextField;
    private final JTextField outputTextField;
    private final Dotenv dotenv = Dotenv.load();
    private final String SUBSCRIPTION_KEY_ONE = dotenv.get("AZURE_TEXT_ANALYTICS_SUBSCRIPTION_KEY");
    private final String ENDPOINT = dotenv.get("AZURE_TEXT_ANALYTICS_ENDPOINT");

    private ConverterGUI(String lastUsedFolderPath, String lastUsedFilePath) {
        // set title and size
        setTitle("PDF to CSV Converter");
        setSize(400, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        // Create components
        inputTextField = new JTextField(15);
        outputTextField = new JTextField(15);
        JButton convertButton = new JButton("Convert to CSV");
        JButton inputSelectButton = new JButton("Select Input Folder");
        JButton outputSelectButton = new JButton("Select a Output file");

        // Add components to window
        add(inputSelectButton);
        add(inputTextField);
        add(outputSelectButton);
        add(outputTextField);
        add(convertButton);

        // Use saved path and file name
        if (!lastUsedFolderPath.isEmpty()) {
            inputTextField.setText(lastUsedFolderPath);
        }
        if (!lastUsedFilePath.isEmpty()) {
            outputTextField.setText(lastUsedFilePath);
        }

        // Set buttons behavior
        inputSelectButton.addActionListener(e ->
                selectPathFromChooser(true, pathPreference.getLastUsedFolder(), inputTextField, null));

        outputSelectButton.addActionListener(e ->
                selectPathFromChooser(false, pathPreference.getLastUsedFilePath(), outputTextField, "csv"));

        // call converter
        convertButton.addActionListener(e -> {
            String inputPath = inputTextField.getText().trim();
            String outputPath = outputTextField.getText().trim();

            try {
                processDirectory(inputPath, outputPath);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public static void main(String[] args) {

        try {
            // TODO: YOU CAN CHANG LOOK AND FEEL HERE
            // flatlaf: FlatLightLaf(), FlatDarkLaf()、FlatIntelliJLaf()
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        // Set preferences
        String lastUsedFolderPath = pathPreference.getLastUsedFolder();
        String lastUsedFilePath = pathPreference.getLastUsedFilePath();

        SwingUtilities.invokeLater(() ->
                new ConverterGUI(lastUsedFolderPath, lastUsedFilePath).setVisible(true));
    }

    private JFileChooser createFileChooser(boolean selectDirectory, String lastUsedPath, String fileTypeFilter) {
        JFileChooser chooser = new JFileChooser(lastUsedPath.isEmpty() ? new java.io.File(".") : new java.io.File(lastUsedPath));

        if (selectDirectory) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            FileNameExtensionFilter filter = new FileNameExtensionFilter(fileTypeFilter + " Files", fileTypeFilter);
            chooser.setFileFilter(filter);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        }

        chooser.setAcceptAllFileFilterUsed(false);
        return chooser;
    }

    private void selectPathFromChooser(boolean selectDirectory, String lastUsedPath, JTextField targetTextField, String fileTypeFilter) {
        JFileChooser chooser = createFileChooser(selectDirectory, lastUsedPath, fileTypeFilter);
        chooser.setDialogTitle(selectDirectory ? "Select Folder" : "Select " + fileTypeFilter + " File");

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            targetTextField.setText(selectedFile.getAbsolutePath());
            // Save the newly selected path
            if (selectDirectory) {
                pathPreference.saveLastUsedFolder(selectedFile.getAbsolutePath());
            } else {
                pathPreference.saveLastUsedFilePath(selectedFile.getAbsolutePath());
            }
        } else {
            if (!lastUsedPath.isEmpty()) {
                targetTextField.setText(lastUsedPath);
            } else {
                System.out.println("No selection was made.");
            }
        }
    }

    private void processDirectory(String dirPath, String outputPath) throws IOException {
        File dir = new File(dirPath);
        boolean hasProcessedFiles = false; // 標記是否處理了任何文件
        if (dir.exists() && dir.isDirectory()) {
            File[] fileList = dir.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    if (file.isFile()) {
                        String filePath = file.getAbsolutePath();
                        String extension = "";

                        int i = filePath.lastIndexOf('.');
                        if (i > 0) {
                            extension = filePath.substring(i+1).toLowerCase();
                        }

                        switch (extension) {
                            case "pdf":
                                System.out.println("Found PDF File, ConvertPDFToCSV Called");
                                convertPDFToCSV(filePath, outputPath);
                                hasProcessedFiles = true; // 標記已處理文件
                                break;
                            case "jpeg":
                            case "jpg":
                            case "png":
                            case "gif":
                            case "bmp":
                                System.out.println("Found Image File, ReadImageWithOCR Called");
                                readImageWithOCR(filePath, outputPath);
                                hasProcessedFiles = true;
                                break;
                            default:
                                System.out.println("Unsupported file type: " + extension);
                                break;
                        }
                    }
                }
                if (hasProcessedFiles) {
                    JOptionPane.showMessageDialog(null, "CSV file was created or updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "No valid files found for conversion.", "Info", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "No files found in the directory.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Invalid directory path.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void convertPDFToCSV(String inputFolderPath, String outputCSVPath) {
        List<String> inputPDFs;
        try (Stream<Path> walk = Files.walk(Paths.get(inputFolderPath))) {
            inputPDFs = walk
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(path -> path.endsWith(".pdf"))
                    .toList();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error reading the folder: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (String inputPDF : inputPDFs) {
            try (PDDocument document = Loader.loadPDF(new File(inputPDF))) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                List<String> lines = Arrays.asList(text.split("\\r?\\n"));

                // Now use processTextBasedOnSource which internally calls createCSVPrinter
                processTextBasedOnSource("PDF", lines, outputCSVPath);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "An error occurred while processing PDF: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void readImageWithOCR(String imagePath, String outputPath) throws IOException {
        // Initialize Client side of Azure Text Analytics
        ImageAnalysisClient client = new ImageAnalysisClientBuilder()
                .credential(new AzureKeyCredential(SUBSCRIPTION_KEY_ONE))
                .endpoint(ENDPOINT)
                .buildClient();

        // Process the single file with OCR
        String textResult = recognizeText(client, Path.of(imagePath));
        List<String> lines = Arrays.asList(textResult.split("\\r?\\n"));

        // Since this is OCR, we use OCRTextProcessor directly
        TextProcessor processor = new OCRTextProcessor();
        createCSVPrinter(processor, lines, outputPath);
        processTextBasedOnSource("OCR", lines, outputPath);
    }

    private static String recognizeText(ImageAnalysisClient client, Path imagePath) {
        StringBuilder resultText = new StringBuilder();
        try {
            ImageAnalysisResult result = client.analyze(
                    BinaryData.fromFile(imagePath), // imageData: Image file loaded into memory as BinaryData
                    Collections.singletonList(VisualFeatures.READ), // visualFeatures
                    null); // options: There are no options for READ visual feature

            for (DetectedTextLine line : result.getRead().getBlocks().get(0).getLines()) {
                resultText.append(line.getText()).append("\n");
            }
        } catch (Exception e) {
            System.err.println("Error occurred while trying to recognize text from the image: " + e.getMessage());
            // Handle the error or propagate it as needed
            resultText.append("Error occurred while trying to recognize text from the image: ").append(e.getMessage());
        }
        return resultText.toString();
    }

    private static class PDFTextProcessor implements TextProcessor {
        DateTimeFormatter originalFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
        DateTimeFormatter targetFormat = DateTimeFormatter.ofPattern("yyyy/M/d");
        @Override
        public void processText(List<String> lines, CSVPrinter csvPrinter) throws IOException {
            String drTcm = "";
            String patientName = "";
            List<String> visitDates = new ArrayList<>();
            List<String> fees = new ArrayList<>();
            boolean isReadingFees = false;

            for (String line : lines) {
                if (line.startsWith("DR. TCM.:") || line.startsWith("DR. TCM:")) {
                    drTcm = extractValueAfterColon(line);
                } else if (line.startsWith("Patient Name:")) {
                    patientName = extractValueAfterColon(line);
                } else if (line.matches("\\b\\w{3} \\s\\d{1,2}, \\d{4}")) {
                    visitDates.add(line.trim());
                } else if (line.startsWith("Subtotal")) {
                    isReadingFees = true;
                } else if (isReadingFees && line.matches("\\$\\d+\\.\\d{2}")) {
                    fees.add(line.trim());
                }
            }

            for (int i = 0; i < visitDates.size(); i++) {
                try {
                    LocalDate date = LocalDate.parse(visitDates.get(i), originalFormat);
                    String formattedDate = date.format(targetFormat);
                    String fee = (i < fees.size()) ? fees.get(i) : "";
                    csvPrinter.printRecord(drTcm, patientName, formattedDate, fee);
                } catch (DateTimeParseException e) {
                    System.err.println("Unable to parse date from line: " + visitDates.get(i));
                }
            }
        }
    }

    private static String extractValueAfterColon(String line) {
        return line.substring(line.indexOf(":") + 1).trim();
    }

    private static class OCRTextProcessor implements TextProcessor {
        @Override
        public void processText(List<String> lines, CSVPrinter csvPrinter) throws IOException {
            String patientNumber = lines.get(0);
            String dr = "";
            String patientName = "";
            List<String> visitDates = new ArrayList<>();
            List<String> fees = new ArrayList<>();
            List<String> startTime = new ArrayList<>();
            List<String> endTime = new ArrayList<>();

            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("Practitioner Info" )) {
                    String[] parts = lines.get(i+2).split(":");
                    if (parts.length > 1) {
                        dr = parts[1].trim();
                    }
                } else if (lines.get(i).startsWith("Patient Name:")) {
                    patientName = lines.get(i).substring("Patient Name:".length()).trim();
                } else if (lines.get(i).matches("^\\w{3} \\d{1,2}, \\d{4}$")) {
                    visitDates.add(lines.get(i).trim());
                } else if (lines.get(i).matches("^\\$\\d+\\.\\d{2}$")) {
                    fees.add(lines.get(i).trim());
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
    }

    private static List<String> dateConverter(List<String> visitDates) {
        List<String> formattedDates = new ArrayList<>();
        DateTimeFormatter originalFormat = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMM d, yyyy")
                .toFormatter(Locale.ENGLISH);
        DateTimeFormatter targetFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        for (String visitDate : visitDates) {
            try {
                LocalDate date = LocalDate.parse(visitDate.trim(), originalFormat);
                String formattedDate = date.format(targetFormat);
                formattedDates.add(formattedDate);
            } catch (DateTimeParseException e) {
                System.err.println("Error: " + e.getMessage());
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
                case "$50.00":
                    convertedFee = "30";
                    break;
                case "$70.00":
                case "$89.00":
                    convertedFee = "50";
                    break;
                case "$75.00":
                    convertedFee = "60";
                    break;
                case "$105.00":
                    convertedFee = "75";
                    break;
                case "$133.00":
                    convertedFee = "75";
                case "$178.00":
                    convertedFee = "100";
                case "$140.00":
                    convertedFee = "100";
                default:
                    convertedFee = "Unknown";
                    break;
            }
            convertedFees.add(convertedFee);
        }
        return convertedFees;
    }

    private void processTextBasedOnSource(String sourceType, List<String> lines, String outputCSVPath) throws IOException {
        TextProcessor processor;
        if ("PDF".equals(sourceType)) {
            processor = new PDFTextProcessor();
        } else { // If not PDF, default to OCR
            processor = new OCRTextProcessor();
        }
        createCSVPrinter(processor, lines, outputCSVPath);
    }

    private void createCSVPrinter(TextProcessor processor, List<String> lines, String outputCSVPath) {
        boolean isNewFile = !Files.exists(Paths.get(outputCSVPath));
        try (FileWriter out = new FileWriter(outputCSVPath, true);
             CSVPrinter csvPrinter = isNewFile ?
                     new CSVPrinter(out, CSVFormat.Builder.create().setHeader("Practitioner Name", "Patient Number", "Subject", "Start Date", "Start Time", "Duration", "End Date", "End Time").build()) :
                     new CSVPrinter(out, CSVFormat.DEFAULT)) {
            processor.processText(lines, csvPrinter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
