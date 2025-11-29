package com.tchalanet.server.features.news.infra.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.features.news.domain.model.NewsArticle;
import com.tchalanet.server.features.news.domain.ports.out.NewsProviderPort;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * HTTP adapter for NewsProviderPort using java.net.http.HttpClient (non-reactive). Supports JSON
 * feeds and XML (RSS/Atom). Normalizes items to Map<String,Object>.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NewsProviderHttpAdapter implements NewsProviderPort {

  private final ObjectMapper objectMapper;

  @Value("${news.provider.url:}")
  private String providerUrl;

  @Value("${news.provider.apiKey:}")
  private String apiKey;

  // max retries for HTTP request
  @Value("${news.provider.maxRetries:1}")
  private int maxRetries;

  @Value("${news.max.items:10}")
  private int maxItemsValue;

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  @Override
  public List<NewsArticle> fetchLatestNews() { // Changed return type to List<NewsArticle>
    if (providerUrl == null || providerUrl.isBlank()) {
      log.warn(
          "News provider URL is not configured (property 'news.provider.url'). Returning empty list.");
      return List.of();
    }

    int attempt = 0;
    Exception lastEx = null;
    while (++attempt <= Math.max(1, maxRetries)) {
      try {
        if (attempt > 1) log.info("Retrying news fetch (attempt {}/{})", attempt, maxRetries);

        var client = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
        var reqBuilder =
            HttpRequest.newBuilder(URI.create(providerUrl))
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json, application/xml, text/xml, */*");
        if (apiKey != null && !apiKey.isBlank()) reqBuilder.header("Authorization", apiKey);

        var req = reqBuilder.build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        String raw = Optional.ofNullable(resp.body()).orElse("");
        if (raw.isBlank()) {
          log.info(
              "News provider {} returned empty response (status={})",
              providerUrl,
              resp.statusCode());
          return List.of();
        }

        // Detect XML (RSS/Atom) by content
        String trimmed = raw.trim();
        if (trimmed.startsWith("<?xml")
            || trimmed.startsWith("<rss")
            || trimmed.startsWith("<feed")
            || trimmed.contains("<rss")
            || trimmed.contains("<feed")) {
          return parseXmlFeed(raw, maxItemsValue);
        }

        // Try parse as JSON array/object
        try {
          // Directly map to List<NewsArticle>
          return objectMapper.readValue(raw, new TypeReference<List<NewsArticle>>() {});
        } catch (Exception ex) {
          log.debug(
              "Response is not a direct JSON array, attempting to parse as JSON object: {}",
              ex.getMessage());
        }

        JsonNode tree = objectMapper.readTree(raw);
        if (tree.isArray()) {
          return objectMapper.convertValue(tree, new TypeReference<List<NewsArticle>>() {});
        }

        for (String candidate : new String[] {"items", "data", "results", "articles", "news"}) {
          JsonNode node = tree.get(candidate);
          if (node != null && node.isArray()) {
            return objectMapper.convertValue(node, new TypeReference<List<NewsArticle>>() {});
          }
        }

        if (tree.isObject()) {
          NewsArticle single = objectMapper.convertValue(tree, NewsArticle.class);
          return List.of(single);
        }

        log.warn("Unable to map news payload from provider {} to expected structure", providerUrl);
        return List.of();

      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        lastEx = ie;
        log.error("News fetch interrupted for {}", providerUrl, ie);
        break; // interruption is fatal
      } catch (Exception e) {
        lastEx = e;
        log.warn(
            "Attempt {}/{} failed to fetch news from {}: {}",
            attempt,
            maxRetries,
            providerUrl,
            e.getMessage());
        if (attempt < Math.max(1, maxRetries)) {
          try {
            long backoffMs = 500L * (1L << (attempt - 1)); // exponential: 500,1000,2000...
            Thread.sleep(backoffMs);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
          continue; // retry
        }
        log.error("All {} attempts failed to fetch news from {}", maxRetries, providerUrl, e);
        return List.of();
      }
    }

    if (lastEx != null)
      log.error(
          "Failed to fetch news from {} after {} attempts: {}",
          providerUrl,
          maxRetries,
          lastEx.getMessage(),
          lastEx);
    return List.of();
  }

  private List<NewsArticle> parseXmlFeed(String xml, int maxItems) { // Changed return type
    var out = new ArrayList<NewsArticle>();
    try {
      var dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      var builder = dbf.newDocumentBuilder();
      Document doc = builder.parse(new InputSource(new StringReader(xml)));

      // RSS: channel/item, Atom: entry
      NodeList items = doc.getElementsByTagName("item");
      if (items == null || items.getLength() == 0) {
        items = doc.getElementsByTagName("entry");
      }

      for (int i = 0; i < items.getLength() && out.size() < maxItems; i++) {
        Node node = items.item(i);
        if (node.getNodeType() != Node.ELEMENT_NODE) continue;
        Element el = (Element) node;

        String title = textContentOf(el, "title");
        String link = textContentOf(el, "link");
        if ((link == null || link.isBlank())) {
          // RSS often has <link> as text; Atom often has <link rel="alternate" href="..."/>
          var linkNode = el.getElementsByTagName("link");
          if (linkNode != null && linkNode.getLength() > 0) {
            Element l = (Element) linkNode.item(0);
            if (l.hasAttribute("href")) link = l.getAttribute("href");
            else link = l.getTextContent();
          }
        }

        String pub = textContentOf(el, "pubDate");
        if (pub == null) pub = textContentOf(el, "published");
        if (pub == null) pub = textContentOf(el, "updated");

        String desc = textContentOf(el, "description");
        if (desc == null) desc = textContentOf(el, "summary");
        if (desc == null) desc = textContentOf(el, "content");

        // Try to extract content:encoded (HTML) if present
        String contentHtml = null;
        NodeList contentEncoded = el.getElementsByTagName("content:encoded");
        if (contentEncoded != null && contentEncoded.getLength() > 0) {
          contentHtml = contentEncoded.item(0).getTextContent().trim();
        } else {
          // try namespace-aware lookup
          NodeList encodedNs =
              el.getElementsByTagNameNS("http://purl.org/rss/1.0/modules/content/", "encoded");
          if (encodedNs != null && encodedNs.getLength() > 0) {
            contentHtml = encodedNs.item(0).getTextContent().trim();
          }
        }

        String image = null;
        // enclosure with url attr
        NodeList enclosures = el.getElementsByTagName("enclosure");
        if (enclosures != null && enclosures.getLength() > 0) {
          Element enc = (Element) enclosures.item(0);
          if (enc.hasAttribute("url")) image = enc.getAttribute("url");
        }
        // media:thumbnail or media:content
        if (image == null) {
          NodeList thumbs = el.getElementsByTagNameNS("http://search.yahoo.com/mrss/", "thumbnail");
          if (thumbs != null && thumbs.getLength() > 0) {
            Element t = (Element) thumbs.item(0);
            if (t.hasAttribute("url")) image = t.getAttribute("url");
          }
        }

        // Map<String, Object> item = new HashMap<>(); // No longer needed
        // item.put("title", title);
        // item.put("link", link);
        // item.put("published_at", normalizeDate(pub));
        // item.put("summary", desc);
        // item.put("image", image);
        // if (contentHtml != null && !contentHtml.isBlank()) item.put("content_html", contentHtml);
        // out.add(item);
        out.add(
            new NewsArticle(
                UUID.randomUUID().toString(),
                title,
                contentHtml != null ? contentHtml : desc,
                link)); // Create NewsArticle
      }

      return out;
    } catch (Exception e) {
      log.warn("Failed to parse XML feed: {}", e.getMessage(), e);
      return List.of();
    }
  }

  private String textContentOf(Element el, String tag) {
    NodeList nl = el.getElementsByTagName(tag);
    if (nl != null && nl.getLength() > 0) return nl.item(0).getTextContent().trim();
    // case-insensitive search
    for (int i = 0; i < el.getChildNodes().getLength(); i++) {
      Node n = el.getChildNodes().item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equalsIgnoreCase(tag))
        return n.getTextContent().trim();
    }
    return null;
  }

  private String normalizeDate(String raw) {
    if (raw == null) return null;
    try {
      // try RFC_1123 (e.g. "Thu, 20 Nov 2025 12:52:22 +0000") used by many RSS feeds
      try {
        var odt =
            java.time.ZonedDateTime.parse(
                raw, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);
        return odt.toOffsetDateTime().toString();
      } catch (Exception ignored) {
      }
      // fallback to ISO offset
      return java.time.OffsetDateTime.parse(raw).toString();
    } catch (DateTimeParseException e) {
      try {
        return java.time.OffsetDateTime.parse(raw + "Z").toString();
      } catch (Exception ex) {
        return raw;
      }
    }
  }
}
