package com.backend14.ideaproject;

import com.backend14.ideaproject.gui.FoodMapGui;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.*;

@SpringBootApplication
public class IdeaprojectApplication {

  public static void main(String[] args) {
    System.setProperty("java.awt.headless", "false");

    SpringApplication.run(IdeaprojectApplication.class, args);

    SwingUtilities.invokeLater(() -> {
      FoodMapGui gui = new FoodMapGui();
      gui.setVisible(true);
    });
  }
}