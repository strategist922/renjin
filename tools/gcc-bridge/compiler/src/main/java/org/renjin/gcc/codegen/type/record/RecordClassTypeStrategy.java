package org.renjin.gcc.codegen.type.record;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.renjin.gcc.InternalCompilerException;
import org.renjin.gcc.codegen.RecordClassGenerator;
import org.renjin.gcc.codegen.array.ArrayTypeStrategies;
import org.renjin.gcc.codegen.array.ArrayTypeStrategy;
import org.renjin.gcc.codegen.expr.*;
import org.renjin.gcc.codegen.fatptr.AddressableField;
import org.renjin.gcc.codegen.fatptr.FatPtrExpr;
import org.renjin.gcc.codegen.fatptr.FatPtrStrategy;
import org.renjin.gcc.codegen.type.*;
import org.renjin.gcc.codegen.type.record.unit.RecordUnitPtr;
import org.renjin.gcc.codegen.type.record.unit.RecordUnitPtrStrategy;
import org.renjin.gcc.codegen.var.VarAllocator;
import org.renjin.gcc.gimple.GimpleVarDecl;
import org.renjin.gcc.gimple.expr.GimpleConstructor;
import org.renjin.gcc.gimple.expr.GimpleFieldRef;
import org.renjin.gcc.gimple.type.GimpleArrayType;
import org.renjin.gcc.gimple.type.GimpleField;
import org.renjin.gcc.gimple.type.GimpleRecordTypeDef;
import org.renjin.repackaged.asm.Type;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for variables and values of type {@code GimpleRecordType} that employs JVM classes
 */
public class RecordClassTypeStrategy extends RecordTypeStrategy<RecordValue> implements SimpleTypeStrategy<RecordValue> {

  private Type jvmType;
  private boolean provided;
  private boolean unitPointer;

  private Map<String, FieldStrategy> nameMap = null;
  private Map<Integer, FieldStrategy> offsetMap = null;

  public RecordClassTypeStrategy(GimpleRecordTypeDef recordTypeDef) {
    super(recordTypeDef);
    
    if(recordTypeDef.isUnion()) {
      throw new UnsupportedOperationException("Unions are not supported. Offending type:\n" + recordTypeDef);
    }
  }

  public Type getJvmType() {
    if(jvmType == null) {
      throw new IllegalStateException("Type name of record " + getRecordType().getName() + " has not been initialized.");
    }
    return jvmType;
  }

  @Override
  public RecordValue wrap(JExpr expr) {
    return new RecordValue(expr);
  }

  public void setJvmType(Type jvmType) {
    this.jvmType = jvmType;
  }

  /**
   *
   * @return true if the class backing this record type is already provided by an existing JVM class
   */
  public boolean isProvided() {
    return provided;
  }

  public void setProvided(boolean provided) {
    this.provided = provided;
  }

  public boolean isUnitPointer() {
    return unitPointer;
  }

  public void setUnitPointer(boolean unitPointer) {
    this.unitPointer = unitPointer;
  }

  @Override
  public void linkFields(TypeOracle typeOracle) {
    nameMap = new HashMap<>();
    offsetMap = new HashMap<>();
    
    for(GimpleField gimpleField : recordTypeDef.getFields()) {
      if(!offsetMap.containsKey(gimpleField.getOffset()) && !isCircularField(recordTypeDef, gimpleField)) {
        FieldStrategy fieldStrategy = fieldStrategy(typeOracle, gimpleField);
        nameMap.put(gimpleField.getName(), fieldStrategy);
        offsetMap.put(gimpleField.getOffset(), fieldStrategy);
      }
    }
  }

  private FieldStrategy fieldStrategy(TypeOracle typeOracle, GimpleField gimpleField) {
    // If the record begins with another record, model the initial field as a superclass rather
    // than a field so that we can cast back and forth between the two types
    if(gimpleField.getOffset() == 0) {
      TypeStrategy fieldTypeStrategy = typeOracle.forType(gimpleField.getType());
      if(fieldTypeStrategy instanceof RecordClassTypeStrategy) {
        RecordClassTypeStrategy fieldRecordTypeStrategy = (RecordClassTypeStrategy) fieldTypeStrategy;
        if(fieldRecordTypeStrategy.isUnitPointer()) {
          if (!fieldRecordTypeStrategy.getJvmType().equals(this.jvmType)) {
            return new SuperClassFieldStrategy((RecordClassTypeStrategy) fieldTypeStrategy);
          }
        }
      }
    }
    return typeOracle.forField(getJvmType(), gimpleField);
  }

  private Type getSuperClass() {
    for (FieldStrategy fieldStrategy : nameMap.values()) {
      if(fieldStrategy instanceof SuperClassFieldStrategy) {
        return ((SuperClassFieldStrategy) fieldStrategy).getType();
      }
    }
    return Type.getType(Object.class);
  }

  @Override
  public final ParamStrategy getParamStrategy() {
    return new RecordClassParamStrategy(jvmType, isUnitPointer());
  }

  @Override
  public ReturnStrategy getReturnStrategy() {
    return new SimpleReturnStrategy(this);
  }

  @Override
  public RecordValue variable(GimpleVarDecl decl, VarAllocator allocator) {

    JLValue instance = allocator.reserve(decl.getName(), jvmType, new RecordConstructor(this));

    if(isUnitPointer()) {
      // If we are using the RecordUnitPtr strategy, then the record value is also it's address
      return new RecordValue(instance, new RecordUnitPtr(instance));

    } else if(decl.isAddressable()) {
      JLValue unitArray = allocator.reserveUnitArray(decl.getName(), jvmType, Optional.of((JExpr)instance));
      FatPtrExpr address = new FatPtrExpr(unitArray);
      JExpr value = Expressions.elementAt(address.getArray(), 0);
      return new RecordValue(value, address);

    } else {
      
      return new RecordValue(instance);
    }
  }

  @Override
  public FieldStrategy fieldGenerator(Type className, String fieldName) {
    return new RecordFieldStrategy(this, className, fieldName);
  }

  @Override
  public FieldStrategy addressableFieldGenerator(Type className, String fieldName) {
    if(isUnitPointer()) {
      // If this type is a unit pointer, we don't need to do anything special
      return new RecordFieldStrategy(this, className, fieldName);
    } else {
      return new AddressableField(getJvmType(), fieldName, new RecordClassValueFunction(this));
    }
  }
  
  @Override
  public RecordValue constructorExpr(ExprFactory exprFactory, GimpleConstructor value) {
    Map<GimpleFieldRef, GExpr> fields = Maps.newHashMap();
    for (GimpleConstructor.Element element : value.getElements()) {
      GExpr fieldValue = exprFactory.findGenerator(element.getValue());
      fields.put((GimpleFieldRef) element.getField(), fieldValue);
    }
    return new RecordValue(new RecordConstructor(this, fields));
  }


  @Override
  public void writeClassFiles(File outputDirectory) throws IOException {
    if(isProvided()) {
      return;
    }

    RecordClassGenerator classGenerator = new RecordClassGenerator(jvmType, getSuperClass(), nameMap.values());
    classGenerator.writeClassFile(outputDirectory);
  }

  @Override
  public GExpr memberOf(RecordValue instance, GimpleFieldRef fieldRef) {
    if(nameMap == null) {
      throw new IllegalStateException("Fields map is not yet initialized.");
    }
    FieldStrategy fieldStrategy = nameMap.get(fieldRef.getName());
    if(fieldStrategy != null) {
      return fieldStrategy.memberExprGenerator(instance.getRef());
    }
    
    // Field names are not really taken seriously in Gimple
    // If we can't find a field by name, then try by offset.
    fieldStrategy = offsetMap.get(fieldRef.getOffset());
    
    if(fieldStrategy != null) {
      return fieldStrategy.memberExprGenerator(instance.getRef());
    }
    
    throw new InternalCompilerException(
          String.format("No field named '%s' in record type '%s'", fieldRef.getName(), jvmType));
  }

  @Override
  public ArrayTypeStrategy arrayOf(GimpleArrayType arrayType) {
    return ArrayTypeStrategies.of(arrayType, new RecordClassValueFunction(this));
  }

  @Override
  public RecordValue cast(GExpr value, TypeStrategy typeStrategy) throws UnsupportedCastException {
    if(typeStrategy instanceof RecordClassTypeStrategy) {
      return (RecordValue) value;
    
    } 
    throw new UnsupportedCastException();
  }

  @Override
  public PointerTypeStrategy pointerTo() {
    if(unitPointer) {
      return new RecordUnitPtrStrategy(this);
    } else {
      return new FatPtrStrategy(new RecordClassValueFunction(this));
    }
  }

  public RecordUnitPtrStrategy pointerToUnit() {
    return new RecordUnitPtrStrategy(this);
  }

  @Override
  public String toString() {
    return "RecordClassTypeStrategy[" + recordTypeDef.getName() + "]";
  }
}
