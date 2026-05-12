package com.backend14.ideaproject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class KakaoPlaceService {

  @Value("${kakao.rest-api-key}")
  private String kakaoRestApiKey;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public PlaceResult searchPlace(String keyword) {
    List<PlaceResult> places = searchPlaces(keyword);

    if (places.isEmpty()) {
      return null;
    }

    return places.get(0);
  }

  public List<PlaceResult> searchPlaces(String keyword) {
    try {
      keyword = normalizeKeyword(keyword);

      if (keyword.isBlank()) {
        return new ArrayList<>();
      }

      String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

      String requestUrl =
          "https://dapi.kakao.com/v2/local/search/keyword.json?query="
              + encodedKeyword
              + "&size=5";

      URL url = new URL(requestUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      connection.setRequestMethod("GET");
      connection.setRequestProperty("Authorization", "KakaoAK " + kakaoRestApiKey);
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(10000);

      int statusCode = connection.getResponseCode();

      BufferedReader reader;

      if (statusCode >= 200 && statusCode < 300) {
        reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
        );
      } else {
        reader = new BufferedReader(
            new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8)
        );
      }

      StringBuilder response = new StringBuilder();
      String line;

      while ((line = reader.readLine()) != null) {
        response.append(line);
      }

      reader.close();
      connection.disconnect();

      if (statusCode < 200 || statusCode >= 300) {
        System.out.println("Kakao API 오류: " + response);
        return new ArrayList<>();
      }

      return parsePlaces(response.toString());

    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  private List<PlaceResult> parsePlaces(String json) {
    List<PlaceResult> result = new ArrayList<>();

    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode documents = root.get("documents");

      if (documents == null || !documents.isArray() || documents.isEmpty()) {
        return result;
      }

      for (JsonNode document : documents) {
        String placeName = document.path("place_name").asText();
        String addressName = document.path("address_name").asText();
        String x = document.path("x").asText(); // 경도
        String y = document.path("y").asText(); // 위도

        if (placeName.isBlank() || x.isBlank() || y.isBlank()) {
          continue;
        }

        double longitude = Double.parseDouble(x);
        double latitude = Double.parseDouble(y);

        result.add(new PlaceResult(placeName, addressName, latitude, longitude));
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  private String normalizeKeyword(String keyword) {
    if (keyword == null) {
      return "";
    }

    String normalized = keyword
        .replaceAll("\\s+", " ")
        .trim();

    if (normalized.length() <= 100) {
      return normalized;
    }

    String shortened = normalized.substring(0, 100).trim();
    int lastSpace = shortened.lastIndexOf(" ");

    if (lastSpace >= 10) {
      shortened = shortened.substring(0, lastSpace).trim();
    }

    return shortened;
  }
}
