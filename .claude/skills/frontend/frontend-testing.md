# Frontend Testing Patterns

Vitest + React Testing Library + Playwright

## Quick Reference

```
테스트 유형 선택
    │
    ├─ 유틸 함수, hooks ──────> Vitest (Unit)
    │
    ├─ Client Components ─────> Vitest + React Testing Library
    │
    ├─ Zustand store ──────────> Vitest + renderHook
    │
    ├─ Async Server Components → Playwright (E2E)
    │
    └─ 전체 사용자 플로우 ───> Playwright (E2E)
```

---

## Vitest 설정

```ts
// vitest.config.ts
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts']
  },
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') }
  }
})
```

```ts
// src/test/setup.ts
import '@testing-library/jest-dom'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => { cleanup() })
```

## Component Test

```tsx
// src/components/ui/button.test.tsx
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import { Button } from './button'

describe('Button', () => {
  it('renders with text', () => {
    render(<Button>Click me</Button>)
    expect(screen.getByRole('button', { name: 'Click me' })).toBeInTheDocument()
  })

  it('calls onClick', async () => {
    const onClick = vi.fn()
    render(<Button onClick={onClick}>Click</Button>)
    await userEvent.click(screen.getByRole('button'))
    expect(onClick).toHaveBeenCalledOnce()
  })

  it('applies variant styles', () => {
    render(<Button variant="danger">Delete</Button>)
    expect(screen.getByRole('button')).toHaveClass('bg-red-600')
  })

  it('disables when disabled prop', () => {
    render(<Button disabled>Submit</Button>)
    expect(screen.getByRole('button')).toBeDisabled()
  })
})
```

## Zustand Store Test

```tsx
// src/stores/seat-store.test.ts
import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach } from 'vitest'
import { useSeatStore } from './seat-store'

describe('useSeatStore', () => {
  beforeEach(() => {
    useSeatStore.setState({ selectedSeats: [] })
  })

  it('selects a seat', () => {
    const { result } = renderHook(() => useSeatStore())
    act(() => { result.current.selectSeat('A-1-1') })
    expect(result.current.selectedSeats).toContain('A-1-1')
  })

  it('deselects a seat', () => {
    const { result } = renderHook(() => useSeatStore())
    act(() => {
      result.current.selectSeat('A-1-1')
      result.current.deselectSeat('A-1-1')
    })
    expect(result.current.selectedSeats).toHaveLength(0)
  })

  it('clears all seats', () => {
    const { result } = renderHook(() => useSeatStore())
    act(() => {
      result.current.selectSeat('A-1-1')
      result.current.selectSeat('A-1-2')
      result.current.clearSeats()
    })
    expect(result.current.selectedSeats).toHaveLength(0)
  })
})
```

## Custom Hook Test

```tsx
// src/hooks/use-countdown.test.ts
import { renderHook, act } from '@testing-library/react'
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'
import { useCountdown } from './use-countdown'

describe('useCountdown', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  it('decrements every second', () => {
    const { result } = renderHook(() => useCountdown(60))
    act(() => { vi.advanceTimersByTime(1000) })
    expect(result.current.remaining).toBe(59)
  })

  it('calls onExpire when reaches 0', () => {
    const onExpire = vi.fn()
    renderHook(() => useCountdown(1, onExpire))
    act(() => { vi.advanceTimersByTime(1000) })
    expect(onExpire).toHaveBeenCalledOnce()
  })
})
```

## API Mocking (MSW)

```tsx
// src/test/mocks/handlers.ts
import { http, HttpResponse } from 'msw'

export const handlers = [
  http.get('/api/v1/games', () => {
    return HttpResponse.json({
      status: 200,
      data: [
        { id: '1', title: 'Game 1', stadium: 'Stadium A' }
      ]
    })
  }),
  http.post('/api/v1/bookings', () => {
    return HttpResponse.json({
      status: 201,
      data: { id: 'booking-1', status: 'CONFIRMED' }
    })
  })
]
```

---

## Playwright E2E

```ts
// playwright.config.ts
import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry'
  },
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI
  }
})
```

```ts
// e2e/booking-flow.spec.ts
import { test, expect } from '@playwright/test'

test('full booking flow', async ({ page }) => {
  await page.goto('/games/1')
  await page.waitForSelector('[data-testid="seat-map"]')
  await page.click('[data-seat-id="A-1-1"]')
  await expect(page.locator('[data-seat-id="A-1-1"]')).toHaveClass(/selected/)
  await page.click('button:has-text("Book")')
  await expect(page.locator('text=Booking Confirmed')).toBeVisible()
})
```

---

## 테스트 전략

| 영역 | 도구 | 커버리지 |
|------|------|----------|
| UI 컴포넌트 | Vitest + RTL | Button, Card, Badge |
| Store 로직 | Vitest | Zustand stores |
| Custom Hooks | Vitest + renderHook | useCountdown, useWebSocket |
| API 통합 | Vitest + MSW | TanStack Query hooks |
| 전체 플로우 | Playwright | 대기열→좌석→결제 |

## 필수 패키지

```bash
npm install -D vitest @vitejs/plugin-react @testing-library/react @testing-library/jest-dom @testing-library/user-event jsdom msw @playwright/test
```

**관련 skill**: `/nextjs-app-router`, `/react-components`
