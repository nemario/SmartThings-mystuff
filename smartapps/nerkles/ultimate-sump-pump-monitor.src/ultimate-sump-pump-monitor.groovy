definition(
    name: "Ultimate Sump Pump Monitor",
    namespace: "nerkles",
    author: "Chris Apple",
    description: "Monitor my sump pump, using power, vibration and water sensors.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/alarm/water/wet.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/alarm/water/wet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartthings-device-icons/alarm/water/wet@3x.png")

preferences {
    section ("Power Device:") {
        input "meter", "capability.powerMeter", multiple: false, required: false
    }
    section("Vibration Sensor:") {
         input "multi", "capability.accelerationSensor", title: "Which?", multiple: false, required: false
    }
    section("Water Sensor:") {
         input "water", "capability.waterSensor", title: "Which?", multiple: false, required: false
    }

    section (title: "Notification method") {
        input "sendPushMessage", "bool", title: "Send a push notification?"
    }
    
    section("Update virtual Sump Pump (optional):") {
         input "sump", "capability.waterSensor", title: "Which?", multiple: false, required: false
    }

//    section (title: "Notification method") {
//        input "phone", "phone", title: "Send a text message to:", required: true
//    }
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
    subscribe(multi, "acceleration.active", tempSendEvtMsg)
    subscribe(multi, "acceleration.inactive", tempSendEvtMsg)
    subscribe(water, "wet", tempSendEvtMsg)
    subscribe(water, "dry", tempSendEvtMsg)
    
    meter.reset();
    meter.on()
    
    state.cycleOn = false;
    state.lastEnergy = 0
    
    log.trace "initialize"
    log.debug "lastEnergy: ${state.lastEnergy}"
    log.trace "Forced switch on."
}

//setup state variables

//process incoming evt
	//energy
    //vibration
    //wet/dry
//run virtual device update
	//running or not running
    //wet/dry
    
//check alerts
	//running / stopping
    //Running too long
    //Running too often
    //Wet!!!
    //Wet while running...
//send notifications

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
        def message = "Ultimate Sump pump - Started"
        
       sump.On()
        
        log.trace "${message}"
        send(message)
    } else if (state.cycleOn && isRunning) {
        // If the sump pump continues drawing energy,
        // send more notifications.
        def message = "Ultimate Sump pump - Running long"
        
        sump.On()
        
        log.trace "${message}"
        // this is probably overkill
        //send(message)
    } else if (state.cycleOn && !isRunning) {
        // If the sump pump stops drawing power, send notification.
        state.cycleOn = false
        def message = "Ultimate Sump pump - Stopped"
        
        sump.Off()
        
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

def tempSendEvtMsg(evt){

	def message = "uSP - ${evt.name} - ${evt.value}"
    log.trace "${message}"
    send(message)
    
    //sump.On()

}

def checkFrequency(evt){
	log.debug("running check sump")
	def lastTime = state[frequencyKeyAccelration(evt)]

	if (lastTime == null) {
		state[frequencyKeyAccelration(evt)] = now()
	}

	else if (now() - lastTime >= frequency * 60000) {
		state[frequencyKeyAccelration(evt)] = now()
	}


	else if (now() - lastTime <= frequency * 60000) {
		log.debug("Last time valid")
		def timePassed = now() - lastTime
		def timePassedFix = timePassed / 60000
		def timePassedRound = Math.round(timePassedFix.toDouble()) + (unit ?: "")
		state[frequencyKeyAccelration(evt)] = now()
		def msg = messageText ?: "Alert: Sump pump has ran in twice in the last ${timePassedRound} minutes."

		if (!phone || pushAndPhone != "No") {
			log.debug "sending push"
			sendPush(msg)
		}
		if (phone) {
			log.debug "sending SMS"
			sendSms(phone, msg)
		}
	}
}

private send(msg) {
    
    if (sendPushMessage) {
        sendPush(msg)
    }
    else {
    	sendNotificationEvent(msg)
    }
   
//    if (phone) {
//        sendSms(phone, msg)
//    }
	
    log.debug msg
}