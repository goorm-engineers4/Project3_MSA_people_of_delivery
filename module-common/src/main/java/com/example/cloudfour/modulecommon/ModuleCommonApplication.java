package com.example.cloudfour.modulecommon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
public class ModuleCommonApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModuleCommonApplication.class, args);
    }
}
