package com.tchalanet.server.common.web.paging;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;

@Component
public class TchPagingArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(TchPaging.class)
        && parameter.getParameterType().equals(TchPageRequest.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    TchPaging cfg = parameter.getParameterAnnotation(TchPaging.class);
    HttpServletRequest req = webRequest.getNativeRequest(HttpServletRequest.class);

    // protect against static-analysis warnings if annotation is unexpectedly null
    int defaultPage = cfg == null ? 0 : cfg.defaultPage();
    int defaultSize = cfg == null ? 20 : cfg.defaultSize();
    int maxSize = cfg == null ? 100 : cfg.maxSize();
    Sort.Direction defaultDir = cfg == null ? Sort.Direction.DESC : cfg.defaultDirection();

    int page =
        parseInt(req == null ? null : req.getParameter("page"), defaultPage, 0, Integer.MAX_VALUE);
    int size = parseInt(req == null ? null : req.getParameter("size"), defaultSize, 1, maxSize);

    // sort can be repeated: sort=field,asc&sort=other,desc
    List<String> sortParams = listParams(req, "sort");
    Sort incoming = parseSort(sortParams, defaultDir);

    Set<String> allowed =
        new HashSet<>(Arrays.asList(cfg == null ? new String[0] : cfg.allowedSort()));
    Sort fallback = parseDefaultSort(cfg == null ? new String[0] : cfg.defaultSort(), defaultDir);

    Sort finalSort = chooseSort(incoming, fallback, allowed);
    Pageable pageable = PageRequest.of(page, size, finalSort);

    return new TchPageRequest(pageable);
  }

  private static int parseInt(String raw, int def, int min, int max) {
    try {
      int v = raw == null ? def : Integer.parseInt(raw);
      if (v < min) return min;
      if (v > max) return max;
      return v;
    } catch (Exception e) {
      return def;
    }
  }

  private static List<String> listParams(HttpServletRequest req, String name) {
    if (req == null) return List.of();
    String[] arr = req.getParameterValues(name);
    if (arr == null || arr.length == 0) return List.of();
    return Arrays.stream(arr).filter(StringUtils::hasText).toList();
  }

  private static Sort parseSort(List<String> params, Sort.Direction defaultDir) {
    if (params == null || params.isEmpty()) return Sort.unsorted();

    Sort out = Sort.unsorted();
    for (String p : params) {
      // expected: "field,asc" OR "field,desc" OR "field"
      String[] parts = p.split(",", 2);
      String field = parts[0].trim();
      if (!StringUtils.hasText(field)) continue;

      Sort.Direction dir = defaultDir;
      if (parts.length == 2) {
        String d = parts[1].trim().toUpperCase(Locale.ROOT);
        if ("ASC".equals(d)) dir = Sort.Direction.ASC;
        else if ("DESC".equals(d)) dir = Sort.Direction.DESC;
      }
      out = out.and(Sort.by(new Sort.Order(dir, field)));
    }
    return out;
  }

  private static Sort parseDefaultSort(String[] defaultSort, Sort.Direction defaultDir) {
    if (defaultSort == null || defaultSort.length == 0) return Sort.unsorted();

    Sort out = Sort.unsorted();
    for (String s : defaultSort) {
      if (!StringUtils.hasText(s)) continue;
      String[] parts = s.split(",", 2);
      String field = parts[0].trim();
      if (!StringUtils.hasText(field)) continue;

      Sort.Direction dir = defaultDir;
      if (parts.length == 2) {
        String d = parts[1].trim().toUpperCase(Locale.ROOT);
        if ("ASC".equals(d)) dir = Sort.Direction.ASC;
        else if ("DESC".equals(d)) dir = Sort.Direction.DESC;
      }
      out = out.and(Sort.by(new Sort.Order(dir, field)));
    }
    return out;
  }

  private static Sort chooseSort(Sort incoming, Sort fallback, Set<String> allowed) {
    Sort a = sanitize(incoming, allowed);
    if (!a.isUnsorted()) return a;

    Sort b = sanitize(fallback, allowed);
    return b.isUnsorted() ? Sort.unsorted() : b;
  }

  private static Sort sanitize(Sort sort, Set<String> allowed) {
    if (sort == null || sort.isUnsorted()) return Sort.unsorted();
    if (allowed == null || allowed.isEmpty()) return Sort.unsorted();

    Sort out = Sort.unsorted();
    for (Sort.Order o : sort) {
      if (allowed.contains(o.getProperty())) {
        out = out.and(Sort.by(new Sort.Order(o.getDirection(), o.getProperty())));
      }
    }
    return out;
  }
}
