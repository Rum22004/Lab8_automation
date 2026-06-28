package edu.itc.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lab 08 starter — Private Cloud Storage App.
 *
 * <p>A small Spring Boot backend where each user manages files and folders in
 * their own isolated storage. New users are granted a 50 MB quota; users can
 * manage their profile and delete their own account.</p>
 *
 * <p>The accompanying test suite (src/test/java) demonstrates every testing
 * method from Lesson 08 and reports through Allure.</p>
 */
@SpringBootApplication
public class PrivateCloudStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrivateCloudStorageApplication.class, args);
    }
}
