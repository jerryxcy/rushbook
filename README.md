# RushBook

RushBook is a limited-slot reservation system for exploring correctness under
burst traffic, reliable event delivery with Kafka, and production-style
deployment on Kubernetes.

## Project goal

When many users compete for a small number of slots, RushBook must never
oversell. The project will build that invariant first, then add Kafka-based
post-booking events, Kubernetes operations, observability, and failure tests.

