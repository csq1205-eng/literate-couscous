package com.backend14.ideaproject;

import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class AiController {

  private final OllamaChatModel chatModel;
  private final VideoService videoService;
  private final KakaoPlaceService kakaoPlaceService;
  private final MapService mapService;
  private final VideoMetadataService videoMetadataService;

  public AiController(OllamaChatModel chatModel,
                      VideoService videoService,
                      KakaoPlaceService kakaoPlaceService,
                      MapService mapService,
                      VideoMetadataService videoMetadataService) {
    this.chatModel = chatModel;
    this.videoService = videoService;
    this.kakaoPlaceService = kakaoPlaceService;
    this.mapService = mapService;
    this.videoMetadataService = videoMetadataService;
  }

  @GetMapping("/ai/test")
  public String test(@RequestParam(value = "message", defaultValue = "반가워! 너는 어떤 모델이니?") String message) {
    return chatModel.call(message);
  }

  @GetMapping("/ai/capture")
  public String capture(@RequestParam String url) {
    String filePath = videoService.captureFrame(url);
    return "캡처 완료! 저장 위치: " + filePath;
  }

  @GetMapping("/ai/analyze")
  public String analyzeImage() {
    try {
      String result = analyzeCapturedImage(
          "이 이미지 속 장소 정보를 알려줘. 맛집이라면 가게 이름, 위치 추정, 음식 종류, 특징을 정리해줘."
      );

      String savedPath = saveResultToTextFile(result);

      return result + "\n\n[텍스트 저장 완료]\n" + savedPath;

    } catch (Exception e) {
      e.printStackTrace();
      return "분석 중 에러 발생: " + e.getMessage();
    }
  }

  @GetMapping("/ai/map-auto")
  public String analyzeAndOpenMap() {
    try {
      String keyword = analyzeCapturedImage(
          """
          이 이미지에서 실제 장소명 또는 가게 이름만 찾아줘.
          설명하지 말고 장소명 하나만 출력해.
          정확히 모르겠으면 '알 수 없음'이라고만 출력해.
          """
      );

      keyword = cleanKeyword(keyword);

      if (keyword.isBlank() || keyword.contains("알 수 없음")) {
        return "장소명을 자동으로 찾지 못했습니다.\n분석 결과: " + keyword;
      }

      PlaceResult place = kakaoPlaceService.searchPlace(keyword);

      if (place == null) {
        return "카카오맵에서 장소를 찾지 못했습니다.\n검색어: " + keyword;
      }

      String mapUrl = mapService.openMap(place);

      return """
                    지도 자동 표시 완료

                    AI 추정 검색어: %s
                    카카오맵 검색 결과: %s
                    주소: %s
                    위도: %f
                    경도: %f
                    지도 주소: %s
                    """.formatted(
          keyword,
          place.getName(),
          place.getAddress(),
          place.getLatitude(),
          place.getLongitude(),
          mapUrl
      );

    } catch (Exception e) {
      e.printStackTrace();
      return "지도 자동 표시 중 에러 발생: " + e.getMessage();
    }
  }

  @GetMapping("/ai/map-keyword")
  public String openMapByKeyword(@RequestParam String keyword) {
    PlaceResult place = kakaoPlaceService.searchPlace(keyword);

    if (place == null) {
      return "카카오맵에서 장소를 찾지 못했습니다.\n검색어: " + keyword;
    }

    String mapUrl = mapService.openMap(place);

    return """
                지도 표시 완료

                검색어: %s
                장소명: %s
                주소: %s
                위도: %f
                경도: %f
                지도 주소: %s
                """.formatted(
        keyword,
        place.getName(),
        place.getAddress(),
        place.getLatitude(),
        place.getLongitude(),
        mapUrl
    );
  }

  private String analyzeCapturedImage(String promptText) {
    String projectPath = System.getProperty("user.dir");
    String filePath = projectPath + "/temp/captured_frame.jpg";

    FileSystemResource imageResource = new FileSystemResource(filePath);

    if (!imageResource.exists()) {
      return "분석할 사진이 없습니다. 먼저 캡처를 진행해주세요.";
    }

    Media media = new Media(MimeTypeUtils.IMAGE_JPEG, imageResource);

    UserMessage userMessage = new UserMessage(promptText, List.of(media));

    return chatModel.call(new Prompt(userMessage))
        .getResult()
        .getOutput()
        .getContent();
  }

  private String cleanKeyword(String keyword) {
    if (keyword == null) {
      return "";
    }

    return keyword
        .replace("\"", "")
        .replace("'", "")
        .replace("장소명:", "")
        .replace("가게명:", "")
        .replace("검색어:", "")
        .trim();
  }

  private String saveResultToTextFile(String result) {
    try {
      String projectPath = System.getProperty("user.dir");

      File resultDir = new File(projectPath + "/result");

      if (!resultDir.exists()) {
        resultDir.mkdirs();
      }

      String time = LocalDateTime.now()
          .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

      File file = new File(resultDir, "analysis_" + time + ".txt");

      FileWriter writer = new FileWriter(file);
      writer.write(result);
      writer.close();

      return file.getAbsolutePath();

    } catch (Exception e) {
      e.printStackTrace();
      return "텍스트 파일 저장 실패: " + e.getMessage();
    }
  }

  @GetMapping("/ai/metadata")
  public String extractMetadata(@RequestParam String url) {
    return videoMetadataService.extractMetadataText(url);
  }

  @GetMapping("/ai/map-from-metadata")
  public String mapFromMetadata(@RequestParam String url) {
    try {
      String metadata = videoMetadataService.extractMetadataText(url);

      String prompt = """
                아래는 영상의 제목, 설명글(캡션), 그리고 업로더의 댓글 내용입니다.
                이 내용을 분석해서 영상이 추천하는 장소(가게 이름 또는 명소)가 어디인지 파악하고, 카카오맵에 검색할 수 있는 실제 장소명 하나만 정확하게 찾아주세요.
                
                규칙: 다른 설명 없이 장소명 하나만 출력하세요. 정확히 모르겠으면 '알 수 없음'이라고만 출력하세요.

                %s
                """.formatted(metadata);

      String keyword = chatModel.call(prompt);
      keyword = cleanKeyword(keyword);

      if (keyword.isBlank() || keyword.contains("알 수 없음")) {
        return "설명글이나 댓글에서 장소명을 찾지 못했습니다.\n\n" + metadata;
      }

      PlaceResult place = kakaoPlaceService.searchPlace(keyword);

      if (place == null) {
        return "카카오맵에서 장소를 찾지 못했습니다.\n검색어: " + keyword + "\n\n" + metadata;
      }

      String mapUrl = mapService.openMap(place);

      return """
                설명글 및 댓글 기반 지도 표시 완료

                추출 검색어: %s
                카카오맵 검색 결과: %s
                주소: %s
                위도: %f
                경도: %f
                지도 주소: %s

                ---- 원본 메타데이터 ----
                %s
                """.formatted(
          keyword,
          place.getName(),
          place.getAddress(),
          place.getLatitude(),
          place.getLongitude(),
          mapUrl,
          metadata
      );

    } catch (Exception e) {
      e.printStackTrace();
      return "설명글/댓글 기반 지도 표시 중 오류 발생: " + e.getMessage();
    }
  }
}