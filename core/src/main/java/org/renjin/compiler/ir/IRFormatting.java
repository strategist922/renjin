package org.renjin.compiler.ir;

public class IRFormatting {

  public static void appendSubscript(StringBuilder sb, int subscript) {
    String digits = Integer.toString(subscript);
    for(int i=0; i!=digits.length(); ++i) {
      int digit = digits.charAt(i) - '0';
      sb.appendCodePoint(0x2080 + digit);
    }
  }

  public static final String LEFT_ARROW = "←";
  
  public static final String LEFT_DOUBLE_ARROW = "⇐";

}
