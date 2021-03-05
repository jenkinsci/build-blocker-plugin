Changelog
===

### Newer versions
See [Github releases](https://github.com/jenkinsci/build-blocker-plugin/releases)

#### 1.7.3 (December 14, 2015)

-   merged Pull Request \# 8 [FIXED
    JENKINS-29924](https://wiki.jenkins.io/display/JENKINS/FIXED+JENKINS-29924)
    Transform AbstractProject into Job for Workflow compatibility

#### 1.7.2 (November 24, 2015)

-   merged Pull Request \# 7 [FIXED
    JENKINS-29924](https://wiki.jenkins.io/display/JENKINS/FIXED+JENKINS-29924)
    Items with non-AbstractProjects tasks block the build queue

#### 1.7.1 (July 3, 2015)

-   Fixed NPE when using existing build blocker config not having the
    new properties.

#### 1.7 (July 1, 2015)

-   Merge Pull Request \#5 and \#6 (avoid NPE and extended to block on
    node level and to scan the queue for builds in all states)

#### 1.6 (March 13, 2015)

-   Merged Pull Request \#4 (Add form validation
    [JENKINS-27411](https://wiki.jenkins.io/display/JENKINS/JENKINS-27411))

#### 1.5 (March 13, 2015)

-   Merged Pull Requests \#2 (Added support for the Folders plugin) and
    \#3 (Regex validation JENKINS-27402)

#### 1.4.1 (June 28, 2013)

-   added "executors.addAll(computer.getOneOffExecutors());" to get a
    build blocked by all Multi-Configuration-Job executions. Now a
    blocked build starts AFTER the whole blocking matrix build and not
    in the middle of it. ATTENTION: With Jenkins version 1.447 the
    blocked job got stuck in the queue. Now the plugin requires Jenkins
    version 1.466 to run.

#### 1.3 (January 8, 2013)

Merged pull request of bramtassyns
(<https://github.com/jenkinsci/build-blocker-plugin/pull/1>) - Thanks
for the great work!:

-   FIX to work with matrix jobs
-   jobs running and - new - in queue with matching names block the
    current job's start

#### 1.2 (June 25, 2012)

-   Added wiki url to pom.

#### 1.1 (June 24, 2012)

-   Initial commit.
