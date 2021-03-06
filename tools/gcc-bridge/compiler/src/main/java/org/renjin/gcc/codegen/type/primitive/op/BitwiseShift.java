package org.renjin.gcc.codegen.type.primitive.op;

import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.expr.JExpr;
import org.renjin.gcc.gimple.GimpleOp;
import org.renjin.gcc.gimple.type.GimpleIntegerType;
import org.renjin.gcc.gimple.type.GimpleType;
import org.renjin.repackaged.asm.Type;

import javax.annotation.Nonnull;

/**
 * Generates bytecode for left and right bitwise shifts
 */
public class BitwiseShift implements JExpr {

  private final GimpleOp op;
  private GimpleIntegerType type;
  private final JExpr x;
  private final JExpr y;

  public BitwiseShift(GimpleOp op, GimpleType type, JExpr x, JExpr y) {
    this.op = op;
    this.type = (GimpleIntegerType) type;
    this.x = x;
    this.y = y;

//    if(!checkTypes()) {
//      throw new UnsupportedOperationException("Shift operations require types (int, int) or (long, int), found: " +
//          this.x.getType() + ", " + this.y.getType());
//    }
  }

  private boolean checkTypes() {
    Type tx = x.getType();
    Type ty = y.getType();

    return (tx.equals(Type.INT_TYPE) || tx.equals(Type.LONG_TYPE)) && ty.equals(tx);
  }

  @Nonnull
  @Override
  public Type getType() {
    return x.getType();
  }

  @Override
  public void load(@Nonnull MethodGenerator mv) {
    x.load(mv);
    y.load(mv);

    Type type = x.getType();

    switch (op) {
      case LSHIFT_EXPR:
        // Shifting left has (bitwise) the same result on signed and unsigned integers
        mv.shl(type);
        break;

      case RSHIFT_EXPR:
        // For right shifts, we need to consider whether the type is signed
        if(this.type.isUnsigned()) {
          mv.ushr(type);

        } else {
          mv.shr(type);
        }
        break;
      
      default:
        throw new UnsupportedOperationException("Op: " + op);
    }
  }
}
