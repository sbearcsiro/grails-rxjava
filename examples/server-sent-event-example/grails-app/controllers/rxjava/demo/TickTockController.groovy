package rxjava.demo

import grails.converters.JSON
import io.reactivex.Emitter
import io.reactivex.Observable
import grails.rx.web.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import reactor.spring.context.annotation.Consumer
import reactor.spring.context.annotation.Selector

import static java.util.concurrent.TimeUnit.SECONDS

/**
 * Created by graemerocher on 28/07/2016.
 */
@Consumer
class TickTockController implements RxController {

    def index() {
        rx.stream { Emitter emitter ->
            log.info("Index Thread ${Thread.currentThread().name}")
            for(i in (0..5)) {
                if(i % 2 == 0) {
                    emitter.onNext(
                        rx.render("Tick", contentType: 'text/plain')
                    )
                }
                else {
                    emitter.onNext(
                        rx.render("Tock", contentType: 'text/plain')
                    )

                }
                sleep 1000
            }
            emitter.onComplete()
        }
    }

    def sse() {
        def lastId = request.getHeader('Last-Event-ID') as Integer
        def startId = lastId ? lastId + 1 : 0
        log.info("Last Event ID: $lastId")
        rx.stream { Emitter emitter ->
            log.info("SSE Thread ${Thread.currentThread().name}")
            for(i in (startId..(startId+9))) {
                if(i % 2 == 0) {
                    emitter.onNext(
                            rx.event("Tick\n$i", id: i, event: 'tick', comment: 'tick')
                    )
                }
                else {
                    emitter.onNext(
                            rx.event("Tock\n$i", id: i, event: 'tock', comment: 'tock')
                    )

                }
                sleep 1000
            }
            emitter.onComplete()
        }
    }

    def observable() {
        rx.stream(
                Observable
                        .interval(1, SECONDS)
                        .doOnSubscribe { log.info("Observable Subscribe Thread ${Thread.currentThread().name}") }
                        .doOnNext { log.info("Observable Thread ${Thread.currentThread().name}") }
                        .map {
                            rx.event([type: 'observable', num: it] as JSON, comment: 'hello')
                        }
                        .take(60),
        )
    }

    Subject subject = PublishSubject.create()
    Observable publishedObservable = subject.publish().autoConnect().observeOn(Schedulers.io())

    @Selector('myEvent')
    void myEventListener(Object data) {
        log.error("myEvent listener Thread ${Thread.currentThread().name}")
        subject.onNext(data)
    }

    def quartz() {
        rx.stream(
                publishedObservable
                        .doOnSubscribe { log.error("Quartz Subscribe Thread ${Thread.currentThread().name}") }
                        .doOnError { log.error("Quartz thread error") }
                        .map {
                            log.error("Quartz Thread ${Thread.currentThread().name}")
                            rx.event([type: 'quartz', num: it] as JSON, comment: 'hello')
                        }
        )
    }
}
