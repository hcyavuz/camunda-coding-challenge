package com.yhc.http;

import com.yhc.http.exceptions.HTTPClientExecutionException;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HTTPClient {
    private static final Logger logger = LoggerFactory.getLogger(HTTPClient.class);
    private int exponentLimit;
    private int maxAttempts;
    private OkHttpClient client;

    public HTTPClient() {
        this.exponentLimit = 4;
        this.maxAttempts = 5;
    }

    /**
     * Creates and configures a new HTTP client
     * @param connectTimeout Connection timeout value in milliseconds
     * @param socketTimeout Socket timeout value in milliseconds
     */
    public void configureConnectionAndCreateClient(int connectTimeout, int socketTimeout) {

        this.client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Executes a given http request,
     * @param httpRequest Okhttp Request object to call
     * @return If the call is successful (HTTP 2xx codes), returns the Response, if the response status was not successful it'll retry as many times as the client is configured for
     * Response might be null, if the response was not successful after all executions
     */
    public Response executeHTTPRequest(Request httpRequest) {
        if(this.client == null){
            configureConnectionAndCreateClient(2000, 2000);
        }
        Response httpResponse = null;
        Call httpCall = this.client.newCall(httpRequest);
        int currentAttempts = 0;
        int responseCode = -1;
        String failureResponseBody = "";

        while (currentAttempts < this.maxAttempts){
            try {
                // if an attempt has already been made, add a delay using exponential backoff
                if (currentAttempts > 0) {
                    // recreate to call because the previous stream will have been consumed
                    waitForHTTPRetry(currentAttempts);

                    logger.debug("Creating a new http call for retry");
                    httpCall = this.client.newCall(httpRequest);
                }

                // attempt to execute request
                currentAttempts++;
                logger.trace("Current attempt to execute the call is '{}'",
                        currentAttempts);
                httpResponse = httpCall.execute();

                // get response code
                responseCode = httpResponse.code();
                logger.trace("httpResponse received with response code '{}'",
                        responseCode);

                // break loop to return response if the request was successful (2xx)
                if (responseCode >= 200 && responseCode <= 299) {
                    logger.trace("API call response was successful");
                    return httpResponse;
                }
                // handle scenario where response code 401 is encountered to check if a stale token caused a failure
                else if(responseCode == 401) {
                    logger.error("401 Unauthorized response received, refreshing token");
                    // execute it again
                    logger.info("Calling the request again");
                    throw new HTTPClientExecutionException(failureResponseBody);
                }
                // throw exception if a client error was returned (4xx)
                else if (responseCode >= 400 && responseCode <= 499)  {
                    // try to get the response body in the exception message
                    logger.debug("API call failed with 4xx, getting the failure body");
                    failureResponseBody = getResponseBody(httpResponse);
                    logger.trace("Failure body is {}",
                            failureResponseBody);

                    throw new HTTPClientExecutionException(failureResponseBody);

                }
                // server errors, retry by default
                else if (responseCode >= 500 && responseCode <= 599)  {
                    // try to get the response body in the exception message
                    logger.debug("API call failed with 5xx, getting the failure body");
                    failureResponseBody = getResponseBody(httpResponse);
                    logger.trace("Failure body is {}",
                            failureResponseBody);
                    logger.debug("This call will be retried by default");
                } else {
                    logger.debug("Unknown status encountered '{}'",
                            responseCode);
                }


            } catch (Exception e){
                logger.error("API call failed", e);
            }
        }
        return httpResponse;
    }

    private void waitForHTTPRetry(int currentAttempts) {
        int delayInMilliseconds = getDelayInMilliseconds(currentAttempts);
        try {
            logger.trace("Thread will be slept for '{}' ms",
                    delayInMilliseconds);
            Thread.sleep(delayInMilliseconds);
        } catch (InterruptedException e) {
            logger.debug("Sleep for http retry has been interrupted",
                    e);
        }
    }

    private int getDelayInMilliseconds(int attempts)
    {
        RandomDataGenerator randomNumberGenerator = new RandomDataGenerator();
        // calculate number of seconds to wait based on exponential backoff algorithm
        int seconds = (int) Math.pow(2, Math.min(attempts - 1, this.exponentLimit));

        // transform seconds into milliseconds and add additional randomised delay
        return (seconds * 1000) + randomNumberGenerator.nextInt(0, 999);
    }

    private String getResponseBody(Response response){
        String responseBody = "";
        if(response != null){
            try {
                responseBody = response.body().string();
            } catch (IOException e) {
                // no body available
            }
        }
        return responseBody;
    }
}
