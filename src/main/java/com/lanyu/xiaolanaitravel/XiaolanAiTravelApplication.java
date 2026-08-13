package com.lanyu.xiaolanaitravel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lanyu.xiaolanaitravel.**.mapper")
public class XiaolanAiTravelApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaolanAiTravelApplication.class, args);
    }

}
