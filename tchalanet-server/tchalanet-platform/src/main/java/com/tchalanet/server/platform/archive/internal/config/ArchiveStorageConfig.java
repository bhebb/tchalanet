package com.tchalanet.server.platform.archive.internal.config;

import com.tchalanet.server.platform.archive.internal.storage.ArchiveStoragePort;
import com.tchalanet.server.platform.archive.internal.storage.LocalFileArchiveStorageAdapter;
import com.tchalanet.server.platform.archive.internal.storage.S3ArchiveStorageAdapter;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(ArchiveProperties.class)
public class ArchiveStorageConfig {

  @Bean
  @ConditionalOnProperty(
      name = "tch.archive.storage.type",
      havingValue = "local",
      matchIfMissing = true)
  public ArchiveStoragePort localArchiveStorage(ArchiveProperties props) {
    return new LocalFileArchiveStorageAdapter(props);
  }

  /**
   * S3-compatible storage — Cloudflare R2 in production.
   *
   * <p>Path-style access is required: R2 does not serve virtual-host style buckets.
   */
  @Bean
  @ConditionalOnProperty(name = "tch.archive.storage.type", havingValue = "s3")
  public ArchiveStoragePort s3ArchiveStorage(ArchiveProperties props) {
    var storage = props.storage();
    if (storage.endpoint() == null || storage.endpoint().isBlank()) {
      throw new IllegalStateException(
          "tch.archive.storage.endpoint is required when storage type is s3");
    }
    if (storage.accessKeyId() == null || storage.secretAccessKey() == null) {
      throw new IllegalStateException(
          "tch.archive.storage.access-key-id and secret-access-key are required for s3 storage");
    }
    var client =
        S3Client.builder()
            .endpointOverride(URI.create(storage.endpoint()))
            .region(Region.of(storage.region()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(storage.accessKeyId(), storage.secretAccessKey())))
            .forcePathStyle(true)
            .build();
    return new S3ArchiveStorageAdapter(props, client);
  }
}
