package hu.bme.mit.theta.analysis.cfa;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.And;
import static hu.bme.mit.theta.core.utils.VarIndexing.all;

import java.util.List;

import hu.bme.mit.theta.analysis.expl.StmtAction;
import hu.bme.mit.theta.common.ObjectUtils;
import hu.bme.mit.theta.core.Expr;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.StmtUnfoldResult;
import hu.bme.mit.theta.core.utils.StmtUtils;
import hu.bme.mit.theta.core.utils.VarIndexing;
import hu.bme.mit.theta.formalism.cfa.CFA.CfaEdge;

public final class CfaAction implements LocAction, StmtAction {

	private final CfaEdge edge;
	private final Expr<BoolType> expr;
	private final VarIndexing nextIndexing;

	private CfaAction(final CfaEdge edge) {
		this.edge = checkNotNull(edge);

		final StmtUnfoldResult toExprResult = StmtUtils.toExpr(edge.getStmts(), all(0));
		expr = And(toExprResult.getExprs());
		nextIndexing = toExprResult.getIndexing();
	}

	public static CfaAction create(final CfaEdge edge) {
		return new CfaAction(edge);
	}

	public CfaEdge getEdge() {
		return edge;
	}

	@Override
	public Expr<BoolType> toExpr() {
		return expr;
	}

	@Override
	public VarIndexing nextIndexing() {
		return nextIndexing;
	}

	@Override
	public String toString() {
		return ObjectUtils.toStringBuilder(getClass().getSimpleName()).addAll(edge.getStmts()).toString();
	}

	@Override
	public List<Stmt> getStmts() {
		return edge.getStmts();
	}

}
