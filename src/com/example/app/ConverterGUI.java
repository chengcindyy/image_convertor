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
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class ConverterGUI extends JFrame {

    static PathPreference pathPreference = new PathPreference();
    private final JTextField inputTextField;
    private final JTextField outputTextField;
    Dotenv dotenv = Dotenv.load();
    private final String SUBSCRIPTION_KEY_ONE = dotenv.get("AZURE_TEXT_ANALYTICS_SUBSCRIPTION_KEY");
    private final String ENDPOINT = dotenv.get("AZURE_TEXT_ANALYTICS_ENDPOINT");

    public ConverterGUI(String lastUsedFolderPath, String lastUsedFilePath) {
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

            processDirectory(inputPath, outputPath);
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
                                break;
                            case "jpeg":
                            case "jpg":
                            case "png":
                            case "gif":
                            case "bmp":
                                System.out.println("Found Image File, ReadImageWithOCR Called");
                                readImageWithOCR(filePath, outputPath); // 确保这个方法已经适当修改来处理单个文件
                                break;
                            default:
                                System.out.println("Unsupported file type: " + extension);
                                break;
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "No files found in the directory.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Invalid directory path.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void convertPDFToCSV(String inputFolderPath, String outputCSVPath) {
        List<String> inputPDFs;
        try (Stream<Path> walk = Files.walk(Paths.get(inputFolderPath))) {
            inputPDFs = walk
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(path -> path.endsWith(".pdf"))
                    .toList();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading the folder: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean isNewFile = !Files.exists(Paths.get(outputCSVPath));
        try (FileWriter out = new FileWriter(outputCSVPath, true);
             CSVPrinter csvPrinter = isNewFile ?
                     new CSVPrinter(out, CSVFormat.Builder.create().setHeader("DR.TCM", "Patient Name", "Start Date", "Duration", "End Date").build()) :
                     new CSVPrinter(out, CSVFormat.DEFAULT)) {

            for (String inputPDF : inputPDFs) {
                try (PDDocument document = Loader.loadPDF(new File(inputPDF))) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String text = stripper.getText(document);
                    String[] lines = text.split("\\r?\\n");

                    processPDFLines(csvPrinter, lines);
                }
            }

            JOptionPane.showMessageDialog(this, "CSV file was created or updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void readImageWithOCR(String imagePath, String outputPath) {
        // Initialize Client side of Azure Text Analytics
        ImageAnalysisClient client = new ImageAnalysisClientBuilder()
                .credential(new AzureKeyCredential(SUBSCRIPTION_KEY_ONE))
                .endpoint(ENDPOINT)
                .buildClient();

        // Process the single file with OCR
        String textResult = recognizeText(client, Path.of(imagePath));
        System.out.println("识别结果: " + textResult);
        // TODO: 处理和储存识别结果，例如储存到 CSV 文件中
    }


    private static String recognizeText(ImageAnalysisClient client, Path imagePath) {
        StringBuilder resultText = new StringBuilder();
        try {
            ImageAnalysisResult result = client.analyze(
                    BinaryData.fromFile(imagePath), // imageData: Image file loaded into memory as BinaryData
                    Collections.singletonList(VisualFeatures.READ), // visualFeatures
                    null); // options: There are no options for READ visual feature

            System.out.println("Image analysis results:");
            System.out.println(" Read:");
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

    private static void processPDFLines(CSVPrinter csvPrinter, String[] lines) throws IOException {
        String drTcm = "";
        String patientName = "";
        List<String> visitDates = new ArrayList<>();
        List<String> fees = new ArrayList<>();
        boolean isReadingFees = false;

        DateTimeFormatter originalFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
        DateTimeFormatter targetFormat = DateTimeFormatter.ofPattern("yyyy/M/d");

        for (String line : lines) {
            String trim = line.substring(line.indexOf(":") + 1).trim();
            if (line.startsWith("DR.TCM.:") || line.startsWith("DR. TCM:")) {
                drTcm = trim;
            } else if (line.startsWith("Patient Name:")) {

                patientName = trim;
            } else if (line.matches("\\b\\w{3}\\s\\d{1,2},\\d{4}")) {
                visitDates.add(line.trim());
            } else if (line.startsWith("Subtotal")) {
                isReadingFees = true;
            } else if (isReadingFees && line.matches("\\$\\d+\\.\\d{2}")) {
                String fee = line.trim();
                String duration = fee.equals("$70.00") ? "50" : fee.equals("$75.00") ? "60" : "0";
                fees.add(duration);
            }
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
