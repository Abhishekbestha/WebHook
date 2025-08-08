package com.example.demo.utils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class Utilities {

    public static <T> T performApiCall(String url, Object requestBody, HttpHeaders requestHeaders, HttpMethod method, Class<T> responseType) {
        boolean proxyEnable = false;
        String proxyHost = "";
        int proxyPort = 0;
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
			if (proxyEnable) {
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
				requestFactory.setProxy(proxy);
			}
            requestFactory.setConnectTimeout(180_000);
            requestFactory.setReadTimeout(180_000);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            HttpEntity<?> requestEntity = new HttpEntity<>(requestBody, requestHeaders);
            ResponseEntity<T> response = restTemplate.exchange(url, method, requestEntity, responseType);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
}
