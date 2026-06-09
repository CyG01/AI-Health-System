package com.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.properties.CorsProperties;
import com.example.properties.DeepSeekProperties;
import com.example.properties.JwtProperties;

@SpringBootApplication
@MapperScan("com.example.mapper")
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, DeepSeekProperties.class})
public class AiHealthSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiHealthSystemApplication.class, args);
    }
}
