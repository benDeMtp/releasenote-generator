package com.kawamind;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

@QuarkusMainTest
@Slf4j
class ReleaseNoteGeneratorTest {

    Repository tmpRepo;
    String tmpRepoPath;

    final PersonIdent personIdent = new PersonIdent("Bill","bill@ggamil.foo",LocalDate.parse("2018-05-05").atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() throws IOException {
        tmpRepo = createNewRepository();
        tmpRepoPath = tmpRepo.getDirectory().getAbsolutePath();
    }

    void addFileAndCommit(String message) throws IOException, GitAPIException {
        Git git = new Git(tmpRepo);
        AddCommand add = git.add();
        var path = tmpRepo.getDirectory().toPath();
        var fileName = UUID.randomUUID() +".html";
        var file1 = Files.createFile(path.resolve(fileName));
        add.addFilepattern(file1.getFileName().toString()).call();
        CommitCommand commit = git.commit();
        commit.setAuthor(personIdent).setMessage(message).call();
    }



    void addTag(String name) throws GitAPIException {
        Git git = new Git(tmpRepo);
        TagCommand tag = git.tag();
        tag.setTagger(personIdent).setName(name).setForceUpdate(true).call();
    }

    @Test
    @DisplayName("ReleaseNoteGenerator should fail with message if there is no commit")
    void rngShouldFailWithMessageIfThereIsNoCommit(QuarkusMainLauncher launcher){
        LaunchResult result =launcher.launch("-d",tmpRepoPath);
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.exitCode()).as("Status code should be 1").isEqualTo(1);
        softly.assertThat(result.getErrorOutput()).as("Error output should describe error").isEqualTo("There is no commit in the repository");

        softly.assertAll();
    }

    @Test
    @DisplayName("ReleaseNoteGenerator should create releasenote file a default location")
    void runReleaseNoteGeneratorShouldCreateReleaseNoteFile(QuarkusMainLauncher launcher) throws GitAPIException, IOException {
        addFileAndCommit("initial commit");
        LaunchResult result =launcher.launch("-d",tmpRepoPath);
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.exitCode()).as("Status code should be 0").isEqualTo(0);
        softly.assertThat(tmpRepo.getDirectory()).as("Directory should exists").exists();
        softly.assertThat(new File(tmpRepo.getDirectory().getAbsolutePath(),"release-note.adoc")).as("default releasenote file should exists").exists();

        softly.assertAll();

    }

    @Test
    @DisplayName("ReleaseNoteGenerator should create releasenote file to specified location")
    void runReleaseNoteGeneratorShouldCreateReleaseNoteFileToSpecifiedLocation(QuarkusMainLauncher launcher) throws GitAPIException, IOException {
        addFileAndCommit("initial commit");
        LaunchResult result =launcher.launch("-d",tmpRepoPath,"-o","foo.adoc");

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(result.exitCode()).as("Status code should be 0").isEqualTo(0);
        softly.assertThat(tmpRepo.getDirectory()).as("Directory should exists").exists();
        softly.assertThat(new File(tmpRepo.getDirectory().getAbsolutePath(),"foo.adoc")).as("releasenote file should exists").exists();

        softly.assertAll();

    }

    @Test
    @DisplayName("release note should respect without-convention.adoc")
    void releaseNoteShouldRespectTemplate1(QuarkusMainLauncher launcher) throws GitAPIException, IOException {
        var standard = new File("src/test/resources/without-convention.adoc");
        addFileAndCommit("Commit 1");
        addFileAndCommit("Commit 2");
        addTag("v1");
        addFileAndCommit("Commit 3");
        addFileAndCommit("Commit 4");
        addTag("v2");
        var releasenote = tmpRepo.getDirectory().toPath().resolve("release-note.adoc").toFile();

        LaunchResult result =launcher.launch("-d",tmpRepoPath);

        printReleaseNote(releasenote);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result.exitCode()).as("Status code should be 0").isEqualTo(0);
        softly.assertThat(releasenote).as("release note should not be empty").isNotEmpty();
        softly.assertThat(releasenote).as("release note should have the same content as sample file").hasSameTextualContentAs(standard);

        softly.assertAll();

    }

    @Test
    @DisplayName("release note should respect with-convention.adoc")
    void releaseNoteShouldRespectTemplateWIthConvention(QuarkusMainLauncher launcher) throws GitAPIException, IOException {
        var standard = new File("src/test/resources/with-convention.adoc");
        addFileAndCommit("fix: issue1");
        addFileAndCommit("fix: issue2");
        addFileAndCommit("feat: add awesome feature");
        addTag("v1");
        addFileAndCommit("test: fix tests");
        addFileAndCommit("build: add uber dep");
        addFileAndCommit("build: add uber dep2");
        addTag("v2");
        addFileAndCommit("ops: add deploy script");
        addFileAndCommit("docs: add doc one");
        addFileAndCommit("doc: add doc two");
        addFileAndCommit("perf: to the sky");
        addTag("v3");
        var releasenote = tmpRepo.getDirectory().toPath().resolve("release-note.adoc").toFile();

        LaunchResult result =launcher.launch("-d",tmpRepoPath);

        printReleaseNote(releasenote);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result.exitCode()).as("Status code should be 0").isEqualTo(0);
        softly.assertThat(releasenote).as("release note should not be empty").isNotEmpty();
        softly.assertThat(releasenote).as("release note should have the same content as sample file").hasSameTextualContentAs(standard);
        softly.assertAll();

    }

    @Test
    @DisplayName("release note should have an history section if there is more than 5 commits")
    void releaseNoteShouldHaveAnHistorySectionIfThereIsMoreThan5Commits(QuarkusMainLauncher launcher) throws GitAPIException, IOException {
        var standard = new File("src/test/resources/with-history.adoc");
        addFileAndCommit("fix: issue1");
        addTag("v1");
        addFileAndCommit("test: fix tests");
        addTag("v2");
        addFileAndCommit("ops: add deploy script");
        addTag("v3");
        addFileAndCommit("ops: add deploy script 2");
        addTag("v4");
        addFileAndCommit("ops: add deploy script 3");
        addTag("v5");
        addFileAndCommit("ops: add deploy script 4");
        addTag("v6");
        var releasenote = tmpRepo.getDirectory().toPath().resolve("release-note.adoc").toFile();


        LaunchResult result =launcher.launch("-d",tmpRepoPath);

        printReleaseNote(releasenote);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result.exitCode()).as("Status code should be 0").isEqualTo(0);
        softly.assertThat(releasenote).as("release note should not be empty").isNotEmpty();
        softly.assertThat(releasenote).as("release note should have the same content as sample file").hasSameTextualContentAs(standard);
        softly.assertAll();
    }

    @Nested
    @DisplayName("Tests with bugtracker options")
    class BugTracker {

        @Test
        @DisplayName("should failed if -b and -p are not in the command line")
        void shouldFailedIfDependentOptionsAreNotSet(QuarkusMainLauncher launcher) throws GitAPIException, IOException {
            addFileAndCommit("fix(project-1): big error");
            addFileAndCommit("feat(project-2): awesome feature");
            addTag("v1");

            LaunchResult result =launcher.launch("-d",tmpRepoPath,"-p","project-\\d*");

            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(result.exitCode()).as("Status code should be 1").isEqualTo(2);
            softly.assertThat(result.getErrorOutput()).as("Error output should describe error").startsWith("Error: Missing required argument(s): --bug-tracker-url=<url>");

            softly.assertAll();

        }

        @Test
        @DisplayName("release note should respect without-bugtracker.adoc")
        void releaseNoteShouldRespectTemplate1(QuarkusMainLauncher launcher) throws GitAPIException, IOException {
            var standard = new File("src/test/resources/with-bugtracker.adoc");
            addFileAndCommit("fix(project-1): big error");
            addFileAndCommit("feat(project-2): awesome feature");
            addTag("v1");
            var releasenote = tmpRepo.getDirectory().toPath().resolve("release-note.adoc").toFile();

            LaunchResult result =launcher.launch("-d",tmpRepoPath,"-p","project-\\d*","-b","http://mybugtracker.com/");

            printReleaseNote(releasenote);
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(result.exitCode()).as("Status code should be 0").isEqualTo(0);
            softly.assertThat(releasenote).as("release note should not be empty").isNotEmpty();
            softly.assertThat(releasenote).as("release note should have the same content as sample file").hasSameTextualContentAs(standard);

            softly.assertAll();

        }




    }


    @AfterEach
    void tearDown() {
        var unused = tmpRepo.getDirectory().delete();
    }

    private static Repository createNewRepository() throws IOException {
        // prepare a new folder
        File localPath = File.createTempFile("TestGitRepository", "");
        if(!localPath.delete()) {
            throw new IOException("Could not delete temporary file " + localPath);
        }

        if(!localPath.mkdirs()) {
            throw new IOException("Could not create directory " + localPath);
        }

        // create the directory
        Repository repository = FileRepositoryBuilder.create(new File(localPath, ".git"));
        repository.create();

        return repository;
    }

    void printReleaseNote(File releasenote) throws IOException {
        try(var read  = new BufferedReader(new FileReader(releasenote))) {
            System.out.println("****");
            read.lines().forEach(System.out::println);
            System.out.println("****");
        }
    }

}