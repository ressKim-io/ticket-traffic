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
STEP 3. review (Critical/High 0건까지) — push 전 필수
STEP 4. push
STEP 5. PR 생성 (/dx:pr-create) — body에 `Closes #{issue-number}` 포함
STEP 5.5. CI가 자동으로 PR에 Review Summary 댓글 게시 (review-summary job)
STEP 6. CI 통과 확인 → squash merge + delete branch
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

## Rule 7: No work without Issue tracking
- Backlog items live as GitHub Issues with priority labels
- `gh issue list --label P0` to see high-priority work
- Pick the highest priority unassigned Issue before starting
