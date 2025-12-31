package com.tchalanet.server.features.news.publicnews;

import com.tchalanet.server.features.news.shared.LotteryNewsModels;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/news")
@RequiredArgsConstructor
public class PublicNewsController {

  private final PublicNewsService listPublicNewsUseCase;

  @GetMapping("/")
  @ResponseBody
  @ResponseStatus(code = HttpStatus.OK)
  public List<LotteryNewsModels.LotteryNewsArticle> listPublicNews() {
    return listPublicNewsUseCase.listAll();
  }
}
