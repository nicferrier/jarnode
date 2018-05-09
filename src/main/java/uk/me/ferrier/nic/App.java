package uk.me.ferrier.nic;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;
import java.io.IOException;

/**
 * Boot node apps by reading them out of the uberjar.
 *
 */
public class App
{
    public static void main( String[] args ) throws IOException
    {
        String path = App.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath();
        JarFile jar = new JarFile(path);
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            System.out.println("entry " + entry.getName()
                               + " " + entry.isDirectory());
        }
    }
}
