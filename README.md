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
  - Spring boot
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
### ERD
![image](https://github.com/user-attachments/assets/1e843b7f-e5a0-4d64-9777-67a2b5056ded)
### 알림 기능 구현 
  - Redis Streams 를 이용한 알림 기능 구현
## 💡 서비스 구현 중 고민과 트러블 슈팅
     
