# 🦁 AI 맛집 탐정 프로젝트 (backend14)

### 🛠 실행 전 준비사항
1. **Ollama 설치 및 모델 다운로드**
    - 터미널에서 `ollama pull llava` 실행 필수 (이미지 분석용)
2. **JDK 버전**
    - Java 21 이상 권장
3. **Gradle 설정**
    - 프로젝트 로드 후 반드시 Gradle Refresh (코끼리 버튼) 클릭!

### 🚀 테스트 엔드포인트
- `GET /ai/capture?url={유튜브주소}` : 화면 캡처
- `GET /ai/capture-percent?url={유튜브주소}` : 영상 길이 기준 10% 간격으로 10장 캡처
- `GET /ai/map-from-metadata?url={유튜브주소}` : 캡션/설명글, 영상 주인/고정 댓글, 10% 간격 캡처 순서로 장소 검색
- `GET /ai/analyze` : 이미지 분석 결과 반환# literate-couscous
