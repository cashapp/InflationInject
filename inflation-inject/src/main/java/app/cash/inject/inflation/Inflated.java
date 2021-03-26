package app.cash.inject.inflation;

import android.content.Context;
import android.util.AttributeSet;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Denotes the {@link Context} and/or {@link AttributeSet} parameters of an
 * @link InflationInject @InflationInject}-annotated view constructor which are fullfilled by the
 * layout inflater rather than the dependency graph.
 *
 * @see InflationInject
 */
@Retention(CLASS)
@Target(PARAMETER)
public @interface Inflated {
}
