package edu.stanford.slac.elog_plus.service.v0;

import edu.stanford.slac.elog_plus.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Path;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
public class StorageService {
    final private MinioClient objectStorageClient;
    final private MinioProperties objectStorageProperties;

    public void getFileObject(String filePath, GetFileResult fs) {
        assertion(() -> filePath != null,
                -1,
                "Filepath is mandatory",
                "StorageService::getFileObject");
        assertion(() -> !filePath.isEmpty(),
                -1,
                "The filepath cannot be ean empty string",
                "StorageService::getFileObject");
        Path p = Path.of(filePath);
        GetObjectResponse objectResponse =
                wrapCatch(
                        () -> objectStorageClient.getObject(
                                GetObjectArgs.builder()
                                        .bucket(objectStorageProperties.getBucket())
                                        .object(filePath)
                                        .build()
                        ),
                        -3,
                        ""
                );
        fs.setIs(objectResponse);
        fs.setFileName(p.getFileName().toString());
        fs.setContentType(objectResponse.headers().get("Content-Type"));
    }


    @Getter
    @Setter
    public static class GetFileResult {
        InputStream is;
        String fileName;
        String contentType;
    }
}
