package com.example.server;


import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.io.IOException;
import java.util.logging.Logger;

public class WeblogServer {
    private static final Logger logger = Logger.getLogger(WeblogServer.class.getName());

    public static void main(String[] args) throws InterruptedException, IOException {
        // Create Server
        Server server = ServerBuilder.forPort(50051)
                .addService(new WeblogServiceImpl())
                .addService(ProtoReflectionService.newInstance()) // added Reflection
                .build();

        // start
        server.start();
        logger.info("********** Server Started");

        // Shutdown: Using Runtime shutdown server [Imp: before await Termination]
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("********** Server Received Shutdown Request");
            server.shutdown();
            logger.info("********** Successfully, Stopped Shutdown the server");
        }));

        server.awaitTermination();
    }
}
