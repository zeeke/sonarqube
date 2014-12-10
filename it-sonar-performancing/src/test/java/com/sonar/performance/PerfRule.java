package com.sonar.performance;

import com.google.common.base.Joiner;
import org.hamcrest.CustomMatcher;
import org.junit.rules.ErrorCollector;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

public abstract class PerfRule extends ErrorCollector {

  private final int runCount;
  private final List<List<Long>> recordedResults = new ArrayList<List<Long>>();

  private int currentRun;
  private String testName;

  public PerfRule(int runCount) {
    this.runCount = runCount;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    this.testName = description.getMethodName();
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        for (currentRun = 1; currentRun <= runCount; currentRun++) {
          recordedResults.add(new ArrayList<Long>());
          beforeEachRun();
          base.evaluate();
        }
        verify();
      }

    };
  }

  protected abstract void beforeEachRun();

  public void assertDurationAround(long duration, long expectedDuration) {
    currentResults().add(duration);
    if (isLastRun()) {
      long meanDuration = computeAverageDurationOfCurrentStep();
      double variation = 100.0 * (0.0 + meanDuration - expectedDuration) / expectedDuration;
      checkThat(String.format("Expected %d ms in average, got %d ms [%s]", expectedDuration, meanDuration, Joiner.on(",").join(getAllResultsOfCurrentStep())), Math.abs(variation),
        new CustomMatcher<Double>(
          "a value less than "
            + PerfTestCase.ACCEPTED_DURATION_VARIATION_IN_PERCENTS) {
          @Override
          public boolean matches(Object item) {
            return ((item instanceof Double) && ((Double) item).compareTo(PerfTestCase.ACCEPTED_DURATION_VARIATION_IN_PERCENTS) < 0);
          }
        });
    }
  }

  private Long[] getAllResultsOfCurrentStep() {
    Long[] result = new Long[runCount];
    for (int i = 0; i < runCount; i++) {
      result[i] = recordedResults.get(i).get(currentResults().size() - 1);
    }
    return result;
  }

  private long computeAverageDurationOfCurrentStep() {
    Long[] result = getAllResultsOfCurrentStep();
    long meanDuration = 0;
    for (int i = 0; i < runCount; i++) {
      meanDuration += result[i];
    }
    meanDuration /= runCount;
    return meanDuration;
  }

  private List<Long> currentResults() {
    return recordedResults.get(currentRun - 1);
  }

  public void assertDurationLessThan(long duration, final long maxDuration) {
    currentResults().add(duration);
    if (isLastRun()) {
      long meanDuration = computeAverageDurationOfCurrentStep();
      checkThat(String.format("Expected less than %d ms in average, got %d ms [%s]", maxDuration, meanDuration, Joiner.on(",").join(getAllResultsOfCurrentStep())), meanDuration,
        new CustomMatcher<Long>("a value less than "
          + maxDuration) {
          @Override
          public boolean matches(Object item) {
            return ((item instanceof Long) && ((Long) item).compareTo(maxDuration) < 0);
          }
        });
    }
  }

  private boolean isLastRun() {
    return currentRun == runCount;
  }

}
