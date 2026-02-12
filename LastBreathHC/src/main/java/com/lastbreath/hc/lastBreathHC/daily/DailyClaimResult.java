package com.lastbreath.hc.lastBreathHC.daily;

import java.util.List;

public record DailyClaimResult(DailyClaimStatus status, int streak, List<String> grantedRewards) {
}
