# Next.js 14 App Router Patterns

Next.js 14+ App Router 핵심 패턴 가이드

## Quick Reference

```
컴포넌트 타입 선택
    │
    ├─ 데이터 fetch / SEO ────> Server Component (기본)
    │
    ├─ 상호작용 (onClick, useState) ──> Client Component ('use client')
    │
    ├─ 폼 제출 ──────────────> Server Action ('use server')
    │
    └─ 실시간 업데이트 ───────> Client Component + WebSocket/SSE
```

---

## Server Components (기본값)

```tsx
// app/games/[id]/page.tsx - Server Component
export default async function GameDetailPage({ params }: { params: { id: string } }) {
  const game = await fetchGame(params.id)  // Direct server-side fetch

  return (
    <div>
      <h1>{game.title}</h1>
      <GameSeatsClient gameId={game.id} />  {/* Interactive part */}
    </div>
  )
}
```

## Client Components

```tsx
// _components/seat-selector.tsx
'use client'

import { useState } from 'react'

export function SeatSelector({ gameId }: { gameId: string }) {
  const [selected, setSelected] = useState<string[]>([])
  return <button onClick={() => setSelected([...selected, 'A1'])}>Select</button>
}
```

## 파일 컨벤션

| 파일 | 용도 |
|------|------|
| `page.tsx` | 라우트 페이지 |
| `layout.tsx` | 공유 UI (헤더/푸터) |
| `loading.tsx` | Suspense 로딩 상태 |
| `error.tsx` | Error Boundary |
| `not-found.tsx` | 404 페이지 |
| `route.ts` | API Route Handler |

## Route Groups

```
app/
├── (auth)/           # URL에 영향 없음
│   ├── login/page.tsx    → /login
│   └── layout.tsx        # Auth 전용 레이아웃
├── (main)/
│   ├── games/page.tsx    → /games
│   └── layout.tsx        # Main 전용 레이아웃 (헤더/푸터)
```

## Data Fetching

```tsx
// Server Component에서 직접 fetch
export default async function GamesPage() {
  const games = await fetch('http://gateway:8080/api/v1/games', {
    next: { revalidate: 60 }  // ISR: 60초마다 재검증
  }).then(r => r.json())

  return <GameList games={games.data} />
}
```

## Streaming + Suspense

```tsx
import { Suspense } from 'react'

export default function GamePage() {
  return (
    <div>
      <h1>Game Details</h1>
      <Suspense fallback={<SeatsSkeleton />}>
        <SeatsData gameId={id} />  {/* Async Server Component */}
      </Suspense>
    </div>
  )
}
```

## Server Actions (Form 처리)

```tsx
// _actions/booking.ts
'use server'

import { revalidatePath } from 'next/cache'

export async function createBooking(formData: FormData) {
  const gameId = formData.get('gameId') as string
  const res = await fetch('http://gateway:8080/api/v1/bookings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ gameId, seats: formData.getAll('seats') })
  })
  revalidatePath(`/games/${gameId}`)
  return res.json()
}
```

## Metadata (SEO)

```tsx
export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const game = await fetchGame(params.id)
  return {
    title: `${game.title} - SportsTix`,
    description: `Book tickets for ${game.title}`
  }
}
```

---

## Anti-Patterns

| 실수 | 올바른 방법 |
|------|------------|
| Server Component에서 useState/useEffect | Client Component로 분리 |
| Client Component에서 async/await | Server Component에서 fetch → props 전달 |
| `'use client'`를 최상위에 | 가능한 한 말단 컴포넌트에만 |
| fetch에 cache 설정 없음 | `next: { revalidate }` 또는 `cache: 'no-store'` |
| Page 전체를 Client Component | Server Component 유지, 인터랙션만 분리 |

**관련 skill**: `/zustand-state`, `/tanstack-query`, `/tailwind-patterns`
