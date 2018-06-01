package uk.me.ferrier.nic.jarnode;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Boot node apps by reading them out of the uberjar.
 *
 */
public class App
{
    static class Entry {
        List<String> list;
        String name;

        Entry(String name) {
            this.list = Arrays.asList(new File(name).list());
            this.name = name;
        }
    }

    static File getTargetDir(String sourceJarPath) {
        Path sourceJar = FileSystems.getDefault().getPath(sourceJarPath);
        Path dir = sourceJar.getParent();
        Path name = sourceJar.getFileName();
        File f = new File(dir.toFile(), "." + name);
        return f;
    }

    static File rmTargetDir(String sourceJarPath) {
        File pathName = getTargetDir(sourceJarPath);
        Path directory = FileSystems.getDefault().getPath(pathName.toString());
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
        }
        catch (IOException e) {
            // swallow
        }
        return directory.toFile();
    }

    static File makeTargetDir(String sourceJarPath) {
        File directory = rmTargetDir(sourceJarPath);
        boolean created = directory.mkdirs();
        return directory;
    }

    static File extractJar(String path) throws IOException {
        File tempDir = makeTargetDir(path);
        JarFile jar = new JarFile(path);
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (!entryName.startsWith("META-INF/")) {
                File f = new File(tempDir, entryName);
                if (entry.isDirectory()) {
                    f.mkdir();
                }
                else {
                    FileOutputStream fout = new FileOutputStream(f);
                    InputStream in = jar.getInputStream(entry);
                    byte[] buf = new byte[5000];
                    int red = in.read(buf);
                    while (red > -1) {
                        fout.write(buf, 0, red);
                        red = in.read(buf, 0, 5000);
                    }
                    fout.close();
                }
            }
        }
        return tempDir;
    }

    static void fixPerms(File jarDir) throws IOException {
        // If there is a list of exe files then we must apply that
        File exeList = new File(jarDir, ".exethings");
        if (exeList.exists()) {
            char[] buf = new char[(int)exeList.length()];
            new FileReader(exeList).read(buf);
            String[] list = new String(buf).split("\n");
            for (String fileName : list) {
                File file = new File(jarDir, fileName.trim());
                file.setExecutable(true);
            }
        }
    }

    public static void main(String[] args) throws IOException
    {
        String path = App.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath();

        File nodeAppJarDir = extractJar(path);
        fixPerms(nodeAppJarDir);

        // Find node in the PATH
        String OS = System.getProperty("os.name");
        boolean isWin = OS.startsWith("Windows");

        String pathVar = System.getenv().get("PATH");
        String[] pathParts = pathVar.split(File.pathSeparator);
        List<String> pathPartList = Arrays.asList(pathParts);
        List<Entry> nodePath = pathPartList.stream()
            .filter(p -> new File(p).exists())
            .map(p -> new Entry(p))
            .filter(e -> e.list.contains("node")
                    || (isWin && e.list.contains("node.exe")))
            .collect(Collectors.toList());

        if (nodePath.size() < 1) {
            System.err.println("node executable not found");
            System.exit(1);
        }
        String nodeExe = nodePath.get(0).name + "/node";
        nodeExe = isWin? nodeExe + ".exe" : nodeExe;
        ProcessBuilder builder = new ProcessBuilder(nodeExe, "server.js");
        builder.directory(nodeAppJarDir);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) { break; }
            System.out.println(line);
        }
    }
}
