package kr.dove.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.inject.Inject;

@RestController
public class HttpService {

    private final WebClient webClient;

    @Inject
    public HttpService(WebClient.Builder builder,
                           @Value("${server.port}") int port) {
        this(builder.baseUrl("https://127.0.0.1:" + port).build());
    }

    HttpService(WebClient webClient) {
        this.webClient = webClient;
    }

    @GetMapping(value = "/greeting")
    public Mono<String> greeting(@RequestParam(name = "name", defaultValue = "armeria") String name) {
        return webClient
                .get()
                .uri("/greeting?name=" + name)
                .retrieve()
                .bodyToMono(String.class);
    }
}
