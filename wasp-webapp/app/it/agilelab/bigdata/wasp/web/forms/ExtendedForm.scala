package it.agilelab.bigdata.wasp.web.forms

import play.api.data.Form
import play.api.data.FormError
import play.api.data.Mapping

case class ExtendedForm[T](mapping : Mapping[T], extendedValidation : Form[T] => Seq[(String, String)]) {
  def bind(data : Map[String, String]) : Form[T] = evaluateAdditionalErrors(form.bind(data))
  def bind(data : play.api.libs.json.JsValue) : Form[T] = evaluateAdditionalErrors(form.bind(data))
  def bindFromRequest()(implicit request : play.api.mvc.Request[_]) : Form[T] = evaluateAdditionalErrors(form.bindFromRequest())
  def bindFromRequest(data : Map[String, Seq[String]]) : Form[T] = evaluateAdditionalErrors(form.bindFromRequest(data))

  def evaluateAdditionalErrors(form : Form[T]) = {
    val additionalErrors = extendedValidation(form).map(t => FormError(t._1, t._2))
    val addedKeys = additionalErrors.map(e => e.key).toSet
    val overwrittenErrors = form.errors.filter(e => addedKeys.contains(e.key))

    Form(form.mapping, form.data, form.errors.diff(overwrittenErrors) ++ additionalErrors, form.value)
  }

  protected val form = Form(mapping, Map(), Nil, None)
}