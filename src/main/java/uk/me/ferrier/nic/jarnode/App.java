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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FileUtils;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;


/**
 * Boot node apps by reading them out of the uberjar.
 *
 */
public class App
{

    static boolean isDebug() {
        String envVar = System.getenv("JARNODE_DEBUG");
        return "true".equals(envVar)
            || "true".equals(System.getProperty("debug"))
            || "true".equals(System.getProperty("DEBUG"));
    }
    
    static boolean DEBUG = isDebug();

    static class Entry {
        List<String> list;
        String name;

        Entry(String name) {
            this.list = Arrays.asList(new File(name).list());
            this.name = name;
        }
    }

    static File getTargetDir(String sourceJarPath) {
        FileSystem fs = FileSystems.getDefault();
        if (System.getProperty("os.name").startsWith("Windows")
            && sourceJarPath.startsWith("/")
            && sourceJarPath.charAt(2) == ':') {
            sourceJarPath = sourceJarPath.substring(1);
        }
        Path sourceJar = fs.getPath(sourceJarPath);
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

    static void copyResourcesFiles(File jarDir) throws IOException {
        File toTransfer = new File(jarDir.getPath(), ".resourcesthings");
        if (toTransfer.exists()) {
            File targetDir = new File(jarDir.getParentFile().getPath());
            try {
                FileUtils.copyDirectory(toTransfer, targetDir);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static JSONObject getPackageJsonObject(File jarDir) {
        try {
            JSONParser parser = new JSONParser();
            FileReader jsonFile = new FileReader(jarDir.getPath() + "/package.json");
            JSONObject json = (JSONObject) parser.parse(jsonFile);
            return json;
        }
        catch (IOException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    static JSONObject getJarnodeJsonObject(JSONObject packageJson) {
        if(packageJson == null){
            return null;
        }
        return (JSONObject) packageJson.get("jarnode");
    }

    static String getNodeEntryPointFilename(JSONObject packageJson) {
        String defaultFilename = "server.js";
        String customFilename = packageJson != null ? (String) packageJson.get("main") : null;
        String fileName = (customFilename != null && !customFilename.isEmpty()) ? customFilename : defaultFilename;
        return fileName;
    }

    static String getNodeMemoryLimitArg(JSONObject jarnodeConfig) {
        String defaultLimit = "512"; // 512MB is node default
        String customLimit = jarnodeConfig != null ? (String) jarnodeConfig.get("memoryLimit") : null;
        String memLimit = (customLimit != null && !customLimit.isEmpty()) ? customLimit : defaultLimit;
        return "--max-old-space-size=" + memLimit;
    }

    /*

      support python

      instead of execing python we have to generate a script and exec that in a shell - this is so venvs work:

source .venv/bin/activate
python t.py

      and then the flow looks something like this:

mkdir demojarplace
cp demoapp/demo.jar demojarplace
cd demojarplace
jar xvf demo.jar
cp ~/script .
bash script

     */

    // Find node in the PATH
    static String OS = System.getProperty("os.name");
    static boolean isWin = OS.startsWith("Windows");
    static String NODE_DISTS_ENV = System.getenv("NODE_DISTS");

    static String pathFind() {
        String pathVar = System
            .getenv()
            .get(isWin ? "Path" : "PATH");
        String[] pathParts = pathVar.split(File.pathSeparator);
        List<String> pathPartList = Arrays.asList(pathParts);
        List<Entry> nodePath = pathPartList.stream()
            .filter(p -> new File(p).exists())
            .map(p -> new Entry(p))
            .filter(e -> e.list.contains("node")
                    || (isWin && e.list.contains("node.exe")))
            .collect(Collectors.toList());

        if (nodePath.size() < 1) {
            System.err.println("jarnode - node executable not found");
            System.exit(1);
        }
        String nodeExe = nodePath.get(0).name + "/node";
        return nodeExe;
    }
    
    static File expandNodeDist(File nodeAppJarDir) throws Exception {
        String OS = System.getProperty("os.name");
        boolean isWin = OS.startsWith("Windows");

        if (DEBUG) {
            System.out.println("jarnode - "
                               + "nodeAppJarDir: " + nodeAppJarDir
                               + " OS: " + OS
                               + " is Windows? " + isWin);
        }

        File nodeDistDir =  new File(nodeAppJarDir, ".node-dists");
        if (nodeDistDir.exists()) {
            if (DEBUG) {
                System.out.println("jarnode - nodeDistDir: " + nodeDistDir);
            }

            File[] ls = nodeDistDir.listFiles();
            for (File file : ls) {
                String baseFileName = file.getName();
                if (baseFileName.endsWith(".tar.xz")
                    && baseFileName.startsWith("node-")) {

                    if (DEBUG) {
                        System.out.println("jarnode - nodeDist: " + file);
                    }

                    File nodeDist = new File(NODE_DISTS_ENV);
                    if (nodeDist.exists()) {
                        FileInputStream fin = new FileInputStream(file);
                        File nodeVersion = TarExpansion.expandTar(fin, nodeDist.getCanonicalPath());
                        fin.close();
                        return nodeVersion;
                    }
                }
            }
        }
        // If we can't find a packaged node install return this
        return new File("/no-node");
    }

    public static void main(String[] args) throws Exception
    {
        String path = App.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath();

        File nodeAppJarDir = extractJar(path);
        fixPerms(nodeAppJarDir);
        copyResourcesFiles(nodeAppJarDir);

        JSONObject packageJson = getPackageJsonObject(nodeAppJarDir);
        JSONObject packageJsonJarnodeConfig = getJarnodeJsonObject(packageJson);

        // See if we can find a node dist and install it if so
        File nodeDist = expandNodeDist(nodeAppJarDir);
        String nodeExe = nodeDist.exists()
            ? new File(nodeDist, "bin/node").getCanonicalPath()
            : pathFind();

        // Fix the exe
        nodeExe = isWin? nodeExe + ".exe" : nodeExe;

        ProcessBuilder builder = new ProcessBuilder(
                nodeExe,
                getNodeEntryPointFilename(packageJson),
                getNodeMemoryLimitArg(packageJsonJarnodeConfig));

        builder.directory(nodeAppJarDir);
        builder.redirectErrorStream(true);
        final Process p = builder.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        p.destroy();
                    }
                    catch (Exception e) {
                        System.err.println("jarnode - error in shutdown hook");
                        System.err.print(e);
                    }
                }
            });

        InputStreamReader inr = new InputStreamReader(p.getInputStream());
        BufferedReader r = new BufferedReader(inr);
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) { break; }
            System.out.println(line);
        }
    }
}
