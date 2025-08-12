/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Frederik Fromm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.buildblocker;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.DumbSlave;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests
 */
@WithJenkins
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlockingJobsMonitorTest {

    private static final String BLOCKING_JOB_NAME = "blockingJob";

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("freeStyle", freeStyleSetUp()),
                Arguments.of("workflow", workflowSetUp()));
    }

    private Supplier<QueueTaskFuture<FreeStyleBuild>> freeStyleSetUp() {
        return () -> {
            try {
                // clear queue from preceding tests
                Jenkins.get().getQueue().clear();

                // init slave
                DumbSlave slave = j.createSlave();
                slave.setLabelString("label");

                SlaveComputer c = slave.getComputer();
                c.connect(false).get(); // wait until it's connected

                FreeStyleProject blockingProject = j.createFreeStyleProject(BLOCKING_JOB_NAME);
                blockingProject.setAssignedLabel(new LabelAtom("label"));

                CommandInterpreter commandInterpreter = Functions.isWindows() ? new BatchFile("ping -n 10 127.0.0.1 >nul") : new Shell("sleep 10");
                blockingProject.getBuildersList().add(commandInterpreter);

                QueueTaskFuture<FreeStyleBuild> future = blockingProject.scheduleBuild2(0);

                // wait until blocking job started
                await().atMost(30, TimeUnit.SECONDS).until(() -> slave.getComputer().getExecutors().get(0).isBusy());

                return future;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        };
    }

    private Supplier<QueueTaskFuture<WorkflowRun>> workflowSetUp() {
        return () -> {
            try {
                // clear queue from preceding tests
                Jenkins.get().getQueue().clear();

                // init slave
                DumbSlave slave = j.createSlave();
                slave.setLabelString("label");

                SlaveComputer c = slave.getComputer();
                c.connect(false).get(); // wait until it's connected

                WorkflowJob workflowBlockingProject = j.jenkins.createProject(WorkflowJob.class, BLOCKING_JOB_NAME);
                workflowBlockingProject.setDefinition(new CpsFlowDefinition("node('label') { sleep 10 }", true));

                QueueTaskFuture<WorkflowRun> future = workflowBlockingProject.scheduleBuild2(1);

                // wait until blocking job started
                await().atMost(30, TimeUnit.SECONDS).until(() -> slave.getComputer().getExecutors().get(0).isBusy());

                return future;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testNullMonitorDoesNotBlock(String type, Supplier<QueueTaskFuture<?>> setUp) {
        QueueTaskFuture<?> future = setUp.get();
        BlockingJobsMonitor blockingJobsMonitorUsingNull = new BlockingJobsMonitor(null);
        assertNull(blockingJobsMonitorUsingNull.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingNull.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testNonMatchingMonitorDoesNotBlock(String type, Supplier<QueueTaskFuture<?>> setUp) {
        QueueTaskFuture<?> future = setUp.get();
        BlockingJobsMonitor blockingJobsMonitorNotMatching = new BlockingJobsMonitor("xxx");
        assertNull(blockingJobsMonitorNotMatching.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorNotMatching.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testMatchingMonitorReturnsBlockingJobsDisplayName(String type, Supplier<QueueTaskFuture<?>> setUp) {
        QueueTaskFuture<?> future = setUp.get();
        BlockingJobsMonitor blockingJobsMonitorUsingFullName = new BlockingJobsMonitor(BLOCKING_JOB_NAME);
        assertEquals(BLOCKING_JOB_NAME, blockingJobsMonitorUsingFullName.checkAllNodesForRunningBuilds().getDisplayName());
        assertNull(blockingJobsMonitorUsingFullName.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testMonitorBlocksBasedOnRegEx(String type, Supplier<QueueTaskFuture<?>> setUp) {
        QueueTaskFuture<?> future = setUp.get();
        BlockingJobsMonitor blockingJobsMonitorUsingRegex = new BlockingJobsMonitor("block.*");
        assertEquals(BLOCKING_JOB_NAME, blockingJobsMonitorUsingRegex.checkAllNodesForRunningBuilds().getDisplayName());
        assertNull(blockingJobsMonitorUsingRegex.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testMonitorBlocksIfConfiguredWithSeveralProjectNames(String type, Supplier<QueueTaskFuture<?>> setUp) {
        QueueTaskFuture<?> future = setUp.get();
        BlockingJobsMonitor blockingJobsMonitorUsingMoreLines = new BlockingJobsMonitor("xxx\nblock.*\nyyy");
        assertEquals(BLOCKING_JOB_NAME, blockingJobsMonitorUsingMoreLines.checkAllNodesForRunningBuilds().getDisplayName());
        assertNull(blockingJobsMonitorUsingMoreLines.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    void testMonitorDoesNotBlockIfRegexDoesNotMatch(String type, Supplier<QueueTaskFuture<?>> setUp) {
        QueueTaskFuture<?> future = setUp.get();
        BlockingJobsMonitor blockingJobsMonitorUsingWrongRegex = new BlockingJobsMonitor("*BW2S.*QRT.");
        assertNull(blockingJobsMonitorUsingWrongRegex.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingWrongRegex.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        await().atMost(30, TimeUnit.SECONDS).until(future::isDone);
    }
}
