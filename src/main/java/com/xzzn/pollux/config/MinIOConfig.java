package com.xzzn.pollux.config;

import com.alibaba.fastjson.JSON;
import com.google.common.io.Resources;
import com.xzzn.pollux.common.enums.S3PolicyEnum;
import com.xzzn.pollux.model.pojo.S3BucketPolicy;
import com.xzzn.pollux.model.pojo.S3Statement;
import io.minio.*;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Configuration
public class MinIOConfig
{
    @Value("${s3.config.access-key}")
    private String accessKey;

    @Value("${s3.config.secret-key}")
    private String secretKey;

    @Value("${s3.config.endpoint}")
    private String endpoint;

    @Value("${s3.config.bucket}")
    private String bucket;

    Set<String> writePolicySet;
    Set<String> readPolicySet;
    private static final String TEMPLATE_PATH = "s3-bucket-policy-template.json";

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        init(client);
        return client;
    }

    private void init(MinioClient client) {
        try {

            boolean found = client.bucketExists(
                    BucketExistsArgs.builder()
                                    .bucket(bucket)
                                    .build()
            );

            if (found) {
                log.info("Bucket {} already exists", bucket);
            } else {
                log.info("Bucket {} not exists, start to create it", bucket);
                client.makeBucket(MakeBucketArgs.builder()
                                                .bucket(bucket)
                                                .build()
                );
                log.info("Bucket {} already gets created", bucket);
            }

            String policy = client.getBucketPolicy(GetBucketPolicyArgs.builder().bucket(bucket).build());

            if (policy.isEmpty() || !verifyPolicy(policy)) {
                String strConfig = loadPolicyFromFile(bucket, TEMPLATE_PATH);
                client.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucket)
                                .config(strConfig)
                                .build()
                );
                log.info("Bucket {} policy has been updated", bucket);
            }


        } catch (MinioException e) {
            log.warn("Error occurs when init minio client", e);

        } catch (Exception e) {
            log.warn("Unknown error occurs when init minio client", e);
        }
    }

    private void initPolicySet() {
        if (readPolicySet == null) {
            readPolicySet = new HashSet<>();
            readPolicySet.add(S3PolicyEnum.S3_POLICY_GET_BUCKET_LOCATION.getValue());
            readPolicySet.add(S3PolicyEnum.S3_POLICY_LIST_BUCKET.getValue());
            readPolicySet.add(S3PolicyEnum.S3_POLICY_LIST_BUCKET_MULTIPART_UPLOADS.getValue());
        }
        if (writePolicySet == null) {
            writePolicySet = new HashSet<>();
            writePolicySet.add(S3PolicyEnum.S3_POLICY_ABORT_MULTIPART_UPLOAD.getValue());
            writePolicySet.add(S3PolicyEnum.S3_POLICY_DELETE_OBJECT.getValue());
            writePolicySet.add(S3PolicyEnum.S3_POLICY_GET_OBJECT.getValue());
            writePolicySet.add(S3PolicyEnum.S3_POLICY_LIST_MULTIPART_UPLOAD_PARTS.getValue());
            writePolicySet.add(S3PolicyEnum.S3_POLICY_PUT_OBJECT.getValue());
        }
    }

    private boolean verifyPolicy(String strConfig) {
        initPolicySet();
        S3BucketPolicy bucketPolicy = JSON.parseObject(strConfig, S3BucketPolicy.class);

        if (bucketPolicy.getStatement().size() != 2) {
            return false;
        }

        // verify policy set for write and read
        for (S3Statement statement : bucketPolicy.getStatement()) {
            if (statement.getResource().size() != 1) {
                return false;
            }
            String resource =  statement.getResource().get(0);
            Set<String> action = new HashSet<>(statement.getAction());
            if (resource.substring(13).equals(bucket)) {
                for (String key : readPolicySet) {
                    if (!action.contains(key)) {
                        return false;
                    }
                }
            } else if (resource.substring(13).equals(bucket + "/*")) {
                for (String key : writePolicySet) {
                    if (!action.contains(key)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }

        return true;
    }

    private String loadPolicyFromFile(String bucket, String templatePath) throws IOException {
        log.info("Start  to load {} from resource", templatePath);
        try {
            String fileString = Resources.toString(Resources.getResource(templatePath), StandardCharsets.UTF_8);
            log.info("Succeed  to load {} from resource", templatePath);
            return fileString.replace("{{bucket}}", bucket);
        } catch (IOException e) {
            log.error("Unable to load file {} from resource, invalid file path", templatePath, e);
            throw e;
        }

    }

}
