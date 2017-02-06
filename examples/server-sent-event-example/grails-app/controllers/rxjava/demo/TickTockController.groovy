package rxjava.demo

import io.reactivex.Emitter
import grails.rx.web.*
/**
 * Created by graemerocher on 28/07/2016.
 */
class TickTockController implements RxController {

    def index() {
        rx.stream { Emitter emitter ->
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
        log.info("Last Event ID: $lastId")
        rx.stream { Emitter emitter ->
            int i = lastId != null ? lastId + 1 : 1
            for(;; ++i) {
                if(i % 2 == 0) {
                    emitter.onNext(
                            rx.event("Tick\nTick$i", id: i, event: 'tick', comment: 'tick')
                    )
                }
                else if(i % 51 == 0) {
                    throw new RuntimeException('Boom')
                }
                else {
                    emitter.onNext(
                            rx.event("Tock\nTock$i", id: i, event: 'tock', comment: 'tock')
                    )

                }
                sleep 1000
            }
            emitter.onComplete()
        }
    }
}
