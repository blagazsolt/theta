package hu.bme.mit.theta.analysis.expr;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.impl.VarIndexing;

public interface ExprAction extends Action {

	Expr<BoolType> toExpr();

	VarIndexing nextIndexing();

}
