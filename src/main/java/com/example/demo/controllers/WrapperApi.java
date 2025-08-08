package com.example.demo.controllers;

import com.example.demo.modals.WrapperModel;
import com.example.demo.utils.Utilities;
import com.example.demo.utils.config.PropertyInputs;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.tika.Tika;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
public class WrapperApi {

    private final PropertyInputs propertyInputs;

    public WrapperApi(PropertyInputs propertyInputs) {
        this.propertyInputs = propertyInputs;
    }

    @RequestMapping("receiver")
    public ResponseEntity<?> fetchFiles(@RequestHeader Map<String, String> incomingHeaders,
                                        @RequestBody WrapperModel request) throws IOException {

        Gson gson = new Gson();
        HttpHeaders httpHeaders = new HttpHeaders();

        // Copy headers except Host and Content-Length
        for (Map.Entry<String, String> entry : incomingHeaders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!key.equalsIgnoreCase("host") &&
                    !key.equalsIgnoreCase("content-length")) {
                httpHeaders.set(key, value);
            }
        }

        byte[] fileBytes;

        // Case 1: No URL → serve local file
        if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
            String fileName = extractFileNameFromData(request.getData());

            if (fileName == null || fileName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Missing 'file' parameter in data");
            }

            Path matchedFile;
            try {
                matchedFile = Files.list(Paths.get(propertyInputs.getFOLDER_PATH()))
                        .filter(path -> {
                            String name = path.getFileName().toString();
                            return name.equals(fileName) || name.startsWith(fileName + ".");
                        })
                        .findFirst()
                        .orElse(null);
            } catch (IOException e) {
                return ResponseEntity.status(500)
                        .body("Error reading directory: " + e.getMessage());
            }

            if (matchedFile != null && Files.exists(matchedFile)) {
                fileBytes = Files.readAllBytes(matchedFile);
            } else {
                return ResponseEntity.status(404)
                        .body("No file found for: " + fileName);
            }

        }
        // Case 2: URL given → fetch from remote API
        else {
            HttpMethod httpMethod = request.getMethod() != null
                    ? HttpMethod.valueOf(request.getMethod().toUpperCase())
                    : HttpMethod.POST;

            fileBytes = Utilities.performApiCall(
                    request.getUrl(),
                    gson.toJson(request.getData()),
                    httpHeaders,
                    httpMethod,
                    byte[].class
            );
        }

        // Detect content type dynamically
        Tika tika = new Tika();
        String contentType = tika.detect(fileBytes);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(fileBytes);
    }

    /**
     * Extracts the "file" field from request data, regardless of JSON type.
     */
    private String extractFileNameFromData(Object data) {
        if (data instanceof Map) {
            Object fileObj = ((Map<?, ?>) data).get("file");
            return fileObj != null ? fileObj.toString() : null;
        } else if (data instanceof LinkedTreeMap) {
            Object fileObj = ((LinkedTreeMap<?, ?>) data).get("file");
            return fileObj != null ? fileObj.toString() : null;
        }
        return null;
    }
}
