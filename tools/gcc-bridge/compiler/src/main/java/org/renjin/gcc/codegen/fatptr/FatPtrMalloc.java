package org.renjin.gcc.codegen.fatptr;

import com.google.common.collect.Lists;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.renjin.gcc.codegen.MethodGenerator;
import org.renjin.gcc.codegen.type.primitive.ConstantValue;
import org.renjin.gcc.codegen.var.LocalVarAllocator;
import org.renjin.gcc.codegen.var.Value;
import org.renjin.gcc.codegen.var.Values;

import java.util.List;

/**
 * Allocates a 
 */
public final class FatPtrMalloc {

  private static final int MAX_UNROLL_COUNT = 5;
  
  private FatPtrMalloc() {}

  public static FatPtrExpr alloc(MethodGenerator mv, ValueFunction valueFunction, Value length) {
    
    List<Value> initialValues = ValueFunctions.getConstructorsChecked(valueFunction);
    Value totalLength = Values.product(length, valueFunction.getElementLength());

    // If the values don't require any initialization (for example, an array of 
    // double is initialized by the JVM to zeros)
    // Then we can just return a new array expression
    if(initialValues.isEmpty()) {
      Value array = Values.newArray(valueFunction.getValueType(), totalLength);

      return new FatPtrExpr(array);
    }
    
    // If we *do* need to construct the array elements, but the length is short and known at compile time,
    // we can unroll it the loop. 
    
    if(length instanceof ConstantValue) {
      ConstantValue constantLength = (ConstantValue) length;
      
      if(constantLength.getIntValue() <= MAX_UNROLL_COUNT) {
        List<Value> arrayValues = Lists.newArrayList();
        for(int i=0;i<constantLength.getIntValue();++i) {
          arrayValues.addAll(initialValues);
        }
        return new FatPtrExpr(Values.newArray(valueFunction.getValueType(), arrayValues));
      }
    } 
    
    // Otherwise we need to actually emit a loop to initialize the array at runtime

    // Reserve local variables for the array and for our counter
    Type arrayType = Wrappers.valueArrayType(valueFunction.getValueType());
    LocalVarAllocator.LocalVar array = mv.getLocalVarAllocator().reserve("$tmp$array", arrayType);
    LocalVarAllocator.LocalVar counter = mv.getLocalVarAllocator().reserve("$tmp$index", Type.INT_TYPE);


    // First allocate the array
    totalLength.load(mv);
    mv.newarray(valueFunction.getValueType());
    mv.store(array.getIndex(), arrayType);

    // initialize the loop counter
    mv.iconst(0);
    mv.store(counter.getIndex(), Type.INT_TYPE);

    // Now loop until we've initialized all array elements
    Label loopHead = new Label();
    Label loopCheck = new Label();

    mv.goTo(loopCheck);

    // Initialize the values
    mv.mark(loopHead);
    for (Value initialValue : initialValues) {
      array.load(mv);
      counter.load(mv);
      initialValues.get(0).load(mv);
      mv.astore(valueFunction.getValueType());
      mv.iinc(counter.getIndex(), 1);
    }

    // Check the condition
    mv.mark(loopCheck);
    mv.load(counter.getIndex(), Type.INT_TYPE);
    totalLength.load(mv);
    mv.ificmplt(loopHead);

    // Load the array back on the stack
    return new FatPtrExpr(array);
  }
  
}
