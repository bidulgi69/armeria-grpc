# armeria-grpc
A simple gRPC &amp; REST communication example with <a href="https://github.com/line/armeria">Armeria</a>

## Usage 
1. Build `.proto` file first.
    
        ./gradlew :grpc-lib:build
        
    <img src="https://user-images.githubusercontent.com/17774927/170542024-26954acd-12d7-461e-b47d-103c5f9aa051.png">
    <br>If the build is successful, you can see that the java file(GreetingGrpc) has been created as above.<br>
          
2. Build & Run the `armeria-server` or `spring-armeria-server`.
<br>


## API
Armeria automatically generates an API document for testing.<br>
After launching the server, you can access it through the link below.
  
    http://localhost:8080/docs
    
## References
- <a href="https://github.com/line/armeria-examples">https://github.com/line/armeria-examples</a>
- <a href="https://tzara.tistory.com/128">https://tzara.tistory.com/128</a>
