///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//REPO mavencentral
//DEPS info.picocli:picocli:4.6.3
//DEPS org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r
//DEPS org.slf4j:slf4j-simple:2.0.11
package com.kawamind;

import com.kawamind.config.ConfigService;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Command(name = "releaseNoteGenerator", mixinStandardHelpOptions = true, version = "releaseNoteGenerator 0.1",
        description = "generate releaseNote for conventional commits")
public class ReleaseNoteGenerator implements Runnable {

    final SimpleDateFormat spf = new SimpleDateFormat("dd/MM/yyyy");


    @Option(description = "local git repository", names = {"-d", "--directory"})
    private String target;

    @Option(description = "file relative path where the release note will be writen", names = {"-o", "--output"}, defaultValue = "release-note.adoc")
    private String output;

    @Option(description = "Tag name", names = {"-t", "--tags"})
    private String tag;


    @CommandLine.ArgGroup(exclusive = false)
    BugTracker bugTracker;

    final ConfigService configService;


    static class BugTracker {
        @Option(description = "Issue id pattern (regex)", names = {"-p", "--pattern"}, required = true)
        private String issueIdPattern;

        @Option(description = "Bug traker url", names = {"-b", "--bug-tracker-url"}, required = true)
        private String url;
    }

    Pattern issueKey;

    private static final int DEFAULT_MAX_VERSION = 5;

    @Inject
    public ReleaseNoteGenerator(ConfigService configService) {
        this.configService = configService;
    }

    @SneakyThrows
    @Override
    public void run() {
        if (Objects.isNull(target))
            target = System.getProperty("user.dir");
        Path p = Path.of(target);

        if (!Objects.isNull(bugTracker) && !Objects.isNull(bugTracker.issueIdPattern)) {
            issueKey = Pattern.compile(bugTracker.issueIdPattern, Pattern.CASE_INSENSITIVE);
        }


        try (var git = Git.open(p.toFile())) {
            var reached = new AtomicBoolean(Boolean.FALSE);
            List<Ref> allTags = new ArrayList<>(git.tagList().call().stream().toList());
            allTags.sort((o1, o2) -> o2.getName().compareTo(o1.getName()));


            var firstTagReached = new AtomicBoolean(Boolean.FALSE);
            var givenTagReached = new AtomicBoolean(Boolean.FALSE);
            try (var releasenote = new PrintWriter(new FileWriter(p.resolve(output).toFile(), false))) {
                releasenote.println("""
                        ==  Releases Note
                        :toc:
                        :toc-title: Versions récentes
                                       
                        """);

                Iterable<RevCommit> commits = null;
                final List<Ref> tagsList = allTags.stream().toList();
                commits = git.log().call();
                var listedTag = new AtomicInteger(0);
                System.out.println("Tag existants : " + tagsList.size());

                List<ReleaseNoteForVersion> versions2commit = new ArrayList<>();
                AtomicReference<ReleasedVersion> currentVersion = new AtomicReference<>();
                AtomicInteger currentVersionId = new AtomicInteger();

                commits.forEach(currentCommit -> {
                    if (tagsList.stream().anyMatch(tg -> hasTagMatchingCommit(tg, currentCommit, git.getRepository()))) {//Check if the commit have an tag
                        if (!versions2commit.isEmpty()) {
                            currentVersionId.getAndIncrement();
                        }
                        String version = tagsList.stream().filter(tg -> hasTagMatchingCommit(tg, currentCommit, git.getRepository())).findFirst().get().getName().replace("refs/tags/", "");
                        currentVersion.set(new ReleasedVersion(version, spf.format(currentCommit.getAuthorIdent().getWhen())));
                        versions2commit.add(new ReleaseNoteForVersion(currentVersion.get(), new ArrayList<>()));
                    }

                    if (!versions2commit.isEmpty() && messageFilter.test(currentCommit.getFullMessage()) && !currentCommit.getFullMessage().startsWith("[dev]"))
                        versions2commit.get(currentVersionId.get()).commits.add(currentCommit);
                });


                String typePattern = "^((build|fix|docs|doc|feat|refactor|style|test|chore|ops|perf)?\\(?([a-zA-Z0-9\\-\\s]*)\\)?:?(.*))$";
                final Pattern pattern = Pattern.compile(typePattern);
                final List<ToDisplay> versionToDisplay = new ArrayList<>();
                versions2commit.forEach((rnfv) -> {
                    final Map<String, List<String>> commitsByType = new HashMap<>();
                    rnfv.commits.forEach(commit -> {
                        var m = pattern.matcher(commit.getShortMessage());
                        if (m.find()) {
                            var type = m.group(2) != null ? m.group(2) : "chore";
                            var precision = m.group(3);
                            var commitmessage = type != null ? m.group(4) : (m.group(3) != null ? m.group(3) : "");


                            if (!commitsByType.containsKey(type)) {
                                commitsByType.put(type, new ArrayList<>());
                            }
                            try {
                                Supplier<String> commitMessageSupplier = () -> commitmessage.trim().isEmpty() ? "" : handleCommitMessage(commitmessage).trim();
                                Supplier<String> commitMessageSuplier2 = () -> (((precision != null && !precision.trim().isEmpty()) ? (handleCommitMessage(precision).trim() + " : ") : "") + commitMessageSupplier.get()).trim();
                                if (commitMessageSuplier2.get() != null && !commitMessageSuplier2.get().isEmpty()) {
                                    commitsByType.get(type).add(commitMessageSuplier2.get());
                                } else {
                                    System.out.println("commitMessageSupplier " + commitMessageSupplier.get());
                                    System.out.println("commitMessageSupplier2 " + commitMessageSuplier2.get());
                                    System.out.println(commit.getShortMessage());
                                    System.out.println(type + " / '" + precision + "' / " + commitmessage);
                                }
                            } catch (Exception e) {
                                System.out.println("there is an issue with " + commit.getShortMessage());
                                System.out.println(type + " " + precision + " " + commitmessage);
                            }


                        }
                    });
                    versionToDisplay.add(new ToDisplay(rnfv, commitsByType));
                });


                versionToDisplay.forEach((td) -> {
                    if (firstTagReached.get()) {
                        releasenote.println("");
                    }

                    if (!givenTagReached.get() && (isPoinsonPill.apply(tag, td.releaseNoteForVersion.releasedVersion.version) || (Objects.isNull(tag) && listedTag.get() >= DEFAULT_MAX_VERSION))) {//View : if the condition is true, switch to historic mode
                        releasenote.println("." + configService.getHistorySectionTitle());
                        releasenote.println("[%collapsible]");
                        releasenote.println("====");
                        givenTagReached.set(Boolean.TRUE);
                    }
                    releasenote.println(versionStringAdoc(td.releaseNoteForVersion.releasedVersion.version, td.releaseNoteForVersion.releasedVersion.date, givenTagReached.get()));//display the version number
                    firstTagReached.set(Boolean.TRUE);
                    listedTag.getAndIncrement();

                    if (td.types.containsKey("feat")) {
                        releasenote.println(typeStringAdoc(firstTagReached.get()) + configService.getFeature());
                        td.types.get("feat").forEach(t -> releasenote.println(commitStringAdoc(firstTagReached.get()) + t));
                        releasenote.println();
                    }
                    if (td.types.containsKey("fix")) {
                        releasenote.println(typeStringAdoc(firstTagReached.get()) + configService.getFix());
                        td.types.get("fix").forEach(t -> releasenote.println(commitStringAdoc(firstTagReached.get()) + t));
                        releasenote.println();
                    }
                    if (td.types.containsKey("refactor") || td.types.containsKey("perf")) {
                        releasenote.println(typeStringAdoc(firstTagReached.get()) + configService.getRefactor());
                        if (td.types.containsKey("refactor"))
                            td.types.get("refactor").forEach(t -> releasenote.println(commitStringAdoc(firstTagReached.get()) + t));
                        if (td.types.containsKey("perf"))
                            td.types.get("perf").forEach(t -> releasenote.println(commitStringAdoc(firstTagReached.get()) + t));
                        releasenote.println();
                    }
                    if (td.types.containsKey("build")) {
                        releasenote.println(typeStringAdoc(firstTagReached.get()) + configService.getBuild());
                        td.types.get("build").forEach(t -> releasenote.println(commitStringAdoc(firstTagReached.get()) + t));
                        releasenote.println();
                    }
                    if (td.types.containsKey("ops")) {
                        releasenote.println(typeStringAdoc(firstTagReached.get()) + configService.getOps());
                        td.types.get("ops").forEach(t -> releasenote.println(commitStringAdoc(firstTagReached.get()) + t));
                        releasenote.println();
                    }
                    if (td.types.containsKey("styled")) {
                        releasenote.println(typeStringAdoc(firstTagReached.get()) + configService.getStyle());
                        td.types.get("style").forEach(t -> releasenote.println(commitStringAdoc(firstTagReached.get()) + t));
                        releasenote.println();
                    }
                    if (td.types.containsKey("doc") || td.types.containsKey("docs")) {
                        releasenote.println(typeStringAdoc(firstTagReached.get()) + configService.getDoc());
                        if (td.types.containsKey("doc"))
                            td.types.get("doc").forEach(t -> releasenote.println(commitStringAdoc(firstTagReached.get()) + t));
                        if (td.types.containsKey("docs"))
                            td.types.get("docs").forEach(t -> releasenote.println(commitStringAdoc(firstTagReached.get()) + t));
                        releasenote.println();
                    }
                    if (td.types.containsKey("chore")) {
                        releasenote.println(typeStringAdoc(firstTagReached.get()) + "Divers");
                        td.types.get("chore").forEach(t -> releasenote.println(commitStringAdoc(firstTagReached.get()) + t));
                        releasenote.println();
                    }

                });


                //fin de la release note
                if (givenTagReached.get()) {
                    releasenote.println("====");
                }
            }
        }
    }

    Boolean hasTagMatchingCommit(Ref t, RevCommit u, Repository repo) {
        return getActualRefObjectId(t, repo).equals(u.getId());

    }

    private ObjectId getActualRefObjectId(Ref ref, Repository repo) {
        final Ref repoPeeled = repo.peel(ref);
        if (repoPeeled.getPeeledObjectId() != null) {
            return repoPeeled.getPeeledObjectId();
        }
        return ref.getObjectId();
    }

    Pattern oldReleasePattern = Pattern.compile("^release\\s\\d(\\.\\d{1,3}){1,2}.*$");

    String[] filteredMessage = {"[release]", "nouvelle version", "Merge branch", "releasenote", "prepare next dev version"};

    Predicate<String> messageFilter = messageToTest -> {

        if (Arrays.stream(filteredMessage).anyMatch(messageToTest::contains)) {
            return false;
        } else {

            var m = oldReleasePattern.matcher(messageToTest.trim());
            return !m.matches();
        }
    };

    BiFunction<String, String, Boolean> isPoinsonPill = (givenTag, currentTag) -> {
        if (Objects.isNull(givenTag) || givenTag.isBlank()) {
            return Boolean.FALSE;
        } else {
            if (givenTag.equals(currentTag.replace("refs/tags/", "")))
                return Boolean.TRUE;
        }
        return Boolean.FALSE;
    };

    String handleCommitMessage(String message) {
        if (issueKey != null && bugTracker.url != null) {
            var m = issueKey.matcher(message);
            if (m.find()) {
                return m.replaceFirst("link:" + bugTracker.url + "$0[$0]");
            }
        }
        return message;
    }


    String versionStringAdoc(String version, String date, boolean collapsibleBlock) {
        return (collapsibleBlock ? "" : "=== ") + "Version : " + version + " (" + date + ")" + (collapsibleBlock ? "\n" : "");
    }

    String typeStringAdoc(boolean collapsibleBlock) {
        return (collapsibleBlock ? "* " : "==== ");
    }

    String commitStringAdoc(boolean collapsibleBlock) {
        return (collapsibleBlock ? "** " : "- ");
    }

    public record ReleasedVersion(String version, String date) {
    }

    public record ReleaseNoteForVersion(ReleasedVersion releasedVersion, List<RevCommit> commits) {
    }

    public record ToDisplay(ReleaseNoteForVersion releaseNoteForVersion, Map<String, List<String>> types) {
    }


}
