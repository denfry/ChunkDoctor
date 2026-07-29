# Reports and Exports

ChunkDoctor can export every cached result or one selected chunk as UTF-8 JSON.

## Commands

```text
/cd export
/cd export <world> <x> <z>
```

Both commands require `chunkdoctor.export`.

## Safety properties

- Serialization runs away from the server thread.
- The export queue is controlled by the plugin lifecycle.
- Output is written to a temporary file first.
- The final file uses atomic replacement when the filesystem supports it.
- The configured directory must remain inside the plugin data folder.
- Unsafe paths fail closed.

## Document structure

An export contains:

- generation timestamp;
- plugin and server versions;
- result-level summary counts;
- one object per exported chunk;
- stable world UUID and display name;
- chunk coordinates;
- analysis timestamp;
- score, level, and confidence;
- complete observed metrics;
- reasons and recommendations;
- whether the result came from a deep scan.

Abbreviated example:

```json
{
  "generatedAt": "2026-07-28T12:00:00Z",
  "pluginVersion": "1.0.0",
  "serverVersion": "Paper 1.21.8",
  "summary": {
    "analyzedChunks": 1,
    "criticalChunks": 1
  },
  "chunks": [
    {
      "key": {
        "worldName": "world",
        "chunkX": 52,
        "chunkZ": -18
      },
      "riskScore": 84,
      "riskLevel": "CRITICAL",
      "confidence": "HIGH",
      "deepScan": true
    }
  ]
}
```

## Privacy

Exports may reveal world names, coordinates, infrastructure patterns, and
administrator observations. Treat them as operational data:

- do not publish production exports without review;
- remove sensitive coordinates before attaching a report publicly;
- use private security reporting for exploit or abuse evidence;
- restrict `chunkdoctor.export` to trusted staff.

Exports are not sent to bStats. See [[Metrics and Privacy]].
