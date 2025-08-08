package com.example.demo.controllers;

import com.example.demo.modals.WrapperModel;
import com.example.demo.utils.Utilities;
import com.google.gson.Gson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class WrapperApi {

    @RequestMapping("wrapperApi")
    public ResponseEntity<?> test(@RequestHeader Map<String, String> incomingHeaders, @RequestBody WrapperModel request) {
        Gson gson = new Gson();
        HttpHeaders httpHeaders = new HttpHeaders();
        incomingHeaders.forEach((key, value) -> {
            if (!key.equalsIgnoreCase("host") &&
                    !key.equalsIgnoreCase("content-length")) {
                httpHeaders.set(key, value);
            }
        });
        byte[] pdfBytes = Utilities.performApiCall(request.getUrl(), gson.toJson(request.getData()), httpHeaders, HttpMethod.POST, byte[].class);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

}
