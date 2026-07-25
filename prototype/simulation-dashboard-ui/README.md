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
