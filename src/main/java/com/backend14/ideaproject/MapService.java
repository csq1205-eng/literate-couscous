package com.backend14.ideaproject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Desktop;
import java.net.URI;

@Service
public class MapService {

  @Value("${kakao.javascript-key}")
  private String kakaoJavaScriptKey;

  private String latestMapHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>맛집 지도</title>
            </head>
            <body>
                <h2>아직 표시할 지도가 없습니다.</h2>
            </body>
            </html>
            """;

  public String openMap(PlaceResult place) {
    try {
      latestMapHtml = createMapHtml(place);

      Desktop.getDesktop().browse(new URI("http://localhost:8080/map/latest"));

      return "http://localhost:8080/map/latest";

    } catch (Exception e) {
      e.printStackTrace();
      return "지도 열기 실패: " + e.getMessage();
    }
  }

  public String getLatestMapHtml() {
    return latestMapHtml;
  }

  private String createMapHtml(PlaceResult place) {
    return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>AI 맛집 지도</title>
                    <style>
                        body {
                            margin: 0;
                            font-family: Arial, sans-serif;
                        }
                        #map {
                            width: 100vw;
                            height: 100vh;
                        }
                        .info-box {
                            position: absolute;
                            top: 20px;
                            left: 20px;
                            z-index: 10;
                            background: white;
                            padding: 14px 18px;
                            border-radius: 10px;
                            box-shadow: 0 2px 10px rgba(0,0,0,0.25);
                            line-height: 1.6;
                        }
                    </style>
                </head>
                <body>
                    <div class="info-box">
                        <b>%s</b><br>
                        %s<br>
                        위도: %f<br>
                        경도: %f
                    </div>

                    <div id="map"></div>

                    <script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=%s"></script>
                    <script>
                        var container = document.getElementById('map');

                        var position = new kakao.maps.LatLng(%f, %f);

                        var options = {
                            center: position,
                            level: 3
                        };

                        var map = new kakao.maps.Map(container, options);

                        var marker = new kakao.maps.Marker({
                            position: position
                        });

                        marker.setMap(map);

                        var infoWindow = new kakao.maps.InfoWindow({
                            content: '<div style="padding:8px;font-size:14px;">%s</div>'
                        });

                        infoWindow.open(map, marker);
                    </script>
                </body>
                </html>
                """.formatted(
        place.getName(),
        place.getAddress(),
        place.getLatitude(),
        place.getLongitude(),
        kakaoJavaScriptKey,
        place.getLatitude(),
        place.getLongitude(),
        place.getName()
    );
  }
}