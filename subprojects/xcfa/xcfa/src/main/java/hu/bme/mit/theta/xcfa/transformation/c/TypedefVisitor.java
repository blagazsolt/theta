package hu.bme.mit.theta.xcfa.transformation.c;

import hu.bme.mit.theta.xcfa.dsl.gen.CBaseVisitor;
import hu.bme.mit.theta.xcfa.dsl.gen.CParser;
import hu.bme.mit.theta.xcfa.transformation.c.declaration.CDeclaration;
import hu.bme.mit.theta.xcfa.transformation.c.types.CType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TypedefVisitor extends CBaseVisitor<List<CDeclaration>> {
	public static final TypedefVisitor instance = new TypedefVisitor();
	private final List<CDeclaration> declarations = new ArrayList<>();

	public Optional<CType> getType(String id) {
		return declarations.stream().filter(cDeclaration -> cDeclaration.getName().equals(id)).map(CDeclaration::getBaseType).findFirst();
	}

	@Override
	public List<CDeclaration> visitDeclaration(CParser.DeclarationContext ctx) {
		if(ctx.declarationSpecifiers().declarationSpecifier(0).getText().equals("typedef")) {
			throw new UnsupportedOperationException("Not yet implemented (local typedef)");
		} else return null;
	}

	@Override
	public List<CDeclaration> visitGlobalDeclaration(CParser.GlobalDeclarationContext ctx) {
		List<CDeclaration> ret = new ArrayList<>();
		if(ctx.declaration().declarationSpecifiers().declarationSpecifier(0).getText().equals("typedef")) {
			List<CDeclaration> declarations = DeclarationVisitor.instance.getDeclarations(ctx.declaration().declarationSpecifiers(), ctx.declaration().initDeclaratorList());
			ret.addAll(declarations);
			return ret;
		}
		return null;
	}

	@Override
	public List<CDeclaration> visitCompilationUnit(CParser.CompilationUnitContext ctx) {
		CParser.TranslationUnitContext translationUnitContext = ctx.translationUnit();
		if(translationUnitContext!=null) {
			for (CParser.ExternalDeclarationContext externalDeclarationContext : translationUnitContext.externalDeclaration()) {
				List<CDeclaration> declList = externalDeclarationContext.accept(this);
				if(declList!=null) {
					declarations.addAll(declList);
				}
			}
		}
		return declarations;
	}
}
