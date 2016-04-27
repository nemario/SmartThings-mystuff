definition(
    name: "Sump Pump Power Monitor",
    namespace: "cschone",
    author: "Chad Schone - Altered by Chris Apple",
    description: "Monitor water and usage of sump pump.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/alarm/water/wet.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/alarm/water/wet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartthings-device-icons/alarm/water/wet@3x.png")

preferences {
    section ("When this device starts drawing power") {
        input "meter", "capability.powerMeter", multiple: false, required: true
    }

    section (title: "Notification method") {
        input "sendPushMessage", "bool", title: "Send a push notification?"
    }

    section (title: "Notification method") {
        input "phone", "phone", title: "Send a text message to:", required: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(meter, "energy", handler)
    subscribe(meter, "power", handler)
    state.cycleOn = false;
    meter.reset();
    state.lastEnergy = 0
    log.trace "initialize"
    log.debug "lastEnergy: ${state.lastEnergy}"
    log.trace "Forced switch on."
    meter.on()
}

def handler(evt) {
    log.trace "handler"
    def currentEnergy = meter.currentValue("energy")
    def currentPower = meter.currentValue("power")
    //def currentState = meter.currentValue("switch")

    log.trace "Current Energy: ${currentEnergy}"
    log.trace "Current Power: ${currentPower}"
    //log.trace "Current Power: ${currentState}"

    def isRunning = (currentEnergy > state.lastEnergy) || (currentPower > 0)

    if (!state.cycleOn && isRunning) {
        // If the sump pump starts drawing energy, send notification.
        state.cycleOn = true
        def message = "Sump pump started running"
        log.trace "${message}"
        send(message)
    } else if (state.cycleOn && isRunning) {
        // If the sump pump continues drawing energy,
        // send more notifications.
        def message = "Your sump pump is still running"
        log.trace "${message}"
        // this is probably overkill
        //send(message)
    } else if (state.cycleOn && !isRunning) {
        // If the sump pump stops drawing power, send notification.
        state.cycleOn = false
        def message = "Your sump pump stopped running"
        log.trace "${message}"
        send(message)
    } else {
        // this should not happen
        log.trace "No activity."
    }
    state.lastEnergy = currentEnergy;

    // negate physical on/off switch
    // If using this device as a monitor it shold never be off
    //if(currentState == "off") {
    //    log.trace "Forced switch on."
    //    meter.on()
    //}
}

private send(msg) {
    if (sendPushMessage) {
        sendPush(msg)
    }

    if (phone) {
        sendSms(phone, msg)
    }
	
	If (!sendPushMessage && !phone){
		sendNotificationEvent(msg)

	}

    log.debug msg
}