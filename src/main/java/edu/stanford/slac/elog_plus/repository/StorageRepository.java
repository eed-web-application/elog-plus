package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.config.StorageProperties;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

/**
 * Repository for the management of the storage
 */
@Repository
@AllArgsConstructor
public class StorageRepository {
    final private S3Client s3Client;
    final private StorageProperties objectStorageProperties;

    public void uploadFile(String id, FileObjectDescription attachment) throws IOException {
        assertion(() -> attachment.getContentType()!=null,
                -1,
                "The Content type is mandatory",
                "AttachmentRepository::uploadFile");
        assertion(() -> attachment.getFileName()!=null,
                -1,
                "The filename type is mandatory",
                "AttachmentRepository::uploadFile");
        assertion(() -> attachment.getIs()!=null,
                -1,
                "The input stream is mandatory type is mandatory",
                "AttachmentRepository::uploadFile");

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(objectStorageProperties.getBucket())
                        .key(id)
                        .contentType(attachment.getContentType())
                        .build(),
                RequestBody.fromInputStream(
                        attachment.getIs(),
                        attachment.getIs().available()
                )
        );
    }

    public void getFile(String id, FileObjectDescription objDesc) {
        assertion(() -> !id.isBlank() && !id.isEmpty(),
                -1,
                "The attachment id is invalid",
                "AttachmentRepository::getFileObject");

        ResponseInputStream<GetObjectResponse> objectResponse =
                wrapCatch(
                        () -> s3Client.getObject(
                                GetObjectRequest.builder()
                                        .bucket(objectStorageProperties.getBucket())
                                        .key(id)
                                        .build(),
                                ResponseTransformer.toInputStream()
                        ),
                        -1,
                        "AttachmentRepository::getFile"
                );
        objDesc.setIs(objectResponse);
        objDesc.setContentType(objectResponse.response().contentType());
    }
}
