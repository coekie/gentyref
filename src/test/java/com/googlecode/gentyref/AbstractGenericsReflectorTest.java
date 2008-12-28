package com.googlecode.gentyref;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.googlecode.gentyref.TypeToken;


import junit.framework.TestCase;

public abstract class AbstractGenericsReflectorTest extends TestCase {
	/**
	 * A constant that's false, to use in an if() block for code that's only there to show that it compiles.
	 * This code "proves" that the test  is an actual valid test case, by showing the compiler agrees.
	 * But some of the code should not actually be executed, because it might throw exceptions
	 * (because we're too lazy to initialize everything). 
	 */
	private static final boolean COMPILE_CHECK = false;
	
	private static final Type ARRAYLIST_OF_STRING = new TypeToken<ArrayList<String>>(){}.getType();
	private ArrayList<String> arrayListOfString;
	private static final Type LIST_OF_STRING = new TypeToken<List<String>>(){}.getType();
	@SuppressWarnings("unused")
	private List<String> listOfString;
	private static final Type COLLECTION_OF_STRING = new TypeToken<Collection<String>>(){}.getType();
	@SuppressWarnings("unused")
	private Collection<String> collectionOfString;
	
	private static final Type ARRAYLIST_OF_LIST_OF_STRING = new TypeToken<ArrayList<List<String>>>(){}.getType();
	private static final Type LIST_OF_LIST_OF_STRING = new TypeToken<List<List<String>>>(){}.getType();
	private static final Type COLLECTION_OF_LIST_OF_STRING = new TypeToken<Collection<List<String>>>(){}.getType();
	
	private static final Type ARRAYLIST_OF_EXT_STRING = new TypeToken<ArrayList<? extends String>>(){}.getType();
	private static final Type COLLECTION_OF_EXT_STRING = new TypeToken<Collection<? extends String>>(){}.getType();
	
	private static final Type COLLECTION_OF_SUPER_STRING = new TypeToken<Collection<? super String>>(){}.getType();
	
	private static final Type ARRAYLIST_OF_LIST_OF_EXT_STRING = new TypeToken<ArrayList<List<? extends String>>>(){}.getType();
	private static final Type LIST_OF_LIST_OF_EXT_STRING = new TypeToken<List<List<? extends String>>>(){}.getType();
	private static final Type COLLECTION_OF_LIST_OF_EXT_STRING = new TypeToken<Collection<List<? extends String>>>(){}.getType();

	class Box<T> {
		public T t;
	}

	private void testFieldTypeExactSuperclass(Type expectedMatch, Type fieldClass, String fieldName) {
		Type fieldType = getExactFieldType(fieldName, fieldClass);
		testExactSuperclass(expectedMatch, fieldType);
	}
	
	private void testFieldTypeInexactSupertype(Type superType, Type fieldClass, String fieldName) {
		Type returnType = getExactFieldType(fieldName, fieldClass);
		testInexactSupertype(superType, returnType);
	}
	
//	private void testReturnTypeExactSuperclass(Type expectedMatch, Type methodClass, String methodName) {
//		Type returnType = getExactReturnType(methodName, methodClass);
//		testExactSuperclass(expectedMatch, returnType);
//	}
//	
//	private void testReturnTypeInexactSupertype(Type superType, Type methodClass, String methodName) {
//		Type returnType = getExactReturnType(methodName, methodClass);
//		testInexactSupertype(superType, returnType);
//	}
	
	abstract protected void testInexactSupertype(Type superType, Type subType);
	
	abstract protected void testExactSuperclass(Type expectedSuperclass, Type type);
	
	static protected Class<?> getClass(Type type) {
		if (type instanceof Class) {
			return (Class<?>)type;
		} else {
			ParameterizedType pType = (ParameterizedType) type;
			return (Class<?>)pType.getRawType();
		}
	}
	
	private Type getExactFieldType(String fieldName, Type forType) {
		try {
			Class<?> clazz = getClass(forType);
			return getExactFieldType(clazz.getField(fieldName), forType);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Error in test: can't find field " + fieldName, e);
		}
	}
	
//	private Type getExactReturnType(String methodName, Type forType) {
//		try {
//			Class<?> clazz = getClass(forType);
//			return getExactReturnType(clazz.getMethod(methodName), forType);
//		} catch (NoSuchMethodException e) {
//			throw new RuntimeException("Error in test: can't find method " + methodName, e);
//		}
//	}
	
//	private void testReturnTypeEquals(Type expected, String methodName, Type forType) {
//		assertEquals(expected, getExactReturnType(methodName, forType));
//	}
	private void testFieldTypeEquals(Type expected, String fieldName, Type forType) {
		assertEquals(expected, getExactFieldType(fieldName, forType));
	}
	
	protected abstract Type getExactReturnType(Method m, Type type);
	protected abstract Type getExactFieldType(Field f, Type type);
	
	private void testExactSuperclassChain(Type type1, Type type2, Type type3) {
		testExactSuperclass(type3, type3);
		testExactSuperclass(type1, type2);
		testExactSuperclass(type2, type3);
		testExactSuperclass(type1, type3);
	}
	
	public void testOfString() {
		collectionOfString = listOfString = arrayListOfString; // compile check
		testExactSuperclassChain(COLLECTION_OF_STRING, LIST_OF_STRING, ARRAYLIST_OF_STRING);
	}

	public interface StringList extends List<String> {
	}
	private StringList stringList;

	public void testStringList() {
		listOfString = stringList; // compile check
		testExactSuperclassChain(COLLECTION_OF_STRING, LIST_OF_STRING, StringList.class);
	}
	

	public void testTextendsStringList() {
		class TExtendsStringList<T extends StringList> {
			public T t;
		}

		if (COMPILE_CHECK) {
			@SuppressWarnings("unchecked")
			TExtendsStringList tExtendsStringList = null;
			listOfString = tExtendsStringList.t;
		}
		testFieldTypeExactSuperclass(LIST_OF_STRING, TExtendsStringList.class, "t");
	}
	
	public void testExtendViaOtherTypeParam() {
		class ExtendViaOtherTypeParam<T extends StringList, U extends T> {
			@SuppressWarnings("unused")
			public U u;
		}
		testFieldTypeExactSuperclass(LIST_OF_STRING, ExtendViaOtherTypeParam.class, "u");
	}
	
	@SuppressWarnings("unchecked")
	public void testRawMultiBoundParametrizedStringList() {
		class MultiBoundParametrizedStringList<T extends Object & StringList> {
			@SuppressWarnings("unused")
			public T t;
		}
		testFieldTypeEquals(Object.class, "t", MultiBoundParametrizedStringList.class);
		
		new MultiBoundParametrizedStringList().t = new Object();
	}
	
	public void testWildcardMultiBoundParametrizedStringList() {
		class C<T extends Object & StringList> {
			public T t;
		}
		testFieldTypeExactSuperclass(LIST_OF_STRING, new TypeToken<C<?>>(){}.getType(), "t");
		listOfString = ((C<?>)new C<StringList>()).t;
	}
	
	public void testFListOfT_String() {
		class FListOfT<T> {
			@SuppressWarnings("unused")
			public List<T> f;
		}
		testFieldTypeEquals(LIST_OF_STRING, "f", new TypeToken<FListOfT<String>>(){}.getType());
	}

	public void testOfListOfString() {
		testExactSuperclassChain(COLLECTION_OF_LIST_OF_STRING, LIST_OF_LIST_OF_STRING, ARRAYLIST_OF_LIST_OF_STRING);
	}
	
	public void testFListOfListOfT_String() {
		class FListOfListOfT<T> {
			@SuppressWarnings("unused")
			public List<List<T>> f;
		}
		testFieldTypeEquals(LIST_OF_LIST_OF_STRING, "f", new TypeToken<FListOfListOfT<String>>(){}.getType());
	}

	public interface ListOfListOfT<T> extends List<List<T>> {}
	
	public void testListOfListOfT_String() {
		testExactSuperclassChain(COLLECTION_OF_LIST_OF_STRING, LIST_OF_LIST_OF_STRING, new TypeToken<ListOfListOfT<String>>(){}.getType());
	}
	
	public interface ListOfListOfT_String extends ListOfListOfT<String> {}
	public void testListOfListOfT_StringInterface() {
		testExactSuperclassChain(COLLECTION_OF_LIST_OF_STRING, LIST_OF_LIST_OF_STRING, ListOfListOfT_String.class);
	}
	
	public interface ListOfListOfString extends List<List<String>> {}
	public void testListOfListOfStringInterface() {
		testExactSuperclassChain(COLLECTION_OF_LIST_OF_STRING, LIST_OF_LIST_OF_STRING, ListOfListOfString.class);
	}
	
	public void testWildcardTExtendsListOfListOfString() {
		class C<T extends List<List<String>>> {
			@SuppressWarnings("unused")
			public T t;
		}
		testFieldTypeExactSuperclass(COLLECTION_OF_LIST_OF_STRING, new TypeToken<C<?>>(){}.getType(), "t");
	}
	
	public void testArrayListOfExtString() {
		testExactSuperclass(COLLECTION_OF_EXT_STRING, ARRAYLIST_OF_EXT_STRING);
	}
	
	public void testArrayListOfListOfExtString() {
		testExactSuperclass(COLLECTION_OF_LIST_OF_EXT_STRING, ARRAYLIST_OF_LIST_OF_EXT_STRING);
	}
	
	public interface ListOfListOfExtT<T> extends List<List<? extends T>> {}
	public void testListOfListOfExtT_String() {
		testExactSuperclass(COLLECTION_OF_LIST_OF_EXT_STRING, new TypeToken<ListOfListOfExtT<String>>(){}.getType());
	}
	
	public void testUExtendsListOfExtT() {
		class C<T, U extends List<? extends T>> {
			public U u;
		}
		new TypeToken<C<? extends String, ?>>(){};
		testFieldTypeInexactSupertype(COLLECTION_OF_EXT_STRING, new TypeToken<C<? extends String, ?>>(){}.getType(), "u");
		
		C<? extends String, ?> c = new C<String, List<String>>();
		@SuppressWarnings("unused")
		Collection<? extends String> o = c.u;
	}

	public void testListOfExtT() {
		class C<T> {
			public List<? extends T> t;
		}
		testFieldTypeExactSuperclass(COLLECTION_OF_EXT_STRING, new TypeToken<C<String>>(){}.getType(), "t");
		if (COMPILE_CHECK) {
			List<? extends String> listOfExtString = null;
			new C<String>().t = listOfExtString;
			listOfExtString = new C<String>().t;
		}
	}
	
	public void testListOfSuperT() {
		class C<T> {
			public List<? super T> t;
		}
		testFieldTypeExactSuperclass(COLLECTION_OF_SUPER_STRING, new TypeToken<C<String>>(){}.getType(), "t");
		if (COMPILE_CHECK) {
			List<? super String> listOfSuperString = null;
			new C<String>().t = listOfSuperString;
			listOfSuperString = new C<String>().t;
		}
	}
	
	public void testInnerFieldWithTypeOfOuter() {
		class Outer<T> {
			@SuppressWarnings("unused")
			class Inner {
				public T t;
				public List<List<? extends T>> llet;
			}
		}
		if (COMPILE_CHECK) {
			Outer<String>.Inner inner = null;
			String s = inner.t = "";
		}
		
		Type outerStringInner = new TypeToken<Outer<String>.Inner>(){}.getType(); 
		
		testFieldTypeEquals(String.class, "t", outerStringInner);
		testFieldTypeEquals(LIST_OF_LIST_OF_EXT_STRING, "llet", outerStringInner);
	}

	/**
	 * Supertype of a raw type is erased
	 */
	@SuppressWarnings("unchecked")
	public void testSubclassRaw() {
		class SuperclassZom<T extends Number> {
			public T t;
		}
		class Subclass<U> extends SuperclassZom<Integer>{}
		testFieldTypeEquals(Number.class, "t", Subclass.class);
		
		Number n = new Subclass().t; // compile check
		new Subclass().t = n; // compile check
	}

	/**
	 * Supertype of a raw type is erased.
	 * (And  there's no such thing as a ParameterizedType with some type parameters raw and others not)
	 */
	@SuppressWarnings("unchecked")
	public void testSubclassRawMix() {
		class Superclass<T, U extends Number> {
//			public T t;
			public U u;
		}
		class Subclass<T> extends Superclass<T, Integer> {}
		testFieldTypeEquals(Number.class, "u", Subclass.class);
		
		Number n = new Subclass().u; // compile check
		new Subclass().u = n; // compile check
	}
	
	/**
	 * If a type has no parameters, it doesn't matter that it got erased.
	 * So even though Middleclass was erased, its supertype is not.
	 */
	@SuppressWarnings("unchecked")
	public void testSubclassRawViaUnparameterized() {
		class Superclass<T extends Number> {
			public T t;
		}
		class Middleclass extends Superclass<Integer> {}
		class Subclass<U> extends Middleclass {}
		
		testFieldTypeEquals(Integer.class, "t", Subclass.class);
		
		Integer i = new Subclass().t; // compile check
		new Subclass().t = i; // compile check
	}
	
	/**
	 * Similar for inner types: the outer type of a raw inner type is also erased
	 */
	@SuppressWarnings("unchecked")
	public void testInnerRaw() {
		class Outer<T extends Number> {
			public Inner rawInner;
			
			class Inner<U extends T> {
				public T t;
				public U u;
			}
		}
		
		testFieldTypeEquals(Outer.Inner.class, "rawInner", Outer.class);
		testFieldTypeEquals(Number.class, "t", Outer.Inner.class);
		testFieldTypeEquals(Number.class, "u", Outer.Inner.class);
		
		if (COMPILE_CHECK) {
			Number n = new Outer<Integer>().rawInner.t; // compile check
			new Outer<Integer>().rawInner.t = n; // compile check
			n = new Outer<Integer>().rawInner.u; // compile check
			new Outer<Integer>().rawInner.u = n; // compile check
		}
	}
	
	public void testSuperWildcard() {
		Box<? super Integer> b = new Box<Integer>(); // compile check
		b.t = new Integer(0); // compile check
		
		testInexactSupertype(getExactFieldType("t", new TypeToken<Box<? super Integer>>(){}.getType()), Integer.class);
	}
	
	// TODO graph tests for recursively referring bounds
//	interface Graph<N extends Node<N, E>, E extends Edge<N, E>> {}
//	interface Node<N extends Node<N,E>, E extends Edge<N, E>> {}
//	interface Edge<N extends Node<N,E>, E extends Edge<N, E>> {}

}
