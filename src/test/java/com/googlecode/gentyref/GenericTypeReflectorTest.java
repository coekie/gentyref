package com.googlecode.gentyref;

import java.util.ArrayList;
import java.util.Collection;

public class GenericTypeReflectorTest extends AbstractGenericsReflectorTest {
	public GenericTypeReflectorTest() {
		super(new GenTyRefReflectionStrategy());
	}
	
	public void testGetTypeParameter() {
		class StringList extends ArrayList<String> {}
		assertEquals(String.class, GenericTypeReflector.getTypeParameter(StringList.class, Collection.class.getTypeParameters()[0]));
	}
}
