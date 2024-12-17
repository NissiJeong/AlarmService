# 💻 알람 서비스
## 일정 계획
|12.14(토)|12.15(일)|12.16(월)|12.16(화)|
|:---:|:---:|:---:|:---:|
|프로젝트 기회<br>도커,스프링 기본세팅<br>기본구현 완료| |테스트 코드 작성<br>Optional 기능 구현|Optional 기능 구현|

## 💡 프로젝트 환경
### Spring Boot 프로젝트 구축
  - Java17
  - Spring Boot 3.3.0
  - Gradle
  - JPA
  - MySQL
  - Redis
### Dockerfile 및 docker-compose.yml 세팅
  - Spring Boot
  - MySQL
  - Redis
## 💡 프로젝트 설명 및 기술적 요구사항
### 프로젝트 설명
  - 상품이 재입고 되었을 때, 재입고 알림을 설정한 유저들에게 재입고 알림
### 비즈니스 요구사항
  - 재입고 알림을 전송하기 전, 상품의 재입고 회차를 1 증가
  - 상품이 재입고 되었을 때, 재입고 알림을 설정한 유저들에게 알림 메시지를 전달
  - 재입고 알림은 재입고 알림을 설정한 유저 순서대로 메시지 전송
  - 회차별 재입고 알림을 받은 유저 목록 저장
  - 재입고 알림을 보내던 중 재고가 모두 없어진다면 알림 중단
  - 재입고 알림 전송의 상태를 DB 에 저장
### 기술적 요구사항
  - 알림 메시지는 1초에 최대 500개의 요청
  - MySQL 조회 시 인덱스를 잘 탈 수 있게 설계
  - 설계해야 할 테이블 목록: Product, ProductNotificationHistory, ProductUserNotification, ProductUserNotificationHistory
  - (Optional) 예외에 의해 알림 메시지 발송이 실패한 경우, manual 하게 상품 재입고 알림 메시지를 다시 보내는 API를 호출한다면 마지막으로 전송 성공한 이후 유저부터 다시 알림 메시지 전송
  - (Optional) 테스트 코드 작성
## 💡 기획
### 데이터베이스 설계 및 ERD
  - 다른 기능 집중을 위해 user 데이터는 더미 데이터로 진행(요청이 들어온 사용자는 인증/인가 완료된 상태로 가정)
  - 4개 테이블 관리
      - Product: 상품관리
      - ProductNotificationHistory: 상품별 재입고 알림 히스토리
      - ProductUserNotification: 상품별 재입고 알림을 설정한 유저
      - ProductUserNotificationHistory: 상품 + 유저별 알림 히스토리<br>
[ERD]<br>
![image](https://github.com/user-attachments/assets/1e843b7f-e5a0-4d64-9777-67a2b5056ded)
### 알림 기능 구현 및 flow
(1) 상품 재입고 회차 1 증가 &rarr; (2) 재입고 알림 설정한 유저들에게 메시지 전달(Redis Streams 사용) &rarr; (3) 알림 전송 성공한 유저 저장 <br>
<br>
(1) 에서 고려 사항<br>
<br>
(2) 에서 고려 사항<br>
  - 알림을 설정한 유서 순서대로 메시지 전송: 생성 일자로 ASC<br>
  - 재입고 알림을 보내던 중 재고가 모두 없어진다면 알림 중단<br>
    추후 다시 재입고 회차가 증가하면 끊어진 사용자부터 알림 재발송 &rarr; 알림 발송 시마다 Redis 에 마지막 알림 발송 사용자 update &rarr; 알림 종료된 시점에 MySQL 과 동기화<br>
  - 예외에 의해 알림이 실패할 경우 manual 하게 상품 재입고 알림 메시지를 다시 보내는 API를 호출한다면 끊어진 사용자부터 알림 재발송<br>
    예외 발생하기 전 Redis 와 MySQL 동기화 &rarr; API 호출 &rarr; 끊어진 사용자부터 알림 재발송<br>
<br>
(3) 에서 고려 사항<br>
  - 회차별 재입고 알림을 받은 유저 목록 저장: ProductUserNotificationHistory 테이블에 저장
<br>

## 💡 서비스 구현 중 고민과 트러블 슈팅
### 문제
- 알림 보내는 로직 500개 데이터를 테스트 해보니 1600ms 이상 소요
- 500개의 알림을 보낼 때마다 재고상태를 확인하다보니 아무리 Redis 에서 가져온다고 하더라도 1초 안으로 처리 안되는 듯
### 해결 과정
- 최초 데이터 조회 후 500번 반복문을 테스트해보니 425ms 소요
- 500번 반복될 때마다 Redis 에 저장하지 않고 끝난 후 데이터베이스에 저장해야 할 것 같은데, 그렇게 되면 중간에 재고상태가 sold out 이 되면 해당 반복을 멈추게 하는 로직이 필요
