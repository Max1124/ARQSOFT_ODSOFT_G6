package pt.psoft.g1.psoftg1.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.AdaptivePolicyParameters;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.PolicySelectionParameters;
import pt.psoft.g1.psoftg1.lendingmanagement.services.policy.StandardPolicyParameters;

/**
 * Binds the lending setup properties into validated, immutable parameter objects at startup.
 * A missing, malformed or constraint-violating value stops startup and names the parameter.
 */
@Configuration
// Binding is kept here, not in the records, so policies stay free of Spring (CON-LP-4).
@PropertySource({"classpath:config/library.properties"})
public class LendingParametersConfig {

    @Bean
    public StandardPolicyParameters standardPolicyParameters(Environment env) {
        return new StandardPolicyParameters(
                intValue(env, "lending.standard.max-active-lendings"),
                intValue(env, "lending.standard.lending-period-days"),
                intValue(env, "lending.standard.max-renewals"),
                intValue(env, "lending.standard.renewal-period-days"));
    }

    @Bean
    public AdaptivePolicyParameters adaptivePolicyParameters(Environment env) {
        return new AdaptivePolicyParameters(
                intValue(env, "lending.adaptive.max-active-lendings"),
                intValue(env, "lending.adaptive.first-threshold"),
                intValue(env, "lending.adaptive.second-threshold"),
                intValue(env, "lending.adaptive.long-period-days"),
                intValue(env, "lending.adaptive.standard-period-days"),
                intValue(env, "lending.adaptive.short-period-days"));
    }

    @Bean
    public PolicySelectionParameters policySelectionParameters(Environment env) {
        return new PolicySelectionParameters(
                intValue(env, "lending.selection.history-period-months"),
                intValue(env, "lending.selection.min-completed-lendings"),
                doubleValue(env, "lending.selection.min-on-time-ratio-percent"));
    }

    // Missing values stop startup instead of silently defaulting, so the operator sees which key is wrong (spec 5.3).
    private static String raw(Environment env, String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing lending parameter '" + key + "'");
        }
        return value.trim();
    }

    private static int intValue(Environment env, String key) {
        String value = raw(env, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid lending parameter '" + key + "': '" + value + "' is not an integer", e);
        }
    }

    private static double doubleValue(Environment env, String key) {
        String value = raw(env, key);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid lending parameter '" + key + "': '" + value + "' is not a number", e);
        }
    }
}
