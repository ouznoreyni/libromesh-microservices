package sn.noreyni.gateweyservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GateweyServiceApplication {
	@Value("application.test")
	private static String values;

	public static void main(String[] args) {
		System.out.println("Starting Gatewey Service Application");
		System.out.println("*************************");
		System.out.println(values);
		System.out.println("*************************");
		SpringApplication.run(GateweyServiceApplication.class, args);
	}

	@Bean
	public DiscoveryClientRouteDefinitionLocator discoveryClientRouteLocator(
			ReactiveDiscoveryClient reactiveDiscoveryClient,
			DiscoveryLocatorProperties discoveryLocatorProperties) {
		return new DiscoveryClientRouteDefinitionLocator(reactiveDiscoveryClient, discoveryLocatorProperties);
	}
}
