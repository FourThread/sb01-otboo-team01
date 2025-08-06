![header](https://capsule-render.vercel.app/api?type=cylinder&color=black&height=150&section=header&text=OZANG&fontSize=50&fontColor=d6ace6)

> 코드잇 스프린트 1기 최종 프로젝트
> 
> BE : 김준우, 김태현, 이용구, 이원길

## ⌚ Project Duration
25.06.23 ~ 25.07.31

## 👀 Project Overview
OZANG 프로젝트는 사용자가 등록한 옷을 날씨 기반으로 개인 맞춤형 의상 조합을 추천해주는 플랫폼 <br>
추천된 의상은 OOTD에 등록할 수 있으며, 다른 사용자의 스타일을 팔로우, 좋아요 및 댓글과 DM을 통해 함께 즐길 수 있습니다.
<img width="1872" height="993" alt="image" src="https://github.com/user-attachments/assets/26309ec3-4051-4e2d-b6b8-26ff1cece4f9" />

## ✨ Features

**사용자 관리**
- 관리자 계정 초기화 및 권한 관리 (ADMIN / USER)
- 계정 잠금 및 자동 로그아웃 처리
- JWT 기반 로그인/로그아웃 및 회원가입
- 임시 비밀번호 발급 및 만료 처리
- Google / Kakao 소셜 로그인 연동

**프로필 관리**
- 사용자 이미지, 성별, 생년월일, 위치, 온도 민감도 설정
- 위도/경도를 기반으로 날씨 API 및 카카오 API 연동

**의상 관리**
- 어드민: 의상 속성 등록
- 사용자: 구매 링크 기반 자동 등록 (크롤링 활용)
- 무신사, 29CM 등에서 자동 추출 가능

**날씨 데이터 관리**
- 기상청 단기 예보 Open API 기반 데이터 수집
- Spring Batch로 정기 작업 스케줄링
- 특이 기상 변화 시 사용자 알림 발송

**의상 추천**
- 날씨, 프로필, 등록 의상 데이터를 활용한 추천 기능
- 커스텀 알고리즘 및 LLM(OpenAI, HuggingFace 등) 활용 가능

**OOTD 피드**
- 추천된 의상 조합을 피드에 등록
- 좋아요 및 댓글 기능 제공
- 내 피드에 대한 반응에 실시간 알림 전송

**팔로우 & DM**
- 사용자 간 팔로우 및 알림 연동
- 웹소켓 기반 DM(실시간 채팅) 기능 제공
- 사용자 ID 기반 DM Key 생성 및 채널 구독 방식

**알림 시스템**
- Server-Sent Events(SSE) 기반 실시간 알림
- 다음 이벤트에 알림 발송:
  - 권한 변경
  - 피드 좋아요 및 댓글
  - 팔로우 / 피드 등록
  - DM 수신
  - 의상 속성 추가

## 🌱 Tech Stack
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

**ETC**  

<img src="https://img.shields.io/badge/swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black"> 
<img src="https://img.shields.io/badge/env-ECD53F?style=for-the-badge&logo=dotenv&logoColor=black"> 

</div>

## 🖥️ System Architecture
<img width="1632" height="1139" alt="스크린샷 2025-07-31 105851" src="https://github.com/user-attachments/assets/fe9dcc42-06b3-451b-8679-6129cb45c7ec" />

## 📁 File Structure
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

## 👤 Member Introduce R&R

| 이용구 👑 | 김태현 | 김준우 | 이원길 |
|:---:|:---:|:---:|:---:|
| <img src="https://avatars.githubusercontent.com/u/86422079?v=4" width="100"> | <img src="https://avatars.githubusercontent.com/u/89383263?v=4" width="100"> | <img src="https://avatars.githubusercontent.com/u/128487020?v=4" width="100"> | <img src="https://avatars.githubusercontent.com/u/139864668?v=4" width="100"> |
| [**이용구 (팀장)**](https://github.com/reflash407) | **[김태현](https://github.com/9taetae9)** | **[김준우](https://github.com/normaldeve)** | **[이원길](https://github.com/realitsyourman)** |
| • 의상<br>• 알림<br>• 팔로우<br>• 발표 영상 | • 프로젝트 관리<br>• 인프라 관리<br>• 날씨 기능<br>• DB 구축 | • 사용자 인증/인가<br>• 사용자 도메인<br>• 배포 다이어그램 및 PPT 작성<br>• 인프라 구성 (Grafana, Prometheus)<br>• 프론트 코드 커스텀 | • 피드 기능<br>• 좋아요<br>• DM 기능<br>• 의상 추천 |


