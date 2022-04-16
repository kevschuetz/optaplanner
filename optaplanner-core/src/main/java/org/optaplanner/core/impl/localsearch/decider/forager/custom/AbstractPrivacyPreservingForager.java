package org.optaplanner.core.impl.localsearch.decider.forager.custom;

import java.util.*;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.localsearch.decider.forager.AbstractLocalSearchForager;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchMoveScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchPhaseScope;
import org.optaplanner.core.impl.localsearch.scope.LocalSearchStepScope;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;

/**
 * A privacy preserving forager that uses the privacy-engine to evaluate the candidates of the search steps.
 * This class can be extended to implement different selection mechanisms, by implementing the pickMoveUsingPrivacyEngineMap()
 * method
 * 
 * @param <Solution_>
 */
public abstract class AbstractPrivacyPreservingForager<Solution_> extends AbstractLocalSearchForager<Solution_> {
    protected Properties configuration;
    /**
     * Specifies how many moves are gathered before a winner is picked
     */
    protected int acceptedCountLimit;

    protected long selectedMoveCount;
    protected long acceptedMoveCount;

    // Privacy Engine
    private final NeighbourhoodEvaluator<Solution_> neighbourhoodEvaluator;

    // Score director to execute moves
    protected ScoreDirector<Solution_> scoreDirector;
    protected InnerScoreDirector<Solution_, ?> innerScoreDirector;

    // Highest score encountered
    HardSoftScore highScore;

    // Current winner of the search phase
    protected LocalSearchMoveScope<Solution_> lastPickedMoveScope;

    // Collections storing candidates
    protected List<LocalSearchMoveScope<Solution_>> candidateMoveScopes;
    protected Map<Solution_, LocalSearchMoveScope<Solution_>> solutionMoveScopeMap;

    // Statistics
    protected int iterations;

    protected AbstractPrivacyPreservingForager() {
        this(50, null);
    }

    protected AbstractPrivacyPreservingForager(int acceptedCountLimit,
            NeighbourhoodEvaluator<Solution_> neighbourhoodEvaluator) {
        logger.info("Initialized " + this.getClass());
        this.acceptedCountLimit = acceptedCountLimit;

        this.highScore = HardSoftScore.of(0, 0);
        this.candidateMoveScopes = new ArrayList<>();
        this.solutionMoveScopeMap = new HashMap<>();
        this.lastPickedMoveScope = null;
        this.neighbourhoodEvaluator = neighbourhoodEvaluator;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************
    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        this.scoreDirector = phaseScope.getScoreDirector();
        if (scoreDirector != null)
            this.innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        this.iterations = 0;
    }

    /**
     * Resets the move counts and notifies the finalistPodium
     * 
     * @param stepScope the step scope
     */
    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        selectedMoveCount = 0L;
        acceptedMoveCount = 0L;

        candidateMoveScopes.clear();
        solutionMoveScopeMap.clear();
        iterations++;
    }

    /**
     * Required to check compliance with a never ending move selector, otherwise steps would never end
     * 
     * @return boolean indicating if acceptedCountLimit has been set
     */
    @Override
    public boolean supportsNeverEndingMoveSelector() {
        // TODO FIXME magical value Integer.MAX_VALUE coming from ForagerConfig
        return acceptedCountLimit < Integer.MAX_VALUE;
    }

    @Override
    public void addMove(LocalSearchMoveScope<Solution_> moveScope) {
        selectedMoveCount++;
        if (Boolean.TRUE.equals(moveScope.getAccepted())) {
            acceptedMoveCount++;
            candidateMoveScopes.add(moveScope);
        }
    }

    /**
     * Checks if enough moves have been gathered according to the limit of accepted moves
     * 
     * @return boolean indicating if limit has been reached
     */
    @Override
    public boolean isQuitEarly() {
        return acceptedMoveCount >= acceptedCountLimit;
    }

    /**
     * Picks a move from all the candidates for the next step
     * 
     * @param stepScope the scope of the step
     * @return the winning move
     */
    @Override
    public LocalSearchMoveScope<Solution_> pickMove(LocalSearchStepScope<Solution_> stepScope) {
        stepScope.setSelectedMoveCount(selectedMoveCount);
        stepScope.setAcceptedMoveCount(acceptedMoveCount);

        // Request the evaluation from the privacy engine
        var orderMap = getOrder();

        if (orderMap == null || orderMap.isEmpty())
            return lastPickedMoveScope;

        // Extract the winner and assign the high score to the move
        var optOrderEntry = orderMap.entrySet().stream().findFirst();
        var optWinner = optOrderEntry.get().getValue().stream().findFirst();

        if (optWinner.isEmpty())
            return lastPickedMoveScope;

        LocalSearchMoveScope<Solution_> winner = optWinner.get();
        winner.setScore(optOrderEntry.get().getKey());

        // Return winner if accepted
        if (this.isAccepted(winner)) {
            this.highScore = (HardSoftScore) winner.getScore();
            this.lastPickedMoveScope = winner;
            return winner;
        }
        // Else return current winner
        logger.error("REACHED LOCAL OPTIMUM, RESTARTING THE STEP!");
        return lastPickedMoveScope;
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        selectedMoveCount = 0L;
        acceptedMoveCount = 0L;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + acceptedCountLimit + ")";
    }

    // ************************************************************************
    // Custom Methods
    // ************************************************************************

    /**
     * Picks the move according to the mapping of the maximum score of the current candidates and the sorted list of the
     * candidates.
     * This method has to be implemented by foragers to facilitate a certain search algorithm mechanism.
     * Every implementation has to check if the currentWinner is null to avoid not setting a winner if the initial solution is
     * already the best solution.
     * 
     * @return the winning move scope or null
     */
    protected abstract boolean isAccepted(LocalSearchMoveScope<Solution_> stepWinner);

    /**
     * Gets the mapping from the max score of the candidates to a sorted list from the privacy engine
     * 
     * @return the map
     */
    private Map<HardSoftScore, List<LocalSearchMoveScope<Solution_>>> getOrder() {
        mapCandidatesToMoveScopes();
        return evaluateStepCandidates();
    }

    private Map<HardSoftScore, List<LocalSearchMoveScope<Solution_>>> evaluateStepCandidates() {
        List<Solution_> candidates = new ArrayList<>(solutionMoveScopeMap.keySet());
        Map<HardSoftScore, List<Solution_>> map = neighbourhoodEvaluator.evaluateNeighbourhood(candidates);
        return this.mapOrderOfMoveScopes(map);
    }

    private Map<HardSoftScore, List<LocalSearchMoveScope<Solution_>>>
            mapOrderOfMoveScopes(Map<HardSoftScore, List<Solution_>> map) {
        var optEntry = map.entrySet().stream().findFirst();

        // Initialize Result
        HashMap<HardSoftScore, List<LocalSearchMoveScope<Solution_>>> result = new HashMap<>();
        List<LocalSearchMoveScope<Solution_>> sortedMoveScopeList = new ArrayList<>();
        HardSoftScore maxScore = null;

        // Get sorted list and max score from privacy engine
        List<Solution_> sortedFlightPrioritizationList = new ArrayList<>();

        // Check if entry is present and set max-score and assign list
        if (optEntry.isPresent()) {
            sortedFlightPrioritizationList = optEntry.get().getValue();
            maxScore = optEntry.get().getKey();
        }

        // Iterate over sorted flight prioritizations and add corresponding MoveScope to result
        for (Solution_ flightPrio : sortedFlightPrioritizationList) {
            sortedMoveScopeList.add(this.solutionMoveScopeMap.get(flightPrio));
        }

        result.put(maxScore, sortedMoveScopeList);

        if (!sortedMoveScopeList.isEmpty())
            return result;
        return null;
    }

    /**
     * Initializes the collections for each step required for communication with the privacy-engine
     */
    private void mapCandidatesToMoveScopes() {
        // Iterate over the accepted move-scopes of the step
        for (var moveScope : candidateMoveScopes) {
            // Execute the move
            var undoMove = moveScope.getMove().doMove(scoreDirector);

            // Map the flight-prioritization to the index of the move
            solutionMoveScopeMap.put(innerScoreDirector.cloneWorkingSolution(), moveScope);

            // Undo the move
            undoMove.doMove(scoreDirector);
        }
    }
}
