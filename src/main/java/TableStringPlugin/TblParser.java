/* (C) Arnaud 'red' Rouyer 2026 */
package tablestring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class TblParser {
  private TblParser() {}

  public static TblTable parse(String name, Reader reader) throws IOException {
    List<TblEntry> entries = new ArrayList<>();

    try (BufferedReader bufferedReader = new BufferedReader(reader)) {
      String line;
      int lineNumber = 0;

      while ((line = bufferedReader.readLine()) != null) {
        lineNumber++;
        line = stripBom(line).trim();

        if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
          continue;
        }

        int separator = line.indexOf('=');
        if (separator < 0) {
          throw new IOException("Invalid .tbl line " + lineNumber + ": missing '='");
        }

        String hexPart = line.substring(0, separator).trim();
        String valuePart = line.substring(separator + 1);

        byte[] key = parseHexKey(hexPart, lineNumber);
        entries.add(new TblEntry(key, unescapeValue(valuePart)));
      }
    }

    if (entries.isEmpty()) {
      throw new IOException("Table is empty");
    }

    return new TblTable(name, entries);
  }

  private static String stripBom(String line) {
    if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
      return line.substring(1);
    }
    return line;
  }

  private static byte[] parseHexKey(String hex, int lineNumber) throws IOException {
    String normalized = hex.replaceAll("\\s+", "");

    if (normalized.isEmpty()) {
      throw new IOException("Invalid .tbl line " + lineNumber + ": empty key");
    }

    if ((normalized.length() % 2) != 0) {
      throw new IOException(
          "Invalid .tbl line " + lineNumber + ": hex key must have an even number of digits");
    }

    byte[] bytes = new byte[normalized.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      int offset = i * 2;
      String pair = normalized.substring(offset, offset + 2);

      try {
        bytes[i] = (byte) Integer.parseInt(pair, 16);
      } catch (NumberFormatException e) {
        throw new IOException(
            "Invalid .tbl line " + lineNumber + ": invalid hex byte '" + pair + "'", e);
      }
    }

    return bytes;
  }

  private static String unescapeValue(String value) {
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c != '\\' || i + 1 >= value.length()) {
        result.append(c);
        continue;
      }

      char next = value.charAt(++i);
      switch (next) {
        case 'n':
          result.append('\n');
          break;
        case 'r':
          result.append('\r');
          break;
        case 't':
          result.append('\t');
          break;
        case '\\':
          result.append('\\');
          break;
        default:
          result.append(next);
          break;
      }
    }

    return result.toString();
  }

  public static final class TblTable {
    private final String name;
    private final List<TblEntry> entries;
    private final List<TblEntry> longestFirstEntries;

    public TblTable(String name, List<TblEntry> entries) {
      this.name = name;
      this.entries = Collections.unmodifiableList(new ArrayList<>(entries));

      List<TblEntry> sorted = new ArrayList<>(entries);
      sorted.sort(Comparator.comparingInt((TblEntry entry) -> entry.key.length).reversed());
      this.longestFirstEntries = Collections.unmodifiableList(sorted);
    }

    public String getName() {
      return name;
    }

    public List<TblEntry> getEntries() {
      return entries;
    }

    public List<TblEntry> getLongestFirstEntries() {
      return longestFirstEntries;
    }
  }

  public static final class TblEntry {
    private final byte[] key;
    private final String value;

    public TblEntry(byte[] key, String value) {
      this.key = key.clone();
      this.value = value;
    }

    public byte[] getKey() {
      return key.clone();
    }

    public String getValue() {
      return value;
    }

    private boolean matches(byte[] bytes, int offset) {
      if (offset + key.length > bytes.length) {
        return false;
      }

      for (int i = 0; i < key.length; i++) {
        if (bytes[offset + i] != key[i]) {
          return false;
        }
      }

      return true;
    }
  }
}
