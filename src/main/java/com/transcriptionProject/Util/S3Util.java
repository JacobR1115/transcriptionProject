package com.transcriptionProject.Util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.services.transcribe.TranscribeClient;
import software.amazon.awssdk.services.transcribe.model.Media;
import software.amazon.awssdk.services.transcribe.model.StartTranscriptionJobRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class S3Util {
    private static final String INPUTBUCKET = "jacob-s3-file-upload-bucket";

    private static final String OUTPUTBUCKET = "jacob-s3-file-output-bucket";

    private static S3Client client = S3Client.builder().build();

    public static void uploadFile(String fileName, InputStream inputStream)
            throws S3Exception, AwsServiceException, SdkClientException, IOException {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(INPUTBUCKET)
                .key(fileName)
                .build();

        client.putObject(request,
                RequestBody.fromInputStream(inputStream, inputStream.available()));

        S3Waiter waiter = client.waiter();
        HeadObjectRequest waitRequest = HeadObjectRequest.builder()
                .bucket(INPUTBUCKET)
                .key(fileName)
                .build();
        waiter.waitUntilObjectExists(waitRequest);
    }

    public static void transcribeObject(String fileName)
            throws S3Exception, AwsServiceException, SdkClientException, IOException {

        TranscribeClient client = TranscribeClient.builder().build();

        Media media = Media.builder()
                .mediaFileUri("s3://"+ INPUTBUCKET + "/" + fileName)
                .build();

        StartTranscriptionJobRequest request = StartTranscriptionJobRequest.builder()
                .transcriptionJobName(fileName + "-job")
                .media(media)
                .outputBucketName(OUTPUTBUCKET)
                .identifyLanguage(true)
                .build();

        client.startTranscriptionJob(request);

    }

    public static String getTranscript(String fileName)
            throws S3Exception, AwsServiceException, SdkClientException, IOException {

        String newFileName = fileName + "-job.json";

        S3Waiter waiter = client.waiter();
        HeadObjectRequest waitRequest = HeadObjectRequest.builder()
                .bucket(OUTPUTBUCKET)
                .key(newFileName)
                .build();
        waiter.waitUntilObjectExists(waitRequest);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(OUTPUTBUCKET)
                .key(newFileName)
                .build();

        ResponseInputStream<GetObjectResponse> inputStream = client.getObject(request);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(json).get("results").get("transcripts").get(0).get("transcript");

        return node.asText();
    }
}
