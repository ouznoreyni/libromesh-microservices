package sn.noreyni.integration.userservice;

import org.springframework.boot.SpringApplication;
import sn.noreyni.userservice.UserServiceApplication;

public class TestUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(UserServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
