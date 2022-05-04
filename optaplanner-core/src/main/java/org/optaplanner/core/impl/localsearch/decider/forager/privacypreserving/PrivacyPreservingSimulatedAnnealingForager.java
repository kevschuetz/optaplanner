package org.optaplanner.core.impl.localsearch.decider.forager.privacypreserving;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.config.localsearch.decider.forager.EvaluationType;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchMoveScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchPhaseScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchStepScope;
import org.optaplanner.core.impl.score.ScoreUtils;

public class PrivacyPreservingSimulatedAnnealingForager<Solution_> extends AbstractPrivacyPreservingForager<Solution_> {

    protected Score startingTemperature;

    protected int levelsLength = -1;
    protected double[] startingTemperatureLevels;
    protected double[] temperatureLevels;
    protected double temperatureMinimum = 1.0E-100;

    public PrivacyPreservingSimulatedAnnealingForager(int acceptedCountLimit_, Score startingTemperature,
            NeighbourhoodEvaluator<Solution_> evaluator, EvaluationType evaluationType) {
        super(acceptedCountLimit_, evaluator, evaluationType);
        this.startingTemperature = startingTemperature;
    }

    @Override
    protected boolean isAccepted(LocalSearchMoveScope<Solution_> stepWinner) {
        LocalSearchPhaseScope<Solution_> phaseScope = stepWinner.getStepScope().getPhaseScope();
        Score lastStepScore = phaseScope.getLastCompletedStepScope().getScore();
        Score moveScore = stepWinner.getScore();
        // Pick move if it increases the score
        if (moveScore.compareTo(lastStepScore) >= 0) {
            logger.info("Found new winner with score: " + moveScore);
            return true;
        }

        // Pick move stochastically according to score difference and temperature

        // Get score differences
        Score moveScoreDifference = lastStepScore.subtract(moveScore);
        double[] moveScoreDifferenceLevels = ScoreUtils.extractLevelDoubles(moveScoreDifference);

        // Adjust accept chance for every level
        double acceptChance = 1.0;
        for (int i = 0; i < levelsLength; i++) {
            // Get score difference of level
            double moveScoreDifferenceLevel = moveScoreDifferenceLevels[i];
            double temperatureLevel = temperatureLevels[i];
            double acceptChanceLevel;
            if (moveScoreDifferenceLevel <= 0.0) {
                // In this level, moveScore is better than the lastStepScore, so do not disrupt the acceptChance
                acceptChanceLevel = 1.0;
            } else {
                acceptChanceLevel = Math.exp(-moveScoreDifferenceLevel / temperatureLevel);
            }
            acceptChance *= acceptChanceLevel;
        }
        // Pick move according to random number and accept chance
        if (stepWinner.getWorkingRandom().nextDouble() < acceptChance) {
            logger.info("Accepted move with score: " + moveScore);
            return true;
        } else {
            return false;
        }
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        // Set starting temperature for score levels
        for (double startingTemperatureLevel : ScoreUtils.extractLevelDoubles(startingTemperature)) {
            if (startingTemperatureLevel < 0.0) {
                throw new IllegalArgumentException("The startingTemperature (" + startingTemperature
                        + ") cannot have negative level (" + startingTemperatureLevel + ").");
            }
        }
        startingTemperatureLevels = ScoreUtils.extractLevelDoubles(startingTemperature);
        temperatureLevels = startingTemperatureLevels;
        levelsLength = startingTemperatureLevels.length;
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        startingTemperatureLevels = null;
        temperatureLevels = null;
        levelsLength = -1;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        // TimeGradient only refreshes at the beginning of a step, so this code is in stepStarted instead of stepEnded
        double timeGradient = stepScope.getTimeGradient();
        double reverseTimeGradient = 1.0 - timeGradient;
        temperatureLevels = new double[levelsLength];

        // Adjust/decrease all temperature levels (hard/soft) according to time passed
        for (int i = 0; i < levelsLength; i++) {
            temperatureLevels[i] = startingTemperatureLevels[i] * reverseTimeGradient;
            if (temperatureLevels[i] < temperatureMinimum) {
                temperatureLevels[i] = temperatureMinimum;
            }
            logger.info("New Temperature Level[" + i + "] is " + temperatureLevels[i]);
        }
    }

}
