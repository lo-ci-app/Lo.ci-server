# 📍 Loci (로키) - Backend Server

> **Location-based Social Networking Service focusing on Real-world Intimacy.**
>
> "우리의 추억은 장소에 머문다."
> 친구들과 함께한 순간을 지도 위에 기록하고, 방문한 장소를 기반으로 서로의 친밀도를 확인하는 하이퍼로컬 SNS, **Loci**의 백엔드 리포지토리입니다.

<br>

## 🛠 Tech Stack

| Category | Technology | Description |
| --- | --- | --- |
| **Language** | ![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white) | Modern Java Features (Record, Switch Expression) |
| **Framework** | ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-6DB33F?style=flat-square&logo=springboot&logoColor=white) | RESTful API Server |
| **Database** | ![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white) | Main RDBMS (Spatial Data, User, Post) |
| **NoSQL** | ![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?style=flat-square&logo=redis&logoColor=white) | Caching (Feed, Friends), Refresh Token |
| **Chat** | ![Firebase](https://img.shields.io/badge/Firebase-Firestore-FFCA28?style=flat-square&logo=firebase&logoColor=black) | Real-time Chatting Service |
| **Infra** | ![AWS](https://img.shields.io/badge/AWS-EC2%20%7C%20S3-232F3E?style=flat-square&logo=amazon-aws&logoColor=white) | Server Hosting & Media Storage |
| **Spatial** | **Uber H3** | Hexagonal Hierarchical Spatial Indexing |

<br>

## ✨ Key Features

### 1. 🗺️ H3 기반 육각형 지도 클러스터링
- **Uber의 H3 Indexing** 기술을 도입하여 전 세계를 육각형 그리드(Beacon)로 나누어 관리합니다.
- 단순 좌표(Point) 기반 검색의 한계를 넘어, **구역(Area) 기반의 데이터 집계**를 통해 지도 로딩 속도를 획기적으로 개선했습니다.
- `UserBeaconStats` 테이블을 통한 **반정규화(Denormalization)** 설계로 수만 건의 마커 조회 쿼리를 **0.1초 이내**로 단축했습니다.

### 2. 🤝 동적 친밀도 시스템 (Intimacy System)
- 단순한 팔로우 관계를 넘어, 유저 간의 상호작용(게시글 태그, 댓글, 반응 등)을 점수화합니다.
- `IntimacyService`를 통해 실시간으로 친밀도 레벨을 계산하고, 레벨에 따라 **"콕 찌르기(Nudge)"** 등 차별화된 기능을 제공합니다.

### 3. 🎥 대용량 미디어 처리 최적화
- **AWS S3 Presigned URL** 방식을 도입하여 서버를 거치지 않고 클라이언트가 S3로 직접 파일을 업로드합니다.
- 이를 통해 백엔드 서버의 메모리 부하를 줄이고, 네트워크 대역폭 비용을 절감했습니다.
- 파일 업로드 시 **500MB 용량 제한 및 파일명 난수화/위생 처리**를 통해 보안성을 강화했습니다.

### 4. ⚡️ 고성능 피드 & 캐싱 전략
- **Global Caching (Redis):** 빈번하게 호출되는 '친구 목록'과 '유저 통계' 데이터에 `@Cacheable`을 적용하여 DB 부하를 최소화했습니다.
- **Push Notification:** 대용량 푸시 발송 시 DB 커넥션 점유 문제를 해결하기 위해 트랜잭션을 분리(`REQUIRES_NEW`)하여 안정성을 확보했습니다.

<br>

## 🏗 System Architecture & Optimization

### 🚀 Performance Tuning
Loci 백엔드는 대규모 트래픽을 고려한 다양한 최적화 기법이 적용되어 있습니다.

* **N+1 문제 해결:** `PostRepository` 및 `FriendshipRepository` 등 주요 조회 로직에서 `JOIN FETCH` 및 `BatchSize`를 활용하여 쿼리 수를 최적화했습니다.
* **동시성 제어:** `UserBeaconStats` 갱신 시 발생하는 동시성 문제를 해결하기 위해 **Native Query 기반의 Upsert (`ON DUPLICATE KEY UPDATE`)** 방식을 적용, 불필요한 Lock 대기를 제거했습니다.
* **비동기 처리:** 알림 발송, 통계 집계 등 응답 속도에 영향을 주지 않는 로직은 `Spring Event`(`@Async`, `@TransactionalEventListener`)를 통해 비동기로 처리하여 사용자 경험(UX)을 향상시켰습니다.

### 🔐 Security
* **JWT Authentication:** Access/Refresh Token 메커니즘을 통한 Stateless 인증.
* **AES-256 Encryption:** 민감한 개인정보(전화번호 등)는 DB 저장 시 암호화하여 보관합니다.

<br>

## 📂 Project Structure

```bash
com.teamloci.loci
├── domain
│   ├── auth          # 인증/인가 (JWT, Phone Login)
│   ├── chat          # 채팅 (Firestore 연동)
│   ├── friend        # 친구 관계 및 연락처 동기화
│   ├── intimacy      # 친밀도 시스템 (Gamification)
│   ├── notification  # FCM 푸시 알림
│   ├── post          # 게시글, 타임라인, 미디어
│   ├── settings      # 사용자 설정
│   ├── stat          # 지도 마커 통계 (H3 Beacon Stats)
│   └── user          # 사용자 프로필 및 활동 정보
└── global
    ├── auth          # Security Filter, Principal
    ├── common        # 공통 Response, Entity
    ├── config        # AWS, Redis, Swagger, Async 설정
    ├── error         # Global Exception Handling
    ├── infra         # 외부 인프라 (S3 File Upload)
    └── util          # GeoUtils(H3), AESUtil, Scheduler
