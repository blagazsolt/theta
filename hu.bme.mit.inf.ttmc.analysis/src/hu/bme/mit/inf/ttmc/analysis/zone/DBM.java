package hu.bme.mit.inf.ttmc.analysis.zone;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.inf.ttmc.analysis.zone.DiffBounds.Inf;
import static hu.bme.mit.inf.ttmc.analysis.zone.DiffBounds.Leq;
import static hu.bme.mit.inf.ttmc.analysis.zone.DiffBounds.Lt;
import static hu.bme.mit.inf.ttmc.formalism.common.decl.impl.Decls2.Clock;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.IntBinaryOperator;

import com.google.common.collect.Sets;

import hu.bme.mit.inf.ttmc.formalism.common.decl.ClockDecl;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.AndConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.ClockConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.DiffEqConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.DiffGeqConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.DiffGtConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.DiffLeqConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.DiffLtConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.TrueConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.UnitEqConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.UnitGeqConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.UnitGtConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.UnitLeqConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.constr.UnitLtConstr;
import hu.bme.mit.inf.ttmc.formalism.ta.op.ClockOp;
import hu.bme.mit.inf.ttmc.formalism.ta.op.CopyOp;
import hu.bme.mit.inf.ttmc.formalism.ta.op.FreeOp;
import hu.bme.mit.inf.ttmc.formalism.ta.op.GuardOp;
import hu.bme.mit.inf.ttmc.formalism.ta.op.ResetOp;
import hu.bme.mit.inf.ttmc.formalism.ta.op.ShiftOp;
import hu.bme.mit.inf.ttmc.formalism.ta.utils.ClockConstrVisitor;
import hu.bme.mit.inf.ttmc.formalism.ta.utils.ClockOpVisitor;

final class DBM {

	private static final IntBinaryOperator ZERO_DBM_VALUES = (x, y) -> Leq(0);
	private static final IntBinaryOperator TOP_DBM_VALUES = (x, y) -> x == 0 || x == y ? Leq(0) : Inf();

	private static final ClockDecl ZERO_CLOCK = Clock("_zero");

	private final ExecuteVisitor executeVisitor;;
	private final AndOperationVisitor andVisitor;

	private final LinkedHashMap<ClockDecl, Integer> clockToIndex;
	private final SimpleDBM dbm;

	private DBM(final LinkedHashMap<ClockDecl, Integer> clockToIndex, final IntBinaryOperator values) {
		this.clockToIndex = clockToIndex;
		this.dbm = new SimpleDBM(clockToIndex.size() - 1, values);
		executeVisitor = new ExecuteVisitor();
		andVisitor = new AndOperationVisitor();
	}

	private DBM(final DBM dbm) {
		this.clockToIndex = new LinkedHashMap<>(dbm.clockToIndex);
		this.dbm = new SimpleDBM(dbm.dbm);
		executeVisitor = new ExecuteVisitor();
		andVisitor = new AndOperationVisitor();

	}

	////

	public static DBM copyOf(final DBM dbm) {
		checkNotNull(dbm);
		return new DBM(dbm);
	}

	public static DBM zero(final Collection<? extends ClockDecl> clocks) {
		checkNotNull(clocks);
		final LinkedHashMap<ClockDecl, Integer> clockToIndex = createIndexMapFrom(clocks);
		return new DBM(clockToIndex, ZERO_DBM_VALUES);
	}

	public static DBM top(final Collection<? extends ClockDecl> clocks) {
		checkNotNull(clocks);
		final LinkedHashMap<ClockDecl, Integer> clockToIndex = createIndexMapFrom(clocks);
		return new DBM(clockToIndex, TOP_DBM_VALUES);
	}

	////

	int getBound(final ClockDecl x, final ClockDecl y) {
		checkNotNull(x);
		checkNotNull(y);

		if (x.equals(y) || x.equals(ZERO_CLOCK)) {
			return Leq(0);
		}

		final Integer i = clockToIndex.get(x);
		final Integer j = clockToIndex.get(y);

		if (i == null || j == null) {
			return Inf();
		}

		return dbm.get(i, j);
	}

	////

	public boolean isConsistent() {
		return dbm.isConsistent();
	}

	public boolean isSatisfied(final ClockConstr constr) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	public DBMRelation getRelation(final DBM that) {
		final Set<ClockDecl> clockDecls = Sets.union(this.clockToIndex.keySet(), that.clockToIndex.keySet());

		boolean leq = true;
		boolean geq = true;

		for (final ClockDecl x : clockDecls) {
			for (final ClockDecl y : clockDecls) {
				leq = leq && this.getBound(x, y) <= that.getBound(x, y);
				geq = geq && this.getBound(x, y) >= that.getBound(x, y);
			}
		}
		return DBMRelation.create(leq, geq);
	}

	////

	public void track(final ClockDecl clock) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	public void untrack(final ClockDecl clock) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	public void execute(final ClockOp op) {
		checkNotNull(op);
		op.accept(executeVisitor, null);
	}

	////

	public void up() {
		dbm.up();
	}

	public void down() {
		dbm.down();
	}

	public void and(final ClockConstr constr) {
		checkNotNull(constr);
		constr.accept(andVisitor, null);
	}

	public void free(final ClockDecl clock) {
		checkNotNull(clock);
		checkArgument(!isZeroClock(clock));
		final int x = indexOf(clock);
		dbm.free(x);

	}

	public void reset(final ClockDecl clock, final int m) {
		checkNotNull(clock);
		checkArgument(!isZeroClock(clock));
		final int x = indexOf(clock);
		dbm.reset(x, m);
	}

	public void copy(final ClockDecl lhs, final ClockDecl rhs) {
		checkNotNull(lhs);
		checkNotNull(rhs);
		checkArgument(!isZeroClock(lhs));
		checkArgument(!isZeroClock(rhs));
		final int x = indexOf(lhs);
		final int y = indexOf(rhs);
		dbm.copy(x, y);
	}

	public void shift(final ClockDecl clock, final int m) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	public void norm(final Map<? super ClockDecl, ? extends Integer> bounds) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	////

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public boolean equals(final Object obj) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public String toString() {
		return dbm.toString();
	}

	////

	private boolean isTracked(final ClockDecl clock) {
		return clockToIndex.containsKey(clock);
	}

	private boolean isZeroClock(final ClockDecl clock) {
		return clock.equals(ZERO_CLOCK);
	}

	private int indexOf(final ClockDecl clock) {
		checkArgument(isTracked(clock));
		return clockToIndex.get(clock);
	}

	private static LinkedHashMap<ClockDecl, Integer> createIndexMapFrom(final Collection<? extends ClockDecl> clocks) {
		final LinkedHashMap<ClockDecl, Integer> result = new LinkedHashMap<ClockDecl, Integer>();
		result.put(ZERO_CLOCK, 0);
		int i = 1;
		for (final ClockDecl clock : clocks) {
			if (!result.containsKey(clock)) {
				result.put(clock, i);
				i++;
			}
		}
		return result;
	}

	////

	private final class ExecuteVisitor implements ClockOpVisitor<Void, Void> {

		@Override
		public Void visit(final CopyOp op, final Void param) {
			copy(op.getClock(), op.getValue());
			return null;
		}

		@Override
		public Void visit(final FreeOp op, final Void param) {
			free(op.getClock());
			return null;
		}

		@Override
		public Void visit(final GuardOp op, final Void param) {
			and(op.getConstr());
			return null;
		}

		@Override
		public Void visit(final ResetOp op, final Void param) {
			reset(op.getClock(), op.getValue());
			return null;
		}

		@Override
		public Void visit(final ShiftOp op, final Void param) {
			shift(op.getClock(), op.getOffset());
			return null;
		}

	}

	////

	private final class AndOperationVisitor implements ClockConstrVisitor<Void, Void> {

		@Override
		public Void visit(final TrueConstr constr, final Void param) {
			return null;
		}

		@Override
		public Void visit(final UnitLtConstr constr, final Void param) {
			final int x = indexOf(constr.getClock());
			final int m = constr.getBound();
			dbm.and(x, 0, Lt(m));
			return null;
		}

		@Override
		public Void visit(final UnitLeqConstr constr, final Void param) {
			final int x = indexOf(constr.getClock());
			final int m = constr.getBound();
			dbm.and(x, 0, Leq(m));
			return null;
		}

		@Override
		public Void visit(final UnitGtConstr constr, final Void param) {
			final int x = indexOf(constr.getClock());
			final int m = constr.getBound();
			dbm.and(0, x, Lt(-m));
			return null;
		}

		@Override
		public Void visit(final UnitGeqConstr constr, final Void param) {
			final int x = indexOf(constr.getClock());
			final int m = constr.getBound();
			dbm.and(0, x, Leq(-m));
			return null;
		}

		@Override
		public Void visit(final UnitEqConstr constr, final Void param) {
			final int x = indexOf(constr.getClock());
			final int m = constr.getBound();
			dbm.and(x, 0, Leq(m));
			dbm.and(0, x, Leq(-m));
			return null;
		}

		@Override
		public Void visit(final DiffLtConstr constr, final Void param) {
			final int x = indexOf(constr.getLeftClock());
			final int y = indexOf(constr.getRightClock());
			final int m = constr.getBound();
			dbm.and(x, y, Lt(m));
			return null;
		}

		@Override
		public Void visit(final DiffLeqConstr constr, final Void param) {
			final int x = indexOf(constr.getLeftClock());
			final int y = indexOf(constr.getRightClock());
			final int m = constr.getBound();
			dbm.and(x, y, Leq(m));
			return null;
		}

		@Override
		public Void visit(final DiffGtConstr constr, final Void param) {
			final int x = indexOf(constr.getLeftClock());
			final int y = indexOf(constr.getRightClock());
			final int m = constr.getBound();
			dbm.and(y, x, Lt(-m));
			return null;
		}

		@Override
		public Void visit(final DiffGeqConstr constr, final Void param) {
			final int x = indexOf(constr.getLeftClock());
			final int y = indexOf(constr.getRightClock());
			final int m = constr.getBound();
			dbm.and(y, x, Leq(-m));
			return null;
		}

		@Override
		public Void visit(final DiffEqConstr constr, final Void param) {
			final int x = indexOf(constr.getLeftClock());
			final int y = indexOf(constr.getRightClock());
			final int m = constr.getBound();
			dbm.and(x, y, Leq(m));
			dbm.and(y, x, Leq(-m));
			return null;
		}

		@Override
		public Void visit(final AndConstr constr, final Void param) {
			for (final ClockConstr atomicConstr : constr.getConstrs()) {
				atomicConstr.accept(this, param);
				if (!dbm.isConsistent()) {
					return null;
				}
			}
			return null;
		}
	}

}
