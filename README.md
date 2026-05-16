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
| `pageSize` | `10` | Number of lookup entries per page. |

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
```

Supported action names include:

```text
break_block, place_block, interact_block, kill_entity, interact_entity
remove_item, add_item, drop_item, pickup_item, craft_item, break_item
consume_item, throw_item, shoot_item, add_item_ender, remove_item_ender
join, quit
```

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

Limits lookup results to recent history.

```text
time.30m
time.1h
time.7d
time.1y
```

Supported suffixes:

- `m`: minutes
- `h`: hours
- `d`: days
- `y`: years

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

- Database writes are queued asynchronously.
- Lookup commands read from PostgreSQL synchronously.
- Language output is resolved on the server from bundled language files instead of sending DoLogger translation keys to clients.
- Bundled languages: `en_us`, `zh_cn`.

## License

All Rights Reserved.
