import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import kr.dove.lib.GreetingGrpc;
import kr.dove.lib.HelloReply;
import kr.dove.lib.HelloRequest;

public class GreetingServiceImpl extends GreetingGrpc.GreetingImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        if (request.getName().isEmpty()) {
            responseObserver.onError(
                    Status.FAILED_PRECONDITION
                            .withDescription("Name field cannot be empty.")
                            .asException()
            );
        } else {
            responseObserver.onNext(
                    HelloReply
                            .newBuilder()
                            .setMessage(String.format(
                                    "Good to meet you! %s(%d) from %s",
                                    request.getName(),
                                    request.getAge(),
                                    request.getAsia())
                            )
                            .build()
            );
            responseObserver.onCompleted();
        }
    }
}
