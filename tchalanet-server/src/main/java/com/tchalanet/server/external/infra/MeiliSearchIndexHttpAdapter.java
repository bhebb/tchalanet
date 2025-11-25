package com.tchalanet.server.external.infra;

import com.tchalanet.server.external.ports.SearchIndexPort;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class MeiliSearchIndexHttpAdapter implements SearchIndexPort {

  private final RestTemplate rest;
  private final String base = "http://localhost:7700"; // property override later

  public MeiliSearchIndexHttpAdapter(RestTemplateBuilder b) {
    this.rest = b.build();
  }

  @Override
  public void index(String indexName, Map<String, Object> document) {
    String url = base + "/indexes/" + indexName + "/documents";
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> req = new HttpEntity<>(document, h);
    rest.postForEntity(url, req, Void.class);
  }

  @Override
  public void delete(String indexName, String id) {
    String url = base + "/indexes/" + indexName + "/documents/" + id;
    rest.delete(url);
  }
}
