# Spring Boot REST API - Restaurant Backend

Next.js 프론트엔드에서 사용하는 Spring Boot 기반 REST API 서버입니다.  
현재 수업자료 버전업에 맞춰 **Spring Boot 4.1.0**, **Java 21**, **Spring Security + JWT + Refresh Token** 구조로 정리되어 있습니다.

## 현재 기준 버전

| 항목 | 버전/방식 |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.0 |
| Dependency Management | 1.1.7 |
| Spring Security | SecurityFilterChain 기반 |
| JWT | JJWT 0.12.6 |
| ORM | Spring Data JPA + Hibernate |
| Database | MySQL |
| Password | BCrypt |
| Frontend 연동 | Next.js 16 / React 19 기준 |

## 프로젝트 개요

이 프로젝트는 상품 조회, 상품 관리, 주문, 리뷰, 회원 인증 기능을 제공하는 REST API 서버입니다.

보안 구조는 `09_Springframework-main`의 최신 Spring Security 예제 흐름을 restaurant 도메인에 맞게 적용했습니다.

핵심 변경점:

- 기존 `WebSecurityConfig`, 커스텀 로그인 필터, JWT 인터셉터 방식 제거
- `SecurityFilterChain` 기반 보안 설정으로 변경
- `JwtAuthenticationFilter extends OncePerRequestFilter` 적용
- 로그인 전용 `LoginRequest` DTO 사용
- Access Token + Refresh Token 발급
- Refresh Token을 HttpOnly Cookie와 DB에 저장
- `TBL_REFRESH_TOKEN` 테이블 추가
- Hibernate naming strategy를 대문자 테이블명 기준으로 고정

## 기술 스택

### Backend

- Spring Boot 4.1.0
- Java 21 toolchain
- Spring Security
- Spring Data JPA
- MySQL Connector/J
- JJWT 0.12.6
- ModelMapper 3.1.1
- Commons IO 2.18.0
- Lombok
- JUnit 5

### Build

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.1.0'
    id 'io.spring.dependency-management' version '1.1.7'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

## 프로젝트 구조

```text
src/main/java/com/ohgiraffers/restapi/
├── auth/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── WebConfig.java
│   │   └── BeanConfiguration.java
│   ├── entity/
│   │   └── RefreshToken.java
│   ├── jwt/
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenProvider.java
│   │   ├── RestAccessDeniedHandler.java
│   │   └── RestAuthenticationEntryPoint.java
│   └── repository/
│       └── RefreshTokenRepository.java
├── common/
├── exception/
├── member/
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── MemberController.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── MemberDTO.java
│   │   └── TokenDTO.java
│   ├── entity/
│   ├── repository/
│   └── service/
│       ├── AuthService.java
│       ├── CustomUserDetailsService.java
│       └── MemberService.java
├── product/
├── purchase/
├── review/
└── util/
```

## 보안 구조

### 1. 로그인

로그인 요청은 `MemberDTO` 전체가 아니라 `LoginRequest`로 받습니다.

```json
{
  "memberId": "admin",
  "memberPassword": "1234"
}
```

이유:

- 로그인에는 `memberId`, `memberPassword`만 필요합니다.
- `MemberDTO`에는 `int memberCode` 같은 primitive 필드가 있어, 요청 JSON에 `null`이 들어오면 Jackson 파싱 단계에서 실패할 수 있습니다.
- 따라서 로그인 요청 전용 DTO를 사용하는 방식이 더 안전합니다.

### 2. 비밀번호 검증

DB에는 평문 비밀번호가 아니라 BCrypt 해시가 저장되어야 합니다.

```java
passwordEncoder.matches(request.getMemberPassword(), member.getPassword())
```

초기 데이터 기준:

- 로그인 ID: `admin`
- 로그인 PW: `1234`
- DB 저장값: BCrypt 해시

### 3. 토큰 발급

로그인 성공 시 서버는 두 토큰을 발급합니다.

| 토큰 | 용도 | 전달 방식 |
| --- | --- | --- |
| Access Token | 보호 API 인증 | 응답 body |
| Refresh Token | Access Token 재발급 | HttpOnly Cookie + DB 저장 |

응답 예시:

```http
HTTP/1.1 200 OK
Set-Cookie: refreshToken=eyJhbGciOi...; HttpOnly; Path=/; SameSite=Strict
```

```json
{
  "status": 200,
  "message": "로그인 성공",
  "userInfo": {
    "grantType": "Bearer",
    "memberName": "관리자",
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi..."
  }
}
```

### 4. 보호 API 요청

로그인 후 보호 API를 호출할 때는 Access Token을 `Authorization` 헤더에 넣습니다.

```http
Authorization: Bearer eyJhbGciOi...
```

처리 흐름:

1. `JwtAuthenticationFilter`가 요청을 가로챕니다.
2. `Authorization` 헤더에서 Bearer Token을 꺼냅니다.
3. `JwtTokenProvider`가 서명과 만료 시간을 검증합니다.
4. 토큰의 subject에서 `memberId`를 꺼냅니다.
5. `CustomUserDetailsService`가 DB에서 회원과 권한을 조회합니다.
6. `SecurityContextHolder`에 `Authentication` 객체를 저장합니다.
7. `SecurityConfig`의 인가 규칙에 따라 요청을 허용하거나 차단합니다.

### 5. Refresh Token

Refresh Token은 서버 DB에도 저장됩니다.

```text
TBL_REFRESH_TOKEN
├── MEMBER_ID
├── TOKEN
└── EXPIRY_DATE
```

재발급 흐름:

1. 클라이언트가 `/auth/refresh` 요청
2. 브라우저가 HttpOnly `refreshToken` 쿠키 자동 전송
3. 서버가 JWT 자체 검증
4. DB의 저장 토큰과 쿠키 토큰 비교
5. 새 Access Token과 Refresh Token 발급
6. DB와 Cookie 갱신

### 6. Logout

로그아웃 시:

- DB의 Refresh Token 삭제
- 브라우저의 Refresh Token Cookie 삭제

```http
Set-Cookie: refreshToken=; Max-Age=0; HttpOnly; Path=/; SameSite=Strict
```

## SecurityConfig 인가 규칙

현재 주요 규칙:

| Method | Path | 권한 |
| --- | --- | --- |
| `OPTIONS` | `/**` | 전체 허용 |
| `POST` | `/auth/signup` | 허용 |
| `POST` | `/auth/login` | 허용 |
| `POST` | `/auth/refresh` | 허용 |
| `POST` | `/auth/logout` | 허용 |
| `GET` | `/api/v1/products`, `/api/v1/products/**` | 허용 |
| `GET` | `/api/v1/reviews/**` | 허용 |
| `GET` | `/api/v1/products-management/**` | `ROLE_ADMIN` |
| `POST` | `/api/v1/products` | `ROLE_ADMIN` |
| `PUT` | `/api/v1/products` | `ROLE_ADMIN` |
| 전체 | `/api/v1/purchase/**` | 인증 필요 |
| 전체 | `/api/v1/members/**` | 인증 필요 |
| `POST`, `PUT` | `/api/v1/reviews` | 인증 필요 |

## API 엔드포인트

### 인증

| Method | Endpoint | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/auth/signup` | 회원가입 | 필요 없음 |
| `POST` | `/auth/login` | 로그인 | 필요 없음 |
| `POST` | `/auth/refresh` | 토큰 재발급 | Refresh Cookie |
| `POST` | `/auth/logout` | 로그아웃 | Refresh Cookie |

### 회원

| Method | Endpoint | 설명 | 인증 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/members/{memberId}` | 회원 정보 조회 | 필요 |

### 상품

| Method | Endpoint | 설명 | 인증 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/products` | 상품 목록 조회 | 필요 없음 |
| `GET` | `/api/v1/products/{productCode}` | 상품 상세 조회 | 필요 없음 |
| `GET` | `/api/v1/products/search?s={keyword}` | 상품 검색 | 필요 없음 |
| `GET` | `/api/v1/products/meals` | 식사 카테고리 조회 | 필요 없음 |
| `GET` | `/api/v1/products/dessert` | 디저트 카테고리 조회 | 필요 없음 |
| `GET` | `/api/v1/products/beverage` | 음료 카테고리 조회 | 필요 없음 |
| `GET` | `/api/v1/products-management/**` | 관리자 상품 조회 | `ROLE_ADMIN` |
| `POST` | `/api/v1/products` | 상품 등록 | `ROLE_ADMIN` |
| `PUT` | `/api/v1/products` | 상품 수정 | `ROLE_ADMIN` |

### 주문

| Method | Endpoint | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/purchase` | 상품 주문 | 필요 |
| `GET` | `/api/v1/purchase/{memberId}` | 회원 주문 내역 조회 | 필요 |

### 리뷰

| Method | Endpoint | 설명 | 인증 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/reviews/{productCode}` | 상품 리뷰 조회 | 필요 없음 |
| `POST` | `/api/v1/reviews` | 상품 리뷰 등록 | 필요 |
| `PUT` | `/api/v1/reviews` | 상품 리뷰 수정 | 필요 |

## 환경 설정

기본 프로필은 `application.yml`에서 `dev`를 include합니다.

```yaml
spring:
  application:
    name: springboot-restapi
  profiles:
    include: dev
```

`application-dev.yml` 주요 설정:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/springreact?serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
    username: ${SPRING_DATASOURCE_USERNAME:user01}
    password: ${SPRING_DATASOURCE_PASSWORD:pass01}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none
      naming:
        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
        implicit-strategy: org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        show_sql: true
        format_sql: true

jwt:
  key: ${JWT_SECRET:c3ByaW5nLWJvb3QtNC1kZXYtc2VjcmV0LWtleS0zMi1ieXRlcy1mb3ItaHMyNTY=}
  time: ${JWT_ACCESS_EXPIRATION:1800000}
  refresh-time: ${JWT_REFRESH_EXPIRATION:604800000}
```

### 환경 변수

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | MySQL 접속 URL | `jdbc:mysql://localhost:3306/springreact...` |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자 | `user01` |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | `pass01` |
| `JWT_SECRET` | Base64 인코딩된 HS256 secret | 개발용 기본값 |
| `JWT_ACCESS_EXPIRATION` | Access Token 만료 시간(ms) | `1800000` |
| `JWT_REFRESH_EXPIRATION` | Refresh Token 만료 시간(ms) | `604800000` |
| `IMAGE_DIR` | 이미지 저장 경로 | `src/main/resources/static/productimgs/` |
| `IMAGE_URL` | 이미지 URL prefix | `/productimgs/` |

운영 환경에서는 반드시 `JWT_SECRET`을 별도 값으로 교체해야 합니다.

## 데이터베이스

초기 스키마와 데이터는 `initdb/01-init.sql`을 사용합니다.

```sql
CREATE DATABASE springreact;
USE springreact;
```

```bash
mysql -u user01 -p springreact < initdb/01-init.sql
```

주요 테이블:

| 테이블 | 설명 |
| --- | --- |
| `TBL_MEMBER` | 회원 정보 |
| `TBL_AUTHORITY` | 권한 정보 |
| `TBL_MEMBER_ROLE` | 회원-권한 관계 |
| `TBL_REFRESH_TOKEN` | Refresh Token 저장 |
| `TBL_PRODUCT` | 상품 정보 |
| `TBL_CATEGORY` | 상품 카테고리 |
| `TBL_ORDER` | 주문 정보 |
| `TBL_REVIEW` | 상품 리뷰 |

### 테이블명 대소문자 주의

수업자료와 엔티티는 대문자 테이블명을 사용합니다.

```sql
SELECT * FROM TBL_MEMBER;
```

MySQL이 Linux/Docker 환경에서 실행되면 테이블명 대소문자를 구분할 수 있습니다.  
따라서 `tbl_member`가 아니라 `TBL_MEMBER`로 조회해야 합니다.

현재 Hibernate 설정도 명시된 대문자 테이블명을 그대로 사용하도록 고정되어 있습니다.

```yaml
spring:
  jpa:
    hibernate:
      naming:
        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
```

## 실행 방법

### Windows PowerShell

```powershell
cd C:\Users\brewfy\Documents\springboot-workspace\spring\restaurant-backend
.\gradlew.bat bootRun
```

### 빌드

```powershell
.\gradlew.bat build
```

### JAR 실행

```powershell
java -jar .\build\libs\springboot-restapi-0.0.1-SNAPSHOT.jar
```

Java 21 런타임으로 실행해야 합니다. Java 17로 실행하면 다음 오류가 발생할 수 있습니다.

```text
UnsupportedClassVersionError:
class file version 65.0, this version only recognizes up to 61.0
```

## 로그인 테스트

초기 데이터 기준 admin 계정:

```http
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "memberId": "admin",
  "memberPassword": "1234"
}
```

보호 API 호출 예:

```http
POST http://localhost:8080/api/v1/products
Authorization: Bearer eyJhbGciOi...
```

## 09 예제와의 관계

`09_Springframework-main`의 보안 구조와 restaurant-backend의 보안 구조는 같은 방식입니다.

| 개념 | 09 예제 | restaurant-backend |
| --- | --- | --- |
| 로그인 URL | `/api/v1/auth/login` | `/auth/login` |
| 사용자 식별자 | `username` | `memberId` |
| 로그인 DTO | `LoginRequest(username, password)` | `LoginRequest(memberId, memberPassword)` |
| 사용자 도메인 | `User` | `Member` |
| 권한 | role enum / `USER` | `ROLE_ADMIN`, `ROLE_USER` |
| JWT 필터 | `JwtAuthenticationFilter` | `JwtAuthenticationFilter` |
| 토큰 제공자 | `JwtTokenProvider` | `JwtTokenProvider` |
| Refresh 저장 | `RefreshTokenRepository` | `TBL_REFRESH_TOKEN` |

정확한 설명:

> 09 예제에서 배운 최신 Spring Security + JWT 구조를 restaurant-backend의 Member 도메인에 맞게 적용한 버전입니다.

## 수업용 시각화 자료

JWT 인증 흐름 설명용 HTML 자료:

```text
C:\Users\brewfy\Desktop\jwt-flow.html
```

포함 내용:

- 로그인 발급 흐름
- JWT Header/Payload/Signature 설명
- 보호 API 요청 흐름
- Refresh Token / Logout 흐름
- 09 예제와 restaurant-backend 매핑
- 401/403/DTO 파싱 오류 등 실패 케이스

## 자주 나는 오류

### 1. Cannot map null into type int

원인:

- 로그인 요청을 `MemberDTO` 전체로 받을 때, `memberCode: null` 같은 값이 들어오면 primitive `int` 필드에서 Jackson 파싱 실패가 납니다.

해결:

- 로그인은 `LoginRequest`로 받습니다.

```json
{
  "memberId": "admin",
  "memberPassword": "1234"
}
```

### 2. admin / 1234 로그인 실패

확인할 것:

- `MEMBER_PASSWORD`가 평문 `1234`가 아니라 BCrypt 해시인지 확인
- 요청 body 필드명이 `memberId`, `memberPassword`인지 확인
- 서버를 수정 후 재시작했는지 확인

### 3. TBL_MEMBER는 조회되는데 tbl_member는 안 됨

원인:

- MySQL 실행 환경에 따라 테이블명 대소문자를 구분합니다.

해결:

- 수업자료 기준으로 대문자 테이블명 사용
- JPA naming strategy는 `PhysicalNamingStrategyStandardImpl` 사용

### 4. Java 버전 오류

원인:

- Java 21로 컴파일한 클래스를 Java 17 런타임으로 실행함

해결:

- IntelliJ Gradle JVM과 Run JVM을 Java 21로 변경
- 터미널 `java -version` 확인

## 보안 메모

- 운영 환경에서는 개발용 `JWT_SECRET`을 사용하지 않습니다.
- JWT Payload에는 비밀번호, 주민번호, 카드번호 등 민감정보를 넣지 않습니다.
- Access Token은 짧게, Refresh Token은 HttpOnly Cookie와 서버 저장소로 관리합니다.
- Refresh Token을 DB에 저장하는 이유는 로그아웃/폐기/재발급 통제를 서버가 하기 위해서입니다.
- 관리자 API는 `ROLE_ADMIN` 권한을 요구합니다.
