package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.config.MinioProperties;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import io.minio.*;
import io.minio.errors.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

/**
 * Repository for the management of the storage
 */
@Repository
@AllArgsConstructor
public class StorageRepository {
    final private MinioClient objectStorageClient;
    final private MinioProperties objectStorageProperties;

    public void uploadFile(String id, FileObjectDescription attachment) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
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

        ObjectWriteResponse owr = objectStorageClient.putObject(
                PutObjectArgs.builder()
                        .bucket(objectStorageProperties.getBucket())
                        .object(id)
                        .stream(attachment.getIs(), -1, 1024*1000*1000)
                        .contentType(attachment.getContentType())
                        .build()
        );
    }

    public void getFile(String id, FileObjectDescription objDesc) {
        assertion(() -> !id.isBlank() && !id.isEmpty(),
                -1,
                "The attachment id is invalid",
                "AttachmentRepository::getFileObject");
        GetObjectResponse objectResponse =
                wrapCatch(
                        () -> objectStorageClient.getObject(
                                GetObjectArgs.builder()
                                        .bucket(objectStorageProperties.getBucket())
                                        .object(id)
                                        .build()
                        ),
                        -1,
                        "AttachmentRepository::getFile"
                );
        objDesc.setIs(objectResponse);
        objDesc.setContentType(objectResponse.headers().get("Content-Type"));
    }
}
