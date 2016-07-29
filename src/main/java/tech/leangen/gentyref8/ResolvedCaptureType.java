package tech.leangen.gentyref8;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bojan.tomic on 7/26/16.
 */
public class ResolvedCaptureType implements AnnotatedWildcardType {

	private final AnnotatedWildcardType wildcard;
	private final AnnotatedTypeVariable variable;
	private final AnnotatedType[] lowerBounds;
	private AnnotatedType[] upperBounds;
	private WildcardType type;

	public ResolvedCaptureType(AnnotatedWildcardType wildcard, AnnotatedTypeVariable variable) {
		this.wildcard = wildcard;
		this.variable = variable;
		this.lowerBounds = wildcard.getAnnotatedLowerBounds();
		this.type = (WildcardType)wildcard.getType();
	}

	/**
	 * Initialize this CaptureTypeImpl.
	 * This is needed for type variable bounds referring to each other: we need the capture of the argument.
	 */
	void init(VarMap varMap) {
		ArrayList<AnnotatedType> upperBoundsList = new ArrayList<>();
		upperBoundsList.addAll(Arrays.asList(varMap.map(variable.getAnnotatedBounds())));

		List<AnnotatedType> wildcardUpperBounds = Arrays.asList(wildcard.getAnnotatedUpperBounds());
		if (wildcardUpperBounds.size() > 0 && wildcardUpperBounds.get(0).getType() == Object.class) {
			// skip the Object bound, we already have a first upper bound from 'variable'
			upperBoundsList.addAll(wildcardUpperBounds.subList(1, wildcardUpperBounds.size()));
		} else {
			upperBoundsList.addAll(wildcardUpperBounds);
		}
		upperBounds = new AnnotatedType[upperBoundsList.size()];
		upperBoundsList.toArray(upperBounds);
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return wildcard.getAnnotation(annotationClass);
	}

	@Override
	public Annotation[] getAnnotations() {
		return wildcard.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		return wildcard.getDeclaredAnnotations();
	}

	/**
	 * Returns an array of <tt>Type</tt> objects representing the upper
	 * bound(s) of this capture. This includes both the upper bound of a <tt>? extends</tt> wildcard,
	 * and the bounds declared with the type variable.
	 * References to other (or the same) type variables in bounds coming from the type variable are
	 * replaced by their matching capture.
	 */
	public AnnotatedType[] getAnnotatedUpperBounds() {
		assert upperBounds != null;
		return upperBounds.clone();
	}

	/**
	 * Returns an array of <tt>Type</tt> objects representing the
	 * lower bound(s) of this type variable. This is the bound of a <tt>? super</tt> wildcard.
	 * This normally contains only one or no types; it is an array for consistency with {@link WildcardType#getLowerBounds()}.
	 */
	public AnnotatedType[] getAnnotatedLowerBounds() {
		return lowerBounds.clone();
	}
}
