package org.renjin.gcc.codegen.type;

import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.condition.ConditionGenerator;
import org.renjin.gcc.codegen.expr.GExpr;
import org.renjin.gcc.codegen.expr.JExpr;
import org.renjin.gcc.codegen.type.voidt.VoidPtr;
import org.renjin.gcc.gimple.GimpleOp;


public interface PointerTypeStrategy<ExprT extends GExpr> extends TypeStrategy<ExprT> {
  
  ExprT malloc(MethodGenerator mv, JExpr sizeInBytes);

  ExprT realloc(ExprT pointer, JExpr newSizeInBytes);

  ExprT pointerPlus(ExprT pointer, JExpr offsetInBytes);

  GExpr valueOf(ExprT pointerExpr);

  ExprT nullPointer();

  ConditionGenerator comparePointers(GimpleOp op, ExprT x, ExprT y);

  JExpr memoryCompare(ExprT p1, ExprT p2, JExpr n);

  void memoryCopy(MethodGenerator mv, ExprT destination, ExprT source, JExpr length, boolean buffer);

  void memorySet(MethodGenerator mv, ExprT pointer, JExpr byteValue, JExpr length);

  VoidPtr toVoidPointer(ExprT ptrExpr);

  ExprT unmarshallVoidPtrReturnValue(MethodGenerator mv, JExpr voidPointer);

}
