/* (C) Arnaud 'red' Rouyer 2026 */
package tablestring;

public final class TableStringDecoder {
  private TableStringDecoder() {}

  public static String decode(byte[] bytes, TblParser.TblTable table) {
    return decode(bytes, table, DecodeOptions.defaults());
  }

  public static String decode(byte[] bytes, TblParser.TblTable table, DecodeOptions options) {
    StringBuilder result = new StringBuilder();
    int offset = 0;

    while (offset < bytes.length) {
      TblParser.TblEntry match = findMatch(bytes, offset, table);

      if (match != null) {
        result.append(match.getValue());
        offset += match.getKey().length;
        continue;
      }

      appendUnknown(result, bytes[offset], options);
      offset++;
    }

    return result.toString();
  }

  private static TblParser.TblEntry findMatch(byte[] bytes, int offset, TblParser.TblTable table) {
    for (TblParser.TblEntry entry : table.getLongestFirstEntries()) {
      byte[] key = entry.getKey();

      if (offset + key.length > bytes.length) {
        continue;
      }

      boolean matches = true;
      for (int i = 0; i < key.length; i++) {
        if (bytes[offset + i] != key[i]) {
          matches = false;
          break;
        }
      }

      if (matches) {
        return entry;
      }
    }

    return null;
  }

  private static void appendUnknown(StringBuilder result, byte value, DecodeOptions options) {
    int unsigned = value & 0xff;

    switch (options.unknownMode) {
      case HEX_ANGLE:
        result.append(String.format("<%02X>", unsigned));
        break;
      case DOT:
        result.append('.');
        break;
      case QUESTION_MARK:
        result.append('?');
        break;
      default:
        result.append(String.format("<%02X>", unsigned));
        break;
    }
  }

  public static final class DecodeOptions {
    private final UnknownMode unknownMode;

    private DecodeOptions(UnknownMode unknownMode) {
      this.unknownMode = unknownMode;
    }

    public static DecodeOptions defaults() {
      return new DecodeOptions(UnknownMode.HEX_ANGLE);
    }

    public static DecodeOptions unknownMode(UnknownMode unknownMode) {
      return new DecodeOptions(unknownMode);
    }
  }

  public enum UnknownMode {
    HEX_ANGLE,
    DOT,
    QUESTION_MARK
  }
}
