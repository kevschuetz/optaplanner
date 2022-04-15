package org.optaplanner.core.config.localsearch.decider.forager;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum ForagerType {
    SLM_HILL_CLIMBING,
    SLM_STEP_COUNTING_HILL_CLIMBING,
    SLM_SIMULATED_ANNEALING
}
