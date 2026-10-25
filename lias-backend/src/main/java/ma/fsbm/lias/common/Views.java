package ma.fsbm.lias.common;

import java.util.*;

public final class Views {

  private Views() {}

  public static Map<String, Object> of(Object... pairs) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) if (pairs[i + 1] != null) m.put(
      pairs[i].toString(),
      pairs[i + 1]
    );
    return m;
  }
}
