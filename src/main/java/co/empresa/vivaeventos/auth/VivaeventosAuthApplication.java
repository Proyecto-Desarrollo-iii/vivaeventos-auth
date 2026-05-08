package co.empresa.vivaeventos.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VivaeventosAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(VivaeventosAuthApplication.class, args);
    }
}