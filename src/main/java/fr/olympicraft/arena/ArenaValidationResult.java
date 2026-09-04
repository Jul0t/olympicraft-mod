package fr.olympicraft.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ArenaValidationResult {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void error(String message) {
        errors.add(message);
    }

    public void warning(String message) {
        warnings.add(message);
    }

    public boolean valid() {
        return errors.isEmpty();
    }

    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }
}
