package uk.me.ferrier.nic.jarnode;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import java.util.Arrays;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
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
}
