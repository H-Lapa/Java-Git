import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GitCommandsTest {

    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private String executeCommand(String... args) {
        outputStream.reset();
        Main.main(args);
        return outputStream.toString().trim();
    }

    // Init command tests
    @Test
    @DisplayName("init creates .git directory")
    void testInit() {
        String output = executeCommand("init");
        assertThat(output).isEqualTo("Initialized git directory");
        assertThat(new File(".git")).exists();
    }

    @Test
    @DisplayName("init creates objects and refs directories")
    void testInitCreatesDirectories() {
        executeCommand("init");
        assertThat(new File(".git/objects")).exists().isDirectory();
        assertThat(new File(".git/refs")).exists().isDirectory();
    }

    @Test
    @DisplayName("init creates HEAD file")
    void testInitCreatesHead() throws IOException {
        executeCommand("init");
        File head = new File(".git/HEAD");
        assertThat(head).exists();
        String content = Files.readString(head.toPath());
        assertThat(content).isEqualTo("ref: refs/heads/main\n");
    }

    // Hash-object tests
    @Test
    @DisplayName("hash-object computes SHA-1 hash")
    void testHashObject() throws IOException {
        executeCommand("init");

        Path testFile = Path.of("test-file.txt");
        Files.writeString(testFile, "hello world");

        try {
            String output = executeCommand("hash-object", testFile.toString());
            assertThat(output).matches("[0-9a-f]{40}");
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    @Test
    @DisplayName("hash-object -w writes blob")
    void testHashObjectWrite() throws IOException {
        executeCommand("init");

        Path testFile = Path.of("test-write.txt");
        Files.writeString(testFile, "test content");

        try {
            String hash = executeCommand("hash-object", "-w", testFile.toString());

            // Check blob exists
            File blobFile = new File(".git/objects/" + hash.substring(0, 2) + "/" + hash.substring(2));
            assertThat(blobFile).exists();
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    // Cat-file tests
    @Test
    @DisplayName("cat-file displays blob content")
    void testCatFile() throws IOException {
        executeCommand("init");

        Path testFile = Path.of("cat-test.txt");
        Files.writeString(testFile, "hello git");

        try {
            String hash = executeCommand("hash-object", "-w", testFile.toString());
            String content = executeCommand("cat-file", "-p", hash);
            assertThat(content).isEqualTo("hello git");
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    // Write-tree tests
    @Test
    @DisplayName("write-tree creates tree hash")
    void testWriteTree() throws IOException {
        executeCommand("init");

        Path testFile = Path.of("tree-test.txt");
        Files.writeString(testFile, "content");

        try {
            String treeHash = executeCommand("write-tree");
            assertThat(treeHash).matches("[0-9a-f]{40}");
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    // Ls-tree tests
    @Test
    @DisplayName("ls-tree lists tree contents")
    void testLsTree() throws IOException {
        executeCommand("init");

        Path file1 = Path.of("ls-file1.txt");
        Path file2 = Path.of("ls-file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        try {
            String treeHash = executeCommand("write-tree");
            String output = executeCommand("ls-tree", "--name-only", treeHash);

            assertThat(output).contains("ls-file1.txt", "ls-file2.txt");
        } finally {
            Files.deleteIfExists(file1);
            Files.deleteIfExists(file2);
        }
    }

    // Commit-tree tests
    @Test
    @DisplayName("commit-tree creates commit")
    void testCommitTree() throws IOException {
        executeCommand("init");

        Path testFile = Path.of("commit-test.txt");
        Files.writeString(testFile, "commit content");

        try {
            String treeHash = executeCommand("write-tree");
            String commitHash = executeCommand("commit-tree", treeHash,
                "-p", "0000000000000000000000000000000000000000",
                "-m", "Test commit");

            assertThat(commitHash).matches("[0-9a-f]{40}");
        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    // Unknown command test
    @Test
    @DisplayName("unknown command shows error")
    void testUnknownCommand() {
        String output = executeCommand("unknown");
        assertThat(output).contains("Unknown command: unknown");
    }
}
