# React 19 Component Patterns

React 19 + Next.js 14 컴포넌트 패턴

## Quick Reference

```
React 19 Hook 선택
    │
    ├─ 폼 제출 + 서버 처리 ──> useActionState + Server Action
    │
    ├─ 제출 중 상태 ──────────> useFormStatus
    │
    ├─ 낙관적 업데이트 ───────> useOptimistic
    │
    ├─ Promise 언래핑 ────────> use()
    │
    └─ 클라이언트 폼 검증 ───> react-hook-form + zod
```

---

## useActionState (폼 제출)

```tsx
'use client'

import { useActionState } from 'react'
import { createBooking } from '../_actions/booking'

export function BookingForm({ gameId }: { gameId: string }) {
  const [state, action, isPending] = useActionState(createBooking, {
    message: '', errors: {}
  })

  return (
    <form action={action}>
      <input type="hidden" name="gameId" value={gameId} />
      <button disabled={isPending}>
        {isPending ? 'Booking...' : 'Book Seat'}
      </button>
      {state.message && <p>{state.message}</p>}
    </form>
  )
}
```

## useFormStatus (제출 상태)

```tsx
'use client'

import { useFormStatus } from 'react-dom'

export function SubmitButton({ children }: { children: React.ReactNode }) {
  const { pending } = useFormStatus()
  return (
    <button type="submit" disabled={pending}
      className={pending ? 'opacity-50' : ''}>
      {pending ? 'Loading...' : children}
    </button>
  )
}
```

## useOptimistic (낙관적 UI)

```tsx
'use client'

import { useOptimistic } from 'react'

export function SeatGrid({ seats }: { seats: Seat[] }) {
  const [optimisticSeats, setOptimistic] = useOptimistic(
    seats,
    (state, updatedSeat: Seat) =>
      state.map(s => s.id === updatedSeat.id ? updatedSeat : s)
  )

  async function handleSelect(seat: Seat) {
    setOptimistic({ ...seat, status: 'SELECTED' })  // Instant UI update
    await selectSeatAction(seat.id)                  // Server action
  }

  return (
    <div className="grid grid-cols-10 gap-1">
      {optimisticSeats.map(seat => (
        <SeatButton key={seat.id} seat={seat} onSelect={handleSelect} />
      ))}
    </div>
  )
}
```

## use() Hook (Promise)

```tsx
// Server Component - Promise 전달
export default async function Page() {
  const gamesPromise = fetchGames()  // Don't await!
  return (
    <Suspense fallback={<Skeleton />}>
      <GamesList gamesPromise={gamesPromise} />
    </Suspense>
  )
}

// Client Component - Promise 소비
'use client'
import { use } from 'react'

export function GamesList({ gamesPromise }: { gamesPromise: Promise<Game[]> }) {
  const games = use(gamesPromise)
  return <div>{games.map(g => <GameCard key={g.id} game={g} />)}</div>
}
```

## react-hook-form + zod (클라이언트 검증)

```tsx
'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'

const schema = z.object({
  email: z.string().email('Invalid email'),
  password: z.string().min(8, 'Min 8 characters')
})

type FormData = z.infer<typeof schema>

export function LoginForm() {
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema)
  })

  const onSubmit = async (data: FormData) => {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(data)
    })
    // handle response
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('email')} />
      {errors.email && <p className="text-red-500">{errors.email.message}</p>}
      <input type="password" {...register('password')} />
      {errors.password && <p className="text-red-500">{errors.password.message}</p>}
      <button type="submit">Login</button>
    </form>
  )
}
```

## WebSocket Hook

```tsx
// src/hooks/use-websocket.ts
'use client'

import { useEffect, useRef, useCallback } from 'react'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

export function useWebSocket(url: string, topic: string, onMessage: (data: any) => void) {
  const clientRef = useRef<Client | null>(null)

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(url),
      onConnect: () => {
        client.subscribe(topic, (message) => {
          onMessage(JSON.parse(message.body))
        })
      },
      reconnectDelay: 5000
    })

    client.activate()
    clientRef.current = client

    return () => { client.deactivate() }
  }, [url, topic, onMessage])

  const send = useCallback((destination: string, body: any) => {
    clientRef.current?.publish({ destination, body: JSON.stringify(body) })
  }, [])

  return { send }
}
```

## Countdown Timer

```tsx
'use client'

import { useState, useEffect } from 'react'

export function Countdown({ seconds, onExpire }: {
  seconds: number
  onExpire: () => void
}) {
  const [remaining, setRemaining] = useState(seconds)

  useEffect(() => {
    if (remaining <= 0) { onExpire(); return }
    const timer = setInterval(() => setRemaining(r => r - 1), 1000)
    return () => clearInterval(timer)
  }, [remaining, onExpire])

  const min = Math.floor(remaining / 60)
  const sec = remaining % 60

  return (
    <span className={remaining < 60 ? 'text-red-500 animate-pulse' : ''}>
      {min}:{sec.toString().padStart(2, '0')}
    </span>
  )
}
```

---

## Anti-Patterns

| 실수 | 올바른 방법 |
|------|------------|
| useEffect로 data fetch | Server Component 또는 TanStack Query |
| prop drilling 5+ 레벨 | Zustand store 또는 Context |
| 불필요한 `'use client'` | 가능한 한 Server Component 유지 |
| 인라인 함수 re-create | useCallback으로 안정화 |

**관련 skill**: `/nextjs-app-router`, `/zustand-state`
