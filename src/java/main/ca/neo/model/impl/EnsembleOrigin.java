/*
 * Created on 31-May-2006
 */
package ca.neo.model.impl;

import ca.neo.model.InstantaneousOutput;
import ca.neo.model.Origin;
import ca.neo.model.RealOutput;
import ca.neo.model.SimulationException;
import ca.neo.model.SpikeOutput;
import ca.neo.model.Units;

/**
 * An Origin that is composed of the Origins of multiple Nodes. The dimension 
 * of this Origin equals the number of Nodes. All the Nodes must produce the 
 * same type of output (RealOutput or SpikeOutput) with the same Unit at the same 
 * time (these things can change in subsequent time steps, but they must change 
 * together for all Nodes). 
 *   
 * @author Bryan Tripp
 */
public class EnsembleOrigin implements Origin {

	private static final long serialVersionUID = 1L;
	
	private Origin[] myNodeOrigins;
	private String myName;
	
	/**
	 * @param name Name of this Origin 
	 * @param nodeOrigins Origins on individual Nodes that are combined to make this 
	 * 		Origin. Each of these is expected to have dimension 1, but this is not enforced. 
	 * 		Other dimensions are ignored. 
	 */
	public EnsembleOrigin(String name, Origin[] nodeOrigins) {
		myNodeOrigins = nodeOrigins;
		myName = name;
	}

	/**
	 * @see ca.neo.model.Origin#getName()
	 */
	public String getName() {
		return myName;
	}

	/**
	 * @see ca.neo.model.Origin#getDimensions()
	 */
	public int getDimensions() {
		return myNodeOrigins.length;
	}

	/**
	 * @return A composite of the first-dimensional outputs of all the Node Origins
	 * 		that make up the EnsembleOrigin. Node Origins should normally have 
	 * 		dimension 1, but this isn't enforced here. All Node Origins must have 
	 * 		the same units, and must output the same type of InstantaneousOuput (ie 
	 * 		either SpikeOutput or RealOutput), otherwise an exception is thrown.   
	 * @see ca.neo.model.Origin#getValues()
	 */
	public InstantaneousOutput getValues() throws SimulationException {
		InstantaneousOutput result = null;
		
		Units units = myNodeOrigins[0].getValues().getUnits(); //must be same for all
		
		if (myNodeOrigins[0].getValues() instanceof RealOutput) {
			result = composeRealOutput(myNodeOrigins, units);
		} else if (myNodeOrigins[0].getValues() instanceof SpikeOutput) {
			result = composeSpikeOutput(myNodeOrigins, units);			
		}
		
		return result;
	}
	
	private static RealOutput composeRealOutput(Origin[] origins, Units units) throws SimulationException {
		float[] values = new float[origins.length];
		
		for (int i = 0; i < origins.length; i++) {
			InstantaneousOutput o = origins[i].getValues();
			if ( !(o instanceof RealOutput) ) {
				throw new SimulationException("Some of the Node Origins are not producing real-valued output");
			}
			if ( !o.getUnits().equals(units) ) {
				throw new SimulationException("Some of the Node Origins are producing outputs with non-matching units");
			}
			
			values[i] = ((RealOutput) o).getValues()[0];
		}
		
		return new RealOutputImpl(values, units, origins[0].getValues().getTime());
	}
	
	private static SpikeOutput composeSpikeOutput(Origin[] origins, Units units) throws SimulationException {
		boolean[] values = new boolean[origins.length];
		
		for (int i = 0; i < origins.length; i++) {
			InstantaneousOutput o = origins[i].getValues();
			if ( !(o instanceof SpikeOutput) ) {
				throw new SimulationException("Some of the Node Origins are not producing spiking output");
			}
			if ( !o.getUnits().equals(units) ) {
				throw new SimulationException("Some of the Node Origins are producing outputs with non-matching units");
			}
			
			values[i] = ((SpikeOutput) o).getValues()[0];
		}
		
		return new SpikeOutputImpl(values, units, origins[0].getValues().getTime());
	}

}
