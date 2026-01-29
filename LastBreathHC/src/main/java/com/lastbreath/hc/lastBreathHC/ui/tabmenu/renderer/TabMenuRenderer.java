package com.lastbreath.hc.lastBreathHC.ui.tabmenu.renderer;

import com.lastbreath.hc.lastBreathHC.ui.tabmenu.TabMenuModel;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.TabMenuModel.PlayerRowFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class TabMenuRenderer {
    private static final int COLUMN_GAP = 4;
    private static final int PING_BAR_COUNT = 5;
    private static final int SECTION_SPACING_LINES = 1;
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("[§&][0-9A-FK-ORa-fk-or]");

    private final LegacyComponentSerializer legacySerializer;

    public TabMenuRenderer() {
        this(LegacyComponentSerializer.legacySection());
    }

    public TabMenuRenderer(LegacyComponentSerializer legacySerializer) {
        this.legacySerializer = Objects.requireNonNull(legacySerializer, "legacySerializer");
    }

    public TabMenuRenderResult render(TabMenuModel model) {
        Objects.requireNonNull(model, "model");
        List<Component> playerLines = renderPlayers(model.players());
        Component header = joinLines(renderHeader(model.header()));
        Component footer = joinLines(renderFooter(model.footer()));
        return new TabMenuRenderResult(header, footer, playerLines);
    }

    private List<Component> renderHeader(TabMenuModel.HeaderFields header) {
        List<Component> lines = new ArrayList<>();
        lines.addAll(renderLines(header.lines()));
        addSectionSpacing(lines);
        return lines;
    }

    private List<Component> renderFooter(TabMenuModel.FooterFields footer) {
        List<Component> lines = new ArrayList<>();
        addSectionSpacing(lines);
        lines.addAll(renderLines(footer.lines()));
        for (String url : footer.urls()) {
            if (url != null && !url.isBlank()) {
                lines.add(Component.text(url).color(NamedTextColor.AQUA));
            }
        }
        return lines;
    }

    private List<Component> renderLines(List<TabMenuModel.LineFields> lines) {
        List<Component> rendered = new ArrayList<>(lines.size());
        for (TabMenuModel.LineFields line : lines) {
            if (line == null || line.text() == null || line.text().isBlank()) {
                continue;
            }
            String text = line.text();
            String color = line.color();
            if (hasLegacyFormatting(text) || color == null || color.isBlank()) {
                rendered.add(legacySerializer.deserialize(text));
            } else {
                rendered.add(Component.text(text).color(parseColor(color)));
            }
        }
        return rendered;
    }

    private List<Component> renderPlayers(List<PlayerRowFields> players) {
        int totalPlayers = players.size();
        int leftSize = (totalPlayers + 1) / 2;
        List<PlayerRowFields> leftPlayers = players.subList(0, leftSize);
        List<PlayerRowFields> rightPlayers = players.subList(leftSize, totalPlayers);

        List<RenderedRow> leftRows = new ArrayList<>(leftPlayers.size());
        List<RenderedRow> rightRows = new ArrayList<>(rightPlayers.size());

        for (PlayerRowFields row : leftPlayers) {
            leftRows.add(renderRow(row));
        }
        for (PlayerRowFields row : rightPlayers) {
            rightRows.add(renderRow(row));
        }

        int leftWidth = leftRows.stream()
                .mapToInt(RenderedRow::textLength)
                .max()
                .orElse(0);

        List<Component> lines = new ArrayList<>(leftRows.size());
        for (int i = 0; i < leftRows.size(); i++) {
            RenderedRow leftRow = leftRows.get(i);
            int padding = Math.max(0, leftWidth - leftRow.textLength());
            TextComponent spacing = Component.text(" ".repeat(padding + COLUMN_GAP));
            Component line = leftRow.component().append(spacing);
            if (i < rightRows.size()) {
                line = line.append(rightRows.get(i).component());
            }
            lines.add(line);
        }
        return lines;
    }

    private RenderedRow renderRow(PlayerRowFields row) {
        Component iconComponent = Component.empty();
        String iconText = row.rankIcon();
        if (iconText != null && !iconText.isBlank()) {
            iconComponent = legacySerializer.deserialize(iconText).append(Component.text(" "));
        }

        String prefixText = row.prefix();
        Component prefixComponent = Component.empty();
        if (prefixText != null && !prefixText.isBlank()) {
            prefixComponent = legacySerializer.deserialize(prefixText).append(Component.text(" "));
        }

        String suffixText = row.suffix();
        Component suffixComponent = Component.empty();
        if (suffixText != null && !suffixText.isBlank()) {
            suffixComponent = Component.text(" ").append(legacySerializer.deserialize(suffixText));
        }

        TextColor nameColor = parseColor(row.customColor());
        Component nameComponent = Component.text(row.username()).color(nameColor);

        Component pingBars = renderPingBars(row.pingBars());

        Component combined = Component.empty()
                .append(iconComponent)
                .append(prefixComponent)
                .append(nameComponent)
                .append(suffixComponent)
                .append(Component.text(" "))
                .append(pingBars);

        int textLength = calculateTextLength(iconText, prefixText, row.username(), suffixText)
                + 1
                + PING_BAR_COUNT;

        return new RenderedRow(combined, textLength);
    }

    private Component renderPingBars(int bars) {
        int clampedBars = Math.max(0, Math.min(PING_BAR_COUNT, bars));
        NamedTextColor activeColor = switch (clampedBars) {
            case 5, 4 -> NamedTextColor.GREEN;
            case 3 -> NamedTextColor.YELLOW;
            case 2 -> NamedTextColor.GOLD;
            default -> NamedTextColor.RED;
        };

        Component result = Component.empty();
        for (int i = 0; i < PING_BAR_COUNT; i++) {
            NamedTextColor color = i < clampedBars ? activeColor : NamedTextColor.DARK_GRAY;
            result = result.append(Component.text("▮").color(color));
        }
        return result;
    }

    private Component joinLines(List<Component> lines) {
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private void addSectionSpacing(List<Component> lines) {
        for (int i = 0; i < SECTION_SPACING_LINES; i++) {
            lines.add(Component.empty());
        }
    }

    private TextColor parseColor(String customColor) {
        if (customColor == null || customColor.isBlank()) {
            return NamedTextColor.WHITE;
        }
        String trimmed = customColor.trim();
        if (trimmed.startsWith("#")) {
            TextColor color = TextColor.fromHexString(trimmed);
            if (color != null) {
                return color;
            }
        }
        NamedTextColor named = NamedTextColor.NAMES.value(trimmed.toLowerCase(Locale.ROOT));
        return named != null ? named : NamedTextColor.WHITE;
    }

    private int calculateTextLength(String icon, String prefix, String username, String suffix) {
        int length = 0;
        if (icon != null && !icon.isBlank()) {
            length += stripLegacy(icon).length() + 1;
        }
        if (prefix != null && !prefix.isBlank()) {
            length += stripLegacy(prefix).length() + 1;
        }
        length += username != null ? username.length() : 0;
        if (suffix != null && !suffix.isBlank()) {
            length += 1 + stripLegacy(suffix).length();
        }
        return length;
    }

    private String stripLegacy(String input) {
        if (input == null) {
            return "";
        }
        return LEGACY_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    private boolean hasLegacyFormatting(String input) {
        if (input == null) {
            return false;
        }
        return LEGACY_COLOR_PATTERN.matcher(input).find();
    }

    private record RenderedRow(Component component, int textLength) {
    }
}
