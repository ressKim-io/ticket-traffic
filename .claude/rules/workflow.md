# Issue-Based Workflow Rules

## Rule 1: Every task MUST have a GitHub Issue
- Before starting any work, an open GitHub Issue must exist
- If no Issue exists, create one first with `gh issue create`
- Issue must have labels: priority (P0/P1/P2), category (infrastructure/security/observability/etc.)

## Rule 2: Branch naming follows Issue number
```
feature/{issue-number}-description
fix/{issue-number}-description
```
- Example: `feature/71-load-testing`, `fix/73-db-users`

## Rule 3: Fixed Routine (mandatory, no exceptions)
```
STEP 1. main 최신화 + branch 생성
STEP 2. 구현 + 촘촘한 커밋 (1-3 files per commit)
STEP 3. push
STEP 4. PR 생성 (/dx:pr-create) — body에 `Closes #{issue-number}` 포함
STEP 5. PR Code Review — 실제 코드 리뷰 실행 → PR 댓글로 기록
STEP 6. CI 통과 + Review 통과 확인 → squash merge + delete branch
STEP 7. main 복귀 + pull — 여기까지 완료해야 다음 작업 시작 가능
```

## Rule 4: PR must reference Issue
- PR body에 `Closes #{issue-number}` 필수
- Merge 시 Issue가 자동으로 닫힘

## Rule 5: Commit message format
```
<type>(<scope>): <subject>
```
- Types: feat, fix, docs, style, refactor, test, chore
- Scopes: common, gateway, auth, game, queue, booking, payment, admin, frontend, infra, k8s, ci
- Each commit: 1-3 files only

## Rule 6: Review requirements by type
| Type | Review command |
|------|---------------|
| Backend (Java/Spring) | `/backend:review` |
| Frontend (Next.js) | `/frontend-review` or lint + build |
| Infra (K8s) | `/k8s:validate` |
| Terraform | `/terraform:validate` |

- 변경된 파일 유형에 따라 해당하는 리뷰를 **모두** 실행
- 예: Backend + K8s 동시 변경 시 → `/backend:review` + `/k8s:validate` 둘 다 실행

## Rule 7: STEP 5 — PR Code Review 절차 (핵심 규칙)

### 7.1 리뷰 실행
PR 생성 직후, 변경 유형에 맞는 리뷰 명령어를 실행한다:
- Backend 변경 → `/backend:review` 실행
- K8s 변경 → `/k8s:validate` 실행
- Frontend 변경 → `/frontend-review` 실행
- 복합 변경 → 해당하는 리뷰 모두 실행

### 7.2 리뷰 결과를 PR 댓글로 기록
리뷰 결과를 아래 형식으로 PR 댓글에 게시한다:
```bash
gh pr comment {number} --body "$(cat <<'EOF'
## Code Review — {review-type}

### Summary
- **Critical**: {count}
- **High**: {count}
- **Medium**: {count}
- **Low**: {count}

### Critical Issues
{없으면 "None" / 있으면 파일:라인 + 설명 + 수정 방안}

### High Issues
{없으면 "None" / 있으면 파일:라인 + 설명 + 수정 방안}

### Medium/Low Issues
{요약 또는 생략 가능}

### Verdict
{✅ PASS — Critical/High 0건 | ❌ FAIL — 수정 필요}
EOF
)"
```

### 7.3 Critical/High 이슈 처리
- **Critical/High가 1건이라도 있으면**: 수정 커밋 → push → 리뷰 재실행 → PR 댓글 업데이트
- **Critical/High 0건이 될 때까지** STEP 6으로 진행 불가
- Medium/Low는 기록만 하고 진행 가능 (판단에 따라 수정)

### 7.4 복합 리뷰 시
여러 리뷰를 실행한 경우, 각각 별도 댓글로 게시하거나 하나의 댓글에 섹션을 나눈다.

## Rule 8: STEP 6 — CI 통과 + Merge 확인 절차
- CI 완료 후 `gh pr checks {number} --watch` 로 전체 통과 확인
- `gh pr view {number} --comments` 로 CI Review Summary 댓글 확인
- 확인 항목:
  - 실행된 check가 모두 ✅ Pass 인지 확인
  - ❌ Fail 이 하나라도 있으면 merge 금지 → 수정 후 재push
  - Code Review 댓글의 Verdict가 ✅ PASS 인지 확인
- 모든 조건 충족 시 squash merge

## Rule 9: No work without Issue tracking
- Backlog items live as GitHub Issues with priority labels
- `gh issue list --label P0` to see high-priority work
- Pick the highest priority unassigned Issue before starting
