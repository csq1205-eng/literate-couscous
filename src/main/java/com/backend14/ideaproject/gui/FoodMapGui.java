package com.backend14.ideaproject.gui;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FoodMapGui extends JFrame {

  private static final String BASE_URL = "http://localhost:8080";

  private JTextField urlField;
  private JTextArea resultArea;
  private JButton analyzeButton;

  public FoodMapGui() {
    setTitle("어슬렁");
    setSize(850, 700);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    createUI();
  }

  private void createUI() {
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    JLabel titleLabel = new JLabel("어슬렁", SwingConstants.CENTER);
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
    mainPanel.add(titleLabel, BorderLayout.NORTH);

    JPanel topPanel = new JPanel(new BorderLayout(10, 10));

    JLabel urlLabel = new JLabel("영상 URL:");
    urlField = new JTextField();

    JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
    inputPanel.add(urlLabel, BorderLayout.WEST);
    inputPanel.add(urlField, BorderLayout.CENTER);

    analyzeButton = new JButton("영상 분석하기");

    JPanel buttonPanel = new JPanel(new FlowLayout());
    buttonPanel.add(analyzeButton);

    topPanel.add(inputPanel, BorderLayout.NORTH);
    topPanel.add(buttonPanel, BorderLayout.SOUTH);

    mainPanel.add(topPanel, BorderLayout.CENTER);

    resultArea = new JTextArea();
    resultArea.setLineWrap(true);
    resultArea.setWrapStyleWord(true);
    resultArea.setFont(new Font("SansSerif", Font.PLAIN, 15));
    resultArea.setText("영상 URL을 입력하고 [영상 분석하기] 버튼을 눌러주세요.");

    JScrollPane scrollPane = new JScrollPane(resultArea);
    scrollPane.setPreferredSize(new Dimension(780, 480));

    mainPanel.add(scrollPane, BorderLayout.SOUTH);

    add(mainPanel);

    analyzeButton.addActionListener(e -> analyzeVideo());
  }

  private void analyzeVideo() {
    String videoUrl = urlField.getText().trim();

    if (videoUrl.isEmpty()) {
      JOptionPane.showMessageDialog(this, "영상 URL을 입력해주세요.");
      return;
    }

    setLoading("영상 설명글과 댓글을 분석해서 장소를 찾는 중입니다...");

    new Thread(() -> {
      try {
        String encodedUrl = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8);

        // 현재 AiController의 자동 장소 분석 API
        String requestUrl = BASE_URL + "/ai/map-from-metadata?url=" + encodedUrl;

        String response = sendGetRequest(requestUrl);

        SwingUtilities.invokeLater(() -> {
          resultArea.setText(response);
          analyzeButton.setEnabled(true);
        });

      } catch (Exception ex) {
        SwingUtilities.invokeLater(() -> {
          resultArea.setText("영상 분석 중 오류 발생: " + ex.getMessage());
          analyzeButton.setEnabled(true);
        });
      }
    }).start();
  }

  private void setLoading(String message) {
    resultArea.setText(message);
    analyzeButton.setEnabled(false);
  }

  private String sendGetRequest(String requestUrl) throws IOException {
    URL url = new URL(requestUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    connection.setRequestMethod("GET");
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(180000);

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
      response.append(line).append("\n");
    }

    reader.close();
    connection.disconnect();

    if (statusCode >= 200 && statusCode < 300) {
      return response.toString();
    }

    return "서버 오류: " + statusCode + "\n" + response;
  }
}