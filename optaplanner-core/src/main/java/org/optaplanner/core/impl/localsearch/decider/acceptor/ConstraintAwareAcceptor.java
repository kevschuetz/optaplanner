package org.optaplanner.core.impl.localsearch.decider.acceptor;

import org.optaplanner.core.impl.heuristic.selector.move.generic.SwapMove;
import org.optaplanner.core.impl.localsearch.AssignmentProblemType;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchMoveScope;

public class ConstraintAwareAcceptor<Solution_> extends AbstractAcceptor<Solution_> {
    private ConstraintValidator<Solution_> constraintValidator;
    private AssignmentProblemType assignmentProblemType = AssignmentProblemType.UNBALANCED;

    public ConstraintAwareAcceptor(ConstraintValidator<Solution_> constraintValidator,
            AssignmentProblemType assignmentProblemType) {
        this.assignmentProblemType = assignmentProblemType;
        this.constraintValidator = constraintValidator;
    }

    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        boolean satisfiesConstraints = constraintValidator.satisfiesConstraints(moveScope.getMove().getPlanningEntities());
        if (assignmentProblemType == AssignmentProblemType.BALANCED) {
            return satisfiesConstraints && moveScope.getMove() instanceof SwapMove;
        }
        return satisfiesConstraints;
    }
}
