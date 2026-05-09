package com.backend14.ideaproject;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Service
public class VideoService {

  public String captureFrame(String videoUrl) {
    try {
      // 1. 저장할 폴더 생성 (프로젝트 루트의 temp 폴더)
      String projectPath = System.getProperty("user.dir");
      String downloadPath = projectPath + "/temp";
      File directory = new File(downloadPath);
      if (!directory.exists()) directory.mkdirs();

      String outputFileName = downloadPath + "/captured_frame.jpg";

      // 2. yt-dlp를 이용해 영상의 '진짜 재생 주소' 추출 (인스타/유튜브 공통)
      // 루트 폴더에 있는 yt-dlp.exe를 실행합니다.
      ProcessBuilder ytPb = new ProcessBuilder("yt-dlp", "-g", videoUrl);
      Process ytProcess = ytPb.start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(ytProcess.getInputStream()));
      String directUrl = reader.readLine(); // 실제 영상 스트리밍 URL 확보
      ytProcess.waitFor();

      if (directUrl == null || directUrl.isEmpty()) {
        return "실패: 영상 주소를 가져오지 못했습니다. 링크를 확인하거나 yt-dlp.exe 위치를 확인하세요.";
      }

      // 3. 추출된 directUrl을 ffmpeg에 넣어 10초 지점 캡처
      // 루트 폴더에 있는 ffmpeg.exe를 실행합니다.
      ProcessBuilder ffPb = new ProcessBuilder(
          "ffmpeg",
          "-ss", "00:00:10",
          "-i", directUrl,
          "-vframes", "1",
          "-q:v", "2",
          outputFileName,
          "-y"
      );

      ffPb.inheritIO().start().waitFor();

      return outputFileName;
    } catch (Exception e) {
      e.printStackTrace();
      return "실패: " + e.getMessage();
    }
  }
}