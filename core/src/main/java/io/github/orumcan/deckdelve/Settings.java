package io.github.orumcan.deckdelve;

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import io.github.orumcan.deckdelve.logic.Card;
import io.github.orumcan.deckdelve.logic.Modifier;
import io.github.orumcan.deckdelve.logic.RulesConfig;
import io.github.orumcan.deckdelve.logic.RulesConfig.FinalRoom;
import io.github.orumcan.deckdelve.logic.RulesConfig.WeaponRule;
import io.github.orumcan.deckdelve.logic.Scoring;
import io.github.orumcan.deckdelve.logic.Suit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class Settings {
    /** Every tile shows the same card, so the themes compare like for like. */
    private static final Card PREVIEW = new Card(Suit.CLUBS, Card.ACE);

    private static final float TILE_WIDTH = 300;
    private static final float TILE_PAD = 24;

    private Settings() {}

    static Table content(Ui ui, Consumer<String> useTheme, Profile profile) {
        Table page = new Table();
        Pages.section(page, ui, "settings.cards.title");
        Pages.paragraph(page, ui, "settings.cards.body");
        page.add(themes(ui, useTheme)).padTop(32).row();

        Pages.section(page, ui, "settings.modifiers.title");
        Pages.paragraph(page, ui, "settings.modifiers.body");
        Label total = new Label(total(ui, profile.rules().modifiers()), ui.styles.accent);
        for (Modifier modifier : Modifier.values()) {
            Toggle toggle = new Toggle(
                    ui,
                    ui.text(Ui.key("modifier", modifier)),
                    ui.multiplier(modifier.hundredths()),
                    ui.text(Ui.key("modifier", modifier) + ".body"),
                    profile.rules().has(modifier));
            StageScreen.onChange(toggle, () -> {
                Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
                modifiers.addAll(profile.rules().modifiers());
                if (toggle.isChecked()) {
                    modifiers.add(modifier);
                } else {
                    modifiers.remove(modifier);
                }
                profile.rules(profile.rules().withModifiers(modifiers));
                total.setText(total(ui, modifiers));
            });
            page.add(toggle).width(Pages.TEXT_WIDTH).padTop(16).row();
        }
        page.add(total).left().padTop(16).row();

        Pages.section(page, ui, "settings.rules.title");
        Pages.paragraph(page, ui, "settings.rules.body");
        variant(
                page,
                ui,
                profile,
                "strictly_lower",
                rules -> rules.weaponRule() == WeaponRule.STRICTLY_LOWER,
                (rules, on) -> rules.withWeaponRule(on ? WeaponRule.STRICTLY_LOWER : WeaponRule.LOWER_OR_EQUAL));
        variant(
                page,
                ui,
                profile,
                "leave_last_card",
                rules -> rules.finalRoom() == FinalRoom.LEAVE_LAST_CARD,
                (rules, on) -> rules.withFinalRoom(on ? FinalRoom.LEAVE_LAST_CARD : FinalRoom.RESOLVE_EVERY_CARD));
        variant(
                page,
                ui,
                profile,
                "subtract_room_monsters",
                RulesConfig::subtractRoomMonsters,
                RulesConfig::withSubtractRoomMonsters);
        variant(page, ui, profile, "floor_health", RulesConfig::floorHealthAtZero, RulesConfig::withFloorHealthAtZero);
        return page;
    }

    private static String total(Ui ui, Set<Modifier> modifiers) {
        return ui.text("settings.modifiers.total", ui.multiplier(Scoring.multiplier(modifiers)));
    }

    private static void variant(
            Table page,
            Ui ui,
            Profile profile,
            String key,
            Predicate<RulesConfig> isOn,
            BiFunction<RulesConfig, Boolean, RulesConfig> switched) {
        Toggle toggle = new Toggle(
                ui, ui.text("variant." + key), "", ui.text("variant." + key + ".body"), isOn.test(profile.rules()));
        StageScreen.onChange(toggle, () -> profile.rules(switched.apply(profile.rules(), toggle.isChecked())));
        page.add(toggle).width(Pages.TEXT_WIDTH).padTop(16).row();
    }

    /** One tile per theme, two to a row. Picking one applies it at once. */
    private static Table themes(Ui ui, Consumer<String> useTheme) {
        Table grid = new Table();
        grid.defaults().width(TILE_WIDTH).space(24);
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        List<String> ids = ui.themes.ids();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            TextButton tile = tile(ui, id);
            tile.setChecked(id.equals(ui.themeId()));
            group.add(tile);
            StageScreen.onChange(tile, () -> {
                if (tile.isChecked()) {
                    useTheme.accept(id);
                }
            });
            grid.add(tile);
            if (i % 2 == 1) {
                grid.row();
            }
        }
        return grid;
    }

    private static TextButton tile(Ui ui, String id) {
        TextButton tile = new TextButton(ui.themeName(id), ui.styles.choice);
        // Only a tap applies a theme. Setting the tiles up, and the group unchecking the old one, stay silent.
        tile.setProgrammaticChangeEvents(false);
        tile.getLabel().setWrap(true);
        // TextButton keeps its label in its only cell; the card goes above it.
        tile.clearChildren();
        tile.pad(TILE_PAD, TILE_PAD, TILE_PAD + Surfaces.DEPTH, TILE_PAD);
        tile.add(CardActor.face(ui.cards, PREVIEW, ui.theme(id)))
                .size(CardActor.WIDTH, CardActor.HEIGHT)
                .row();
        tile.add(tile.getLabel()).width(TILE_WIDTH - 2 * TILE_PAD).padTop(16);
        return tile;
    }
}
