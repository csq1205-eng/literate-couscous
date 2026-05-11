package com.backend14.ideaproject;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Service
public class VideoService {

  public String captureFrame(String videoUrl, String captureTime) {
    try {
      String projectPath = System.getProperty("user.dir");
      String downloadPath = projectPath + "/temp";

      File directory = new File(downloadPath);
      if (!directory.exists()) {
        directory.mkdirs();
      }

      String outputFileName = downloadPath + "/captured_frame.jpg";

      ProcessBuilder ytPb = new ProcessBuilder(
          "yt-dlp",
          "-g",
          videoUrl
      );

      Process ytProcess = ytPb.start();

      BufferedReader reader = new BufferedReader(
          new InputStreamReader(ytProcess.getInputStream())
      );

      String directUrl = reader.readLine();
      ytProcess.waitFor();

      if (directUrl == null || directUrl.isEmpty()) {
        return "실패: 영상 주소를 가져오지 못했습니다.";
      }

      ProcessBuilder ffPb = new ProcessBuilder(
          "ffmpeg",
          "-ss", captureTime,
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