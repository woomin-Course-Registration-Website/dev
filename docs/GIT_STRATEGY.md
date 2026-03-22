# Git 브랜치 전략

## 브랜치 구조

```
main
 └── develop
      ├── feature/EP-01-auth
      ├── feature/EP-02-grade
      ├── feature/EP-03-student-record
      ├── feature/EP-04-feedback
      ├── feature/EP-05-counseling
      ├── feature/EP-06-notification
      └── feature/EP-07-report
```

## 브랜치 용도

| 브랜치 | 용도 | 병합 방식 |
|--------|------|---------|
| `main` | 배포용 (안정 버전) | PR only (squash merge) |
| `develop` | 개발 통합 브랜치 | PR only |
| `feature/EP-XX-기능명` | 기능 개발 | develop으로 PR |
| `hotfix/이슈명` | 긴급 버그 수정 | main + develop 동시 병합 |

## 커밋 컨벤션

```
[TYPE] 한글 설명

TYPE 목록:
  feat     - 새로운 기능 추가
  fix      - 버그 수정
  docs     - 문서 수정
  refactor - 코드 리팩토링
  test     - 테스트 코드
  chore    - 빌드/설정 변경
```

**예시:**
```
[feat] 학생 성적 입력 API 구현
[fix] JWT 토큰 만료 처리 오류 수정
[docs] API 설계 문서 업데이트
```

## 작업 흐름

```
1. develop에서 feature 브랜치 생성
   git checkout develop
   git checkout -b feature/EP-01-auth

2. 기능 개발 후 커밋
   git add .
   git commit -m "[feat] 로그인 API 구현"

3. develop으로 PR 생성 후 병합
   (코드 리뷰 후 병합)

4. Sprint 완료 시 develop → main PR 생성
```
