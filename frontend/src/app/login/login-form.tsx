"use client";

import { useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Input, Button } from "@/components/ui";
import { GuestGuard } from "@/components/auth";
import { useLogin } from "@/hooks";
import { getErrorMessage } from "@/lib";

export function LoginForm() {
  const searchParams = useSearchParams();
  const registered = searchParams.get("registered") === "true";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});

  const login = useLogin();

  function validate(): boolean {
    const newErrors: Record<string, string> = {};
    if (!email) newErrors.email = "Email is required";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      newErrors.email = "Invalid email format";
    if (!password) newErrors.password = "Password is required";
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;
    login.mutate({ email, password });
  }

  const serverError = login.error
    ? getErrorMessage(login.error, "Login failed. Please try again.")
    : null;

  return (
    <GuestGuard>
      <div className="flex min-h-[70vh] items-center justify-center px-4">
        <div className="w-full max-w-sm">
          <h1 className="text-center text-2xl font-bold text-gray-900">
            Sign in to SportsTix
          </h1>
          <p className="mt-2 text-center text-sm text-gray-600">
            Don&apos;t have an account?{" "}
            <Link
              href="/signup"
              className="font-medium text-primary-600 hover:text-primary-500"
            >
              Sign up
            </Link>
          </p>

          {registered && (
            <div className="mt-4 rounded-lg bg-green-50 p-3 text-sm text-green-700">
              Account created successfully. Please sign in.
            </div>
          )}

          {serverError && (
            <div className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">
              {serverError}
            </div>
          )}

          <form onSubmit={handleSubmit} className="mt-8 space-y-5">
            <Input
              id="email"
              label="Email"
              type="email"
              placeholder="you@example.com"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              error={errors.email}
            />
            <Input
              id="password"
              label="Password"
              type="password"
              placeholder="Enter your password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              error={errors.password}
            />

            <Button
              type="submit"
              isLoading={login.isPending}
              className="w-full"
            >
              Sign In
            </Button>
          </form>
        </div>
      </div>
    </GuestGuard>
  );
}
