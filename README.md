# People of Delivery — Phase 3 (Event-Driven & Cloud-Native)

## 프로젝트 소개
2차에서 분해한 MSA를 기반으로 **이벤트 기반 아키텍처(EDA)** 를 적용하고, **재고/주문 동시성**, **분산 일관성**, **장애 대응**을 고도화한 단계입니다.  
추가적으로, **Kubernetes** 로 이관하고 **관측성(Observability)** 스택을 구축해 운영 신뢰성과 확장성을 확보했습니다.

## 개발 기간
- 25.09.02 ~ 25.09.19

## 개발 구성원 및 역할

### 인프라
#### [조성규](https://github.com/sungchilll), [김지윤](https://github.com/JIYOOnii007), [정병민](https://github.com/ByeongminJeong)
- **클러스터 & 네트워킹**: Kubernetes/Ingress, Service, HPA, 네트워크 정책
- **릴리즈 전략**: Helm/Kustomize 템플릿, Blue-Green/Rolling 업데이트
- **CI/CD**: Jenkins(or GitHub Actions) → ECR → EKS(**Argo Rollouts**) 배포, IRSA 권한 위임
- **시크릿 & 구성**: Secret/ConfigMap, KMS/Secrets Manager
- **관측성**: Prometheus/Grafana(메트릭), Loki(or ELK), OpenTelemetry/Tempo(트레이스), Alertmanager→Slack
- **로그 파이프라인**: Fluent Bit DaemonSet → Kinesis Firehose → S3(아카이빙) + Loki(실시간)
> **산출물**: 배포 차트/매니페스트, 파이프라인, 대시보드, Runbook

### 백엔드 개발
#### [김준형](https://github.com/jh010303), [모시은](https://github.com/shiien14)
- **이벤트 기반 아키텍처**: Kafka Producer/Consumer, **Outbox 패턴**, **Saga(보상 트랜잭션)**
- **동시성/일관성**: Redis+Lua, 낙관적 락, 분산 락, 멱등성 키
- **장애 대응**: 재시도/백오프, **DLQ** 격리, 알림 훅(Discord)
- **도메인 서비스**: 주문/재고/결제 이벤트 설계, 컨슈머 계약·상태 전이
- **내부 토큰(Passport)**: Gateway 교환 패턴으로 내부 신뢰 축 단일화
> **산출물**: 서비스 코드/테스트, 이벤트 스키마/토픽 규약, 장애 대응 전략 문서

## 기술 스택
- **언어/런타임**: Java 21, Gradle(멀티프로젝트)
- **프레임워크**: Spring Boot, Spring MVC, Spring Data JPA, Spring Security
- **메시징/이벤트**: **Apache Kafka**, **Outbox**(DB→Kafka 릴레이)
- **사가/일관성**: 오케스트레이션·코레오그래피 혼합
- **데이터**: PostgreSQL, Redis(락/캐시/토큰), MongoDB(조회)
- **보안**: JWT(Access/Refresh), OAuth2(소셜)
- **플랫폼/배포**: **Kubernetes**, Docker, Helm/Kustomize, Argo Rollouts
- **관측성**: Prometheus/Grafana, Loki(or ELK), OpenTelemetry/Tempo, Alertmanager
- **문서/테스트**: Swagger/OpenAPI, JUnit 5

## 핵심 목표
1. **신뢰 가능한 이벤트 발행/소비**: Outbox → Kafka 릴레이, 멱등 소비, 재시도/백오프, DLQ 분리
2. **재고·주문 동시성 & 분산 일관성**: 낙관적 락 + 분산 락 조합, Saga 보상 로직으로 최종 일관성 확보
3. **장애 복원력 강화**: 파티션 레벨 재처리, 실패 격리, 실시간 알림·대시보드
4. **클라우드-네이티브 운영**: K8s 배포 자동화, HPA로 수요 대응, 설정/시크릿 표준화
5. **관측성 체계화**: 지표/로그/트레이스 상관 분석으로 MTTR 단축과 병목 지점 가시화

## 주요 기능
- **이벤트 아키텍처**
  - 주문/결제/재고 등 도메인 이벤트 정의
  - 트랜잭션 내 **Outbox** 기록 → 릴레이가 **Kafka 토픽**으로 발행
  - 컨슈머는 **멱등 처리**(메시지 ID+컨슈머 그룹) 및 **파티션 내 순서 보존**
- **Saga(보상 트랜잭션)**
  - 성공 플로우: `OrderCreated → ReserveInventory → PaymentCreated → PaymentAuthorized → InventoryCommitted → ApproveOrder`
  - 실패 시 보상 플로우: 재고/주문 상태 롤백 및 정합성 회복
- **동시성 제어**
  - **Redis + Lua**: 원자적 수량 감소/해제, 단기 임계구역 보호
  - **낙관적 락(PostgreSQL)**: 결제 성공 시 최종 커밋
  - **분산 락(선택)**: 경합이 높은 자원 보호
- **오류/장애 처리**
  - 컨슈머 재시도(고정/지수 백오프) → 실패 시 **DLQ** 격리, 알림 연동
- **내부 보안(Passport)**
  - Gateway에서 외부 JWT 검증 후 내부 **Passport**(최소 Claim: `userId`, `role`)로 교환·전파
- **조회 최적화**
  - **MongoDB Projection**으로 필요한 필드만 읽어 과다 페치 제거
- **API 문서/테스트**
  - 서비스별 Swagger/OpenAPI, JUnit 5 기반 단위/통합 테스트

## 아키텍처 하이라이트
- **서비스 경계(예)**: `apigateway`, `discovery`, `auth-service`, `user-service`, `store-service`, `cart-service`, `payment-service`, `ai-service`, `module-common`
- **이벤트 흐름**: Service(DB 트랜잭션) → **Outbox** 적재 → 릴레이 → **Kafka** → 컨슈머(사가/도메인 리액션)
- **일관성 전략**: 로컬 트랜잭션 + 비동기 이벤트로 최종 일관성, 실패 시 보상
- **동시성 전략**: 낙관적 락 + 분산 락 + 멱등 재처리
- **플랫폼/배포**: K8s, Argo Rollouts, IRSA, HPA/프로브/리소스 한계 설정
- **관측성**: 메트릭/로그/트레이스 3종 연동, Alertmanager→Slack

## 디렉토리 구조
```
Project3_MSA_people_of_delivery/
├─ apigateway/ # API Gateway
├─ discovery/ # Eureka
├─ auth-service/ # 인증/인가
├─ user-service/ # 회원
├─ store-service/ # 상점/메뉴
├─ cart-service/ # 장바구니
├─ payment-service/ # 결제
├─ ai-service/ # 추천/설명 등
├─ module-common/ # 공통 DTO/에러/유틸
├─ build.gradle
├─ settings.gradle
└─ README.md
```

## 인프라 구성도

<img width="2286" height="1101" alt="스크린샷 2025-09-21 오전 1 57 43" src="https://github.com/user-attachments/assets/86e68c0b-6a2b-4a00-9a92-5dfb65a4f289" />

## 운영/관측 상세 (EKS & Observability)
- **EKS 노드그룹 분리**: `SYSTEM`(배포/운영 도구), `OBS`(Prometheus/Loki/Grafana), `DATA`(Redis 등), `APP`(업무 서비스)
- **로그 파이프라인**: Fluent Bit DaemonSet → Kinesis Firehose → S3 아카이빙 + Kinesis/Lambda → **Loki** 실시간 조회
- **메트릭/알림**: **Prometheus + Grafana** 대시보드, **Alertmanager → Slack** 알림  
  *메트릭과 로그를 상관 분석해 근본 원인 분석(RCA) 속도 향상.*

## CI/CD 파이프라인
- **Jenkins(or GitHub Actions) → ECR → EKS(Argo Rollouts)**  
  Git 푸시 시 이미지 빌드/스캔/푸시 → Rollout 리소스로 선언적 Blue-Green/Rolling 배포  
- **권한 위임**: **IRSA** 기반으로 K8s·스토리지·레지스트리 접근 제어  

## 이벤트·일관성 상세 (Saga / Outbox / 멱등)
- **도메인 이벤트 플로우**: 주문-재고-결제의 상태 전이를 이벤트로 표현하고, 오케스트레이터가 진행 상태를 추적
- **Outbox 패턴**: 트랜잭션 내 Outbox 적재, 커밋 후 퍼블리시(예: `@TransactionalEventListener` + Relay), 발행 상태 추적/재시도
- **멱등성 보장**: `processed_message`(메시지ID+컨슈머 그룹)로 중복 처리 방지
- **에러 격리**: 컨슈머 재시도 후 **DLQ**로 격리, 알림 훅으로 즉시 대응

## 재고·동시성 전략
- **Redis + Lua**: 재고 감소/해제의 원자적 연산, TTL로 홀드 만료 처리
- **최종 커밋**: 결제 성공 시 PostgreSQL **낙관적 락** 기반 커밋, Redis는 **write-through** 동기화

## 보안/내부 토큰 (Passport)
- **Gateway 교환 패턴**: 외부 JWT 검증 후 내부 **Passport**(최소 Claim: `userId`, `role`)로 교환·전파  
  키·정책 중앙화, 최소 권한, 내부 트러스트 도메인 확립

## 데이터 조회 최적화
- **MongoDB Projection**: 필요한 필드만 읽는 Projection으로 과다 페치 제거, 응답 시간 단축

## 프로젝트 운영 & 관리
- **환경 분리**: `application.yml`(로컬/클라우드), Git 브랜치(`develop`/`cloud`) 이원화
- **인프라 구성 분리**: 공통 리소스는 콘솔 관리, **RDS/DocumentDB/MSK는 IaC(Terraform)** 로 선언적 관리
- **팀 운영 방식**: 오전/오후 데일리 스크럼, Notion/이슈 트래커로 이슈·해결 내역 축적(재발 방지)

## Commit Message Convention

| Tag Name            | Description                                                                                     |
|--------------------|-------------------------------------------------------------------------------------------------|
| :sparkles: Feat    | 새로운 기능 추가                                                                                |
| :bug: Fix          | 버그 수정                                                                                        |
| :art: Style        | 코드 포맷 변경 등(기능 변경 없음)                                                                |
| :hammer: Refactor  | 리팩토링(프로덕션 로직 개선)                                                                      |
| :memo: Docs        | 문서 수정                                                                                        |
| :test_tube: Test   | 테스트 코드 추가/수정(프로덕션 코드 변경 없음)                                                    |
| :rocket: Chore     | 빌드/배포/의존성/설정 등 변경(프로덕션 로직 변경 없음)    

