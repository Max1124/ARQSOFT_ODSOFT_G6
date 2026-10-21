package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

/** The lending being renewed, reduced to what a policy needs; the entity itself stays out of the policy. */
public record LendingToRenew(boolean overdue, int renewalsMade) {

    public LendingToRenew {
        if (renewalsMade < 0) {
            throw new IllegalArgumentException("Renewals made cannot be negative");
        }
    }
}
