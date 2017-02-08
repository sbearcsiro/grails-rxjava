package rxjava.demo

import grails.events.Events

class MyJob implements Events {
    static triggers = {
        simple name: 'mySimpleTrigger', startDelay: 1000, repeatInterval: 1000
    }
    def group = "MyGroup"
    def description = "Example job with Simple Trigger"

    static int i = 0

    def execute(){
        log.trace('MyJob.execute()')

        notify "myEvent", "${i++}"
    }
}