# SkysongClaims

A Paper plugin that groups WorldGuard regions into **Claims** owned by SkysongStats characters,
with per-type ownership limits, owner build groups, open use of doors and containers, claim signs,
and an API for SkysongEconomy to sell Claims.

Server owners: read the [owner's manual](docs/owners-manual.md) for every file, command and
permission, and the WorldGuard permissions players must not have.

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

Everything below runs on the main thread (other threads get an `IllegalStateException`), and an
unknown Claim or type id throws `IllegalArgumentException`. Characters are SkysongStats'
`CharacterId`.

| Part | What it does |
| --- | --- |
| `claims()` | Reads Claims as `ClaimInfo` snapshots: all, by id, at a location, by WorldGuard region, owned by a character. |
| `types()` | Reads the types of `types.yml`, including their `extra:` section for your own settings. |
| `ownership()` | `canOwn`, `addOwner`, `removeOwner`, `transfer` (one owner's place to another) and `clearOwners`. They answer an `OwnershipCheck` instead of throwing, so you can refund. |
| `market()` | Marks Claims for sale or not. The price stays in your plugin. |
| `groups()` | Reads owners' build groups and whether a character may build. |
| `limits()` | Reads limits and registers live `LimitRule`s that raise them (e.g. +1 hovel for a rank). |
| `signs()` | Sets the fourth line of a Claim's signs (your price) and adds buttons to the sign menu. |

Limit rules and menu buttons are removed when your plugin disables.

A sale, for example:

```java
SkysongClaimsApi claims = SkysongClaimsApi.get();
OwnershipCheck check = claims.ownership().canOwn(buyer, "rose_house");
if (check != OwnershipCheck.OK) {
    // Tell the player why: TYPE_LIMIT, ALREADY_OWNER, UNKNOWN_CHARACTER, or FULL (Free Claims only;
    // bought Claims have no cap on owners).
    return;
}
// Take the money, then:
if (claims.ownership().addOwner(this, "rose_house", buyer) != OwnershipCheck.OK) {
    // Refund: something changed in between.
}
claims.market().setForSale(this, "rose_house", false);
claims.signs().clearPriceLine(this, "rose_house");
```

### Events

All are fired on the main thread after the change is saved, and can't be cancelled. Each Claim
event carries the Claim after the change and a `ChangeCause`: `COMMAND`, `LEAVE` (an owner
left), `FREE_CLAIM`, `API` or `RECONCILE` (SkysongClaims fixed it, e.g. a deleted profile).

| Event | When |
| --- | --- |
| `ClaimCreateEvent`, `ClaimDeleteEvent` | Staff created or deleted a Claim. |
| `ClaimOwnerAddEvent`, `ClaimOwnerRemoveEvent` | A character became or stopped being an owner. A transfer fires a remove, then an add. |
| `ClaimSaleChangeEvent` | A Claim was put on sale or taken off. |
| `ClaimTypeChangeEvent` | Staff changed a Claim's type. |
| `ClaimSignChangeEvent` | A sign was linked to a Claim or stopped showing it. A new sign's price line starts blank. |
| `SkysongClaimsReloadEvent` | Staff reloaded the config, so types may have changed. |
