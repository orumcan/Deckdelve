# Deckdelve

A solo roguelike dungeon crawl played with a 44-card deck. Every card is a monster, a weapon or a potion. Each room deals four cards: you resolve three, and the fourth carries into the next room.

*Based on [Scoundrel](http://www.stfj.net/art/2011/Scoundrel.pdf) by Zach Gage and Kurt Bieg.*

## About

Deckdelve is a free, open-source and fully offline version of Scoundrel. The classic rules stay faithful, and everything added on top is opt-in: switchable themes, challenge modifiers and achievements. There are no ads, accounts, tracking or network access.

Platforms: Windows, macOS and Linux, Android and the web.

## Building

You need JDK 21. The Gradle wrapper fetches everything else. Android builds also need the Android SDK, found through `ANDROID_HOME` or `sdk.dir` in `local.properties`.

| Task | Command |
|---|---|
| Run on desktop | `./gradlew lwjgl3:run` |
| Run in the browser | `./gradlew teavm:run`, then open the printed link |
| Build an Android APK | `./gradlew android:assembleDebug` |
| Play the text version, for testing the rules | `./gradlew -q --console=plain console:run` |
| Build, test and check formatting | `./gradlew build` |
| Format the code | `./gradlew spotlessApply` |

## License

Deckdelve is free software, released under the [GNU General Public License v3.0 or later](LICENSE).

Scoundrel was designed by Zach Gage and Kurt Bieg. Deckdelve isn't affiliated with or endorsed by them. The font and libraries Deckdelve uses are credited in [CREDITS.md](CREDITS.md).
