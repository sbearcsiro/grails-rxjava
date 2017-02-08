package org.grails.plugins.rx

import grails.artefact.Controller
import grails.artefact.controller.RestResponder
import grails.converters.JSON
import grails.util.GrailsWebMockUtil
import io.reactivex.Emitter
import org.grails.plugins.rx.web.NewObservableResult
import org.grails.plugins.rx.web.RxResultTransformer
import org.grails.plugins.rx.web.StreamingNewObservableResult
import org.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.grails.web.converters.marshaller.json.DomainClassMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.async.WebAsyncManager
import org.springframework.web.context.request.async.WebAsyncUtils
import spock.lang.Specification
import static grails.rx.web.Rx.*
/**
 * Created by graemerocher on 29/07/2016.
 */
class NewObservableResultSpec extends Specification {

    void "test create an observable"() {
        setup:
        GrailsWebRequest webRequest = GrailsWebMockUtil.bindMockWebRequest()
        MockHttpServletRequest request = webRequest.getCurrentRequest()
        request.setAsyncSupported(true)
        NewObservableController controller = new NewObservableController()
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)

        when:"An action is rendered that creates an observable"
        def observable = controller.index()

        then:"The result is correct"
        observable instanceof NewObservableResult

        when:"The observable is transformed"
        RxResultTransformer transformer = new RxResultTransformer()
        def result = transformer.transformActionResult(webRequest, "index", observable)
        then:"null is returned"
        result == null
        webRequest.response.contentAsString == "Foo"

        cleanup:
        RequestContextHolder.setRequestAttributes(null)
    }

    void "test stream an observable"() {
        setup:
        GrailsWebRequest webRequest = GrailsWebMockUtil.bindMockWebRequest()
        MockHttpServletRequest request = webRequest.getCurrentRequest()
        request.setAsyncSupported(true)
        NewObservableController controller = new NewObservableController()
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)

        when:"An action is rendered that creates an observable"
        def observable = controller.stream()

        then:"The result is correct"
        observable instanceof StreamingNewObservableResult

        when:"The observable is transformed"
        RxResultTransformer transformer = new RxResultTransformer()
        def result = transformer.transformActionResult(webRequest, "index", observable)
        then:"null is returned"
        result == null
        webRequest.response.contentType == RxResultTransformer.CONTENT_TYPE_EVENT_STREAM
        webRequest.response.contentAsString == '''\
data: Foo 0

data: Foo 1

data: Foo 2

data: Foo 3

'''

        cleanup:
        RequestContextHolder.setRequestAttributes(null)
    }

    void "test stream json an observable"() {
        setup:
        JSON.registerObjectMarshaller(new MapMarshaller())
        GrailsWebRequest webRequest = GrailsWebMockUtil.bindMockWebRequest()
        MockHttpServletRequest request = webRequest.getCurrentRequest()
        request.addHeader("Accept", "application/json")
        request.setAsyncSupported(true)
        NewObservableController controller = new NewObservableController()
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)

        when:"An action is rendered that creates an observable"
        def observable = controller.streamJson()

        then:"The result is correct"
        observable instanceof StreamingNewObservableResult

        when:"The observable is transformed"
        RxResultTransformer transformer = new RxResultTransformer()
        def result = transformer.transformActionResult(webRequest, "index", observable)
        then:"null is returned"
        result == null
        webRequest.response.contentAsString == '''data: {"foo":"bar 0"}

data: {"foo":"bar 1"}

data: {"foo":"bar 2"}

data: {"foo":"bar 3"}

'''

        cleanup:
        RequestContextHolder.setRequestAttributes(null)
    }

    void "test exception is handled and async connection is complete"() {
        setup:
        GrailsWebRequest webRequest = GrailsWebMockUtil.bindMockWebRequest()
        MockHttpServletRequest request = webRequest.getCurrentRequest()
        request.setAsyncSupported(true)
        NewObservableController controller = new NewObservableController()
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)
        WebAsyncManager webAsyncManager = WebAsyncUtils.getAsyncManager(webRequest.currentRequest)

        when:"An action is rendered that creates an observable"
        def observable = controller.streamException()

        then:"The result is correct"
        observable instanceof StreamingNewObservableResult

        when:"The observable is transformed"
        RxResultTransformer transformer = new RxResultTransformer()
        def result = transformer.transformActionResult(webRequest, "stream", observable)
        then:"null is returned"
        result == null
        webRequest.response.contentType == RxResultTransformer.CONTENT_TYPE_EVENT_STREAM
        webRequest.response.committed == true
        webRequest.response.contentAsString == ''''''

        then:"The async request is complete"
        webAsyncManager?.asyncWebRequest?.isAsyncComplete() == true

        cleanup:
        RequestContextHolder.setRequestAttributes(null)
    }
}
class NewObservableController implements Controller, RestResponder{
    def index() {
        create { Emitter subscriber ->
            subscriber.onNext(
                render("Foo")
            )
            subscriber.onComplete()
        }
    }

    def stream() {
        stream { Emitter subscriber ->
            for(i in 0..3) {
                subscriber.onNext(
                        render("Foo $i")
                )
            }
            subscriber.onComplete()
        }
    }

    def streamJson() {
        stream { Emitter subscriber ->
            for(i in 0..3) {
                subscriber.onNext(
                        render(contentType:"application/json") {
                            foo "bar $i"
                        }
                )
            }
            subscriber.onComplete()
        }
    }

    def streamException() {
        stream { Emitter emitter ->
            throw new RuntimeException("Expected", null, true, false) {}
        }
    }
}
