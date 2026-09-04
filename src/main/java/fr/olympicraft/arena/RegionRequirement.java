package fr.olympicraft.arena;

public record RegionRequirement(
        int minimum,
        int maximum
) {

    public static final int UNLIMITED = -1;

    public RegionRequirement {
        if (minimum < 0) {
            throw new IllegalArgumentException(
                    "Le minimum ne peut pas être négatif."
            );
        }

        if (maximum != UNLIMITED && maximum < minimum) {
            throw new IllegalArgumentException(
                    "Le maximum doit être supérieur "
                            + "ou égal au minimum."
            );
        }
    }

    public static RegionRequirement optionalSingle() {
        return new RegionRequirement(0, 1);
    }

    public static RegionRequirement requiredSingle() {
        return new RegionRequirement(1, 1);
    }

    public static RegionRequirement optionalMultiple() {
        return new RegionRequirement(0, UNLIMITED);
    }

    public static RegionRequirement requiredMultiple(
            int minimum
    ) {
        return new RegionRequirement(minimum, UNLIMITED);
    }

    public boolean unlimited() {
        return maximum == UNLIMITED;
    }

    public boolean canAdd(int currentAmount) {
        return unlimited() || currentAmount < maximum;
    }

    public boolean satisfiedBy(int amount) {
        if (amount < minimum) {
            return false;
        }

        return unlimited() || amount <= maximum;
    }
}
