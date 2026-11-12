package cl.duoc.pedidos360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
		"com.azure.spring.cloud.autoconfigure.implementation.kafka.AzureEventHubsKafkaOAuth2AutoConfiguration"
})
public class Pedidos360BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(Pedidos360BackendApplication.class, args);
	}

}
