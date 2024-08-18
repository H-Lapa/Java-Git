import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.Arrays;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        final String command = args[0];

        switch (command) {
            case "init" -> {
                final File root = new File(".git");
                new File(root, "objects").mkdirs();
                new File(root, "refs").mkdirs();
                final File head = new File(root, "HEAD");

                try {
                    head.createNewFile();
                    Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
                    System.out.println("Initialized git directory");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case "cat-file" -> {
                final String hash = args[2];
                final File blobFile = new File("./.git/objects/" + hash.substring(0, 2) + "/" + hash.substring(2));

                // Decompress and output the contents of the file
                try {
                    String blob = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(blobFile)))).readLine();
                    String content = blob.substring(blob.indexOf("\0") + 1);
                    System.out.print(content);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            case "hash-object" -> {
                String path = null;
                boolean write = false;
                if (args[1].equals("-w")) {
                    path = args[2];
                    write = true;
                } else {
                    path = args[1];
                }

                File file = new File(path);
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));

                    String header = "blob " + content.length() + "\0" + content;

                    // Hashing
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    byte[] messageDigest = md.digest(header.getBytes());

                    StringBuilder hexString = new StringBuilder();
                    for (byte b : messageDigest) {
                        String hex = Integer.toHexString(0xff & b);
                        if (hex.length() == 1) hexString.append('0');
                        hexString.append(hex);
                    }

                    String hash = hexString.toString();
                    System.out.println(hash);

                    if (write) {
                        final File dir = new File("./.git/objects/", hash.substring(0, 2));
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }

                        File blobFile = new File(dir, hash.substring(2));

                        String blobContent = "blob " + content.length() + "\0" + content;
                        byte[] blobBytes = blobContent.getBytes(StandardCharsets.UTF_8);

                        // Compress the blob content
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
                            dos.write(blobBytes);
                        }

                        // Write the compressed blob content to the file
                        try (FileOutputStream fos = new FileOutputStream(blobFile)) {
                            fos.write(baos.toByteArray());
                        }
                    }

                    reader.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            case "ls-tree" -> {
                String tree_sha = null;
                boolean nameOnly = false;
                if (args[1].equals("--name-only")) {
                    tree_sha = args[2];
                    nameOnly = true;
                } else {
                    tree_sha = args[1];
                }
            
                // Read the tree object from .git/objects and decompress to find contents
                final File tree_file = new File("./.git/objects/" + tree_sha.substring(0, 2) + "/" + tree_sha.substring(2));
            
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (InflaterInputStream iis = new InflaterInputStream(new FileInputStream(tree_file))) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = iis.read(buffer)) > 0) {
                            baos.write(buffer, 0, len);
                        }
                    }
                    byte[] decompressedBytes = baos.toByteArray();
            
                    // Skip the header
                    int headerEnd = indexOf(decompressedBytes, (byte) 0);
                    if (headerEnd == -1) {
                        throw new RuntimeException("Invalid tree object format");
                    }
            
                    // Parse the decompressed tree
                    int i = headerEnd + 1;
                    while (i < decompressedBytes.length) {
                        // Find the next null byte
                        int nullIndex = indexOf(decompressedBytes, (byte) 0, i);
                        if (nullIndex == -1) {
                            break;
                        }
            
                        // Extract the mode and filename
                        String entry = new String(decompressedBytes, i, nullIndex - i, StandardCharsets.UTF_8);
                        String[] parts = entry.split(" ", 2);
                        if (parts.length == 2) {
                            String filename = parts[1];
                            System.out.println(filename);
                        }
            
                        // Skip the SHA-1 (20 bytes)
                        i = nullIndex + 21;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            case "write-tree" -> {
                try {
                    String treeHash = writeTree(new File("."));
                    System.out.println(treeHash);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            case "commit-tree" -> {
                String treeHash = args[1];
                String parentCommitHash = args[3];
                String message = args[5];
                try {
                    String commitHash = createCommit(treeHash, parentCommitHash, message);
                    System.out.println(commitHash);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            default -> System.out.println("Unknown command: " + command);
        }
    }

    // Recursive method to write tree objects
    private static String writeTree(File directory) throws Exception {
      ByteArrayOutputStream treeContent = new ByteArrayOutputStream();
  
      File[] files = directory.listFiles();
      if (files != null) {
          // Sort files and directories lexicographically by their names
          Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));
  
          for (File file : files) {
              if (file.getName().equals(".git")) continue;
  
              if (file.isFile()) {
                  String blobHash = generateBlobHash(file);
                  String mode = "100644"; // regular file mode
                  treeContent.write((mode + " " + file.getName() + "\0").getBytes(StandardCharsets.UTF_8));
                  treeContent.write(hexStringToByteArray(blobHash));
              } else if (file.isDirectory()) {
                  String subTreeHash = writeTree(file);
                  String mode = "40000"; // directory mode (removed leading zero)
                  treeContent.write((mode + " " + file.getName() + "\0").getBytes(StandardCharsets.UTF_8));
                  treeContent.write(hexStringToByteArray(subTreeHash));
              }
          }
      }
  
      byte[] treeContentBytes = treeContent.toByteArray();
      String treeHeader = "tree " + treeContentBytes.length + "\0";
      byte[] treeBytes = concatArrays(treeHeader.getBytes(StandardCharsets.UTF_8), treeContentBytes);
  
      String treeHash = bytesToHex(MessageDigest.getInstance("SHA-1").digest(treeBytes));
      writeObjectToGit(treeHash, treeBytes);
  
      return treeHash;
    }
  

    // Generate the blob hash and write the blob to .git/objects
    private static String generateBlobHash(File file) throws Exception {
      byte[] content = Files.readAllBytes(file.toPath());
      String header = "blob " + content.length + "\0";
      byte[] fullContent = concatArrays(header.getBytes(StandardCharsets.UTF_8), content);
  
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] sha1 = md.digest(fullContent);
      String blobHash = bytesToHex(sha1);
  
      // Write the blob object to .git/objects
      writeObjectToGit(blobHash, fullContent);
  
      return blobHash;
    }

    // Write the object to .git/objects directory
    private static void writeObjectToGit(String objectHash, byte[] objectContent) throws IOException {
      File dir = new File("./.git/objects/", objectHash.substring(0, 2));
      if (!dir.exists()) {
          dir.mkdirs();
      }
  
      File objectFile = new File(dir, objectHash.substring(2));
  
      if (!objectFile.exists()) {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          try (DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
              dos.write(objectContent);
          }
  
          try (FileOutputStream fos = new FileOutputStream(objectFile)) {
              fos.write(baos.toByteArray());
          }
      }
    }

    // Utility method to convert byte array to hex string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Utility method to convert hex string to byte array
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    // Utility method to concatenate two byte arrays
    private static byte[] concatArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static int indexOf(byte[] array, byte target) {
        return indexOf(array, target, 0);
    }
    
    private static int indexOf(byte[] array, byte target, int start) {
        for (int i = start; i < array.length; i++) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }

    private static String createCommit(String treeHash, String parentCommitHash, String message) throws Exception {
        // Hardcode author and committer information
        String author = "John Doe <john@example.com>";
        String committer = "John Doe <john@example.com>";
        long timestamp = System.currentTimeMillis() / 1000L;
        String timeZone = "+0000";
    
        // Create commit content
        StringBuilder commitContent = new StringBuilder();
        commitContent.append("tree ").append(treeHash).append("\n");
        commitContent.append("parent ").append(parentCommitHash).append("\n");
        commitContent.append("author ").append(author).append(" ").append(timestamp).append(" ").append(timeZone).append("\n");
        commitContent.append("committer ").append(committer).append(" ").append(timestamp).append(" ").append(timeZone).append("\n");
        commitContent.append("\n");
        commitContent.append(message).append("\n");
    
        // Create commit object
        String header = "commit " + commitContent.length() + "\0";
        byte[] commitBytes = concatArrays(header.getBytes(StandardCharsets.UTF_8), commitContent.toString().getBytes(StandardCharsets.UTF_8));
    
        // Generate commit hash
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1 = md.digest(commitBytes);
        String commitHash = bytesToHex(sha1);
    
        // Write commit object to .git/objects
        writeObjectToGit(commitHash, commitBytes);
    
        return commitHash;
    }
}
