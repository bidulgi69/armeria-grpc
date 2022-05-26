import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.annotation.Default;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.Param;
import kr.dove.lib.GreetingGrpc;
import kr.dove.lib.HelloRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.server.HttpServiceWithRoutes;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.docs.DocServiceFilter;
import com.linecorp.armeria.server.grpc.GrpcService;

import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;

public final class Launcher {
    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        try (Server server = launch(8080, 8443)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop().join();
                logger.info("Server has been stopped.");
            }));

            server.start().join();

            logger.info("Server has been started. Serving DocService at http://127.0.0.1:{}/docs",
                    server.activeLocalPort());
        } catch (RuntimeException ignored) {

        }
    }

    private static Server launch(int httpPort, int httpsPort) {
        final ServerBuilder sb = Server.builder();
        sb.http(httpPort)
                .https(httpsPort)
                .tlsSelfSigned();
        configureServices(sb);
        return sb.build();
    }

    static void configureServices(ServerBuilder sb) {
        final HelloRequest exampleRequest = HelloRequest
                .newBuilder()
                .setName("Armeria")
                .setAge(7)
                .setAsia(HelloRequest.Asia.Korea)
                .build();
        final HttpServiceWithRoutes grpcService =
                GrpcService.builder()
                        .addService(new GreetingServiceImpl())
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

        sb.annotatedService(new Object() {
            @Get("/greeting")
            public HttpResponse greet(@Param(value = "name") @Default(value = "armeria") String name) {
                return HttpResponse.of(String.format("Good to meet you %s!", name));
            }
        });
    }

    private Launcher() {}
}
