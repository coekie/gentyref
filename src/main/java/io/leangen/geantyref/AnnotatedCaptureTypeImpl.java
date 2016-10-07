package io.leangen.geantyref;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.leangen.geantyref.GenericTypeReflector.typeArraysEqual;
import static java.util.Arrays.stream;

class AnnotatedCaptureTypeImpl implements AnnotatedCaptureType {

    private final AnnotatedWildcardType wildcard;
    private final AnnotatedTypeVariable variable;
    private final AnnotatedType[] lowerBounds;
    private AnnotatedType[] upperBounds;
    private CaptureType type;
    private Map<Class<? extends Annotation>, Annotation> annotations;
    private Annotation[] declaredAnnotations;

    public AnnotatedCaptureTypeImpl(AnnotatedWildcardType wildcard, AnnotatedTypeVariable variable, AnnotatedType[] upperBounds, Annotation[] annotations) {
        this(wildcard, variable, upperBounds);
        this.annotations = Arrays.stream(annotations).collect(Collectors.toMap(Annotation::getClass, annotation -> annotation));
    }

    public AnnotatedCaptureTypeImpl(AnnotatedWildcardType wildcard, AnnotatedTypeVariable variable, AnnotatedType[] upperBounds) {
        this(wildcard, variable);
        this.upperBounds = upperBounds;
    }

    public AnnotatedCaptureTypeImpl(AnnotatedWildcardType wildcard, AnnotatedTypeVariable variable) {
        this.wildcard = wildcard;
        this.variable = variable;
        this.lowerBounds = wildcard.getAnnotatedLowerBounds();
        this.type = new CaptureTypeImpl((WildcardType) wildcard.getType(), (TypeVariable) variable.getType());
        this.annotations = Stream.concat(
                stream(wildcard.getAnnotations()),
                stream(variable.getAnnotations())
        ).collect(Collectors.toMap(Annotation::getClass, annotation -> annotation));
        this.declaredAnnotations = Stream.concat(
                Arrays.stream(wildcard.getDeclaredAnnotations()),
                Arrays.stream(variable.getDeclaredAnnotations())
        ).toArray(Annotation[]::new);
    }

    /**
     * Initialize this CaptureTypeImpl. This is needed for type variable bounds referring to each
     * other: we need the capture of the argument.
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

        ((CaptureTypeImpl) type).init(varMap);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return (T) annotations.get(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations.values().toArray(new Annotation[annotations.size()]);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return declaredAnnotations;
    }

    /**
     * Returns an array of <tt>Type</tt> objects representing the upper bound(s) of this capture.
     * This includes both the upper bound of a <tt>? extends</tt> wildcard, and the bounds declared
     * with the type variable. References to other (or the same) type variables in bounds coming
     * from the type variable are replaced by their matching capture.
     */
    @Override
    public AnnotatedType[] getAnnotatedUpperBounds() {
        assert upperBounds != null;
        return upperBounds.clone();
    }

    @Override
    public void setAnnotatedUpperBounds(AnnotatedType[] upperBounds) {
        this.upperBounds = upperBounds;
        this.type.setUpperBounds(Arrays.stream(upperBounds).map(AnnotatedType::getType).toArray(Type[]::new));
    }

    /**
     * Returns an array of <tt>Type</tt> objects representing the lower bound(s) of this type
     * variable. This is the bound of a <tt>? super</tt> wildcard. This normally contains only one
     * or no types; it is an array for consistency with {@link WildcardType#getLowerBounds()}.
     */
    @Override
    public AnnotatedType[] getAnnotatedLowerBounds() {
        return lowerBounds.clone();
    }

    @Override
    public AnnotatedTypeVariable getAnnotatedTypeVariable() {
        return variable;
    }

    @Override
    public AnnotatedWildcardType getAnnotatedWildcardType() {
        return wildcard;
    }

    @Override
    public int hashCode() {
        return wildcard.hashCode() + variable.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AnnotatedCaptureType)) {
            return false;
        }
        AnnotatedCaptureType other = ((AnnotatedCaptureType) obj);
        if (!wildcard.equals(other.getAnnotatedWildcardType()) || !variable.equals(other.getAnnotatedTypeVariable())) {
            return false;
        }
        return typeArraysEqual(upperBounds, other.getAnnotatedUpperBounds());
    }
}
