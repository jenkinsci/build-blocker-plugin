package hudson.plugins.buildblocker;

import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.OneOffExecutor;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;
import hudson.model.queue.SubTask;
import hudson.model.queue.WorkUnit;
import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.quality.Strictness;
import org.powermock.reflect.Whitebox;


import java.util.ArrayList;
import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;


@RunWith(MockitoJUnitRunner.class)
public class BlockingJobsMonitorUnitTest {

    @Mock
    private Node node;
    @Mock
    private Computer computer;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private Queue queue;
    @Mock
    private Executor idleExecutor;
    @Mock
    private Executor executor;
    @Mock
    private OneOffExecutor idleOneOffExecutor;
    @Mock
    private OneOffExecutor oneOffExecutor;
    @Mock
    private Label blockingLabel;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private Label nonBlockingLabel;
    @Mock
    private SubTask subTask;
    @Mock
    private MatrixConfiguration configuration;

    private Project project;
    private Project nonBlockingProject;
    private MatrixProject nonBlockingMatrixProject;
    private MatrixProject matrixProject;
    private BuildableItem buildableItem;
    private BuildableItem buildableItemOnDifferentNode;
    private BuildableItem nonBlockingBuildableItem;
    private Queue.BlockedItem blockedItem;
    private Queue.BlockedItem blockedItemOnDifferentNode;
    private Queue.BlockedItem nonBlockingBlockedItem;
    private Queue.WaitingItem waitingItem;
    private Queue.WaitingItem waitingItemOnDifferentNode;
    private Queue.WaitingItem nonBlockingWaitingItem;
    private WorkUnit workUnit;

    private BlockingJobsMonitor monitor;
    private MockedStatic<Jenkins> mockedJenkins;

    @Before
    public void setup() throws IllegalAccessException {
        monitor = new BlockingJobsMonitor("blockingProject\nblockingMatrixProject");

        trainProjects();
        trainBuildableItems();
        trainBlockedItems();
        trainWaitingItems();
        trainLabels();
        trainJenkins();
        trainNodes();
        trainWorkUnit();
        trainExecutors();
    }
    @After
    public void tearDown() {
        if(mockedJenkins != null){
            mockedJenkins.close();
        }
    }

    private void trainWorkUnit() throws IllegalAccessException {
        workUnit = mock(WorkUnit.class);
        Whitebox.getField(WorkUnit.class, "work").set(workUnit, subTask);
    }

    private void trainLabels() {
        when(blockingLabel.contains(eq(node))).thenReturn(true);
        when(nonBlockingLabel.contains(eq(node))).thenReturn(false);
    }

    private void trainWaitingItems() throws IllegalAccessException {
        waitingItem = mock(Queue.WaitingItem.class);
        nonBlockingWaitingItem = mock(Queue.WaitingItem.class);
        waitingItemOnDifferentNode = mock(Queue.WaitingItem.class);
        Whitebox.getField(Queue.BlockedItem.class, "task").set(waitingItem, project);
        Whitebox.getField(Queue.BlockedItem.class, "task").set(waitingItemOnDifferentNode, project);
        Whitebox.getField(Queue.BlockedItem.class, "task").set(nonBlockingWaitingItem, nonBlockingProject);
        when(waitingItem.getAssignedLabel()).thenReturn(blockingLabel);
        when(waitingItemOnDifferentNode.getAssignedLabel()).thenReturn(nonBlockingLabel);
        when(nonBlockingWaitingItem.getAssignedLabel()).thenReturn(blockingLabel);
    }

    private void trainBlockedItems() throws IllegalAccessException {
        blockedItem = mock(Queue.BlockedItem.class);
        nonBlockingBlockedItem = mock(Queue.BlockedItem.class);
        blockedItemOnDifferentNode = mock(Queue.BlockedItem.class);
        Whitebox.getField(Queue.BlockedItem.class, "task").set(blockedItem, project);
        Whitebox.getField(Queue.BlockedItem.class, "task").set(nonBlockingBlockedItem, nonBlockingProject);
        Whitebox.getField(Queue.BlockedItem.class, "task").set(blockedItemOnDifferentNode, project);
        when(blockedItem.getAssignedLabel()).thenReturn(blockingLabel);
        when(blockedItemOnDifferentNode.getAssignedLabel()).thenReturn(nonBlockingLabel);
        when(nonBlockingBlockedItem.getAssignedLabel()).thenReturn(blockingLabel);
    }

    private void trainBuildableItems() throws IllegalAccessException {
        buildableItem = mock(BuildableItem.class);
        buildableItemOnDifferentNode = mock(BuildableItem.class);
        nonBlockingBuildableItem = mock(BuildableItem.class);
        Whitebox.getField(BuildableItem.class, "task").set(buildableItem, project);
        Whitebox.getField(BuildableItem.class, "task").set(buildableItemOnDifferentNode, project);
        Whitebox.getField(BuildableItem.class, "task").set(nonBlockingBuildableItem, nonBlockingProject);
        when(buildableItem.getAssignedLabel()).thenReturn(blockingLabel);
        when(buildableItemOnDifferentNode.getAssignedLabel()).thenReturn(blockingLabel);
        when(nonBlockingBuildableItem.getAssignedLabel()).thenReturn(blockingLabel);
    }

    private void trainExecutors() {
        when(idleExecutor.isBusy()).thenReturn(false);
        when(executor.isBusy()).thenReturn(true);
        when(executor.getCurrentWorkUnit()).thenReturn(workUnit);

        when(idleOneOffExecutor.isBusy()).thenReturn(false);
        when(oneOffExecutor.isBusy()).thenReturn(true);
        when(oneOffExecutor.getCurrentWorkUnit()).thenReturn(workUnit);

    }

    private void trainNodes() {
        when(node.toComputer()).thenReturn(computer);
    }

    private void trainJenkins() {
        Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.getQueue()).thenReturn(queue);
        when(jenkins.getComputers()).thenReturn(new Computer[]{computer});
        mockedJenkins = mockStatic(Jenkins.class);
        mockedJenkins.when(Jenkins::get).thenReturn(jenkins);
    }

    private void trainProjects() {
        project = mock(Project.class);
        nonBlockingProject = mock(Project.class);
        matrixProject = mock(MatrixProject.class);
        nonBlockingMatrixProject = mock(MatrixProject.class);
        when(project.getFullName()).thenReturn("blockingProject");
        when(nonBlockingProject.getFullName()).thenReturn("harmlessProject");
        when(matrixProject.getFullName()).thenReturn("blockingMatrixProject");
        when(nonBlockingMatrixProject.getFullName()).thenReturn("harmlessMatrixProject");
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesItemDoesNotSelfBlock() {
        when(queue.getBuildableItems(Mockito.any(Computer.class))).thenReturn(singletonList(buildableItem));

        assertThat(monitor.checkNodeForBuildableQueueEntries(buildableItem, node), is(nullValue()));

        //the do not selfblock condition is hit => no interactions with the project
        verifyNoInteractions(project);
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getBuildableItems(Mockito.any(Computer.class))).thenReturn(Collections.<BuildableItem>emptyList());

        assertThat(monitor.checkNodeForBuildableQueueEntries(buildableItem, node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getBuildableItems(eq(computer))).thenReturn(asList(nonBlockingBuildableItem, buildableItem));

        assertThat((Project) monitor.checkNodeForBuildableQueueEntries(mock(BuildableItem.class), node), is(equalTo(project)));
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsNullForDifferentNode() {
        when(queue.getBuildableItems(eq(computer))).thenReturn(asList(nonBlockingBuildableItem, buildableItem));
        Node differentNode = mock(Node.class);
        Computer differentComputer = mock(Computer.class);
        when(differentNode.toComputer()).thenReturn(differentComputer);
        when(queue.getBuildableItems(eq(computer))).thenReturn(asList(nonBlockingBuildableItem, buildableItem));
        when(queue.getBuildableItems(eq(differentComputer))).thenReturn(Collections.<BuildableItem>emptyList());

        assertThat(monitor.checkNodeForBuildableQueueEntries(mock(BuildableItem.class), differentNode), is(nullValue()));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{});

        assertThat(monitor.checkNodeForQueueEntries(buildableItem, node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItem});

        assertThat((Project) monitor.checkNodeForQueueEntries(mock(BuildableItem.class), node), is(equalTo(project)));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsNullForDifferentNode() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItemOnDifferentNode});
        Node differentNode = mock(Node.class);
        Computer differentComputer = mock(Computer.class);
        lenient().when(differentNode.toComputer()).thenReturn(differentComputer);

        assertThat(monitor.checkNodeForQueueEntries(mock(BuildableItem.class), differentNode), is(nullValue()));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsBlockedTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItem});

        assertThat((Project) monitor.checkNodeForQueueEntries(mock(BuildableItem.class), node), is(equalTo(project)));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsNullForDifferentNodeCaseBlocked() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItemOnDifferentNode});
        Node differentNode = mock(Node.class);
        Computer differentComputer = mock(Computer.class);
        lenient().when(differentNode.toComputer()).thenReturn(differentComputer);

        assertThat(monitor.checkNodeForQueueEntries(mock(BuildableItem.class), differentNode), is(nullValue()));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsWaitingTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItem});

        assertThat((Project) monitor.checkNodeForQueueEntries(mock(BuildableItem.class), node), is(equalTo(project)));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsNullForDifferentNodeCaseWaiting() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItemOnDifferentNode});
        Node differentNode = mock(Node.class);
        Computer differentComputer = mock(Computer.class);
        lenient().when(differentNode.toComputer()).thenReturn(differentComputer);

        assertThat(monitor.checkNodeForQueueEntries(mock(BuildableItem.class), differentNode), is(nullValue()));
    }

    @Test
    public void testCheckForBuildableQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getBuildableItems()).thenReturn(Collections.<BuildableItem>emptyList());

        assertThat(monitor.checkForBuildableQueueEntries(buildableItem), is(nullValue()));
    }

    @Test
    public void testCheckForBuildableQueueEntriesItemDoesNotSelfBlock() {
        when(queue.getBuildableItems()).thenReturn(singletonList(buildableItem));

        assertThat(monitor.checkNodeForBuildableQueueEntries(buildableItem, node), is(nullValue()));

        //the do not selfblock condition is hit => no interactions with the project
        verifyNoInteractions(project);
    }

    @Test
    public void testCheckForBuildableQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getBuildableItems()).thenReturn(asList(nonBlockingBuildableItem, buildableItem));

        assertThat((Project) monitor.checkForBuildableQueueEntries(mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForBuildableQueueEntriesReturnsProjectForDifferentNode() {
        when(queue.getBuildableItems()).thenReturn(asList(nonBlockingBuildableItem, buildableItem));

        assertThat((Project) monitor.checkForBuildableQueueEntries(mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{});

        assertThat(monitor.checkForQueueEntries(buildableItem), is(nullValue()));
    }

    @Test
    public void testCheckForQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItem});

        assertThat((Project) monitor.checkForQueueEntries(mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsProjectForDifferentNode() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItemOnDifferentNode});

        assertThat((Project) monitor.checkForQueueEntries(mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsBlockedTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItem});

        assertThat((Project) monitor.checkForQueueEntries(mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsProjectForDifferentNodeCaseBlocked() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItemOnDifferentNode});

        assertThat((Project) monitor.checkForQueueEntries(mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsWaitingTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItem});

        assertThat((Project) monitor.checkForQueueEntries(mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsProjectForDifferentNodeCaseWaiting() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItemOnDifferentNode});

        assertThat((Project) monitor.checkForQueueEntries(mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsNullForNonBlockingItems() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, nonBlockingBuildableItem, nonBlockingBlockedItem});

        assertThat(monitor.checkForQueueEntries(mock(BuildableItem.class)), is(nullValue()));

        //verify that the different project was actually checked (three items are checked for two job names each)
        verify(nonBlockingProject, times(6)).getFullName();
    }


    @Test
    public void testCheckNodeForRunningBuildNeedsNode() {
        assertThat(monitor.checkNodeForRunningBuilds(null), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForNonBusyExecutor() {
        when(computer.getExecutors()).thenReturn(singletonList(idleExecutor));
        when(computer.getOneOffExecutors()).thenReturn(new ArrayList<OneOffExecutor>());

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));

        verify(idleExecutor, only()).isBusy();
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForNonBusyOneOffExecutor() {
        when(computer.getExecutors()).thenReturn(new ArrayList<Executor>());
        when(computer.getOneOffExecutors()).thenReturn(singletonList(idleOneOffExecutor));

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));

        verify(idleOneOffExecutor, only()).isBusy();
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForDifferentRunningProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingProject, times(2)).getFullName();
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsBlockedProjectIfItIsRunning() throws IllegalAccessException {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkNodeForRunningBuilds(node), is(equalTo(project)));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForDifferentRunningProjectOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingProject, times(2)).getFullName();
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsBlockedProjectIfItIsRunningOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkNodeForRunningBuilds(node), is(equalTo(project)));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForDifferentRunningMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(nonBlockingMatrixProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingMatrixProject, times(2)).getFullName();
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsBlockedMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(matrixProject);

        assertThat((MatrixProject) monitor.checkNodeForRunningBuilds(node), is(equalTo(matrixProject)));
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsNullForNonBusyExecutor() {
        when(computer.getExecutors()).thenReturn(singletonList(idleExecutor));
        when(computer.getOneOffExecutors()).thenReturn(new ArrayList<OneOffExecutor>());

        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));

        verify(idleExecutor, only()).isBusy();
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsNullForNonBusyOneOffExecutor() {
        when(computer.getExecutors()).thenReturn(new ArrayList<Executor>());
        when(computer.getOneOffExecutors()).thenReturn(singletonList(idleOneOffExecutor));
        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));

        verify(idleOneOffExecutor, only()).isBusy();
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsNullForDifferentRunningProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingProject, times(2)).getFullName();
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsBlockedProjectIfItIsRunning() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkAllNodesForRunningBuilds(), is(equalTo(project)));
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsNullForDifferentRunningProjectOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingProject, times(2)).getFullName();
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsBlockedProjectIfItIsRunningOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkAllNodesForRunningBuilds(), is(equalTo(project)));
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsNullForDifferentRunningMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(nonBlockingMatrixProject);

        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));

        //verify that the different project was actually checked (two job names are checked)
        verify(nonBlockingMatrixProject, times(2)).getFullName();
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsBlockedMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(matrixProject);

        assertThat((MatrixProject) monitor.checkAllNodesForRunningBuilds(), is(equalTo(matrixProject)));
    }

}
