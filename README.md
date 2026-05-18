# DoLogger

DoLogger is a server-side activity logging mod for Minecraft 1.21.1 on NeoForge. It records block changes, container transactions, item activity, entity interactions, player sessions, chat, and commands, then exposes in-game lookup and inspect commands backed by PostgreSQL.

## Relationship To GriefLogger

DoLogger is a NeoForge-native port/reimplementation of the original GriefLogger feature set. It follows GriefLogger's core gameplay model: inspect mode, paged lookups, action filters, and history output for block, item, container, entity, session, chat, and command activity.

Important differences:

- DoLogger targets NeoForge 1.21.1 only.
- DoLogger is not an Architectury or multi-loader mod.
- DoLogger uses PostgreSQL through HikariCP instead of SQLite.
- DoLogger does not create the PostgreSQL database itself; the database must already exist.

This project is not the original GriefLogger project and does not imply upstream endorsement.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- PostgreSQL server

The default PostgreSQL connection is:

```text
host=localhost
port=5432
database=dologger
username=postgres
password=
```

DoLogger creates its own tables after connecting successfully, but it expects the `dologger` database to already exist.

## Installation

1. Install a NeoForge 1.21.1 server.
2. Put the DoLogger jar in the server's `mods/` directory.
3. Start the server once to generate `config/dologger-common.toml`.
4. Create the PostgreSQL database if it does not already exist.
5. Edit `config/dologger-common.toml` with the database connection settings.
6. Restart the server.

If PostgreSQL is unavailable, the server continues starting, but logging and lookup functionality are unavailable until the database connection is available.

## Configuration

Main config keys in `config/dologger-common.toml`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables or disables DoLogger logging. |
| `language` | `en_us` | Server-side language file for DoLogger messages. Bundled values: `en_us`, `zh_cn`. |
| `host` | `localhost` | PostgreSQL host. |
| `port` | `5432` | PostgreSQL port. |
| `database` | `dologger` | PostgreSQL database name. |
| `username` | `postgres` | PostgreSQL username. |
| `password` | empty | PostgreSQL password. |
| `poolSize` | `3` | HikariCP maximum pool size. |
| `connectionTimeout` | `5000` | Connection timeout in milliseconds. |
| `validationTimeout` | `3000` | Validation timeout in milliseconds. |
| `idleTimeout` | `600000` | HikariCP idle timeout in milliseconds. |
| `maxLifetime` | `1800000` | HikariCP max lifetime in milliseconds. |
| `queueFlushTimeout` | `10000` | Maximum queue flush time during server stop, in milliseconds. |
| `queueCapacity` | `10000` | Maximum number of asynchronous database tasks kept in memory. |
| `queueBusyThreshold` | `9000` | Rejects new asynchronous database tasks once the queue reaches this many pending tasks. |
| `pageSize` | `10` | Number of lookup entries per page. |
| `maxRevertRestorePlanSize` | `1000` | Default maximum number of rows selected by revert/restore preview when no `limit.` filter is provided. |
| `purgeRetentionDays` | `0` | Deletes history older than this many days on database startup. `0` disables automatic purge. |

Use `/dologger reload` after changing reloadable config values. NeoForge handles file watching for config files; the command clears DoLogger's config cache and reloads the server-side language table from the currently loaded config.

## Commands

DoLogger registers two command roots:

- `/dologger`
- `/gl`

Both roots expose the same subcommands.

### `/dologger inspect`

Toggles inspect mode for the player. While inspect mode is enabled, clicking blocks shows recorded history for the clicked position instead of performing the normal block action.

Permission node: `dologger.inspect`

### `/dologger lookup [filters]`

Queries stored history near the player. Results are sorted newest-first and split into pages using the configured `pageSize`.

Permission node: `dologger.lookup`

Examples:

```text
/dologger lookup
/dologger lookup user.SaltWood_233
/dologger lookup action.break_block,place_block radius.10 time.1d
/dologger lookup action.chat,command user.SaltWood_233 time.30s
/dologger lookup action.chat,command user.SaltWood_233 time.1h
/dologger lookup include.stone,dirt exclude.bedrock radius.2c
```

### `/dologger page <n>`

Displays a stored lookup result page.

Permission node: `dologger.page`

Example:

```text
/dologger page 2
```

### `/dologger reload`

Refreshes DoLogger's cached config values and reloads the server-side language file.

Permission node: `dologger.reload`

Example:

```text
/dologger reload
```

### `/dologger purge [days]`

Deletes history rows older than the selected number of days. Without `days`, this uses `purgeRetentionDays` from the config. A value of `0` disables purge and does not delete rows.

Permission node: `dologger.purge`

Examples:

```text
/dologger purge
/dologger purge 30
```

### `/dologger revert preview <filters>` / `confirm` / `cancel`

Previews and safely reverts matching block break/place history. Revert uses the same lookup filters, but requires an explicit `action.` filter and only supports `action.break_block` and `action.place_block`. Preview stores a pending plan for 60 seconds; confirm executes the saved plan newest-to-oldest; cancel clears it. Successfully reverted source rows are marked as currently reverted in the database, clearing any prior restored marker so the same source row can be reverted/restored repeatedly. Inverse audit rows are linked back to the source rows.

Permission node: `dologger.revert`

Safety limits:

- Block-only MVP: no container, item, session, entity, chat, or command revert.
- Preview includes up to `limit.<n>` matched eligible block changes, or `maxRevertRestorePlanSize` when no limit filter is provided, and confirm runs newest-first over the saved IDs.
- Reverting a place removes the block only if the current block still matches the logged material.
- Reverting a break restores only default block state, only into air, and skips block-entity blocks such as chests or signs.

Examples:

```text
/dologger revert preview action.place_block user.SaltWood_233 radius.10 time.1h
/dologger revert preview action.break_block include.stone radius.b
/dologger revert preview action.place_block radius.10 time.1h limit.200
/dologger revert confirm
/dologger revert cancel
```

### `/dologger restore preview <filters>` / `confirm` / `cancel`

Previews and safely restores currently reverted block break/place history. Restore requires an explicit `action.` filter and only supports `action.break_block` and `action.place_block`. Preview only includes source rows that are currently reverted; confirm executes the saved plan oldest-to-newest, marks successful source rows as restored, and clears the current revert marker so the same source row can be reverted/restored repeatedly. Command-generated audit rows are linked back to the original source rows and are not themselves selected for revert/restore.

Permission node: `dologger.restore`

Safety limits:

- Block-only MVP: no container, item, session, entity, chat, or command restore.
- Preview includes up to `limit.<n>` matched currently reverted block changes, or `maxRevertRestorePlanSize` when no limit filter is provided, and confirm runs oldest-first over the saved IDs.
- Restoring an original place places the logged material back only into air.
- Restoring an original break removes the block only if the current block still matches the logged material.
- Restore conflicts instead of overwriting mismatched blocks.

Examples:

```text
/dologger restore preview action.place_block user.SaltWood_233 radius.10 time.1h
/dologger restore preview action.break_block include.stone radius.b
/dologger restore preview action.break_block user.SaltWood_233 limit.50
/dologger restore confirm
/dologger restore cancel
```

All permission checks currently fall back to Minecraft permission level 2.

## Lookup Filters

Lookup filters use this format:

```text
prefix.value
```

Multiple filters are separated by spaces. Each filter prefix can appear at most once. Filters support command suggestions.

### `user`

Filters by current or historical player name.

```text
user.SaltWood_233
user.Steve,Alex
```

### `action`

Filters by logged action name. Multiple values are comma-separated.

Common examples:

```text
action.break_block
action.place_block
action.interact_block
action.kill_entity
action.drop_item,pickup_item
action.join,quit
action.chat,command
```

Supported action names include:

```text
break_block, place_block, interact_block, kill_entity, interact_entity
remove_item, add_item, drop_item, pickup_item, craft_item, break_item
consume_item, throw_item, shoot_item, add_item_ender, remove_item_ender
join, quit, chat, command
```

Chat and command history are first-class lookup results. Material `include` and `exclude` filters only apply to block, container, and item rows; chat, command, and session rows are selected by user, time, radius, and their `action.` family.

### `include` / `exclude`

Includes or excludes materials by registry name. The `minecraft:` namespace can be omitted for vanilla materials.

```text
include.stone,dirt
exclude.bedrock
include.minecraft:diamond_block
```

### `radius`

Limits lookup results around the player's current position.

```text
radius.10
radius.b
radius.5b
radius.1c
radius.2c
```

Meaning:

- `radius.10`: 10-block radius in X/Y/Z.
- `radius.b`: current block only.
- `radius.5b`: 5-block radius in X/Y/Z.
- `radius.1c`: current chunk.
- `radius.2c`: current chunk plus surrounding chunks, a 3x3 chunk area.

### `time`

Limits lookup results by time. By default, filters to recent history (newer than the given duration). Prefix with `>` to filter to older history instead.

```text
time.30s
time.30m
time.1h
time.7d
time.1y
time.>30m
time.>1h
time.>1d
```

Supported suffixes:

- `s`: seconds
- `m`: minutes
- `h`: hours
- `d`: days
- `y`: years

The `>` prefix selects entries older than the given duration instead of newer:

- `time.1h`: entries from the last hour (newer than 1 hour ago)
- `time.>1h`: entries older than 1 hour ago

### `limit`

Caps revert/restore preview result size. Lookup commands ignore this filter; it is intended for destructive preview plans.

```text
limit.50
limit.200
limit.1000
```

When omitted, revert/restore preview uses `maxRevertRestorePlanSize`.

## Logged Activity

DoLogger records these major categories:

- Block break, place, and interaction history.
- Container item additions/removals through transaction diffs.
- Item drop, pickup, craft, consume, break, throw, and shoot actions.
- Entity attribution for kills and selected indirect block/entity actions.
- Player join and quit sessions.
- Player chat messages.
- Player commands.

## Notes

- Database writes are queued asynchronously. The queue is bounded by `queueCapacity`; once it reaches `queueBusyThreshold`, new asynchronous writes are rejected and DoLogger reports the database queue as busy instead of growing memory without limit. Critical revert/restore finalization uses synchronous database transactions.
- Lookup commands read from PostgreSQL synchronously.
- Language output is resolved on the server from bundled language files instead of sending DoLogger translation keys to clients.
- Bundled languages: `en_us`, `zh_cn`.

## License

All Rights Reserved.
