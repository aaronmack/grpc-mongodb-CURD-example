package com.example.server;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.DB;
import com.mongodb.client.result.DeleteResult;

import com.proto.weblog.*;

import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import io.grpc.Status;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.logging.Logger;
import java.io.IOException;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class WeblogServiceImpl extends WeblogServiceGrpc.WeblogServiceImplBase{
    private MongoCollection<Document> mongoCollection;
    private static final Logger logger = Logger.getLogger(WeblogServiceImpl.class.getName());
    
    WeblogServiceImpl(){
        String host = "127.0.0.1";
        int port = 27017;
        String databaseName = "weblog";
        String username = "user";
        String password = "user";

        ServerAddress serverAddress = new ServerAddress(host,port);
        List<ServerAddress> addrs = new ArrayList<ServerAddress>();
        addrs.add(serverAddress);

        MongoCredential credential = MongoCredential.createScramSha1Credential(username, databaseName, password.toCharArray());
        List<MongoCredential> credentials = new ArrayList<MongoCredential>();
        credentials.add(credential);

        MongoClient mongoClient = new MongoClient(addrs,credentials);

        logger.info("********** New Mongo Client " + mongoClient.toString());

        MongoDatabase mongoDatabase = mongoClient.getDatabase("weblog");

        mongoCollection = mongoDatabase.getCollection("weblog");
    }

    @Override
    public void createWeblog(CreateWeblogRequest request, StreamObserver<CreateWeblogResponse> responseObserver) {

        logger.info("********** Received, Create Weblog Request");
        
        Weblog blog = request.getWeblog();

        Document document = new Document("authorId", blog.getAuthorId())
                .append("title", blog.getTitle())
                .append("content", blog.getContent()
                );

        logger.info("********** Inserting Weblog to MongoDB");

        mongoCollection.insertOne(document);

        String id = document.getObjectId("_id").toString();
        logger.info("********** Inserted Weblog Doc with id: " + id);

        // Create MongoResponse
        CreateWeblogResponse weblogResponse = CreateWeblogResponse.newBuilder()
                .setWeblog(
                        blog.toBuilder().setId(id).build()
                )
                .build();

        responseObserver.onNext(weblogResponse);

        responseObserver.onCompleted();
    }

    @Override
    public void readWeblog(ReadWeblogRequest request, StreamObserver<ReadWeblogResponse> responseObserver) {

        logger.info("Received Read Weblog Request");
        String blogId = request.getId();

        Document document = null;
        try {
            document = mongoCollection.find(eq("_id", new ObjectId(blogId)))
                    .first(); // from the LIST, fetch the first one
        } catch (Exception e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Weblog is not found for id:" + blogId)
                    .augmentDescription(e.getLocalizedMessage())
                    .asRuntimeException());
        }


        logger.info("Searching for doc");
        if (document == null) {
            logger.info("Weblog is not found");
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Weblog is not found for id:" + blogId)
                    .asRuntimeException()
            );

        } else {
            logger.info("Weblog is found");
            Weblog blog = documentToWeblog(document);

            responseObserver.onNext(
                    ReadWeblogResponse.newBuilder()
                            .setWeblog(blog)
                            .build());
            logger.info("Sent the Response");

            responseObserver.onCompleted();
            logger.info("Server done");
        }

    }

    private Weblog documentToWeblog(Document document) {
        return Weblog.newBuilder()
                .setTitle(document.getString("title"))
                .setAuthorId(document.getString("authorId"))
                .setId(document.getObjectId("_id").toString())
                .setContent(document.getString("content"))
                .build();

    }

    @Override
    public void updateWeblog(UpdateWeblogRequest request, StreamObserver<UpdateWeblogResponse> responseObserver) {
        logger.info("Received Update Weblog Request");

        Weblog blog = request.getWeblog();

        // get Weblog Id
        String blogId = blog.getId();

        Document document = null;
        logger.info("Searching for Weblog");
        try {

            document = mongoCollection.find(eq("_id", new ObjectId(blogId)))
                    .first();
        } catch (Exception e) {

            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Weblog to be updated is not found for id:" + blogId)
                    .augmentDescription(e.getLocalizedMessage())
                    .asRuntimeException());
        }

        if (document == null) {

            logger.info("Weblog is not found");
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Weblog is not found for id:" + blogId)
                    .asRuntimeException()
            );

        } else {

            Document replacementDoc = new Document("authorId", blog.getAuthorId())
                    .append("title", blog.getTitle())
                    .append("content", blog.getContent())
                    .append("_id", new ObjectId(blogId));

            logger.info("Replacing Weblog in dB..");
            // Replace replacementDoc with old doc
            mongoCollection.replaceOne(eq("_id", document.getObjectId("_id")), replacementDoc);

            logger.info("Replaced Weblog in DB");

            responseObserver.onNext(UpdateWeblogResponse.newBuilder()
                    .setWeblog(documentToWeblog(replacementDoc))
                    .build());

            logger.info("Server completed Update");
            responseObserver.onCompleted();
        }


    }

    @Override
    public void deleteWeblog(DeleteWeblogRequest request, StreamObserver<DeleteWeblogResponse> responseObserver) {

        String blogId = request.getWeblogId();

        logger.info("Recieved a request from client to delete a doc");

        DeleteResult result = null;
        try {
            result = mongoCollection.deleteOne(eq("_id", new ObjectId(blogId)));
        } catch (Exception e) {
            logger.info("Weblog delete failed");
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Weblog is not found for id:" + blogId)
                    .asRuntimeException()
            );
        }


        if (result.getDeletedCount() == 0) {
            // Nothing was deleted
            logger.info("Weblog delete failed: Maybe Weblog was not found");
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Weblog is not found for id:" + blogId)
                    .asRuntimeException()
            );
        } else {
            // Doc was deleted
            logger.info("Deleted a doc with id: " + blogId);

            responseObserver.onNext(DeleteWeblogResponse.newBuilder()
                    .setWeblogId(blogId)
                    .build());
            logger.info("Delete: Server has completed processing");
            responseObserver.onCompleted();
        }

    }

    @Override
    public void listWeblog(ListWeblogRequest request, StreamObserver<ListWeblogResponse> responseObserver) {

        logger.info("Received List Weblog Request");

        mongoCollection.find().iterator().forEachRemaining(document -> responseObserver.onNext(
                ListWeblogResponse.newBuilder()
                        .setWeblog(documentToWeblog(document))
                        .build()
        ));

        responseObserver.onCompleted();
    }

}
