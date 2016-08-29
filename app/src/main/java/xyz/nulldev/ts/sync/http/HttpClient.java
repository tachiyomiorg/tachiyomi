package xyz.nulldev.ts.sync.http;

import java.io.IOException;
import java.util.Map;

/**
 * Project: TachiServer
 * Author: nulldev
 * Creation Date: 26/08/16
 *
 * Skeleton for a class that can make HTTP requests
 */
public interface HttpClient {
    /**
     * Make a synchronous HTTP POST request
     * @param url The URL to POST to.
     * @param headers The headers to include in the request.
     * @param postBody The body of the POST request.
     * @return The response from the server
     * @throws IOException If an error occurred while making the request.
     */
    String postRequest(String url, Map<String, String> headers, String postBody) throws IOException;

    /**
     * Make a synchronous HTTP GET request
     * @param url The URL to GET from.
     * @param headers The headers to include in the request.
     * @return The response from the server.
     * @throws IOException If an error occured while making the request.
     */
    String getRequest(String url, Map<String, String> headers) throws IOException;
}
