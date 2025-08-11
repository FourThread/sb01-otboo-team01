![header](https://capsule-render.vercel.app/api?type=cylinder&color=black&height=150&section=header&text=OZANG&fontSize=50&fontColor=d6ace6)

## 프로젝트 소개
### 👗 OZANG(옷장을 부탁해) - AI 의상 추천 서비스
**OZANG(옷장을 부탁해)** 은 사용자가 소유한 의상을 기반으로 **날씨와 개인 취향을 분석**하여 최적의 의상 조합을 추천하는 **AI 기반 개인화 스타일링 플랫폼**입니다. <br>
단순한 추천을 넘어, 사용자는 자신의 스타일을 **OOTD(Outfit Of The Day) 피드**에 공유하고, 다른 사용자와 **팔로우, 좋아요, 댓글, DM** 등 소셜 기능을 통해 패션 커뮤니티를 형성할 수 있습니다.
<img width="1872" height="993" alt="image" src="https://github.com/user-attachments/assets/26309ec3-4051-4e2d-b6b8-26ff1cece4f9" />



<br>

## ⌚ 프로젝트 정보
* **기간**: 2025.06.23 ~ 2025.07.31
* **시연 영상**: [YouTube 링크](https://www.youtube.com/watch?v=fOUkyDvbtDo)
* **배포 주소**: [https://www.ozang.shop](https://www.ozang.shop)

<br>


## ✨ 주요 기능

| 기능 | 설명 |
| :--- | :--- |
| **사용자 관리** | <ul><li>관리자 계정 초기화 및 권한 관리 (ADMIN / USER)</li><li>계정 잠금 및 자동 로그아웃 처리</li><li>JWT 기반 로그인/로그아웃 및 회원가입</li><li>임시 비밀번호 발급 및 만료 처리</li><li>Google/Kakao 소셜 로그인 연동</li></ul> |
| **프로필 관리** | <ul><li>사용자 이미지, 성별, 생년월일, 위치, 온도 민감도 설정</li><li>위도/경도를 기반으로 날씨 API 및 카카오 API 연동</li></ul> |
| **의상 관리** | <ul><li>사용자 의상 등록 및 속성 관리</li><li>웹 크롤링을 통한 쇼핑몰 링크(무신사, 29CM 등) 기반 정보 자동 입력</li></ul> |
| **날씨 데이터** | <ul><li>기상청 Open API를 활용한 실시간 날씨 데이터 수집</li><li>Spring Batch로 정기 작업 스케줄링</li><li>사용자 위치 기반 날씨 정보 제공 및 특이 기상 변화 알림</li></ul> |
| **AI 의상 추천** | <ul><li>날씨, 사용자 프로필(온도 민감도), 보유 의상을 종합한 개인 맞춤 추천</li><li>LLM API(OpenAI) 활용</li></ul> |
| **OOTD 소셜 피드** | <ul><li>추천 조합을 활용한 OOTD 피드 등록</li><li>좋아요 및 댓글 기능 제공</li></ul> |
| **팔로우 & DM** | <ul><li>사용자 간 팔로우 및 알림 연동</li><li>WebSocket 기반 DM(실시간 채팅) 기능 제공</li></ul> |
| **알림 시스템** | <ul><li>Server-Sent Events(SSE), Kafka 기반 실시간 알림 제공</li><li>이벤트(권한 변경, 피드 좋아요 및 댓글, 팔로우 / 피드 등록, DM 수신, 의상 속성 추가) 기반 알림 발송</li></ul> |

<br>

## 🌱 기술 스택
<div align="center">

**Back-end**  

<img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> 
<img src="https://img.shields.io/badge/Spring%20BATCH-6DB33F?style=for-the-badge&logo=Spring&logoColor=white"> 
<img src="https://img.shields.io/badge/Spring%20SECURITY-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"> 

**Security**

<img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logoColor=white"> 
<img src="https://img.shields.io/badge/OAuth2-000000?style=for-the-badge&logoColor=white"> 
<img src="https://img.shields.io/badge/kakao-FFCD00?style=for-the-badge&logo=kakao&logoColor=white"> 
<img src="https://img.shields.io/badge/google-4285F4?style=for-the-badge&logo=google&logoColor=white"> 

**DataBase**

<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white"> 
<img src="https://img.shields.io/badge/Redis-FF4438?style=for-the-badge&logo=redis&logoColor=white"> 

**Infra**

<img src="https://img.shields.io/badge/CloudFront-DC682E?style=for-the-badge&logoColor=white"> 
<img src="https://img.shields.io/badge/ALB-DC682E?style=for-the-badge&logoColor=white"> 
<img src="https://img.shields.io/badge/AWS%20ECR-FFB71B?style=for-the-badge&logoColor=white"> 
<img src="https://img.shields.io/badge/AWS%20ECS-FFB71B?style=for-the-badge&logoColor=white"> 
<img src="https://img.shields.io/badge/AWS%20S3-006600?style=for-the-badge&logoColor=white"> 
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> 
<img src="https://img.shields.io/badge/githubactions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white"> 

**Monitoring**

<img src="https://img.shields.io/badge/prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white"> 
<img src="https://img.shields.io/badge/grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white"> 

**Search**

<img src="https://img.shields.io/badge/elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white"> 

**Messaging**

<img src="https://img.shields.io/badge/apachekafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white"> 
<img src="https://img.shields.io/badge/WebSocket-000000?style=for-the-badge&logoColor=white"> 

**Test**

<img src="https://img.shields.io/badge/junit5-25A162?style=for-the-badge&logo=junit5&logoColor=white"> 
<img src="https://img.shields.io/badge/k6-7D64FF?style=for-the-badge&logo=k6&logoColor=white"> 

**Collaboration**  

<img src="https://img.shields.io/badge/swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black">
<img src="https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white">

</div>



## 🖥️ 시스템 아키텍처
<img width="1632" height="1139" alt="스크린샷 2025-07-31 105851" src="https://github.com/user-attachments/assets/fe9dcc42-06b3-451b-8679-6129cb45c7ec" />

## 🗂️ ERD
<img width="3840" height="2223" alt="Untitled diagram _ Mermaid Chart-2025-08-11-055331" src="https://github.com/user-attachments/assets/fc56729f-5e61-492c-a292-94b8df10a2e2" />



## 📁 프로젝트 구조
```
ozang
├── build.gradle
├── settings.gradle
├── ozang-app               # Spring Boot 앱 (REST API)
│   ├── controller          # User, Outfit, Style, AI 관련 컨트롤러
│   ├── service             # 비즈니스 로직 (OutfitService 등)
│   ├── config              # WebConfig, OAuth2Config, OpenAIConfig
│   ├── websocket           # 실시간 WebSocket 처리
│   ├── file                # 파일 업로드 관련
│   └── recommendation      # AI 추천 로직
│
├── ozang-batch             # Spring Batch 모듈
│   ├── config              # Job 설정 (RecommendJob, CleanupJob 등)
│   ├── processor           # ItemReader, Processor, Writer
│   └── service             # JobLauncher, ScheduleService 등
│
├── ozang-core              # 공통 모듈 (core library)
│   ├── entity              # JPA Entity (User, Product, Style 등)
│   ├── repository          # QueryDSL, CustomRepository
│   ├── service             # SecurityUtils, ValidationRules 등
│   ├── config              # JPA, QueryDSL, Security 설정
│   └── exception           # 공통 예외 처리 및 validation
```

## 👥  팀원 구성 & R&R
<div align="center">
  
| [**이용구 (팀장)**](https://github.com/reflash407) | **[김태현](https://github.com/9taetae9)** | **[김준우](https://github.com/normaldeve)** | **[이원길](https://github.com/realitsyourman)** |
|:---:|:---:|:---:|:---:|
| <img src="https://avatars.githubusercontent.com/u/86422079?v=4" width="100"> | <img src="https://avatars.githubusercontent.com/u/89383263?v=4" width="100"> | <img src="https://avatars.githubusercontent.com/u/128487020?v=4" width="100"> | <img src="https://avatars.githubusercontent.com/u/139864668?v=4" width="100"> |
| • 의상<br>• 알림<br>• 팔로우<br>• 발표 영상 | • 프로젝트 관리(자동화 workflow 구축, 멀티모듈 설계)<br>• 인프라(CI/CD 파이프라인, 시스템 아키텍처 설계)<br>• 날씨(캐싱, 배치 시스템 구현)<br>• DB 구축(ERD 설계, 스키마 관리) | • 사용자 인증/인가<br>• 사용자 도메인<br>• 배포 다이어그램 및 PPT 작성<br>• 모니터링 시스템 구축(Grafana, Prometheus)<br>• 프론트 코드 커스텀 | • 피드 기능<br>• 좋아요<br>• DM 기능<br>• 의상 추천 |
</div>


