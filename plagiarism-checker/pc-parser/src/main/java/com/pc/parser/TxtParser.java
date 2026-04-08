package com.pc.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Reads plain text files (TXT, MD, source code) as-is in UTF-8.
 * Also handles basic source code files by preserving structure.
 */
public class TxtParser implements DocumentParser {

    @Override
    public String parse(InputStream input) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @Override
    public String supportedExtension() {
        return "txt";
    }
}
