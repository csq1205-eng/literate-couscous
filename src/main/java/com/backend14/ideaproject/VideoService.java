package com.backend14.ideaproject;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoService {

  public String captureFrame(String videoUrl) {
    return captureFrame(videoUrl, "00:00:30", "captured_frame.jpg");
  }

  public String captureFrame(String videoUrl, String captureTime, String outputFileName) {
    try {
      String directUrl = getDirectVideoUrl(videoUrl);
      return captureFrameFromDirectUrl(directUrl, captureTime, outputFileName);

    } catch (Exception e) {
      e.printStackTrace();
      return "실패: " + e.getMessage();
    }
  }

  public List<String> captureFrames(String videoUrl, List<VideoMetadataService.CapturePoint> capturePoints, String filePrefix) {
    List<String> result = new ArrayList<>();

    try {
      String directUrl = getDirectVideoUrl(videoUrl);

      for (int i = 0; i < capturePoints.size(); i++) {
        VideoMetadataService.CapturePoint cp = capturePoints.get(i);
        String outputFileName = filePrefix + "_" + String.format("%02d", i + 1) + ".jpg";
        result.add(captureFrameFromDirectUrl(directUrl, cp.getTime(), outputFileName));
      }

    } catch (Exception e) {
      e.printStackTrace();
      result.add("실패: " + e.getMessage());
    }

    return result;
  }

  public String captureFrame(String videoUrl, String captureTime) {
    return captureFrame(videoUrl, captureTime, "captured_frame.jpg");
  }

  private String getDirectVideoUrl(String videoUrl) throws Exception {
    ProcessBuilder ytPb = new ProcessBuilder(
        "yt-dlp",
        "-g",
        videoUrl
    );

    Process ytProcess = ytPb.start();

    BufferedReader reader = new BufferedReader(
        new InputStreamReader(ytProcess.getInputStream(), StandardCharsets.UTF_8)
    );

    String directUrl = reader.readLine();
    int exitCode = ytProcess.waitFor();

    if (exitCode != 0 || directUrl == null || directUrl.isBlank()) {
      throw new RuntimeException("영상 주소를 가져오지 못했습니다.");
    }

    return directUrl;
  }

  private String captureFrameFromDirectUrl(String directUrl, String captureTime, String outputFileName) throws Exception {
    String projectPath = System.getProperty("user.dir");
    String tempDirPath = projectPath + "/temp";

    File tempDir = new File(tempDirPath);
    if (!tempDir.exists()) {
      tempDir.mkdirs();
    }

    String outputFilePath = tempDirPath + "/" + outputFileName;

    ProcessBuilder ffPb = new ProcessBuilder(
        "ffmpeg",
        "-y",
        "-ss", captureTime,
        "-i", directUrl,
        "-vframes", "1",
        "-q:v", "2",
        outputFilePath
    );

    Process ffProcess = ffPb.start();
    int exitCode = ffProcess.waitFor();

    if (exitCode != 0) {
      return "실패: " + captureTime + " 캡처에 실패했습니다.";
    }

    return outputFilePath;
  }
}
