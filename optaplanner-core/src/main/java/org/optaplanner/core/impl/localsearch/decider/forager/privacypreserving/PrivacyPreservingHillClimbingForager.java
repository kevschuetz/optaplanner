package org.optaplanner.core.impl.localsearch.decider.forager.privacypreserving;

import org.optaplanner.core.impl.localsearch.scope.LocalSearchMoveScope;

public class PrivacyPreservingHillClimbingForager<Solution_> extends AbstractPrivacyPreservingForager<Solution_> {

    public PrivacyPreservingHillClimbingForager(int acceptedCountLimit_, NeighbourhoodEvaluator<Solution_> evaluator) {
        super(acceptedCountLimit_, evaluator);
    }

    /**
     * Implements a selection mechanism representing the Hill-Climbing idea by picking a move if the
     * highest score of the candidates is higher than the score of the current solution.
     * 
     * @return the winning move of the step
     */
    @Override
    protected boolean isAccepted(LocalSearchMoveScope<Solution_> winner) {
        var score = winner.getScore();

        // Pick move only if score gets improved
        if (score.compareTo(winner.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore()) >= 0) {
            logger.info("Found new winner with score: " + score);
            return true;
        }
        return false;
    }
}
