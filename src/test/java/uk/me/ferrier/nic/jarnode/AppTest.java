package uk.me.ferrier.nic.jarnode;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;

import java.util.Comparator;
import java.util.Arrays;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Test the file creation and destruction logic.
     */

    public void testApp() throws IOException
    {
        String targetJar = new File("./somejar.jar").getCanonicalPath();

        // Make fake path
        File f = App.getTargetDir(targetJar);
        File path = new File(new File(f, "level1"), "level2");
        path.mkdirs();

        File targetDir = App.rmTargetDir(targetJar);
        assertTrue( !targetDir.exists() ); // should have been rm'd

        targetDir = App.makeTargetDir(targetJar);
        assertTrue( targetDir.exists() );
    }

    public void testExtract() throws Exception {
        String jarPath = new File("src/test/resources/demojar.jar")
            .getCanonicalPath();
        File extractJar = App.extractJar(jarPath);
        File[] list = extractJar.listFiles();
        // for (File f : list) System.out.println("list member " + f);
        File jarPathDir = new File(jarPath).getParentFile();
        File serverJsPath = new File(jarPathDir, ".demojar.jar/server.js");
        // System.out.println("serverJsPath " + serverJsPath);
        assertTrue( Arrays
                    .asList(list)
                    .contains(serverJsPath) );
    }

    public void testPerms() throws Exception {
        String jarPath = new File("src/test/resources/demojar.jar")
            .getCanonicalPath();
        File extractJar = App.extractJar(jarPath);
        App.fixPerms(extractJar);

        String testExeFile = "node_modules/mkdirp/bin/cmd.js";
        File jarPathDir = new File(jarPath).getParentFile();
        File exePath = new File(jarPathDir, ".demojar.jar/" + testExeFile);
        boolean isExe = exePath.canExecute();
        assertTrue( isExe );
    }

    public void testResourcesFiles() throws Exception {
        String jarPath = new File("src/test/resources/demojar.jar")
                .getCanonicalPath();
        File extractJar = App.extractJar(jarPath);
        App.copyResourcesFiles(extractJar);
        File confFilePath = new File("src/test/resources/resources_file.txt");
        boolean confFileExists = confFilePath.exists();
        assertTrue( confFileExists );
    }

    public void testNodeDist() throws Exception {
        File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if(!(temp.delete())) throw new IOException("can't delete the temp file");
        if(!(temp.mkdir())) throw new IOException("can't make the directory");

        try {
            // #1 - Make a directory with a node dist in it, like in an artifact

            File dists = new File(temp, ".node-dists");
            dists.mkdirs();

            // #2 - Copy the nodejs binary to there, , as if it came from an artifact
            String nodePath = "node-v10.8.0-linux-x64.tar.xz";
            InputStream in = this.getClass().getResourceAsStream("/" + nodePath);
            
            File distFile = new File(dists, nodePath);
            FileOutputStream dest = new FileOutputStream(distFile);
            
            int count;
            byte data[] = new byte[2048];
            while((count = in.read(data)) != -1) {
                dest.write(data, 0, count);
            }
            dest.close();

            // #3 - Make a directory looking like a system's node-dists directory

            File nodeDists = File.createTempFile("node-dists", Long.toString(System.nanoTime()));
            if(!(nodeDists.delete())) throw new IOException("can't delete the temp file");
            if(!(nodeDists.mkdir())) throw new IOException("can't make the directory");

            try {
                // #4 - Set the NODE DISTS variable to the one we just made
                App.NODE_DISTS_ENV = nodeDists.getCanonicalPath();

                // #5 - Do the App - this should install node in the NODE DISTS dir
                File newDist = App.expandNodeDist(temp);

                // General tests about the state of that
                assertTrue(newDist != null);
                assertTrue(newDist.exists());
                assertTrue(newDist.isDirectory());

                // These are more specific tests
                String openSSLPath = "include/node/openssl/archs/VC-WIN32/asm/include/progs.h";
                String packageStreamJs = "lib/node_modules/npm/lib/search/format-package-stream.js";
                String nodeBin = "bin/node";
                
                assertTrue(new File(newDist, openSSLPath).exists());
                assertTrue(new File(newDist, packageStreamJs).exists());
                assertTrue(new File(newDist, nodeBin).exists());
            }
            finally {
                Files.walk(nodeDists.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
        finally {
            Files.walk(temp.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}
