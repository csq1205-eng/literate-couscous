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


  private JTextField urlField;
  private JTextArea resultArea;

  private JButton analyzeAllButton;
  private JButton autoMapButton;
  private JButton keywordMapButton;
  private JButton saveButton;
  private JButton viewResultsButton;

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

    analyzeAllButton = new JButton("1. 영상 분석하기");
    autoMapButton = new JButton("2. 자동 지도 표시");
    keywordMapButton = new JButton("3. 직접 검색해서 지도 표시");
    saveButton = new JButton("4. 결과 저장하기");
    viewResultsButton = new JButton("5. 저장한 결과 보기");

    buttonPanel.add(analyzeAllButton);
    buttonPanel.add(autoMapButton);
    buttonPanel.add(keywordMapButton);
    buttonPanel.add(saveButton);
    buttonPanel.add(viewResultsButton);

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

    analyzeAllButton.addActionListener(e -> analyzeVideoAllInOne());
    autoMapButton.addActionListener(e -> openAutoMapMenu());
    keywordMapButton.addActionListener(e -> openMapByKeyword());
    saveButton.addActionListener(e -> saveResultToTxt());
    viewResultsButton.addActionListener(e -> openSavedResultsWindow());
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
    analyzeAllButton.setEnabled(enabled);
    autoMapButton.setEnabled(enabled);
    keywordMapButton.setEnabled(enabled);
    saveButton.setEnabled(enabled);
    viewResultsButton.setEnabled(enabled);
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
  private void openSavedPostsWindow() {
    JFrame frame = new JFrame("저장한 맛집");
    frame.setSize(650, 750);
    frame.setLocationRelativeTo(this);

    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
    listPanel.setBackground(new Color(245, 245, 245));

    File savedDir = new File("saved_posts");

    if (!savedDir.exists() || savedDir.listFiles() == null) {
      JLabel emptyLabel = new JLabel("저장한 게시물이 없습니다.");
      emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
      listPanel.add(emptyLabel);
    } else {
      File[] postDirs = savedDir.listFiles(File::isDirectory);

      if (postDirs == null || postDirs.length == 0) {
        JLabel emptyLabel = new JLabel("저장한 게시물이 없습니다.");
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        listPanel.add(emptyLabel);
      } else {
        for (File postDir : postDirs) {
          JPanel card = createPostCard(postDir);
          listPanel.add(card);
          listPanel.add(Box.createVerticalStrut(16));
        }
      }
    }

    JScrollPane scrollPane = new JScrollPane(listPanel);
    frame.add(scrollPane);

    frame.setVisible(true);
  }
  private JPanel createPostCard(File postDir) {
    JPanel card = new JPanel();
    card.setLayout(new BorderLayout(10, 10));
    card.setBackground(Color.WHITE);
    card.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(12, 12, 12, 12),
        BorderFactory.createLineBorder(new Color(220, 220, 220))
    ));

    card.setMaximumSize(new Dimension(580, 650));

    // 상단 제목
    JLabel header = new JLabel("  🍽 맛길잡이");
    header.setFont(new Font("SansSerif", Font.BOLD, 18));
    header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    card.add(header, BorderLayout.NORTH);

    // 이미지
    File imageFile = new File(postDir, "image.jpg");

    if (imageFile.exists()) {
      ImageIcon imageIcon = new ImageIcon(imageFile.getAbsolutePath());

      Image scaledImage = imageIcon.getImage()
          .getScaledInstance(540, 300, Image.SCALE_SMOOTH);

      JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
      imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

      card.add(imageLabel, BorderLayout.CENTER);
    } else {
      JLabel noImageLabel = new JLabel("이미지가 없습니다.", SwingConstants.CENTER);
      noImageLabel.setPreferredSize(new Dimension(540, 200));
      card.add(noImageLabel, BorderLayout.CENTER);
    }

    // 텍스트
    String content = readContent(new File(postDir, "content.txt"));

    JTextArea contentArea = new JTextArea(content);
    contentArea.setLineWrap(true);
    contentArea.setWrapStyleWord(true);
    contentArea.setEditable(false);
    contentArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
    contentArea.setBackground(Color.WHITE);
    contentArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JScrollPane textScroll = new JScrollPane(contentArea);
    textScroll.setPreferredSize(new Dimension(540, 180));

    // 하단 버튼
    JButton openFolderButton = new JButton("폴더 열기");
    openFolderButton.addActionListener(e -> {
      try {
        Desktop.getDesktop().open(postDir);
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "폴더 열기 실패: " + ex.getMessage());
      }
    });

    JPanel bottomPanel = new JPanel(new BorderLayout());
    bottomPanel.setBackground(Color.WHITE);
    bottomPanel.add(textScroll, BorderLayout.CENTER);
    bottomPanel.add(openFolderButton, BorderLayout.SOUTH);

    card.add(bottomPanel, BorderLayout.SOUTH);

    return card;
  }
  private void openSavedResultsWindow() {
    JFrame frame = new JFrame("저장한 분석 결과");
    frame.setSize(650, 750);
    frame.setLocationRelativeTo(this);

    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
    listPanel.setBackground(new Color(245, 245, 245));
    listPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    File resultDir = new File("result");

    if (!resultDir.exists()) {
      JLabel emptyLabel = new JLabel("저장된 분석 결과가 없습니다.");
      emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
      listPanel.add(emptyLabel);
    } else {
      File[] txtFiles = resultDir.listFiles((dir, name) -> name.endsWith(".txt"));

      if (txtFiles == null || txtFiles.length == 0) {
        JLabel emptyLabel = new JLabel("저장된 분석 결과가 없습니다.");
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        listPanel.add(emptyLabel);
      } else {
        // 최신 파일이 위로 오게 정렬
        java.util.Arrays.sort(txtFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        for (File txtFile : txtFiles) {
          JPanel card = createResultCard(txtFile);
          listPanel.add(card);
          listPanel.add(Box.createVerticalStrut(16));
        }
      }
    }


    JScrollPane scrollPane = new JScrollPane(listPanel);
    frame.add(scrollPane);
    frame.setVisible(true);
  }
  private JPanel createResultCard(File txtFile) {
    JPanel card = new JPanel();
    card.setLayout(new BorderLayout(10, 10));
    card.setBackground(Color.WHITE);
    card.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(220, 220, 220)),
        BorderFactory.createEmptyBorder(14, 14, 14, 14)
    ));

    card.setMaximumSize(new Dimension(580, 420));

    JLabel header = new JLabel("  🍽 맛길잡이");
    header.setFont(new Font("SansSerif", Font.BOLD, 18));
    header.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

    JLabel fileNameLabel = new JLabel("  " + txtFile.getName());
    fileNameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
    fileNameLabel.setForeground(Color.GRAY);

    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setBackground(Color.WHITE);
    headerPanel.add(header, BorderLayout.NORTH);
    headerPanel.add(fileNameLabel, BorderLayout.SOUTH);

    card.add(headerPanel, BorderLayout.NORTH);

    String content = readContent(txtFile);

    JTextArea contentArea = new JTextArea(content);
    contentArea.setLineWrap(true);
    contentArea.setWrapStyleWord(true);
    contentArea.setEditable(false);
    contentArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
    contentArea.setBackground(Color.WHITE);
    contentArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JScrollPane textScroll = new JScrollPane(contentArea);
    textScroll.setPreferredSize(new Dimension(540, 250));

    card.add(textScroll, BorderLayout.CENTER);

    JButton openFileButton = new JButton("txt 파일 열기");
    openFileButton.addActionListener(e -> {
      try {
        Desktop.getDesktop().open(txtFile);
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "파일 열기 실패: " + ex.getMessage());
      }
    });

    JButton copyButton = new JButton("내용 복사");
    copyButton.addActionListener(e -> {
      Toolkit.getDefaultToolkit()
          .getSystemClipboard()
          .setContents(new java.awt.datatransfer.StringSelection(content), null);

      JOptionPane.showMessageDialog(this, "내용을 복사했습니다.");
    });

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.setBackground(Color.WHITE);
    buttonPanel.add(copyButton);
    buttonPanel.add(openFileButton);

    card.add(buttonPanel, BorderLayout.SOUTH);

    return card;
  }
  private String readContent(File file) {
    if (!file.exists()) {
      return "내용이 없습니다.";
    }

    try {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      StringBuilder sb = new StringBuilder();

      String line;

      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }

      reader.close();

      return sb.toString();

    } catch (Exception e) {
      return "파일 읽기 실패: " + e.getMessage();
    }
  }
    private void analyzeVideoAllInOne() {
      String videoUrl = urlField.getText().trim();

      if (videoUrl.isEmpty()) {
        JOptionPane.showMessageDialog(this, "영상 URL을 입력해주세요.");
        return;
      }

      setLoading("영상 캡처 후 AI 분석 중입니다...");

      new Thread(() -> {
        try {
          String encodedUrl = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8);

          String captureUrl = BASE_URL + "/ai/capture?url=" + encodedUrl;
          String captureResult = sendGetRequest(captureUrl);

          String analyzeUrl = BASE_URL + "/ai/analyze";
          String analyzeResult = sendGetRequest(analyzeUrl);

          String finalResult = """
          [캡처 결과]
          %s

          [AI 분석 결과]
          %s
          """.formatted(captureResult, analyzeResult);

          SwingUtilities.invokeLater(() -> {
            resultArea.setText(finalResult);
            setButtonEnabled(true);
          });

        } catch (Exception ex) {
          SwingUtilities.invokeLater(() -> {
            resultArea.setText("영상 분석 중 오류 발생: " + ex.getMessage());
            setButtonEnabled(true);
          });
        }
      }).start();
    }
    private void openAutoMapMenu() {
      String[] options = {
          "설명글 기반 자동 표시",
          "이미지 분석 기반 자동 표시"
      };

      int choice = JOptionPane.showOptionDialog(
          this,
          "자동 지도 표시 방식을 선택하세요.",
          "자동 지도 표시",
          JOptionPane.DEFAULT_OPTION,
          JOptionPane.QUESTION_MESSAGE,
          null,
          options,
          options[0]
      );

      if (choice == 0) {
        openMapFromMetadata();
      } else if (choice == 1) {
        openMapAutomatically();
      }
    }

}