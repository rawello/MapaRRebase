package com.rawello.maparserver;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@Configurable
public class MaparServer {

	public static void main(String[] args) {
		SpringApplication.run(MaparServer.class, args);
	}
}