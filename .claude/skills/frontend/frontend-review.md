# Frontend Code Review

프론트엔드 코드 리뷰 체크리스트

## Contract

| Aspect | Description |
|--------|-------------|
| Input | Git diff 또는 특정 파일 |
| Output | 이슈 목록 (심각도별 분류) |
| Required Tools | git |
| Verification | 모든 Critical 이슈 해결 |

## Checklist

### Security (Critical)
- [ ] XSS: dangerouslySetInnerHTML 사용 여부
- [ ] 토큰 노출: localStorage vs httpOnly cookie
- [ ] 환경 변수: NEXT_PUBLIC_ prefix 확인 (클라이언트 노출)
- [ ] API key/secret 클라이언트 번들에 포함 여부

### Performance (High)
- [ ] 불필요한 'use client' 사용 (Server Component 가능한데)
- [ ] 큰 번들 import (lodash 대신 lodash/specific)
- [ ] Image 최적화 (next/image 사용)
- [ ] 불필요한 리렌더: useCallback/useMemo 누락
- [ ] Zustand selector 미사용 (전체 store 구독)
- [ ] TanStack Query staleTime 미설정

### Accessibility (Medium)
- [ ] alt text for images
- [ ] aria-label for interactive elements
- [ ] 키보드 내비게이션
- [ ] 색상 대비 (contrast ratio)

### Code Quality (Low)
- [ ] 컴포넌트 크기 (100줄 초과 시 분리)
- [ ] TypeScript any 사용
- [ ] 하드코딩된 문자열/URL
- [ ] 일관된 네이밍 (PascalCase 컴포넌트, camelCase hooks)

## Output Format

```markdown
## Frontend Code Review Summary

### Critical Issues
- [파일:라인] 이슈 설명
  제안: 수정 방법
```

## Review Process

1. `git diff main...HEAD` 로 변경 확인
2. 변경 파일 읽기 및 분석
3. 체크리스트 기반 이슈 분류
4. 심각도별 정리 및 수정 제안

## Usage

```
/frontend review                # 모든 변경 리뷰
/frontend review src/app/...    # 특정 파일
```
