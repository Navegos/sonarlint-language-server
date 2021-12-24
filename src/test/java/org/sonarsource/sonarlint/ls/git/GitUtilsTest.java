/*
 * SonarLint Language Server
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.sonarlint.ls.git;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonarsource.sonarlint.core.serverapi.branches.ServerBranch;
import org.sonarsource.sonarlint.ls.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitUtilsTest {

  @Test
  void noGitRepoShouldBeNull(@TempDir File projectDir) throws IOException, URISyntaxException {
    javaUnzip("no-git-repo.zip", projectDir);
    URI uri = new URI(projectDir.toURI() + "no-git-repo");
    Git git = GitUtils.getGitForDir(uri);

    assertThat(git).isNull();
  }

  @Test
  void gitRepoShouldBeNotNull(@TempDir File projectDir) throws IOException, URISyntaxException {
    javaUnzip("dummy-git.zip", projectDir);
    URI uri = new URI(projectDir.toURI() + "dummy-git");
    Git git = GitUtils.getGitForDir(uri);

    assertThat(git).isNotNull();

    Map<String, List<String>> commitsCache = GitUtils.buildCommitsCache(git);
    Optional<String> branch = GitUtils.electSQBranchForLocalBranch("foo", git,
      Set.of(new ServerBranch("foo", false),
        new ServerBranch("bar", false),
        new ServerBranch("master", true)));

    assertThat(commitsCache).hasSize(5);
    assertThat(branch).contains("master");
  }

  @Test
  void shouldElectAnalyzedBranch(@TempDir File projectDir) throws IOException, URISyntaxException {
    javaUnzip("analyzed-branch.zip", projectDir);
    URI uri = new URI(projectDir.toURI() + "analyzed-branch");
    Git git = GitUtils.getGitForDir(uri);

    assertThat(git).isNotNull();

    Map<String, List<String>> commitsCache = GitUtils.buildCommitsCache(git);
    Optional<String> branch = GitUtils.electSQBranchForLocalBranch("closest_branch", git,
      Set.of(new ServerBranch("foo", false),
        new ServerBranch("closest_branch", false),
        new ServerBranch("master", true)));

    assertThat(commitsCache).hasSize(3);
    assertThat(branch).contains("closest_branch");
  }

  @Test
  void shouldElectClosestBranch(@TempDir File projectDir) throws IOException, URISyntaxException {
    javaUnzip("closest-branch.zip", projectDir);
    URI uri = new URI(projectDir.toURI() + "closest-branch");
    Git git = GitUtils.getGitForDir(uri);

    assertThat(git).isNotNull();

    Map<String, List<String>> commitsCache = GitUtils.buildCommitsCache(git);
    Optional<String> branch = GitUtils.electSQBranchForLocalBranch("current_branch", git,
      Set.of(new ServerBranch("foo", false),
        new ServerBranch("closest_branch", false),
        new ServerBranch("master", true)));

    assertThat(commitsCache).hasSize(3);
    assertThat(branch).contains("closest_branch");
  }

  @Test
  void shouldElectMasterForNonAnalyzedChildBranch(@TempDir File projectDir) throws IOException, URISyntaxException {
    javaUnzip("child-from-non-analyzed.zip", projectDir);
    URI uri = new URI(projectDir.toURI() + "child-from-non-analyzed");
    Git git = GitUtils.getGitForDir(uri);

    assertThat(git).isNotNull();

    Map<String, List<String>> commitsCache = GitUtils.buildCommitsCache(git);
    Optional<String> branch = GitUtils.electSQBranchForLocalBranch("not_analyzed_branch", git,
      Set.of(new ServerBranch("foo", false),
        new ServerBranch("branch_to_analyze", false),
        new ServerBranch("master", true)));

    assertThat(commitsCache).hasSize(5);
    assertThat(branch).contains("master");
  }

  @Test
  void shouldReturnEmptyOptionalOnException() throws IOException {
    Git git = mock(Git.class);
    Repository repo = mock(Repository.class);
    RefDatabase database = mock(RefDatabase.class);
    when(git.getRepository()).thenReturn(repo);
    when(repo.getRefDatabase()).thenReturn(database);
    when(database.getRefs()).thenThrow(new IOException());

    Optional<String> branch = GitUtils.electSQBranchForLocalBranch("foo", git,
      Set.of(new ServerBranch("foo", false),
        new ServerBranch("bar", false),
        new ServerBranch("master", true)));

    assertThat(branch).isEmpty();
  }

  @Test
  void shouldReturnEmptyOptionalIfNoMainBranch(@TempDir File projectDir) throws IOException, URISyntaxException {
    javaUnzip("dummy-git.zip", projectDir);
    URI uri = new URI(projectDir.toURI() + "dummy-git");
    Git git = GitUtils.getGitForDir(uri);


    Optional<String> branch = GitUtils.electSQBranchForLocalBranch("foo", git,
      Set.of(new ServerBranch("foo", false),
        new ServerBranch("bar", false),
        new ServerBranch("master", false)));

    assertThat(branch).isEmpty();
  }


  @Test
  void shouldReturnEmptyCacheOnException() throws IOException {
    Git git = mock(Git.class);
    Repository repo = mock(Repository.class);
    RefDatabase database = mock(RefDatabase.class);
    when(git.getRepository()).thenReturn(repo);
    when(repo.getRefDatabase()).thenReturn(database);
    when(database.getRefs()).thenThrow(new IOException());

    Map<String, List<String>> cache = GitUtils.buildCommitsCache(git);

    assertThat(cache).isEmpty();
  }

  public void javaUnzip(String zipFileName, File toDir) throws IOException {
    try {
      File testRepos = new File(Utils.class.getResource("/test-repos").toURI());
      File zipFile = new File(testRepos, zipFileName);
      javaUnzip(zipFile, toDir);
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  private static void javaUnzip(File zip, File toDir) {
    try {
      try (ZipFile zipFile = new ZipFile(zip)) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          File to = new File(toDir, entry.getName());
          if (entry.isDirectory()) {
            FileUtils.forceMkdir(to);
          } else {
            File parent = to.getParentFile();
            if (parent != null) {
              FileUtils.forceMkdir(parent);
            }

            Files.copy(zipFile.getInputStream(entry), to.toPath());
          }
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException(format("Fail to unzip %s to %s", zip, toDir), e);
    }
  }
}