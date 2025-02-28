package com.xzzn.pollux.common.enums;

public enum S3PolicyEnum {
    S3_POLICY_GET_BUCKET_LOCATION("s3:GetBucketLocation"),
    S3_POLICY_LIST_BUCKET("s3:ListBucket"),
    S3_POLICY_LIST_BUCKET_MULTIPART_UPLOADS("s3:ListBucketMultipartUploads"),
    S3_POLICY_ABORT_MULTIPART_UPLOAD("s3:AbortMultipartUpload"),
    S3_POLICY_DELETE_OBJECT("s3:DeleteObject"),
    S3_POLICY_GET_OBJECT("s3:GetObject"),
    S3_POLICY_LIST_MULTIPART_UPLOAD_PARTS("s3:ListMultipartUploadParts"),
    S3_POLICY_PUT_OBJECT("s3:PutObject");

    private String value;
    S3PolicyEnum(String value) {
        this.value = value;
    }
    public String getValue() {
        return this.value;
    }

}
