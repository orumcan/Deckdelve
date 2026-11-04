package io.github.orumcan.deckdelve;

import io.github.orumcan.deckdelve.logic.Event;
import java.util.List;

/** Sums up what an action did in one line, since the table shows the result but not how it got there. */
final class StatusText {
    private StatusText() {}

    static String of(List<Event> events, Ui ui) {
        String message = "";
        boolean dealt = false;
        for (Event event : events) {
            if (event instanceof Event.CardDealt) {
                dealt = true;
            } else if (event instanceof Event.RoomAvoided) {
                message = ui.text("status.avoided");
            } else if (event instanceof Event.WeaponEquipped equipped) {
                message = ui.text(equipped.replaced().isPresent() ? "status.replaced" : "status.equipped");
            } else if (event instanceof Event.PotionDrunk drunk) {
                message = drunk.healed() > 0 ? ui.text("status.healed", drunk.healed()) : ui.text("status.fullHealth");
            } else if (event instanceof Event.PotionWasted) {
                message = ui.text("status.wasted");
            } else if (event instanceof Event.MonsterFought fought) {
                message = fought.damage() > 0 ? ui.text("status.damage", fought.damage()) : ui.text("status.noDamage");
            }
        }
        if (dealt) {
            message = message.isEmpty() ? ui.text("status.newRoom") : message + " " + ui.text("status.newRoom");
        }
        return message;
    }
}
