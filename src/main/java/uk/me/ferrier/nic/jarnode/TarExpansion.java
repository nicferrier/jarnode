package uk.me.ferrier.nic.jarnode;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.tukaani.xz.XZInputStream;
import org.kamranzafar.jtar.TarInputStream;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarHeader;


public class TarExpansion {
    public static File expandTar(InputStream tarXzIn, String destDir) throws Exception {
        InputStream i2 = new XZInputStream(tarXzIn);
        InputStream i3 = new BufferedInputStream(i2);
        TarInputStream tis = new TarInputStream(i3);
        TarEntry entry = tis.getNextEntry();

        String topEntryName = entry.getName();
        File top = new File(destDir, topEntryName);
        if (top.exists()) return top;
        
        // Now the rest of the tar
        while((entry = tis.getNextEntry()) != null) {
            String entryName = entry.getName();
            File f = new File(destDir, entryName);
            boolean isCreated = f.getParentFile().mkdirs();

            if (!entry.isDirectory() && !f.getName().equals(".")) {
                FileOutputStream fos = new FileOutputStream(f);
                BufferedOutputStream dest = new BufferedOutputStream(fos);

                int count;
                byte data[] = new byte[2048];
                while((count = tis.read(data)) != -1) {
                    dest.write(data, 0, count);
                }
                
                dest.flush();
                dest.close();

                int mode = entry.getHeader().mode;
                boolean userExecute = (mode & 0700) == 0700;
                boolean groupExecute = (mode & 070) == 070;
                boolean otherExecute = (mode & 07) == 07;

                Path path = f.toPath();
                Set<PosixFilePermission> set = Files.getPosixFilePermissions(path);
                if (userExecute) set.add(PosixFilePermission.OWNER_EXECUTE);
                if (groupExecute) set.add(PosixFilePermission.GROUP_EXECUTE);
                if (otherExecute) set.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(path, set);
            }
        }
  
        tis.close();
        return top;
    }
}
