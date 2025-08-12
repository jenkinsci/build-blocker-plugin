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
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.CauseOfBlockage;
import hudson.slaves.DumbSlave;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests
 */
@WithJenkins
class BuildBlockerQueueTaskDispatcherTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    /**
     * One test for all for faster execution.
     *
     * @throws Exception
     */
    @Test
    void testCanRun() throws Exception {
        // init slave
        LabelAtom slaveLabel = new LabelAtom("slave");
        LabelAtom masterLabel = new LabelAtom("built-in");

        DumbSlave slave = j.createSlave(slaveLabel);
        SlaveComputer c = slave.getComputer();
        c.connect(false).get(); // wait until it's connected

        assertFalse(c.isOffline(), "Slave failed to go online: " + c.getLog());

        BuildBlockerQueueTaskDispatcher dispatcher = new BuildBlockerQueueTaskDispatcher();

        String blockingJobName = "blockingJob";

        CommandInterpreter commandInterpreter = Functions.isWindows() ? new BatchFile("ping -n 10 127.0.0.1 >nul") : new Shell("sleep 10");

        Future<FreeStyleBuild> future1 = createBlockingProject("xxx", commandInterpreter, masterLabel);
        Future<FreeStyleBuild> future2 = createBlockingProject(blockingJobName, commandInterpreter, masterLabel);
        Future<FreeStyleBuild> future3 = createBlockingProject("yyy", commandInterpreter, slaveLabel);
        // add project to slave
        FreeStyleProject project = j.createFreeStyleProject();
        project.setAssignedLabel(slaveLabel);

        Queue.BuildableItem item = new Queue.BuildableItem(new Queue.WaitingItem(Calendar.getInstance(), project, new ArrayList<>()));

        CauseOfBlockage causeOfBlockage = dispatcher.canRun(item);

        assertNull(causeOfBlockage);

        BuildBlockerProperty property = new BuildBlockerPropertyBuilder()
                .setBlockingJobs(".*ocki.*")
                .setUseBuildBlocker()
                .setBlockOnGlobalLevel()
                .createBuildBlockerProperty();

        project.addProperty(property);

        causeOfBlockage = dispatcher.canRun(item);
        assertNotNull(causeOfBlockage);

        assertTrue(causeOfBlockage.getShortDescription().contains(" by " + blockingJobName + "."));

        // wait until jobs are done.
        await().atMost(30, TimeUnit.SECONDS).until(() -> future1.isDone() && future2.isDone() && future3.isDone());
    }

    @Test
    void testMultipleExecutors() throws Exception {
        // Job1 runs for 1 second, no dependencies
        FreeStyleProject theJob1 = j.createFreeStyleProject("MultipleExecutor_Job1");
        CommandInterpreter commandInterpreter = Functions.isWindows() ? new BatchFile("ping -n 10 127.0.0.1 >nul") : new Shell("sleep 10");
        theJob1.getBuildersList().add(commandInterpreter);
        assertTrue(theJob1.getBuilds().isEmpty());

        // Job2 returns immediately but can't run while Job1 is running.
        FreeStyleProject theJob2 = j.createFreeStyleProject("MultipleExecutor_Job2");
        {
            BuildBlockerProperty theProperty = new BuildBlockerPropertyBuilder()
                    .setBlockingJobs("MultipleExecutor_Job1")
                    .setUseBuildBlocker()
                    .setBlockOnGlobalLevel()
                    .setScanBuildableQueueItemStates()
                    .createBuildBlockerProperty();
            theJob2.addProperty(theProperty);
        }
        assertTrue(theJob1.getBuilds().isEmpty());

        // allow executing two simultaneous jobs
        int theOldNumExecutors = j.jenkins.getNumExecutors();
        j.jenkins.setNumExecutors(2);

        Future<FreeStyleBuild> theFuture1 = theJob1.scheduleBuild2(0);
        Future<FreeStyleBuild> theFuture2 = theJob2.scheduleBuild2(0);

        // let the jobs process
        await().atMost(30, TimeUnit.SECONDS).until(() -> theFuture1.isDone() && theFuture2.isDone());

        // check if job2 was not started before job1 was finished
        Run theRun1 = theJob1.getLastBuild();
        Run theRun2 = theJob2.getLastBuild();
        assertTrue(theRun1.getTimeInMillis() + theRun1.getDuration() <= theRun2.getTimeInMillis());

        // restore changed settings
        j.jenkins.setNumExecutors(theOldNumExecutors);
        theJob2.delete();
        theJob1.delete();
    }

    @Test
    void testSelfExcludingJobs() throws Exception {
        BuildBlockerProperty theProperty = new BuildBlockerPropertyBuilder()
                .setBlockingJobs("SelfExcluding_.*")
                .setUseBuildBlocker()
                .setBlockOnGlobalLevel()
                .setScanBuildableQueueItemStates()
                .createBuildBlockerProperty();

        FreeStyleProject theJob1 = j.createFreeStyleProject("SelfExcluding_Job1");
        theJob1.addProperty(theProperty);
        assertTrue(theJob1.getBuilds().isEmpty());

        FreeStyleProject theJob2 = j.createFreeStyleProject("SelfExcluding_Job2");
        theJob2.addProperty(theProperty);
        assertTrue(theJob2.getBuilds().isEmpty());

        // allow executing two simultaneous jobs
        int theOldNumExecutors = j.jenkins.getNumExecutors();
        j.jenkins.setNumExecutors(2);

        Future<FreeStyleBuild> theFuture1 = theJob1.scheduleBuild2(0);
        Future<FreeStyleBuild> theFuture2 = theJob2.scheduleBuild2(0);

        long theStartTime = System.currentTimeMillis();
        long theEndTime = theStartTime;
        while ((!theFuture1.isDone() || !theFuture2.isDone())
                && theEndTime < theStartTime + 5000) {
            int countBusy = 0;
            for (Computer computor : j.jenkins.getComputers()) {
                for (Executor executor : computor.getExecutors()) {
                    if (executor.isBusy()) {
                        countBusy++;
                        //the two jobs dont run in parallel
                        assertThat(countBusy, is(lessThanOrEqualTo(1)));
                    }
                }
            }
            theEndTime = System.currentTimeMillis();
        }

        // if more than five seconds have passed, we assume its a deadlock.
        assertTrue(theEndTime < theStartTime + 5000);

        // restore changed settings
        j.jenkins.setNumExecutors(theOldNumExecutors);
        theJob2.delete();
        theJob1.delete();
    }

    /**
     * Returns the future object for a newly created project.
     *
     * @param blockingJobName    the name for the project
     * @param commandInterpreter the command interpreter to add
     * @param label              the label to bind to master or slave
     * @return the future object for a newly created project
     * @throws IOException
     */
    private Future<FreeStyleBuild> createBlockingProject(String blockingJobName, CommandInterpreter commandInterpreter, Label label) throws
            IOException {
        FreeStyleProject blockingProject = j.createFreeStyleProject(blockingJobName);
        blockingProject.setAssignedLabel(label);

        blockingProject.getBuildersList().add(commandInterpreter);
        Future<FreeStyleBuild> future = blockingProject.scheduleBuild2(0);

        await().atMost(30, TimeUnit.SECONDS).until(() -> blockingProject.isBuilding());

        return future;
    }
}
