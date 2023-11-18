package com.yhc.codingchallenge;

import com.yhc.models.User;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserClientTest {

    @Test
    void fetchUsers() throws IOException {
        UserClient userClient = new UserClient();
        List<User> listOfUsers = userClient.fetchUsers(1);

        assertNotNull(listOfUsers);
        assertEquals(6, listOfUsers.size());
        assertEquals("George", listOfUsers.get(0).getFirstName());
    }
}