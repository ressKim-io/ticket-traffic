# Tailwind CSS Component Patterns

Tailwind CSS + React 컴포넌트 기반 패턴

## Quick Reference

```
스타일링 전략
    │
    ├─ 재사용 UI (Button, Card) ──> CVA (class-variance-authority)
    │
    ├─ 조건부 클래스 ──────────> cn() 헬퍼 (clsx + tailwind-merge)
    │
    ├─ 디자인 토큰 ────────────> tailwind.config.ts extend
    │
    └─ 레이아웃/페이지 ────────> 직접 className
```

---

## cn() 헬퍼 (필수)

```ts
// src/lib/utils.ts
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

```tsx
// 사용
<div className={cn(
  'p-4 rounded-lg',
  isActive && 'bg-blue-500 text-white',
  isDisabled && 'opacity-50 cursor-not-allowed'
)} />
```

## CVA - 컴포넌트 Variants

```tsx
// src/components/ui/button.tsx
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-md font-medium transition-colors disabled:opacity-50',
  {
    variants: {
      variant: {
        primary: 'bg-blue-600 text-white hover:bg-blue-700',
        secondary: 'bg-gray-200 text-gray-900 hover:bg-gray-300',
        outline: 'border border-gray-300 bg-white hover:bg-gray-50',
        danger: 'bg-red-600 text-white hover:bg-red-700'
      },
      size: {
        sm: 'h-8 px-3 text-sm',
        md: 'h-10 px-4 text-base',
        lg: 'h-12 px-6 text-lg'
      }
    },
    defaultVariants: { variant: 'primary', size: 'md' }
  }
)

interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

export function Button({ className, variant, size, ...props }: ButtonProps) {
  return (
    <button className={cn(buttonVariants({ variant, size, className }))} {...props} />
  )
}
```

## Design Tokens (tailwind.config.ts)

```ts
// tailwind.config.ts
const config = {
  content: ['./src/**/*.{js,ts,jsx,tsx,mdx}'],
  theme: {
    extend: {
      colors: {
        brand: { primary: '#0070F3', secondary: '#FF6B6B' },
        seat: {
          available: '#10B981',
          selected: '#3B82F6',
          held: '#F59E0B',
          sold: '#EF4444'
        }
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite'
      }
    }
  }
}
```

## 좌석 상태 매핑 패턴

```tsx
// Seat status → Tailwind class mapping
const seatStatusStyles = {
  AVAILABLE: 'bg-seat-available hover:bg-green-400 cursor-pointer',
  SELECTED: 'bg-seat-selected ring-2 ring-blue-400',
  HELD: 'bg-seat-held cursor-not-allowed',
  SOLD: 'bg-seat-sold cursor-not-allowed opacity-60'
} as const

// ✅ 전체 리터럴 클래스명 사용
<div className={cn('w-6 h-6 rounded-sm', seatStatusStyles[status])} />

// ❌ 동적 클래스명 생성 금지 (purge됨)
<div className={`bg-${color}-500`} />
```

## 반응형 패턴

```tsx
// Mobile-first breakpoints
<div className="
  grid grid-cols-1      // 모바일
  sm:grid-cols-2        // 640px+
  md:grid-cols-3        // 768px+
  lg:grid-cols-4        // 1024px+
  gap-4
"/>
```

---

## SportsTix 컴포넌트 목록

| 컴포넌트 | Variant | 용도 |
|----------|---------|------|
| `Button` | primary, secondary, outline, danger | 공통 버튼 |
| `Card` | default, elevated | 게임 카드, 예매 카드 |
| `Badge` | success, warning, error, info | 좌석 상태, 예매 상태 |
| `Skeleton` | - | 로딩 플레이스홀더 |
| `Countdown` | - | 결제 타이머 |

## 필수 패키지

```bash
npm install clsx tailwind-merge class-variance-authority
```

---

## Anti-Patterns

| 실수 | 올바른 방법 |
|------|------------|
| `@apply` 남용 | React 컴포넌트 + CVA |
| 동적 클래스 ``bg-${x}-500`` | 전체 클래스명 매핑 객체 |
| inline style과 혼용 | Tailwind로 통일 |
| 중복 utility 나열 | 컴포넌트로 추출 |

**관련 skill**: `/nextjs-app-router`, `/react-components`
