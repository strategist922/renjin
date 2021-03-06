package org.renjin.primitives.combine;

import com.google.common.collect.Lists;
import org.renjin.eval.Context;
import org.renjin.invoke.codegen.ArgumentIterator;
import org.renjin.sexp.*;

import java.util.Iterator;
import java.util.List;

/**
 * methods used by both cbind and rbind
 */
public abstract class AbstractBindFunction extends SpecialFunction {


  protected AbstractBindFunction(String name) {
    super(name);
  }

  public List<BindArgument> createBindArgument(Context context, Environment rho, int deparseLevel,
                                               boolean defaultToRow, ArgumentIterator argumentItr) {

    List<BindArgument> bindArguments = Lists.newArrayList();
    while(argumentItr.hasNext()) {
      PairList.Node currentNode = argumentItr.nextNode();
      SEXP evaluated = context.evaluate(currentNode.getValue(), rho);
      bindArguments.add(new BindArgument(currentNode.getName(), (Vector) evaluated, defaultToRow, currentNode.getValue(), deparseLevel, context));
    }
    return bindArguments;
  }

  public List<BindArgument> cleanBindArguments(List<BindArgument> arguments) {
    Iterator<BindArgument> argumentsItr = arguments.iterator();
    List<BindArgument> cleanList = Lists.newArrayList();
    while(argumentsItr.hasNext()) {
      BindArgument arg = argumentsItr.next();
      if (arg.getVector().length() != 0) {
        cleanList.add(arg);
      }
    }
    return cleanList;
  }

  /**
   *    The method dispatching is _not_ done via ‘UseMethod()’, but by
   C-internal dispatching.  Therefore there is no need for, e.g.,
   ‘rbind.default’.

   <p>The dispatch algorithm is described in the source file
   (‘.../src/main/bind.c’) as

   <ol>
   <li>For each argument we get the list of possible class
   memberships from the class attribute.</li>

   <li>We inspect each class in turn to see if there is an
   applicable method.</li>

   <li>If we find an applicable method we make sure that it is
   identical to any method determined for prior arguments.  If
   it is identical, we proceed, otherwise we immediately drop
   through to the default code.</li>
   </ol>
   */
  public static SEXP tryBindDispatch(Context context, Environment rho,
                                     String bindFunctionName, int deparseLevel, List<BindArgument> arguments) {

    Symbol foundMethod = null;
    org.renjin.sexp.Function foundFunction = null;

    for(BindArgument argument : arguments) {
      Vector classes = argument.getClasses();
      for(int i=0;i!=classes.length();++i) {
        Symbol methodName = Symbol.get(bindFunctionName + "." + classes.getElementAsString(i));
        org.renjin.sexp.Function function = rho.findFunction(context, methodName);
        if(function != null) {
          if(foundMethod != null && methodName != foundMethod) {
            // conflicting overloads,
            // drop into default function
            return null;
          }
          foundMethod = methodName;
          foundFunction = function;
        }
      }
    }

    if(foundFunction == null) {
      // no methods found, drop thru to default
      return null;
    }

    // build a new FunctionCall object and apply
    PairList.Builder args = new PairList.Builder();
    args.add("deparse.level", new Promise(Symbol.get("deparse.level"), new IntArrayVector(deparseLevel)));

    for (BindArgument argument : arguments) {
      args.add(argument.getArgName(), argument.repromise());
    }

    PairList buildArgs = args.build();

    FunctionCall call = new FunctionCall(Symbol.get(bindFunctionName), buildArgs);
    return foundFunction.apply(context, rho, call, buildArgs);
  }

}
