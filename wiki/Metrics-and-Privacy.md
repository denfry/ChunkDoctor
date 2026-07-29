# Metrics and Privacy

ChunkDoctor uses bStats plugin ID
[32969](https://bstats.org/plugin/bukkit/ChunkDoctor/32969) for anonymous,
aggregated usage metrics.

## Standard bStats data

bStats may collect standard operational fields such as:

- current online player count;
- server software and Minecraft version;
- ChunkDoctor version;
- Java version;
- operating-system type;
- country-level location.

ChunkDoctor does not register custom charts.

## Data ChunkDoctor does not send

- player names or UUIDs;
- chat or commands;
- world names or world data;
- chunk coordinates;
- scan results or risk scores;
- configuration values;
- JSON report contents;
- filesystem paths.

## Disable metrics

bStats uses one shared server-wide configuration:

```text
plugins/bStats/config.yml
```

Set:

```yaml
enabled: false
```

Restart the server after changing the shared bStats configuration.

## Performance

bStats performs its network submission asynchronously. It is separate from
ChunkDoctor's main-thread collection and bounded analysis pipeline.

## References

- [ChunkDoctor bStats dashboard](https://bstats.org/plugin/bukkit/ChunkDoctor/32969)
- [bStats information for server owners](https://bstats.org/docs/server-owners)
- [bStats source code](https://github.com/Bastian/bStats)
