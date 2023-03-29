import org.springframework.core.io.ClassPathResource;
import sun.misc.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class LoadSo {

    public static void main(String[] args) throws IOException {

        String property = System.getProperty("java.library.path");
        System.out.println(property);


        ClassPathResource libMpc = new ClassPathResource("libmpc_crypto.so");
        InputStream inputStream = libMpc.getInputStream();

        File tmpFile = File.createTempFile("libmpc_crypto", ".so");
        tmpFile.deleteOnExit();

        try (OutputStream outputStream = Files.newOutputStream(tmpFile.toPath())) {
            byte[] buffer = IOUtils.readAllBytes(inputStream);
            outputStream.write(buffer);
        }

        System.load(tmpFile.getAbsolutePath());
    }
}
