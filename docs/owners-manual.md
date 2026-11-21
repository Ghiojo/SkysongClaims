# SkysongClaims owner's manual

This manual explains how every part of SkysongClaims works. It is written for the server owner. It covers what each file and setting does, what each command and permission does, and how to make the usual changes yourself.

When this manual and the code disagree, the code wins. The last section, [Where things live in the code](#where-things-live-in-the-code), says which file to open for each part.

## Contents

1. [What the plugin does](#what-the-plugin-does)
2. [Requirements and installing](#requirements-and-installing)
3. [WorldGuard permissions players must not have](#worldguard-permissions-players-must-not-have)
4. [The plugin folder and reloading](#the-plugin-folder-and-reloading)
5. [config.yml](#configyml)
6. [types.yml](#typesyml)
7. [messages.yml](#messagesyml)
8. [Claims and their regions](#claims-and-their-regions)
9. [Owners, limits and invites](#owners-limits-and-invites)
10. [Build groups](#build-groups)
11. [Owner flags](#owner-flags)
12. [Claim signs](#claim-signs)
13. [What SkysongClaims writes to WorldGuard](#what-skysongclaims-writes-to-worldguard)
14. [Deleted profiles and the check](#deleted-profiles-and-the-check)
15. [Commands](#commands)
16. [Permissions](#permissions)
17. [The API and events for other plugins](#the-api-and-events-for-other-plugins)
18. [The database](#the-database)
19. [How to make common changes](#how-to-make-common-changes)
20. [Things to know](#things-to-know)
21. [Where things live in the code](#where-things-live-in-the-code)

---

## What the plugin does

Staff draw WorldGuard regions as usual. They then group one or more regions into a **Claim**, for example a house with a garden and a cellar. The Claim is what players own. Its regions only give it a shape.

- Every Claim has a **type** from `types.yml`, such as Hovel or Other. The type sets how many Claims of it one character may own.
- Claims are owned by **SkysongStats characters (profiles)**, not by player accounts. A player who switches to another character loses the rights of the first one at once.
- A Claim can have several **owners**. A bought Claim has no cap on owners, and a Free Claim has a fixed number of slots. Owners can never remove each other. Only staff can, or an owner leaves on their own.
- **Free Claims**, such as hovels, are taken by players themselves, one slot each, with `/sclaim take` or from the Claim's sign.
- Other Claims are given out by staff, or sold by SkysongEconomy through the API.
- Owners can make **build groups**, which let other characters build. Nobody can lock doors or chests. Those are open to everyone in every Claim, and theft is handled by lock items.
- Owners may set a few WorldGuard flags on their Claim. By default these are the greeting and farewell messages.
- Staff can turn any sign into a **Claim sign**. It shows the Claim's name, its type and its open slots, and right-clicking it opens a menu.

---

## Requirements and installing

| Needs | Why |
|---|---|
| Paper 1.21.11, Java 21 | The plugin is built against `paper-api` 1.21.11. |
| **WorldGuard 7.0.17** | Hard dependency. WorldGuard enforces who may build. 7.0.18 and later need Java 25, so stay on 7.0.17 while the server runs Java 21. |
| **SkysongStats** | Hard dependency. Every owner is a SkysongStats character. |
| **MySQL** | Required. If the database can't be reached at start-up, the plugin logs why and **disables itself**. |

Paper downloads HikariCP (the database connection pool) by itself on the first start, through `libraries:` in `plugin.yml`.

**Building:** run `./gradlew build`. The jar is `build/libs/SkysongClaims-<version>.jar`. The build also runs the unit tests.

**First start:** the plugin writes `config.yml`, `types.yml` and `messages.yml` into `plugins/SkysongClaims/`. Fill in `database:` in `config.yml`, then restart. It can be the same database as the other Skysong plugins. The tables are created on their own.

---

## WorldGuard permissions players must not have

SkysongClaims is the only thing that writes the owners, members and open-use flags of a Claim's regions. If players keep WorldGuard's own region commands, they can get around the rules. For example, they could add a friend as a member with `/rg addmember` or lock chests with `/rg flag`. SkysongClaims writes everything back on the next change, profile switch, restart or `/sclaims check`, but until then their change holds.

**Do not give players these WorldGuard permissions:**

- `worldguard.region.addmember.*` and `worldguard.region.addowner.*`
- `worldguard.region.removemember.*` and `worldguard.region.removeowner.*`
- `worldguard.region.flag.*`, including `worldguard.region.flag.own.*` and `worldguard.region.flag.member.*`

Players are owners of their Claim's WorldGuard regions, so the `.own.*` versions would apply to them. Check that no LuckPerms group gives these through a wildcard such as `worldguard.*`.

Staff who need to bypass build protection use WorldGuard's own `worldguard.region.bypass.*`. `skysongclaims.admin` only covers SkysongClaims' commands.

---

## The plugin folder and reloading

| File | What it holds | `/sclaims reload` |
|---|---|---|
| `config.yml` | Database, limit scope, invite expiry, group limits, owner flags | Yes, except `database:`, which needs a restart |
| `types.yml` | Claim types | Yes |
| `messages.yml` | Every text players and staff see | Yes |

`/sclaims reload` reads all three files again, all at once. If one is broken, nothing changes and the old config stays in use. Staff see the error. After a reload, every Claim is written to WorldGuard again and every Claim sign is redrawn. Claims whose type is no longer in `types.yml` are listed in the console.

If your `messages.yml` lacks a message, for example after an update, the default from the jar is used.

---

## config.yml

```yaml
database:
  host: localhost
  port: 3306
  user: root
  password: ""
  database_name: skysong

limit-per: character
invite-expiry: 5m
max-groups-per-claim: 5
max-members-per-group: 0
owner-flags: [greeting, farewell]
```

| Setting | What it does |
|---|---|
| `database:` | The MySQL connection. Changes need a restart. |
| `limit-per` | `character`: each SkysongStats profile has its own count for a type's limit. `account`: all of a player's characters share one count. A type can override it with its own `limit-per`. |
| `invite-expiry` | How long a co-owner invite waits for `/sclaim accept`, such as `30s`, `5m` or `1h`. |
| `max-groups-per-claim` | How many build groups one Claim's owners may make. `0` means no limit. |
| `max-members-per-group` | How many characters one group may hold. `0` means no limit. |
| `owner-flags` | The WorldGuard flags owners may set with `/sclaim flag`. `build` and the type's open-use flags are always refused, even if listed. |

---

## types.yml

```yaml
types:
  hovel:
    display-name: "<gold>Hovel"
    limit: 1
    free-claim: { default-max-owners: 2 }
    open-use: [use, chest-access, interact, sleep, use-anvil, use-dripleaf, respawn-anchors, ride,
               vehicle-place, vehicle-destroy, item-frame-rotation]
    extra: {}
  other:
    display-name: "Other"
    limit: 3
```

The key (`hovel`, `other`) is the type's id. Use up to 32 lower case letters, digits, `_` and `-`.

| Field | What it does |
|---|---|
| `display-name` | Required. The name players see, in MiniMessage. |
| `limit` | Required. How many Claims of this type one character (or account) may own. Other plugins can raise it for some players through the API. |
| `limit-per` | `character` or `account`. When it is left out, `config.yml`'s `limit-per` is used. |
| `free-claim` | Makes new Claims of this type Free Claims, with `default-max-owners` slots (1 when left out). Claims of types without it are bought or given by staff, and have **no cap on owners**: their owners may invite as many co-owners as they like. |
| `open-use` | The WorldGuard flags set to `allow` for everyone in the Claim's regions. When left out, it uses the list shown above. `build` is never allowed here. Animals keep WorldGuard's default. |
| `extra` | Free settings that other plugins read, such as SkysongEconomy price modifiers. SkysongClaims doesn't use them. |

A type's `free-claim` only sets the **starting** value of a new Claim. Staff can change any single Claim afterwards with `/sclaims free`. An old `max-owners:` line is ignored, with a warning in the console.

If you remove a type that Claims still use, those Claims keep working. Their owners keep them, no one can take new ones (the limit counts as 0), and their regions keep the standard open-use flags, so chests never lock. They are listed in the console at start-up and after every reload. Give them a new type with `/sclaims settype`.

---

## messages.yml

Every text is MiniMessage, and `prefix` is put in front of chat messages. The sections are:

| Section | What it covers |
|---|---|
| `command` | Usage, unknown command, no permission, players only. |
| `claim` | Staff Claim commands: create, delete, regions, type, slots, rename. |
| `ownership` | Taking, inviting, accepting, leaving, adding and removing owners, and every refusal (type limit, full, already an owner, unknown character). |
| `group` | Build group commands. |
| `flag` | Owner flags. |
| `signs` | The sign lines (`line-name`, `line-type`, `free-open`, `free-full`, `for-sale`, `owned`, `unowned`) and the menu's items. |
| `player` | `/sclaim info` and `/sclaim list`. |
| `claims` | `/sclaims info`, `list`, `check`, `reload` and the delete confirmation. |

Placeholders such as `<claim>` and `<character>` are filled in as plain text. Names that staff typed in MiniMessage, such as a Claim's display name, are filled in with their colours.

---

## Claims and their regions

- Staff make a Claim with `/sclaims create <id> <type> <regions...>`. The id is what commands use, such as `rose_house`. The name players see starts as the id, and `/sclaims rename` changes it.
- A region is written `region` (the world you stand in) or `world:region`. A Claim's regions can be in different worlds.
- One WorldGuard region belongs to at most one Claim.
- A new Claim is unowned and not for sale. It is a Free Claim if its type says so.
- `/sclaims removeregion` hands the region back to staff. Its owners, members and owner flags are cleared, its open-use flags stay, and the WorldGuard region itself is not deleted.
- `/sclaims delete <id> confirm` deletes the Claim with its owners, groups and signs. Its WorldGuard regions stay and are handed back like this too.
- If you delete a WorldGuard region by hand, the Claim keeps it on its list. Start-up and `/sclaims check` report it, and you remove it with `/sclaims removeregion`.

---

## Owners, limits and invites

**Ways to become an owner:**

| How | Who | Checks |
|---|---|---|
| `/sclaim take` or the sign menu's **Take slot** | A player, on a Free Claim | A slot is open, and the type's limit isn't reached. |
| `/sclaim invite`, then `/sclaim accept` | An owner invites, the invitee accepts | The type's limit, and on a Free Claim an open slot. Checked at invite time and again at accept time. |
| `/sclaims addowner` | Staff | On a Free Claim, an open slot. `-force` skips the type limit. |
| The API | SkysongEconomy, for example on a sale | The type's limit, and on a Free Claim an open slot. |

Only Free Claims have slots. A bought Claim has no cap on owners, so its owners can invite as many co-owners as they like.

- **Limits** count the Claims of a type a character owns (or an account owns, with `limit-per: account`). A shared Claim counts once for each owner. Other plugins can add live bonuses through the API, for example "+1 hovel while they have the Noble rank". Nothing is saved for bonuses.
- Someone **over their limit** (for example after a bonus ended, or after `/sclaims settype`) keeps what they own but can't take more.
- **Invites** last `invite-expiry` and are kept in memory only, so a restart drops them. A new invite to the same Claim replaces the old one.
- **Leaving:** `/sclaim leave [id] confirm` or the sign menu's **Leave** (two clicks). It asks first, because a bought Claim can't be taken back.
- **Owners can't remove each other.** Only staff can, with `/sclaims removeowner`.
- When the **last owner** leaves, the Claim is unowned again. A Free Claim opens every slot, and the Claim's groups are cleared.
- Non-player characters (NPC profiles) count toward limits like any other character.

---

## Build groups

`/sclaim group <id> create|delete|rename|add|remove|list` manages a Claim's groups.

- Any owner manages every group of their Claim. Staff manage any Claim's groups.
- A group's members may **build**, which includes placing mannequins. That is all a group gives, because doors and chests are open to everyone already.
- A group's id is its name in lower case. `rename` changes only the name shown.
- Owners can't be members, because they build already. Someone who becomes an owner leaves their groups.
- Limits come from `max-groups-per-claim` and `max-members-per-group`.

---

## Owner flags

`/sclaim flag <id> <flag> <value...|clear>` sets a WorldGuard flag on **every** region of the Claim, for example `/sclaim flag rose_house greeting <gold>Welcome!`.

- Only the flags in `owner-flags` can be set. `build` and the type's open-use flags are always refused.
- WorldGuard checks the value. If it refuses, the owner sees why.
- The flags are saved with the Claim, so they come back when WorldGuard data is lost or a region is added later. They are cleared from a region when it leaves the Claim.

---

## Claim signs

Look at a sign and run `/sclaims sign <id>` to link it to a Claim. `/sclaims unsign` makes it a normal sign again. A sign can stand anywhere, inside the Claim or not.

| Line | Shows |
|---|---|
| 1 | The Claim's name (`signs.line-name`). |
| 2 | Its type (`signs.line-type`). |
| 3 | On a Free Claim, the open slots (`free-open`) or `free-full`. On other Claims, `for-sale`, `owned` or `unowned` (blank by default). |
| 4 | Kept for another plugin, such as SkysongEconomy's price. It is blank until that plugin sets it. |

- **Right-clicking** opens a menu. It shows the Claim's name, type, owners and open slots, a **Take slot** button on a Free Claim with room, a **Leave** button for its owners, and buttons from other plugins (such as Buy) in the bottom row.
- Only staff can break, edit or wax a Claim sign. Explosions, fire and pistons leave it alone.
- Signs are redrawn whenever their Claim changes, when their chunk loads, and after a reload. If a sign's block is gone, it is unlinked and logged.

---

## What SkysongClaims writes to WorldGuard

For every region of a Claim, SkysongClaims writes:

- **Owners:** the players whose current character owns the Claim.
- **Members:** the players whose current character is in one of its build groups.
- **Open-use flags:** the type's `open-use` flags, set to `allow`.
- **Owner flags:** the ones owners set.

WorldGuard knows players, not characters. A player only counts while they play the character that has the right. Switching profile, joining the server, any Claim change, start-up, reload and `/sclaims check` all write the regions again. That also puts back anything someone changed by hand. Everything else about the region is yours: its shape, its priority, its parent, and other flags.

---

## Deleted profiles and the check

SkysongStats doesn't announce deleted profiles. So at start-up, and with `/sclaims check`, SkysongClaims looks for characters that no longer exist and releases them from every Claim they owned or were a group member of. The check also lists Claim regions that WorldGuard no longer has, and writes every Claim to WorldGuard again.

---

## Commands

`/sclaim` (alias `/sc` when no other plugin uses it) is for players. `/sclaims` is for staff. Where a command takes a `[profile]`, leaving it out means the character the player plays now.

### Players: `/sclaim` (`skysongclaims.use`)

| Command | What it does |
|---|---|
| `/sclaim take` | Take a slot of the Free Claim you stand in. |
| `/sclaim invite <id> <player> [profile]` | Invite a character to co-own your Claim. |
| `/sclaim accept [id]` / `/sclaim deny [id]` | Answer an invite. The id is only needed when you have several. |
| `/sclaim leave [id] [confirm]` | Stop owning a Claim, by default the one you stand in. |
| `/sclaim group <id> create <name>` | Make a build group. |
| `/sclaim group <id> delete <group>` | Delete it. |
| `/sclaim group <id> rename <group> <name...>` | Rename it. |
| `/sclaim group <id> add <group> <player> [profile]` | Add a member. |
| `/sclaim group <id> remove <group> <player> [profile]` | Remove a member. |
| `/sclaim group <id> list` | List the groups and their members. |
| `/sclaim flag <id> <flag> <value...\|clear>` | Set or clear an owner flag. |
| `/sclaim info` | About the Claim you stand in. |
| `/sclaim list` | Your character's Claims, and how many of each type it may still own. |

### Staff: `/sclaims` (`skysongclaims.admin`)

| Command | What it does |
|---|---|
| `/sclaims create <id> <type> <[world:]region...>` | Make an unowned Claim. |
| `/sclaims delete <id> [confirm]` | Delete a Claim. Without `confirm`, it only says what will happen. |
| `/sclaims addregion <id> <[world:]region>` | Add a region. |
| `/sclaims removeregion <id> <[world:]region>` | Take a region out. |
| `/sclaims settype <id> <type>` | Change the type. Owners keep it even if that puts them over the new limit. |
| `/sclaims free <id> true [slots]` | Make it a Free Claim, or change a Free Claim's slots. Without a number, it keeps a Free Claim's slots or uses its type's. Slots can't go below the current number of owners. |
| `/sclaims free <id> false` | Make it a bought Claim, with no cap on owners. |
| `/sclaims rename <id> <name...>` | The name players see, in MiniMessage. |
| `/sclaims addowner <id> <player> [profile] [-force]` | Add an owner. `-force` skips the type limit. |
| `/sclaims removeowner <id> <player> [profile]` | Remove an owner. |
| `/sclaims sign <id>` / `/sclaims unsign` | Link or unlink the sign you look at. |
| `/sclaims info <id>` | Everything about one Claim. |
| `/sclaims list [type]` | Every Claim, or those of one type. |
| `/sclaims check` | The [check](#deleted-profiles-and-the-check). |
| `/sclaims reload` | [Reload](#the-plugin-folder-and-reloading) the config files. |

Staff with `skysongclaims.admin` may also use `/sclaim group` and `/sclaim flag` on any Claim.

---

## Permissions

| Node | Default | What it gives |
|---|---|---|
| `skysongclaims.use` | everyone | `/sclaim`. Owner commands also check that you own the Claim. |
| `skysongclaims.admin` | op | `/sclaims`, managing any Claim's groups and flags, and breaking or editing Claim signs. |

See also [WorldGuard permissions players must not have](#worldguard-permissions-players-must-not-have).

---

## The API and events for other plugins

Other plugins, mainly SkysongEconomy, use `SkysongClaimsApi.get()`. With it they read Claims and types, add, remove and transfer owners, mark Claims for sale, read groups, raise limits with live rules, set the price line on signs and add buttons to the sign menu. Bukkit events announce every change. The [README](../README.md#the-api) lists them, with an example sale.

---

## The database

All tables are created at start-up if they are missing.

| Table | Holds |
|---|---|
| `claims` | One row per Claim: id, display name, type, its slots (empty for a bought Claim) and the plugin that put it on sale. |
| `claim_regions` | The Claim's regions (world and region id). A region can be in only one Claim. |
| `claim_owners` | Owners as player UUID and profile, since when, and how they became an owner (staff, Free Claim, invite, API). |
| `claim_groups` / `claim_group_members` | Build groups and their characters. |
| `claim_flags` | Owner flags and their values. |
| `claim_signs` | Linked signs and their fourth line (as Adventure JSON). |

Changes are saved in the background, so commands never wait for MySQL. Everything is also kept in memory while the server runs.

---

## How to make common changes

### Add a Claim type
Add a section under `types:` in `types.yml` with at least `display-name` and `limit`, then run `/sclaims reload`.

### Make a type's Claims shared hovels
Give it `free-claim: { default-max-owners: <slots> }`. This only affects new Claims. Existing ones change with `/sclaims free <id> true <slots>`.

### Let two characters own a bought house
Nothing to do: one owner invites the other with `/sclaim invite`. Bought Claims have no cap on owners.

### Count limits per player instead of per character
Set `limit-per: account` in `config.yml`, or on one type in `types.yml`.

### Close something everyone can use (for example anvils)
Give the type an `open-use:` list without that flag. Then WorldGuard's normal rule applies, so only owners and members can use it.

### Let owners set another flag
Add it to `owner-flags` in `config.yml`, for example `[greeting, farewell, time-lock]`, then reload.

### Change what a sign or message says
Edit `messages.yml` and run `/sclaims reload`.

---

## Things to know

- Invites are lost on restart.
- `/sc` only works when no other plugin has claimed it. `/sclaim` always works.
- Over-limit owners keep their Claims, so changing a type's limit or a Claim's type never takes anything away.
- A region shared between a Claim and something else (for example a city region) works, because WorldGuard applies priorities as usual. Just don't add the same region to two Claims, which SkysongClaims refuses anyway.
- SkysongClaims never deletes a WorldGuard region.

---

## Where things live in the code

Code is under `src/main/java/org/skysongdev/skysongClaims/`. The public API is in `api/src/main/java/org/skysongdev/skysongClaims/api/`.

| Part | Package |
|---|---|
| Start-up and wiring | `SkysongClaims.java` |
| Config files | `config/`, `type/` (types.yml), `messaging/` (messages.yml) |
| Claims in memory and the one place that changes them | `claim/` (`ClaimStore`) |
| Database | `storage/` |
| Owners, limits and invites | `ownership/`, `limit/` |
| Build groups | `group/` |
| Owner flags | `flag/` |
| WorldGuard | `worldguard/` |
| SkysongStats characters | `character/` |
| Signs and their menu | `sign/` |
| Commands | `commands/player/`, `commands/staff/` |
| API implementation and events | `apiimpl/` |
