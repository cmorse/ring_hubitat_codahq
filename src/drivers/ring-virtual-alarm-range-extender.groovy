/**
 *  Ring Virtual Alarm Range Extender Driver
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
 *  2019-04-26: Initial
 *  2019-11-15: Import URL
 *  2020-02-12: Fixed battery % to show correctly in dashboards
 *  2020-02-29: Added checkin event
 *              Changed namespace
 *  2020-05-06: Added "PowerSource" capability
 *              Changed acStatus values to match closer to Ring's documentation labels for those alerts
 *
 */

import groovy.json.JsonSlurper

metadata {
  definition(name: "Ring Virtual Alarm Range Extender", namespace: "ring-hubitat-codahq", author: "Ben Rimmasch",
    importUrl: "https://raw.githubusercontent.com/codahq/ring_hubitat_codahq/master/src/drivers/ring-virtual-alarm-range-extender.groovy") {
    capability "Refresh"
    capability "Sensor"
    capability "Battery"
    capability "PowerSource"

    attribute "acStatus", "string"
    attribute "batteryStatus", "string"
    attribute "lastCheckin", "string"
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

def setValues(deviceInfo) {
  logDebug "updateDevice(deviceInfo)"
  logTrace "deviceInfo: ${deviceInfo}"

  if (deviceInfo.acStatus && deviceInfo.acStatus != null) {
    def acStatus = deviceInfo.acStatus == "ok" ? "connected" : (deviceInfo.acStatus == "error" ? "disconnected" : "brownout")
    checkChanged("acStatus", acStatus)
    def powerSource = deviceInfo.acStatus == "ok" ? "mains" : (deviceInfo.acStatus == "error" ? "battery" : "unknown")
    checkChanged("powerSource", powerSource)
  }
  //"batteryStatus": "full",
  //impulseType == battery.changed-out-of-band
  if (deviceInfo.batteryLevel != null) {
    checkChanged("battery", deviceInfo.batteryLevel, "%")
  }
  if (deviceInfo.batteryStatus != null /*&& deviceInfo.impulses?."battery.changed-out-of-band" != null*/) {
    checkChanged("batteryStatus", deviceInfo.batteryStatus)
  }
  if (deviceInfo.lastUpdate) {
    state.lastUpdate = deviceInfo.lastUpdate
  }
  if (deviceInfo.impulseType) {
    state.impulseType = deviceInfo.impulseType
    if (deviceInfo.impulseType == "comm.heartbeat") {
      sendEvent(name: "lastCheckin", value: convertToLocalTimeString(new Date()), displayed: false, isStateChange: true)
    }
  }
  if (deviceInfo.lastCommTime) {
    state.signalStrength = deviceInfo.lastCommTime
  }
  if (deviceInfo.nextExpectedWakeup) {
    state.nextExpectedWakeup = deviceInfo.nextExpectedWakeup
  }
  if (deviceInfo.signalStrength) {
    state.signalStrength = deviceInfo.signalStrength
  }
  if (deviceInfo.firmware && device.getDataValue("firmware") != deviceInfo.firmware) {
    device.updateDataValue("firmware", deviceInfo.firmware)
  }
  if (deviceInfo.hardwareVersion && device.getDataValue("hardwareVersion") != deviceInfo.hardwareVersion) {
    device.updateDataValue("hardwareVersion", deviceInfo.hardwareVersion)
  }

}

def checkChanged(attribute, newStatus) {
  checkChanged(attribute, newStatus, null)
}

def checkChanged(attribute, newStatus, unit) {
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
