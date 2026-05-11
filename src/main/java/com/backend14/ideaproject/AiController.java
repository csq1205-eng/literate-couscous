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
  public String capture(@RequestParam String url,
                        @RequestParam(defaultValue = "00:00:10") String time) {
    String filePath = videoService.captureFrame(url, time);
    return "캡처 완료! 캡처 시간: " + time + "\n저장 위치: " + filePath;
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
        .replace("입니다.", "")
        .replace("입니다", "")
        .replace("입니다,", "")
        .replace(".", "")
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
        아래는 영상 제목과 설명글입니다.
        이 내용에서 카카오맵에 검색할 수 있는 실제 가게 이름 또는 주소 하나만 찾아주세요.

        규칙:
        1. 설명하지 마세요.
        2. 문장으로 쓰지 마세요.
        3. '~입니다', '~로 보입니다' 같은 말은 붙이지 마세요.
        4. 실제 상호명 또는 주소만 한 줄로 출력하세요.
        5. 식당목록이 있으면 그중 하나의 상호명만 출력하세요.
        6. 찾을 수 없으면 '알 수 없음'이라고만 출력하세요.

        좋은 출력 예시:
        쿠치카츠쿠마

        나쁜 출력 예시:
        쿠치카츠쿠마입니다.

        [영상 정보]
        %s
        """.formatted(metadata);

      String keyword = chatModel.call(prompt);
      keyword = cleanKeyword(keyword);

      if (keyword.isBlank() || keyword.contains("알 수 없음")) {
        return "설명글에서 장소명을 찾지 못했습니다.\n\n" + metadata;
      }

      PlaceResult place = kakaoPlaceService.searchPlace(keyword);

      if (place == null) {
        String address = findAddressNearKeyword(metadata, keyword);

        if (!address.isBlank()) {
          place = kakaoPlaceService.searchPlace(address);
        }

        if (place == null) {
          return "카카오맵에서 장소를 찾지 못했습니다."
              + "\n검색어: " + keyword
              + "\n주소 후보: " + address
              + "\n\n" + metadata;
        }
      }

      String mapUrl = mapService.openMap(place);

      return """
                설명글 기반 지도 표시 완료

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
      return "설명글 기반 지도 표시 중 오류 발생: " + e.getMessage();
    }
  }
  private String findAddressNearKeyword(String metadata, String keyword) {
    if (metadata == null || keyword == null || keyword.isBlank()) {
      return "";
    }

    String[] lines = metadata.split("\\R");

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();

      if (line.contains(keyword)) {
        // 가게명 바로 다음 줄에 주소가 있는 경우
        if (i + 1 < lines.length) {
          String nextLine = lines[i + 1].trim();

          if (looksLikeAddress(nextLine)) {
            return nextLine;
          }
        }

        // 혹시 다음다음 줄에 주소가 있는 경우
        if (i + 2 < lines.length) {
          String nextNextLine = lines[i + 2].trim();

          if (looksLikeAddress(nextNextLine)) {
            return nextNextLine;
          }
        }
      }
    }

    return "";
  }

  private boolean looksLikeAddress(String text) {
    if (text == null) {
      return false;
    }

    return text.contains("서울")
        || text.contains("경기")
        || text.contains("인천")
        || text.contains("부산")
        || text.contains("대구")
        || text.contains("광주")
        || text.contains("대전")
        || text.contains("울산")
        || text.contains("세종")
        || text.contains("강원")
        || text.contains("충북")
        || text.contains("충남")
        || text.contains("전북")
        || text.contains("전남")
        || text.contains("경북")
        || text.contains("경남")
        || text.contains("제주");
  }
}