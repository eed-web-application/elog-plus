package edu.stanford.slac.elog_plus;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableMongock
@SpringBootApplication()
@ComponentScan({"edu.stanford.slac"})
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
