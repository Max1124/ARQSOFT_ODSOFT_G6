package pt.psoft.g1.psoftg1.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.AdaptiveLendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.LendingPolicySelector;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.ReaderLendingHistory;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.StandardLendingPolicy;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Collaboration level: real selector, rule, policies and parameters wired from the shipped properties. */
class LendingPoliciesConfigTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(LendingPoliciesConfig.class);

    @Test
    void readerWithoutHistoryGetsTheStandardPolicy() {
        runner.run(context -> {
            LendingPolicySelector selector = context.getBean(LendingPolicySelector.class);
            assertThat(selector.select(new ReaderLendingHistory(TODAY, 0, 0, 0, 0)))
                    .isSameAs(context.getBean(StandardLendingPolicy.class));
        });
    }

    @Test
    void reliableReaderGetsTheAdaptivePolicy() {
        // shipped selection values: 6 months, at least 5 completed lendings, at least 90% on time
        runner.run(context -> {
            LendingPolicySelector selector = context.getBean(LendingPolicySelector.class);
            assertThat(selector.select(new ReaderLendingHistory(TODAY, 0, 0, 10, 10)))
                    .isSameAs(context.getBean(AdaptiveLendingPolicy.class));
        });
    }

    @Test
    void sameReaderFallsBackToStandardOnceALendingIsOverdue() {
        runner.run(context -> {
            LendingPolicySelector selector = context.getBean(LendingPolicySelector.class);
            assertThat(selector.select(new ReaderLendingHistory(TODAY, 1, 1, 10, 10)))
                    .isSameAs(context.getBean(StandardLendingPolicy.class));
        });
    }

    @Test
    void changedSelectionValueChangesWhoQualifiesWithoutCodeChange() {
        runner.withPropertyValues("lending.selection.min-completed-lendings=20").run(context -> {
            LendingPolicySelector selector = context.getBean(LendingPolicySelector.class);
            assertThat(selector.select(new ReaderLendingHistory(TODAY, 0, 0, 10, 10)))
                    .isSameAs(context.getBean(StandardLendingPolicy.class));
        });
    }
}
