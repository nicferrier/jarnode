package uk.me.ferrier.nic.jarnode;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Comparator;
import java.util.Arrays;

/**
 * Unit test for simple App.
 */
public class TarTest extends TestCase
{
    public TarTest(String testName)
    {
        super( testName );
    }

    public static Test suite()
    {
        return new TestSuite(TarTest.class);
    }

    /**
     * Test the tar unpacking
     */
    public void testApp() throws Exception
    {
        File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if(!(temp.delete())) throw new IOException("can't delete the temp file");
        if(!(temp.mkdir())) throw new IOException("can't make the directory");

        try {
            String nodePath = "node-v10.8.0-linux-x64.tar.xz";
            InputStream in = this.getClass().getResourceAsStream("/" + nodePath);
            TarExpansion.expandTar(in, temp.getCanonicalPath());

            String openSSLPath = "node-v10.8.0-linux-x64/include/node/openssl/archs/VC-WIN32/asm/include/progs.h";
            String packageStreamJs = "node-v10.8.0-linux-x64/lib/node_modules/npm/lib/search/format-package-stream.js";
            String nodeBin = "node-v10.8.0-linux-x64/bin/node";

            assertTrue(new File(temp, openSSLPath).exists());
            assertTrue(new File(temp, packageStreamJs).exists());
            assertTrue(new File(temp, nodeBin).exists());
        }
        finally {
            Path rootPath = temp.toPath();
            Files.walk(rootPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

}
