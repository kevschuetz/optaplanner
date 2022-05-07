package org.optaplanner.core.impl.localsearch.decider.forager.privacypreserving;

import java.util.List;
import java.util.Map;

import org.optaplanner.core.api.score.Score;

public interface NeighbourhoodEvaluator<Solution_> {
    public Map<Score, Solution_> getBestSolutionFromNeighbourhood(List<Solution_> candidates);
    public Map<Score, List<Solution_>> getCandidatesAboveThreshold(List<Solution_> candidates, double threshold);
}
