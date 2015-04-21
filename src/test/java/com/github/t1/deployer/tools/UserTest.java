package com.github.t1.deployer.tools;

import static com.github.t1.deployer.tools.User.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class UserTest {
    @Test
    public void shouldCreateUnprivileged() {
        User user = new User("tom");

        assertEquals("tom", user.getName());
        assertTrue(user.getPrivileges().isEmpty());
        assertFalse(user.hasPrivilege("foo"));
    }

    @Test
    public void shouldAddOnePrivilege() {
        User orig = new User("tom");

        User mod = orig.withPrivilege("foo");

        assertTrue(orig.getPrivileges().isEmpty());
        assertFalse(orig.hasPrivilege("foo"));

        assertEquals(asSet("foo"), mod.getPrivileges());
        assertTrue(mod.hasPrivilege("foo"));
        assertFalse(mod.hasPrivilege("bar"));
    }

    @Test
    public void shouldAddTwoPrivileges() {
        User orig = new User("tom");

        User mod = orig.withPrivilege("foo", "bar");

        assertTrue(orig.getPrivileges().isEmpty());
        assertEquals(asSet("foo", "bar"), mod.getPrivileges());
        assertTrue(mod.hasPrivilege("foo"));
        assertTrue(mod.hasPrivilege("bar"));
    }

    @Test
    public void shouldAddPrivilegesTwice() {
        User orig = new User("tom");

        User mod = orig.withPrivilege("foo").withPrivilege("bar");

        assertTrue(orig.getPrivileges().isEmpty());
        assertEquals(asSet("foo", "bar"), mod.getPrivileges());
    }
}
