#🦁 멋사대학 14기 가천대학교 5조 Git-깔나는놈들
똑똑한 지도 [어슬렁]입니다.
### 🛠 실행 전 준비사항
1. **Ollama 설치 및 모델 다운로드**
    - 터미널에서 `ollama pull qwen2.5` 실행 필수 (AI 모델)
2. **JDK 버전**
    - Java 21 이상 권장
3. **yt-dlp 사용**

## 핵심 구현 기능

- yt-dlp를 활용하여 영상 URL에서 제목, 설명글, 댓글 정보를 추출했습니다.
- AI 모델을 이용해 영상 설명글과 댓글에서 장소명, 주소, 지역 단서를 분석했습니다.
- 주소가 포함된 경우 주소를 우선 검색하도록 하여 장소 검색 정확도를 높였습니다.
- 주소가 없는 경우 매장명과 지역 단서를 조합하여 카카오맵 검색어 후보를 생성했습니다.
- Kakao Local API를 연동하여 장소명, 주소, 위도, 경도 정보를 가져오도록 구현했습니다.
- 검색된 장소 정보를 바탕으로 카카오맵에서 해당 위치를 확인할 수 있도록 구현했습니다.
- Java Swing 기반 GUI를 제작하여 영상 URL 입력, 분석 실행, 결과 확인이 가능하도록 구성했습니다.

## 실행 화면
<img width="837" height="687" alt="스크린샷 2026-05-18 오전 12 38 36" src="https://github.com/user-attachments/assets/79f52e5d-b13b-4248-8036-1cc70e5dbf05" />
<img width="832" height="688" alt="스크린샷 2026-05-18 오전 12 39 23" src="https://github.com/user-attachments/assets/b7870e3c-def0-4e13-985d-4afbbe3003eb" />
<img width="1004" height="642" alt="스크린샷 2026-05-18 오전 12 40 06" src="https://github.com/user-attachments/assets/e249fb45-7784-4ee8-99c6-dda765448b10" />


