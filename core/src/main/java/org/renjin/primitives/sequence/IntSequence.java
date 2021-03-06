package org.renjin.primitives.sequence;


import org.renjin.sexp.AtomicVector;
import org.renjin.sexp.AttributeMap;
import org.renjin.sexp.IntVector;
import org.renjin.sexp.SEXP;

public class IntSequence extends IntVector {

  private int from;
  private int by;
  private int length;

  public IntSequence(int from, int by, int length) {
    this.from = from;
    this.by = by;
    this.length = length;
  }

  public IntSequence(AttributeMap attributes, int from, int by, int length) {
    super(attributes);
    this.from = from;
    this.by = by;
    this.length = length;
  }

  public int getFrom() {
    return from;
  }

  public int getBy() {
    return by;
  }

  public int getLength() {
    return length;
  }

  @Override
  public int length() {
    return length;
  }

  @Override
  public int getElementAsInt(int i) {
    return from + i*by;
  }

  @Override
  public boolean isElementNA(int index) {
    return false;
  }

  @Override
  public boolean isConstantAccessTime() {
    return true;
  }

  @Override
  protected SEXP cloneWithNewAttributes(AttributeMap attributes) {
    return new IntSequence(attributes, from, by, length);
  }

  public static AtomicVector fromTo(int n1, int n2) {
    if(n1 <= n2) {
      return new IntSequence(n1, 1, n2-n1+1);
    } else {
      return new IntSequence(n1, -1, n1-n2+1);
    }
  }

  public static AtomicVector fromTo(double n1, double n2) {
    return fromTo((int)n1, (int)n2);
  }
}
