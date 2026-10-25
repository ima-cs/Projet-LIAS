package ma.fsbm.lias.module.science;

import java.io.*;
import org.springframework.web.multipart.MultipartFile;

public record GeneratedFile(
  String name,
  byte[] bytes
) implements MultipartFile {
  public String getName() {
    return "file";
  }

  public String getOriginalFilename() {
    return name;
  }

  public String getContentType() {
    return "application/pdf";
  }

  public boolean isEmpty() {
    return bytes.length == 0;
  }

  public long getSize() {
    return bytes.length;
  }

  public byte[] getBytes() {
    return bytes;
  }

  public InputStream getInputStream() {
    return new ByteArrayInputStream(bytes);
  }

  public void transferTo(File f) throws IOException {
    java.nio.file.Files.write(f.toPath(), bytes);
  }
}
