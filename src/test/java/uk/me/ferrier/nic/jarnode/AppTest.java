package uk.me.ferrier.nic.jarnode;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import java.util.Arrays;

import org.json.simple.JSONObject;

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

    public void testResourcesFiles() throws Exception {
        String jarPath = new File("src/test/resources/demojar.jar")
                .getCanonicalPath();
        File extractJar = App.extractJar(jarPath);
        App.copyResourcesFiles(extractJar);
        File confFilePath = new File("src/test/resources/resources_file.txt");
        boolean confFileExists = confFilePath.exists();
        assertTrue( confFileExists );
    }

    public void testGetPackageJsonObject() throws Exception {
        String jarPath = new File("src/test/resources/demojar.jar")
                .getCanonicalPath();
        File extractJar = App.extractJar(jarPath);
        File jarPathDir = new File(jarPath).getParentFile();
        File exePath = new File(jarPathDir, ".demojar.jar");
        JSONObject packageJson = App.getPackageJsonObject(exePath);
        assertTrue( packageJson != null );
    }

    public void testNodeEntryPointFilenames() throws Exception {
        // when there is no package.json
        String result1 = App.getNodeEntryPointFilename(null);
        assertTrue( result1.equals("server.js") );

        // when there is a package.json with no main
        JSONObject fakePackageJson = new JSONObject();
        String result2 = App.getNodeEntryPointFilename(fakePackageJson);
        assertTrue( result2.equals("server.js") );

        // when there is a package.json with a main
        fakePackageJson.put("main", "./entry.js");
        String result3 = App.getNodeEntryPointFilename(fakePackageJson);
        assertTrue( result3.equals("./entry.js") );
    }

    public void testNodeMemoryLimitArguments() throws Exception {
        // when there is no jarnode config
        String result1 = App.getNodeMemoryLimitArg(null);
        assertTrue( result1.equals("--max-old-space-size=512") );

        // when there is jarnode config with no memoryLimit set
        JSONObject fakePackageJson = new JSONObject();
        String result2 = App.getNodeMemoryLimitArg(fakePackageJson);
        assertTrue( result2.equals("--max-old-space-size=512") );

        // when there is jarnode config with memoryLimit set
        fakePackageJson.put("memoryLimit", "64");
        String result3 = App.getNodeMemoryLimitArg(fakePackageJson);
        assertTrue( result3.equals("--max-old-space-size=64") );
    }
}
