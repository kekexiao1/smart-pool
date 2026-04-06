package com.xiao.smartpooladminserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class SmartPoolAdminServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartPoolAdminServerApplication.class, args);
	}

}
