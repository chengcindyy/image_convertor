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
import java.util.concurrent.ExecutionException;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class ConverterGUI extends JFrame {

    private static final PathPreference pathPreference = new PathPreference();
    private static JTextField step1InputTextField;
    private static JTextField step1OutputTextField;
    private static JTextField step2InputTextField;
    private static JLabel progressLabel;
    private static JProgressBar progressBar;
    private static JTabbedPane tabbedPane = new JTabbedPane();
    private static Dotenv dotenv = Dotenv.load();
    private final String SUBSCRIPTION_KEY_ONE = dotenv.get("AZURE_TEXT_ANALYTICS_SUBSCRIPTION_KEY");
    private final String ENDPOINT = dotenv.get("AZURE_TEXT_ANALYTICS_ENDPOINT");
    private ImageAnalysisClient client = new ImageAnalysisClientBuilder()
            .credential(new AzureKeyCredential(SUBSCRIPTION_KEY_ONE))
            .endpoint(ENDPOINT)
            .buildClient();

    private ConverterGUI(String lastUsedFolderPath, String lastUsedFilePath) {
        // set title and size
        setTitle("PDF to CSV Converter");
        setSize(370, 230);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create components
        JPanel step1Panel = new JPanel();
        JPanel step2Panel = new JPanel();
        JButton convertButton = new JButton("Convert to CSV");
        JButton processButton = new JButton("Process");
        JButton step1InputSelectButton = new JButton("Select Input Folder");
        JButton step1OutputSelectButton = new JButton("Select a Output File");
        JButton step2InputSelectButton = new JButton("Select a Input File");
        JLabel step1HeaderLabel = new JLabel("Step 1: Convert PDF to CSV");
        JLabel step1ContentLabel = new JLabel("Select input folder and output file to convert");
        JLabel step2HeaderLabel = new JLabel("Step 2: Arrange Appointment Time");
        JLabel step2ContentLabel = new JLabel("After converting to csv, select the csv file to process");
        step1InputTextField = new JTextField(15);
        step1OutputTextField = new JTextField(15);
        progressLabel = new JLabel("Click to start");
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(322, 20));
        step2InputTextField = new JTextField(15);

        // Add components to window
        // TAB:
        tabbedPane.addTab("Step 1", step1Panel);
        tabbedPane.addTab("Step 2", step2Panel);
        add(tabbedPane, BorderLayout.CENTER);
        setVisible(true);
        // STEP 1:
        step1Panel.add(step1HeaderLabel);
        step1Panel.add(step1ContentLabel);
        step1Panel.add(step1InputSelectButton);
        step1Panel.add(step1InputTextField);
        step1Panel.add(step1OutputSelectButton);
        step1Panel.add(step1OutputTextField);
        step1Panel.add(progressBar);
        step1Panel.add(convertButton);
        step1Panel.add(progressLabel);
        // STEP 2:
        step2Panel.add(step2HeaderLabel);
        step2Panel.add(step2ContentLabel);
        step2Panel.add(step2InputSelectButton);
        step2Panel.add(step2InputTextField);
        step2Panel.add(processButton);

        // Use saved path and file name
        if (!lastUsedFolderPath.isEmpty()) {
            step1InputTextField.setText(lastUsedFolderPath);

        }
        if (!lastUsedFilePath.isEmpty()) {
            step1OutputTextField.setText(lastUsedFilePath);
            step2InputTextField.setText(lastUsedFilePath);
        }

        // Set buttons behavior
        step1InputSelectButton.addActionListener(e ->
                selectPathFromChooser(true, pathPreference.getLastUsedFolder(), step1InputTextField, null));

        step1OutputSelectButton.addActionListener(e ->
                selectPathFromChooser(false, pathPreference.getLastUsedFilePath(), step1OutputTextField, "csv"));

        step2InputSelectButton.addActionListener(e ->
                selectPathFromChooser(false, pathPreference.getLastUsedFilePath(), step1OutputTextField, "csv"));

        // call converter
        convertButton.addActionListener(e -> {
            String step1InputPath = step1InputTextField.getText().trim();
            String step1OutputPath = step1OutputTextField.getText().trim();

            // Disable the convert button while processing
            convertButton.setEnabled(false);

            try {
                processDirectory(step1InputPath, step1OutputPath);
            } finally {
                // Re-enable the convert button after processing
                convertButton.setEnabled(true);
            }
        });

        processButton.addActionListener(e -> {
            String step2InputPath = step2InputTextField.getText().trim();

            try {
                TimeFiller filler = new TimeFiller();
                TimeFiller.processCSV(step2InputPath);
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

    private void processDirectory(String dirPath, String outputPath) {
        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                File dir = new File(dirPath);
                if (dir.exists() && dir.isDirectory()) {
                    File[] fileList = dir.listFiles();
                    if (fileList != null) {
                        int fileListSize = fileList.length;
                        System.out.println("Reading " + fileListSize + " file(s)...");

                        for (int i = 0; i < fileListSize; i++) {
                            if (isCancelled()) {
                                break; // Allow the swing worker to be cancellable
                            }

                            File file = fileList[i];
                            if (file.isFile()) {
                                String filePath = file.getAbsolutePath();
                                String extension = "";

                                int j = filePath.lastIndexOf('.');
                                if (j > 0) {
                                    extension = filePath.substring(j + 1).toLowerCase();
                                }

                                System.out.println("Processing " + file.getName() + "...");

                                switch (extension) {
                                    case "pdf":
                                        //check content (img or txt)
                                        //System.out.println("Found pdf, PDFContentAnalyzer Called");
                                        PDFContentAnalyzer(file, outputPath);
                                        break;
                                    case "jpeg":
                                    case "jpg":
                                    case "png":
                                    case "gif":
                                    case "bmp":
                                        //System.out.println("Found Image File, ReadImageWithOCR Called");
                                        readImageWithOCR(filePath, outputPath);
                                        break;
                                    default:
                                        System.err.println("Unsupported file type: " + extension);
                                        break;
                                }

                                // Update progress
                                int progress = (int) (((double) (i + 1) / fileListSize) * 100);
                                publish(progress);
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "No files found in the directory.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid directory path.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int mostRecentValue = chunks.get(chunks.size() - 1);
                progressBar.setValue(mostRecentValue);
                progressLabel.setText("Processing... " + mostRecentValue + "% completed");
            }

            @Override
            protected void done() {
                try {
                    get(); // Call get to rethrow exceptions occurred during doInBackground
                    System.out.println("Completed!");
                    progressBar.setValue(100);
                    progressLabel.setText("Completed!");
                    JOptionPane.showMessageDialog(null, "CSV file was created or updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    progressLabel.setText("Failed!");
                    JOptionPane.showMessageDialog(null, "Error occurred during conversion.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
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
