package com.example.demo.controllers;

import com.example.demo.modals.WebhookDataModel;
import com.google.gson.Gson;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
public class Webhook {

    @RequestMapping(value = "/webhook", method = {RequestMethod.GET, RequestMethod.POST},
            consumes = {"application/json", "application/xml", "application/x-www-form-urlencoded", "*/*"})
    public ResponseEntity<String> receiveWebhook(
            @RequestParam(required = false) Map<String, String> queryParams,
            @RequestParam(required = false) MultiValueMap<String, String> formParams,
            @RequestBody(required = false) String body,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            HttpServletRequest httpServletRequest) {

        Gson gson = new Gson();
        if (queryParams != null && !queryParams.isEmpty()) {
            queryParams.forEach((key, value) -> System.out.println(key + ": " + value));
        }
        ResponseEntity<String> response;
        WebhookDataModel payloadObj;
        try {
            if (contentType != null && contentType.contains("application/json") && body != null) {
                response = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
            } else if (contentType != null && contentType.contains("application/xml") && body != null) {
                response = ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(body);
            } else if (contentType != null && contentType.contains("application/x-www-form-urlencoded") && formParams != null) {
                formParams.forEach((key, value) -> System.out.println(key + ": " + value));
                payloadObj = new WebhookDataModel();
                Map<String, Object> data = new HashMap<>();
                data.put("workflowid", Objects.requireNonNull(formParams.getFirst("workflowid")));
                data.put("referenceno", Objects.requireNonNull(formParams.getFirst("referenceno")));
                payloadObj.setData(data);

                response = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(gson.toJson(formParams.toSingleValueMap()));
            } else if ((body == null || body.isEmpty()) && queryParams != null && !queryParams.isEmpty()) {
                payloadObj = new WebhookDataModel();
                Map<String, Object> data = new HashMap<>();
                data.put("workflowid", queryParams.getOrDefault("workflowid", ""));
                data.put("referenceno", queryParams.getOrDefault("referenceno", ""));
                payloadObj.setData(data);
                response = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(gson.toJson(queryParams));
            } else {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(gson.toJson("{}"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK).body("{\"error\":\"Failed to parse payload\"}");
        }
        return response;
    }
}
