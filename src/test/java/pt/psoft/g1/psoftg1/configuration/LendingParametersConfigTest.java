package pt.psoft.g1.psoftg1.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.AdaptivePolicyParameters;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.PolicySelectionParameters;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.StandardPolicyParameters;

import static org.assertj.core.api.Assertions.assertThat;

/** Setup-time binding of the lending properties; starts only the binding configuration, not the application. */
class LendingParametersConfigTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(LendingParametersConfig.class);

    private static String allMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable t = failure; t != null; t = t.getCause()) {
            messages.append(t.getMessage()).append(" | ");
        }
        return messages.toString();
    }

    @Test
    void shippedPropertiesBindToValidParameterObjects() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(StandardPolicyParameters.class))
                    .isEqualTo(new StandardPolicyParameters(3, 15, 2, 7));
            assertThat(context.getBean(AdaptivePolicyParameters.class))
                    .isEqualTo(new AdaptivePolicyParameters(5, 2, 4, 21, 15, 7));
            assertThat(context.getBean(PolicySelectionParameters.class))
                    .isEqualTo(new PolicySelectionParameters(6, 5, 90.0));
        });
    }

    @Test
    void externalValueOverridesShippedValueWithoutCodeChange() {
        runner.withPropertyValues("lending.standard.lending-period-days=21")
                .run(context -> assertThat(context.getBean(StandardPolicyParameters.class).lendingPeriodDays())
                        .isEqualTo(21));
    }

    @Test
    void constraintViolationStopsStartupNamingParameterAndConstraint() {
        runner.withPropertyValues("lending.adaptive.first-threshold=4")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(allMessages(context.getStartupFailure()))
                            .contains("FirstThreshold=4")
                            .contains("SecondThreshold (4)");
                });
    }

    @Test
    void nonPositiveValueStopsStartup() {
        runner.withPropertyValues("lending.standard.max-active-lendings=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(allMessages(context.getStartupFailure())).contains("MaxActiveLendings=0");
                });
    }

    @Test
    void malformedNumberStopsStartupNamingTheProperty() {
        runner.withPropertyValues("lending.selection.history-period-months=six")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(allMessages(context.getStartupFailure()))
                            .contains("lending.selection.history-period-months")
                            .contains("not an integer");
                });
    }

    @Test
    void missingValueStopsStartupNamingTheProperty() {
        runner.withPropertyValues("lending.standard.renewal-period-days=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(allMessages(context.getStartupFailure()))
                            .contains("Missing lending parameter 'lending.standard.renewal-period-days'");
                });
    }
}
