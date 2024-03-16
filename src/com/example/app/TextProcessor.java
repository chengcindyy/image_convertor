package com.example.app;

import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.List;

public interface TextProcessor {
    void processText(List<String> lines, CSVPrinter csvPrinter) throws IOException;
}
