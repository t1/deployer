package com.github.t1.deployer.tools;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static lombok.AccessLevel.*;

import java.util.*;

import lombok.*;

@Value
@AllArgsConstructor(access = PRIVATE)
public class User {
    public static HashSet<String> asSet(String... privileges) {
        return new HashSet<>(asList(privileges));
    }

    public static final User ANONYMOUS = new User("anonymous");

    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    public static User getCurrent() {
        User current = CURRENT.get();
        return (current == null) ? ANONYMOUS : current;
    }

    public static void setCurrent(User user) {
        CURRENT.set(user);
    }

    String name;
    Set<String> privileges;

    public User(String name) {
        this.name = name;
        this.privileges = emptySet();
    }

    public boolean hasPrivilege(String privilege) {
        return privileges.contains(privilege);
    }

    public User withPrivilege(String... privileges) {
        Set<String> merged = (this.privileges.isEmpty()) ? asSet(privileges) : merged(this.privileges, privileges);
        return new User(name, merged);
    }

    private Set<String> merged(Set<String> existing, String... additional) {
        Set<String> out = new HashSet<>(existing);
        for (String privilege : additional) {
            out.add(privilege);
        }
        return unmodifiableSet(out);
    }

    @Override
    public String toString() {
        return getName();
    }
}
