/**
 *  Virtual Sump Pump
 *
 *  Copyright 2016 Chris Apple
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
        definition (name: "Virtual Sump Pump", namespace: "nerkles", author: "Chris Apple") {
			capability "Switch"
			capability "Refresh"        
			capability "Water Sensor"
            
            attribute "lastTimeRan", "string"
			
		}
		// simulator metadata
		simulator {
		}

		// UI tile definitions
		tiles {
			valueTile("water", "device.water", width: 2, height: 2) {
				state "dry", icon:"st.alarm.water.dry", backgroundColor:"#ffffff", action: "wet"
				state "wet", icon:"st.alarm.water.wet", backgroundColor:"#53a7c0", action: "dry"
			}
            standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: true) {
				state "off", label: 'Off',  icon: "st.Kids.kid10", backgroundColor: "#ffffff"//, nextState: "on"
				state "on", label: 'On',  icon: "st.Kids.kid10", backgroundColor: "#79b821"//, nextState: "off"
			}
			standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
				state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
			}        
            //valueTile("lastTimeRan", "lastTimeRan", decoration: "flat") {
    			//state "lastTimeRan", label:'${currentValue}'
			//}
			standardTile("wet", "device.water", inactiveLabel: false, decoration: "flat") {
				state "default", label:'Wet', action:"wet", icon: "st.alarm.water.wet"
			}         
			standardTile("dry", "device.water", inactiveLabel: false, decoration: "flat") {
				state "default", label:'Dry', action:"dry", icon: "st.alarm.water.dry"
			}  
			main "water"
			details(["water","wet","dry", "button", "refresh"])
		}
}

def parse(String description) {
}

def on() {
	sendEvent(name: "switch", value: "on")
}

def off() {
	sendEvent(name: "switch", value: "off")
}

def wet() {
	sendEvent(name: "water", value: "wet")
}

def dry() {
	sendEvent(name: "water", value: "dry")
}