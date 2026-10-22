package pt.psoft.g1.psoftg1.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.AdaptiveEligibilityRule;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.AdaptiveLendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.AdaptivePolicyParameters;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.LendingPolicySelector;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.PolicySelectionParameters;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.StandardLendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.StandardPolicyParameters;

import java.util.List;

/**
 * Setup composition of the lending policies: the installed policies and the ordered selection rules.
 * Adding a policy adds a bean here and its parameter keys; the service and existing policies do not change (QAS3).
 */
@Configuration
@Import(LendingParametersConfig.class)
public class LendingPoliciesConfig {

    @Bean
    public StandardLendingPolicy standardLendingPolicy(StandardPolicyParameters parameters) {
        return new StandardLendingPolicy(parameters);
    }

    @Bean
    public AdaptiveLendingPolicy adaptiveLendingPolicy(AdaptivePolicyParameters parameters) {
        return new AdaptiveLendingPolicy(parameters);
    }

    // Rule order is stated here, in one visible place, because the first matching rule wins.
    @Bean
    public LendingPolicySelector lendingPolicySelector(StandardLendingPolicy standard,
                                                       AdaptiveLendingPolicy adaptive,
                                                       PolicySelectionParameters selectionParameters) {
        return new LendingPolicySelector(List.of(new AdaptiveEligibilityRule(selectionParameters, adaptive)), standard);
    }
}
