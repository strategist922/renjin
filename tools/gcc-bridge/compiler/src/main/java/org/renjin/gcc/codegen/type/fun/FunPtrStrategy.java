package org.renjin.gcc.codegen.type.fun;

import com.google.common.base.Preconditions;
import org.renjin.gcc.InternalCompilerException;
import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.array.ArrayTypeStrategies;
import org.renjin.gcc.codegen.array.ArrayTypeStrategy;
import org.renjin.gcc.codegen.condition.ConditionGenerator;
import org.renjin.gcc.codegen.expr.*;
import org.renjin.gcc.codegen.fatptr.FatPtrStrategy;
import org.renjin.gcc.codegen.type.*;
import org.renjin.gcc.codegen.type.primitive.PrimitiveTypeStrategy;
import org.renjin.gcc.codegen.type.record.unit.RecordUnitPtrStrategy;
import org.renjin.gcc.codegen.type.record.unit.RefConditionGenerator;
import org.renjin.gcc.codegen.type.voidt.VoidPtr;
import org.renjin.gcc.codegen.type.voidt.VoidPtrStrategy;
import org.renjin.gcc.codegen.var.VarAllocator;
import org.renjin.gcc.gimple.GimpleOp;
import org.renjin.gcc.gimple.GimpleVarDecl;
import org.renjin.gcc.gimple.expr.GimpleConstructor;
import org.renjin.gcc.gimple.type.GimpleArrayType;
import org.renjin.repackaged.asm.Type;

import java.lang.invoke.MethodHandle;

/**
 * Strategy for function pointer types
 */
public class FunPtrStrategy implements PointerTypeStrategy<FunPtr>, SimpleTypeStrategy<FunPtr> {
  
  public static final Type METHOD_HANDLE_TYPE = Type.getType(MethodHandle.class);

  public FunPtrStrategy() {
  }

  @Override
  public ParamStrategy getParamStrategy() {
    return new RefPtrParamStrategy<>(this);
  }

  @Override
  public FunPtr variable(GimpleVarDecl decl, VarAllocator allocator) {
    return new FunPtr(allocator.reserve(decl.getNameIfPresent(), Type.getType(MethodHandle.class)));
  }

  @Override
  public FieldStrategy fieldGenerator(Type className, String fieldName) {
    return new SimpleFieldStrategy(fieldName, METHOD_HANDLE_TYPE, FunPtr.class);
  }

  @Override
  public FunPtr constructorExpr(ExprFactory exprFactory, GimpleConstructor value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FieldStrategy addressableFieldGenerator(Type className, String fieldName) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public ReturnStrategy getReturnStrategy() {
    return new SimpleReturnStrategy(this);
  }

  @Override
  public FatPtrStrategy pointerTo() {
    return new FatPtrStrategy(new FunPtrValueFunction(32));
  }

  @Override
  public ArrayTypeStrategy arrayOf(GimpleArrayType arrayType) {
    return ArrayTypeStrategies.of(arrayType, new FunPtrValueFunction(32));
  }

  @Override
  public FunPtr cast(GExpr value, TypeStrategy typeStrategy) throws UnsupportedCastException {
    if(typeStrategy instanceof FunPtrStrategy) {
      // We can liberally cast between different types of function pointers thanks
      // to the flexibility of MethodHandles.
      return (FunPtr) value;
    }
    
    // TODO: remove this, just to get rtti running
    if(typeStrategy instanceof RecordUnitPtrStrategy) {
      return nullPointer();
    }
    
    if(typeStrategy instanceof PrimitiveTypeStrategy) {
      return nullPointer();
    }
    
    if(typeStrategy instanceof VoidPtrStrategy) {
      VoidPtr voidPtr = (VoidPtr) value;
      return new FunPtr(Expressions.cast(voidPtr.unwrap(), METHOD_HANDLE_TYPE));
    }
    
    throw new UnsupportedCastException();
  }

  @Override
  public FunPtr nullPointer() {
    return new FunPtr(Expressions.nullRef(METHOD_HANDLE_TYPE));
  }

  @Override
  public ConditionGenerator comparePointers(GimpleOp op, FunPtr x, FunPtr y) {
    return new RefConditionGenerator(op, x.unwrap(), y.unwrap());
  }

  @Override
  public JExpr memoryCompare(FunPtr p1, FunPtr p2, JExpr n) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public void memoryCopy(MethodGenerator mv, FunPtr destination, FunPtr source, JExpr length, boolean buffer) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public void memorySet(MethodGenerator mv, FunPtr pointer, JExpr byteValue, JExpr length) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public VoidPtr toVoidPointer(FunPtr ptrExpr) {
    return new VoidPtr(ptrExpr.unwrap());
  }

  @Override
  public FunPtr unmarshallVoidPtrReturnValue(MethodGenerator mv, JExpr voidPointer) {
    return new FunPtr(Expressions.cast(voidPointer, METHOD_HANDLE_TYPE));
  }

  @Override
  public FunPtr malloc(MethodGenerator mv, JExpr sizeInBytes) {
    throw new UnsupportedOperationException("Cannot malloc function pointers");
  }

  @Override
  public FunPtr realloc(FunPtr pointer, JExpr newSizeInBytes) {
    throw new InternalCompilerException("Cannot realloc function pointers");
  }

  @Override
  public FunPtr pointerPlus(FunPtr pointer, JExpr offsetInBytes) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public GExpr valueOf(FunPtr pointerExpr) {
    return pointerExpr;
  }

  @Override
  public String toString() {
    return "FunPtrStrategy";
  }

  @Override
  public Type getJvmType() {
    return METHOD_HANDLE_TYPE;
  }

  @Override
  public FunPtr wrap(JExpr expr) {
    Preconditions.checkArgument(expr.getType().equals(METHOD_HANDLE_TYPE));
    return new FunPtr(expr);
  }
}
