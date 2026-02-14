# TanStack Query + Next.js 14

서버 상태 관리: Prefetch + Hydration 패턴

## Quick Reference

```
데이터 fetch 전략
    │
    ├─ 정적/SEO 필요 ──> Server Component + fetch (ISR/SSG)
    │
    ├─ 동적 + 클라이언트 캐시 ──> TanStack Query (prefetch + hydration)
    │
    └─ 실시간 업데이트 ──> TanStack Query + WebSocket invalidation
```

---

## Provider 설정

```tsx
// src/components/providers/query-provider.tsx
'use client'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { useState } from 'react'

export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(() =>
    new QueryClient({
      defaultOptions: {
        queries: {
          staleTime: 60 * 1000,
          retry: 1,
          refetchOnWindowFocus: false
        }
      }
    })
  )

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  )
}
```

```tsx
// app/layout.tsx
import { QueryProvider } from '@/components/providers/query-provider'

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  )
}
```

## Server Prefetch + Hydration (핵심 패턴)

```tsx
// app/games/page.tsx - Server Component
import { dehydrate, HydrationBoundary, QueryClient } from '@tanstack/react-query'
import { fetchGames } from '@/lib/api'
import { GamesList } from './_components/games-list'

export default async function GamesPage() {
  const queryClient = new QueryClient()

  await queryClient.prefetchQuery({
    queryKey: ['games'],
    queryFn: fetchGames
  })

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <GamesList />
    </HydrationBoundary>
  )
}
```

```tsx
// _components/games-list.tsx - Client Component
'use client'

import { useQuery } from '@tanstack/react-query'
import { fetchGames } from '@/lib/api'

export function GamesList() {
  const { data: games, isLoading } = useQuery({
    queryKey: ['games'],
    queryFn: fetchGames
    // No loading state on initial render (prefetched)
  })

  if (isLoading) return <GamesSkeleton />
  return <div>{games?.map(g => <GameCard key={g.id} game={g} />)}</div>
}
```

## API Client 설정

```tsx
// src/lib/api-client.ts
import axios from 'axios'
import { useAuthStore } from '@/stores/auth-store'

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  timeout: 10000
})

// Token interceptor
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default apiClient
```

```tsx
// src/lib/api/games.ts
import apiClient from '@/lib/api-client'

export async function fetchGames() {
  const { data } = await apiClient.get('/api/v1/games')
  return data.data  // ApiResponse wrapper
}

export async function fetchGame(id: string) {
  const { data } = await apiClient.get(`/api/v1/games/${id}`)
  return data.data
}
```

## Mutation 패턴

```tsx
'use client'

import { useMutation, useQueryClient } from '@tanstack/react-query'

export function useCreateBooking() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: BookingRequest) =>
      apiClient.post('/api/v1/bookings', data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['games', variables.gameId] })
    }
  })
}
```

## Polling (실시간 대기열)

```tsx
const { data: queueStatus } = useQuery({
  queryKey: ['queue', gameId],
  queryFn: () => fetchQueueStatus(gameId),
  refetchInterval: 3000,  // 3초마다 폴링
  enabled: !!gameId       // gameId 있을 때만
})
```

## Query Key 컨벤션

```tsx
// 계층적 key 구조
['games']                     // 전체 게임 목록
['games', gameId]             // 게임 상세
['games', gameId, 'seats']    // 게임별 좌석
['queue', gameId]             // 대기열 상태
['bookings']                  // 내 예매 목록
['bookings', bookingId]       // 예매 상세
```

---

## Anti-Patterns

| 실수 | 올바른 방법 |
|------|------------|
| queryFn에서 state 참조 | 파라미터로 전달 |
| Server Component에서 useQuery | prefetch + HydrationBoundary |
| staleTime 0 (기본) | 적절한 staleTime 설정 |
| onSuccess에서 state 변경 | select로 데이터 변환 |

**관련 skill**: `/nextjs-app-router`, `/zustand-state`
