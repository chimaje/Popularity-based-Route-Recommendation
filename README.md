# Popularity-Based Route Recommendation

A feasibility study into whether Strava's crowdsourced segment data can be turned into route recommendations that favour *popular* paths, not just the shortest ones. Built as the practical component of my MSc dissertation (Advanced Computer Science, Leeds Beckett University, 2026).

## Problem

Strava lets athletes carve routes up into user-defined "segments," and exposes how often each one gets run (`effort_count`) and by how many distinct people (`athlete_count`). That's a genuine signal of real-world route preference ,arguably a better one than distance or elevation for recreational routing. But nobody uses it to actually build routes. It gets used almost exclusively as an *observational* dataset (mapping where cyclists ride, measuring infrastructure demand) rather than as an input to a routing algorithm.

The reason it's hard: segments aren't a network. Each one is a short, independently-drawn line with no declared relationship to its neighbours. They overlap, vary wildly in length, and their endpoints rarely line up with anything. So the core technical question this project set out to answer was:

**Can fragmented, user-generated Strava segments be algorithmically assembled into a connected graph that supports popularity-weighted route generation ,and what breaks when you try?**

## Approach

**Data.** Strava API, running segments only, over a bounding box covering Leeds, UK (roughly 14 km × 10 km, ~140 km²). The `/segments/explore` endpoint caps out at 10 segments per call regardless of box size, so the area was split into a 2×2 tile grid and queried separately to spread coverage. Each discovered segment ID was then resolved via `/segments/{id}` to get its polyline, `effort_count`, and `athlete_count`. Result: **39 segments**, cached locally so the Strava API only had to be hit once (`leeds_running_segments_from_strava.json`).

**Preprocessing.** Encoded polylines were decoded into lat/lng points, rounded to 5 decimal places (~1 m  anything finer is floating-point noise, not GPS precision) to clean the data, and to 4 decimal places (~11 m) specifically when generating node keys, so that near-identical points recorded by different users' GPS traces snap to the same graph node.

**Graph construction.** Nodes come from three sources: segment start points, segment end points, and detected intersections , found by a pairwise Haversine comparison of every polyline point against every other segment's points, using a 15 m threshold (chosen to sit just above Strava's documented 5–10 m GPS accuracy). Segments are split into directed sub-edges at each detected intersection, with a reverse edge added for bidirectional travel. Edge weights come in three variants, all *inverted* so that Dijkstra's cost-minimising search ends up maximising popularity:

```
effort_weight   = 1 / (effort_count + 1)
athlete_weight  = 1 / (athlete_count + 1)
combined_weight = 1 / (0.7 × effort_count + 0.3 × athlete_count + 1)
```

**Algorithm.** Dijkstra's shortest path (via JGraphT), run over whichever weighted graph is selected. A* was considered and explicitly rejected: A*'s speed advantage relies on an admissible distance heuristic, but here the edge cost is popularity, which doesn't correlate with geographic proximity ,a nearby node can sit behind a very unpopular (high-cost) edge. A distance heuristic would misguide the search rather than help it, and at 206 nodes Dijkstra already returns in under 10 ms, so there was nothing to gain from the added complexity anyway.

**System.** A Java/Spring Boot REST API loads all three pre-built graphs and serves route queries; a React + Leaflet frontend lets you pick a weighting scheme, click a start/end node on the map, and see the generated route drawn back.

## Key Finding

The weighting mechanism does exactly what it's supposed to. Across every scheme tested, routes came back more popular than the dataset average — for example, EFFORT-weighted routes averaged **88,334 efforts/segment** against a dataset baseline of **83,333**, and the same held for ATHLETE and COMBINED weighting. Dijkstra over inverted-popularity weights reliably prefers well-used segments over obscure ones. That part of the hypothesis is confirmed.

The honest part of the result is what happened to the graph itself. 39 segments produced **206 nodes split across 33 disconnected components**, with the single largest connected piece holding only **53 nodes — 25.7% coverage**. Running the same pipeline with the intersection threshold at 10 m and 20 m instead of 15 m changed local density slightly but never pushed coverage above ~28%, which rules out "bad threshold" as the explanation. Routing only works when both endpoints happen to fall in the same component; everything else correctly returns "no path exists," which is the graph behaving correctly given what it was fed, not a bug.

I'm treating that ceiling as the headline finding rather than a footnote. If the algorithm had failed to route or failed to favour popular segments, that would point at a design flaw. Instead, both of those work — the limiting factor is provably that 39 segments over 140 km² is nowhere near enough to build a connected city-scale network, and the evidence (33 components regardless of threshold, coverage capped near 28% at every sensitivity setting) says that's a data availability problem, not an algorithmic one. Knowing precisely *which* of the two it is is what tells you what to fix next: more segments (wider OAuth scope, longer collection window, multiple activity types), not a different graph or a different pathfinding algorithm.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2.0 |
| Graph & routing | JGraphT 1.5.2 (graph model + Dijkstra's algorithm) |
| Strava API client | OkHttp 4.12.0, Gson 2.10.1, dotenv-java (OAuth 2.0 + JSON) |
| Polyline decoding | Google Maps Services Java library 2.2.0 |
| Frontend | React 19 + TypeScript, Vite |
| Mapping | Leaflet / react-leaflet |
| Build | Maven |

Pre-built graphs (`leeds_graph_effort.json`, `leeds_graph_athlete.json`, `leeds_graph_combined.json`) and the cached raw segment data (`leeds_running_segments_from_strava.json`) are committed to the repo, so the API and frontend run out of the box ,a Strava developer account is only needed if you want to re-collect data for a different area.

## How to Run It

### Prerequisites
- JDK 17+ and Maven 3.8+
- Node.js 20+ and npm

### 1. Run the API and frontend (no Strava account needed)

```bash
git clone https://github.com/chimaje/Popularity-based-Route-Recommendation.git
cd Popularity-based-Route-Recommendation

# Backend — starts the REST API on http://localhost:8080
mvn spring-boot:run
```

In a second terminal:

```bash
cd src/test/UI_route
npm install
npm run dev
```

Open the URL Vite prints (typically `http://localhost:5173`). Pick a weighting scheme (EFFORT / ATHLETE / COMBINED), click a start node and an end node on the map, and generate a route. The frontend is pre-configured to call the API at `localhost:8080`, and the API's CORS policy is already set to allow `localhost:5173` and `localhost:3000`.

### 2. (Optional) Regenerate the dataset for a different city

This re-runs the full pipeline in `proj.Main`: fetch segments from Strava → cache them → build all three weighted graphs → print connectivity/coverage stats.

1. Register an app at the [Strava API settings page](https://www.strava.com/settings/api) to get a client ID and secret.
2. Create a `.env` file in the repo root:
   ```
   STRAVA_CLIENT_ID=your_client_id
   STRAVA_CLIENT_SECRET=your_client_secret
   STRAVA_ACCESS_TOKEN=your_access_token_here
   ```
3. Run `proj.Main` with the `--auth` flag (easiest from an IDE — open the project in VS Code/IntelliJ and run `Main.java` with args `--auth`). It prints an authorization URL and the `curl` command to exchange the resulting code for an access token; paste that token into `.env`.
4. Run `Main.java` again with no arguments. On first run (no cache present) it calls `exploreSegmentsNearLeeds` with the Leeds bounding box hardcoded — edit the coordinates in `Main.java` if you want a different city — fetches and decodes segments, and writes the cache and all three graph JSON files.
5. Restart the Spring Boot API to pick up the new graphs.

## Limitations

- Running segments only; cycling data wasn't collected, though the same pipeline would apply.
- The 15 m intersection threshold is a fixed heuristic tuned to typical GPS accuracy, not learned from the data.
- Evaluation used a small number of manually chosen start/end pairs, appropriate for a feasibility study but not a statistically powered benchmark.
- No comparison against existing routing tools (Google Maps, Strava's own route builder) — this project was scoped to test technical feasibility of the approach itself, not to benchmark it.

## License

Apache 2.0 — see [LICENSE](./LICENSE).

