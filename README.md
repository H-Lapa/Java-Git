# Git-Clone (Minimal Git Implementation)

This project is an implementation of a subset of Git's core commands in Java. It simulates basic git operations such as initializing a repository, creating blobs, trees, commits, and simulating some essential Git functionalities like hashing objects, writing trees, and more.

## Features Implemented

- **init**: Initializes a Git repository by creating the necessary `.git` directory and structure (e.g., objects, refs).
- **hash-object**: Generates the SHA-1 hash for a file, and optionally writes the file's content as a Git blob to the `.git/objects` directory.
- **cat-file**: Reads and decompresses a file from the `.git/objects` directory by its SHA-1 hash and outputs its content.
- **ls-tree**: Lists the contents of a tree object by parsing and printing the file names stored within the tree.
- **write-tree**: Recursively writes the state of the current directory as a Git tree object.
- **commit-tree**: Creates a commit object that links to a tree and optionally to a parent commit, storing the commit metadata and message.

## Commands

### `init`
Initializes a new `.git` repository in the current directory.

```bash
java Main init
```

### `hash-object`
Generates a SHA-1 hash for the specified file. With the `-w` flag, it also writes the blob object to `.git/objects`.

```bash
# Print the SHA-1 hash
java Main hash-object <file-path>

# Write the object to .git/objects and print the SHA-1 hash
java Main hash-object -w <file-path>
```

### `cat-file`
Decompresses and displays the content of a stored blob file using its SHA-1 hash.

```bash
java Main cat-file -p <hash>
```

### `ls-tree`
Lists the contents of a tree object based on its SHA-1 hash.

```bash
java Main ls-tree <tree-sha>
```

### `write-tree`
Writes the current directory's contents as a tree object and prints the tree's SHA-1 hash.

```bash
java Main write-tree
```

### `commit-tree`
Creates a commit object with the specified tree and parent commit SHA-1 hashes, and a commit message.

```bash
java Main commit-tree <tree-sha> -p <parent-commit-sha> -m "<commit-message>"
```

## Installation

To run this project, you need to have Java installed on your system. Clone the repository and compile the `Main.java` file:

```bash
git clone https://github.com/<your-username>/<repository-name>.git
cd <repository-name>
javac Main.java
```

You can now run the different git commands using the compiled `Main` class:

```bash
java Main <command> [arguments]
```

## Testing

This project includes a simple test suite covering all implemented Git commands.

### Running Tests

```bash
mvn test
```

### Test Coverage

The test suite (`GitCommandsTest.java`) includes **10 tests** covering:
- **init** - Creates `.git` directory, objects, refs, and HEAD file
- **hash-object** - Computes SHA-1 hash and writes blobs
- **cat-file** - Reads blob content
- **write-tree** - Creates tree objects
- **ls-tree** - Lists tree contents
- **commit-tree** - Creates commit objects
- **Error handling** - Unknown commands

## How It Works

This project works similarly to how Git operates internally:

- **Blob Objects**: Files are compressed and hashed using the SHA-1 algorithm and stored in the `.git/objects` directory.
- **Tree Objects**: The structure of directories is stored as tree objects. Each tree references blobs or subtrees by their SHA-1 hashes.
- **Commit Objects**: Commits link to tree objects (representing the state of the repository) and optionally to a parent commit. Metadata like author, committer, and timestamp are stored alongside the commit message.

## Future Improvements

- Implement git clone using Git's Smart HTTP transfer protocol
- Implement branching and merging capabilities
- Add support for tracking multiple branches and the concept of `HEAD`

## Resources

- [What is in that .git directory?](https://blog.meain.io/2023/what-is-in-dot-git/)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

