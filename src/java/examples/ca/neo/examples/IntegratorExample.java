/*
 * Created on 14-Jun-2006
 */
package ca.neo.examples;

import java.io.File;
import java.io.IOException;

import ca.neo.io.MatlabExporter;
import ca.neo.math.Function;
import ca.neo.math.impl.ConstantFunction;
import ca.neo.model.Ensemble;
import ca.neo.model.Network;
import ca.neo.model.SimulationException;
import ca.neo.model.SimulationMode;
import ca.neo.model.StructuralException;
import ca.neo.model.Termination;
import ca.neo.model.Units;
import ca.neo.model.impl.EnsembleFactory;
import ca.neo.model.impl.FunctionInput;
import ca.neo.model.impl.NetworkImpl;
import ca.neo.model.nef.NEFEnsemble;
import ca.neo.plot.Plotter;
import ca.neo.sim.Simulator;
import ca.neo.sim.impl.LocalSimulator;
import ca.neo.util.Recorder;

public class IntegratorExample {

	public static Network createNetwork() throws StructuralException {
		
		Network network = new NetworkImpl();
		
		Function f = new ConstantFunction(1, 1f);
//		Function f = new SineFunction();
		FunctionInput input = new FunctionInput("input", new Function[]{f}, Units.UNK);
		network.addNode(input);
		
		EnsembleFactory ef = new EnsembleFactory();
		
		NEFEnsemble integrator = ef.make("integrator", 500, 1, "integrator1", false);  
		network.addNode(integrator);
		integrator.collectSpikes(true);

		Plotter.plot(integrator);
		Plotter.plot(integrator, NEFEnsemble.X);
		
		float tau = .05f; 
		
		Termination interm = integrator.addDecodedTermination("input", new float[][]{new float[]{tau}}, tau, false);
//		Termination interm = integrator.addDecodedTermination("input", new float[][]{new float[]{1f}}, tau);
		network.addProjection(input, interm);
		
		Termination fbterm = integrator.addDecodedTermination("feedback", new float[][]{new float[]{1f}}, tau, false);
		network.addProjection(integrator.getOrigin(NEFEnsemble.X), fbterm);
		
		//System.out.println("Network creation: " + (System.currentTimeMillis() - start));
		return network;
	}
		
	
	public static void main(String[] args) {
				
		try {
			Network network = createNetwork();
			Simulator simulator = new LocalSimulator();
			simulator.initialize(network); 
			
			Recorder inputRecorder = simulator.addRecorder("input", "input");
			Recorder integratorRecorder = simulator.addRecorder("integrator", NEFEnsemble.X);
			Recorder neuronRecorder = simulator.addRecorder("integrator", 0, "V");

			long startTime = System.currentTimeMillis();
			simulator.run(0f, 1f, .0002f, SimulationMode.DEFAULT);
			System.out.println("Run time: " + ((System.currentTimeMillis() - startTime)/1000f) );
		
			Plotter.plot(inputRecorder.getData(), "Input");
			Plotter.plot(integratorRecorder.getData(), .005f, "Integrator");
			Plotter.plot(neuronRecorder.getData(), "Neuron #0");
			
			Plotter.plot(((Ensemble) network.getNode("integrator")).getSpikePattern());
			
			MatlabExporter me = new MatlabExporter();
			me.add("input", inputRecorder.getData());
			me.add("integrator", integratorRecorder.getData(), .01f);
			me.add("neuron", neuronRecorder.getData());
			me.write(new File("export.mat"));
			
		} catch (SimulationException e) {
			e.printStackTrace();
		} catch (StructuralException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
