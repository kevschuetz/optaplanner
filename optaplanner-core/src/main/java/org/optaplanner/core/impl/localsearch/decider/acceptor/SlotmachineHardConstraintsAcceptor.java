package org.optaplanner.core.impl.localsearch.decider.acceptor;

import org.optaplanner.core.impl.localsearch.scope.LocalSearchMoveScope;

public class SlotmachineHardConstraintsAcceptor<Solution_> extends AbstractAcceptor<Solution_> {
    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        return true;
    }
}
