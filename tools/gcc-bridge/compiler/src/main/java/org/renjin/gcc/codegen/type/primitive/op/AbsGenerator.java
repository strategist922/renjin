package org.renjin.gcc.codegen.type.primitive.op;

import org.objectweb.asm.Type;
import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.var.Value;

import javax.annotation.Nonnull;


public class AbsGenerator implements Value {

  private Value x;

  public AbsGenerator(Value x) {
    this.x = x;
  }

  @Nonnull
  @Override
  public Type getType() {
    return x.getType();
  }

  @Override
  public void load(@Nonnull MethodGenerator mv) {
    x.load(mv);
    mv.invokestatic(Math.class, "abs", Type.getMethodDescriptor(x.getType(), x.getType()));
  }
}
