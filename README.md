# CombatLogg

Minecraft Spigot/Paper plugin for combat-log protection.

## Функционал

- При ударе игроком моба, мобом игрока или игроком игрока появляется bossbar сверху.
- Таймер по умолчанию длится 30 секунд.
- Каждый новый удар обновляет таймер до полного времени.
- Если игрок выходит во время combat-tag, он умирает, а ресурсы выпадают стандартной механикой смерти сервера.
- Во время PvP combat-tag элитры запрещены. Если игрок уже летит на элитрах, полет сбрасывается.

## Команды

- `/combatlogg time <секунды>` - меняет время combat-log и сохраняет его в `config.yml`.
- `/combatlogg elytra on` - включает запрет элитр во время PvP.
- `/combatlogg elytra off` - выключает запрет элитр во время PvP.
- `/combatlogg reload` - перезагружает `config.yml`.

Право: `combatlogg.admin`.

## Сборка строго на GitHub

Локально jar собирать не нужно.

1. Залей этот проект в GitHub-репозиторий.
2. Открой вкладку `Actions`.
3. Запусти workflow `Build` через `Run workflow` или просто сделай push.
4. Готовый jar будет в артефактах workflow с именем `CombatLogg`.

## Версия Minecraft

Проект сейчас компилируется против `spigot-api 1.16.5-R0.1-SNAPSHOT` и использует `api-version: 1.16`, чтобы jar был совместим с Spigot/Paper 1.16+.

Если нужна строго другая версия из скриншота, поменяй в `pom.xml` значение `spigot.version`, а в `src/main/resources/plugin.yml` значение `api-version`.
