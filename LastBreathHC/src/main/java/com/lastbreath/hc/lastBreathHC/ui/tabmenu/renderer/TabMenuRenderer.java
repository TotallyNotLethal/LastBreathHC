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
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class TabMenuRenderer {
    private static final int COLUMN_GAP = 4;
    private static final ColumnLayout LEFT_COLUMN_LAYOUT = new ColumnLayout(32, ColumnAlignment.LEFT);
    private static final ColumnLayout RIGHT_COLUMN_LAYOUT = new ColumnLayout(32, ColumnAlignment.LEFT);
    private static final int PING_BAR_COUNT = 5;
    private static final int SECTION_SPACING_LINES = 1;
    private static final NamedTextColor DEFAULT_RANK_COLOR = NamedTextColor.WHITE;
    private static final NamedTextColor DEFAULT_STATS_COLOR = NamedTextColor.GRAY;
    private static final NamedTextColor FOOTER_URL_COLOR = NamedTextColor.AQUA;
    private static final NamedTextColor PING_EXCELLENT_COLOR = NamedTextColor.GREEN;
    private static final NamedTextColor PING_GOOD_COLOR = NamedTextColor.YELLOW;
    private static final NamedTextColor PING_WARN_COLOR = NamedTextColor.GOLD;
    private static final NamedTextColor PING_POOR_COLOR = NamedTextColor.RED;
    private static final NamedTextColor PING_INACTIVE_COLOR = NamedTextColor.DARK_GRAY;
    private static final Style HEADER_STYLE = Style.style(TextDecoration.BOLD);
    private static final Style FOOTER_STYLE = Style.style(TextDecoration.ITALIC);
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
        lines.addAll(renderLines(header.lines(), DEFAULT_STATS_COLOR, HEADER_STYLE));
        addSectionSpacing(lines);
        return lines;
    }

    private List<Component> renderFooter(TabMenuModel.FooterFields footer) {
        List<Component> lines = new ArrayList<>();
        addSectionSpacing(lines);
        lines.addAll(renderLines(footer.lines(), DEFAULT_STATS_COLOR, FOOTER_STYLE));
        for (String url : footer.urls()) {
            if (url != null && !url.isBlank()) {
                lines.add(Component.text(url).color(FOOTER_URL_COLOR).style(FOOTER_STYLE));
            }
        }
        return lines;
    }

    private List<Component> renderLines(List<TabMenuModel.LineFields> lines,
                                        TextColor fallbackColor,
                                        Style style) {
        List<Component> rendered = new ArrayList<>(lines.size());
        for (TabMenuModel.LineFields line : lines) {
            if (line == null || line.text() == null || line.text().isBlank()) {
                continue;
            }
            String text = line.text();
            String color = line.color();
            Component component;
            if (hasLegacyFormatting(text)) {
                component = legacySerializer.deserialize(text);
            } else {
                component = Component.text(text).color(parseColor(color, fallbackColor));
            }
            rendered.add(style != null ? component.style(style) : component);
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
        leftWidth = Math.max(leftWidth, LEFT_COLUMN_LAYOUT.width());

        int rightWidth = rightRows.stream()
                .mapToInt(RenderedRow::textLength)
                .max()
                .orElse(0);
        rightWidth = Math.max(rightWidth, RIGHT_COLUMN_LAYOUT.width());

        List<Component> lines = new ArrayList<>(leftRows.size());
        for (int i = 0; i < leftRows.size(); i++) {
            RenderedRow leftRow = leftRows.get(i);
            Component alignedLeft = alignRow(leftRow, leftWidth, LEFT_COLUMN_LAYOUT.alignment());
            TextComponent spacing = Component.text(" ".repeat(COLUMN_GAP));
            Component line = alignedLeft.append(spacing);
            if (i < rightRows.size()) {
                RenderedRow rightRow = rightRows.get(i);
                line = line.append(alignRow(rightRow, rightWidth, RIGHT_COLUMN_LAYOUT.alignment()));
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

        String displayName = row.displayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = row.username();
        }
        TextColor nameColor = parseColor(row.customColor(), DEFAULT_RANK_COLOR);
        Component nameComponent = hasLegacyFormatting(displayName)
                ? legacySerializer.deserialize(displayName)
                : Component.text(displayName).color(nameColor);

        Component pingBars = renderPingBars(row.pingBars());

        Component combined = Component.empty()
                .append(iconComponent)
                .append(prefixComponent)
                .append(nameComponent)
                .append(suffixComponent)
                .append(Component.text(" "))
                .append(pingBars);

        int textLength = calculateTextLength(iconText, prefixText, displayName, suffixText)
                + 1
                + PING_BAR_COUNT;

        return new RenderedRow(combined, textLength);
    }

    private Component renderPingBars(int bars) {
        int clampedBars = Math.max(0, Math.min(PING_BAR_COUNT, bars));
        NamedTextColor activeColor = switch (clampedBars) {
            case 5, 4 -> PING_EXCELLENT_COLOR;
            case 3 -> PING_GOOD_COLOR;
            case 2 -> PING_WARN_COLOR;
            default -> PING_POOR_COLOR;
        };

        Component result = Component.empty();
        for (int i = 0; i < PING_BAR_COUNT; i++) {
            NamedTextColor color = i < clampedBars ? activeColor : PING_INACTIVE_COLOR;
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

    private Component alignRow(RenderedRow row, int width, ColumnAlignment alignment) {
        int padding = Math.max(0, width - row.textLength());
        if (padding == 0) {
            return row.component();
        }
        TextComponent paddingComponent = Component.text(" ".repeat(padding));
        return alignment == ColumnAlignment.RIGHT
                ? paddingComponent.append(row.component())
                : row.component().append(paddingComponent);
    }

    private TextColor parseColor(String customColor, TextColor fallbackColor) {
        if (customColor == null || customColor.isBlank()) {
            return fallbackColor;
        }
        String trimmed = customColor.trim();
        if (trimmed.startsWith("#")) {
            TextColor color = TextColor.fromHexString(trimmed);
            if (color != null) {
                return color;
            }
        }
        NamedTextColor named = NamedTextColor.NAMES.value(trimmed.toLowerCase(Locale.ROOT));
        return named != null ? named : fallbackColor;
    }

    private int calculateTextLength(String icon, String prefix, String displayName, String suffix) {
        int length = 0;
        if (icon != null && !icon.isBlank()) {
            length += stripLegacy(icon).length() + 1;
        }
        if (prefix != null && !prefix.isBlank()) {
            length += stripLegacy(prefix).length() + 1;
        }
        length += displayName != null ? stripLegacy(displayName).length() : 0;
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

    private enum ColumnAlignment {
        LEFT,
        RIGHT
    }

    private record ColumnLayout(int width, ColumnAlignment alignment) {
    }

    private record RenderedRow(Component component, int textLength) {
    }
}
