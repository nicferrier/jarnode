package uk.me.ferrier.nic.jarnode;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    public static void main( String[] args ) throws IOException
    {
        File tempDir = File.createTempFile("nodeapp", Long.toString(System.nanoTime()));

        if(! (tempDir.delete())) {
            throw new IOException("Could not delete temp file: " + tempDir.getAbsolutePath());
        }

        if(! (tempDir.mkdir())) {
            throw new IOException("Could not create temp directory: " + tempDir.getAbsolutePath());
        }

        System.out.println("nodeapp - " + tempDir.getAbsolutePath());

        String path = App.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath();
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

        // Find node in the PATH
        String pathVar = System.getenv().get("PATH");
        String[] pathParts = pathVar.split(File.pathSeparator);
        List<String> pathPartList = Arrays.asList(pathParts);
        List<Entry> nodePath = pathPartList.stream()
            .filter(p -> new File(p).exists())
            .map(p -> new Entry(p))
            .filter(e -> e.list.contains("node"))
            .collect(Collectors.toList());

        if (nodePath.size() < 1) {
            System.err.println("node executable not found");
            System.exit(1);
        }
        String nodeExe = nodePath.get(0).name + "/node";
        ProcessBuilder builder = new ProcessBuilder(nodeExe, "server.js");
        builder.directory(tempDir);
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
