package com.transcriptionProject.Util;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.services.transcribe.TranscribeClient;
import software.amazon.awssdk.services.transcribe.model.Media;
import software.amazon.awssdk.services.transcribe.model.StartTranscriptionJobRequest;

import java.io.IOException;
import java.io.InputStream;

public class S3Util {
    private static final String INPUTBUCKET = "jacob-s3-file-upload-bucket";

    private static final String OUTPUTBUCKET = "jacob-s3-file-output-bucket";

    public static void uploadFile(String fileName, InputStream inputStream)
            throws S3Exception, AwsServiceException, SdkClientException, IOException {
        S3Client client = S3Client.builder().build();

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

        WaiterResponse<HeadObjectResponse> waitResponse = waiter.waitUntilObjectExists(waitRequest);
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
}
