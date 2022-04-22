package org.optaplanner.core.impl.localsearch.decider.forager.privacypreserving;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchMoveScope;

public class PrivacyPreservingGreatDelugeForager<Solution_> extends AbstractPrivacyPreservingForager<Solution_> {
    private Score initialWaterLevel;
    private Score waterLevelIncrementScore;
    private Double waterLevelIncrementRatio;

    private Score startingWaterLevel = null;

    private Score currentWaterLevel = null;
    private Double currentWaterLevelRatio = null;

    public Score getWaterLevelIncrementScore() {
        return this.waterLevelIncrementScore;
    }

    public void setWaterLevelIncrementScore(Score waterLevelIncrementScore) {
        this.waterLevelIncrementScore = waterLevelIncrementScore;
    }

    public Score getInitialWaterLevel() {
        return this.initialWaterLevel;
    }

    public void setInitialWaterLevel(Score initialLevel) {
        this.initialWaterLevel = initialLevel;
    }

    public Double getWaterLevelIncrementRatio() {
        return this.waterLevelIncrementRatio;
    }

    public void setWaterLevelIncrementRatio(Double waterLevelIncrementRatio) {
        this.waterLevelIncrementRatio = waterLevelIncrementRatio;
    }

    @Override
    protected boolean isAccepted(LocalSearchMoveScope<Solution_> stepWinner) {
        return false;
    }

}
