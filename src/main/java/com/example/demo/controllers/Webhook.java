package com.example.demo.controllers;

import com.example.demo.modals.WebhookDataModel;
import com.example.demo.utils.config.PropertyInputs;
import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class Webhook {

    private final PropertyInputs propertyInputs;

    public Webhook(PropertyInputs propertyInputs) {
        this.propertyInputs = propertyInputs;
    }

    @RequestMapping(
            value = "v1/listener",
            method = {RequestMethod.GET, RequestMethod.POST},
            consumes = {
                    "application/json",
                    "application/xml",
                    "application/x-www-form-urlencoded",
                    "multipart/form-data",
                    "*/*"
            }
    )
    public ResponseEntity<String> receiveWebhook(
            @RequestParam(required = false) Map<String, String> queryParams,
            @RequestParam(required = false) MultiValueMap<String, String> formParams,
            @RequestParam(required = false) MultiValueMap<String, MultipartFile> files,
            @RequestBody(required = false) String body,
            @RequestHeader(value = "Content-Type", required = false) String contentType
    ) {

        Gson gson = new Gson();
        if (queryParams != null && !queryParams.isEmpty()) {
            queryParams.forEach((key, value) -> System.out.println(key + ": " + value));
        }

        ResponseEntity<String> response;
        WebhookDataModel payloadObj;

        try {
            // JSON payload
            if (contentType != null && contentType.contains("application/json") && body != null) {
                response = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);

                // XML payload
            } else if (contentType != null && contentType.contains("application/xml") && body != null) {
                response = ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(body);

                // URL-encoded form data
            } else if (contentType != null && contentType.contains("application/x-www-form-urlencoded") && formParams != null) {
                formParams.forEach((key, value) -> System.out.println(key + ": " + value));
                payloadObj = new WebhookDataModel();
                Map<String, Object> data = new HashMap<>();
                data.put("workflowid", Objects.requireNonNull(formParams.getFirst("workflowid")));
                data.put("referenceno", Objects.requireNonNull(formParams.getFirst("referenceno")));
                payloadObj.setData(data);

                response = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(gson.toJson(formParams.toSingleValueMap()));

                // File uploads
            } else if (files != null && !files.isEmpty()) {
                files.forEach((key, fileList) -> {
                    for (MultipartFile f : fileList) {
                        System.out.println("Field: " + key);
                        System.out.println("File: " + f.getOriginalFilename() + " (" + f.getSize() + " bytes)");
                    }
                });
                response = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":\"ok\"}");

                // Query parameters only
            } else if ((body == null || body.isEmpty()) && queryParams != null && !queryParams.isEmpty()) {
                payloadObj = new WebhookDataModel();
                Map<String, Object> data = new HashMap<>();
                data.put("workflowid", queryParams.getOrDefault("workflowid", ""));
                data.put("referenceno", queryParams.getOrDefault("referenceno", ""));
                payloadObj.setData(data);

                response = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(gson.toJson(queryParams));

                // Empty payload
            } else {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(gson.toJson("{}"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK).body("{\"error\":\"Failed to parse payload\"}");
        }

        return response;
    }

    @RequestMapping(value = "/listener", consumes = MediaType.ALL_VALUE)
    public Map<String, Object> listener(
            @RequestBody(required = false) String body,
            @RequestParam(required = false) MultiValueMap<String, MultipartFile> files,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        List<String> fileIds = new ArrayList<>();

        try {
            // Handle multiple file uploads
            if (files != null && !files.isEmpty()) {
                for (List<MultipartFile> fileList : files.values()) {
                    for (MultipartFile f : fileList) {
                        if (!f.isEmpty()) {
                            String generatedId = UUID.randomUUID().toString().replace("-", "");
                            String ext = getFileExtension(f.getOriginalFilename());
                            String path = propertyInputs.getFOLDER_PATH() + File.separator + generatedId + (ext.isEmpty() ? "" : "." + ext);

                            Files.copy(f.getInputStream(), Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
                            fileIds.add(generatedId);
                        }
                    }
                }
            }

            // Handle body (JSON, XML, form-urlencoded)
            if (body != null && !body.trim().isEmpty()) {
                String generatedId = UUID.randomUUID().toString().replace("-", "");
                String contentType = request.getContentType();
                String extension;

                if (contentType != null) {
                    if (contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
                        extension = ".json";
                    } else if (contentType.contains(MediaType.APPLICATION_XML_VALUE)) {
                        extension = ".xml";
                    } else if (contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
                        extension = ".txt";
                        body = request.getParameterMap().entrySet()
                                .stream()
                                .map(e -> e.getKey() + "=" + String.join(",", e.getValue()))
                                .collect(Collectors.joining("\n"));
                    } else {
                        extension = ".txt";
                    }
                } else {
                    extension = ".txt";
                }

                String path = propertyInputs.getFOLDER_PATH() + File.separator + generatedId + extension;
                Files.write(Paths.get(path), body.getBytes());
                fileIds.add(generatedId);
            }

            response.put("status", true);
            response.put("message", fileIds);
            return response;

        } catch (Exception e) {
            response.put("status", false);
            response.put("message", e.getMessage());
            return response;
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0) ? filename.substring(dotIndex + 1) : "";
    }

}
