/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.constraint.streams.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.optaplanner.constraint.streams.bavet.uni.UniTuple;
import org.optaplanner.core.impl.score.stream.JoinerType;

class EqualsAndComparisonIndexerTest extends AbstractIndexerTest {

    @Test
    void getEmpty() {
        Indexer<UniTuple<String>, String> indexer = new EqualsAndComparisonIndexer<>(JoinerType.LESS_THAN_OR_EQUAL);
        assertThat(getTupleMap(indexer, "F", 40)).isEmpty();
    }

    @Test
    void putTwice() {
        Indexer<UniTuple<String>, String> indexer = new EqualsAndComparisonIndexer<>(JoinerType.LESS_THAN_OR_EQUAL);
        UniTuple<String> annTuple = newTuple("Ann-F-40");
        indexer.put(new Object[] { "F", 40 }, annTuple, "Ann value");
        assertThatThrownBy(() -> indexer.put(new Object[] { "F", 40 }, annTuple, "Ann value"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeTwice() {
        Indexer<UniTuple<String>, String> indexer = new EqualsAndComparisonIndexer<>(JoinerType.LESS_THAN_OR_EQUAL);
        UniTuple<String> annTuple = newTuple("Ann-F-40");
        indexer.put(new Object[] { "F", 40 }, annTuple, "Ann value");

        UniTuple<String> ednaTuple = newTuple("Edna-F-40");
        assertThatThrownBy(() -> indexer.remove(new Object[] { "F", 40 }, ednaTuple))
                .isInstanceOf(IllegalStateException.class);
        assertThat(indexer.remove(new Object[] { "F", 40 }, annTuple))
                .isEqualTo("Ann value");
        assertThatThrownBy(() -> indexer.remove(new Object[] { "F", 40 }, annTuple))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void visit() {
        Indexer<UniTuple<String>, String> indexer = new EqualsAndComparisonIndexer<>(JoinerType.LESS_THAN_OR_EQUAL);

        UniTuple<String> annTuple = newTuple("Ann-F-40");
        indexer.put(new Object[] { "F", 40 }, annTuple, "Ann value");
        UniTuple<String> bethTuple = newTuple("Beth-F-30");
        indexer.put(new Object[] { "F", 30 }, bethTuple, "Beth value");
        indexer.put(new Object[] { "M", 40 }, newTuple("Carl-M-40"), "Carl value");
        indexer.put(new Object[] { "M", 30 }, newTuple("Dan-M-30"), "Dan value");
        UniTuple<String> ednaTuple = newTuple("Edna-F-40");
        indexer.put(new Object[] { "F", 40 }, ednaTuple, "Edna value");

        assertThat(getTupleMap(indexer, "F", 40)).containsOnlyKeys(annTuple, bethTuple, ednaTuple);
        assertThat(indexer.countValues(new Object[] { "F", 40 })).isEqualTo(3);

        assertThat(getTupleMap(indexer, "F", 35)).containsOnlyKeys(bethTuple);
        assertThat(indexer.countValues(new Object[] { "F", 35 })).isEqualTo(1);

        assertThat(getTupleMap(indexer, "F", 30)).containsOnlyKeys(bethTuple);
        assertThat(indexer.countValues(new Object[] { "F", 30 })).isEqualTo(1);

        assertThat(getTupleMap(indexer, "F", 20)).isEmpty();
        assertThat(indexer.countValues(new Object[] { "F", 20 })).isZero();
    }

    private static UniTuple<String> newTuple(String factA) {
        return new UniTuple<>(factA, 0);
    }

}
