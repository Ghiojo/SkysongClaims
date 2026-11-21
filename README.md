# SkysongClaims

A Paper plugin that groups WorldGuard regions into **Claims** owned by SkysongStats characters,
with per-type ownership limits, owner build groups, open use of doors and containers, claim signs,
and an API for SkysongEconomy to sell Claims.

## Requirements

| Needs | Why |
| --- | --- |
| Paper 1.21.11, Java 21 | The plugin is built against `paper-api` 1.21.11. |
| **WorldGuard** 7.0.17 | Hard dependency. It enforces who may build. 7.0.18 and later need Java 25 and Paper 26. |
| **SkysongStats** | Hard dependency. Every owner is one of its characters. |

## Building

```
./gradlew build
```

The jar is `build/libs/SkysongClaims-<version>.jar`. `./gradlew test` runs the unit tests (JUnit and MockBukkit).

## The API

Other plugins compile against the `api` module, which JitPack publishes from a git tag:

```groovy
repositories { maven { url 'https://jitpack.io' } }
dependencies { compileOnly 'com.github.Ghiojo:SkysongClaims:<tag>' }
```

Add `depend: [SkysongClaims]` to your plugin.yml and get the API with `SkysongClaimsApi.get()`.
The API classes ship inside the SkysongClaims jar, so never shade them.
