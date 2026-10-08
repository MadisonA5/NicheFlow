package com.nicheflow;

import org.springframework.test.util.ReflectionTestUtils;

import com.nicheflow.model.Role;
import com.nicheflow.model.User;

/** Builds users with an id, which is normally assigned by the database. */
public final class TestUsers {

    private TestUsers() {
    }

    public static User withId(long id, String email, String passwordHash, Role role) {
        User user = new User(email, passwordHash, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
