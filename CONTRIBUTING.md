# Contributing

Deckdelve is a small project with a narrow goal: a faithful, offline Scoundrel that stays simple. Bug reports, themes and fixes are all welcome, and everyone taking part is expected to follow the [code of conduct](.github/CODE_OF_CONDUCT.md).

## Reporting a bug

Open an issue and say what you did, what you expected and what happened instead. Add the platform and the version, which the Credits screen shows. A screenshot helps.

If it happened during a run, add the seed from the end-of-run panel, such as `7KQ2-M9XD`, along with the modifiers and rule variants you had on. A seed deals the same dungeon on every platform, so it lets us replay your run.

A security problem doesn't belong in a public issue: the [security policy](.github/SECURITY.md) says where to send it.

## Adding a theme

A theme is a folder of names, colors and icons, with no code. [THEMES.md](THEMES.md) has the format and an example.

## Changing the code

You need JDK 21. Before opening a pull request, run `./gradlew spotlessApply` to format the code and `./gradlew build` to check it: the build compiles every platform, runs the tests and lints the Android app.

For anything larger than a fix, open an issue first, so the design is agreed before you write it.

A few things hold the project together:

- **The classic rules don't change.** They follow the official rules. Where those are silent, the game picks one reading and offers the other as a rule variant in Settings. A new reading is a new variant, switched off by default.
- **Rules and scoring changes come with tests.** A fix for a rules bug starts with a test that fails.
- **A seed plays the same everywhere.** `logic` is plain Java without libGDX, and nothing in it may depend on the clock, on `java.util.Random` or on the order of a hash map. The shuffle and its random number generator are frozen, because every existing seed depends on them.
- **Shared code has to compile for the web.** TeaVM has no reflection, threads or `java.time`.
- **No text in code.** Everything a player reads lives in `assets/i18n/strings.properties`, and a theme's text in its `theme.json`.
- **Information is never carried by color alone.** Every card always shows its rank and suit.
- **Plain Java.** Small classes and good names, with a comment only where the code can't say why.

## What Deckdelve won't take

- Network access, ads, accounts, analytics or tracking.
- Dependencies that aren't free software. F-Droid builds the game from source and rejects them.
- Art, sound or names that belong to someone else. Assets must be under CC0, CC BY, CC BY-SA or the OFL, and each one is credited in [CREDITS.md](CREDITS.md) with its source, author and license.

## Translating

Every text is in `assets/i18n/strings.properties`, and a translation is a copy of it named for its language, such as `strings_de.properties`. The fonts only carry Western European letters so far, so open an issue before starting on another script.

## Commits and license

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org), such as `feat(logic): add weapon tie variant`.

Deckdelve is released under the [GNU General Public License v3.0 or later](LICENSE), and by contributing you agree to release your work under it too.
