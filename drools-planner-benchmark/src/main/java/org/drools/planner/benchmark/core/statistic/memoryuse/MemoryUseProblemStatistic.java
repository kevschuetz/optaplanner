/*
 * Copyright 2011 JBoss Inc
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

package org.drools.planner.benchmark.core.statistic.memoryuse;

import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Locale;
import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.drools.planner.benchmark.core.DefaultPlannerBenchmark;
import org.drools.planner.benchmark.core.ProblemBenchmark;
import org.drools.planner.benchmark.core.SingleBenchmark;
import org.drools.planner.benchmark.core.statistic.AbstractProblemStatistic;
import org.drools.planner.benchmark.core.statistic.MillisecondsSpendNumberFormat;
import org.drools.planner.benchmark.core.statistic.ProblemStatisticType;
import org.drools.planner.benchmark.core.statistic.SingleStatistic;
import org.drools.planner.core.Solver;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class MemoryUseProblemStatistic extends AbstractProblemStatistic {

    protected File graphStatisticFile = null;

    public MemoryUseProblemStatistic(ProblemBenchmark problemBenchmark) {
        super(problemBenchmark, ProblemStatisticType.MEMORY_USE);
    }

    public SingleStatistic createSingleStatistic() {
        return new MemoryUseSingleStatistic();
    }

    /**
     * @return never null, relative to the {@link DefaultPlannerBenchmark#benchmarkReportDirectory}
     * (not {@link ProblemBenchmark#problemReportDirectory})
     */
    public String getGraphFilePath() {
        return toFilePath(graphStatisticFile);
    }

    // ************************************************************************
    // Write methods
    // ************************************************************************

    protected void writeCsvStatistic() {
        ProblemStatisticCsv csv = new ProblemStatisticCsv();
        for (SingleBenchmark singleBenchmark : problemBenchmark.getSingleBenchmarkList()) {
            if (singleBenchmark.isSuccess()) {
                MemoryUseSingleStatistic singleStatistic = (MemoryUseSingleStatistic)
                        singleBenchmark.getSingleStatistic(problemStatisticType);
                for (MemoryUseSingleStatisticPoint point : singleStatistic.getPointList()) {
                    long timeMillisSpend = point.getTimeMillisSpend();
                    MemoryUseMeasurement memoryUseMeasurement = point.getMemoryUseMeasurement();
                    csv.addPoint(singleBenchmark, timeMillisSpend,
                            Long.toString(memoryUseMeasurement.getUsedMemory())
                            + "/" + Long.toString(memoryUseMeasurement.getMaxMemory()));
                }
            } else {
                csv.addPoint(singleBenchmark, 0L, "Failed");
            }
        }
        csvStatisticFile = new File(problemBenchmark.getProblemReportDirectory(),
                problemBenchmark.getName() + "MemoryUseStatistic.csv");
        csv.writeCsvStatisticFile();
    }

    protected void writeGraphStatistic() {
        Locale locale = problemBenchmark.getPlannerBenchmark().getPlannerStatistic().getLocale();
        NumberAxis xAxis = new NumberAxis("Time spend");
        xAxis.setNumberFormatOverride(new MillisecondsSpendNumberFormat(locale));
        NumberAxis yAxis = new NumberAxis("Memory");
        yAxis.setNumberFormatOverride(NumberFormat.getInstance(locale));
        XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
        plot.setOrientation(PlotOrientation.VERTICAL);
        int seriesIndex = 0;
        for (SingleBenchmark singleBenchmark : problemBenchmark.getSingleBenchmarkList()) {
            XYSeries usedSeries = new XYSeries(
                    singleBenchmark.getSolverBenchmark().getNameWithFavoriteSuffix() + " used");
            // TODO enable max memory, but in the same color as used memory, but with a dotted line instead
//            XYSeries maxSeries = new XYSeries(
//                    singleBenchmark.getSolverBenchmark().getNameWithFavoriteSuffix() + " max");
            XYItemRenderer renderer = new XYLineAndShapeRenderer();
            if (singleBenchmark.isSuccess()) {
                MemoryUseSingleStatistic singleStatistic = (MemoryUseSingleStatistic)
                        singleBenchmark.getSingleStatistic(problemStatisticType);
                for (MemoryUseSingleStatisticPoint point : singleStatistic.getPointList()) {
                    long timeMillisSpend = point.getTimeMillisSpend();
                    MemoryUseMeasurement memoryUseMeasurement = point.getMemoryUseMeasurement();
                    usedSeries.add(timeMillisSpend, memoryUseMeasurement.getUsedMemory());
//                    maxSeries.add(timeMillisSpend, memoryUseMeasurement.getMaxMemory());
                }
            }
            XYSeriesCollection seriesCollection = new XYSeriesCollection();
            seriesCollection.addSeries(usedSeries);
//            seriesCollection.addSeries(maxSeries);
            plot.setDataset(seriesIndex, seriesCollection);
            if (singleBenchmark.getSolverBenchmark().isFavorite()) {
                // Make the favorite more obvious
                renderer.setSeriesStroke(0, new BasicStroke(2.0f));
//                renderer.setSeriesStroke(1, new BasicStroke(2.0f));
            }
            plot.setRenderer(seriesIndex, renderer);
            seriesIndex++;
        }
        JFreeChart chart = new JFreeChart(problemBenchmark.getName() + " memory use statistic",
                JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        graphStatisticFile = writeChartToImageFile(chart, problemBenchmark.getName() + "MemoryUseStatistic");
    }

    @Override
    protected void fillWarningList() {
        if (problemBenchmark.getPlannerBenchmark().hasMultipleParallelBenchmarks()) {
            warningList.add("This memory use statistic shows the sum of the memory of all benchmarks "
                    + "that ran in parallel, due to parallelBenchmarkCount ("
                    + problemBenchmark.getPlannerBenchmark().getParallelBenchmarkCount() + ").");
        }
    }

}
