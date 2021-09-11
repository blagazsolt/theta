/*
 * Copyright 2021 Budapest University of Technology and Economics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.bme.mit.theta.xcfa.passes.procedurepass;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.AssignStmt;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.stmt.HavocStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.xcfa.model.XcfaEdge;
import hu.bme.mit.theta.xcfa.model.XcfaLabel;
import hu.bme.mit.theta.xcfa.model.XcfaProcedure;
import hu.bme.mit.theta.xcfa.model.utils.XcfaLabelVarReplacer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static hu.bme.mit.theta.core.stmt.Stmts.Havoc;
import static hu.bme.mit.theta.xcfa.model.XcfaLabel.Stmt;
import static hu.bme.mit.theta.xcfa.passes.procedurepass.Utils.getVars;

public class HavocPromotion extends ProcedurePass {
	private Map<XcfaEdge, XcfaEdge> forwardLut;
	private Map<XcfaEdge, XcfaEdge> reverseLut;
	@Override
	public XcfaProcedure.Builder run(XcfaProcedure.Builder builder) {
		forwardLut = new LinkedHashMap<>();
		reverseLut = new LinkedHashMap<>();
		Map<VarDecl<?>, Set<Tuple2<XcfaEdge, XcfaLabel>>> varUsage = new LinkedHashMap<>();
		for (XcfaEdge edge : builder.getEdges()) {
			for (XcfaLabel stmt : edge.getLabels()) {
				Tuple2<XcfaEdge, XcfaLabel> tup = Tuple2.of(edge, stmt);
				Set<VarDecl<?>> vars = getVars(stmt);
				for (VarDecl<?> var : vars) {
					varUsage.putIfAbsent(var, new LinkedHashSet<>());
					varUsage.get(var).add(tup);
				}
			}
		}

		boolean found = true;
		while(found) {
			found = false;
			for (XcfaEdge edge : new LinkedHashSet<>(builder.getEdges())) {
				for (XcfaLabel label : edge.getLabels()) {
					if(label instanceof XcfaLabel.StmtXcfaLabel) {
						Stmt stmt = label.getStmt();
						if (stmt instanceof HavocStmt) {
							VarDecl<?> toRemove = ((HavocStmt<?>) stmt).getVarDecl();
							if (varUsage.get(toRemove).size() == 3 && varUsage.get(toRemove).stream().allMatch(objects -> getEdge(objects.get1()).equals(edge))) { // havoc, assume, assign
								Optional<? extends AssignStmt<?>> assign = varUsage.get(toRemove).stream().filter(objects -> objects.get2() instanceof XcfaLabel.StmtXcfaLabel && objects.get2().getStmt() instanceof AssignStmt).map(objects -> (AssignStmt<?>) objects.get2().getStmt()).findAny();
								Optional<AssumeStmt> assume = varUsage.get(toRemove).stream().filter(objects -> objects.get2() instanceof XcfaLabel.StmtXcfaLabel && objects.get2().getStmt() instanceof AssumeStmt).map(objects -> (AssumeStmt) objects.get2().getStmt()).findAny();
								if (assign.isPresent() && assume.isPresent()) {
									if (replaceStmt(builder, edge, List.of(Stmt(stmt), Stmt(assume.get()), Stmt(assign.get())), List.of(Stmt(Havoc(assign.get().getVarDecl())), Stmt(assume.get()).accept(new XcfaLabelVarReplacer(), Map.of(((HavocStmt<?>) stmt).getVarDecl(), assign.get().getVarDecl()))))) {
										found = true;
										break;
									}
								}
							}
							if (varUsage.get(toRemove).size() == 2 && varUsage.get(toRemove).stream().allMatch(objects -> getEdge(objects.get1()).equals(edge))) { // havoc, assign
								Optional<? extends AssignStmt<?>> assign = varUsage.get(toRemove).stream().filter(objects -> objects.get2() instanceof XcfaLabel.StmtXcfaLabel && objects.get2().getStmt() instanceof AssignStmt).map(objects -> (AssignStmt<?>) objects.get2().getStmt()).findAny();
								if (assign.isPresent()) {
									if (replaceStmt(builder, edge, List.of(Stmt(stmt), Stmt(assign.get())), List.of(Stmt(Havoc(assign.get().getVarDecl()))))) {
										found = true;
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		return builder;
	}


	private XcfaEdge getEdge(XcfaEdge oldEdge) {
		return forwardLut.getOrDefault(oldEdge, oldEdge);
	}

	private boolean replaceStmt(XcfaProcedure.Builder builder, XcfaEdge edge, List<XcfaLabel> oldStmts, List<XcfaLabel> newStmts) {
		List<XcfaLabel> stmts = new ArrayList<>();
		int i = 0;
		for (XcfaLabel stmt : edge.getLabels()) {
			if(oldStmts.size() > i && stmt == oldStmts.get(i)) {
				if(newStmts.size() > i) stmts.add(newStmts.get(i));
				++i;
			}
			else stmts.add(stmt);
		}
		if(i == oldStmts.size()) {
			builder.removeEdge(edge);
			XcfaEdge newEdge = XcfaEdge.of(edge.getSource(), edge.getTarget(), stmts);
			builder.addEdge(newEdge);
			XcfaEdge originalEdge = reverseLut.getOrDefault(edge, edge);
			forwardLut.put(originalEdge, newEdge);
			reverseLut.put(newEdge, originalEdge);
			return true;
		}
		return false;
	}



	@Override
	public boolean isPostInlining() {
		return true;
	}
}
