# THROWAWAY PROTOTYPE — Simulation Dashboard UI

Question this prototype answers:

> Which dashboard layout helps a learner understand
> Booking → PostgreSQL → Kafka → Notification without feeling overloaded?

Three structurally different variants live on one route and are selected with
the `?variant=` query parameter:

- `A` — Pipeline Story
- `B` — Operations Console
- `C` — Guided Hybrid

Run:

```bash
./prototype/simulation-dashboard-ui/run.sh
```

Then open <http://localhost:4173/?variant=A>. Use the floating switcher or the
left/right arrow keys to compare variants.

All data is simulated in memory. This code has no tests, persistence, backend,
Kafka, or Kubernetes connection. It must never be merged into `main`.

## Verdict

Selected on 2026-07-25: **C — Guided Hybrid**.

The implementation should preserve these decisions:

- Lead with the business outcome: confirmed Bookings, rejected requests, and
  completed Notifications.
- Keep the simulation controls close to that outcome.
- Explain the system as a four-step journey:
  Request → PostgreSQL → Kafka → Notification.
- Show one plain-language explanation of the current state or failure.
- Keep a short recent-event feed for evidence.
- Send detailed infrastructure metrics to Grafana instead of crowding the
  learner-facing Dashboard.

The production UI must be rewritten with production tests and error handling;
the prototype code itself remains only on this throwaway branch.
