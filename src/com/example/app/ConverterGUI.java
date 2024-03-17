package com.example.app;
import javax.imageio.ImageIO;
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
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class ConverterGUI extends JFrame {

    static PathPreference pathPreference = new PathPreference();
    private final JTextField inputTextField;
    private final JTextField outputTextField;

    private JLabel progressLabel;
    private final Dotenv dotenv = Dotenv.load();
    private final String SUBSCRIPTION_KEY_ONE = dotenv.get("AZURE_TEXT_ANALYTICS_SUBSCRIPTION_KEY");
    private final String ENDPOINT = dotenv.get("AZURE_TEXT_ANALYTICS_ENDPOINT");

    private ImageAnalysisClient client = new ImageAnalysisClientBuilder()
            .credential(new AzureKeyCredential(SUBSCRIPTION_KEY_ONE))
            .endpoint(ENDPOINT)
            .buildClient();


    private ConverterGUI(String lastUsedFolderPath, String lastUsedFilePath) {

        // set title and size
        setTitle("PDF to CSV Converter");
        setSize(400, 200); // Increased height to accommodate the new text area
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        // Create components
        inputTextField = new JTextField(15);
        outputTextField = new JTextField(15);
        JButton convertButton = new JButton("Convert to CSV");
        JButton inputSelectButton = new JButton("Select Input Folder");
        JButton outputSelectButton = new JButton("Select a Output file");
        progressLabel = new JLabel("Click to start");

        // Add components to window
        add(inputSelectButton);
        add(inputTextField);
        add(outputSelectButton);
        add(outputTextField);
        add(convertButton);
        add(progressLabel);

        // Use saved path and file name
        if (!lastUsedFolderPath.isEmpty()) {
            inputTextField.setText(lastUsedFolderPath);
        }
        if (!lastUsedFilePath.isEmpty()) {
            outputTextField.setText(lastUsedFilePath);
        }

        // Set buttons behavior
        inputSelectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectPathFromChooser(true, pathPreference.getLastUsedFolder(), inputTextField, null);
            }
        });

        outputSelectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectPathFromChooser(false, pathPreference.getLastUsedFilePath(), outputTextField, "csv");
            }
        });

        // call converter
        convertButton.addActionListener(e -> {
            String inputPath = inputTextField.getText().trim();
            String outputPath = outputTextField.getText().trim();

            // Disable the convert button while processing
            convertButton.setEnabled(false);

            try {
                processDirectory(inputPath, outputPath);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                // Re-enable the convert button after processing
                convertButton.setEnabled(true);
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

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ConverterGUI(lastUsedFolderPath, lastUsedFilePath).setVisible(true);
            }
        });

    }

    private JFileChooser createFileChooser(boolean selectDirectory, String lastUsedPath, String fileTypeFilter) {
        JFileChooser chooser = new JFileChooser(lastUsedPath.isEmpty() ? new File(".") : new File(lastUsedPath));

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

    public void selectPathFromChooser(boolean selectDirectory, String lastUsedPath, JTextField targetTextField, String fileTypeFilter) {
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

    private void processDirectory(String dirPath, String outputPath) {
        File dir = new File(dirPath);

        boolean hasProcessedFiles = false;
        if (dir.exists() && dir.isDirectory()) {
            File[] fileList = dir.listFiles();
            if (fileList != null) {

                int fileListSize = fileList.length;
                System.out.println("Reading "+ fileListSize + " file(s)...");

                for (int i = 0; i < fileListSize; i++) {

                    File file = fileList[i];
                    int remainingFiles = fileListSize - i;

                    if (file.isFile()) {
                        String filePath = file.getAbsolutePath();
                        String extension = "";

                        int j = filePath.lastIndexOf('.');
                        if (j > 0) {
                            extension = filePath.substring(j+1).toLowerCase();
                        }

                        System.out.println("Processing " + file.getName() + "... "
                                + remainingFiles + " file(s) remained.");
                        // TODO:Update the status label with processing progress
//                        SwingUtilities.invokeLater(() -> {
//                            progressLabel.setText("Processing " + file.getName() + "... " + remainingFiles + " file(s) remained.");
//                        });

                        switch (extension) {
                            case "pdf":
                                //check content (img or txt)
                                //System.out.println("Found pdf, PDFContentAnalyzer Called");
                                PDFContentAnalyzer(file, outputPath);
                                hasProcessedFiles = true;

                                break;
                            case "jpeg":
                            case "jpg":
                                System.out.println("Found jpg");
                                break;
                            case "png":
                                System.out.println("Found png");
                                break;
                            case "gif":
                                break;
                            case "bmp":
                                //System.out.println("Found Image File, ReadImageWithOCR Called");
                                readImageWithOCR(filePath, outputPath);
                                hasProcessedFiles = true;

                                break;
                            default:
                                System.err.println("Unsupported file type: " + extension);
                                break;
                        }

                    }
                }

                if (hasProcessedFiles) {
                    System.out.println("Completed!");
                    progressLabel.setText("Completed!");
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

    public void PDFContentAnalyzer(File file, String outputCSVPath) {
        try {
            // Load the PDF file
            //File file = new File("example.pdf");
            PDDocument document = Loader.loadPDF(file);

            // Create PDFTextStripper object
            PDFTextStripper pdfStripper = new PDFTextStripper();

            // Extract text from the PDF
            String text = pdfStripper.getText(document);

            // Analyze the extracted text
            if (text.trim().isEmpty()) {
                //System.out.println("The PDF content is primarily image-based.");
                //read multi-page image-style PDF
                //System.out.println("Found Image PDF File, ConvertImagePDFToCSV Called");
                convertImagePDFToCSV(document, outputCSVPath);
            } else {
                //System.out.println("The PDF content is primarily text-based.");
                //System.out.println("Found Text PDF File, ConvertTextPDFToCSV Called");

                //read single-page text-style PDF
                convertTextPDFToCSV(document, outputCSVPath);
            }

            // Close the document
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void convertImagePDFToCSV(PDDocument document, String outputCSVPath) {
        try {
            // Create PDFRenderer object
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            // Iterate through each page
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                // Render the page as an image
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 300, ImageType.RGB);

                //try to read the text
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    ImageIO.write(image, "png", byteArrayOutputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] imageData = byteArrayOutputStream.toByteArray();

                StringBuilder resultText = new StringBuilder();
                try {
                    ImageAnalysisResult result = client.analyze(
                            BinaryData.fromBytes(imageData), // imageData: Image data in byte array
                            Collections.singletonList(VisualFeatures.READ), // visualFeatures
                            null);

                    for (DetectedTextLine line : result.getRead().getBlocks().get(0).getLines()) {
                        resultText.append(line.getText()).append("\n");
                    }
                    List<String> lines = Arrays.asList(resultText.toString().split("\\r?\\n"));
                    // Since this is OCR, we use OCRTextProcessor directly
                    processTextBasedOnSource("imgPDF", lines, outputCSVPath);
                } catch (Exception e) {
                    System.err.println("Error occurred while trying to recognize text from the image: " + e.getMessage());
                    // Handle the error or propagate it as needed
                    resultText.append("Error occurred while trying to recognize text from the image: ").append(e.getMessage());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void convertTextPDFToCSV(PDDocument document, String outputCSVPath) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            List<String> lines = Arrays.asList(text.split("\\r?\\n"));
            // Now use processTextBasedOnSource which internally calls createCSVPrinter
            processTextBasedOnSource("textPDF", lines, outputCSVPath);
            //System.out.println(lines);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "An error occurred while processing PDF: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void readImageWithOCR(String imagePath, String outputPath) throws IOException {

        // Process the single file with OCR
        String textResult = recognizeTextInImage(Path.of(imagePath));
        List<String> lines = Arrays.asList(textResult.split("\\r?\\n"));

        // Since this is OCR, we use OCRTextProcessor directly
        processTextBasedOnSource("OCR", lines, outputPath);
    }

    private String recognizeTextInImage(Path imagePath) {
        StringBuilder resultText = new StringBuilder();
        try {
            ImageAnalysisResult result = client.analyze(
                    BinaryData.fromFile(imagePath), // imageData: Image file loaded into memory as BinaryData
                    Collections.singletonList(VisualFeatures.READ), // visualFeatures
                    null); // options: There are no options for READ visual feature

            System.out.println("Image analysis results:");
            System.out.println("Read:");
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


    private static class PDFTextProcessor extends TextProcessor {
        @Override
        public void processText(List<String> lines, CSVPrinter csvPrinter) throws IOException {
            extractInfo(lines, csvPrinter, TextProcessor.PDFTEXTPROCESSOR);
        }
    }

    private static class OCRTextProcessor extends TextProcessor {
        @Override
        public void processText(List<String> lines, CSVPrinter csvPrinter) throws IOException {
            extractInfo(lines, csvPrinter, TextProcessor.OCRTEXTPROCESSOR);
        }
    }

    private void processTextBasedOnSource(String sourceType, List<String> lines, String outputCSVPath) throws IOException {
        TextProcessor processor;
        if ("textPDF".equals(sourceType)) {
            processor = new PDFTextProcessor();
        } else { // If not PDF, default to OCR
            processor = new OCRTextProcessor();
        }

        for (int i = 0; i < visitDates.size(); i++) {
            String dateStr = visitDates.get(i).replace(",", ", ");
            try {
                LocalDate date = LocalDate.parse(dateStr, originalFormat);
                String formattedDate = date.format(targetFormat);
                csvPrinter.printRecord(drTcm, patientName, formattedDate, fees.get(i));
            } catch (DateTimeParseException e) {
                System.err.println("Unable to parse date from line: " + visitDates.get(i));
            }
        }
    }
}
