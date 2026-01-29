package com.lastbreath.hc.lastBreathHC.ui.tabmenu.renderer;

import java.util.List;
import net.kyori.adventure.text.Component;

public record TabMenuRenderResult(Component header,
                                  Component footer,
                                  List<Component> playerLines) {
}
