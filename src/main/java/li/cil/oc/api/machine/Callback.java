package li.cil.oc.api.machine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as callable from an OpenComputers machine.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Callback {
    String value() default "";

    boolean direct() default false;

    int limit() default Integer.MAX_VALUE;

    String doc() default "";

    boolean getter() default false;

    boolean setter() default false;
}
