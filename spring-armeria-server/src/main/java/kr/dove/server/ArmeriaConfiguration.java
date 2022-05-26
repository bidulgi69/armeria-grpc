package kr.dove.server;

import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.circuitbreaker.CircuitBreakerClient;
import com.linecorp.armeria.client.circuitbreaker.CircuitBreakerRule;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.server.HttpServiceWithRoutes;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.annotation.Default;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.docs.DocServiceFilter;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.server.logging.AccessLogWriter;
import com.linecorp.armeria.server.logging.ContentPreviewingService;
import com.linecorp.armeria.server.logging.LoggingService;
import com.linecorp.armeria.spring.ArmeriaServerConfigurator;
import com.linecorp.armeria.spring.web.reactive.ArmeriaClientConfigurator;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import kr.dove.lib.GreetingGrpc;
import kr.dove.lib.HelloRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class ArmeriaConfiguration {

    private final GreetingServiceImpl greetingServiceImpl;

    @Autowired
    public ArmeriaConfiguration(GreetingServiceImpl greetingServiceImpl) {
        this.greetingServiceImpl = greetingServiceImpl;
    }

    @Bean
    public ArmeriaServerConfigurator armeriaServerConfigurator() {
        // Customize the server using the given ServerBuilder. For example:
        return builder -> {
            builder.http(8080)
                    .https(8443)
                    .tlsSelfSigned();

            // Add DocService that enables you to send Thrift and gRPC requests from web browser.
            builder.serviceUnder("/docs", new DocService());

            // Log every message which the server receives and responds.
            builder.decorator(LoggingService.newDecorator());
            builder.decorator(ContentPreviewingService.newDecorator(Integer.MAX_VALUE, StandardCharsets.UTF_8));
            // Write access log after completing a request.
            builder.accessLogWriter(AccessLogWriter.combined(), false);

            // You can also bind annotated HTTP services and asynchronous RPC services such as Thrift and gRPC:
            // builder.annotatedService("/rest", service);
            // builder.service("/thrift", THttpService.of(...));
            // builder.service(GrpcService.builder()...build());
            configureServices(builder);
        };
    }

    @Bean
    public ClientFactory clientFactory() {
        return ClientFactory.insecure();
    }

    @Bean
    public ArmeriaClientConfigurator armeriaClientConfigurator(ClientFactory clientFactory) {
        // Customize the client using the given WebClientBuilder. For example:
        return builder -> {
            // Use a circuit breaker for each remote host.
            final CircuitBreakerRule rule = CircuitBreakerRule.builder()
                    .onServerErrorStatus()
                    .onException()
                    .thenFailure();
            builder.decorator(CircuitBreakerClient.builder(rule)
                    .newDecorator());

            // Set a custom client factory.
            builder.factory(clientFactory);
        };
    }

    private void configureServices(ServerBuilder sb) {
        final HelloRequest exampleRequest = HelloRequest
                .newBuilder()
                .setName("Armeria")
                .setAge(7)
                .setAsia(HelloRequest.Asia.Korea)
                .build();
        final HttpServiceWithRoutes grpcService =
                GrpcService.builder()
                        .addService(greetingServiceImpl)
                        // See https://github.com/grpc/grpc-java/blob/master/documentation/server-reflection-tutorial.md
                        .addService(ProtoReflectionService.newInstance())
                        .supportedSerializationFormats(GrpcSerializationFormats.values())
                        .enableUnframedRequests(true)
                        // You can set useBlockingTaskExecutor(true) in order to execute all gRPC
                        // methods in the blockingTaskExecutor thread pool.
                        // .useBlockingTaskExecutor(true)
                        .build();
        sb.service(grpcService)
                .service("prefix:/prefix", grpcService)
                // You can access the documentation service at http://127.0.0.1:8080/docs.
                // See https://armeria.dev/docs/server-docservice for more information.
                .serviceUnder("/docs",
                        DocService.builder()
                                .exampleRequests(GreetingGrpc.SERVICE_NAME,
                                        "Hello", exampleRequest)
                                .exampleRequests(GreetingGrpc.SERVICE_NAME,
                                        "LazyHello", exampleRequest)
                                .exampleRequests(GreetingGrpc.SERVICE_NAME,
                                        "BlockingHello", exampleRequest)
                                .exclude(DocServiceFilter.ofServiceName(
                                        ServerReflectionGrpc.SERVICE_NAME))
                                .build());

        //  http
        sb.annotatedService(new Object() {
            @Get("/greeting")
            public HttpResponse greet(@Param(value = "name") @Default(value = "armeria") String name) {
                return HttpResponse.of(String.format("Good to meet you %s!", name));
            }
        });
    }
}
