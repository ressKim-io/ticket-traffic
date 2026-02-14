# Zustand State Management

Zustand + Next.js 14 App Router 상태 관리 패턴

## Quick Reference

```
상태 유형 선택
    │
    ├─ 서버 데이터 (API 응답) ──> TanStack Query (/tanstack-query)
    │
    ├─ UI 상태 (모달, 선택) ───> Zustand
    │
    ├─ URL 상태 (필터, 페이지) ─> useSearchParams
    │
    └─ 폼 상태 ────────────────> react-hook-form or useActionState
```

---

## 기본 Store 패턴

```tsx
// src/stores/seat-store.ts
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'

interface SeatStore {
  selectedSeats: string[]
  selectSeat: (seatId: string) => void
  deselectSeat: (seatId: string) => void
  clearSeats: () => void
}

export const useSeatStore = create<SeatStore>()(
  devtools((set) => ({
    selectedSeats: [],
    selectSeat: (seatId) =>
      set((state) => ({
        selectedSeats: [...state.selectedSeats, seatId]
      })),
    deselectSeat: (seatId) =>
      set((state) => ({
        selectedSeats: state.selectedSeats.filter((id) => id !== seatId)
      })),
    clearSeats: () => set({ selectedSeats: [] })
  }))
)
```

## Persist Middleware (localStorage)

```tsx
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthStore {
  accessToken: string | null
  setToken: (token: string) => void
  clearToken: () => void
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      accessToken: null,
      setToken: (token) => set({ accessToken: token }),
      clearToken: () => set({ accessToken: null })
    }),
    { name: 'auth-storage' }
  )
)
```

## Server Component → Zustand 초기화

```tsx
// app/queue/[gameId]/page.tsx - Server Component
export default async function QueuePage({ params }: { params: { gameId: string } }) {
  const data = await fetchQueuePosition(params.gameId)
  return (
    <>
      <QueueStoreInit position={data.position} />
      <QueueDisplay />
    </>
  )
}

// _components/queue-store-init.tsx - Client Component
'use client'
import { useEffect } from 'react'
import { useQueueStore } from '@/stores/queue-store'

export function QueueStoreInit({ position }: { position: number }) {
  const setPosition = useQueueStore((s) => s.setPosition)
  useEffect(() => { setPosition(position) }, [position, setPosition])
  return null
}
```

## Selector로 최적화 (리렌더 방지)

```tsx
// ✅ 필요한 값만 구독
const selectedSeats = useSeatStore((s) => s.selectedSeats)
const clearSeats = useSeatStore((s) => s.clearSeats)

// ❌ 전체 store 구독 → 불필요한 리렌더
const store = useSeatStore()
```

## Computed Values (Derived State)

```tsx
export const useSeatStore = create<SeatStore>()((set, get) => ({
  selectedSeats: [],
  // Computed
  get totalPrice() {
    return get().selectedSeats.reduce((sum, s) => sum + s.price, 0)
  },
  get seatCount() {
    return get().selectedSeats.length
  }
}))
```

---

## SportsTix 권장 Store 구조

| Store | 용도 | Persist |
|-------|------|---------|
| `useAuthStore` | JWT 토큰, 로그인 상태 | Yes |
| `useSeatStore` | 좌석 선택 상태 | No |
| `useQueueStore` | 대기열 위치, 토큰 | No |
| `useUIStore` | 모달, 사이드바, 테마 | Yes |

---

## Anti-Patterns

| 실수 | 올바른 방법 |
|------|------------|
| 서버 데이터를 Zustand에 저장 | TanStack Query 사용 |
| 전체 store 구독 `useStore()` | Selector `useStore(s => s.field)` |
| Server Component에서 store 사용 | Client Component에서만 사용 |
| 큰 단일 store | 도메인별 분리 |

**관련 skill**: `/tanstack-query`, `/nextjs-app-router`
