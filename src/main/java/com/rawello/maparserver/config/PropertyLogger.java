package com.rawello.maparserver.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PropertyLogger {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @PostConstruct
    public void logProperties() {
        System.out.println("jwt.secret: " + jwtSecret);
        System.out.println("jwt.expiration: " + jwtExpiration);
    }
}