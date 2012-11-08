/*
The contents of this file are subject to the Mozilla Public License Version 1.1
(the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific
language governing rights and limitations under the License.

The Original Code is "ProjectionImpl.java". Description:
"Default implementation of Projection.

  TODO: unit tests

  @author Bryan Tripp"

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
 * Created on May 5, 2006
 */
package ca.nengo.model.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ca.nengo.math.Function;
import ca.nengo.math.impl.IdentityFunction;
import ca.nengo.math.impl.IndicatorPDF;
import ca.nengo.math.impl.PostfixFunction;
import ca.nengo.model.Network;
import ca.nengo.model.impl.NetworkImpl.OriginWrapper;
import ca.nengo.model.impl.NetworkImpl.TerminationWrapper;
import ca.nengo.model.Node;
import ca.nengo.model.Origin;
import ca.nengo.model.Projection;
import ca.nengo.model.StructuralException;
import ca.nengo.model.Termination;
import ca.nengo.model.nef.NEFEnsemble;
import ca.nengo.model.nef.impl.BiasOrigin;
import ca.nengo.model.nef.impl.BiasTermination;
import ca.nengo.model.nef.impl.DecodedOrigin;
import ca.nengo.model.nef.impl.DecodedTermination;
import ca.nengo.util.MU;
import ca.nengo.util.ScriptGenException;

/**
 * Default implementation of <code>Projection</code>.
 *
 * TODO: unit tests
 *
 * @author Bryan Tripp
 */
public class ProjectionImpl implements Projection {

	private static final long serialVersionUID = 1L;

	private Origin myOrigin;
	private Termination myTermination;
	private Network myNetwork;

	private boolean myBiasIsEnabled;
	private NEFEnsemble myInterneurons;
	private BiasOrigin myBiasOrigin;
	private BiasTermination myDirectBT;
	private BiasTermination myIndirectBT;
	private DecodedTermination myInterneuronTermination;

	/**
	 * @param origin  The Origin at the start of this Projection
	 * @param termination  The Termination at the end of this Projection
	 * @param network The Network of which this Projection is a part
	 */
	public ProjectionImpl(Origin origin, Termination termination, Network network) {
		myOrigin = origin;
		myTermination = termination;
		myNetwork = network;

		myBiasIsEnabled = false;
		myInterneurons = null;
		myDirectBT = null;
		myIndirectBT = null;
	}

	/**
	 * @see ca.nengo.model.Projection#getOrigin()
	 */
	public Origin getOrigin() {
		return myOrigin;
	}

	/**
	 * @see ca.nengo.model.Projection#getTermination()
	 */
	public Termination getTermination() {
		return myTermination;
	}

	/**
	 * @see ca.nengo.model.Projection#biasIsEnabled()
	 */
	public boolean biasIsEnabled() {
		return myBiasIsEnabled;
	}

	/**
	 * @see ca.nengo.model.Projection#enableBias(boolean)
	 */
	public void enableBias(boolean enable) {
		if (myInterneurons != null) {
			myDirectBT.setEnabled(enable);
			myIndirectBT.setEnabled(enable);
			myBiasIsEnabled = enable;
		}
	}

	/**
	 * @see ca.nengo.model.Projection#getNetwork()
	 */
	public Network getNetwork() {
		return myNetwork;
	}

	/**
	 * @throws StructuralException if the origin and termination are not decoded
	 * @see ca.nengo.model.Projection#addBias(int, float, float, boolean, boolean)
	 */
	public void addBias(int numInterneurons, float tauInterneurons, float tauBias, boolean excitatory, boolean optimize) throws StructuralException {
		if ( !(myOrigin instanceof DecodedOrigin) || !(myTermination instanceof DecodedTermination)) {
			throw new RuntimeException("This feature is only implemented for projections from DecodedOrigins to DecodedTerminations");
		}

		DecodedOrigin baseOrigin = (DecodedOrigin) myOrigin;
		DecodedTermination baseTermination = (DecodedTermination) myTermination;
		NEFEnsemble pre = (NEFEnsemble) baseOrigin.getNode();
		NEFEnsemble post = (NEFEnsemble) baseTermination.getNode();

		myBiasOrigin = pre.addBiasOrigin(baseOrigin, numInterneurons, getUniqueNodeName(post.getName() + ":" + baseTermination.getName()), excitatory);
		myInterneurons = myBiasOrigin.getInterneurons();
		myNetwork.addNode(myInterneurons);
		BiasTermination[] bt = post.addBiasTerminations(baseTermination, tauBias, myBiasOrigin.getDecoders(), baseOrigin.getDecoders());
		myDirectBT = bt[0];
		myIndirectBT = bt[1];
		if (!excitatory) {
            myIndirectBT.setStaticBias(new float[]{-1});
        }
		float[][] tf = new float[][]{new float[]{0, 1/tauInterneurons/tauInterneurons}, new float[]{2/tauInterneurons, 1/tauInterneurons/tauInterneurons}};
		myInterneuronTermination = (DecodedTermination) myInterneurons.addDecodedTermination("bias", MU.I(1), tf[0], tf[1], 0, false);

		myNetwork.addProjection(myBiasOrigin, myDirectBT);
		myNetwork.addProjection(myBiasOrigin, myInterneuronTermination);
		myNetwork.addProjection(myInterneurons.getOrigin(NEFEnsemble.X), myIndirectBT);

		if (optimize) {
			float[][] baseWeights = MU.prod(post.getEncoders(), MU.prod(baseTermination.getTransform(), MU.transpose(baseOrigin.getDecoders())));
			myBiasOrigin.optimizeDecoders(baseWeights, myDirectBT.getBiasEncoders(), excitatory);
			myBiasOrigin.optimizeInterneuronDomain(myInterneuronTermination, myIndirectBT);
		}

		myBiasIsEnabled = true;
	}

	private String getUniqueNodeName(String base) {
		String result = base;
		boolean done = false;
		int c = 2;
		Node[] nodes = myNetwork.getNodes();
		while (!done) {
			done = true;
			for (Node node : nodes) {
				if (node.getName().equals(result)) {
					done = false;
					result = base + c++;
				}
			}
		}
		return result;
	}

	/**
	 * @see ca.nengo.model.Projection#removeBias()
	 */
	public void removeBias() {
		try {
			DecodedOrigin baseOrigin = (DecodedOrigin) myOrigin;
			DecodedTermination baseTermination = (DecodedTermination) myTermination;
			NEFEnsemble pre = (NEFEnsemble) baseOrigin.getNode();
			NEFEnsemble post = (NEFEnsemble) baseTermination.getNode();

			myNetwork.removeProjection(myDirectBT);
			myNetwork.removeProjection(myIndirectBT);
			myNetwork.removeProjection(myInterneuronTermination);
			myNetwork.removeNode(myInterneurons.getName());

			pre.removeDecodedOrigin(myBiasOrigin.getName());
			post.removeDecodedTermination(myDirectBT.getName());
			post.removeDecodedTermination(myIndirectBT.getName());

			myBiasIsEnabled = false;
		} catch (StructuralException e) {
			throw new RuntimeException("Error while trying to remove bias (this is probably a bug in ProjectionImpl)", e);
		}
	}

	/**
	 * @see ca.nengo.model.Projection#getWeights()
	 */
	public float[][] getWeights() {
		float[][] result = null;

		if ( (myOrigin instanceof DecodedOrigin) && (myTermination instanceof DecodedTermination)) {
			float[][] encoders = ((NEFEnsemble) myTermination.getNode()).getEncoders();
			float[][] transform = ((DecodedTermination) myTermination).getTransform();
			float[][] decoders = ((DecodedOrigin) myOrigin).getDecoders();
			result = MU.prod(encoders, MU.prod(transform, MU.transpose(decoders)));

			if (myBiasIsEnabled) {
				float[] biasEncoders = myDirectBT.getBiasEncoders();
				float[][] biasDecoders = myBiasOrigin.getDecoders();
				float[][] weightBiases = MU.prod(MU.transpose(new float[][]{biasEncoders}), MU.transpose(biasDecoders));
				result = MU.sum(result, weightBiases);
			}
		} else if (myTermination instanceof DecodedTermination) {
			float[][] encoders = ((NEFEnsemble) myTermination.getNode()).getEncoders();
			float[][] transform = ((DecodedTermination) myTermination).getTransform();
			result = MU.prod(encoders, transform);
		} else {
			//TODO: add getWeights() to Termination, implement in EnsembleTermination from LinearExponentialTermination.getWeights()
			throw new RuntimeException("Not implemented for non-DecodedTerminations");
		}

		return result;
	}
	
	public String toScript(HashMap<String, Object> scriptData) throws ScriptGenException {
		
	    StringBuilder py = new StringBuilder();
	    
	    String pythonNetworkName = scriptData.get("prefix") 
	    			+ getNetwork().getName().replace(' ', ((Character)scriptData.get("spaceDelim")).charValue());
	    
	    py.append(String.format("%1s.connect(", pythonNetworkName));
	    
	    StringBuilder originNodeFullName = new StringBuilder();
	    Origin tempOrigin = myOrigin;

	    while(tempOrigin instanceof OriginWrapper)
	    {
	    	originNodeFullName.append(tempOrigin.getNode().getName() + ".");
	    	tempOrigin = ((OriginWrapper) tempOrigin).getWrappedOrigin();
	    }
	    
	    originNodeFullName.append(tempOrigin.getNode().getName());
	    
	    py.append("\'" + originNodeFullName + "\'");
	    
	    
	    
	    StringBuilder terminationNodeFullName = new StringBuilder();
	    Termination tempTermination = myTermination;

	    while(tempTermination instanceof TerminationWrapper)
	    {
	    	terminationNodeFullName.append(tempTermination.getNode().getName() + ".");
	    	tempTermination = ((TerminationWrapper) tempTermination).getWrappedTermination();
	    }
	    
	    terminationNodeFullName.append(tempTermination.getNode().getName());
	    
	    py.append(", \'" + terminationNodeFullName + "\'");
	    
	    
	    
	    
	    DecodedTermination dTermination; 
	    if(tempTermination instanceof DecodedTermination)
	    {
	    	dTermination = (DecodedTermination) tempTermination;
	    }
	    else
	    {
	    	throw new ScriptGenException("Trying to generate script of non decoded termination which is not supported.");
	    }

	    StringBuilder transformString = new StringBuilder();
	    transformString.append('[');
	    
	    float[][] transform = dTermination.getTransform();
	    
	    for(int i = 0; i < transform.length; i++)
	    {
	    	if(i != 0)
	    		transformString.append(", ");
	    	
	    	transformString.append("[");
	    	
	    	for(int j = 0; j < transform[i].length; j++)
	    	{
	    		if(j != 0)
		    		transformString.append(", ");
		    	
		    	transformString.append(transform[i][j]);
	    	}
	    	
	    	transformString.append("]");
	    }
	    
	    transformString.append("]");
	   
	    py.append(", transform="+transformString.toString());
	    
	    
								
	    // Now handle origin function if there is one
	   
	    if(!(tempOrigin.getNode() instanceof FunctionInput))
	    {
		    DecodedOrigin dOrigin; 
		    if(tempOrigin instanceof DecodedOrigin)
		    {
		    	dOrigin = (DecodedOrigin) tempOrigin;
		    }
		    else
		    {
		    	throw new ScriptGenException("Trying to generate script of non decoded origin which is not supported.");
		    }

		    StringBuilder funcString = new StringBuilder();
		    boolean first = true;
		    
		    boolean allIdentity = true;
		    for(Function f: dOrigin.getFunctions())
		    {
		    	 if(!(f instanceof IdentityFunction))
		    	 {
		    		 allIdentity = false;
		    		 break;
		    	 }
		    }
		    
		    if(!allIdentity)
		    {
			    
			    for(Function f: dOrigin.getFunctions())
			    {
			    	if(f instanceof PostfixFunction)
			    	{
			    		PostfixFunction pf = (PostfixFunction) f;
			    		String exp = pf.getExpression();
			    		
			    		exp=exp.replaceAll("^","**");
			    		exp=exp.replaceAll("!"," not ");
			    		exp=exp.replaceAll("&"," and ");
			    		exp=exp.replaceAll("|"," or ");
			    		exp=exp.replaceAll("ln","log");
			    		
			    		for(int j = 0; j < f.getDimension(); j++)
			    		{
			    			String find = "x"+ Integer.toString(j);
			    			String replace = "x["+ Integer.toString(j) + "]";
			    			exp=exp.replaceAll(find, replace);
			    		}
			    		
			    		if (first)
			    		{
			    			funcString.append(exp);
			    			first = false;
			    		}
			    		else
			    		{
			    			funcString.append(", " + exp);	
			    		}
			    	}
			    	else if(f instanceof IdentityFunction)
			    	{
			    		String exp = "x[" + Integer.toString(((IdentityFunction) f).getIdentityDimension()) + "]";
			    			
			    		if (first)
			    		{
			    			funcString.append(exp);
			    			first = false;
			    		}
			    		else
			    		{
			    			funcString.append(", " + exp);	
			    		}
			    	}
			    	else
			    	{
			    		throw new ScriptGenException("Trying to generate script of non user-defined function on an origin which is not supported.");
			    	}
			    }
			    
			    py.insert(0, "    return [" + funcString.toString() + "]\n\n");
			    py.insert(0, "def function(x):\n");
			    
			    py.append(", func=function");
		    }
	    }
	    
	    py.append(")\n");
	    
	    return py.toString();
	}
}
