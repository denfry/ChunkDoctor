# Risk Scoring

ChunkDoctor converts observable chunk conditions into an explainable risk score
from 0 to 100.

## What the score means

A high score means:

- potentially expensive objects are concentrated in one chunk;
- the chunk deserves earlier manual investigation;
- one or more configurable thresholds were exceeded.

A high score does **not** prove:

- the chunk consumes an exact amount of TPS;
- the chunk is the only source of lag;
- removing everything in the chunk will fix the server.

Use spark to confirm execution-time cost.

## Inputs

Quick observations may include:

- total entities;
- villagers and hostile mobs;
- dropped items and armor stands;
- minecarts, boats, and other vehicles;
- hoppers, furnaces, containers, spawners, and other block entities.

Completed deep scans add block-level observations such as redstone components,
pistons, observers, and powered states exposed through the public Paper API.

## Calculation stages

1. Each observed count contributes `count × configured weight`.
2. Counts beyond configured excess thresholds receive nonlinear penalties.
3. Density signals add a penalty when several expensive categories are
   concentrated together.
4. The result is clamped to the range 0–100.
5. The configured thresholds map the score to a risk level.

Default levels:

| Score | Level |
|---:|---|
| 0–29 | LOW |
| 30–59 | MEDIUM |
| 60–79 | HIGH |
| 80–100 | CRITICAL |

## Confidence

Confidence describes observation completeness, not server quality.

| Confidence | Meaning |
|---|---|
| LOW | Some quick-scan context was unavailable |
| MEDIUM | Quick observation had useful entity/context data |
| HIGH | A manual deep scan completed successfully |

A LOW-confidence HIGH score should be investigated, not ignored. A HIGH
confidence does not turn the risk estimate into exact profiling.

## Reasons and recommendations

Each result contains:

- the strongest score contributors;
- relevant recommendations derived from observed metrics;
- whether it came from a quick or deep scan;
- the observation timestamp.

Recommendations are conditional. For example, hopper advice appears only when
hopper metrics justify it.

## Tuning safely

1. Export a baseline set of results.
2. Change one family of weights or thresholds.
3. Reload the configuration.
4. Re-scan representative chunks.
5. Compare false positives and false negatives.
6. Validate suspicious areas with spark.

Avoid tuning solely around one unusual farm. The model should remain useful
across different worlds and play styles.
