# DoraAmo

[中文](README.md) | [English](README_EN.md)

Minecraft 1.12.2 / Forge 14.23.5.2860

Placeable two-block Anywhere Door with Coordinator for cross-dimension destinations.

## Features

- Stand in the door ~4s to teleport; empty-hand right-click cycles blank / scaled destination
- Main door spawns a sub-door at the target; overwriting the binding rebuilds it
- Use Coordinator on a main door to configure (dimension / coords / biome / structure); empty-hand right-click locates nearby doors
- Advancements: dragon head → Anywhere Door → Coordinator

## Recipes

- **Anywhere Door**: diamonds at corners, dragon head center, obsidian elsewhere
- **Coordinator**: vertical diamond block / nether star / obsidian

## Config

| Path | Description |
|------|-------------|
| `config/doraamo.cfg` | Forge config |
| `config/doraamo/catalog/` | Display name / pinyin search JSON |

## Build

JDK 8

```bat
gradlew.bat build
```

Output: `build/libs/doraamo-1.0.0.jar`

## License

[MIT](LICENSE)
