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

import hudson.matrix.MatrixConfiguration;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.AbstractProject;
import hudson.model.queue.SubTask;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * This class represents a monitor that checks all running jobs if
 * one of their names matches with one of the given blocking job's
 * regular expressions.
 *
 * The first hit returns the blocking job's name.
 */
public class BlockingJobsMonitor {

    /**
     * the list of regular expressions from the job configuration
     */
    private List<String> blockingJobs;

    /**
     * Constructor using the job configuration entry for blocking jobs
     * @param blockingJobs line feed separated list og blocking jobs
     */
    public BlockingJobsMonitor(String blockingJobs) {
        if(StringUtils.isNotBlank(blockingJobs)) {
            this.blockingJobs = Arrays.asList(blockingJobs.split("\n"));
        }
    }

    /**
     * Returns the name of the first blocking job. If not found, it returns null.
     * @param item The queue item for which we are checking whether it can run or not.
     *        or null if we are not checking a job from the queue (currently only used by testing).
     * @return the name of the first blocking job.
     */
    public SubTask getBlockingJob(Queue.Item item) {
        if(this.blockingJobs == null) {
            return null;
        }

        Computer[] computers = Jenkins.getInstance().getComputers();

        for (Computer computer : computers) {
            List<Executor> executors = computer.getExecutors();

            executors.addAll(computer.getOneOffExecutors());

            for (Executor executor : executors) {
                if(executor.isBusy()) {
                    Queue.Executable currentExecutable = executor.getCurrentExecutable();

                    SubTask subTask = currentExecutable.getParent();
                    Queue.Task task = subTask.getOwnerTask();

                    if (task instanceof MatrixConfiguration) {
                        task = ((MatrixConfiguration) task).getParent();
                    }

                    AbstractProject project = (AbstractProject) task;

                    for (String blockingJob : this.blockingJobs) {
                        if(project.getFullName().matches(blockingJob)) {
                            return subTask;
                        }
                    }
                }
            }
        }

        /**
         * check the list of items that have
         * already been approved for building
         * (but haven't actually started yet)
         */
        List<Queue.BuildableItem> buildableItems
            = Jenkins.getInstance().getQueue().getBuildableItems();

        for (Queue.BuildableItem buildableItem : buildableItems) {
        	if(item != buildableItem) {
	            for (String blockingJob : this.blockingJobs) {
                    AbstractProject project = (AbstractProject) buildableItem.task;
	                if(project.getFullName().matches(blockingJob)) {
	                    return buildableItem.task;
	                }
	            }
        	}
        }

        return null;
    }
}
