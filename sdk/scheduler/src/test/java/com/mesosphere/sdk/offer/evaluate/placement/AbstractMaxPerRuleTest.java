package com.mesosphere.sdk.offer.evaluate.placement;

import com.mesosphere.sdk.offer.taskdata.TaskLabelWriter;
import com.mesosphere.sdk.specification.PodInstance;
import com.mesosphere.sdk.testutils.OfferTestUtils;
import com.mesosphere.sdk.testutils.PodTestUtils;
import com.mesosphere.sdk.testutils.TestConstants;
import org.apache.mesos.Protos;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This class tests the {@link AbstractMaxPerRule} class.
 */
public class AbstractMaxPerRuleTest {
    private Protos.TaskInfo taskInfo;
    private Protos.Offer offer;
    private PodInstance podInstance;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        podInstance = PodTestUtils.getPodInstance(0);
        taskInfo = TestConstants.TASK_INFO.toBuilder().setLabels(
                new TaskLabelWriter(TestConstants.TASK_INFO)
                        .setType("different-type")
                        .setIndex(100)
                        .toProto())
                .build();

        offer = OfferTestUtils.getEmptyOfferBuilder().build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void limitZero() {
        new MaxPerHostnameRule(0);
    }

    /**
     * Neither the offer nor task contains any keys which should limit placement, and the task list is empty, so the
     * offer should be accepted.
     */
    @Test
    public void acceptEmptyAll() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                1,
                Collections.emptyList(),
                Collections.emptyList(),
                AnyMatcher.create());
        assertTrue(rule.filter(offer, podInstance, Collections.emptyList()).isPassing());
    }

    /**
     * Neither the offer nor task contains any keys which should limit placement, and the task list is non-empty, so the
     * offer should be accepted.
     */
    @Test
    public void acceptEmptyKeysNonEmptyTasks() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                1,
                Collections.emptyList(),
                Collections.emptyList(),
                AnyMatcher.create());
        assertTrue(rule.filter(offer, podInstance, Arrays.asList(taskInfo)).isPassing());
    }

    /**
     * A task has been launched using "key0".  The offer contains no keys, so the offer should be accepted.
     */
    @Test
    public void acceptOfferWithoutKeys() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                1,
                Arrays.asList("key0"),
                Collections.emptyList(),
                AnyMatcher.create());

        assertTrue(rule.filter(offer, podInstance, Arrays.asList(taskInfo)).isPassing());
    }

    /**
     * A task has been launched using "key0".  The offer contains a different key, so the offer should be accepted.
     */
    @Test
    public void acceptOfferWithDifferentKey() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                1,
                Arrays.asList("key0"),
                Arrays.asList("key1"),
                AnyMatcher.create());

        assertTrue(rule.filter(offer, podInstance, Arrays.asList(taskInfo)).isPassing());
    }

    /**
     * A task has been launched using "key0".  The offer contains this key, so the offer should be rejected.
     */
    @Test
    public void rejectSingleTask() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                1,
                Arrays.asList("key0"),
                Arrays.asList("key0"),
                AnyMatcher.create());

        assertFalse(rule.filter(offer, podInstance, Arrays.asList(taskInfo)).isPassing());
    }

    /**
     * A task has been launched using "key0", but that is the task we're currently evaluating, so the offer should be
     * accepted.
     */
    @Test
    public void ignoreSelf() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                1,
                Arrays.asList("key0"),
                Arrays.asList("key0"),
                AnyMatcher.create());

        taskInfo = taskInfo.toBuilder().setLabels(
                new TaskLabelWriter(taskInfo)
                        .setType(podInstance.getPod().getType())
                        .setIndex(podInstance.getIndex())
                        .toProto())
                .build();

        assertTrue(rule.filter(offer, podInstance, Arrays.asList(taskInfo)).isPassing());
    }

    /**
     * Two tasks have been launched using "key0", but one of those is the task we're currently evaluating, so the offer
     * should be accepted.
     */
    @Test
    public void ignoreSelfLargerLimit() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                2,
                Arrays.asList("key0", "key0"),
                Arrays.asList("key0"),
                AnyMatcher.create());

        taskInfo = taskInfo.toBuilder().setLabels(
                new TaskLabelWriter(taskInfo)
                        .setType(podInstance.getPod().getType())
                        .setIndex(podInstance.getIndex())
                        .toProto())
                .build();

        assertTrue(rule.filter(offer, podInstance, Arrays.asList(taskInfo)).isPassing());
    }

    /**
     * Tasks have been launched with "key0" and "key1", one of the keys is present in the offer, so the offer should be
     * rejected.
     */
    @Test
    public void rejectOneOfMultipleOverLimit() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                1,
                Arrays.asList("key0", "key1"),
                Arrays.asList("key1"),
                AnyMatcher.create());

        assertFalse(rule.filter(offer, podInstance, Arrays.asList(taskInfo)).isPassing());
    }

    /**
     * One Task has been launched with "key0" and two with "key1", "key1" is present in the offer, so the offer should
     * be rejected.
     */
    @Test
    public void rejectOneOfMultipleOverLargerLimit() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                2,
                Arrays.asList("key0", "key1", "key1"),
                Arrays.asList("key1"),
                AnyMatcher.create());

        assertFalse(rule.filter(offer, podInstance, Arrays.asList(taskInfo)).isPassing());
    }

    /**
     * Tasks have been launched with "key0" and "key1", one of the keys is present in the offer, so the offer should be
     * rejected.
     */
    @Test
    public void rejectMultipleOverLimit() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                1,
                Arrays.asList("key0", "key1"),
                Arrays.asList("key1", "key0"),
                AnyMatcher.create());

        assertFalse(rule.filter(offer, podInstance, Arrays.asList(taskInfo)).isPassing());
    }

    /**
     * Tasks have been launched with "key0" and "key1", one of the keys is present in the offer, so the offer should be
     * rejected.
     */
    @Test
    public void rejectMultipleOverLargerLimit() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                2,
                Arrays.asList("key0", "key0", "key1", "key1"),
                Arrays.asList("key1", "key0"),
                AnyMatcher.create());

        assertFalse(rule.filter(offer, podInstance, Arrays.asList(taskInfo)).isPassing());
    }

    /**
     * Tasks have been launched with "key0" and "key1", different keys are present in the offer, so the offer should be
     * accepted.
     */
    @Test
    public void acceptOfferWithDisjointKeys() {
        AbstractMaxPerRule rule = new TestMaxPerRule(
                1,
                Arrays.asList("key0", "key1"),
                Arrays.asList("key2", "key3"),
                AnyMatcher.create());

        assertTrue(rule.filter(offer, podInstance, Arrays.asList(taskInfo)).isPassing());
    }

    private static class TestMaxPerRule extends AbstractMaxPerRule {
        private final StringMatcher stringMatcher;
        private final Collection<String> taskKeys;
        private final Collection<String> offerKeys;

        public TestMaxPerRule(
                Integer max,
                Collection<String> taskKeys,
                Collection<String> offerKeys,
                StringMatcher stringMatcher) {
            super(max, stringMatcher);
            this.taskKeys = taskKeys;
            this.offerKeys = offerKeys;
            this.stringMatcher = stringMatcher;
        }

        @Override
        public Collection<String> getKeys(Protos.TaskInfo taskInfo) {
            return taskKeys;
        }

        @Override
        public Collection<String> getKeys(Protos.Offer offer) {
            return offerKeys;
        }

        @Override
        public StringMatcher getTaskFilter() {
            return stringMatcher;
        }

        @Override
        public PlacementOutcome filter(Protos.Offer offer, PodInstance podInstance, Collection<Protos.TaskInfo> tasks) {
            if (isAcceptable(offer, podInstance, tasks)) {
                return PlacementOutcome.pass(this, "Offer is acceptable").build();
            } else {
                return PlacementOutcome.fail(this, "Offer is NOT acceptable").build();
            }
        }

        @Override
        public Collection<PlacementField> getPlacementFields() {
            return Collections.emptyList();
        }
    }
}