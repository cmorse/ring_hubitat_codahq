/**
 *  Ring Virtual Siren Driver
 *
 *  Copyright 2019 Ben Rimmasch
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
 *
 *  Change Log:
 *  2019-11-12: Initial
 *  2019-11-15: Import URL
 *  2020-02-12: Fixed battery % to show correctly in dashboards
 *  2020-02-29: Added checkin event
 *              Changed namespace
 *  2020-05-11: Reduce repetition in some of the code
 */

metadata {
  definition(name: "Ring Virtual Siren", namespace: "ring-hubitat-codahq", author: "Ben Rimmasch",
    importUrl: "https://raw.githubusercontent.com/codahq/ring_hubitat_codahq/master/src/drivers/ring-virtual-siren.groovy") {
    capability "Refresh"
    capability "Sensor"
    capability "Battery"
    //capability "Alarm" For now this is commented out because I can't see a way through the WS or API to turn the siren on
    //using the alarm hub's 'security-panel.sound-siren' set command does not work.  technically, the siren tests could be
    //chained back to back with a scheduled call back but leaving this as is for now

    attribute "lastCheckin", "string"

    command "sirenTest"
    command "sirenTestCancel"
  }

  preferences {
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
  }
}

private logInfo(msg) {
  if (descriptionTextEnable) log.info msg
}

def logDebug(msg) {
  if (logEnable) log.debug msg
}

def logTrace(msg) {
  if (traceLogEnable) log.trace msg
}

def refresh() {
  logDebug "Attempting to refresh."
  //parent.simpleRequest("refresh-device", [dni: device.deviceNetworkId])
}

def sirenTest() {
  //TODO: make this impossible when alarm is armed.  if you attempt this through Ring's UIs it is prevented
  //pearl is too deep a dive to add code so this device can ask the hub device what the mode is right now.
  parent.simpleRequest("setcommand", [type: "siren-test.start", zid: device.getDataValue("zid"), dst: device.getDataValue("src"), data: {
  }])
}

def sirenTestCancel() {
  parent.simpleRequest("setcommand", [type: "siren-test.stop", zid: device.getDataValue("zid"), dst: device.getDataValue("src"), data: {
  }])
}

def setValues(deviceInfo) {
  logDebug "updateDevice(deviceInfo)"
  logTrace "deviceInfo: ${deviceInfo}"

  if (deviceInfo.batteryLevel != null) {
    checkChanged("battery", deviceInfo.batteryLevel, "%")
  }
  if (deviceInfo.tamperStatus) {
    checkChanged("tamper", deviceInfo.tamperStatus == "tamper" ? "detected" : "clear")
  }
  
  for(key in ['impulseType', 'lastCommTime', 'lastUpdate', 'nextExpectedWakeup', 'signalStrength']) {
    if (deviceInfo[key]) {
      state[key] = deviceInfo[key]
    }
  }
  
  if (deviceInfo?.impulseType == "comm.heartbeat") {
    sendEvent(name: "lastCheckin", value: convertToLocalTimeString(new Date()), displayed: false, isStateChange: true)
  }
  
  for(key in ['firmware', 'hardwareVersion']) {
    if (deviceInfo[key] && device.getDataValue(key) != deviceInfo[key]) {
      device.updateDataValue(key, deviceInfo[key])
    }
  }
}

def checkChanged(attribute, newStatus, unit=null) {
  if (device.currentValue(attribute) != newStatus) {
    logInfo "${attribute.capitalize()} for device ${device.label} is ${newStatus}"
    sendEvent(name: attribute, value: newStatus, unit: unit)
  }
}

private convertToLocalTimeString(dt) {
  def timeZoneId = location?.timeZone?.ID
  if (timeZoneId) {
    return dt.format("yyyy-MM-dd h:mm:ss a", TimeZone.getTimeZone(timeZoneId))
  }
  else {
    return "$dt"
  }
}