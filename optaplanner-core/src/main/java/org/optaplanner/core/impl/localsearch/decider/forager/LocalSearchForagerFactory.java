/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.impl.localsearch.decider.forager;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.config.localsearch.decider.acceptor.stepcountinghillclimbing.StepCountingHillClimbingType;
import org.optaplanner.core.config.localsearch.decider.forager.FinalistPodiumType;
import org.optaplanner.core.config.localsearch.decider.forager.ForagerType;
import org.optaplanner.core.config.localsearch.decider.forager.LocalSearchForagerConfig;
import org.optaplanner.core.config.localsearch.decider.forager.LocalSearchPickEarlyType;
import org.optaplanner.core.impl.localsearch.decider.forager.privacypreserving.NeighbourhoodEvaluator;
import org.optaplanner.core.impl.localsearch.decider.forager.privacypreserving.PrivacyPreservingHillClimbingForager;
import org.optaplanner.core.impl.localsearch.decider.forager.privacypreserving.PrivacyPreservingSimulatedAnnealingForager;
import org.optaplanner.core.impl.localsearch.decider.forager.privacypreserving.PrivacyPreservingStepCountingHillClimbingForager;

public class LocalSearchForagerFactory<Solution_> {

    public static <Solution_> LocalSearchForagerFactory<Solution_> create(LocalSearchForagerConfig foragerConfig) {
        return new LocalSearchForagerFactory<>(foragerConfig);
    }

    private final LocalSearchForagerConfig foragerConfig;
    private NeighbourhoodEvaluator<Solution_> neighbourhoodEvaluator;

    public LocalSearchForagerFactory(LocalSearchForagerConfig foragerConfig) {
        this.foragerConfig = foragerConfig;
    }

    public LocalSearchForager<Solution_> buildForager() {
        if (foragerConfig.getForagerType() != null)
            return buildCustomForager();
        LocalSearchPickEarlyType pickEarlyType_ =
                Objects.requireNonNullElse(foragerConfig.getPickEarlyType(), LocalSearchPickEarlyType.NEVER);
        int acceptedCountLimit_ = Objects.requireNonNullElse(foragerConfig.getAcceptedCountLimit(), Integer.MAX_VALUE);
        FinalistPodiumType finalistPodiumType_ =
                Objects.requireNonNullElse(foragerConfig.getFinalistPodiumType(), FinalistPodiumType.HIGHEST_SCORE);
        // Breaking ties randomly leads statistically to much better results
        boolean breakTieRandomly_ = Objects.requireNonNullElse(foragerConfig.getBreakTieRandomly(), true);
        return new AcceptedLocalSearchForager<>(finalistPodiumType_.buildFinalistPodium(), pickEarlyType_,
                acceptedCountLimit_, breakTieRandomly_);
    }

    private LocalSearchForager<Solution_> buildCustomForager() {
        int acceptedCountLimit_ = Objects.requireNonNullElse(foragerConfig.getAcceptedCountLimit(), Integer.MAX_VALUE);
        try {
            neighbourhoodEvaluator = (NeighbourhoodEvaluator<Solution_>) Class
                    .forName(foragerConfig.getNeighbourhoodEvaluatorClass()).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException
                | InstantiationException e) {
            throw new IllegalArgumentException(
                    "Could not load NeighbourhoodEvaluator-class although forager type has been set. " + e.getMessage());
        }
        if (foragerConfig.getForagerType() == ForagerType.PP_HILL_CLIMBING) {
            return new PrivacyPreservingHillClimbingForager<>(acceptedCountLimit_, neighbourhoodEvaluator);
        } else if (foragerConfig.getForagerType() == ForagerType.PP_STEP_COUNTING_HILL_CLIMBING) {
            return new PrivacyPreservingStepCountingHillClimbingForager<>(acceptedCountLimit_, 20,
                    StepCountingHillClimbingType.STEP, neighbourhoodEvaluator);
        } else if (foragerConfig.getForagerType() == ForagerType.PP_SIMULATED_ANNEALING) {
            return new PrivacyPreservingSimulatedAnnealingForager<>(acceptedCountLimit_, HardSoftScore.of(0, 500),
                    neighbourhoodEvaluator);
        }
        return null;
    }
}
