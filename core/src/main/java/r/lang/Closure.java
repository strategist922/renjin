/*
 * R : A Computer Language for Statistical Data Analysis
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1997--2008  The R Development Core Team
 * Copyright (C) 2003, 2004  The R Foundation
 * Copyright (C) 2010 bedatadriven
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package r.lang;

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import r.lang.exception.EvalException;
import r.lang.primitive.Evaluation;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Collections2.transform;

/**
 * The function closure data type.
 *
 * <p>
 * In R functions are objects and can be manipulated in much the same way as any other object.
 * Functions (or more precisely, function closures) have three basic components:
 *  a formal argument list, a body and an environment.
 *
 */
public class Closure extends AbstractSEXP implements Function {

  public static final String TYPE_NAME = "closure";
  public static final int TYPE_CODE = 4;
  public static final String IMPLICIT_CLASS = "function";

  private Environment enclosingEnvironment;
  private SEXP body;
  private PairList formals;

  public Closure(Environment enclosingEnvironment, PairList formals, SEXP body, PairList attributes) {
    super(attributes);
    this.enclosingEnvironment = enclosingEnvironment;
    this.body = body;
    this.formals = formals;
  }

  public Closure(Environment environment, PairList formals, SEXP body) {
    this(environment, formals, body, Null.INSTANCE);
  }

  @Override
  public int getTypeCode() {
    return TYPE_CODE;
  }

  @Override
  public String getTypeName() {
    return TYPE_NAME;
  }

  @Override
  protected String getImplicitClass() {
    return IMPLICIT_CLASS;
  }

  @Override
  public void accept(SexpVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public EvalResult apply(Context context, Environment rho, FunctionCall call, PairList args) {
    return apply(context, rho, args);
  }

  public EvalResult apply(Context context, Environment rho, PairList arguments) {
    Context functionContext = context.beginFunction(enclosingEnvironment, arguments);
    Environment functionEnvironment = functionContext.getEnvironment();

    try {
      matchArgumentsInto(arguments, enclosingEnvironment, functionEnvironment, rho, context);

      EvalResult result = body.evaluate(functionContext, functionEnvironment);

      functionContext.exit();

      return result;
    } catch(Evaluation.ReturnException e) {
      if(e.getEnvironment() != functionEnvironment) {
        throw e;
      }
      return EvalResult.visible(e.getValue());
    }
  }

  /**
   * A function's <strong> evaluation environment</strong> is the environment
   * that was active at the time that the
   * function was created. Any symbols bound in that environment are
   * captured and available to the function. This combination of the code of the
   * function and the bindings in its environment is called a `function closure', a
   * term from functional programming theory.
   *
   */
  public Environment getEnclosingEnvironment() {
    return enclosingEnvironment;
  }

  /**
   * The body is a parsed R statement.
   * It is usually a collection of statements in braces but it
   * can be a single statement, a symbol or even a constant.
   */
  public SEXP getBody() {
    return body;
  }

  /**
   * The formal argument list is a a pair list of arguments.
   * An argument can be a symbol, or a ‘symbol = default’ construct, or
   * the special argument ‘...’.
   *
   * <p> The second form of argument is
   *  used to specify a default value for an argument.
   * This value will be used if the function is called
   *  without any value specified for that argument.
   * The ‘...’ argument is special and can contain any number of arguments.
   * It is generally used if the number of arguments
   * is unknown or in cases where the arguments will
   * be passed on to another function.
   */
  public PairList getFormals() {
    return formals;
  }

  /**
   * Argument matching is done by a three-pass process:
   * <ol>
   * <li><strong>Exact matching on tags.</strong> For each named supplied argument the list of formal arguments
   *  is searched for an item whose name matches exactly. It is an error to have the same formal
   * argument match several actuals or vice versa.</li>
   *
   * <li><strong>Partial matching on tags.</strong> Each remaining named supplied argument is compared to the
   * remaining formal arguments using partial matching. If the name of the supplied argument
   * matches exactly with the first part of a formal argument then the two arguments are considered
   * to be matched. It is an error to have multiple partial matches.
   *  Notice that if f <- function(fumble, fooey) fbody, then f(f = 1, fo = 2) is illegal,
   * even though the 2nd actual argument only matches fooey. f(f = 1, fooey = 2) is legal
   * though since the second argument matches exactly and is removed from consideration for
   * partial matching. If the formal arguments contain ‘...’ then partial matching is only applied to
   * arguments that precede it.
   *
   * <li><strong>Positional matching.</strong> Any unmatched formal arguments are bound to unnamed supplied arguments,
   * in order. If there is a ‘...’ argument, it will take up the remaining arguments, tagged or not.
   * If any arguments remain unmatched an error is declared.
   *
   * @param actuals the actual arguments supplied to the list
   * @param innerEnv the environment in which to resolve the arguments;
   * @param rho  the environment from which the function is called
   * @param context
   */
  private void matchArgumentsInto(PairList actuals, Environment enclosure, Environment innerEnv, Environment rho, Context context) {



    List<PairList.Node> unmatchedActuals = Lists.newArrayList();
    for(PairList.Node argNode : actuals.nodes()) {
      if(SymbolExp.ELLIPSES.equals(argNode.getValue())) {
        DotExp dotExp = (DotExp) argNode.getValue().evalToExp(context, rho);
        for(PairList.Node dotArg : dotExp.getPromises().nodes()) {
          unmatchedActuals.add(dotArg);
        }
      }  else {
        unmatchedActuals.add(argNode);
      }
    }

    List<PairList.Node> unmatchedFormals = Lists.newArrayList(formals.nodes());

    // do exact matching
    for(ListIterator<PairList.Node> formalIt = unmatchedFormals.listIterator(); formalIt.hasNext(); ) {
      PairList.Node formal = formalIt.next();
      if(formal.hasTag()) {
        SymbolExp name = (SymbolExp) formal.getTag();
        Collection<PairList.Node> matches = Collections2.filter(unmatchedActuals, PairList.Predicates.matches(name));

        if(matches.size() == 1) {
          PairList.Node match = first(matches);
          innerEnv.setVariable(name, new Promise(rho, match.getValue()));
          formalIt.remove();
          unmatchedActuals.remove(match);

        } else if(matches.size() > 1) {
          throw new EvalException(String.format("Multiple named values provided for argument '%s'", name.getPrintName()));
        }
      }
    }

    // do partial matching
    Collection<PairList.Node> remainingNamedFormals = filter(unmatchedFormals, PairList.Predicates.hasTag());
    for(Iterator<PairList.Node> actualIt = unmatchedActuals.iterator(); actualIt.hasNext(); ) {
      PairList.Node actual = actualIt.next();
      if(actual.hasTag()) {
        Collection<PairList.Node> matches = Collections2.filter(remainingNamedFormals,
            PairList.Predicates.startsWith(actual.getTag()));

        if(matches.size() == 1) {
          PairList.Node match = first(matches);
          innerEnv.setVariable(match.getTag(), new Promise(rho, actual.getValue()));
          actualIt.remove();
          unmatchedFormals.remove(match);

        } else if(matches.size() > 1) {
          throw new EvalException(String.format("Provided argument '%s' matches multiple named formal arguments: %s",
              actual.getTag().getPrintName(), argumentTagList(matches)));
        }
      }
    }

    // match any unnamed args positionally

    Iterator<PairList.Node> formalIt = unmatchedFormals.iterator();
    PeekingIterator<PairList.Node> actualIt = Iterators.peekingIterator(unmatchedActuals.iterator());
    while( formalIt.hasNext()) {
      PairList.Node formal = formalIt.next();
      if(SymbolExp.ELLIPSES.equals(formal.getTag())) {
        PairList.Node.Builder promises = PairList.Node.newBuilder();
        while(actualIt.hasNext()) {
          PairList.Node actual = actualIt.next();
          promises.add( actual.getRawTag(),  new Promise(rho, actual.getValue()) );
        }
        innerEnv.setVariable(formal.getTag(), new DotExp( promises.build() ));

      } else if( hasNextUnTagged(actualIt) ) {
        innerEnv.setVariable(formal.getTag(), new Promise(rho, nextUnTagged(actualIt).getValue()) );

      } else if( formal.getValue() == SymbolExp.MISSING_ARG ) {
        innerEnv.setVariable(formal.getTag(), SymbolExp.MISSING_ARG);

      } else {
        innerEnv.setVariable(formal.getTag(), new Promise(innerEnv, formal.getValue())); // default
      }
    }
    if(actualIt.hasNext()) {
      throw new EvalException(String.format("Unmatched positional arguments"));
    }
  }

  private boolean hasNextUnTagged(PeekingIterator<PairList.Node> it) {
    return it.hasNext() && !it.peek().hasTag();
  }

  private PairList.Node nextUnTagged(Iterator<PairList.Node> it) {
    PairList.Node arg = it.next() ;
    while( arg.hasTag() ) {
      arg = it.next();
    }
    return arg;
  }

  private String argumentTagList(Collection<PairList.Node> matches) {
    return Joiner.on(", ").join(transform(matches, new CollectionUtils.TagName()));
  }

  private <X> X first(Iterable<X> values) {
    return values.iterator().next();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("function(");
    if(getFormals() instanceof PairList.Node) {
      ((PairList.Node) getFormals()).appendValuesTo(sb);
    }
    return sb.append(")").toString();
  }
}