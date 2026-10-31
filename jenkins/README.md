# Pipelines

Three pipelines, one Jenkinsfile each. All three run the same per-student areas, defined once in `areas.groovy`.

| # | File | Where | Fires on | Does |
|---|---|---|---|---|
| 1 | `Jenkinsfile.local` | your own Jenkins | every local commit (git hook) | build, then the three student areas; no deploy; mutation testing off by default |
| 2 | `Jenkinsfile.staging` | Azure Jenkins, multibranch job | push to `feature/*` | build, three areas, archive the jar, deploy to **dev** (8081) |
| 3 | `Jenkinsfile.release` | Azure Jenkins, job `lms-pipeline` | push or merged PR on `main` | build, three areas, archive the jar, deploy to **uat** (8082) + Newman, then **prod** (8083) |

## One stage per student

Each pipeline shows three top-level stages, so the owner of a failure is obvious:

| Stage | Owner | Maven profile | Inside |
|---|---|---|---|
| Lending policies (Guilherme) | Guilherme | `-Plending` | tests + JaCoCo, SpotBugs, PIT |
| Bibliographic sources (Max1124) | Max1124 | `-Pbibliographic` | tests + JaCoCo, PIT |
| Reader notifications (AlexandreMouraCosta) | AlexandreMouraCosta | `-Pnotification` | tests + JaCoCo, PIT |

A failing area does not stop the other two from running (each leaf is wrapped in `catchError`); the build ends red, and
the archive and deploy stages are skipped. To change what an area runs, edit its Maven profile in `pom.xml` and its
functions in `areas.groovy`; the three Jenkinsfiles do not change.

Reports: `JaCoCo (lending|bibliographic|notification)` and `PIT (lending|bibliographic|notification)` on the build page.

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
4. Install one more plugin: Manage Jenkins, Plugins, Available plugins, search **HTML Publisher**, tick it, Install,
   and tick "Restart Jenkins when installation is complete".

#### Step 5. Create the `lms-local` job (once)

1. Dashboard, New Item, name `lms-local`, type **Pipeline**, OK.
2. Tick **This project is parameterized**, Add Parameter, String Parameter: name `BRANCH`, default `main`.
3. Build Triggers: tick **Trigger builds remotely** and set the authentication token to `lms-local`.
4. Pipeline: Definition **Pipeline script from SCM**, SCM **Git**.
   - Repository URL: the path of your clone, as the system running Jenkins sees it.
     WSL with the repository on the Windows drive: `/mnt/c/Users/<you>/Documents/GitHub/arqsoft_odsoft/ARQSOFT_ODSOFT_G6`.
     macOS: `/Users/<you>/.../ARQSOFT_ODSOFT_G6`.
   - Branches to build: `*/${BRANCH}`.
   - Script Path: `jenkins/Jenkinsfile.local`.
5. Save.

#### Step 6. Run it once by hand

1. Open the job, **Build with Parameters**.
2. Set `JAVA_HOME_17` to your JDK 17 folder from Step 2 (WSL: `/usr/lib/jvm/java-17-openjdk-amd64`; macOS: the `/opt/homebrew/opt/openjdk@17/...` path). Leave `RUN_MUTATION` unticked; mutation testing is slow.
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

#### If something goes wrong

| Symptom | Fix |
|---|---|
| `Java 17 found; Jenkins needs 21 or newer` (or Jenkins says it is older than the minimum) | Set `JENKINS_JAVA_HOME` to a JDK 21+ folder before running the script (Step 3). The build still uses 17 through `JAVA_HOME_17`. |
| `Address already in use` | Another Jenkins or program uses 9090. Stop it, or run `JENKINS_PORT=9191 bash jenkins/local/start-jenkins.sh` and use that port in the hook URL. |
| Download of `jenkins.war` fails | Download it from <https://www.jenkins.io/download/> (Generic Java package, war) and save it as `~/jenkins-local/jenkins.war`. |
| Build fails at checkout: `dubious ownership` | Run `git config --global --add safe.directory '*'` in the terminal that started Jenkins, then rebuild. |
| Checkout fails: "local" repository not allowed | Jenkins was not started with `jenkins/local/start-jenkins.sh`, which sets the flag. Restart it with the script. |
| `publishHTML` not found | The HTML Publisher plugin is missing (Step 4.4). |
| `TypeTag :: UNKNOWN` in the build | `JAVA_HOME_17` points to a JDK that is not 17. Fix the build parameter. |
| Hook prints "could not reach the local Jenkins" | Jenkins is not running, or `JENKINS_URL` is wrong. |
| Hook runs but no build starts (HTTP 403 or 404 is hidden) | Check the job name `lms-local`, the token `lms-local`, and `JENKINS_USER` / `JENKINS_API_TOKEN`. Test by hand: `curl -i -X POST -u "$JENKINS_USER:$JENKINS_API_TOKEN" "$JENKINS_URL/job/lms-local/buildWithParameters?token=lms-local&BRANCH=main"`. |
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
