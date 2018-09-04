Change Log
==========

Version 0.2.1 *(2018-09-04)*
----------------------------

* Fix: Ensure the generated Dagger 2 module is public if the user-defined module is public.


Version 0.2.0 *(2018-08-20)*
----------------------------

 * New: Android view-inflation injection! Inject dependencies into your custom views as constructor
   parameters while still allowing inflation from XML.
 * Fix: Factory parameter order is no longer required to match constructor parameter order.
 * Fix: Requesting a `Provider<T>` injection now works correctly.
 * Fix: Duplicate assisted or provided dependencies now issue a compiler error.
 * Fix: Validate visibility of the type, constructor, and factory prior to generating the factory.
   This produces a better error message instead of generating code that will fail to compile.


Version 0.1.2 *(2017-07-19)*
----------------------------

 * Fix: Support creating parameterized types.


Version 0.1.1 *(2017-07-03)*
----------------------------

 * Fix: Ensure annotation processors are registered as such in the jar.


Version 0.1.0 *(2017-07-02)*
----------------------------

Initial preview release.
