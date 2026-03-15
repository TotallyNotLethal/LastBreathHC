package com.lastbreath.hc.lastBreathHC.bootstrap;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleOrchestratorTest {

    @Test
    void registerAndShutdownFollowDeterministicLifecycleOrder() {
        List<String> calls = new ArrayList<>();
        PluginModule first = new RecordingModule("structure", calls);
        PluginModule second = new RecordingModule("holiday", calls);
        PluginModule third = new RecordingModule("nemesis", calls);

        ModuleOrchestrator orchestrator = new ModuleOrchestrator(List.of(first, second, third));

        orchestrator.registerAll();
        orchestrator.shutdownAll();

        assertEquals(
                List.of(
                        "register:structure",
                        "register:holiday",
                        "register:nemesis",
                        "shutdown:nemesis",
                        "shutdown:holiday",
                        "shutdown:structure"
                ),
                calls
        );
    }

    private record RecordingModule(String name, List<String> calls) implements PluginModule {
        @Override
        public void register() {
            calls.add("register:" + name);
        }

        @Override
        public void shutdown() {
            calls.add("shutdown:" + name);
        }
    }
}
