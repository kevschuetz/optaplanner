package org.optaplanner.core.impl.localsearch.decider.forager.privacypreserving;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.optaplanner.core.impl.localsearch.scope.LocalSearchMoveScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchStepScope;

public class PrivacyPreservingTabuSearchForager<Solution_> extends AbstractPrivacyPreservingForager<Solution_> {
    private List<Solution_> tabuList;
    private Iterator<Solution_> it;
    private int tabuListSize;

    public PrivacyPreservingTabuSearchForager(int acceptedCountLimit, NeighbourhoodEvaluator<Solution_> neighbourhoodEvaluator,
            int tabuListSize) {
        super(acceptedCountLimit, neighbourhoodEvaluator);
        tabuList = new ArrayList<>();
        it = tabuList.listIterator();
        this.tabuListSize = tabuListSize;
    }

    @Override
    protected boolean isAccepted(LocalSearchMoveScope<Solution_> stepWinner) {
        var undoMove = stepWinner.getMove().doMove(stepWinner.getScoreDirector());
        var winningSolution = stepWinner.getScoreDirector().cloneWorkingSolution();
        undoMove.doMove(stepWinner.getScoreDirector());

        // Move is not tabu
        if (!tabuList.contains(winningSolution)) {
            logger.info("Accepted winner because it is not tabu.");
            return true;
        }

        // Move is tabu but aspiration
        if (stepWinner.getScore()
                .compareTo(stepWinner.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore()) >= 0) {
            logger.info("Accepted winner because of aspiration.");
            return true;
        }

        logger.info("Could not accept winner.");
        return false;
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        for (var solution : this.solutionMoveScopeMap.keySet()) {
            tabuList.add(solution);
            if (tabuList.size() >= tabuListSize) {
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
        }
    }
}
