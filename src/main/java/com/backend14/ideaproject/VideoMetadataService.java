package com.backend14.ideaproject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class VideoMetadataService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public String extractMetadataText(String videoUrl) {
    try {
      ProcessBuilder pb = new ProcessBuilder(
          "yt-dlp",
          "--dump-json",
          "--skip-download",
          videoUrl
      );

      Process process = pb.start();

      BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream())
      );

      StringBuilder jsonBuilder = new StringBuilder();
      String line;

      while ((line = reader.readLine()) != null) {
        jsonBuilder.append(line);
      }

      BufferedReader errorReader = new BufferedReader(
          new InputStreamReader(process.getErrorStream())
      );

      StringBuilder errorBuilder = new StringBuilder();
      String errorLine;

      while ((errorLine = errorReader.readLine()) != null) {
        errorBuilder.append(errorLine).append("\n");
      }

      int exitCode = process.waitFor();

      if (exitCode != 0) {
        return "메타데이터 추출 실패:\n" + errorBuilder;
      }

      JsonNode root = objectMapper.readTree(jsonBuilder.toString());

      String title = root.path("title").asText("");
      String description = root.path("description").asText("");
      String uploader = root.path("uploader").asText("");
      String webpageUrl = root.path("webpage_url").asText(videoUrl);

      return """
                    [영상 제목]
                    %s

                    [업로더]
                    %s

                    [영상 설명글/캡션]
                    %s

                    [원본 URL]
                    %s
                    """.formatted(
          title,
          uploader,
          description,
          webpageUrl
      );

    } catch (Exception e) {
      e.printStackTrace();
      return "메타데이터 추출 중 오류 발생: " + e.getMessage();
    }
  }
}