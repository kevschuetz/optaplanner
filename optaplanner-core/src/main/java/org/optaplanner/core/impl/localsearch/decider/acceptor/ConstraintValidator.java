package org.optaplanner.core.impl.localsearch.decider.acceptor;

import java.util.Collection;

public interface ConstraintValidator<Solution_> {
    public boolean satisfiesConstraints(Collection<?> entities);
}
