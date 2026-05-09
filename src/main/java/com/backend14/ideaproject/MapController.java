package com.backend14.ideaproject;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MapController {

  private final MapService mapService;

  public MapController(MapService mapService) {
    this.mapService = mapService;
  }

  @GetMapping(value = "/map/latest", produces = MediaType.TEXT_HTML_VALUE)
  public String latestMap() {
    return mapService.getLatestMapHtml();
  }
}