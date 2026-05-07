# Super extra bonus: Advanced GitHub Actions

These exercises extend the CI workflow built in the main lab. Each one is independent, you can do them in any order. Every exercise introduces a GitHub Actions concept not covered in the main lab or in class, so read the **Context** block before starting.

All changes go inside `.github/workflows/` unless stated otherwise.

---

## Exercise 1: Split Build and Test into Separate Jobs

### Context

So far the workflow has a single job that does everything. GitHub Actions lets you define multiple jobs that run sequentially using the `needs:` keyword. This mirrors how real pipelines work: compile first, then run tests only if compilation succeeds. Each job runs on a fresh virtual machine, so artifacts (compiled code) must be explicitly passed between them.

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - ...

  test:
    runs-on: ubuntu-latest
    needs: build        # waits for build to finish and pass
    steps:
      - ...
```

To pass files between jobs, use two built-in actions:
- `actions/upload-artifact@v4`: saves files from one job
- `actions/download-artifact@v4`: retrieves them in the next job

### Task

1. Split your current `build-and-test` job into two jobs:
   - `build`: checks out, sets up JDK, caches Maven, runs `mvn clean package -DskipTests`, then uploads the `target/` directory as an artifact named `build-output`
   - `test`: downloads the artifact, then runs `mvn test`

2. Add `needs: build` to the `test` job.

3. Push and verify in the Actions tab that the two jobs appear sequentially, and that `test` only starts after `build` turns green.

4. Intentionally break the build step (e.g. add a syntax error). Confirm that `test` is skipped entirely: it shows as "skipped" not "failed".

---

## Exercise 2: Matrix Builds. Test on Multiple Java Versions

### Context

A **matrix build** runs the same job multiple times in parallel with different input values. It is used to verify that your code works across multiple Java versions, operating systems, or dependency versions.

```yaml
jobs:
  test:
    strategy:
      matrix:
        java: [17, 21]
    steps:
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}   # value comes from the matrix
```

This produces two parallel jobs. The overall workflow only passes if both pass. You can see both runs side by side in the Actions tab.

### Task

1. Add a `strategy.matrix` block to your job with Java versions `17` and `21`.

2. Replace the hardcoded `java-version: '17'` with `${{ matrix.java }}`.

3. Update the Maven cache key to include the Java version so the two jobs maintain separate caches:
   ```yaml
   key: ${{ runner.os }}-java${{ matrix.java }}-maven-${{ hashFiles('**/pom.xml') }}
   ```

4. Push and observe two parallel jobs in the Actions tab. Check that both pass.

5. **Bonus**: Add `fail-fast: false` under `strategy:` and observe what it means: by default, if one matrix job fails, GitHub cancels the others immediately. `fail-fast: false` lets all jobs run to completion regardless.

---

## Exercise 3: Secrets and Environment Variables

### Context

Hard-coding configuration values (ports, credentials, feature flags) in workflow files is not the best practice. They are committed to source control and hard to change per environment. GitHub provides two mechanisms:

- **Repository variables** (`vars.NAME`): non-sensitive values visible in logs
- **Repository secrets** (`secrets.NAME`): sensitive values that are masked in logs and never exposed

You set both in **GitHub → Settings → Secrets and variables → Actions**.

In a workflow file you reference them as:
```yaml
env:
  SERVER_PORT: ${{ vars.SERVER_PORT }}
  DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
```

They can be set at the job level (available to all steps in the job) or at the step level (available only to that step).

### Task

1. In your GitHub repository settings, create:
   - A **variable** named `SERVER_PORT` with value `8080`
   - A **secret** named `APP_ENV` with value `ci`

2. Update your workflow to pass both as environment variables to the build/test step:
   ```yaml
   env:
     SERVER_PORT: ${{ vars.SERVER_PORT }}
     SPRING_PROFILES_ACTIVE: ${{ secrets.APP_ENV }}
   ```

3. Push and open the workflow run logs. Verify that `SERVER_PORT` value (`8080`) is visible in the logs but `APP_ENV` appears as `***`.

4. Try printing a secret value with `run: echo ${{ secrets.APP_ENV }}` in a step. Observe how GitHub automatically redacts it in the output.

---

## Exercise 4: Notify on Failure

### Context

In real teams, a broken pipeline needs to alert someone immediately. GitHub Actions supports **conditional steps** using the `if:` keyword. The built-in function `failure()` returns true only when a previous step in the job has failed.

```yaml
- name: Notify on failure
  if: failure()
  run: echo "Something went wrong!"
```

A common real-world pattern is to post to a Slack webhook or send an email. For this exercise you will use a GitHub Actions action that creates a **GitHub Issue** automatically when the build breaks: no external service needed.

The action `actions/github-script@v7` lets you call the GitHub API directly from a step using JavaScript:

```yaml
- uses: actions/github-script@v7
  with:
    script: |
      github.rest.issues.create({
        owner: context.repo.owner,
        repo: context.repo.repo,
        title: 'CI pipeline failed',
        body: `Run: ${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId}`
      })
```

### Task

1. Add a final step to your `build-and-test` job that only runs when the job has failed (`if: failure()`).

2. Use `actions/github-script@v7` to open a GitHub Issue with:
   - Title: `"CI failed on <branch name>"`: use `${{ github.ref_name }}` to get the branch
   - Body: a direct link to the failed run using `context.runId`

3. Intentionally break a test, push, and verify that a new issue is automatically created in your repository's Issues tab.

4. Fix the test, push again, and add a second step (also `if: failure()`) that closes any open issues with the title containing "CI failed": use `github.rest.issues.list` and `github.rest.issues.update`.

---

## Exercise 5: Scheduled Runs

### Context

Beyond push and pull-request triggers, GitHub Actions supports a `schedule:` trigger that runs your workflow on a cron schedule. Useful for nightly builds, dependency audits, or health checks against a staging environment.

Cron syntax has five fields: `minute hour day-of-month month day-of-week`. GitHub Actions uses UTC.

```yaml
on:
  schedule:
    - cron: '0 3 * * 1-5'   # 03:00 UTC, Monday–Friday
```

A few things to know:
- Scheduled workflows only run on the **default branch** (usually `main`)
- GitHub may delay scheduled runs by up to 15 minutes during high load
- If no one has committed to the repo in 60 days, GitHub disables the schedule

### Task

1. Add a `schedule:` trigger to your workflow that runs every day at 06:00 UTC.

2. Add a step that only runs on scheduled triggers (not on push or PR) using:
   ```yaml
   if: github.event_name == 'schedule'
   ```
   Have it print a message like `"Scheduled nightly run – all tests passed"`.

3. To test without waiting for 06:00 UTC, trigger the workflow manually via `workflow_dispatch` (already added in the main lab) and confirm the scheduled-only step is skipped.

4. Check **Actions → your workflow** to see the next scheduled run time shown by GitHub.

---

## Exercise 6: Branch-Specific Workflow Behaviour

### Context

Not all branches need the same pipeline. A common strategy is:

- **Feature branches**: run tests only (fast feedback)
- **`main` branch**: run tests + produce a release artifact (JAR)

GitHub Actions exposes the current branch as `github.ref_name`. You can use it in `if:` conditions on individual steps or entire jobs to skip or include them based on which branch triggered the run.

```yaml
- name: Package for release
  if: github.ref_name == 'main'
  run: mvn package -DskipTests
```

Alternatively, you can use **path filters** to skip the pipeline entirely when only documentation files change:
```yaml
on:
  push:
    branches: [main]
    paths-ignore:
      - '**.md'
```

### Task

1. Create a new branch called `feature/branch-test`.

2. Add a step to your workflow that only runs when the branch is `main`:
   - Runs `mvn clean package -DskipTests`
   - Uploads the resulting JAR from `target/` as a build artifact named `release-jar` using `actions/upload-artifact@v4`

3. Add `paths-ignore` to your `push` trigger so that changes to `.md` files do not trigger the workflow on any branch.

4. Push a commit that only changes `README.md` on `main` and verify no workflow run is triggered.

5. Push a code change on `feature/branch-test` and verify the release step is skipped but tests still run.

6. Push a code change on `main` and verify the release step runs and the JAR appears in the run's **Artifacts** section.
