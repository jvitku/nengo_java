/*
The contents of this file are subject to the Mozilla Public License Version 1.1
(the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific
language governing rights and limitations under the License.

The Original Code is "LinearSynapticIntegrator.java". Description:
"A basic linear SynapticIntegrator model.

  Synaptic inputs are individually weighted, passed through decaying
  exponential dynamics, and summed"

The Initial Developer of the Original Code is Bryan Tripp & Centre for Theoretical Neuroscience, University of Waterloo. Copyright (C) 2006-2008. All Rights Reserved.

Alternatively, the contents of this file may be used under the terms of the GNU
Public License license (the GPL License), in which case the provisions of GPL
License are applicable  instead of those above. If you wish to allow use of your
version of this file only under the terms of the GPL License and not to allow
others to use your version of this file under the MPL, indicate your decision
by deleting the provisions above and replace  them with the notice and other
provisions required by the GPL License.  If you do not delete the provisions above,
a recipient may use your version of this file under either the MPL or the GPL License.
*/

/*
 * Created on May 4, 2006
 */
package ca.nengo.model.neuron.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ca.nengo.model.Node;
import ca.nengo.model.SimulationException;
import ca.nengo.model.StructuralException;
import ca.nengo.model.Termination;
import ca.nengo.model.Units;
import ca.nengo.model.impl.LinearExponentialTermination;
import ca.nengo.model.neuron.ExpandableSynapticIntegrator;
import ca.nengo.model.neuron.SynapticIntegrator;
import ca.nengo.util.TimeSeries1D;
import ca.nengo.util.impl.TimeSeries1DImpl;

/**
 * <p>A basic linear <code>SynapticIntegrator</code> model.</p>
 *
 * <p>Synaptic inputs are individually weighted, passed through decaying
 * exponential dynamics, and summed.</p>
 *
 * <p>A synaptic weight corresponds to the time integral of the current induced by
 * one spike, or to the time integral of current induced by a real-valued input of
 * 1 over 1 second. Thus a real-valued firing-rate input has roughly the same effect
 * as a series spikes at the same rate. So a simulation can switch between spike
 * and rate inputs, with minimal impact and without the need to modify synaptic
 * weights. </p>
 *
 * @author Bryan Tripp
 */
public class LinearSynapticIntegrator implements ExpandableSynapticIntegrator {

	private static final long serialVersionUID = 1L;

	private static final float ourTimeStepCorrection = 1.01f;

	private Node myNode;
	private float myMaxTimeStep;
	private Units myCurrentUnits;
	private Map<String, LinearExponentialTermination> myTerminations;

	/**
	 * @param maxTimeStep Maximum length of integration time step. Shorter steps may be used to better match
	 * 		length of run(...)
	 * @param currentUnits Units of current in input weights, scale, bias, and result of run(...)
	 */
	public LinearSynapticIntegrator(float maxTimeStep, Units currentUnits) {
		myMaxTimeStep = maxTimeStep * 1.01f; //increased slightly because float/float != integer
		myCurrentUnits = currentUnits;
		myTerminations = new HashMap<String, LinearExponentialTermination>(10);
	}

	/**
	 * Defaults to max timestep 1ms and units Units.ACU.
	 */
	public LinearSynapticIntegrator() {
		this(.001f, Units.ACU);
	}

	/**
	 * @see ca.nengo.model.neuron.SynapticIntegrator#run(float, float)
	 */
	public TimeSeries1D run(float startTime, float endTime) throws SimulationException {
		float len = endTime - startTime;
		int steps = (int) Math.ceil(len / myMaxTimeStep);
		float dt = len / steps;

		float[] times = new float[steps+1];
		float[] currents = new float[steps+1];

		times[0] = startTime;
		if (myTerminations.size() == 0) {
			for (int i = 1; i <= steps; i++) {
				times[i] = startTime + i * dt;
			}
		} else {
			//Note: we leave out decay and real input integration at start time, to make total
			//decay and integration times equal to simulation time (previously left integration out of
			//end step, but some spike generators need accurate value at end time)

			times[0] = startTime;
			currents[0] = update(myTerminations.values(), true, 0, 0);

			for (int i = 1; i <= steps; i++) {
				times[i] = startTime + i * dt;
				currents[i] = update(myTerminations.values(), false, dt, dt);
			}
		}

		return new TimeSeries1DImpl(times, currents, myCurrentUnits);
	}

	//update current in all Terminations
	private static float update(Collection<LinearExponentialTermination> terminations, boolean spikes, float intTime, float decayTime) {
		float result = 0f;

		Iterator<LinearExponentialTermination> it = terminations.iterator();
		while (it.hasNext()) {
			LinearExponentialTermination t = it.next();
			float current = t.updateCurrent(spikes, intTime, decayTime);
			if (!t.getModulatory()) {
                result += current;
            }
		}

		return result;
	}

	/**
	 * @see ca.nengo.model.Resettable#reset(boolean)
	 */
	public void reset(boolean randomize) {
		Iterator<LinearExponentialTermination> it = myTerminations.values().iterator();
		while (it.hasNext()) {
			it.next().reset(false);
		}
	}

	public float getMaxTimeStep() {
		return myMaxTimeStep / ourTimeStepCorrection;
	}

	public void setMaxTimeStep(float maxTimeStep) {
		myMaxTimeStep = maxTimeStep * ourTimeStepCorrection; //increased slightly because float/float != integer
	}

	public Units getCurrentUnits() {
		return myCurrentUnits;
	}

	public void setCurrentUnits(Units units) {
		myCurrentUnits = units;
	}

	/**
	 * @see ca.nengo.model.neuron.SynapticIntegrator#getTerminations()
	 */
	public Termination[] getTerminations() {
		return myTerminations.values().toArray(new Termination[0]);
	}

	/**
	 * @see ca.nengo.model.neuron.ExpandableSynapticIntegrator#addTermination(java.lang.String, float[], float, boolean)
	 */
	public Termination addTermination(String name, float[] weights, float tauPSC, boolean modulatory) throws StructuralException {
		if (myTerminations.containsKey(name)) {
			throw new StructuralException("This SynapticIntegrator already has a Termination named " + name);
		}

		LinearExponentialTermination result = new LinearExponentialTermination(myNode, name, weights, tauPSC);
		result.setModulatory(modulatory);
		myTerminations.put(name, result);

		return result;
	}

	/**
	 * @see ca.nengo.model.neuron.ExpandableSynapticIntegrator#removeTermination(java.lang.String)
	 */
	public Termination removeTermination(String name) throws StructuralException {
		return myTerminations.remove(name);
	}

	/**
	 * @see ca.nengo.model.neuron.SynapticIntegrator#getTermination(java.lang.String)
	 */
	public Termination getTermination(String name) throws StructuralException {
		return myTerminations.get(name);
	}

	/**
	 * @param node The parent node (Terminations need a reference to this)
	 */
	public void setNode(Node node) {
		myNode = node;

		for (LinearExponentialTermination t : myTerminations.values()) {
			t.setNode(myNode);
		}
	}

	@Override
	public SynapticIntegrator clone() throws CloneNotSupportedException {
		LinearSynapticIntegrator result = (LinearSynapticIntegrator) super.clone();

		result.myTerminations = new HashMap<String, LinearExponentialTermination>(10);
		for (LinearExponentialTermination oldTerm : myTerminations.values()) {
			String name = oldTerm.getName();
			LinearExponentialTermination newTerm = (LinearExponentialTermination) oldTerm.clone();
			newTerm.setNode(result.myNode);
			result.myTerminations.put(name, newTerm);
		}

		return result;
	}


	public static class Factory implements SynapticIntegratorFactory {

		private static final long serialVersionUID = 1L;

		private Units myUnits;
		private float myMaxTimeStep;

		public Factory() {
			myUnits = Units.ACU;
			myMaxTimeStep = .0005f;
		}

		/**
		 * @return Units of output current value
		 */
		public Units getUnits() {
			return myUnits;
		}

		/**
		 * @param units Units of output current value
		 */
		public void setUnits(Units units){
			myUnits = units;
		}

		/**
		 * @return Maximum time step taken by the synaptic integrators produced here,
		 * 		regardless of network time step
		 */
		public float getMaxTimeStep() {
			return myMaxTimeStep;
		}

		/**
		 * @param maxTimeStep Maximum time step taken by the synaptic integrators produced here,
		 * 		regardless of network time step
		 */
		public void setMaxTimeStep(float maxTimeStep) {
			myMaxTimeStep = maxTimeStep;
		}

		/**
		 * @see ca.nengo.model.neuron.impl.SynapticIntegratorFactory#make()
		 */
		public SynapticIntegrator make() {
			return new LinearSynapticIntegrator(myMaxTimeStep, myUnits);
		}

	}

}
