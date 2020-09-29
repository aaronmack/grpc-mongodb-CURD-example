package com.example.client;

import com.example.server.WeblogServer;
import com.proto.weblog.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.logging.Logger;

public class WeblogClient {
    private static final Logger logger = Logger.getLogger(WeblogClient.class.getName());

    public static void main(String[] args) {
        logger.info("********** Weblog Client is running <-----");
        WeblogClient client = new WeblogClient();

        client.run();
    }

    private void run() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        logger.info("********** Connected to Channel");

        // Create sync Stub
        WeblogServiceGrpc.WeblogServiceBlockingStub blogServiceBlockingStub = WeblogServiceGrpc.newBlockingStub(channel);

        // Create new Weblog
        Weblog blog = Weblog.newBuilder()
                .setTitle("Weblog 2")
                .setContent("this is second weblog")
                .setAuthorId("Mack")
                .build();

        CreateWeblogResponse createWeblogResponse = blogServiceBlockingStub.createWeblog(CreateWeblogRequest.newBuilder()
                .setWeblog(blog)
                .build());

        logger.info("********** Received create Weblog Response from Server: " + createWeblogResponse.toString());

        // Create new Weblog to update
        Weblog newWeblog = Weblog.newBuilder()
                .setId(createWeblogResponse.getWeblog().getId())
                .setTitle("Weblog 3")
                .setContent("this is third weblog")
                .setAuthorId("Jams")
                .build();

        logger.info("********** Updating a blog");
        UpdateWeblogResponse updateWeblogResponse =
                blogServiceBlockingStub.updateWeblog(UpdateWeblogRequest.newBuilder()
                        .setWeblog(newWeblog)
                        .build());

        logger.info("********** Updated Received blog: " + updateWeblogResponse.toString());


        // Delete the updated Weblog
        logger.info("********** Deleting Weblog");
        DeleteWeblogResponse deleteWeblogResponse =
                blogServiceBlockingStub.deleteWeblog(DeleteWeblogRequest.newBuilder()
                        .setWeblogId(updateWeblogResponse.getWeblog().getId())
                        .build());

        logger.info("********** Deleted blog: " + deleteWeblogResponse.getWeblogId());


        logger.info("********** Requesting List Weblog");
        blogServiceBlockingStub.listWeblog(ListWeblogRequest.newBuilder()
                .build())
                .forEachRemaining(listWeblogResponse -> logger.info("********** List Weblog: " + listWeblogResponse.getWeblog().toString())
                );


        // Read a Weblog
        String read_id = "5f734a20e2e9c203f825364a";

        logger.info("********** Read a weblog with id: " + read_id);
        ReadWeblogResponse readWeblogResponse =
                blogServiceBlockingStub.readWeblog(ReadWeblogRequest.newBuilder()
                .setId(read_id)
                .build()
                );
        logger.info("********** Read a weblog: "+ readWeblogResponse.toString());

        channel.shutdown();
        logger.info("********** End Requesting List Weblog");
    }
}
