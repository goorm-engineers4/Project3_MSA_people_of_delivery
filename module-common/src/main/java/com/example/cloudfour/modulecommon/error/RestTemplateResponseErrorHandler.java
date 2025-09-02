package com.example.cloudfour.modulecommon.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestTemplateResponseErrorHandler.class);
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        handleErrorInternal(url, method, response);
    }

    private void handleErrorInternal(URI url, HttpMethod method, ClientHttpResponse response) throws IOException, HttpClientErrorException {
        var status = response.getStatusCode();
        String statusText = response.getStatusText();
        HttpHeaders headers = response.getHeaders();

        String body = "";
        try {
            body = StreamUtils.copyToString(response.getBody(), DEFAULT_CHARSET);
        } catch (Exception ignore) { /* body 없을 수 있음 */ }

        logger.error("### RestTemplate ERROR ###");
        if (url != null) logger.error("Request: {} {}", method, url);
        logger.error("Status: {} {}", status.value(), status);
        logger.error("StatusText: {}", statusText);
        logger.error("Headers: {}", headers);
        logger.error("Body: {}", body);

        byte[] bodyBytes = body != null ? body.getBytes(DEFAULT_CHARSET) : new byte[0];

        if (status.is5xxServerError()) {
            throw HttpServerErrorException.create(status, statusText, headers, bodyBytes, DEFAULT_CHARSET);
        } else if (status.is4xxClientError()) {
            switch (status.value()) {
                case 401, 404, 403 -> throw HttpClientErrorException.Unauthorized.create(status, statusText, headers, bodyBytes, DEFAULT_CHARSET);
                default -> throw HttpClientErrorException.create(status, statusText, headers, bodyBytes, DEFAULT_CHARSET);
            }
        }
    }
}


