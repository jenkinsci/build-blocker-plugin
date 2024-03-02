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

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.slaves.SlaveComputer;
import hudson.tasks.Shell;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Unit tests
 */
public class BlockingJobsMonitorTest {

    @Rule
    public  JenkinsRule j = new JenkinsRule();

    private String blockingJobName;
    private Future<FreeStyleBuild> future;
    private Future<WorkflowRun> futureWorkflow;
    private static DumbSlave slave;
    private  Queue queue;

    @Before
    public void setup() throws Exception{
        slave = j.createSlave();
        slave.setLabelString("label");
        SlaveComputer c = slave.getComputer();
        c.connect(false).get();
        queue = j.jenkins.getQueue();
    }

    @After
    public void clearQueue() throws Exception {
        j.jenkins.getQueue().clear();
    }


    protected void FreeStyleSetUp() throws Exception {
        blockingJobName = "blockingJob";

        FreeStyleProject blockingProject = j.createFreeStyleProject(blockingJobName);
        blockingProject.setAssignedLabel(new LabelAtom("label"));

        Shell shell = new Shell("sleep 1");
        blockingProject.getBuildersList().add(shell);

        future = blockingProject.scheduleBuild2(1);
        // wait until blocking job started
       while (!slave.getComputer().getExecutors().get(0).isBusy()) {
            TimeUnit.SECONDS.sleep(1);
       }
    }

    protected void WorkFlowSetUp() throws Exception {
        blockingJobName = "blockingJob";
        WorkflowJob workflowBlockingProject = j.createProject(WorkflowJob.class, blockingJobName);
        workflowBlockingProject.setDefinition(new CpsFlowDefinition("node('label') { sleep 10}", true));

        futureWorkflow = workflowBlockingProject.scheduleBuild2(0);

        // wait until blocking job started
       while (!slave.getComputer().getExecutors().get(0).isBusy()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void testNullMonitorDoesNotBlockWithFreeSytle() throws Exception {
        FreeStyleSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingNull = new BlockingJobsMonitor(null);
        assertNull(blockingJobsMonitorUsingNull.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingNull.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        queue.maintain();
    }

    @Test
    public void testNullMonitorDoesNotBlockWithWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingNull = new BlockingJobsMonitor(null);
        assertNull(blockingJobsMonitorUsingNull.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingNull.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!futureWorkflow.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        queue.maintain();
    }

    @Test
    public void testNonMatchingMonitorDoesNotBlockWithFreeSytle() throws Exception {
        FreeStyleSetUp();
        BlockingJobsMonitor blockingJobsMonitorNotMatching = new BlockingJobsMonitor("xxx");
        assertNull(blockingJobsMonitorNotMatching.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorNotMatching.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        queue.maintain();
    }

    @Test
    public void testNonMatchingMonitorDoesNotBlockWithWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorNotMatching = new BlockingJobsMonitor("xxx");
        assertNull(blockingJobsMonitorNotMatching.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorNotMatching.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!futureWorkflow.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        queue.maintain();
    }

    @Test
    public void testMatchingMonitorReturnsBlockingJobsDisplayNameWithFreeSytle() throws Exception {
        FreeStyleSetUp();

        BlockingJobsMonitor blockingJobsMonitorUsingFullName = new BlockingJobsMonitor(blockingJobName);

        assertEquals(blockingJobName, blockingJobsMonitorUsingFullName.checkAllNodesForRunningBuilds().getDisplayName());
        assertNull(blockingJobsMonitorUsingFullName.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        queue.maintain();
    }

    @Test
    public void testMatchingMonitorReturnsBlockingJobsDisplayNameWithWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingFullName = new BlockingJobsMonitor(blockingJobName);

        assertEquals(blockingJobName, blockingJobsMonitorUsingFullName.checkAllNodesForRunningBuilds().getDisplayName
                ());
        assertNull(blockingJobsMonitorUsingFullName.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!futureWorkflow.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        queue.maintain();
    }

    @Test
    public void testMonitorBlocksBasedOnRegExWithFreeSytle() throws Exception {
        FreeStyleSetUp();

        BlockingJobsMonitor blockingJobsMonitorUsingRegex = new BlockingJobsMonitor("block.*");
        assertEquals(blockingJobName, blockingJobsMonitorUsingRegex.checkAllNodesForRunningBuilds().getDisplayName());
        assertNull(blockingJobsMonitorUsingRegex.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        queue.maintain();
    }

    @Test
    public void testMonitorBlocksBasedOnRegExWitWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingRegex = new BlockingJobsMonitor("block.*");
        assertEquals(blockingJobName, blockingJobsMonitorUsingRegex.checkAllNodesForRunningBuilds().getDisplayName());
        assertNull(blockingJobsMonitorUsingRegex.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!futureWorkflow.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        queue.maintain();
    }

    @Test
    public void testMonitorBlocksIfConfiguredWithSeveralProjectnamesWithFreeSytle() throws Exception {
        FreeStyleSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingMoreLines = new BlockingJobsMonitor("xxx\nblock.*\nyyy");
        assertEquals(blockingJobName, blockingJobsMonitorUsingMoreLines.checkAllNodesForRunningBuilds()
                .getDisplayName());
        assertNull(blockingJobsMonitorUsingMoreLines.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        queue.maintain();
    }

   @Test
    public void testMonitorBlocksIfConfiguredWithSeveralProjectnamesWithWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingMoreLines = new BlockingJobsMonitor("xxx\nblock.*\nyyy");
        assertEquals(blockingJobName, blockingJobsMonitorUsingMoreLines.checkAllNodesForRunningBuilds()
                .getDisplayName());
        assertNull(blockingJobsMonitorUsingMoreLines.checkForBuildableQueueEntries(null));
       // wait until blocking job stopped
       while (!futureWorkflow.isDone()) {
           TimeUnit.SECONDS.sleep(1);
       }
       queue.maintain();
    }

    @Test
    public void testMonitorDoesNotBlockIfRegexDoesNotMatchWithFreeSytle() throws Exception {
        FreeStyleSetUp();

        BlockingJobsMonitor blockingJobsMonitorUsingWrongRegex = new BlockingJobsMonitor("*BW2S.*QRT.");
        assertNull(blockingJobsMonitorUsingWrongRegex.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingWrongRegex.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        queue.maintain();
    }

    @Test
    public void testMonitorDoesNotBlockIfRegexDoesNotMatchWithWorkflow() throws Exception {
        WorkFlowSetUp();
        BlockingJobsMonitor blockingJobsMonitorUsingWrongRegex = new BlockingJobsMonitor("*BW2S.*QRT.");
        assertNull(blockingJobsMonitorUsingWrongRegex.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingWrongRegex.checkForBuildableQueueEntries(null));
        // wait until blocking job stopped
        while (!futureWorkflow.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        queue.maintain();
    }
}
