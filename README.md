# DoraAmo

[中文](README.md) | [English](README_EN.md)

Minecraft 1.12.2 / Forge 14.23.5.2860

可放置的双格高任意门，用协调器配置跨维度目的地。

## 功能

- 站入任意门蓄力约 4 秒后传送；空手右键切换空白 / 坐标换算目的地
- 主门绑定后在目标生成子门；覆盖绑定会重建子门
- 协调器对主门右键打开配置（维度 / 坐标 / 群系 / 结构）；空手右键探测附近门
- 进度：龙首 → 任意门 → 协调器

## 合成

- **任意门**：四角钻石，中心龙首，其余黑曜石
- **协调器**：竖列 钻石块 / 下界之星 / 黑曜石

## 配置

| 路径 | 说明 |
|------|------|
| `config/doraamo.cfg` | Forge 配置 |
| `config/doraamo/catalog/` | 显示名与拼音检索 JSON |

## 构建

JDK 8

```bat
gradlew.bat build
```

产物：`build/libs/doraamo-1.0.0.jar`

## 许可

[MIT](LICENSE)
