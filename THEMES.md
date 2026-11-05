# Themes

A theme gives every card a name and a picture. The game still draws each card's rank and suit in its corners, so a theme never hides what a card does. Deckdelve ships Classic Cards, which are plain playing cards, plus the themes in `assets/themes/`. Players pick one in Settings.

A theme is data and pictures, with no code. Themes are built into the game for now, so a new one arrives as a pull request.

## The folder

```
assets/themes/
├── themes.json        the themes, in the order Settings shows them
└── fantasy/
    ├── theme.json     the theme's name, colors, cards and credits
    └── icons/         a picture for each card
```

Name the folder with a short lowercase id, such as `pirates`, and add that id to `themes.json`:

```json
["fantasy", "pirates"]
```

## theme.json

Here is the Fantasy theme, shortened to two of its 44 cards:

```json
{
  "name": {"en": "Fantasy"},
  "tints": {"clubs": "#3f8a4c", "spades": "#6c4fa3", "diamonds": "#a37a1c", "hearts": "#c0392b"},
  "cards": {
    "2D": {"name": {"en": "Dagger"}, "icon": "dagger.png"},
    "AS": {"name": {"en": "Demon Lord"}, "icon": "demon-lord.png"}
  },
  "credits": [
    {"en": "Icons by Lorc, Delapouite, Skoll and Caro Asercion from game-icons.net, under CC BY 3.0."},
    {"en": "The Longsword icon is adapted from Lorc's shining-sword."}
  ]
}
```

- **name:** the theme's name in Settings.
- **tints:** a color for each suit, as `#rrggbb`. It colors the card's frame and the icon's black parts and outline, so it needs to stand out on the card's off-white face.
- **cards:** every one of the 44 cards, each with a name and the file name of its icon.
- **credits:** the lines the Credits screen shows for the theme.

Clubs and spades are monsters, diamonds are weapons and hearts are potions. The two monster suits can be two families with their own looks, such as The Wilds and The Crypt in Fantasy.

## Cards

A card's key is its rank followed by its suit:

- Rank: `2` to `10`, or `J`, `Q`, `K` or `A`.
- Suit: `C` for clubs, `S` for spades, `D` for diamonds, `H` for hearts.

Clubs and spades run from 2 to the ace, and diamonds and hearts from 2 to 10. `10D` is the 10 of diamonds and `QS` the queen of spades.

Keep names short. A name shares the middle of the card with its icon and has room for two short lines. A word too long for one line is drawn smaller.

## Icons

- A square PNG for each card, in the `icons/` folder.
- White and black on a transparent background. The game draws the white in the card's off-white and the black in the suit's tint, and outlines the whole shape in the tint. Grays mix the two.
- Paint black whatever should take the tint, such as eyes or the gaps inside a face. Transparent areas show the card, with the outline around their edges. Icons from game-icons.net come white on transparent, so their gaps start out transparent.
- 256 by 256 pixels is enough: the game shrinks icons to 128 pixels.

## Other languages

Every text is a map from a language code to the text, such as `{"en": "Goblin", "de": "Kobold"}`. English is required, and any language a text lacks falls back to English.

## Licenses and credit

Icons must be under CC0, CC BY or CC BY-SA. Credit their authors in the theme's `credits`, and add the icons to [CREDITS.md](CREDITS.md) with each one's author, source and license. Icons from [game-icons.net](https://game-icons.net) are CC BY 3.0 and need their authors named.

Themes use original characters only. No names or likenesses from existing games, films or comics.

## Trying a theme

`./gradlew core:test` checks every theme in `themes.json`: all 44 cards are present, each text has English, each icon is a square PNG, and every file in `icons/` belongs to a card. Then run the game with `./gradlew lwjgl3:run` and pick the theme in Settings.
