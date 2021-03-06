package org.renjin.compiler.builtins;

import org.renjin.compiler.codegen.EmitContext;
import org.renjin.compiler.ir.ValueBounds;
import org.renjin.compiler.ir.tac.IRArgument;
import org.renjin.compiler.ir.tac.expressions.Expression;
import org.renjin.invoke.model.JvmMethod;
import org.renjin.repackaged.asm.Type;
import org.renjin.repackaged.asm.commons.InstructionAdapter;

import java.util.Iterator;
import java.util.List;

/**
 * Call to a data parallel operator with scalar arguments. 
 */
public class DataParallelScalarCall implements Specialization {
  
  private final JvmMethod method;
  private final ValueBounds valueBounds;
  private final boolean constant;

  public DataParallelScalarCall(JvmMethod method, List<ValueBounds> argumentBounds, ValueBounds resultBounds) {
    this.method = method;
    this.valueBounds = resultBounds;
    this.constant = ValueBounds.allConstant(argumentBounds);
  }
  
  public Specialization trySpecializeFurther() {
    return this;
  }

  @Override
  public Type getType() {
    return Type.getType(method.getReturnType());
  }

  @Override
  public ValueBounds getValueBounds() {
    return valueBounds;
  }

  @Override
  public void load(EmitContext emitContext, InstructionAdapter mv, List<IRArgument> arguments) {
    
    Iterator<IRArgument> argumentIt = arguments.iterator();

    for (JvmMethod.Argument formal : method.getAllArguments()) {
      if(formal.isContextual()) {
        throw new UnsupportedOperationException("TODO");
        
      } else if(formal.isRecycle()) {
        Expression argument = argumentIt.next().getExpression();
        argument.load(emitContext, mv);
        emitContext.convert(mv, argument.getType(), Type.getType(formal.getClazz()));
      }
    }
    
    mv.invokestatic(Type.getInternalName(method.getDeclaringClass()), method.getName(), 
        Type.getMethodDescriptor(method.getMethod()), false);
  }
}
