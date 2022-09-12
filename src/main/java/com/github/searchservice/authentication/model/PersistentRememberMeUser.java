package com.github.searchservice.authentication.model;

import java.util.Set;

public record PersistentRememberMeUser(String username, Set<String> series) {
}
