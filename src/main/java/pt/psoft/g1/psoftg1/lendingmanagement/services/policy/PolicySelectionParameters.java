package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

/** Quantitative parameters of the Lending Policy Selection (specification 4.2); validated on construction. */
public record PolicySelectionParameters(
        int historyPeriodMonths,
        int minCompletedLendings,
        double minOnTimeRatioPercent) {

    // Ratio is a percentage (0-100) per spec 4.2.2; checked here so the eligibility rule needs no guards.
    public PolicySelectionParameters {
        PolicyParameterChecks.positive("HistoryPeriod", historyPeriodMonths, "4.2");
        PolicyParameterChecks.positive("MinCompletedLendings", minCompletedLendings, "4.2.1");
        PolicyParameterChecks.require(minOnTimeRatioPercent >= 0.0 && minOnTimeRatioPercent <= 100.0,
                "MinOnTimeRatio", minOnTimeRatioPercent, "must be between 0 and 100 (percent)", "4.2.2");
    }
}
