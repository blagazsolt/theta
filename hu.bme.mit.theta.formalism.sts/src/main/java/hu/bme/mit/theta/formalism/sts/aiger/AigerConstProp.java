/*
 *  Copyright 2017 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hu.bme.mit.theta.formalism.sts.aiger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import hu.bme.mit.theta.formalism.sts.aiger.elements.AigerNode;
import hu.bme.mit.theta.formalism.sts.aiger.elements.AigerSystem;
import hu.bme.mit.theta.formalism.sts.aiger.elements.AigerWire;
import hu.bme.mit.theta.formalism.sts.aiger.elements.AndGate;
import hu.bme.mit.theta.formalism.sts.aiger.elements.FalseConst;
import hu.bme.mit.theta.formalism.sts.aiger.elements.Latch;

public class AigerConstProp {

	private AigerConstProp() {
	}

	public static void apply(final AigerSystem system) {

		final Optional<AigerNode> falseOpt = system.getNodes().stream().filter(n -> n instanceof FalseConst)
				.findFirst();
		if (!falseOpt.isPresent()) {
			return;
		}

		final FalseConst falseConst = (FalseConst) falseOpt.get();

		while (propagateAnd(system, falseConst) || propagateLatch(system, falseConst)) {

		}
	}

	private static boolean propagateAnd(final AigerSystem system, final FalseConst falseConst) {
		final Optional<AigerWire> wireOpt = falseConst.getOutWires().stream()
				.filter(w -> w.getTarget() instanceof AndGate).findFirst();
		if (wireOpt.isPresent()) {
			final AigerWire wire = wireOpt.get();
			final AndGate andGate = (AndGate) wire.getTarget();
			final AigerWire otherWire = andGate.getInWire1().equals(wire) ? andGate.getInWire2() : andGate.getInWire1();
			final AigerNode otherSource = otherWire.getSource();
			otherSource.removeOutWire(otherWire);
			final List<AigerWire> redirectedWires = new ArrayList<>();
			redirectedWires.addAll(andGate.getOutWires());
			if (wire.isPonated()) {
				redirectedWires.forEach(w -> w.modifySource(falseConst));
			} else {
				redirectedWires.forEach(w -> w.modifySource(otherSource));
				if (!otherWire.isPonated()) {
					redirectedWires.forEach(w -> w.invert());
				}
			}
			system.getNodes().remove(andGate);
			falseConst.removeOutWire(wire);
			return true;
		} else {
			return false;
		}
	}

	private static boolean propagateLatch(final AigerSystem system, final FalseConst falseConst) {
		final Optional<AigerWire> wireOpt = falseConst.getOutWires().stream()
				.filter(w -> w.getTarget() instanceof Latch && w.isPonated()).findFirst();
		if (wireOpt.isPresent()) {
			final AigerWire wire = wireOpt.get();
			final Latch latch = (Latch) wire.getTarget();
			final List<AigerWire> redirectedWires = new ArrayList<>();
			redirectedWires.addAll(latch.getOutWires());
			redirectedWires.forEach(w -> w.modifySource(falseConst));
			system.getNodes().remove(latch);
			falseConst.removeOutWire(wire);
			return true;
		} else {
			return false;
		}
	}
}
