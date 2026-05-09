package com.backend14.ideaproject.gui;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FoodMapGui extends JFrame {

  private static final String BASE_URL = "http://localhost:8080";


  private JButton metadataMapButton;
  private JTextField urlField;
  private JTextArea resultArea;

  private JButton captureButton;
  private JButton analyzeButton;
  private JButton saveButton;
  private JButton autoMapButton;
  private JButton keywordMapButton;

  public FoodMapGui() {

    setTitle("AI 맛집 탐정");
    setSize(850, 700);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    createUI();
  }

  private void createUI() {
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));


    JLabel titleLabel = new JLabel("AI 맛집 탐정", SwingConstants.CENTER);
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
    mainPanel.add(titleLabel, BorderLayout.NORTH);

    JPanel topPanel = new JPanel(new BorderLayout(10, 10));

    JLabel urlLabel = new JLabel("영상 URL:");
    urlField = new JTextField();

    JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
    inputPanel.add(urlLabel, BorderLayout.WEST);
    inputPanel.add(urlField, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout());

    captureButton = new JButton("1. 영상 캡처하기");
    analyzeButton = new JButton("2. AI 분석하기");
    saveButton = new JButton("3. 결과 txt 저장");
    autoMapButton = new JButton("4. AI가 찾은 장소 지도 표시");
    keywordMapButton = new JButton("5. 직접 검색해서 지도 표시");
    metadataMapButton = new JButton("6. 설명글 기반 지도 표시");
    buttonPanel.add(captureButton);
    buttonPanel.add(analyzeButton);
    buttonPanel.add(saveButton);
    buttonPanel.add(autoMapButton);
    buttonPanel.add(keywordMapButton);
    buttonPanel.add(metadataMapButton);

    topPanel.add(inputPanel, BorderLayout.NORTH);
    topPanel.add(buttonPanel, BorderLayout.SOUTH);

    mainPanel.add(topPanel, BorderLayout.CENTER);

    resultArea = new JTextArea();
    resultArea.setLineWrap(true);
    resultArea.setWrapStyleWord(true);
    resultArea.setFont(new Font("SansSerif", Font.PLAIN, 15));
    resultArea.setText("결과가 여기에 표시됩니다.");

    JScrollPane scrollPane = new JScrollPane(resultArea);
    scrollPane.setPreferredSize(new Dimension(780, 430));

    mainPanel.add(scrollPane, BorderLayout.SOUTH);

    add(mainPanel);

    captureButton.addActionListener(e -> captureVideo());
    analyzeButton.addActionListener(e -> analyzeImage());
    saveButton.addActionListener(e -> saveResultToTxt());
    autoMapButton.addActionListener(e -> openMapAutomatically());
    keywordMapButton.addActionListener(e -> openMapByKeyword());
    metadataMapButton.addActionListener(e -> openMapFromMetadata());

  }

  private void captureVideo() {
    String videoUrl = urlField.getText().trim();

    if (videoUrl.isEmpty()) {
      JOptionPane.showMessageDialog(this, "영상 URL을 입력해주세요.");
      return;
    }

    setLoading("영상 캡처 중입니다...");

    new Thread(() -> {
      try {
        String encodedUrl = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8);
        String requestUrl = BASE_URL + "/ai/capture?url=" + encodedUrl;

        String response = sendGetRequest(requestUrl);

        SwingUtilities.invokeLater(() -> {
          resultArea.setText(response);
          setButtonEnabled(true);
        });

      } catch (Exception ex) {
        SwingUtilities.invokeLater(() -> {
          resultArea.setText("캡처 중 오류 발생: " + ex.getMessage());
          setButtonEnabled(true);
        });
      }
    }).start();
  }

  private void analyzeImage() {
    setLoading("AI 분석 중입니다...");

    new Thread(() -> {
      try {
        String requestUrl = BASE_URL + "/ai/analyze";
        String response = sendGetRequest(requestUrl);

        SwingUtilities.invokeLater(() -> {
          resultArea.setText(response);
          setButtonEnabled(true);
        });

      } catch (Exception ex) {
        SwingUtilities.invokeLater(() -> {
          resultArea.setText("분석 중 오류 발생: " + ex.getMessage());
          setButtonEnabled(true);
        });
      }
    }).start();
  }

  private void openMapAutomatically() {
    setLoading("AI가 장소명을 찾고 카카오맵에 표시하는 중입니다...");

    new Thread(() -> {
      try {
        String requestUrl = BASE_URL + "/ai/map-auto";
        String response = sendGetRequest(requestUrl);

        SwingUtilities.invokeLater(() -> {
          resultArea.setText(response);
          setButtonEnabled(true);
        });

      } catch (Exception ex) {
        SwingUtilities.invokeLater(() -> {
          resultArea.setText("지도 자동 표시 중 오류 발생: " + ex.getMessage());
          setButtonEnabled(true);
        });
      }
    }).start();

  }

  private void openMapByKeyword() {
    String keyword = JOptionPane.showInputDialog(this, "검색할 가게 이름이나 주소를 입력하세요.");

    if (keyword == null || keyword.trim().isEmpty()) {
      return;
    }

    setLoading("카카오맵에서 검색 중입니다...");

    new Thread(() -> {
      try {
        String encodedKeyword = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
        String requestUrl = BASE_URL + "/ai/map-keyword?keyword=" + encodedKeyword;

        String response = sendGetRequest(requestUrl);

        SwingUtilities.invokeLater(() -> {
          resultArea.setText(response);
          setButtonEnabled(true);
        });

      } catch (Exception ex) {
        SwingUtilities.invokeLater(() -> {
          resultArea.setText("지도 표시 중 오류 발생: " + ex.getMessage());
          setButtonEnabled(true);
        });
      }
    }).start();
  }

  private void setLoading(String message) {
    resultArea.setText(message);
    setButtonEnabled(false);
  }

  private void setButtonEnabled(boolean enabled) {
    captureButton.setEnabled(enabled);
    analyzeButton.setEnabled(enabled);
    saveButton.setEnabled(enabled);
    autoMapButton.setEnabled(enabled);
    keywordMapButton.setEnabled(enabled);
    metadataMapButton.setEnabled(enabled);
  }

  private String sendGetRequest(String requestUrl) throws IOException {
    URL url = new URL(requestUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    connection.setRequestMethod("GET");
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(120000);

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

  private void saveResultToTxt() {
    String result = resultArea.getText();

    if (result.isBlank() || result.equals("결과가 여기에 표시됩니다.")) {
      JOptionPane.showMessageDialog(this, "저장할 결과가 없습니다.");
      return;
    }

    try {
      File resultDir = new File("result");

      if (!resultDir.exists()) {
        resultDir.mkdirs();
      }

      String time = LocalDateTime.now()
          .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

      File file = new File(resultDir, "analysis_" + time + ".txt");

      FileWriter writer = new FileWriter(file);
      writer.write(result);
      writer.close();

      JOptionPane.showMessageDialog(this,
          "저장 완료!\n" + file.getAbsolutePath());

    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          "파일 저장 실패: " + e.getMessage());
    }
  }
  private void openMapFromMetadata() {
    String videoUrl = urlField.getText().trim();

    if (videoUrl.isEmpty()) {
      JOptionPane.showMessageDialog(this, "영상 URL을 입력해주세요.");
      return;
    }

    setLoading("영상 설명글에서 장소를 찾고 카카오맵에 표시하는 중입니다...");

    new Thread(() -> {
      try {
        String encodedUrl = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8);
        String requestUrl = BASE_URL + "/ai/map-from-metadata?url=" + encodedUrl;

        String response = sendGetRequest(requestUrl);

        SwingUtilities.invokeLater(() -> {
          resultArea.setText(response);
          setButtonEnabled(true);
        });

      } catch (Exception ex) {
        SwingUtilities.invokeLater(() -> {
          resultArea.setText("설명글 기반 지도 표시 중 오류 발생: " + ex.getMessage());
          setButtonEnabled(true);
        });
      }
    }).start();
  }

}