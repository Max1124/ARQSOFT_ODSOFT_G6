package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LendingPolicySelectorTest {
    private static final ReaderLendingHistory HISTORY = new ReaderLendingHistory(LocalDate.of(2026, 3, 10), 0, 0, 0, 0);
    // Plain doubles: the selector only orders rules, so it needs no real policy behaviour.
    private final LendingPolicy standard = mock(LendingPolicy.class);
    private final LendingPolicy first = mock(LendingPolicy.class);
    private final LendingPolicy second = mock(LendingPolicy.class);

    private static PolicySelectionRule matching(LendingPolicy policy) {
        return history -> Optional.of(policy);
    }

    private static PolicySelectionRule notMatching() {
        return history -> Optional.empty();
    }

    @Test
    void returnsStandardWhenNoRuleMatches() {
        LendingPolicySelector selector = new LendingPolicySelector(List.of(notMatching(), notMatching()), standard);
        assertThat(selector.select(HISTORY)).isSameAs(standard);
    }

    @Test
    void returnsStandardWhenThereAreNoRules() {
        assertThat(new LendingPolicySelector(List.of(), standard).select(HISTORY)).isSameAs(standard);
    }

    @Test
    void returnsThePolicyOfTheMatchingRule() {
        LendingPolicySelector selector = new LendingPolicySelector(List.of(notMatching(), matching(first)), standard);
        assertThat(selector.select(HISTORY)).isSameAs(first);
    }

    @Test
    void firstMatchingRuleWinsWhenSeveralMatch() {
        LendingPolicySelector selector = new LendingPolicySelector(List.of(matching(first), matching(second)), standard);
        assertThat(selector.select(HISTORY)).isSameAs(first);
    }

    @Test
    void selectionIsEvaluatedAgainOnEveryCall() {
        // A rule whose answer changes between calls: nothing may be cached (LP-SEL2).
        List<LendingPolicy> answers = new ArrayList<>(List.of(first, second));
        PolicySelectionRule changing = history -> Optional.of(answers.remove(0));
        LendingPolicySelector selector = new LendingPolicySelector(List.of(changing), standard);

        assertThat(selector.select(HISTORY)).isSameAs(first);
        assertThat(selector.select(HISTORY)).isSameAs(second);
    }

    @Test
    void laterChangesToTheRuleListDoNotAffectTheSelector() {
        List<PolicySelectionRule> rules = new ArrayList<>(List.of(notMatching()));
        LendingPolicySelector selector = new LendingPolicySelector(rules, standard);

        rules.add(matching(first));

        assertThat(selector.select(HISTORY)).isSameAs(standard);
    }
}
