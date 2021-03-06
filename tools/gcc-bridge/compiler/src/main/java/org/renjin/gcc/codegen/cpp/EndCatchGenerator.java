package org.renjin.gcc.codegen.cpp;

import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.call.CallGenerator;
import org.renjin.gcc.codegen.expr.ExprFactory;
import org.renjin.gcc.gimple.statement.GimpleCall;

public class EndCatchGenerator implements CallGenerator {
  
  public static final String NAME = "__cxa_end_catch";

  @Override
  public void emitCall(MethodGenerator mv, ExprFactory exprFactory, GimpleCall call) {
    // NOOP
  }
}
