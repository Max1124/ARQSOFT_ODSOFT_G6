package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

/** Quantitative parameters of the Adaptive Lending Policy (specification 3.1-3.5); validated on construction. */
public record AdaptivePolicyParameters(
        int maxActiveLendings,
        int firstThreshold,
        int secondThreshold,
        int longPeriodDays,
        int standardPeriodDays,
        int shortPeriodDays) {

    // Ordering rules of spec 3.5 are checked here, so the policy can trust its thresholds and periods.
    public AdaptivePolicyParameters {
        PolicyParameterChecks.positive("MaxActiveLendings", maxActiveLendings, "3.1");
        PolicyParameterChecks.positive("LongPeriod", longPeriodDays, "3.2");
        PolicyParameterChecks.positive("StandardPeriod", standardPeriodDays, "3.3");
        PolicyParameterChecks.positive("ShortPeriod", shortPeriodDays, "3.4");
        PolicyParameterChecks.positive("FirstThreshold", firstThreshold, "3.5");
        PolicyParameterChecks.require(firstThreshold < secondThreshold, "FirstThreshold", firstThreshold,
                "must be < SecondThreshold (" + secondThreshold + ")", "3.5");
        PolicyParameterChecks.require(secondThreshold < maxActiveLendings, "SecondThreshold", secondThreshold,
                "must be < MaxActiveLendings (" + maxActiveLendings + ")", "3.5");
        PolicyParameterChecks.require(longPeriodDays > standardPeriodDays, "LongPeriod", longPeriodDays,
                "must be > StandardPeriod (" + standardPeriodDays + ")", "3.5");
        PolicyParameterChecks.require(standardPeriodDays > shortPeriodDays, "StandardPeriod", standardPeriodDays,
                "must be > ShortPeriod (" + shortPeriodDays + ")", "3.5");
    }
}
