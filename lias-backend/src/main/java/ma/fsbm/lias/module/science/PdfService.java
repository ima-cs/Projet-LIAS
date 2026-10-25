package ma.fsbm.lias.module.science;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;

/** Small self-contained PDF writer: text pages, correct byte offsets and paginated content. */
@Service
public class PdfService {

  public byte[] create(List<String> lines) {
    try {
      List<String> wrapped = new ArrayList<>();
      for (String line : lines) {
        var s = line == null ? "" : line;
        while (s.length() > 95) {
          wrapped.add(s.substring(0, 95));
          s = s.substring(95);
        }
        wrapped.add(s);
      }
      int pages = Math.max(1, (wrapped.size() + 47) / 48);
      List<String> objects = new ArrayList<>();
      objects.add("<< /Type /Catalog /Pages 2 0 R >>");
      StringBuilder kids = new StringBuilder();
      for (int i = 0; i < pages; i++) kids.append(4 + i * 2).append(" 0 R ");
      objects.add(
        "<< /Type /Pages /Kids [" + kids + "] /Count " + pages + " >>"
      );
      objects.add(
        "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"
      );
      for (int i = 0; i < pages; i++) {
        int content = 5 + i * 2;
        objects.add(
          "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents " +
            content +
            " 0 R >>"
        );
        StringBuilder stream = new StringBuilder(
          "BT /F1 10 Tf 45 795 Td 15 TL\n"
        );
        for (var line : wrapped.subList(
          i * 48,
          Math.min(wrapped.size(), (i + 1) * 48)
        ))
          stream
            .append('(')
            .append(
              line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
            )
            .append(") Tj T*\n");
        stream.append("ET");
        objects.add(
          "<< /Length " +
            stream.toString().getBytes(StandardCharsets.ISO_8859_1).length +
            " >>\nstream\n" +
            stream +
            "\nendstream"
        );
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
      List<Integer> offsets = new ArrayList<>();
      for (int i = 0; i < objects.size(); i++) {
        offsets.add(out.size());
        out.write(
          (i + 1 + " 0 obj\n" + objects.get(i) + "\nendobj\n").getBytes(
            StandardCharsets.ISO_8859_1
          )
        );
      }
      int xref = out.size();
      out.write(
        (
          "xref\n0 " +
          (objects.size() + 1) +
          "\n0000000000 65535 f \n"
        ).getBytes(StandardCharsets.ISO_8859_1)
      );
      for (int off : offsets)
        out.write(
          String.format(
            java.util.Locale.ROOT,
            "%010d 00000 n \n",
            off
          ).getBytes(StandardCharsets.ISO_8859_1)
        );
      out.write(
        (
          "trailer\n<< /Size " +
          (objects.size() + 1) +
          " /Root 1 0 R >>\nstartxref\n" +
          xref +
          "\n%%EOF"
        ).getBytes(StandardCharsets.ISO_8859_1)
      );
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
