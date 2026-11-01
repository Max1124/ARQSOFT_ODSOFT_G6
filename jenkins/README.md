# Pipelines

| # | File | Where | Fires on | Does |
|---|---|---|---|---|
| 1 | `Jenkinsfile.local` | your own Jenkins | every local commit (git hook) | build, then the three student areas; no deploy; mutation testing off by default |
| 2 | `Jenkinsfile.staging` | Azure Jenkins, multibranch job | push to `feature/*` | build, three areas, archive the jar, deploy to **dev** (8081) |
| 3 | `Jenkinsfile.release` | Azure Jenkins, job `lms-pipeline` | push or merged PR on `main` | build, three areas, archive the jar, deploy to **uat** (8082) + Newman, then **prod** (8083) |

## One stage per student

Each pipeline has three student stages alongside the shared test stages and the final global coverage stage, so the owner of a failure is clear:

| Stage | Owner | Maven profile | Inside |
|---|---|---|---|
| Lending policies (Guilherme) | Guilherme | `-Plending` | unit tests, integration tests (Spring, API, database; tagged `integration`), JaCoCo, SpotBugs, PIT |
| Bibliographic sources (Max1124) | Max1124 | `-Pbibliographic` | tests + JaCoCo, SpotBugs, PIT |
| Reader notifications (AlexandreMouraCosta) | AlexandreMouraCosta | `-Pnotification` | tests + JaCoCo, integration tests, PIT |

A failing area does not stop the other two from running (each leaf is wrapped in `catchError`); the build ends red, and
the archive and deploy stages are skipped. To change what an area runs, edit its Maven profile in `pom.xml` and its
functions in `areas.groovy`; the three Jenkinsfiles do not change.

Reports: `JaCoCo (global)`, `JaCoCo (lending|bibliographic|notification)` and `PIT (lending|bibliographic|notification)` on the build page.

## Prod gate

Pipeline 3 waits for a manual approval before prod. Tick the build parameter `AUTO_PROD` (or change its default in
`Jenkinsfile.release`) to deploy straight after uat. A Newman run that is only UNSTABLE does not block prod; a FAILED
area or a failed uat deploy does.

## Setting up the jobs

### 1. Local Jenkins (pipeline 1)

Committing does nothing by itself. A commit starts pipeline 1 only when all three of these exist: a Jenkins running on
your machine, a job named `lms-local` in it, and the git hook. Do the steps once, in this order. No Docker is needed:
Jenkins runs from its `jenkins.war` file.

If you only want the same checks without Jenkins, run from the repository (with JDK 17 as `JAVA_HOME`):
`./mvnw -B -Plending verify`, `./mvnw -B -Pbibliographic test`, `./mvnw -B -Pnotification test`.

**Which system?** The Jenkinsfiles use `sh` and `./mvnw`, so Jenkins must run on Linux or macOS.
On Windows, run Jenkins **inside WSL** (Ubuntu): follow the WSL column. Do not run Jenkins as a native Windows program.

#### Step 1. Install the two JDKs

Jenkins itself needs **Java 21 or newer**. The project build needs **JDK 17** (Lombok does not compile on newer JDKs).
The job runs the build with the 17 you give it, so the two do not conflict.

| System | Commands |
|---|---|
| WSL / Ubuntu | `sudo apt update && sudo apt install -y openjdk-17-jdk openjdk-21-jdk` |
| macOS | `brew install openjdk@17 openjdk@21` |
| Windows | Install WSL first (PowerShell as administrator: `wsl --install -d Ubuntu`, restart, open Ubuntu), then use the WSL command inside Ubuntu |

If you already have Java 21 or 25 (check with `java -version`), you only need the 17 one.

#### Step 2. Find the paths of the two JDKs

| System | JDK 17 (for the build) | JDK 21 or newer (for Jenkins) |
|---|---|---|
| WSL / Ubuntu | `/usr/lib/jvm/java-17-openjdk-amd64` | `/usr/lib/jvm/java-21-openjdk-amd64` (or run `ls /usr/lib/jvm` and use the folder of the 25 you have) |
| macOS | `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` | `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` |

Note both. The first goes into the job (Step 6); the second is `JENKINS_JAVA_HOME` in the next step.

#### Step 3. Start Jenkins

From the repository root, in a terminal you leave open (WSL example; use your own path from Step 2):

```
export JENKINS_JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
bash jenkins/local/start-jenkins.sh
```

If your normal `java -version` already says 21 or higher, you can skip the `export`.

The first start downloads `jenkins.war` (about 90 MB) into `~/jenkins-local` and takes about a minute. When the log says
"Jenkins is fully up and running", open <http://localhost:9090>. On WSL the page also opens from the Windows browser.
Stop it with Ctrl+C. Next time repeat this step; your jobs and settings are kept in `~/jenkins-local`.

#### Step 4. First-time Jenkins setup (once)

1. The page asks for an administrator password. It is printed in the terminal, and also in
   `~/jenkins-local/secrets/initialAdminPassword`. Paste it.
2. Choose "Install suggested plugins" and wait.
3. Create your admin user (any name and password), keep the suggested URL, finish.
4. Install two more plugins: Manage Jenkins, Plugins, Available plugins, search **HTML Publisher** and
   **Lockable Resources**, tick them, Install, and tick "Restart Jenkins when installation is complete".

#### Step 5. Create the `lms-local` job (once)

1. Dashboard, New Item, name `lms-local`, type **Pipeline**, OK.
2. Tick **This project is parameterized**, Add Parameter, String Parameter: name `BRANCH`, default `main`.
3. When using a Jenkins user and API token with Build permission (Step 7), no remote job token is required. **Trigger builds remotely** with token `lms-local` is an optional legacy alternative.
4. Pipeline: Definition **Pipeline script from SCM**, SCM **Git**.
   - Repository URL: the path of your clone, as the system running Jenkins sees it.
     WSL with the repository on the Windows drive: `/mnt/c/Users/User/source/repos/psoft-project-2024-g1`.
     macOS: `/Users/<you>/.../ARQSOFT_ODSOFT_G6`.
   - Branches to build: `*/${BRANCH}`.
   - Script Path: `jenkins/Jenkinsfile.local`.
5. Save.

#### Step 6. Run it once by hand

1. Open the job, **Build with Parameters**.
2. Leave `JAVA_HOME_17` empty: the pipeline finds your JDK 17 itself (macOS `java_home` and Homebrew, Linux and WSL `/usr/lib/jvm`). Only fill it if you installed JDK 17 somewhere unusual. Leave `RUN_MUTATION` unticked; mutation testing is slow.
3. Build. Open the build, **Console Output**, and watch the stages. A green build means the setup works.
   The first run is slow because Maven downloads dependencies.

#### Step 7. Connect the commit hook (once per clone)

1. In Jenkins: click your user name (top right), Security, **API Token**, Add new Token, Generate, copy it.
2. In the terminal where you commit, add these to `~/.bashrc` (WSL) or `~/.zshrc` (macOS), then open a new terminal:
   ```
   export JENKINS_URL=http://localhost:9090
   export JENKINS_USER=<your Jenkins user>
   export JENKINS_API_TOKEN=<the token>
   ```
3. From the repository root: `bash jenkins/hooks/install.sh`.
4. Test: `git commit --allow-empty -m "test: local pipeline"`. The terminal prints
   "post-commit: local pipeline started" and a new build appears in the `lms-local` job within seconds.
   If Jenkins is not running, the commit still succeeds and the hook only prints a warning.

Commits made from Windows tools (VS Code, GitHub Desktop) run the hook with Git for Windows. For them set the same three
variables in Windows (`setx JENKINS_URL http://localhost:9090`, and likewise the other two), then open the tool again.
If you commit from the WSL terminal, the steps above are enough.

The API token authenticates your Jenkins user. It is different from the optional remote job token. The hook sends the remote job token only when user/API-token authentication is absent, because an incorrect extra job token causes HTTP 403 even for an authenticated user.

If variables were saved in the Windows user environment after your terminal or IDE was opened, restart that application or load them into the current PowerShell session before committing:

```powershell
"JENKINS_URL", "JENKINS_USER", "JENKINS_API_TOKEN" | ForEach-Object {
    [Environment]::SetEnvironmentVariable($_, [Environment]::GetEnvironmentVariable($_, "User"), "Process")
}
```

#### If something goes wrong

| Symptom | Fix |
|---|---|
| `Java 17 found; Jenkins needs 21 or newer` (or Jenkins says it is older than the minimum) | Set `JENKINS_JAVA_HOME` to a JDK 21+ folder before running the script (Step 3). The build still uses 17 through `JAVA_HOME_17`. |
| `Address already in use` | Another Jenkins or program uses 9090. Stop it, or run `JENKINS_PORT=9191 bash jenkins/local/start-jenkins.sh` and use that port in the hook URL. |
| Download of `jenkins.war` fails | Download it from <https://www.jenkins.io/download/> (Generic Java package, war) and save it as `~/jenkins-local/jenkins.war`. |
| Build fails at checkout: `dubious ownership` | Run `git config --global --add safe.directory /mnt/c/Users/User/source/repos/psoft-project-2024-g1` in the terminal that started Jenkins, then rebuild. |
| Checkout fails: "local" repository not allowed | Jenkins was not started with `jenkins/local/start-jenkins.sh`, which sets the flag. Restart it with the script. |
| `publishHTML` not found | The HTML Publisher plugin is missing (Step 4.4). |
| `TypeTag :: UNKNOWN` in the build, or "No JDK 17 found" | Install JDK 17 (Step 1) or fill the build parameter `JAVA_HOME_17` with its folder. |
| Hook prints "could not reach the local Jenkins" | Jenkins is not running, or `JENKINS_URL` is wrong. |
| Hook reports HTTP 401, 403 or 404 | For 401/403, check `JENKINS_USER`, `JENKINS_API_TOKEN` and the user's Build permission. For 404, check `JENKINS_URL` and `LMS_LOCAL_JOB`. Test by hand: `curl -i -X POST -u "$JENKINS_USER:$JENKINS_API_TOKEN" "$JENKINS_URL/job/lms-local/buildWithParameters?BRANCH=main"`. |
| Bash says `\r: command not found` | The scripts got Windows line endings: `sed -i 's/\r$//' jenkins/hooks/* jenkins/local/*`. |

### 2. Staging (pipeline 2) on the Azure Jenkins
1. New item `lms-staging`, type Multibranch Pipeline.
2. Branch Source: GitHub or Git, repository `Max1124/ARQSOFT_ODSOFT_G6`. Behaviours: filter by name, include `feature/*`.
3. Build Configuration: by Jenkinsfile, Script Path `jenkins/Jenkinsfile.staging`.
4. Scan trigger: the existing GitHub webhook (event "push") makes the scan start; add a periodic scan as a fallback.
5. Several feature branches share dev (8081): the last one deployed wins. `disableConcurrentBuilds` only serialises
   builds of the same branch, so avoid pushing two feature branches at the same moment.

### 3. Release (pipeline 3)
Open the existing job `lms-pipeline` and change Script Path from `Jenkinsfile` to `jenkins/Jenkinsfile.release`. Branch
`*/main` and the webhook stay as they are. Then delete the old root `Jenkinsfile`, so there are no two copies to maintain.

## Branches

The pipelines assume feature work happens on `feature/<name>` branches and reaches `main` through a pull request.


#### Optional: Windows without opening WSL

Starts Jenkins hidden when you log in, and lets you commit from Windows tools such as IntelliJ. Do Steps 1 to 7 in WSL first.

1. In Ubuntu, from the repository root, copy the start script to your home folder with LF line endings:
   ```
   cp jenkins/local/start-jenkins.sh ~/start-jenkins.sh && sed -i 's/\r$//' ~/start-jenkins.sh
   ```
   If `start-jenkins.sh` changes later, repeat this step.
2. In PowerShell, set the hook variables, then fully close and reopen your Git tool:
   ```
   setx JENKINS_URL http://localhost:9090
   setx JENKINS_USER <your Jenkins user>
   setx JENKINS_API_TOKEN <the token>
   ```
3. Stop any Jenkins you started by hand. Then, in PowerShell as administrator, create and start the task. If your distro is not `Ubuntu` (check with `wsl -l -v`) or Jenkins uses another JDK 21+, change those values:
   ```
   $a = New-ScheduledTaskAction -Execute "conhost.exe" -Argument "--headless wsl.exe -d Ubuntu --cd ~ -- env JENKINS_JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 bash start-jenkins.sh"
   $t = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
   $s = New-ScheduledTaskSettingsSet -ExecutionTimeLimit 0 -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
   Register-ScheduledTask LmsJenkinsWsl -Action $a -Trigger $t -Settings $s
   Start-ScheduledTask LmsJenkinsWsl
   ```
4. After about a minute, check that <http://localhost:9090> opens. No window appears.
5. Commit from your Git tool. In IntelliJ, keep **Run Git hooks** ticked. A new build appears in `lms-local` within seconds.

To stop Jenkins, run `Stop-ScheduledTask LmsJenkinsWsl`. If the page still responds, run `wsl --terminate Ubuntu`, which stops all of Ubuntu.


#### Notification integration tests (Mailpit and Mockoon)

The `Notifications: integration tests` stage needs Node 18+, Mailpit and the **Lockable Resources** Jenkins plugin. Install them once, on the system that runs Jenkins. You don't need to install or start Mockoon: `jenkins/run-notification-its.sh` fetches Mockoon CLI with `npx` (internet needed the first time), starts Mailpit and Mockoon, and stops both when the tests end. No real email or SMS is sent.

**Windows (inside WSL Ubuntu, not PowerShell)**

```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
sudo bash < <(curl -sL https://raw.githubusercontent.com/axllent/mailpit/develop/install.sh)
which node npx mailpit
```

All three paths must be Linux paths. A `/mnt/c/...` path is the Windows copy and won't work.

**macOS**

```bash
brew install node mailpit
which node npx mailpit
```

If Jenkins was already running, restart it from a new terminal so it picks up the new tools.

**Jenkins plugin:** Manage Jenkins, Plugins, Available plugins, then install **Lockable Resources**.

**Run the tests without Jenkins**, from the repository root (in Ubuntu on Windows):

```bash
JAVA_HOME=$(bash jenkins/local/find-jdk17.sh) bash jenkins/run-notification-its.sh
```

| Symptom | Fix |
|---|---|
| `No such DSL method 'lock'` | Install the Lockable Resources plugin. |
| `Node is not installed` or `Mailpit is not installed` | Install it as above, on the system that runs Jenkins. |
| `Port 1025`, `8025` or `8027 is already in use` | Close your own Mailpit or Mockoon, then rebuild. |
| `./mvnw: /bin/sh^M: bad interpreter` when running by hand in WSL | Run `sed -i 's/\r$//' mvnw .mvn/wrapper/maven-wrapper.properties`. |
| SMS tests fail with `Connection refused` | `notification.sms.gateway-url` in `src/test/resources/application.properties` must be `http://localhost:8027/message`. |

## Test separation and validation in WSL

To run all test selections without Jenkins: `bash jenkins/local/verify-checks.sh`.

After Check Java, all three pipelines run:

1. Unit tests: clean target and run classes ending in Test.
2. Existing integration tests: run IT classes for existing behaviour, excluding Bibliographic, Notifications and LendingSystemIT, which run in their respective areas.
3. Build and package: create the JAR without cleaning the reports again.
4. Student areas: run tests, analysis and mutation testing as configured. Bibliographic has a separate integration test stage.
5. STAGING deploys to dev; release deploys to UAT and requests approval for PROD.

Integration and system test classes end in IT and remain in src/test/java.
The student areas may repeat general tests to produce their own coverage reports.
Do not activate all three student profiles in the same Maven invocation: each defines the test selection.
Bibliographic integration tests use simulated providers and do not need a Google key or Internet access to contact providers.

On Ubuntu, from the repository root:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
bash mvnw -B clean test -Dtest='*Test' -Dsurefire.reportsDirectory=target/surefire-reports-unit
bash mvnw -B test -Dtest='*IT,!**/bibliographicmanagement/**,!*Notification*IT,!ScheduledNotificationCheckIT,!LendingSystemIT' -Dsurefire.reportsDirectory=target/surefire-reports-existing-it
bash mvnw -B -Pbibliographic test
bash jenkins/run-bibliographic-its.sh
bash jenkins/run-notification-its.sh
```

### Google Books: private local configuration

Bibliographic settings are defined in `src/main/resources/application.properties`.
The property `bibliographic.google-books.api-key=${GOOGLE_BOOKS_API_KEY:}` receives the key
from the `GOOGLE_BOOKS_API_KEY` environment variable; the actual key is not stored in the project or the JAR.
In this WSL setup, new Bash terminals load the variable from
`~/.config/lms/google-books.env`, a local file outside the repository with permissions 600.
After changing the variable, restart the application so it inherits the new value.
Applications started directly on Windows need the variable in their Windows environment.
For an Azure deployment, provide the variable to the service running the JAR; a Jenkins credential
is not automatically inherited by the deployed application. Do not copy the key into Git or logs.
BIBLIOGRAPHIC_SOURCES and BIBLIOGRAPHIC_TIMEOUT_MS override the sources and timeout.

### Local notifications

The tests use SMS at localhost:8027/message with test-user/test-password, and SMTP at
localhost:1025. The script starts Mailpit/Mockoon and stops them when the tests finish. No real credentials are required.
Jenkins needs the Lockable Resources plugin to reserve the ports during these tests.

### Testing the three flows

First configure and run lms-local manually with the branch containing these changes.
Then install the hook and make a work commit: the hook calls localhost:9090
(or the explicit JENKINS_URL value). BRANCH is sent to the job and must be used for checkout.

Once LOCAL passes, create a feature/<name> branch, make a small change and commit,
validate LOCAL, then push that branch to test lms-staging and deployment to dev.
Confirm the review and Jenkins build before merging into main. A push/merge to main triggers
lms-pipeline: UAT, Newman and approval for PROD (keep AUTO_PROD=false during the trial).
A local commit does not trigger Azure. Do not test release with a direct push to main just to experiment.
The existing Azure addresses and deployment ports remain unchanged.

### Bibliographic integration tests

`jenkins/run-bibliographic-its.sh` runs only the Bibliographic `*IT` classes and is used by all three pipelines. It requires Java 17. The tests start and stop their own local HTTP providers on available ports, without Mockoon, Mailpit or a real Google key. Reports are saved in `target/surefire-reports-bibliographic-it`; coverage is appended to the bibliographic unit test coverage.

On WSL, from the repository root: `JAVA_HOME=$(bash jenkins/local/find-jdk17.sh) bash jenkins/run-bibliographic-its.sh`.

## Complete project coverage

All three pipelines finish their test stages with **Global coverage**. Each suite saves a separate execution-data snapshot in `target/coverage-data` before another stage resets its working coverage file. The final step merges those snapshots and generates a report across all production classes. Repeated tests contribute the union of executed code; percentages are not added or averaged. Notification delivery tests also collect coverage in their own file.

Open **JaCoCo (global)** in Jenkins, or `target/site/jacoco-global/index.html` locally. The conventional `target/site/jacoco/index.html` path is updated with the same final global report. Student reports remain available separately. Both line and instruction coverage are shown; compare the same metric and scope between runs. A report from `mvn test` alone covers only the default test selection and is not the complete unit-plus-integration result.

To measure the entire project without Jenkins, with Java 17, Node and Mailpit installed, run from the repository root in WSL:

```bash
bash jenkins/run-global-coverage.sh
```

This starts with a clean build, runs all `*Test` and `*IT` classes, and runs the notification delivery and scheduled-check tests with Mailpit/Mockoon. It then merges the coverage data and generates HTML, XML and CSV reports. `bash jenkins/local/verify-checks.sh` also publishes global coverage after its existing checks. The publishing helper rejects missing suite snapshots instead of silently presenting incomplete data as a complete report. PIT is separate from this functional coverage measurement.

### Bibliographic static analysis

All three pipelines run `Bibliographic: static analysis` through `areas.bibliographicStaticAnalysis()`. The `bibliographic` Maven profile restricts SpotBugs findings to `pt.psoft.g1.psoftg1.bibliographicmanagement` and its subpackages. Jenkins archives `target/spotbugsXml-bibliographic.xml` separately from the other student reports. The existing non-blocking policy for SpotBugs findings is unchanged.

To run it manually with Java 17: `./mvnw -B -Pbibliographic compile spotbugs:spotbugs`. The manual command writes `target/spotbugsXml.xml`; the Jenkins helper copies it to the bibliographic report filename.
