# CosmeticTags Minecraft Chat Cosmetics Plugin

CosmeticTags is a Minecraft chat cosmetics plugin for Paper and Folia servers. It lets players equip multiple configured titles and tags, choose an emoji and chat color, and enable animated name coloring in chat.

## Install

1. Build the plugin with Java 21 and Maven by running `mvn package` in the project directory.
2. Copy `target/plugin.jar` into your server’s `plugins` folder.
3. Start the server. CosmeticTags creates its configuration and player data files on startup.
4. Edit `plugins/CosmeticChat/config.yml` to configure titles, tags, emojis, and colors, then run `/cosmetic reload` as an operator.

## Use

- Run `/cosmetic list` to see the configured titles, tags, emojis, and colors.
- Run `/cosmetic title hero,explorer` or `/cosmetic tag welcome,builder` to equip multiple entries. Use `off` to remove all equipped titles or tags of that kind.
- Run `/cosmetic emoji star` or `/cosmetic emoji off` to set or remove your emoji.
- Run `/cosmetic color gold` to choose a configured chat color, or `/cosmetic color off` to restore white.
- Run `/cosmetic animation` to toggle animated name coloring.
- Run `/cosmetic clear` to reset your cosmetics.
- Run `/cosmetic help` for the command overview.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/04956b2e-1dbe-434b-8df0-6928610a85a3?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=04956b2e-1dbe-434b-8df0-6928610a85a3
