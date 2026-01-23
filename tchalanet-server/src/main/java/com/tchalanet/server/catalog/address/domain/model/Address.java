package com.tchalanet.server.catalog.address.domain.model;

import java.util.UUID;

public final class Address {
  private final UUID id;
  private final String line1;
  private final String line2;
  private final String city;
  private final String region;
  private final String country;
  private final String postalCode;
  private final Double latitude;
  private final Double longitude;

  public Address(
      UUID id,
      String line1,
      String line2,
      String city,
      String region,
      String country,
      String postalCode,
      Double latitude,
      Double longitude) {
    this.id = id;
    this.line1 = line1;
    this.line2 = line2;
    this.city = city;
    this.region = region;
    this.country = country;
    this.postalCode = postalCode;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public UUID id() {
    return id;
  }

  public String line1() {
    return line1;
  }

  public String line2() {
    return line2;
  }

  public String city() {
    return city;
  }

  public String region() {
    return region;
  }

  public String country() {
    return country;
  }

  public String postalCode() {
    return postalCode;
  }

  public Double latitude() {
    return latitude;
  }

  public Double longitude() {
    return longitude;
  }
}
