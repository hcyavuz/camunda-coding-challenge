package com.yhc.codingchallenge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yhc.generic.ApplicationConstants;
import com.yhc.http.HTTPClient;
import com.yhc.models.User;
import com.yhc.models.UserResponse;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Class to perform API operations with Users
 */
public class UserClient {
    private static final Logger logger = LoggerFactory.getLogger(UserClient.class);
    private HTTPClient httpClient;
    private final String API_END_POINT;
    private Gson gson;
    public UserClient() {
        httpClient = new HTTPClient();
        this.API_END_POINT = ApplicationConstants.API_BASE_URL + ApplicationConstants.API_USERS_END_POINT;
        gson = new GsonBuilder().create();
    }

    /**
     * Fetches users from the API with the given page number
     * @param pageNumber Page number to fetch the users from
     * @return Returns the list of users if any found, else if the response body is null, returns null
     * @throws IOException If the API response stream could not be read, throws an IOException
     */
    public List<User> fetchUsers(int pageNumber) throws IOException {
        HttpUrl httpUrl = HttpUrl.parse(API_END_POINT).newBuilder()
                .addQueryParameter("page", String.valueOf(pageNumber))
                .build();

        Request request = new Request.Builder()
                .url(httpUrl.toString())
                .addHeader(ApplicationConstants.CONTENT_TYPE, ApplicationConstants.CONTENT_TYPE_API)
                .method("GET", null)
                .build();

        Response response = httpClient.executeHTTPRequest(request);
        if (response.body() == null) {
            logger.info("Response body was null, users could not be fetched");
            return null;
        }
        UserResponse apiResponse = gson.fromJson(response.body().string(), UserResponse.class);
        return apiResponse.data;
    }
}
