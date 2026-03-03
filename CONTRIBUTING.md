<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
</head>
<body>

<h1>Git Workflow Guidelines</h1>
<p>This page describes our <strong>branch naming conventions</strong> and <strong>Pull Request (PR) guidelines</strong> to ensure a consistent and professional Git workflow across the team.</p>
<hr>

<h2>📑 Table of Contents</h2>
<ul>
  <li>
    <a href="#branch-naming-convention">1️⃣ Branch Naming Convention</a>
    <ul>
      <li><a href="#general-rules">General Rules</a></li>
      <li><a href="#allowed-branch-structures">Allowed Branch Structures</a></li>
      <li><a href="#summary-table">Summary Table</a></li>
      <li><a href="#additional-rules">Additional Rules</a></li>
    </ul>
  </li>
  <li>
    <a href="#pull-request-guidelines">2️⃣ Pull Request Guidelines</a>
    <ul>
      <li><a href="#pr-general-rules">General Rules</a></li>
      <li><a href="#pr-content">PR Content</a></li>
      <li><a href="#workflow-example">Workflow Example</a></li>
      <li><a href="#best-practices">Best Practices</a></li>
      <li><a href="#do-not">Do Not</a></li>
    </ul>
  </li>
</ul>
<hr>

<h2 id="branch-naming-convention">1️⃣ Branch Naming Convention</h2>
<p>To keep our workflow clean, readable, and consistent, all developers must follow these branch naming rules.</p>

<h3 id="general-rules">General Rules</h3>
<ol>
  <li>
    Start branches with <strong>one of these categories</strong>:
    <ul>
      <li><code>feature</code> — new functionality or UI</li>
      <li><code>bugfix</code> — fixing non-critical bugs</li>
      <li><code>hotfix</code> — urgent fix for production issues</li>
      <li><code>chore</code> — setup, cleanup, configs, folder structure, refactors</li>
    </ul>
  </li>
  <li>Branch format:</li>
</ol>

<pre>
type/domain/short-description
</pre>


<ol start="3">
  <li><strong>Domain</strong> is either <code>frontend</code> or <code>backend</code>.</li>
  <li><strong>Short-description</strong> should be lowercase, kebab-case (<code>-</code>), and descriptive.</li>
</ol>

<h3 id="allowed-branch-structures">Allowed Branch Structures</h3>
<p><strong>Chores</strong> (setup, configs, boilerplate):</p>
<pre>
chore/frontend/<detail-of-task>
chore/backend/<detail-of-task>
</pre>

<p><strong>Features</strong> (user-facing functionality):</p>
<pre>
feature/frontend/<feature-name>
feature/backend/<feature-name>
</pre>

<p><strong>Bug Fixes</strong> (non-critical fixes):</p>
<pre>
bugfix/frontend/<issue-name>
bugfix/backend/<issue-name>
</pre>

<p><strong>Hotfixes</strong> (critical, urgent fixes):</p>
<pre>
hotfix/frontend/<issue-name>
hotfix/backend/<issue-name>
</pre>

<h3 id="summary-table">Summary Table</h3>
<table border="1" cellspacing="0" cellpadding="5">
  <tr>
    <th>Type</th>
    <th>When to Use</th>
    <th>Pattern</th>
  </tr>
  <tr>
    <td>chore</td>
    <td>Setup, configs, folder structure</td>
    <td><code>chore/{frontend|backend}/desc</code></td>
  </tr>
  <tr>
    <td>feature</td>
    <td>New functionality</td>
    <td><code>feature/{frontend|backend}/name</code></td>
  </tr>
  <tr>
    <td>bugfix</td>
    <td>Normal bug fix</td>
    <td><code>bugfix/{frontend|backend}/name</code></td>
  </tr>
  <tr>
    <td>hotfix</td>
    <td>Production-critical fix</td>
    <td><code>hotfix/{frontend|backend}/name</code></td>
  </tr>
</table>

<h3 id="additional-rules">Additional Rules</h3>
<ul>
  <li>Never create branches directly named <code>frontend</code> or <code>backend</code>.</li>
  <li>Avoid spaces, uppercase letters, or special characters.</li>
  <li>Each branch must go through a <strong>Pull Request</strong> into <code>main</code>.</li>
  <li>Avoid long-lived branches; keep changes small and frequent.</li>
</ul>
<hr>

<h2 id="pull-request-guidelines">2️⃣ Pull Request Guidelines</h2>
<p>All code changes must go through Pull Requests (PRs) before merging into the <code>main</code> branch.</p>

<h3 id="pr-general-rules">General Rules</h3>
<ol>
  <li>All changes must be made in a <strong>feature/bugfix/chore/hotfix branch</strong>.</li>
  <li>One PR per task or feature — keep PRs small and focused.</li>
  <li>PRs must target the <code>main</code> branch (unless specified otherwise).</li>
  <li>Every PR must be <strong>reviewed and approved by at least one team member</strong>.</li>
  <li>Update your branch with <code>main</code> before merging.</li>
</ol>

<h3 id="pr-content">PR Content</h3>
<ul>
  <li><strong>Title</strong> (clear and descriptive)</li>
</ul>
<pre>
[feature/frontend] Add login page
[bugfix/backend] Fix email validation
[chore/frontend] Setup routing structure
</pre>

<ul>
  <li><strong>Description</strong>:
    <ul>
      <li>What the PR does</li>
      <li>Why it is needed</li>
      <li>Screenshots or demos if applicable</li>
      <li>Link to related issue or task</li>
    </ul>
  </li>
  <li><strong>Checklist (Optional but Recommended)</strong>:
    <ul>
      <li><input disabled type="checkbox"> Code builds without errors</li>
      <li><input disabled type="checkbox"> Tests added / updated</li>
      <li><input disabled type="checkbox"> Code follows project style guidelines</li>
      <li><input disabled type="checkbox"> No sensitive data committed</li>
      <li><input disabled type="checkbox"> PR reviewed and approved</li>
    </ul>
  </li>
</ul>

<h3 id="workflow-example">Workflow Example</h3>
<pre>
git checkout main
git pull origin main
git checkout -b feature/frontend/login-page

# work on feature
git add .
git commit -m "feature/frontend: implement login page"
git push -u origin feature/frontend/login-page

# open PR to main
</pre>

<ul>
  <li>Reviewer checks the code</li>
  <li>Reviewer requests changes if needed</li>
  <li>Author fixes issues → PR updates automatically</li>
  <li>Once approved, PR is merged into <code>main</code></li>
</ul>

<h3 id="best-practices">Best Practices</h3>
<ul>
  <li>Small, frequent PRs</li>
  <li>Clear, descriptive commit messages</li>
  <li>Include related issue/ticket references</li>
  <li>Use code reviews effectively</li>
</ul>

<h3 id="do-not">Do Not</h3>
<ul>
  <li>Merge without review</li>
  <li>Commit secrets or sensitive data</li>
  <li>Push directly to <code>main</code> without PR</li>
</ul>

</body>
</html>
