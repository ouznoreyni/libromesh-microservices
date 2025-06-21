package sn.noreyni.userservice.authentication;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @GetMapping("/status")
    public Mono<Map<String, String>> getAuthStatus() {
        return Mono.just(new HashMap<>() {{
            put("status", "active");
            put("message", "Authentication service is running");
            put("timestamp", String.valueOf(System.currentTimeMillis()));
        }});
    }

    @PostMapping("/login")
    public Mono<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        return Mono.just(new HashMap<>() {{
            put("success", true);
            put("token", "generated-jwt-token");
            put("username", credentials.get("username"));
            put("roles", new String[]{"USER"});
        }});
    }
}