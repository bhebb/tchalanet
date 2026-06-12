package com.tchalanet.server.platform.archive.internal.config;

import com.tchalanet.server.platform.archive.internal.storage.ArchiveStoragePort;
import com.tchalanet.server.platform.archive.internal.storage.LocalFileArchiveStorageAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ArchiveProperties.class)
public class ArchiveStorageConfig {

  @Bean
  @ConditionalOnProperty(name = "tch.archive.storage.type", havingValue = "local", matchIfMissing = true)
  public ArchiveStoragePort localArchiveStorage(ArchiveProperties props) {
    return new LocalFileArchiveStorageAdapter(props);
  }

  // S3-compatible bean can be added here behind @ConditionalOnProperty(havingValue = "s3")
  // when software.amazon.awssdk:s3 dependency is added to pom.xml.
}
