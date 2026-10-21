package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

/** Quantitative parameters of the Standard Lending Policy (specification 2.1-2.3); validated on construction. */
public record StandardPolicyParameters(
        int maxActiveLendings,
        int lendingPeriodDays,
        int maxRenewals,
        int renewalPeriodDays) {

    // Validated on construction so an invalid configuration can never exist as an object (spec 5.3).
    public StandardPolicyParameters {
        PolicyParameterChecks.positive("MaxActiveLendings", maxActiveLendings, "2.1");
        PolicyParameterChecks.positive("LendingPeriod", lendingPeriodDays, "2.2");
        PolicyParameterChecks.notNegative("MaxRenewals", maxRenewals, "2.3");
        PolicyParameterChecks.positive("RenewalPeriod", renewalPeriodDays, "2.3");
    }
}
