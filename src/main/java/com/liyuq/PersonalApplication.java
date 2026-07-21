package com.liyuq;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@MapperScan("com.liyuq.mapper")
@SpringBootApplication
public class PersonalApplication {
    public static void main(String[] args) {
        SpringApplication.run(PersonalApplication.class, args);
    }


}
