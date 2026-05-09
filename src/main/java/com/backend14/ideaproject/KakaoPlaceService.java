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

@Service
public class KakaoPlaceService {

  @Value("${kakao.rest-api-key}")
  private String kakaoRestApiKey;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public PlaceResult searchPlace(String keyword) {
    try {
      String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

      String requestUrl =
          "https://dapi.kakao.com/v2/local/search/keyword.json?query="
              + encodedKeyword;

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
        return null;
      }

      return parseFirstPlace(response.toString());

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private PlaceResult parseFirstPlace(String json) {
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode documents = root.get("documents");

      if (documents == null || !documents.isArray() || documents.isEmpty()) {
        return null;
      }

      JsonNode first = documents.get(0);

      String placeName = first.path("place_name").asText();
      String addressName = first.path("address_name").asText();
      String x = first.path("x").asText(); // 경도
      String y = first.path("y").asText(); // 위도

      if (placeName.isBlank() || x.isBlank() || y.isBlank()) {
        return null;
      }

      double longitude = Double.parseDouble(x);
      double latitude = Double.parseDouble(y);

      return new PlaceResult(placeName, addressName, latitude, longitude);

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}