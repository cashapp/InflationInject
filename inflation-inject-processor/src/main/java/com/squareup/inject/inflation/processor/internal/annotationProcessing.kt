package com.squareup.inject.inflation.processor.internal

import com.squareup.inject.inflation.processor.internal.MirrorValue.Array
import com.squareup.inject.inflation.processor.internal.MirrorValue.Error
import com.squareup.inject.inflation.processor.internal.MirrorValue.Type
import com.squareup.inject.inflation.processor.internal.MirrorValue.Unmapped
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ErrorType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.SimpleAnnotationValueVisitor6
import javax.lang.model.util.SimpleTypeVisitor6

/** Return a list of elements annotated with `T`. */
inline fun <reified T : Annotation> RoundEnvironment.findElementsAnnotatedWith(): Set<Element>
    = getElementsAnnotatedWith(T::class.java)

/** Return true if this [AnnotatedConstruct] is annotated with `T`. */
inline fun <reified T : Annotation> AnnotatedConstruct.hasAnnotation()
    = getAnnotation(T::class.java) != null

/** Return true if this [AnnotatedConstruct] is annotated with `qualifiedName`. */
fun AnnotatedConstruct.hasAnnotation(qualifiedName: String) = getAnnotation(qualifiedName) != null

/** Return the first annotation matching [qualifiedName] or null. */
fun AnnotatedConstruct.getAnnotation(qualifiedName: String) = annotationMirrors
    .firstOrNull {
      it.annotationType.asElement().cast<TypeElement>().qualifiedName.contentEquals(qualifiedName)
    }

fun AnnotationMirror.getValue(property: String, elements: Elements) = elements
    .getElementValuesWithDefaults(this)
    .entries
    .firstOrNull { it.key.simpleName.contentEquals(property) }
    ?.value
    ?.toMirrorValue()

fun AnnotationValue.toMirrorValue(): MirrorValue = accept(MirrorValueVisitor, null)

sealed class MirrorValue {
  data class Type(private val value: TypeMirror) : MirrorValue(), TypeMirror by value
  data class Array(private val value: List<MirrorValue>) : MirrorValue(), List<MirrorValue> by value
  object Unmapped : MirrorValue()
  object Error : MirrorValue()
}

private object MirrorValueVisitor : SimpleAnnotationValueVisitor6<MirrorValue, Nothing?>() {
  override fun defaultAction(o: Any, ignored: Nothing?) = Unmapped

  override fun visitType(mirror: TypeMirror, ignored: Nothing?) = mirror.accept(TypeVisitor, null)

  override fun visitArray(values: List<AnnotationValue>, ignored: Nothing?) =
      Array(values.map { it.accept(this, null) })
}
private object TypeVisitor : SimpleTypeVisitor6<MirrorValue, Nothing?>() {
  override fun visitError(type: ErrorType, ignored: Nothing?) = Error
  override fun defaultAction(type: TypeMirror, ignored: Nothing?) = Type(type)
}
