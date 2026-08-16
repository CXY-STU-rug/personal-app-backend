package com.liyuq;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@MapperScan("com.liyuq.mapper")
@SpringBootApplication
@EnableAsync
public class PersonalApplication {
    public static void main(String[] args) {
        SpringApplication.run(PersonalApplication.class, args);
    }


}
