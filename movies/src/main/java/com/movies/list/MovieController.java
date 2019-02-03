package com.movies.list;

import io.opentracing.Tracer;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.http.*;
//import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class MovieController {
	private static final String RESPONSE_STRING_FORMAT = "movies => %s\n";
	
	private static final String RESPONSE_STRING_FORMAT_UI = "[{\"name\":\"Baahubali\", \"director\":\"SS Rajamouli\"} , %s\n]";
	
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;

    @Value("${booking.api.url:http://localhost:7777}")
    private String remoteURL;
    
    @Value("${booking2.api.url:http://localhost:7777/ui}")
    private String UIremoteURL;

    @Autowired
    private Tracer tracer;

    public MovieController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<String> getMovies(@RequestHeader("User-Agent") String userAgent, @RequestParam("x-header-apikey") String moviePreference) {
        try {
            /**
             * Set baggage
             */
            tracer.activeSpan().setBaggageItem("user-agent", userAgent);
            if (moviePreference != null && !moviePreference.isEmpty()) {
                tracer.activeSpan().setBaggageItem("x-header-apikey", moviePreference);
            }

            ResponseEntity<String> responseEntity = restTemplate.getForEntity(remoteURL, String.class);
            String response = responseEntity.getBody();
            return ResponseEntity.ok(String.format(RESPONSE_STRING_FORMAT, response.trim()));
        } catch (HttpStatusCodeException ex) {
            logger.warn("Exception trying to get the response from booking service.", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(String.format(RESPONSE_STRING_FORMAT,
                            String.format("%d %s", ex.getRawStatusCode(), createHttpErrorResponseString(ex))));
        } catch (RestClientException ex) {
            logger.warn("Exception trying to get the response from booking service.", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(String.format(RESPONSE_STRING_FORMAT, ex.getMessage()));
        }
    }
    
    
    @RequestMapping(value = "/ui", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getMoviesUI(@RequestHeader("User-Agent") String userAgent, @RequestHeader(value = "movie-preference", required = false) String uiPreference) {
        try {
            /**
             * Set baggage
             */
            tracer.activeSpan().setBaggageItem("user-agent", userAgent);
            if (uiPreference != null && !uiPreference.isEmpty()) {
                tracer.activeSpan().setBaggageItem("movie-preference", uiPreference);
            }

            ResponseEntity<String> responseEntity = restTemplate.getForEntity(UIremoteURL, String.class);
            String response = responseEntity.getBody();
            return ResponseEntity.ok(String.format(RESPONSE_STRING_FORMAT_UI, response.trim()));
        } catch (HttpStatusCodeException ex) {
            logger.warn("Exception trying to get the response from booking service.", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(String.format(RESPONSE_STRING_FORMAT_UI,
                            String.format("%d %s", ex.getRawStatusCode(), createHttpErrorResponseString(ex))));
        } catch (RestClientException ex) {
            logger.warn("Exception trying to get the response from booking service.", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(String.format(RESPONSE_STRING_FORMAT_UI, ex.getMessage()));
        }
    }
    
    private String createHttpErrorResponseString(HttpStatusCodeException ex) {
        String responseBody = ex.getResponseBodyAsString().trim();
        if (responseBody.startsWith("null")) {
            return ex.getStatusCode().getReasonPhrase();
        }
        return responseBody;
    }

}
