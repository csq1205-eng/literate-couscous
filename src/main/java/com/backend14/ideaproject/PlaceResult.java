package com.backend14.ideaproject;

public class PlaceResult {

  private final String name;
  private final String address;
  private final double latitude;
  private final double longitude;

  public PlaceResult(String name, String address, double latitude, double longitude) {
    this.name = name;
    this.address = address;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }
}