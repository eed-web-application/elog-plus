package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.config.StorageProperties;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.model.ObjectListResult;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;


/**
 * Repository for the management of the storage
 */
@Repository
@AllArgsConstructor
public class StorageRepository {
    final private S3Client s3Client;
    final private StorageProperties objectStorageProperties;

    public void uploadFile(String id, FileObjectDescription attachment) throws IOException {
        PutObjectResponse putObjectResponse = null;
        assertion(() -> attachment.getContentType() != null,
                -1,
                "The Content type is mandatory",
                "AttachmentRepository::uploadFile");
        assertion(() -> attachment.getFileName() != null,
                -1,
                "The filename type is mandatory",
                "AttachmentRepository::uploadFile");
        assertion(() -> attachment.getIs() != null,
                -1,
                "The input stream is mandatory type is mandatory",
                "AttachmentRepository::uploadFile");

        putObjectResponse = s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(objectStorageProperties.getBucket())
                        .key("attachment/%s".formatted(id))
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
                                        .key("attachment/%s".formatted(id))
                                        .build(),
                                ResponseTransformer.toInputStream()
                        ),
                        -1,
                        "AttachmentRepository::getFile"
                );
        objDesc.setIs(objectResponse);
        objDesc.setContentType(objectResponse.response().contentType());
    }

    /**
     * Cycle on all storage file giving the maximum number of element ad using a continuation token
     *
     * @param maxKeysPerPage    the maximum number of elements
     * @param continuationToken the token returned in the last call if the new call is a continuation
     * @return the list of found key
     */
    public ObjectListResult listFilesInBucket(int maxKeysPerPage, String continuationToken) {
        List<String> foundKeys = new ArrayList<>();
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(objectStorageProperties.getBucket())
                .maxKeys(maxKeysPerPage)
                .prefix("attachment/")
                .continuationToken(continuationToken)
                .build();

        ListObjectsV2Response result = s3Client.listObjectsV2(request);

        result.contents().forEach(content -> foundKeys.add(content.key()));

        return ObjectListResult
                .builder()
                .continuationToken(result.nextContinuationToken())
                .keyFounds(foundKeys)
                .build();
    }
}

