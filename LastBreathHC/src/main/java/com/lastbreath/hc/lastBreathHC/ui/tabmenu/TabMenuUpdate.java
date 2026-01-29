package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.ui.tabmenu.renderer.TabMenuRenderResult;
import java.util.EnumSet;
import java.util.Objects;

public record TabMenuUpdate(EnumSet<TabMenuSection> sections,
                            TabMenuModel model,
                            TabMenuRenderResult renderResult) {

    public TabMenuUpdate {
        Objects.requireNonNull(sections, "sections");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(renderResult, "renderResult");
    }

    public boolean isEmpty() {
        return sections.isEmpty();
    }
}
