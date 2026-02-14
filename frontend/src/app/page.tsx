import Link from "next/link";

export default function HomePage() {
  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
      {/* Hero Section */}
      <section className="py-20 text-center">
        <h1 className="text-4xl font-bold tracking-tight text-gray-900 sm:text-5xl md:text-6xl">
          Get Your <span className="text-primary-600">Sports Tickets</span>
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-lg text-gray-600">
          Real-time seat selection with fair queueing system. Never miss your
          favorite games again.
        </p>
        <div className="mt-10 flex items-center justify-center gap-4">
          <Link
            href="/games"
            className="rounded-lg bg-primary-600 px-6 py-3 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-primary-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-600"
          >
            Browse Games
          </Link>
          <Link
            href="/signup"
            className="rounded-lg px-6 py-3 text-sm font-semibold text-gray-900 ring-1 ring-inset ring-gray-300 transition-colors hover:bg-gray-50"
          >
            Create Account
          </Link>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16">
        <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
          <FeatureCard
            title="Fair Queue System"
            description="First come, first served with real-time position updates via WebSocket."
          />
          <FeatureCard
            title="Interactive Seat Map"
            description="Select your seats in real-time with live availability status."
          />
          <FeatureCard
            title="Secure Payments"
            description="Safe and fast checkout with 5-minute hold guarantee."
          />
        </div>
      </section>
    </div>
  );
}

function FeatureCard({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="rounded-xl border border-gray-200 p-6 transition-shadow hover:shadow-md">
      <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
      <p className="mt-2 text-sm text-gray-600">{description}</p>
    </div>
  );
}
