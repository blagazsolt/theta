package hu.bme.mit.theta.core.type.fptype;

import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.BinaryExpr;
import hu.bme.mit.theta.core.type.Expr;

import static hu.bme.mit.theta.core.utils.TypeUtils.castFp;
import static hu.bme.mit.theta.core.utils.TypeUtils.checkAllTypesEqual;

public final class FpDivExpr extends BinaryExpr<FpType, FpType> {
    private static final int HASH_SEED = 1646;
    private static final String OPERATOR = "fpdiv";

    private final FpRoundingMode roundingMode;

    private FpDivExpr(final FpRoundingMode roundingMode, final Expr<FpType> leftOp, final Expr<FpType> rightOp) {
        super(leftOp, rightOp);
        checkAllTypesEqual(leftOp, rightOp);
        this.roundingMode = roundingMode;
    }

    public static FpDivExpr of(final FpRoundingMode roundingMode, final Expr<FpType> leftOp, final Expr<FpType> rightOp) {
        return new FpDivExpr(roundingMode, leftOp, rightOp);
    }

    public static FpDivExpr create(final FpRoundingMode roundingMode, final Expr<?> leftOp, final Expr<?> rightOp) {
        final Expr<FpType> newLeftOp = castFp(leftOp);
        final Expr<FpType> newRightOp = castFp(rightOp);
        return FpDivExpr.of(roundingMode, newLeftOp, newRightOp);
    }

    public FpRoundingMode getRoundingMode() {
        return roundingMode;
    }

    @Override
    public FpType getType() {
        return getOps().get(0).getType();
    }

    @Override
    public FpLitExpr eval(final Valuation val) {
        final FpLitExpr leftOpVal = (FpLitExpr) getLeftOp().eval(val);
        final FpLitExpr rightOpVal = (FpLitExpr) getRightOp().eval(val);

        return leftOpVal.div(roundingMode, rightOpVal);
    }

    @Override
    public FpDivExpr with(final Expr<FpType> leftOp, final Expr<FpType> rightOp) {
        if (leftOp == getLeftOp() && rightOp == getRightOp()) {
            return this;
        } else {
            return FpDivExpr.of(roundingMode, leftOp, rightOp);
        }
    }

    @Override
    public FpDivExpr withLeftOp(final Expr<FpType> leftOp) {
        return with(leftOp, getRightOp());
    }

    @Override
    public FpDivExpr withRightOp(final Expr<FpType> rightOp) {
        return with(getLeftOp(), rightOp);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof FpDivExpr) {
            final FpDivExpr that = (FpDivExpr) obj;
            return this.getLeftOp().equals(that.getLeftOp()) && this.getRightOp().equals(that.getRightOp());
        } else {
            return false;
        }
    }

    @Override
    protected int getHashSeed() {
        return HASH_SEED;
    }

    @Override
    public String getOperatorLabel() {
        return OPERATOR;
    }
}
