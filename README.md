# SweetBook — 코스튬 플레이어 포토북 제작 서비스 기획서

## 1. 서비스 소개

- 팬과 작가가 함께 참여하여 코스프레 모델의 매력을 한 권의 포토북으로 완성하는 **참여형 굿즈 제작 플랫폼**입니다. 흩어져 있는 팬들의 시선을 모아 크리에이터만의 브랜드 가치를 구축하고, 서브컬쳐 시장 내 새로운 수익 모델을 제시합니다.
- 타겟 수요층 : 
   1. 코스튬 플레이 촬영 및 행사 참여 경험이 풍부하고, 자신의 사진을 의미 있는 결과물로 남기고 싶은 유저
   2. 활동 기록을 정리하고 팬들과 공유하고 싶은 모델, 소규모 서클 및 아티스트 그룹


## 2. 실행 방법

### 백엔드 (Spring Boot)

#### 환경 변수 설정

```bash
cp .env.example .env
```
.env 파일 입력값 : 
   - SWEETBOOK_API_KEY= api 키 값
   - SWEETBOOK_ENV=sandbox
   - SWEETBOOK_CONTENTS_TEMPLATE_UID= 구글 포토북 A 표지지
   - SWEETBOOK_COVER_TEMPLATE_UID= 구글 포토북 A 내지 data A
   - SWEETBOOK_CONTENTS_BREAK_BEFORE=page
   - SWEETBOOK_API_BASE_URL= sanbox url

---
SWEETBOOK_ENV, SWEETBOOK_CONTENTS_BREAK_BEFORE는 위의 값 그대로 사용

### 프론트엔드 (Next.js)

#### 설치

```bash
cd frontend
npm install
```

#### 실행

```bash
npm run dev
```

브라우저에서 `http://localhost:3000` 접속

---


## 3. 사용한 API 목록

백엔드 `SweetbookApiClient`가 Sweetbook 서버에 호출하는 경로입니다. (베이스 URL에 `/v1` 등이 포함되면 아래는 그 이하 상대 경로.)

### Book API

| API | 용도 |
|-----|------|
| `GET /books` | 포토북 목록 조회 |
| `POST /books` | 포토북 생성 |
| `DELETE /books/{bookUid}` | 포토북 삭제 |
| `GET /books/{bookUid}/photos` | 북 사진 목록 조회 |
| `POST /books/{bookUid}/photos` | 사진 업로드 |
| `DELETE /books/{bookUid}/photos/{fileName}` | 사진 삭제 |
| `POST /books/{bookUid}/contents` | 내지 콘텐츠 추가 |
| `POST /books/{bookUid}/cover` | 표지 등록 |
| `POST /books/{bookUid}/finalization` | 포토북 최종 확정 |

### Order API

| API | 용도 |
|-----|------|
| `POST /orders` | 주문 생성 |
| `GET /orders` | 주문 목록 조회 |
| `GET /orders/{orderUid}` | 주문 상세 조회 |
| `POST /orders/{orderUid}/cancel` | 주문 취소 |
| `PATCH /orders/{orderUid}/shipping` | 배송지 정보 반영 |
| `POST /orders/estimate` | 주문 견적 |

### Credit API

| API | 용도 |
|-----|------|
| `POST /credits/sandbox/charge` | 샌드박스 크레딧 충전 |
| `GET /credits` | 크레딧 잔액 조회 |
| `GET /credits/transactions` | 크레딧 거래 내역 조회 |

## 4. AI 도구 사용 내역

| AI 도구 | 활용 내용용 |
|------|------|
| Claude Code | Sweetbook API 통신 환경 설계. 코드 리펙토링 | 
| Gemini | Sweetbook API 개발 문서 질의 및 개념 정리 | 


## 5. 설계 의도

- 현장 경험 기반의 문제 발굴
   - 서브컬쳐 행사를 참여하고 여러 종류의 굿즈 시장을 소비하며 포토북에 대한 수요를 확인했습니다. 그러나 현재의 거래 방식은 주로 구글 폼과 개인 계좌 이체에 의존하고 있어, 결제 확인의 불투명성, 주문 현황 관리의 어려움 등 소비자와 판매자 양측 모두가 불편을 겪는 구조임을 파악했습니다.
- 구매 프로세스의 신뢰성 확보 및 효율화
   - 위와 같은 수동 프로세스를 자동화하여 구매자에게는 거래의 불확실성을 해소해주고, 판매자에게는 체계적인 주문 제작 시스템을 제공함으로써 서비스의 신뢰도를 높이고자 했습니다
- 확장 가능한 비즈니스 모델 설계
   - 포토북 주문 제작을 확장하여 추후 P2P 거래 활성화 사진 펀딩 기능을 단계적으로 확장하여 서브컬쳐 크리에이터 생태계를 지원하는 종합 플랫폼으로 발전시키는 것을 목표로 합니다

---
