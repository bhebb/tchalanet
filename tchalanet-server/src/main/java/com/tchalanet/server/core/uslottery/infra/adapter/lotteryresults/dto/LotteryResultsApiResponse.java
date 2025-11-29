package com.tchalanet.server.core.uslottery.infra.adapter.lotteryresults.dto;

import java.time.LocalDate;
import java.util.List;

public class LotteryResultsApiResponse {
  private String lottery;
  private LocalDate date;
  private List<String> numbers;
  private Object raw;

  public LotteryResultsApiResponse() {}

  public String getLottery() {
    return lottery;
  }

  public void setLottery(String lottery) {
    this.lottery = lottery;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public List<String> getNumbers() {
    return numbers;
  }

  public void setNumbers(List<String> numbers) {
    this.numbers = numbers;
  }

  public Object getRaw() {
    return raw;
  }

  public void setRaw(Object raw) {
    this.raw = raw;
  }
}
