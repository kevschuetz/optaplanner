/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.constraint.streams.bavet.uni;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.optaplanner.constraint.streams.bavet.BavetConstraintFactory;
import org.optaplanner.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import org.optaplanner.constraint.streams.bavet.common.NodeBuildHelper;
import org.optaplanner.constraint.streams.bavet.common.index.Indexer;
import org.optaplanner.constraint.streams.bavet.common.index.IndexerFactory;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.stream.ConstraintStream;

public final class BavetIfExistsUniConstraintStream<Solution_, A, B> extends BavetAbstractUniConstraintStream<Solution_, A> {

    private final BavetAbstractUniConstraintStream<Solution_, A> parentA;
    private final BavetIfExistsBridgeUniConstraintStream<Solution_, A, B> parentBridgeB;

    private final boolean shouldExist;
    private final Function<A, Object[]> mappingA;
    private final Function<B, Object[]> mappingB;
    private final IndexerFactory indexerFactory;

    public BavetIfExistsUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parentA,
            BavetIfExistsBridgeUniConstraintStream<Solution_, A, B> parentBridgeB,
            boolean shouldExist,
            Function<A, Object[]> mappingA, Function<B, Object[]> mappingB,
            IndexerFactory indexerFactory) {
        super(constraintFactory, parentA.getRetrievalSemantics());
        this.parentA = parentA;
        this.parentBridgeB = parentBridgeB;
        this.shouldExist = shouldExist;
        this.mappingA = mappingA;
        this.mappingB = mappingB;
        this.indexerFactory = indexerFactory;
    }

    @Override
    public boolean guaranteesDistinct() {
        return parentA.guaranteesDistinct();
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        parentA.collectActiveConstraintStreams(constraintStreamSet);
        parentBridgeB.collectActiveConstraintStreams(constraintStreamSet);
        constraintStreamSet.add(this);
    }

    @Override
    public ConstraintStream getTupleSource() {
        return parentA.getTupleSource();
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        int inputStoreIndexA = buildHelper.reserveTupleStoreIndex(parentA.getTupleSource());
        int inputStoreIndexB = buildHelper.reserveTupleStoreIndex(parentBridgeB.getTupleSource());
        Consumer<UniTuple<A>> insert = buildHelper.getAggregatedInsert(childStreamList);
        Consumer<UniTuple<A>> retract = buildHelper.getAggregatedRetract(childStreamList);
        Indexer<UniTuple<A>, IfExistsUniWithUniNode.Counter<A>> indexerA = indexerFactory.buildIndexer(true);
        Indexer<UniTuple<B>, Set<IfExistsUniWithUniNode.Counter<A>>> indexerB = indexerFactory.buildIndexer(false);
        IfExistsUniWithUniNode<A, B> node = new IfExistsUniWithUniNode<>(
                shouldExist, mappingA, mappingB, inputStoreIndexA, inputStoreIndexB,
                insert, retract,
                indexerA, indexerB);
        buildHelper.addNode(node);
        buildHelper.putInsertRetract(this, node::insertA, node::retractA);
        buildHelper.putInsertRetract(parentBridgeB, node::insertB, node::retractB);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    // TODO

    @Override
    public String toString() {
        return "IfExists() with " + childStreamList.size() + " children";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

}
