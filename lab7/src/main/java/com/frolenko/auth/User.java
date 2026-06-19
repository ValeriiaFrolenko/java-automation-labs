package com.frolenko.auth;

public record User(
        Long id,
        String username,
        String email,
        String passwordHash,
        String salt,
        Role role
) {
    public enum Role { USER, ADMIN }
}