package com.yhc.codingchallenge;

import com.yhc.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Coding Challenge for Technical Consultants
 * Camunda Consulting
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        logger.info("Stating the coding challenge!");
        UserClient userClient = new UserClient();
        try {
            List<User> listOfUsers = userClient.fetchUsers(1);
            logger.info("Users have been fetched, there are '{}' users",
                    listOfUsers.size());
        } catch (IOException e) {
            logger.error("IO Exception occurred, response body might have been read properly",
                    e);
        } catch (Exception e) {
            logger.error("Unexpected exception occurred while fetching users",
                    e);
        }
    }
}