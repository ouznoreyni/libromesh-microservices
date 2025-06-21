package sn.noreyni.userservice.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConfigurationLogger {

    @Autowired
    private Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void logConfigurationOnStartup() {
        log.info("=== Session Configuration ===");
        log.info("vault default-context: {}", environment.getProperty("spring.cloud.vault.kv.default-context"));
        log.info("Session Timeout: {}", environment.getProperty("app.timeout.session"));
        log.info("User Registration: {}", environment.getProperty("app.feature.user-registration-enabled"));
        log.info("Server Port: {}", environment.getProperty("server.port"));
        log.info("Application Name: {}", environment.getProperty("spring.application.name"));
    }
}