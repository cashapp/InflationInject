package app.cash.inject.inflation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Mark a Dagger {@code @Module} which will have a peer module generated with bindings for all of
 * the {@link InflationInject @InflationInject}-annotated types in this compilation unit.
 * The generated module must then be added to this module's {@code includes} array.
 * <p>
 * For example:
 * <pre><code>
 *   {@literal @}InflationModule
 *   {@literal @}Module(includes = InflationInject_PresenterModule.class)
 *   abstract class PresenterModule {}
 * </code></pre>
 * <p>
 * The generated module's bindings look approximately like this:
 * <pre><code>
 *   {@literal @}Binds
 *   {@literal @}IntoMap
 *   {@literal @}StringKey("com.example.CustomView")
 *   abstract ViewFactory bindComExampleCustomView(CustomView_InflationFactory factory)
 * </code></pre>
 * {@code CustomView_InflationFactory} is also a generated type from annotating one of
 * {@code CustomView}'s constructors with {@link InflationInject @InflationInject}.
 * <p>
 * The result is that a {@code Map<String, ViewFactory>} is available in the graph. However, you
 * usually want to interact with it by injecting {@link InflationInjectFactory} rather than dealing
 * directly with the map.
 *
 * @see InflationInjectFactory
 */
@Target(TYPE)
@Retention(CLASS)
public @interface InflationModule {
}
