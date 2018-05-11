package uk.me.ferrier.nic.jarnode;

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
    public static void main( String[] args ) throws IOException
    {
        File temp = File.createTempFile("nodeapp", Long.toString(System.nanoTime()));

        if(! (temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if(! (temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        System.out.println("nodeapp - " + temp.getAbsolutePath());

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
                File f = new File(temp, entryName);
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

        String nodeExe = "/home/nicferrier/.nvm/versions/node/v9.11.1/bin/node";
        ProcessBuilder builder = new ProcessBuilder(nodeExe, "server.js");
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
