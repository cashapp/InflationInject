package app.cash.inject.inflation;

import android.content.Context;
import android.util.AttributeSet;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Denotes a view constructor which will be used for inflation. The normal {@link Context} and
 * {@link AttributeSet} parameters should be marked with {@link Inflated @Inflated}. All other
 * parameters will be fetched from the dependency graph.
 * <p>
 * For example:
 * <pre><code>
 * {@literal @}InflationInject
 * public CustomView(
 *   {@literal @}Inflated Context context,
 *   {@literal @}Inflated AttributeSet attrs,
 *   Picasso picasso
 * ) {
 *   super(context, attrs);
 *   this.picasso = picasso;
 * }
 * </code></pre>
 *
 * The annotation processor will automatically include all {@code @InflationInject}-annotated types
 * in a generated module that can be included in the graph.
 *
 * @see InflationModule
 */
@Retention(CLASS)
@Target(CONSTRUCTOR)
public @interface InflationInject {
}
