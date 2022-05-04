package org.optaplanner.core.impl.localsearch.decider.forager.privacypreserving;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.optaplanner.core.config.localsearch.decider.forager.EvaluationType;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchMoveScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchStepScope;

public class PrivacyPreservingTabuSearchForager<Solution_> extends AbstractPrivacyPreservingForager<Solution_> {
    private List<Solution_> tabuList;
    private int tabuListSize;

    public PrivacyPreservingTabuSearchForager(int acceptedCountLimit, NeighbourhoodEvaluator<Solution_> neighbourhoodEvaluator,
            int tabuListSize, EvaluationType evaluationType) {
        super(acceptedCountLimit, neighbourhoodEvaluator, evaluationType);
        tabuList = new ArrayList<>();
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
        tabuList.addAll(this.solutionMoveScopeMap.keySet());
        if (tabuList.size() > tabuListSize) {
            tabuList = tabuList.subList(tabuList.size() - tabuListSize, tabuList.size());
        }
    }
}
