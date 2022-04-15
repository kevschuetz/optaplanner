package at.jku.dke.slotmachine.optimizer.optimization.optaplanner;

import java.util.List;
import java.util.Map;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

public interface NeighbourhoodEvaluator<Solution_> {
    public Map<HardSoftScore, List<Solution_>> evaluateNeighbourhood(List<Solution_> candidates);
}
