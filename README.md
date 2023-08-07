# Build Blocker plugin

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/build-blocker-plugin.svg)](https://plugins.jenkins.io/build-blocker-plugin)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/build-blocker-plugin.svg?label=release)](https://github.com/jenkinsci/build-blocker-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/build-blocker-plugin.svg?color=blue)](https://plugins.jenkins.io/build-blocker-plugin)

This plugin keeps the actual job in the queue if at least one name of
currently running jobs is matching with one of the given regular
expressions.

# General

This plugin is similar to the [locks and latches
plugin](https://wiki.jenkins.io/display/JENKINS/Locks+and+Latches+plugin).
The main difference is, that it uses regular expressions to find
potentially blocking jobs by their names in the list of currently
running builds. It uses the
[QueueTaskDispatcher](https://www.jenkins.io/doc/developer/extensions/jenkins-core/#queuetaskdispatcher)
to check if the actual job may be build. The dispatcher uses the list of
regular expressions configured in the job. If one of the currently
running jobs matches with one of the regular expressions, the job stays
in the queue.

# How to use

After installing the plugin, the job configuration page has a new
property "Block build if certain jobs are running" in the upper section.

![Configuring blocking jobs](docs/images/build-blocker-plugin.png)

Insert one regular expression per line into the textarea. Each
expression is used to detect currently running jobs that match with
their names. The first matching job name will block the build and the
job will stay in the queue until all expression are evaluated without
match.

Other than the locks and latches plugin where both, the job to be build
and the blocking job, need to have the same lock configured, this plugin
allows to just configure to job to be build. No jenkins system
configuration is needed.


The blocking behaviour can be configured to either block builds

 * from running on the same node  
 * from running at all  

Additionally, the blocking behaviour can be configured to consider
planned, but not yet running builds in the decision to block a build.
Either buildable builds can stop another build from running (for instance
 builds that are waiting for an available executor) or  
all planned builds can stop another build from running (blocked builds,
pending builds waiting builds and buildable builds)

## Declarative Pipeline

Inside a declarative pipeline, this can be used as is:

```
pipeline {
    agent any
    
    options {
      buildBlocker (useBuildBlocker: true, blockLevel: 'NODE', scanQueueFor: 'ALL', blockingJobs: 'foo-.*')
    }   
    ....
}    
```

## JobDSL
Usage inside jobdsl scripts is simple as well.
For example in order to create a pipeline job which blocks on global level
when another job is building you can write the following

```
pipelineJob('MyPipeline') {

    // Block build if certain jobs are running.
    blockOn('.*AnotherPipeline.*') {
        // Possible values are 'GLOBAL' and 'NODE' (default).
        blockLevel('GLOBAL')
        // Possible values are 'ALL', 'BUILDABLE' and 'DISABLED' (default).
        scanQueueFor('DISABLED')
    } 
}
```
