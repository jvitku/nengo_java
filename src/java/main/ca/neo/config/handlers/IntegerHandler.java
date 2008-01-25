/*
 * Created on 17-Dec-07
 */
package ca.neo.config.handlers;

public class IntegerHandler extends BaseHandler {

	public IntegerHandler() {
		super(Integer.class);
	}

	/**
	 * @see ca.neo.config.ConfigurationHandler#getDefaultValue(java.lang.Class)
	 */
	public Object getDefaultValue(Class c) {
		return new Integer(0);
	}

}