definition(
    name: "Ultimate Sump Pump Monitor",
    namespace: "nerkles",
    author: "Chris Apple",
    description: "Monitor my sump pump, using power, vibration and water sensors.  Also updates a virtual sump pump so its status can be seen as a device.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/alarm/water/wet.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/alarm/water/wet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartthings-device-icons/alarm/water/wet@3x.png")

preferences {
    section ("Devices:") {
        input "meter", "capability.powerMeter", title: "Power Meter", multiple: false, required: true
        input "multi", "capability.accelerationSensor", title: "Vibration Sensor", multiple: false, required: true
        input "water", "capability.waterSensor", title: "Water Sensor", multiple: false, required: true
        input "sump", "capability.waterSensor", title: "Virtual Sump Pump (not currentl optional)", multiple: false, required: true
    }

    section (title: "Notification Settings") {
	    input "runLength", "number", required: true, title: "How long does your sump pump run (in seconds)?"
        input "runsEvery", "number", required: true, title: "How often should your sump pump run (in minutes)?"
        input "sendPushMessage", "bool", title: "Send a push notification?"
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
    subscribe(meter, "energy", incomingEnergyEvent)
    subscribe(meter, "power", incomingEnergyEvent)
    
    meter.reset();
    meter.on()
        
    subscribe(multi, "acceleration.active", incomingVibrationEvent)
    subscribe(multi, "acceleration.inactive", incomingVibrationEvent)
        
    subscribe(water, "wet", incomingWaterEvents)
    subscribe(water, "dry", incomingWaterEvents)
    
	setupState()   
    
    log.trace "initialize"
    log.debug "lastEnergy: ${state.lastEnergy}"
    log.trace "Forced switch on."
}

//setup state variables
def setupState() {
	state.cycleOn = false;
    state.lastEnergy = 0
        
    state.lastCycleStartDateTime = now()
    state.lastCycleEndDateTime = now()
    
    state.currentOnOffActivity = "inactive"
    state.lastOnOffActivity = "turnedOff"
}

//process incoming evt===============================================================
	//energy===============================================================
    def incomingEnergyEvent(evt){

        log.trace "processIncomingEnergyEvents"
        
        def currentEnergy = meter.currentValue("energy")
        def currentPower = meter.currentValue("power")
        def currentState = meter.currentValue("switch")

        if(currentState == "off") {
            log.trace "Forced switch on."
            send("Sump Pump power turned back on.")
            meter.on()
        }

        log.trace "Current Energy: ${currentEnergy}"
        log.trace "Current Power: ${currentPower}"
        log.trace "Current Power: ${currentState}"

        def isRunning = (currentEnergy > state.lastEnergy) || (currentPower > 0)
        def onOffSwitch = "na"

        //Determine which thing is did
        if (!state.cycleOn && isRunning) {
            state.cycleOn = true
            state.currentOnOffActivity = "turnedOn"
        } else if (state.cycleOn && isRunning) {
             state.currentOnOffActivity = "stillRunnning"
        } else if (state.cycleOn && !isRunning) {
            state.cycleOn = false
            state.currentOnOffActivity = "turnedOff"
        }

        state.lastEnergy = currentEnergy;
        
        processOnOffActivity() 
        cleanup()
    }
    //vibration===============================================================
    def incomingVibrationEvent(evt){
        def message = "uSP - ${evt.name} - ${evt.value}"
        log.trace "${message}"
        //send(message)
        
        if (evt.value == "active"){
        	state.currentOnOffActivity = "turnedOn"
        }
        else if (evt.value == "inactive"){
            state.currentOnOffActivity = "turnedOff"
        }
		processOnOffActivity() 
        cleanup()
    }
    
    //wet/dry===============================================================
    def incomingWaterEvents(evt){
    	//TO BE ADDED
        def message = "uSP - ${evt.name} - ${evt.value}"
        log.trace "${message}"
        send(message)
}
    
//run virtual device update===============================================================
	//running or not running===============================================================
    def processOnOffActivity() {
		
        //state.lastCycleStartDateTime = 0
    	//state.lastCycleEndDateTime = 0
        //runLength
        //runEvery
        
        def isGreaterThanRunLength = false
        def isGreaterThanRunEvery = false
        
		if (state.lastCycleEndDateTime >= now() - (runEvery * 60) * 60000){
           	isGreaterThanRunEvery = true
        }
        
        if (now() - state.lastCycleStartDateTime >= runLength * 60000) {
            isGreaterThanRunLength = true
        } 
        		
        //-----------------------------------------
		if (state.lastOnOffActivity == "turnedOn"){
        	//===============
        	if (state.currentOnOffActivity == "turnedOff"){
            	turnOffVirtualSump()
                state.lastCycleEndDateTime = now()
                if (isGreaterThanRunLength){
                	notifyRanTooLong() 
                }
                else{
                	notifyOff() 
                }
            }
            //===============
            else if (state.currentOnOffActivity == "turnedOn"){
				turnOnVirtualSump()
                if (isGreaterThanRunLength) {
                    if (!isGreaterThanRunEvery){
                        notifyRunTooOften()
                    }
                    else{
                        notifyOn()
                    }
                    state.lastCycleStartDateTime = now()
                }
			}
            //===============
            else if (state.currentOnOffActivity == "stillRunnning"){
            	turnOnVirtualSump()
                if (isGreaterThanRunLength){
                	notifyRunningTooLong()
                }
                else { 
                	//Nothing 
                }
            }
        }
        //-----------------------------------------
        else if (state.lastOnOffActivity == "turnedOff"){
        	//===============
        	if (state.currentOnOffActivity == "turnedOn"){
            	turnOnVirtualSump()
                state.lastCycleStartDateTime = now()
                if (!isGreaterThanRunEvery) {
                	notifyRunTooOften()
                }else{
                	notifyOn()
                }
            }
            //===============
            else if (state.currentOnOffActivity == "turnedOff"){
            	turnOffVirtualSump()
                state.lastCycleStopDateTime = now()
            	if (isGreaterThanRunLength){
                    notifyRanTooLong()
                }
                else{
                	notifyOff()
                }
            }
            //===============
            else if (state.currentOnOffActivity == "stillRunnning"){
            	turnOnVirtualSump()
                state.lastCycleStopDateTime = now()
                if (!isGreaterThanRunEvery) {
                	notifyRunTooOften()
                }else{
                	notifyOn()
                }
            }
        }
        
    }
    
    def turnOffManuallyAfterFiveMinutes(){
    	if (state.currentOnOffActivity == "turnedOff"){
        	//Do Nothing
        }else{
    		turnOffVirtualSump()
        	send("Manually turning off virtual Sump Pump")
        }
    }
    
    def turnOnVirtualSump(){
        log.trace "Turning on - virtual Sump Pump"
        sump.on()
        //runIn(runLength, checkToSeeIfTurnedOff)
    }

    def turnOffVirtualSump(){
	    log.trace "Turning off - virtual Sump Pump"
        sump.off()
    }
    //wet/dry===============================================================
    def turnOnWetStatus(){
        log.trace "Wet Status - virtual Sump Pump"
        sump.wet()
    }

    def turnOnDryStatus(){
        log.trace "Dry Status - virtual Sump Pump"
        sump.wet()
    }

//define alerts===============================================================
	//running
    def notifyOn(){
        log.trace "notifyOn"
        def message = "Sump Pump - On"
        sendNotification(message)
    }
	//Stopping
    def notifyOff(){
        log.trace "notifyOff"
        def message = "Sump Pump - Off"
        sendNotification(message)
    }
    //Running too long
    def notifyRunningTooLong(){
        log.trace "notifyRunningTooLong"
        def message = "Sump Pump - Running Longer than configured"
        send(message)
    }
    //Running too often
    def notifyRunTooOften(){
        log.trace "notifyRunTooOften"
        def message = "Sump Pump - Running more often than configured"
        send(message)
    }
    //Ran too long
    def notifyRanTooLong(){
        log.trace "notifyRanTooLong"
        def message = "Sump Pump - (Off) Ran longer than configured"
        send(message)
    }
    //Ran too often
    def notifyRanTooOften(){
        log.trace "notifyRanTooOften"
        def message = "Sump Pump - (Off) Ran more often than configured"
        send(message)
    }
    //Wet!!!
    def notifyWet(){
        log.trace "notifyWet"
        def message = "Sump Pump - Is half full and not running"
        send(message)
    }
    //Dry
    def notifyDry(){
        log.trace "notifyDry"
        def message = "Sump Pump - Dry"
        send(message)
    }
    //Wet while running...
    def notifyWetAndRunning(){
        log.trace "notifyWetAndRunning"
        def message = "Sump Pump - Is running but half full"
        send(message)
    }
//send notifications
private send(msg) {
    
    if (sendPushMessage) {
        sendPush(msg)
    }
    else {
    	sendNotificationEvent(msg)
    }
   
    if (phone) {
        sendSms(phone, msg)
    }
	
    log.debug msg
}

private sendNotification(msg) {
   	sendNotificationEvent(msg)
    log.debug msg
}

//cleanup
def cleanup(){
	state.lastOnOffActivity = state.currentOnOffActivity
}
