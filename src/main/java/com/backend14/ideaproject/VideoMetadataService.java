package com.backend14.ideaproject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Service
public class VideoMetadataService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public String extractMetadataText(String videoUrl) {
    try {
      // 유튜브 댓글은 너무 많을 수 있으므로 추출에 시간 제한과 가져올 댓글 수를 제한합니다.
      ProcessBuilder pb = new ProcessBuilder(
          "yt-dlp",
          "--dump-json",
          "--write-comments",
          "--extractor-args", "youtube:comment_sort=top;max_comments=10,all", // 최상단 댓글 10개로 제한
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

      // 40초 대기
      boolean finished = process.waitFor(40, TimeUnit.SECONDS);

      if (!finished) {
        process.destroy();
        return "메타데이터 추출 실패: 시간 초과 (댓글이 너무 많거나 연결이 지연되고 있습니다.)";
      }

      if (process.exitValue() != 0) {
        return "메타데이터 추출 실패:\n" + errorBuilder;
      }

      JsonNode root = objectMapper.readTree(jsonBuilder.toString());

      String title = root.path("title").asText("");
      String description = root.path("description").asText("");
      String uploader = root.path("uploader").asText("");
      String webpageUrl = root.path("webpage_url").asText(videoUrl);

      StringBuilder uploaderComments = new StringBuilder();
      JsonNode commentsNode = root.path("comments");
      if (commentsNode.isArray()) {
        for (JsonNode comment : commentsNode) {
          boolean isUploader = comment.path("author_is_uploader").asBoolean(false);
          if (isUploader) {
            uploaderComments.append(comment.path("text").asText("")).append("\n");
          }
        }
      }

      return """
                    [영상 제목]
                    %s

                    [업로더]
                    %s

                    [영상 설명글/캡션]
                    %s

                    [업로더 댓글]
                    %s

                    [원본 URL]
                    %s
                    """.formatted(
          title,
          uploader,
          description,
          uploaderComments.toString().trim(),
          webpageUrl
      );

    } catch (Exception e) {
      e.printStackTrace();
      return "메타데이터 추출 중 오류 발생: " + e.getMessage();
    }
  }
  public static class CapturePoint {
    private final String time;
    private final String label;

    public CapturePoint(String time, String label) {
      this.time = time;
      this.label = label;
    }

    public String getTime() {
      return time;
    }

    public String getLabel() {
      return label;
    }
  }
}